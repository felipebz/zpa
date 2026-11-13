//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.project.*
import com.felipebz.zpa.symbols.StaticArithmetic
import com.felipebz.zpa.symbols.local.LocalDeclarationKind.*
import java.math.BigDecimal
import com.felipebz.zpa.api.project.ProjectTypeShape as PublicTypeShape
import com.felipebz.zpa.project.ProjectCallArguments as InternalCallArguments

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
    /** For a [Terminal.PROJECT] evidence, the project's description it classifies; for a package cursor's row, its `%ROWTYPE`'s. */
    val description: ProjectExternalTypeDescription? = null,
    /** For an unknown evidence, the incomplete lookup that stopped the walk, when one did. */
    val incomplete: LocalResolution.Incomplete? = null,
    /** For a [Terminal.BUILT_IN] evidence, the written type the walk ended at; null for an index typed by its control. */
    val terminalUse: LocalTypeUse? = null,
    /**
     * The first written type along the walk that writes constraints (`s_vc(20)` of a variable of the unconstrained subtype
     * `s_vc`): Oracle applies the constraints written at the nearest use, those of a constrained subtype included.
     */
    val constrainedUse: LocalTypeUse? = null,
    /** For a [Terminal.COMPOSITE] row of a package cursor of another file, the select list its fields follow. */
    val selectList: SelectList? = null
) {
    enum class Terminal {
        /** A predefined type name, written or reached through local subtypes. */
        BUILT_IN,

        /** A local record, collection or ref cursor type, a `%ROWTYPE` of a local cursor, a package cursor's row, or `SELF`. */
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
            else -> LocalTypeEvidence(ProjectArgumentType.builtIn(literal.typeName, literal.rank), Terminal.LITERAL, emptyList(), null)
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
        LocalReferenceOutcomes(model, snapshot?.let { LocalSpecificationCoverage.of(model, it) }),
    /** For a dynamic SQL text, the compilation schema of the unit that runs it, for every region of the text. */
    private val heldSchema: HeldSchema? = null
) {
    class HeldSchema(val schema: ProjectIdentifier?)

    /** A walk over [text], a dynamic SQL text that the unit holding [region] of this walk's file runs. */
    fun forText(text: LocalSemanticModel, region: LocalRegionId): LocalTypeWalk =
        LocalTypeWalk(text, snapshot, external, LocalReferenceOutcomes(text, null), HeldSchema(compilationSchema(region)))

    private val metadata = external.metadata

    /** The project as seen from the unit holding [region]: its compilation schema first, and the run's synonyms. */
    fun projectView(region: LocalRegionId): ProjectSnapshot? = snapshot?.preferring(compilationSchema(region), metadata)

    /**
     * The schema the file-level unit holding [region] compiles in, from the run's external context: its written schema,
     * else its file's configured schema path, else the configured default. Code outside a named unit (an anonymous
     * block of a script) runs in the session's schema, which the configuration gives for its file. Null when nothing
     * establishes it: the project then keeps the uncertainty. A dynamic SQL text compiles in the schema of the unit
     * that runs it.
     */
    fun compilationSchema(region: LocalRegionId): ProjectIdentifier? {
        heldSchema?.let { return it.schema }
        val fileSchema = { external.fileSchema(model.fileId.value).orElse(null) }
        val top = LocalResolver.chain(model, region).firstOrNull { it.parent == model.root.id } ?: return fileSchema()
        val name = model.declarationsOf(model.root.id).firstNotNullOfOrNull { declaration ->
            when (val details = declaration.details) {
                is LocalUnit -> details.unitName.takeIf { details.region == top.id }
                is LocalSubprogram -> details.unitName.takeIf { details.body == top.id }
                is LocalTrigger -> details.unitName.takeIf { details.body == top.id }
                else -> null
            }
        } ?: return fileSchema()
        return external.compilationSchema(ProjectQualifiedName.wrap(name), model.fileId.value).orElse(null)
    }

    /**
     * The cursor of a package specification in another project file that [name] denotes from [region] (`pk.c`, or `c`
     * in the body of `pk`), as the project reads the name there; null when the project names no single cursor.
     */
    fun projectCursor(name: QualifiedName, region: LocalRegionId): ProjectPackageCursorView? =
        packageMember(name, region)?.takeIf { it.status != ProjectValueResolution.Status.INCOMPLETE_INDEX }?.candidates?.singleOrNull() as? ProjectPackageCursorView

    /** What the prepared project reads [name] as from [region], among package values and cursors; null when not prepared. */
    fun packageMember(name: QualifiedName, region: LocalRegionId): ProjectValueResolution? =
        if (!prepared) null else projectView(region)?.resolvePackageValue(ProjectQualifiedName.wrap(name), packageOwner(region))

    /**
     * Whether a package variable or constant takes [segments] read as a PL/SQL name from [region] (`pk.v`, `pk.v.f`,
     * `schema.pk.v`): true when the project knows one; false only for `q.v` when the project proves that no object is
     * named `q` or that `q` is a package of the project whose members named `v` are all subprograms; null otherwise.
     */
    fun packageValueTakes(segments: List<OracleIdentifier>, region: LocalRegionId): Boolean? {
        if (!prepared) return null
        val view = projectView(region) ?: return null
        for (size in 2..minOf(3, segments.size)) {
            val status = view.resolvePackageValue(ProjectQualifiedName.wrap(QualifiedName(segments.take(size))), null).status
            if (status == ProjectValueResolution.Status.RESOLVED || status == ProjectValueResolution.Status.AMBIGUOUS) return true
        }
        if (segments.size != 2) return null
        return false.takeIf { view.provesNoPackageValue(segments[0], segments[1], compilationSchema(region), metadata) }
    }

    /** Whether the project and the metadata prove that no schema object is named [name] from [region]. */
    fun provesNoObjectNamed(name: OracleIdentifier, region: LocalRegionId): Boolean =
        prepared && projectView(region)?.provesNoObjectNamed(name, compilationSchema(region), metadata) == true

    /**
     * The select list the fields of the row [evidence] gives follow: a package cursor's, a local cursor's (its
     * `%ROWTYPE`), or a cursor `FOR` loop's query. Null for any other value, and for a query whose row is a relation's
     * columns (`SELECT *` from one relation).
     */
    fun selectListOf(evidence: LocalTypeEvidence): SelectList? {
        if (evidence.terminal != LocalTypeEvidence.Terminal.COMPOSITE) return null
        evidence.selectList?.let { return it }
        val last = evidence.followed.lastOrNull()?.let(model::declaration) ?: return null
        val row = when (last.kind) {
            CURSOR -> (last.details as? LocalCursor)?.row
            LOOP_INDEX -> (last.details as? LocalLoopIndex)?.control?.takeIf { it.form == LocalLoopControl.Form.QUERY }?.row
            else -> null
        }
        return when (row) {
            is LocalQueryRow.OwnColumns -> row.selectList
            is LocalQueryRow.Mixed -> row.selectList
            else -> null
        }
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
     * Evidence of the qualified name [name] whose first segment the file does not declare: the package value the project
     * describes for the whole name, else a member chain after the one prefix of it that names such a value (`p.g.n`
     * is field `n` of `p.g`). Two prefixes that both name a value leave the whole name's evidence.
     */
    fun projectChain(name: QualifiedName, region: LocalRegionId): LocalTypeEvidence {
        val whole = projectValue(name, region)
        if (!stoppedBeforeValue(whole)) return whole
        val readings = (name.segments.size - 1 downTo 1).mapNotNull { size ->
            projectValue(QualifiedName(name.segments.take(size)), region).takeUnless { stoppedBeforeValue(it) || it.description?.source?.values.isNullOrEmpty() }
                ?.let { size to it }
        }
        val (size, value) = readings.singleOrNull() ?: return whole
        return members(value, name.segments.drop(size), region)
    }

    /**
     * The project names no value for the whole name: none found, or one whose fields the name goes into. A field path the
     * project walked counts too: the member chain keeps the reasons each member gives, conditional fields included.
     */
    private fun stoppedBeforeValue(evidence: LocalTypeEvidence): Boolean {
        val source = evidence.description?.source ?: return false
        val stop = source.anchorStop.orElse(null)
        return source.status == ProjectTypeDescription.Status.UNRESOLVED_VALUE || source.fieldPath.isNotEmpty() ||
            stop == ProjectTypeDescription.AnchorStop.FIELD_PATH || stop == ProjectTypeDescription.AnchorStop.FIELD_NOT_FOUND ||
            source.steps.any { it is ProjectShapedTypeView }
    }

    /**
     * Evidence of `SELF` in the body of an object type, for the members its specification or supertypes in other files
     * hold: the type the project describes under the body's name. Null when [region] is in no type body.
     */
    fun enclosingType(region: LocalRegionId): LocalTypeEvidence? {
        val body = LocalResolver.chain(model, region).firstOrNull { it.kind == LocalRegionKind.TYPE_BODY } ?: return null
        val unit = LocalTypeScope.unitOf(model, body.id) ?: return null
        val snapshot = snapshot?.takeIf { prepared } ?: return LocalTypeEvidence.unknown(NO_PREPARED_PROJECT)
        val described = snapshot.describeExternalType(ProjectQualifiedName.wrap(unit.unitName), null, metadata, compilationSchema(region))
        return LocalTypeEvidence(ProjectArgumentType.described(described, null), LocalTypeEvidence.Terminal.PROJECT, emptyList(), null, described)
    }

    /**
     * [members], one step after another, from the value [evidence] gives, the last of them possibly the collection
     * method COUNT of a collection value, a PLS_INTEGER (it binds a PLS_INTEGER formal over NUMBER and VARCHAR2, for
     * nested tables, varrays and associative arrays alike). A collection has no attribute, so COUNT
     * after one is always the method.
     */
    fun memberPath(evidence: LocalTypeEvidence, members: List<OracleIdentifier>, region: LocalRegionId): LocalTypeEvidence {
        val last = members.lastOrNull() ?: return evidence
        val base = members(evidence, members.dropLast(1), region)
        if (last.lookupName == "COUNT" && isCollection(base)) {
            return LocalTypeEvidence(ProjectArgumentType.builtIn("PLS_INTEGER"), LocalTypeEvidence.Terminal.BUILT_IN, emptyList(), null)
        }
        return members(base, listOf(last), region)
    }

    /** Whether [evidence] is a value of a local collection type or of a collection type the project describes. */
    private fun isCollection(evidence: LocalTypeEvidence): Boolean = when (evidence.terminal) {
        LocalTypeEvidence.Terminal.COMPOSITE -> evidence.followed.lastOrNull()?.let(model::declaration)?.kind == COLLECTION_TYPE
        LocalTypeEvidence.Terminal.PROJECT -> evidence.description?.takeIf { !it.anchor.isPresent && it.source.status == ProjectTypeDescription.Status.DECLARED_TYPE }
            ?.source?.declaration?.orElse(null)?.let { (it as? ProjectShapedTypeView)?.shape?.orElse(null) == PublicTypeShape.COLLECTION } == true
        else -> false
    }

    /**
     * A value of the type [name] names, when the project describes it as a type created with CREATE TYPE; null for
     * anything else (a package type, an unresolved name, no prepared project).
     */
    fun schemaTypeValue(name: QualifiedName, region: LocalRegionId): LocalTypeEvidence? {
        val snapshot = snapshot?.takeIf { prepared } ?: return null
        val described = snapshot.describeExternalType(ProjectQualifiedName.wrap(name), null, metadata, compilationSchema(region))
        if (DatatypeFamilies.compositeKind(described) != CompositeKind.SCHEMA_OBJECT) return null
        return LocalTypeEvidence(ProjectArgumentType.described(described, null), LocalTypeEvidence.Terminal.PROJECT, emptyList(), null, described)
    }

    /** [members], one step after another, from the value [evidence] gives. */
    fun members(evidence: LocalTypeEvidence, members: List<OracleIdentifier>, region: LocalRegionId): LocalTypeEvidence =
        // An undecided value keeps its reasons: its members are no more decided.
        members.fold(evidence) { current, member -> if (current.terminal == LocalTypeEvidence.Terminal.UNKNOWN) current else member(current, member, region) }

    /**
     * Evidence of the member [name] of a value whose type [evidence] gives, when the project describes that type: a
     * field of a record type or an attribute of an object type ([ProjectSnapshot.memberType]), typed as the project
     * describes the member's written type; or a column of a `%ROWTYPE` of a relation the external metadata confirms,
     * typed as the column's `%TYPE`. Anything else (a method, no such member, a local type, no prepared project) is
     * unknown.
     */
    fun member(evidence: LocalTypeEvidence, name: OracleIdentifier, region: LocalRegionId): LocalTypeEvidence {
        localRecord(evidence)?.let { record ->
            val fields = (record.details as LocalRecordType).fields
            val ordinal = fields.indexOfFirst { it.name == name }.takeIf { it >= 0 && fields.count { field -> field.name == name } == 1 }
                ?: return LocalTypeEvidence.unknown("$name is not one field of ${record.name}")
            return field(record.id, ordinal)
        }
        val snapshot = snapshot?.takeIf { prepared } ?: return LocalTypeEvidence.unknown(NO_PREPARED_PROJECT)
        val description = evidence.description?.takeIf { evidence.terminal == LocalTypeEvidence.Terminal.PROJECT }
            ?: return LocalTypeEvidence.unknown("member $name of a value the project does not describe")
        if (description.anchor.orElse(null) == ProjectTypeReference.Anchor.ROWTYPE) {
            val relation = description.anchorName.orElse(null)?.takeIf { description.status == ProjectExternalTypeDescription.Status.RESOLVED }
                ?: return LocalTypeEvidence.unknown("member $name of a row the external metadata does not confirm")
            val owner = packageOwner(region)
            val column = snapshot.describeExternalType(
                ProjectWrittenAnchor.of(ProjectQualifiedName.wrap(relation.name.append(name)), ProjectTypeReference.Anchor.TYPE),
                owner, metadata, compilationSchema(region)
            )
            return LocalTypeEvidence(ProjectArgumentType.described(column, owner), LocalTypeEvidence.Terminal.PROJECT, emptyList(), null, column)
        }
        val type = description.source.declaration.orElse(null)?.takeIf { description.source.status == ProjectTypeDescription.Status.DECLARED_TYPE }
            ?: return LocalTypeEvidence.unknown("member $name of a value of no declared project type")
        val member = snapshot.memberType(type.id, name) ?: return LocalTypeEvidence.unknown("$name is not one field or attribute of ${type.qualifiedName}")
        return described(snapshot, member, region)
    }

    /**
     * What the member [name] of the value [evidence] gives is, when the project describes the value's type as a declared
     * type (not an anchor): a field or attribute with the type declaring it, a method set, a collection method, or why
     * there is none. Null for any other value: a local type, an anchor, an undecided value, no prepared project.
     */
    fun projectMember(evidence: LocalTypeEvidence, name: OracleIdentifier): ProjectRecordMemberResolution? {
        val snapshot = snapshot?.takeIf { prepared } ?: return null
        val description = evidence.description?.takeIf { evidence.terminal == LocalTypeEvidence.Terminal.PROJECT && !it.anchor.isPresent } ?: return null
        val type = description.source.declaration.orElse(null)?.takeIf { description.source.status == ProjectTypeDescription.Status.DECLARED_TYPE } ?: return null
        return snapshot.memberResolution(type.id, name)
    }

    private fun described(snapshot: ProjectSnapshot, member: ProjectMemberType, region: LocalRegionId): LocalTypeEvidence {
        val schema = compilationSchema(region)
        val typeRef = member.typeRef
        val described = if (typeRef != null) snapshot.describeExternalType(typeRef, member.owner, metadata, schema)
        else snapshot.describeExternalType(member.typeName ?: return LocalTypeEvidence.unknown("no member type"), null, metadata, schema)
        return describedResult(described, member.owner, typeRef, inSpecification = true)
    }

    /**
     * A described type, or, when the project gives it no family, the composite an undeclared one-segment type name
     * stands for: SYS_REFCURSOR is a ref cursor and any other name that is not predefined a schema object, unless the
     * package [owner] declares the name. [inSpecification] is false for a package body's private declaration, whose
     * own declarations are not covered.
     */
    private fun describedResult(
        described: ProjectExternalTypeDescription,
        owner: ProjectQualifiedName?,
        typeRef: ProjectTypeReference?,
        inSpecification: Boolean
    ): LocalTypeEvidence {
        val argumentType = ProjectArgumentType.described(described, owner)
        val name = typeRef?.takeIf { argumentType.familyOrNull == null && inSpecification && it.kind == ProjectTypeReference.Kind.NAMED && it.writtenIdentity == null }
            ?.name?.name?.segments?.singleOrNull()
        val kind = name?.let { DatatypeFamilies.undeclaredTypeKind(it) }
        if (kind != null && (owner == null || !shadowed(owner.name, name))) {
            return LocalTypeEvidence(ProjectArgumentType.composite(kind), LocalTypeEvidence.Terminal.PROJECT, emptyList(), null, described)
        }
        return LocalTypeEvidence(argumentType, LocalTypeEvidence.Terminal.PROJECT, emptyList(), null, described)
    }

    /** Whether a package member may take a predefined type name, answered once per package and name. */
    private val shadowed: (QualifiedName, OracleIdentifier) -> Boolean by lazy(LazyThreadSafetyMode.NONE) {
        snapshot?.let { ProjectCallNarrower.shadowCheck(it) } ?: { _, _ -> true }
    }

    /** The local record type [evidence] ends at, or null. */
    private fun localRecord(evidence: LocalTypeEvidence): LocalDeclaration? =
        evidence.followed.lastOrNull()?.let(model::declaration)
            ?.takeIf { evidence.terminal == LocalTypeEvidence.Terminal.COMPOSITE && it.kind == RECORD_TYPE }

    /**
     * Evidence of an element of the collection value [evidence] gives, indexed once (`v(i)`): the element type of a
     * local collection type, or the project's description of a project collection type's element. Anything else is
     * unknown; an undecided value stays so.
     */
    fun element(evidence: LocalTypeEvidence, region: LocalRegionId): LocalTypeEvidence {
        if (evidence.terminal == LocalTypeEvidence.Terminal.UNKNOWN) return evidence
        val local = evidence.followed.lastOrNull()?.let(model::declaration)
        if (evidence.terminal == LocalTypeEvidence.Terminal.COMPOSITE) {
            return local?.takeIf { it.kind == COLLECTION_TYPE }?.let { written(it.type) } ?: LocalTypeEvidence.unknown("an element of a value that is not a collection")
        }
        val description = evidence.description?.takeIf { evidence.terminal == LocalTypeEvidence.Terminal.PROJECT && it.source.status == ProjectTypeDescription.Status.DECLARED_TYPE }
            ?: return LocalTypeEvidence.unknown("an element of a value of no declared type")
        val collection = (description.source.declaration.orElse(null) as? ProjectShapedTypeView)?.takeIf { it.shape.orElse(null) == PublicTypeShape.COLLECTION }
            ?: return LocalTypeEvidence.unknown("an element of a value that is not a collection")
        val element = projectElement(collection, region) ?: return LocalTypeEvidence.unknown("no element type")
        val owner = (collection as? ProjectPackageTypeView)?.owner
        return LocalTypeEvidence(ProjectArgumentType.described(element, owner), LocalTypeEvidence.Terminal.PROJECT, emptyList(), null, element)
    }

    /**
     * Evidence of the result of calling the method [name] with [arguments] on the value [evidence] gives, a value of an
     * object type the project describes ([ProjectSnapshot.methodResultType]), several applicable methods narrowed by
     * [argumentTypes] (null when the actuals' types are not known). Anything else is unknown.
     */
    fun methodResult(
        evidence: LocalTypeEvidence,
        name: OracleIdentifier,
        arguments: InternalCallArguments,
        argumentTypes: List<ProjectArgumentType>?,
        region: LocalRegionId
    ): LocalTypeEvidence {
        if (evidence.terminal == LocalTypeEvidence.Terminal.UNKNOWN) return evidence
        val snapshot = snapshot?.takeIf { prepared } ?: return LocalTypeEvidence.unknown(NO_PREPARED_PROJECT)
        val type = evidence.description?.source?.takeIf { evidence.terminal == LocalTypeEvidence.Terminal.PROJECT && it.status == ProjectTypeDescription.Status.DECLARED_TYPE }
            ?.declaration?.orElse(null) ?: return LocalTypeEvidence.unknown("method $name of a value of no declared project type")
        val result = snapshot.methodResultType(type.id, name, arguments, argumentTypes, metadata, compilationSchema(region))
            ?: return LocalTypeEvidence.unknown("$name binds no single function of ${type.qualifiedName}")
        return described(snapshot, result, region)
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

    /** The project's fact for the local [declaration], the one with the same range in this file; null when none. */
    fun projectDeclarationOf(declaration: LocalDeclarationId): ProjectDeclarationView? {
        if (!prepared) return null
        val range = model.declaration(declaration).range
        return snapshot!!.declarationsFor(model.fileId.value).declarations.singleOrNull {
            val r = it.sourceRange
            r.startLine == range.startLine && r.startColumn == range.startColumn && r.endLine == range.endLine && r.endColumn == range.endColumn
        }
    }

    /**
     * Evidence of the result of a call of [name] that nothing local takes, when the project describes [name] as an object
     * type: its constructor, user-defined or the default one, builds a value of that type. Null otherwise.
     */
    fun constructorResult(name: QualifiedName, region: LocalRegionId): LocalTypeEvidence? {
        val snapshot = snapshot?.takeIf { prepared } ?: return null
        val described = snapshot.describeExternalType(ProjectQualifiedName.wrap(name), packageOwner(region), metadata, compilationSchema(region))
        val type = described.source.declaration.orElse(null) as? ProjectStandaloneTypeView ?: return null
        if (described.source.status != ProjectTypeDescription.Status.DECLARED_TYPE || type.shape.orElse(null) != PublicTypeShape.OBJECT) return null
        return LocalTypeEvidence(ProjectArgumentType.described(described, null), LocalTypeEvidence.Terminal.PROJECT, emptyList(), null, described)
    }

    /** A predefined type written as [typeRef], with its exact type read from the whole written type. */
    private fun builtIn(typeRef: NamedTypeRef): ProjectArgumentType =
        ProjectArgumentType.builtIn(typeRef.name.last.originalSpelling, OverloadRank.of(typeRef.name, typeRef.headerTypeIdentity))

    /**
     * Evidence of a type written inside SQL: a predefined type name, else a schema type only. SQL sees neither a local
     * nor a package type (ORA-00902), and a local declaration does not hide a schema type there.
     */
    fun schemaType(use: LocalTypeUse): LocalTypeEvidence {
        val typeRef = use.typeRef as? NamedTypeRef ?: return LocalTypeEvidence.unknown("not a named type in SQL")
        val name = typeRef.name
        if (name.segments.size == 1 && DatatypeFamilies.ofTypeName(name.last.originalSpelling) != null) {
            return LocalTypeEvidence(builtIn(typeRef), LocalTypeEvidence.Terminal.BUILT_IN, emptyList(), null)
        }
        val snapshot = snapshot?.takeIf { prepared } ?: return LocalTypeEvidence.unknown(NO_PREPARED_PROJECT)
        val written = ProjectQualifiedName.wrap(name)
        val resolution = snapshot.preferring(compilationSchema(use.lookupRegion), metadata).resolveNamedType(written, null)
        if (resolution.status == ProjectNamedTypeResolution.Status.RESOLVED && resolution.declaration.get() !is ProjectStandaloneTypeView) {
            return LocalTypeEvidence.unknown("SQL does not see the package type ${resolution.declaration.get().qualifiedName}")
        }
        val described = snapshot.describeExternalType(written, null, metadata, compilationSchema(use.lookupRegion))
        return LocalTypeEvidence(ProjectArgumentType.described(described, null), LocalTypeEvidence.Terminal.PROJECT, emptyList(), null, described)
    }

    /** Evidence of a written type at its own lookup site, such as a formal parameter's. */
    fun written(use: LocalTypeUse?): LocalTypeEvidence = use?.let { Walk().type(it) } ?: LocalTypeEvidence.unknown(NO_WRITTEN_TYPE)

    /** Evidence of the result of the local function [function], a call resolved to that entity. */
    fun result(function: LocalDeclarationId): LocalTypeEvidence = Walk().result(model.declaration(function), 0)

    /**
     * Evidence of a project call's result: the RETURN type of the function a RESOLVED call selected, or the one every
     * candidate of an AMBIGUOUS call shares.
     */
    fun projectResult(typed: ProjectTypedCallResolution?, region: LocalRegionId): LocalTypeEvidence {
        if (typed == null) return LocalTypeEvidence.unknown("not forwarded")
        val function = when (typed.status) {
            ProjectTypedCallResolution.Status.RESOLVED -> typed.selected.get().declaration as? ProjectFunctionView ?: return LocalTypeEvidence.unknown("not a function")
            ProjectTypedCallResolution.Status.AMBIGUOUS -> sameReturn(typed) ?: return LocalTypeEvidence.unknown("the call is AMBIGUOUS")
            else -> return LocalTypeEvidence.unknown("the call is ${typed.status}")
        }
        val owner = (function as? ProjectPackageSubprogramView)?.packageOwner
        val described = snapshot!!.describeExternalType(function.returnType, owner, metadata, compilationSchema(region))
        return describedResult(described, owner, function.returnType, function.role != ProjectDeclarationRole.BODY)
    }

    /**
     * A function standing for every candidate an AMBIGUOUS call retains, when they are all functions of one package (or
     * all standalone) declaring the same RETURN type: whichever Oracle binds, the result has that type. When Oracle cannot
     * pick one either, it raises PLS-00307 at the call, so no compiled call gets another type.
     */
    private fun sameReturn(typed: ProjectTypedCallResolution): ProjectFunctionView? {
        val functions = typed.evaluations.filter { it.outcome == ProjectCandidateCompatibility.Outcome.RETAINED }
            .map { it.candidate.declaration as? ProjectFunctionView ?: return null }
        val first = functions.firstOrNull() ?: return null
        val owner = (first as? ProjectPackageSubprogramView)?.packageOwner
        return first.takeIf {
            functions.all { f ->
                (f as? ProjectPackageSubprogramView)?.packageOwner == owner && f.role == first.role &&
                    f.returnType.kind == first.returnType.kind && f.returnType.anchor == first.returnType.anchor &&
                    f.returnType.name == first.returnType.name && f.returnType.writtenIdentity == first.returnType.writtenIdentity
            }
        }
    }

    private fun composite(followed: List<LocalDeclarationId>, kind: CompositeKind? = null) =
        LocalTypeEvidence(kind?.let { ProjectArgumentType.composite(it) } ?: ProjectArgumentType.composite(), LocalTypeEvidence.Terminal.COMPOSITE, followed, null)

    /** A specification type the project does not match by range keeps no declared type: the project may still hold it. */
    private fun declaredComposite(type: LocalDeclaration, followed: List<LocalDeclarationId>): LocalTypeEvidence {
        val project = projectDeclarationOf(type.id)
        val declared = when {
            project != null -> (project as? ProjectPackageTypeView)?.let { DeclaredCompositeType.Project(it.id) }
            model.region(type.region).kind == LocalRegionKind.PACKAGE_SPEC -> null
            else -> DeclaredCompositeType.Local(model.fileId.value, type.id.ordinal)
        }
        val argumentType = declared?.let { ProjectArgumentType.composite(it) } ?: ProjectArgumentType.composite()
        return LocalTypeEvidence(argumentType, LocalTypeEvidence.Terminal.COMPOSITE, followed, null)
    }

    /** A decision made with the project's specification coverage is still the body's: its declaration is local. */
    private fun covered(outcome: LocalResolution) = (outcome as? LocalResolution.SpecificationCovered)?.outcome ?: outcome

    /** [segments], a member of the specification of the package body holding [region], named through that package. */
    fun specificationMember(segments: List<OracleIdentifier>, region: LocalRegionId): QualifiedName? {
        val body = LocalResolver.chain(model, region).firstOrNull { it.kind == LocalRegionKind.PACKAGE_BODY } ?: return null
        val unit = LocalTypeScope.unitOf(model, body.id) ?: return null
        return QualifiedName(unit.unitName.segments + segments)
    }

    /**
     * Whether [name], written in [region], denotes no package cursor: a one-segment name outside a package never does;
     * otherwise the project, read from the unit's schema as Oracle reads the name, must know no package value nor
     * cursor of that name, with a complete index.
     */
    fun namesNoPackageCursor(name: ProjectQualifiedName, region: LocalRegionId): Boolean {
        val owner = packageOwner(region)
        if (name.segments.size == 1 && owner == null) return true
        val resolution = projectView(region)?.resolvePackageValue(name, owner) ?: return false
        return resolution.status == ProjectValueResolution.Status.NOT_FOUND_IN_PROJECT && resolution.candidates.none { it is ProjectPackageCursorView }
    }

    /**
     * The value of the constant [name] (a `VARIABLE_NAME` or `MEMBER_EXPRESSION` written in [region]): a local constant
     * whose default, given by [defaultOf], evaluates in turn, or a package constant of another file the project holds
     * with an integer literal default. Null for anything else, and for a constant met again through [active].
     */
    fun constantValue(name: AstNode, region: LocalRegionId, defaultOf: (LocalDeclaration) -> AstNode?, active: MutableSet<LocalDeclarationId>): BigDecimal? {
        val identifiers = when (name.type) {
            PlSqlGrammar.VARIABLE_NAME -> listOf(name.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME) ?: return null)
            PlSqlGrammar.MEMBER_EXPRESSION -> {
                if (name.hasDirectChildren(PlSqlPunctuator.MOD, PlSqlPunctuator.REMOTE)) return null
                val first = name.firstChild.takeIf { it.type === PlSqlGrammar.VARIABLE_NAME }?.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME) ?: return null
                listOf(first) + name.getChildren(PlSqlGrammar.IDENTIFIER_NAME)
            }
            else -> return null
        }
        val segments = identifiers.map(LocalSyntax::identifier)
        val reference = LocalReference(
            segments.first(), LocalSyntax.range(model.fileId, name),
            if (segments.size == 1) LocalReferenceContext.VALUE else LocalReferenceContext.QUALIFIER, region,
            identifiers.drop(1).map { LocalSegment(LocalSyntax.identifier(it), false, LocalSyntax.range(model.fileId, it)) },
            LocalReferenceContext.VALUE, false, null
        )
        val steps = outcomes.steps(reference)
        val written = QualifiedName(segments)
        if (LocalReferenceOutcomes.isAbsent(steps.first())) return projectConstant(written, region)
        val member = steps.indexOfFirst(LocalReferenceOutcomes::isSpecificationMember)
        if (member >= 0) return (if (member == 0) specificationMember(segments, region) else written)?.let { projectConstant(it, region) }
        if (steps.size != segments.size) return null
        val declaration = (covered(steps.last()) as? LocalResolution.Bound)?.let { model.declaration(it.declaration) } ?: return null
        if (declaration.kind != VARIABLE || !declaration.isConstant || !active.add(declaration.id)) return null
        return try {
            StaticArithmetic.evaluate(defaultOf(declaration)) { constantValue(it, declaration.region, defaultOf, active) }
        } finally {
            active.remove(declaration.id)
        }
    }

    private fun projectConstant(name: QualifiedName, region: LocalRegionId): BigDecimal? {
        val snapshot = projectView(region)?.takeIf { prepared } ?: return null
        val resolution = snapshot.resolvePackageValue(ProjectQualifiedName.wrap(name), packageOwner(region))
        val value = resolution.value.orElse(null)?.takeIf { resolution.status == ProjectValueResolution.Status.RESOLVED } ?: return null
        val fact = snapshot.occurrenceOf(value.id)?.declaration as? PackageValueDeclaration ?: return null
        return fact.staticDefault?.takeIf { fact.constant }?.toBigDecimal()
    }

    /**
     * The type reference the project retains for the declaration of this file whose written type starts at [line],
     * [column]: a package specification's variable, constant, formal, record field or RETURN type, a cursor's RETURN
     * type, a subtype's or collection's type, or a standalone unit's formal, RETURN type or attribute.
     */
    fun retainedReference(line: Int, column: Int): ProjectTypeReference? = retained[line to column]

    private val retained: Map<Pair<Int, Int>, ProjectTypeReference> by lazy(LazyThreadSafetyMode.NONE) {
        val snapshot = snapshot?.takeIf { prepared } ?: return@lazy emptyMap()
        snapshot.declarationsFor(model.fileId.value).declarations.flatMap { declaration ->
            when (declaration) {
                is ProjectPackageValueView -> listOf(declaration.typeRef)
                is ProjectFunctionView -> declaration.parameters.map { it.typeRef } + declaration.returnType
                is ProjectSubprogramView -> declaration.parameters.map { it.typeRef }
                is ProjectPackageCursorView -> declaration.parameters.map { it.typeRef } + listOfNotNull(declaration.returnType.orElse(null))
                is ProjectPackageTypeView -> declaration.recordFields.map { it.typeRef } + listOfNotNull(declaration.elementType.orElse(null))
                is ProjectStandaloneTypeView -> declaration.attributes.map { it.typeRef } + listOfNotNull(declaration.elementType.orElse(null))
                is ProjectSubtypeView -> listOf(declaration.baseType)
                else -> emptyList()
            }
        }.associateBy { it.sourceRange.startLine to it.sourceRange.startColumn }
    }

    /**
     * The project's reading of the relation [name] written in a SQL statement at [region]: `name%ROWTYPE`, through its
     * synonyms and the run's external metadata. A SQL statement sees no PL/SQL declaration, so no package owner applies.
     */
    fun relationRow(name: QualifiedName, region: LocalRegionId): ProjectExternalTypeDescription? = snapshot?.describeExternalType(
        ProjectWrittenAnchor.of(ProjectQualifiedName.wrap(name), ProjectTypeReference.Anchor.ROWTYPE), null, metadata, compilationSchema(region)
    )

    /**
     * Whether the relation [description] found has [column], and that column: true or false when the metadata covers the
     * relation's columns, null otherwise.
     */
    fun relationColumn(description: ProjectExternalTypeDescription?, column: OracleIdentifier): Pair<Boolean?, ProjectExternalColumn?> {
        val relation = description?.takeIf { it.status == ProjectExternalTypeDescription.Status.RESOLVED }?.lookup?.orElse(null)?.relation?.orElse(null)
            ?: return null to null
        val lookup = metadata.findColumn(relation.schema, relation.name, ProjectIdentifier.wrap(column))
        return when (lookup.status) {
            ProjectExternalLookup.Status.FOUND -> true to lookup.column.orElse(null)
            ProjectExternalLookup.Status.COLUMN_NOT_FOUND -> false to null
            else -> null to null
        }
    }

    /** The relation [description] found, when the metadata covers all of its columns. */
    fun coveredRelation(description: ProjectExternalTypeDescription?): ProjectExternalRelation? =
        description?.takeIf { it.status == ProjectExternalTypeDescription.Status.RESOLVED }?.lookup?.orElse(null)?.relation?.orElse(null)
            ?.takeIf { it.isColumnsComplete }

    /** The package whose specification or body holds [region], as an owner for the project's name lookups. */
    fun packageOwner(region: LocalRegionId): ProjectQualifiedName? =
        LocalResolver.chain(model, region).firstOrNull { it.kind == LocalRegionKind.PACKAGE_SPEC || it.kind == LocalRegionKind.PACKAGE_BODY }
            ?.let { LocalTypeScope.unitOf(model, it.id) }?.let { ProjectQualifiedName.wrap(it.unitName) }

    private inner class Walk {
        private val followed = ArrayList<LocalDeclarationId>()
        private val fields = HashSet<Pair<LocalDeclarationId, Int>>()
        private var constrained: LocalTypeUse? = null

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
            LocalLoopControl.Form.NAME -> cursorLoop(declaration, control)
            null -> unknown("typed by its iteration control")
        }

        /**
         * The index of a loop over a named cursor: the row of its `RETURN` type when it has one, else of its query. The
         * control is written before the index is declared: it is looked up where the loop is written.
         */
        private fun cursorLoop(declaration: LocalDeclaration, control: LocalLoopControl): LocalTypeEvidence {
            val enclosing = model.region(declaration.region).parent ?: return unknown("no enclosing region")
            val qualified = control.segments.isNotEmpty()
            val reference = LocalReference(control.name!!, control.nameRange!!, if (qualified) LocalReferenceContext.QUALIFIER else LocalReferenceContext.FOR_CONTROL,
                enclosing, control.segments, LocalReferenceContext.FOR_CONTROL, false, null)
            val steps = outcomes.steps(reference)
            val name = QualifiedName(listOf(control.name) + control.segments.map { it.name })
            if (qualified && LocalReferenceOutcomes.isAbsent(steps.first())) {
                return projectCursor(name, enclosing)?.let { cursorRow(it, true, enclosing) } ?: unknown("not a cursor the project holds")
            }
            val member = steps.indexOfFirst(LocalReferenceOutcomes::isSpecificationMember)
            if (member >= 0) {
                // A cursor of the specification in another file.
                val full = if (member == 0) specificationMember(name.segments, enclosing) else name
                full?.let { projectCursor(it, enclosing) }?.let { return cursorRow(it, true, enclosing) }
                val row = full?.takeIf { !qualified }?.let { project(it, TypeAnchor.ROWTYPE, enclosing) }
                return if (row?.argumentType?.familyOrNull == ProjectDatatypeFamily.COMPOSITE) row else unknown("a specification name the project does not describe as a row")
            }
            val last = covered(steps.last())
            val cursor = (last as? LocalResolution.Bound)?.takeIf { steps.size == control.segments.size + 1 }?.let { model.declaration(it.declaration) }
            if (cursor?.kind != CURSOR) return unknown("iterates over a value")
            followed += cursor.id
            return (cursor.details as? LocalCursor)?.returnType?.let(::type) ?: composite(followed.toList())
        }

        /**
         * The row of the project's [cursor]. A loop's index has its `RETURN` type's row when it has one; a `%ROWTYPE`
         * has its query's row when the declaration writes a query, a `RETURN` type notwithstanding, else the `RETURN`
         * type's. [anchored] is the project's description of the written `%ROWTYPE`, kept with a query's row.
         */
        fun cursorRow(cursor: ProjectPackageCursorView, loop: Boolean, region: LocalRegionId, anchored: ProjectExternalTypeDescription? = null): LocalTypeEvidence {
            val snapshot = snapshot ?: return unknown(NO_PREPARED_PROJECT)
            val fact = snapshot.occurrenceOf(cursor.id)?.declaration as? PackageCursorDeclaration ?: return unknown("no cursor fact")
            val returnType = cursor.returnType.orElse(null)
            if (returnType != null && (loop || fact.selectList == null)) {
                val described = snapshot.describeExternalType(returnType, cursor.owner, metadata, compilationSchema(region))
                return LocalTypeEvidence(ProjectArgumentType.described(described, cursor.owner), LocalTypeEvidence.Terminal.PROJECT, followed.toList(), null, described)
            }
            return LocalTypeEvidence(ProjectArgumentType.composite(), LocalTypeEvidence.Terminal.COMPOSITE, followed.toList(), null, anchored, selectList = fact.selectList)
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
            if (constrained == null && (typeRef as? NamedTypeRef)?.writtenSyntax?.groups?.isNotEmpty() == true) constrained = use
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
                RECORD_TYPE, COLLECTION_TYPE -> { followed += target.id; declaredComposite(target, followed.toList()) }
                REF_CURSOR_TYPE -> { followed += target.id; composite(followed.toList(), CompositeKind.REF_CURSOR) }
                else -> unknown("${target.kind} ${target.name} is not a type")
            }
            typeRef is AnchoredTypeRef && typeRef.anchor == TypeAnchor.TYPE -> value(target)
            typeRef is AnchoredTypeRef && target.kind == CURSOR -> {
                followed += target.id
                // A forward declaration with a RETURN type writes no query: Oracle gives its %ROWTYPE the RETURN type's row.
                val cursor = target.details as? LocalCursor
                cursor?.returnType?.takeIf { cursor.isForward }?.let(::type) ?: composite(followed.toList())
            }
            else -> unknown("${target.kind} ${target.name} anchors no row")
        }

        /** The first segment is proven absent: the name is not local. */
        private fun project(typeRef: com.felipebz.zpa.project.TypeRef, use: LocalTypeUse): LocalTypeEvidence {
            val name = typeRef.name
            if (typeRef is NamedTypeRef && name.segments.size == 1) {
                val spelling = name.last.originalSpelling
                // SYS_REFCURSOR is STANDARD's ref cursor type: a SYS_REFCURSOR formal takes it, a schema object
                // type rejects it.
                val kind = DatatypeFamilies.undeclaredTypeKind(name.last)
                if (kind == CompositeKind.REF_CURSOR) {
                    return LocalTypeEvidence(ProjectArgumentType.composite(kind), LocalTypeEvidence.Terminal.BUILT_IN, followed.toList(), null, terminalUse = use, constrainedUse = constrained)
                }
                // A schema type never shadows a predefined name; any other one-segment name is a schema type.
                if (DatatypeFamilies.ofTypeName(spelling) != null || snapshot == null && kind == null) {
                    return LocalTypeEvidence(builtIn(typeRef), LocalTypeEvidence.Terminal.BUILT_IN, followed.toList(), null, terminalUse = use, constrainedUse = constrained)
                }
                val described = snapshot?.describeExternalType(ProjectQualifiedName.wrap(name), null, metadata, compilationSchema(use.lookupRegion))
                val argumentType = described?.let { ProjectArgumentType.described(it, null) }
                // A schema object the project does not describe is still composite: it rejects every scalar formal.
                if (kind == CompositeKind.SCHEMA_OBJECT && argumentType?.familyOrNull == null) {
                    return LocalTypeEvidence(ProjectArgumentType.composite(kind), LocalTypeEvidence.Terminal.PROJECT, followed.toList(), null, described, constrainedUse = constrained)
                }
                return LocalTypeEvidence(argumentType!!, LocalTypeEvidence.Terminal.PROJECT, followed.toList(), null, described, constrainedUse = constrained)
            }
            return project(name, (typeRef as? AnchoredTypeRef)?.anchor, use.lookupRegion)
        }

        /**
         * [name] described by the project, as a type, or as the `%TYPE` / `%ROWTYPE` [anchor] of it; the `%ROWTYPE` of a
         * package cursor of another file is that cursor's row.
         */
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
            if (anchor == TypeAnchor.ROWTYPE) projectCursor(name, region)?.let { return cursorRow(it, false, region, described) }
            return LocalTypeEvidence(ProjectArgumentType.described(described, owner), LocalTypeEvidence.Terminal.PROJECT, followed.toList(), null, described, constrainedUse = constrained)
        }

        /**
         * A specification variable or constant: the [local] walk when it gives a family, else the project's fact for it,
         * matched by range; two conclusive families that differ give unknown.
         */
        private fun packageValue(declaration: LocalDeclaration, local: LocalTypeEvidence): LocalTypeEvidence {
            val project = projectFact(declaration) ?: return local
            val localFamily = local.argumentType.familyOrNull ?: return project
            // A built-in name the project reached may still be shadowed by a package member: not conclusive.
            val projectFamily = project.argumentType.familyOrNull?.takeIf { project.argumentType.shadowName == null }
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
        /** The declarations that hold a value: an object type's attribute is one (`self.a`, or `a` alone in a method). */
        private val VALUES = setOf(VARIABLE, PARAMETER, CURSOR_PARAMETER, LOOP_INDEX, ATTRIBUTE)
    }
}

private const val NO_WRITTEN_TYPE = "no written type"
private const val NO_PREPARED_PROJECT = "no prepared project"
