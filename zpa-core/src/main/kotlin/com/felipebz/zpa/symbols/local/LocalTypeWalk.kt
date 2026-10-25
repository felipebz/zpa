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
    val unknownReason: String?,
    /** For a [Terminal.PROJECT] evidence, the project's description it classifies. */
    val description: ProjectExternalTypeDescription? = null,
    /** For an unknown evidence, the incomplete lookup that stopped the walk, when one did. */
    val incomplete: LocalResolution.Incomplete? = null
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
        fun unknown(reason: String, followed: List<LocalDeclarationId> = emptyList(), incomplete: LocalResolution.Incomplete? = null) =
            LocalTypeEvidence(ProjectArgumentType.unknown(), Terminal.UNKNOWN, followed, reason, incomplete = incomplete)

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
 * - a one-segment name proven absent is a predefined type name for [ProjectArgumentType.builtIn] when it names a
 *   predefined type, and is described by the project otherwise (a schema type never shadows a predefined name);
 * - a qualified name or an anchor whose first segment is proven absent is described by the project, with the
 *   enclosing package as owner;
 * - an index typed by its iteration control is a PLS_INTEGER for a numeric range and a row for a cursor or a query;
 * - anything else (an incomplete step, a wrong kind, a later declaration, a cycle) is unknown.
 *
 * A package variable or constant of a specification is walked locally first. The project's own fact for that
 * declaration, matched by range with a prepared [snapshot], is used only when the local walk gives no family; when
 * both give a conclusive family and they differ, the evidence is unknown. Legacy datatypes are never consulted.
 */
