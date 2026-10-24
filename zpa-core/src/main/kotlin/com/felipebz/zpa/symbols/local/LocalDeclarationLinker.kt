/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.project.*

/**
 * The passes that run after the walk of one build call: forward-declaration links, unit links and heading links.
 * They only read and rewrite the drafts of that call.
 */
internal class LocalDeclarationLinker(private val regions: List<RegionDraft>, private val declarations: List<DeclarationDraft>) {
    /**
     * Links a forward declaration and a body of the same region both ways when exactly one forward declaration
     * and exactly one body have the same kind, name, parameter names, modes and written parameter and return
     * types. Anything else (no match, several, an unrepresentable type) stays unlinked; order never decides.
     * One pass over each region's own declarations, grouped by a signature key.
     */
    fun linkForwardDeclarations() {
        for (region in regions) {
            if (region.kind !in FORWARD_SCOPES) continue
            val bodies = subprogramsByKey(region, LocalSubprogramForm.BODY)
            for ((key, matches) in subprogramsByKey(region, LocalSubprogramForm.HEADING_ONLY)) {
                val forward = matches.singleOrNull() ?: continue
                val body = bodies[key]?.singleOrNull() ?: continue
                forward.details = (forward.details as LocalSubprogram).copy(counterpart = body.id)
                body.details = (body.details as LocalSubprogram).copy(counterpart = forward.id)
            }
        }
    }

    /**
     * Links specifications and bodies of packages and object types, then each specification heading to its
     * implementation in the linked body. A unit pair is linked only when the file holds exactly one specification
     * and exactly one body with the same written unit name. An incomplete type (`CREATE TYPE t;`) does not count as
     * a specification: it is the same object that a later complete definition completes. Wrapped units count. Headings use the forward-link signature key (which
     * includes the method kind). Grouping by key in hash maps: linear, and order never decides.
     */
    fun linkUnits() {
        fun unitsByKey(vararg kinds: LocalDeclarationKind): Map<String, List<DeclarationDraft>> = declarations
            .filter { it.kind in kinds && (it.details as LocalUnit).form != LocalUnitForm.INCOMPLETE }
            .groupBy { draft ->
                val family = if (draft.kind == LocalDeclarationKind.PACKAGE || draft.kind == LocalDeclarationKind.PACKAGE_BODY) "package" else "type"
                family + "|" + (draft.details as LocalUnit).unitName.lookupKey()
            }
        val specifications = unitsByKey(LocalDeclarationKind.PACKAGE, LocalDeclarationKind.OBJECT_TYPE)
        val bodies = unitsByKey(LocalDeclarationKind.PACKAGE_BODY, LocalDeclarationKind.TYPE_BODY)
        for ((key, matches) in specifications) {
            val specification = matches.singleOrNull() ?: continue
            val body = bodies[key]?.singleOrNull() ?: continue
            val specificationUnit = specification.details as LocalUnit
            val bodyUnit = body.details as LocalUnit
            val specificationRegion = specificationUnit.region ?: continue
            val bodyRegion = bodyUnit.region ?: continue
            specification.details = LocalUnit(specificationUnit.unitName, specificationUnit.form, specificationRegion, bodyRegion, specificationUnit.supertype)
            body.details = LocalUnit(bodyUnit.unitName, bodyUnit.form, bodyRegion, specificationRegion, bodyUnit.supertype)
            linkHeadings(regions[specificationRegion.ordinal], regions[bodyRegion.ordinal])
        }
    }

    private fun linkHeadings(specification: RegionDraft, body: RegionDraft) {
        val headings = subprogramsByKey(specification, LocalSubprogramForm.HEADING_ONLY)
        val implementations = subprogramsByKey(body, LocalSubprogramForm.BODY)
        for ((key, matches) in headings) {
            val heading = matches.singleOrNull() ?: continue
            val implementation = implementations[key]?.singleOrNull() ?: continue
            heading.details = (heading.details as LocalSubprogram).copy(specificationCounterpart = implementation.id)
            implementation.details = (implementation.details as LocalSubprogram).copy(specificationCounterpart = heading.id)
        }
    }

    private fun subprogramsByKey(region: RegionDraft, form: LocalSubprogramForm): Map<String, List<DeclarationDraft>> {
        val result = HashMap<String, MutableList<DeclarationDraft>>()
        for (id in region.declarations) {
            val draft = declarations[id.ordinal]
            val subprogram = draft.details as? LocalSubprogram ?: continue
            if (subprogram.form != form) continue
            val key = signatureKey(draft.kind, subprogram) ?: continue
            result.getOrPut(key) { ArrayList() } += draft
        }
        return result
    }

    companion object {
        /**
         * The heading identity of a subprogram: kind, method kind, name, and each parameter's name, mode and written type,
         * plus the RETURN type of a function. Null when a type cannot be represented. Also used by the resolver to find two
         * bodies of one heading. Like Oracle, it ignores case, `NOCOPY`, defaults and whether `IN` is written.
         */
        fun signatureKey(kind: LocalDeclarationKind, subprogram: LocalSubprogram): String? = key(
            kind.name, subprogram.methodKind, subprogram.unitName.last,
            subprogram.parameters.map { Triple(it.name, it.mode, it.type?.typeRef ?: return null) },
            subprogram.returnsSelf,
            if (kind == LocalDeclarationKind.FUNCTION && !subprogram.returnsSelf) subprogram.returnType?.typeRef ?: return null else null
        )

        /** The heading identity of a package specification subprogram of the project, comparable with the local one. */
        fun signatureKey(declaration: PackageSubprogramDeclaration): String = key(
            if (declaration is PackageFunctionDeclaration) LocalDeclarationKind.FUNCTION.name else LocalDeclarationKind.PROCEDURE.name,
            null, declaration.name, declaration.parameters.map { Triple(it.name, it.mode, it.typeRef) },
            false, (declaration as? PackageFunctionDeclaration)?.returnType
        )

        private fun key(
            kind: String,
            methodKind: LocalMethodKind?,
            name: OracleIdentifier,
            parameters: List<Triple<OracleIdentifier, ParameterMode, TypeRef>>,
            returnsSelf: Boolean,
            returnType: TypeRef?
        ): String {
            val key = StringBuilder(kind).append('|').append(methodKind ?: "").append('|').append(QualifiedName(name).lookupKey())
            for ((parameterName, mode, type) in parameters) {
                key.append('|').append(QualifiedName(parameterName).lookupKey()).append(' ').append(mode).append(' ').append(type.headerIdentityKey())
            }
            if (returnsSelf) key.append("|return SELF") else if (returnType != null) key.append("|return ").append(returnType.headerIdentityKey())
            return key.toString()
        }

        /** Regions whose forward subprogram declarations are linked to their bodies. */
        private val FORWARD_SCOPES = setOf(
            LocalRegionKind.ANONYMOUS_BLOCK, LocalRegionKind.SUBPROGRAM, LocalRegionKind.PACKAGE_BODY,
            LocalRegionKind.TRIGGER, LocalRegionKind.TIMING_POINT
        )
    }
}
