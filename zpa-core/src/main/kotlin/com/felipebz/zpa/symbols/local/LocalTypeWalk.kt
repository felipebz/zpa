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
@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.project.*
import com.felipebz.zpa.symbols.local.LocalDeclarationKind.*

/**
 * Datatype evidence of a local value, as an AST-detached fact: the [argumentType] given to
 * [ProjectSnapshot.narrowCall], the local declarations followed to reach it, where the walk ended, and why the
 * evidence is unknown when it is.
 */
internal class LocalTypeEvidence(
    val argumentType: ProjectArgumentType,
    val terminal: Terminal,
    /** The local declarations followed, in order: the value, then each subtype or anchored declaration. */
    val followed: List<LocalDeclarationId>,
    val unknownReason: String?
) {
    enum class Terminal {
        /** A predefined type name, written or reached through local subtypes. */
        BUILT_IN,

        /** A local record, collection or ref cursor type, a `%ROWTYPE` of a local cursor, or `SELF`. */
        COMPOSITE,

        /** A name or anchor handed to the project and described there. */
        PROJECT,

        /** A literal actual. */
        LITERAL,

        /** No evidence: [unknownReason] says why. */
        UNKNOWN
    }

    override fun toString() = "$terminal $argumentType${unknownReason?.let { " ($it)" } ?: ""}"

    companion object {
        fun unknown(reason: String, followed: List<LocalDeclarationId> = emptyList()) =
            LocalTypeEvidence(ProjectArgumentType.unknown(), Terminal.UNKNOWN, followed, reason)

        fun literal(literal: LocalLiteral?): LocalTypeEvidence = when {
            literal == null -> unknown("literal not classified")
            literal.typeName == null -> LocalTypeEvidence(ProjectArgumentType.nullLiteral(), Terminal.LITERAL, emptyList(), null)
            else -> LocalTypeEvidence(ProjectArgumentType.builtIn(literal.typeName), Terminal.LITERAL, emptyList(), null)
        }
    }
}

/**
 * Follows the written type of a local value at its declaration site, through the resolver's outcomes for the type's
 * own lookup, until a terminal:
 * - a local subtype is followed to its written type; a local record, collection or ref cursor type is composite;
 * - a `%TYPE` anchor resolved locally walks the anchored value or field; a `%ROWTYPE` of a local cursor is composite;
 * - a one-segment name proven absent is a predefined type name for [ProjectArgumentType.builtIn];
 * - a qualified name or an anchor whose first segment is proven absent is described by the project, with the
 *   enclosing package as owner;
 * - anything else (an incomplete step, a wrong kind, a later declaration, a cycle) is unknown.
 *
 * A package variable or constant of a specification is described from the project's own fact for that declaration,
 * matched by range, when [snapshot] is prepared; otherwise its written type is walked locally. Legacy datatypes are
 * never consulted.
 */