internal class LocalTypeWalk(
    private val model: LocalSemanticModel,
    private val snapshot: ProjectSnapshot?,
    /** The run's external metadata and compilation schema, as the project's own resolution uses them. */
    private val external: ProjectExternalContext = ProjectExternalContext.none(),
    /** The outcomes of its lookups: the resolver's, refined with the project's specification coverage. */
    private val outcomes: LocalReferenceOutcomes =
        LocalReferenceOutcomes(model, snapshot?.let { LocalSpecificationCoverage.of(model, it) })
) {
    private val metadata = external.metadata

    /**
     * The schema the file-level unit holding [region] compiles in, from the run's external context: its written schema,
     * else the configured default. Null for code outside a named unit (an anonymous block of a script), and when
     * neither establishes it: the project then keeps the uncertainty.
     */
    fun compilationSchema(region: LocalRegionId): ProjectIdentifier? {
        val top = LocalResolver.chain(model, region).firstOrNull { it.parent == model.root.id } ?: return null
        val name = model.declarationsOf(model.root.id).firstNotNullOfOrNull { declaration ->
            when (val details = declaration.details) {
                is LocalUnit -> details.unitName.takeIf { details.region == top.id }
                is LocalSubprogram -> details.unitName.takeIf { details.body == top.id }
                is LocalTrigger -> details.unitName.takeIf { details.body == top.id }
                else -> null
            }
        } ?: return null
        return external.compilationSchema(ProjectQualifiedName.wrap(name)).orElse(null)
    }
    private val prepared = snapshot != null && snapshot.preparationState != ProjectPreparationState.NOT_PREPARED

    /** Evidence of the value [declaration] names. */
    fun value(declaration: LocalDeclarationId): LocalTypeEvidence = Walk().value(model.declaration(declaration))

    /** Evidence of field [ordinal] of the local record type [recordType]. */
    fun field(recordType: LocalDeclarationId, ordinal: Int): LocalTypeEvidence = Walk().field(recordType, ordinal)

    /**
     * Evidence of a resolved name: a value, a record field, `SELF`, or the result of a single local function called
     * without arguments; anything else is unknown.
     */
    fun outcome(outcome: LocalResolution): LocalTypeEvidence = when (outcome) {
        is LocalResolution.Bound -> value(outcome.declaration)
        is LocalResolution.Field -> field(outcome.recordType, outcome.ordinal)
        is LocalResolution.ImplicitSelf -> composite(emptyList())
        is LocalResolution.OverloadSet -> Walk().result(outcome)
        else -> LocalTypeEvidence.unknown("not a local value: $outcome")
    }

    /**
     * Evidence of `name(…)` resolved to [outcome]: an element of a local collection value, whatever the index, or the
     * result of a single local function called with [arguments] actuals. An overload set of several functions is unknown.
     */
    fun call(outcome: LocalResolution, arguments: Int): LocalTypeEvidence = when (outcome) {
        is LocalResolution.Bound, is LocalResolution.Field -> Walk().element(outcome)
        is LocalResolution.OverloadSet -> Walk().result(outcome, arguments)
        else -> LocalTypeEvidence.unknown("not a local function or collection: $outcome")
    }

    /** Evidence of the package value [name] (first segment proven absent): the project describes it as `name%TYPE`. */
    fun projectValue(name: QualifiedName, region: LocalRegionId): LocalTypeEvidence {
        val owner = packageOwner(region)
        val described = snapshot?.describeExternalType(
            ProjectWrittenAnchor.of(ProjectQualifiedName.wrap(name), ProjectTypeReference.Anchor.TYPE), owner, metadata, compilationSchema(region)
        ) ?: return LocalTypeEvidence.unknown("no project snapshot")
        return LocalTypeEvidence(ProjectArgumentType.described(described, owner), LocalTypeEvidence.Terminal.PROJECT, emptyList(), null, described)
    }

    /**
     * The project's description of the element of [collection], a collection type the project describes, read from
     * [region]: a package type's element is looked up with its package as owner. Null without a project or an element.
     */
    fun projectElement(collection: ProjectShapedTypeView, region: LocalRegionId): ProjectExternalTypeDescription? {
        val element = collection.elementType.orElse(null) ?: return null
        val snapshot = snapshot ?: return null
        val owner = (collection as? ProjectPackageTypeView)?.owner
        return snapshot.describeExternalType(element, owner, metadata, compilationSchema(region))
    }

    /** Evidence of a written type at its own lookup site, such as a formal parameter's. */
    fun written(use: LocalTypeUse?): LocalTypeEvidence = use?.let { Walk().type(it) } ?: LocalTypeEvidence.unknown(NO_WRITTEN_TYPE)

    /** Evidence of the result of the local function [function], a call resolved to that entity. */
    fun result(function: LocalDeclarationId): LocalTypeEvidence = Walk().result(model.declaration(function), 0)

    /** Evidence of a project call's result: the RETURN type of the function a RESOLVED call selected. */
    fun projectResult(typed: ProjectTypedCallResolution?, region: LocalRegionId): LocalTypeEvidence {
        if (typed == null) return LocalTypeEvidence.unknown("not forwarded")
        if (typed.status != ProjectTypedCallResolution.Status.RESOLVED) return LocalTypeEvidence.unknown("the call is ${typed.status}")
        val function = typed.selected.get().declaration as? ProjectFunctionView ?: return LocalTypeEvidence.unknown("not a function")
        val owner = (function as? ProjectPackageSubprogramView)?.packageOwner
        val described = snapshot!!.describeExternalType(function.returnType, owner, metadata, compilationSchema(region))
        return LocalTypeEvidence(ProjectArgumentType.described(described, owner), LocalTypeEvidence.Terminal.PROJECT, emptyList(), null, described)
    }

    private fun composite(followed: List<LocalDeclarationId>) =
        LocalTypeEvidence(ProjectArgumentType.composite(), LocalTypeEvidence.Terminal.COMPOSITE, followed, null)

    /** A decision made with the project's specification coverage is still the body's: its declaration is local. */
    private fun covered(outcome: LocalResolution) = (outcome as? LocalResolution.SpecificationCovered)?.outcome ?: outcome

    /** [segments], a member of the specification of the package body holding [region], named through that package. */
    fun specificationMember(segments: List<OracleIdentifier>, region: LocalRegionId): QualifiedName? {
        val body = LocalResolver.chain(model, region).firstOrNull { it.kind == LocalRegionKind.PACKAGE_BODY } ?: return null
        val unit = LocalTypeScope.unitOf(model, body.id) ?: return null
        return QualifiedName(unit.unitName.segments + segments)
    }

    /** The package whose specification or body holds [region], as an owner for the project's name lookups. */
    fun packageOwner(region: LocalRegionId): ProjectQualifiedName? =
        LocalResolver.chain(model, region).firstOrNull { it.kind == LocalRegionKind.PACKAGE_SPEC || it.kind == LocalRegionKind.PACKAGE_BODY }
            ?.let { LocalTypeScope.unitOf(model, it.id) }?.let { ProjectQualifiedName.wrap(it.unitName) }

    private inner class Walk {
        private val followed = ArrayList<LocalDeclarationId>()
        private val fields = HashSet<Pair<LocalDeclarationId, Int>>()

        private fun unknown(reason: String, incomplete: LocalResolution.Incomplete? = null) =
            LocalTypeEvidence.unknown(reason, followed.toList(), incomplete)

        fun value(declaration: LocalDeclaration): LocalTypeEvidence {
            if (declaration.kind !in VALUES) return unknown("${declaration.kind} ${declaration.name} is not a value")
            if (declaration.id in followed) return unknown("anchor cycle")
            followed += declaration.id
            val index = declaration.details as? LocalLoopIndex
            if (index?.isTypeImplicit == true) return loopIndex(declaration, index.control)
            val local = declaration.type?.let(::type) ?: unknown(NO_WRITTEN_TYPE)
            return packageValue(declaration, local)
        }

        /** An index typed by its iteration control (Oracle: a numeric range's index is a PLS_INTEGER). */
        private fun loopIndex(declaration: LocalDeclaration, control: LocalLoopControl?): LocalTypeEvidence = when (control?.form) {
            LocalLoopControl.Form.STEPPED ->
                LocalTypeEvidence(ProjectArgumentType.builtIn("PLS_INTEGER"), LocalTypeEvidence.Terminal.BUILT_IN, followed.toList(), null)
            LocalLoopControl.Form.QUERY -> composite(followed.toList())
            LocalLoopControl.Form.NAME -> {
                // The control is written before the index is declared: it is looked up where the loop is written.
                val enclosing = model.region(declaration.region).parent ?: return unknown("no enclosing region")
                val reference = LocalReference(control.name!!, control.nameRange!!, LocalReferenceContext.FOR_CONTROL, enclosing, emptyList(),
                    LocalReferenceContext.FOR_CONTROL, false, null)
                val first = outcomes.first(reference)
                if (LocalReferenceOutcomes.isSpecificationMember(first)) {
                    // A cursor of the specification in another file: a row only when the project describes one.
                    val row = specificationMember(listOf(control.name), enclosing)?.let { name -> project(name, TypeAnchor.ROWTYPE, enclosing) }
                    if (row?.argumentType?.family?.orElse(null) == ProjectDatatypeFamily.COMPOSITE) row else unknown("a specification name the project does not describe as a row")
                } else {
                    val cursor = (covered(first) as? LocalResolution.Bound)?.let { model.declaration(it.declaration) }
                    if (cursor?.kind == CURSOR) { followed += cursor.id; composite(followed.toList()) } else unknown("iterates over a value")
                }
            }
            null -> unknown("typed by its iteration control")
        }

        /** The element type of the local collection [holder] holds. */
        fun element(holder: LocalResolution): LocalTypeEvidence {
            val evidence = when (holder) {
                is LocalResolution.Bound -> value(model.declaration(holder.declaration))
                is LocalResolution.Field -> field(holder.recordType, holder.ordinal)
                else -> return unknown("not a value")
            }
            val collection = evidence.followed.lastOrNull()?.let(model::declaration)
                ?.takeIf { evidence.terminal == LocalTypeEvidence.Terminal.COMPOSITE && it.kind == COLLECTION_TYPE }
                ?: return unknown("not an element of a local collection")
            return collection.type?.let(::type) ?: unknown("no written element type")
        }

        /** The RETURN type of the single local function [functions] names; unknown for several entities. */
        fun result(functions: LocalResolution.OverloadSet, arguments: Int = 0): LocalTypeEvidence =
            functions.declarations.singleOrNull()?.let { result(model.declaration(it), arguments) } ?: unknown("overloaded")

        fun result(function: LocalDeclaration, arguments: Int): LocalTypeEvidence {
            val subprogram = function.details as? LocalSubprogram
            if (function.kind != FUNCTION || subprogram == null) return unknown("not a function")
            // Oracle rejects `f(1)` for a parameterless `f` (PLS-00306): it does not index the result.
            if (arguments > 0 && subprogram.parameters.isEmpty()) return unknown("a parameterless function called with actuals")
            followed += function.id
            if (subprogram.returnsSelf) return composite(followed.toList())
            return subprogram.returnType?.let(::type) ?: unknown("no written RETURN type")
        }

        fun field(recordType: LocalDeclarationId, ordinal: Int): LocalTypeEvidence {
            if (!fields.add(recordType to ordinal)) return unknown("anchor cycle")
            val field = (model.declaration(recordType).details as LocalRecordType).fields[ordinal]
            return type(field.type ?: return unknown(NO_WRITTEN_TYPE))
        }

        fun type(use: LocalTypeUse): LocalTypeEvidence {
            val typeRef = use.typeRef ?: return unknown("type not representable")
            if (typeRef is RefTypeRef) return unknown("REF type")
            val reference = LocalReferenceCollector.typeUseReference(use) ?: return unknown("type behind a database link")
            val steps = outcomes.steps(reference)
            if (LocalReferenceOutcomes.isAbsent(steps.first())) return project(typeRef, use)
            // A member of the package's specification in another file: described by the project through the package.
            val member = steps.indexOfFirst(LocalReferenceOutcomes::isSpecificationMember)
            if (member >= 0) {
                val name = if (member == 0) specificationMember(typeRef.name.segments, use.lookupRegion) else typeRef.name
                val anchor = (typeRef as? AnchoredTypeRef)?.anchor
                return name?.let { project(it, anchor, use.lookupRegion) } ?: unknown("no package for a specification member")
            }
            val last = covered(steps.last())
            if (last is LocalResolution.Incomplete) return unknown(last.toString(), last)
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
                    target.type?.let(::type) ?: unknown(NO_WRITTEN_TYPE)
                }
                RECORD_TYPE, COLLECTION_TYPE, REF_CURSOR_TYPE -> { followed += target.id; composite(followed.toList()) }
                else -> unknown("${target.kind} ${target.name} is not a type")
            }
            typeRef is AnchoredTypeRef && typeRef.anchor == TypeAnchor.TYPE -> value(target)
            typeRef is AnchoredTypeRef && target.kind == CURSOR -> { followed += target.id; composite(followed.toList()) }
            else -> unknown("${target.kind} ${target.name} anchors no row")
        }

        /** The first segment is proven absent: the name is not local. */
        private fun project(typeRef: com.felipebz.zpa.project.TypeRef, use: LocalTypeUse): LocalTypeEvidence {
            val name = typeRef.name
            if (typeRef is NamedTypeRef && name.segments.size == 1) {
                val spelling = name.last.originalSpelling
                // A schema type never shadows a predefined name; any other one-segment name is a schema type.
                if (DatatypeFamilies.ofTypeName(spelling) != null || snapshot == null) {
                    return LocalTypeEvidence(ProjectArgumentType.builtIn(spelling), LocalTypeEvidence.Terminal.BUILT_IN, followed.toList(), null)
                }
                val described = snapshot.describeExternalType(ProjectQualifiedName.wrap(name), null, metadata, compilationSchema(use.lookupRegion))
                return LocalTypeEvidence(ProjectArgumentType.described(described, null), LocalTypeEvidence.Terminal.PROJECT, followed.toList(), null, described)
            }
            return project(name, (typeRef as? AnchoredTypeRef)?.anchor, use.lookupRegion)
        }

        /** [name] described by the project, as a type, or as the `%TYPE` / `%ROWTYPE` [anchor] of it. */
        fun project(name: QualifiedName, anchor: TypeAnchor?, region: LocalRegionId): LocalTypeEvidence {
            val snapshot = snapshot ?: return unknown("no project snapshot")
            val owner = packageOwner(region)
            val written = ProjectQualifiedName.wrap(name)
            val described = when (anchor) {
                null -> snapshot.describeExternalType(written, owner, metadata, compilationSchema(region))
                else -> snapshot.describeExternalType(
                    ProjectWrittenAnchor.of(written, if (anchor == TypeAnchor.TYPE) ProjectTypeReference.Anchor.TYPE else ProjectTypeReference.Anchor.ROWTYPE),
                    owner, metadata, compilationSchema(region)
                )
            }
            return LocalTypeEvidence(ProjectArgumentType.described(described, owner), LocalTypeEvidence.Terminal.PROJECT, followed.toList(), null, described)
        }

        /**
         * A specification variable or constant: the [local] walk when it gives a family, else the project's fact for it,
         * matched by range; two conclusive families that differ give unknown.
         */
        private fun packageValue(declaration: LocalDeclaration, local: LocalTypeEvidence): LocalTypeEvidence {
            val project = projectFact(declaration) ?: return local
            val localFamily = local.argumentType.family.orElse(null) ?: return project
            // A built-in name the project reached may still be shadowed by a package member: not conclusive.
            val projectFamily = project.argumentType.family.orElse(null)?.takeIf { project.argumentType.shadowName == null }
            return if (projectFamily == null || projectFamily == localFamily) local
            else unknown("the local walk gives $localFamily, the project fact $projectFamily")
        }

        private fun projectFact(declaration: LocalDeclaration): LocalTypeEvidence? {
            if (!prepared || declaration.kind != VARIABLE || model.region(declaration.region).kind != LocalRegionKind.PACKAGE_SPEC) return null
            val range = declaration.range
            val view = snapshot!!.declarationsFor(model.fileId.value).declarations.filterIsInstance<ProjectPackageValueView>().singleOrNull {
                val r = it.sourceRange
                r.startLine == range.startLine && r.startColumn == range.startColumn && r.endLine == range.endLine && r.endColumn == range.endColumn
            } ?: return null
            val described = snapshot.describeExternalType(view.typeRef, view.owner, metadata, compilationSchema(declaration.region))
            return LocalTypeEvidence(ProjectArgumentType.described(described, view.owner), LocalTypeEvidence.Terminal.PROJECT, followed.toList(), null, described)
        }
    }

    companion object {
        private val VALUES = setOf(VARIABLE, PARAMETER, CURSOR_PARAMETER, LOOP_INDEX)
    }
}

private const val NO_WRITTEN_TYPE = "no written type"
