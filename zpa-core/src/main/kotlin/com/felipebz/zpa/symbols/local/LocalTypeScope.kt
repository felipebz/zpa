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

import com.felipebz.zpa.project.QualifiedName

import com.felipebz.zpa.symbols.local.LocalDeclarationKind.ATTRIBUTE
import com.felipebz.zpa.symbols.local.LocalDeclarationKind.OBJECT_TYPE
import java.util.*

/** What a lookup inside an object type sees beyond the ordinary regions: the type scope and the enclosing method. */
internal object LocalTypeScope {

    private val INSTANCE_METHODS = EnumSet.of(LocalMethodKind.MEMBER, LocalMethodKind.MAP, LocalMethodKind.ORDER)

    /**
     * A type body with its specification, or a specification, plus each supertype found in the file, following the
     * `UNDER` chain. Supertype attributes are visible unqualified; supertype methods only through `SELF`
     * ([supertypeMethods]). Specification and supertype declarations are visible whatever their order.
     */
    fun of(model: LocalSemanticModel, region: LocalRegion, supertypeMethods: Boolean): LocalResolver.Scope {
        val regions = arrayListOf(region)
        val attributesOnly = HashSet<LocalRegionId>()
        val gaps = EnumSet.noneOf(LocalLookupGap::class.java)
        val absenceGaps = EnumSet.noneOf(LocalLookupGap::class.java)
        val specification = if (region.kind == LocalRegionKind.OBJECT_TYPE) region else unitOf(model, region.id)?.counterpart?.let(model::region)
        if (region.kind == LocalRegionKind.TYPE_BODY) {
            if (specification == null) gaps += LocalLookupGap.SPECIFICATION_NOT_IN_FILE
            else regions += specification
        }
        var supertype = specification?.let { unitOf(model, it.id)?.supertype }
        while (supertype != null) {
            val unit = model.declarationsOf(model.root.id)
                .mapNotNull { declaration -> (declaration.details as? LocalUnit)?.takeIf { declaration.kind == OBJECT_TYPE && it.region != null && it.unitName == supertype } }
                .singleOrNull()
            val superRegion = unit?.region?.let(model::region)
            if (superRegion == null) {
                // A subtype cannot redeclare a supertype attribute (PLS-00410): only an absence relies on the supertype.
                absenceGaps += LocalLookupGap.SUPERTYPE_NOT_IN_FILE
                break
            }
            if (regions.any { it.id == superRegion.id }) break
            regions += superRegion
            if (!supertypeMethods) attributesOnly += superRegion.id
            supertype = unit.supertype
        }
        // A type body implements only methods its specification declares (PLS-00539): order never decides there.
        val earlier = regions.mapTo(HashSet()) { it.id }
        return LocalResolver.Scope(
            model, regions, specification?.id?.takeIf { region.kind == LocalRegionKind.TYPE_BODY }, earlier, gaps, absenceGaps, attributesOnly
        )
    }

    /** A constructor is named like its type: in a type position the name denotes the type, never the constructor. */
    fun capturesIn(declaration: LocalDeclaration, context: LocalReferenceContext): Boolean =
        context != LocalReferenceContext.TYPE || (declaration.details as? LocalSubprogram)?.methodKind != LocalMethodKind.CONSTRUCTOR

    fun unitOf(model: LocalSemanticModel, region: LocalRegionId): LocalUnit? = model.index.unit(region)

    /** The written name of the object type specification or type body enclosing [reference], or null. */
    fun enclosingTypeName(model: LocalSemanticModel, reference: LocalReference): QualifiedName? =
        LocalResolver.chain(model, reference.region)
            .firstOrNull { it.kind == LocalRegionKind.TYPE_BODY || it.kind == LocalRegionKind.OBJECT_TYPE }
            ?.let { unitOf(model, it.id)?.unitName }

    /**
     * The methods of [methods] without those a subtype overrides: a supertype method and a method of a type nearer the
     * type body [body] with the same heading are one entity (Oracle runs the override and reports no ambiguity), and
     * the nearer one is kept.
     */
    fun withoutOverridden(model: LocalSemanticModel, body: LocalRegion, methods: List<LocalDeclarationId>): List<LocalDeclarationId> {
        val regions = of(model, body, supertypeMethods = true).regions.map { it.id }
        fun depth(declaration: LocalDeclaration): Int {
            val region = model.region(declaration.region)
            val specification = if (region.kind == LocalRegionKind.TYPE_BODY) unitOf(model, region.id)?.counterpart ?: region.id else region.id
            return regions.indexOf(specification).takeIf { it >= 0 } ?: regions.indexOf(region.id)
        }
        val declarations = methods.map(model::declaration)
        val keys = declarations.map { LocalDeclarationLinker.signatureKey(it.kind, it.details as LocalSubprogram) }
        return declarations.filterIndexed { index, declaration ->
            val key = keys[index] ?: return@filterIndexed true
            val own = depth(declaration)
            own < 0 || declarations.indices.none { other -> other != index && keys[other] == key && depth(declarations[other]).let { it in 0 until own } }
        }.map { it.id }
    }

    /** The method of a type body whose region is on [chain], with that region. */
    class Method(val declaration: LocalDeclaration, val region: LocalRegionId) {
        val isStatic: Boolean get() = isStatic(declaration)
    }

    fun method(model: LocalSemanticModel, chain: List<LocalRegion>): Method? {
        val index = chain.indexOfFirst { it.kind == LocalRegionKind.TYPE_BODY }
        if (index <= 0) return null
        val region = chain[index - 1]
        val declaration = model.declarationsOf(chain[index].id).firstOrNull { (it.details as? LocalSubprogram)?.body == region.id }
        return declaration?.let { Method(it, region.id) }
    }

    /** A static method. */
    fun isStatic(declaration: LocalDeclaration) = (declaration.details as? LocalSubprogram)?.methodKind == LocalMethodKind.STATIC

    /** A member method, MAP and ORDER included: it needs an instance. */
    fun isInstanceMethod(declaration: LocalDeclaration) = (declaration.details as? LocalSubprogram)?.methodKind in INSTANCE_METHODS

    /**
     * From a static method, an attribute or a set of member methods is rejected (PLS-00588, PLS-00306). From a member
     * method, a set of static methods named without qualifier is invoked on the instance and rejected (PLS-00587).
     */
    fun rejectAcrossInstance(model: LocalSemanticModel, outcome: LocalResolution, fromStatic: Boolean): LocalResolution {
        fun instance(id: LocalDeclarationId) = model.declaration(id).let { it.kind == ATTRIBUTE || isInstanceMethod(it) }
        fun static(id: LocalDeclarationId) = isStatic(model.declaration(id))
        return when {
            !fromStatic -> if (outcome is LocalResolution.OverloadSet && outcome.declarations.all(::static)) {
                LocalResolution.WrongKind(outcome.declarations.first())
            } else outcome
            outcome is LocalResolution.Bound -> if (instance(outcome.declaration)) LocalResolution.InstanceMemberInStatic(outcome.declaration) else outcome
            outcome is LocalResolution.OverloadSet ->
                if (outcome.declarations.all(::instance)) LocalResolution.InstanceMemberInStatic(outcome.declarations.first()) else outcome
            else -> outcome
        }
    }
}