internal class LocalTypeWalk(
    private val model: LocalSemanticModel,
    private val snapshot: ProjectSnapshot?,
    private val metadata: ProjectExternalMetadata = ProjectExternalMetadata.none()
) {
    private val prepared = snapshot != null && snapshot.preparationState != ProjectPreparationState.NOT_PREPARED

    /** Evidence of the value [declaration] names. */
    fun value(declaration: LocalDeclarationId): LocalTypeEvidence = Walk().value(model.declaration(declaration))

    /** Evidence of field [ordinal] of the local record type [recordType]. */
    fun field(recordType: LocalDeclarationId, ordinal: Int): LocalTypeEvidence = Walk().field(recordType, ordinal)

    /** Evidence of a resolved name: a value, a record field or `SELF`; anything else is unknown. */
    fun outcome(outcome: LocalResolution): LocalTypeEvidence = when (outcome) {
        is LocalResolution.Bound -> value(outcome.declaration)
        is LocalResolution.Field -> field(outcome.recordType, outcome.ordinal)
        is LocalResolution.ImplicitSelf -> composite(emptyList())
        else -> LocalTypeEvidence.unknown("not a local value: $outcome")
    }

    /** Evidence of the package value [name] (first segment proven absent): the project describes it as `name%TYPE`. */
    fun projectValue(name: QualifiedName, region: LocalRegionId): LocalTypeEvidence {
        val owner = packageOwner(region)
        val described = snapshot?.describeExternalType(
            ProjectWrittenAnchor.of(ProjectQualifiedName.wrap(name), ProjectTypeReference.Anchor.TYPE), owner, metadata, null
        ) ?: return LocalTypeEvidence.unknown("no project snapshot")
        return LocalTypeEvidence(ProjectArgumentType.described(described, owner), LocalTypeEvidence.Terminal.PROJECT, emptyList(), null)
    }

    private fun composite(followed: List<LocalDeclarationId>) =
        LocalTypeEvidence(ProjectArgumentType.composite(), LocalTypeEvidence.Terminal.COMPOSITE, followed, null)

    /** The package whose specification or body holds [region], as an owner for the project's name lookups. */
    private fun packageOwner(region: LocalRegionId): ProjectQualifiedName? =
        LocalResolver.chain(model, region).firstOrNull { it.kind == LocalRegionKind.PACKAGE_SPEC || it.kind == LocalRegionKind.PACKAGE_BODY }
            ?.let { LocalTypeScope.unitOf(model, it.id) }?.let { ProjectQualifiedName.wrap(it.unitName) }

    private inner class Walk {
        private val followed = ArrayList<LocalDeclarationId>()
        private val fields = HashSet<Pair<LocalDeclarationId, Int>>()

        private fun unknown(reason: String) = LocalTypeEvidence.unknown(reason, followed.toList())

        fun value(declaration: LocalDeclaration): LocalTypeEvidence {
            if (declaration.kind !in VALUES) return unknown("${declaration.kind} ${declaration.name} is not a value")
            if (declaration.id in followed) return unknown("anchor cycle")
            followed += declaration.id
            if ((declaration.details as? LocalLoopIndex)?.isTypeImplicit == true) return unknown("typed by its iteration control")
            packageValue(declaration)?.let { return it }
            val use = declaration.type ?: return unknown("no written type")
            return type(use)
        }

        fun field(recordType: LocalDeclarationId, ordinal: Int): LocalTypeEvidence {
            if (!fields.add(recordType to ordinal)) return unknown("anchor cycle")
            val field = (model.declaration(recordType).details as LocalRecordType).fields[ordinal]
            return type(field.type ?: return unknown("no written type"))
        }

        private fun type(use: LocalTypeUse): LocalTypeEvidence {
            val typeRef = use.typeRef ?: return unknown("type not representable")
            if (typeRef is RefTypeRef) return unknown("REF type")
            val reference = LocalReferenceCollector.typeUseReference(use) ?: return unknown("type behind a database link")
            val steps = LocalResolver.resolveQualified(model, reference)
            if (steps.first() == LocalResolution.ProvenAbsent) return project(typeRef, use)
            val last = steps.last()
            if (steps.size != reference.segments.size + 1) return unknown("stopped at $last")
            return when (last) {
                is LocalResolution.Bound -> declared(model.declaration(last.declaration), typeRef)
                is LocalResolution.Field ->
                    if ((typeRef as? AnchoredTypeRef)?.anchor == TypeAnchor.TYPE) field(last.recordType, last.ordinal) else unknown("a field is not a type")
                else -> unknown(last.toString())
            }
        }

        private fun declared(target: LocalDeclaration, typeRef: com.felipebz.zpa.project.TypeRef): LocalTypeEvidence = when {
            typeRef is NamedTypeRef -> when (target.kind) {
                SUBTYPE -> if (target.id in followed) unknown("subtype cycle") else {
                    followed += target.id
                    target.type?.let(::type) ?: unknown("no written type")
                }
                RECORD_TYPE, COLLECTION_TYPE, REF_CURSOR_TYPE -> { followed += target.id; composite(followed.toList()) }
                else -> unknown("${target.kind} ${target.name} is not a type")
            }
            typeRef is AnchoredTypeRef && typeRef.anchor == TypeAnchor.TYPE -> value(target)
            typeRef is AnchoredTypeRef && target.kind == CURSOR -> { followed += target.id; composite(followed.toList()) }
            else -> unknown("${target.kind} ${target.name} anchors no row")
        }

        /** The first segment is proven absent in complete regions: the name is not local. */
        private fun project(typeRef: com.felipebz.zpa.project.TypeRef, use: LocalTypeUse): LocalTypeEvidence {
            val name = typeRef.name
            if (typeRef is NamedTypeRef && name.segments.size == 1) {
                return LocalTypeEvidence(ProjectArgumentType.builtIn(name.last.originalSpelling), LocalTypeEvidence.Terminal.BUILT_IN, followed.toList(), null)
            }
            val snapshot = snapshot ?: return unknown("no project snapshot")
            val owner = packageOwner(use.lookupRegion)
            val written = ProjectQualifiedName.wrap(name)
            val described = when (typeRef) {
                is AnchoredTypeRef -> snapshot.describeExternalType(
                    ProjectWrittenAnchor.of(written, if (typeRef.anchor == TypeAnchor.TYPE) ProjectTypeReference.Anchor.TYPE else ProjectTypeReference.Anchor.ROWTYPE),
                    owner, metadata, null
                )
                else -> snapshot.describeExternalType(written, owner, metadata, null)
            }
            return LocalTypeEvidence(ProjectArgumentType.described(described, owner), LocalTypeEvidence.Terminal.PROJECT, followed.toList(), null)
        }

        /** A specification variable or constant with a prepared snapshot: the project's fact for it, matched by range. */
        private fun packageValue(declaration: LocalDeclaration): LocalTypeEvidence? {
            if (!prepared || declaration.kind != VARIABLE || model.region(declaration.region).kind != LocalRegionKind.PACKAGE_SPEC) return null
            val range = declaration.range
            val view = snapshot!!.declarationsFor(model.fileId.value).declarations.filterIsInstance<ProjectPackageValueView>().singleOrNull {
                val r = it.sourceRange
                r.startLine == range.startLine && r.startColumn == range.startColumn && r.endLine == range.endLine && r.endColumn == range.endColumn
            } ?: return null
            val described = snapshot.describeExternalType(view.typeRef, view.owner, metadata, null)
            return LocalTypeEvidence(ProjectArgumentType.described(described, view.owner), LocalTypeEvidence.Terminal.PROJECT, followed.toList(), null)
        }
    }

    companion object {
        private val VALUES = setOf(VARIABLE, PARAMETER, CURSOR_PARAMETER, LOOP_INDEX)
    }
}
