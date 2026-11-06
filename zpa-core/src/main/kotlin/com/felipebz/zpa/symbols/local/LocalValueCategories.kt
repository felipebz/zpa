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
import com.felipebz.zpa.api.semantics.*
import com.felipebz.zpa.project.NamedTypeRef
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.QualifiedName
import com.felipebz.zpa.symbols.callArguments
import java.util.*

/**
 * The category of the type of a named value written in a file, from the file's reference outcomes and the type walk
 * over the scan's project. Computed per name when asked; it holds the project through [walk], so it lives on the visitor
 * context, never in the detached facts.
 */
internal class LocalValueCategories(private val facts: LocalSemanticFacts, private val walk: LocalTypeWalk) {
    private val model = facts.model
    private val referencesByStart = facts.references.indices
        .filter { facts.references[it].lastSegmentContext in CONTEXTS }
        .associateBy { facts.references[it].range.let { range -> range.startLine to range.startColumn } }

    fun category(name: AstNode): ValueCategory {
        receiverOf(name)?.let { receiver ->
            val walked = receivers.getOrPut(receiver) { walkReceiver(receiver) } ?: return unavailable()
            return walked.evidence[name]?.let { of(walked.region, it) } ?: unavailable()
        }
        val written = writtenSegments(name) ?: return unavailable()
        val index = referencesByStart[name.token.line to name.token.column] ?: return unavailable()
        val reference = facts.references[index]
        if (reference.segments.size != written || reference.segments.any { it.isAttribute }) return unavailable()
        if (sqlFunctionMayTake(reference, facts.resolutions[index].last())) return unavailable()
        return of(reference.region, nameEvidence(reference, facts.resolutions[index].map(::fieldStep), reference.segments.size))
    }

    private val calls by lazy { facts.calls.associateBy { it.reference.range.startLine to it.reference.range.startColumn } }

    /** The walked receivers, by their `OBJECT_REFERENCE`; null for one this does not model. */
    private val receivers = IdentityHashMap<AstNode, Walked?>()

    /** The evidence of an `OBJECT_REFERENCE` and of each of its member items, read from [region]. */
    private class Walked(val region: LocalRegionId, val evidence: Map<AstNode, LocalTypeEvidence>)

    /**
     * The `OBJECT_REFERENCE` that [name] is, or whose member item (or the name of a call item) [name] is: a member written
     * after an element or a call result. Null for any other node, the receiver's first child included.
     */
    private fun receiverOf(name: AstNode): AstNode? {
        if (name.type === PlSqlGrammar.OBJECT_REFERENCE) return name
        if (name.type !== PlSqlGrammar.VARIABLE_NAME && name.type !== PlSqlGrammar.MEMBER_EXPRESSION && name.type !== PlSqlGrammar.METHOD_CALL) return null
        var current = name
        while (current.parentOrNull?.type === PlSqlGrammar.METHOD_CALL && current.parent.firstChild === current) current = current.parent
        val parent = current.parentOrNull ?: return null
        return parent.takeIf { it.type === PlSqlGrammar.OBJECT_REFERENCE && it.firstChild !== current }
    }

    /**
     * Walks a receiver: the value its first child gives (an element of a collection, one step per argument list, or
     * the result of a call bound to one function or method), then each member item after it. Receivers in SQL, and
     * first children of another form, are not modeled.
     */
    private fun walkReceiver(receiver: AstNode): Walked? {
        val head = receiver.firstChild.takeIf { it.type === PlSqlGrammar.METHOD_CALL } ?: return null
        val (callee, lists) = callParts(head)
        if (callee.type !== PlSqlGrammar.VARIABLE_NAME && callee.type !== PlSqlGrammar.MEMBER_EXPRESSION) return null
        val index = referencesByStart[callee.token.line to callee.token.column] ?: return null
        val reference = facts.references[index]
        if (!inPlSqlOrValues(reference) || writtenSegments(callee) != reference.segments.size || reference.segments.any { it.isAttribute }) return null
        if (sqlFunctionMayTake(reference, facts.resolutions[index].last())) return null
        val region = reference.region
        val evidence = IdentityHashMap<AstNode, LocalTypeEvidence>()
        var current = head(reference, facts.resolutions[index].map(::fieldStep), lists, region)
        for (item in receiver.children.drop(1)) {
            when (item.type) {
                PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION -> current = walk.members(current, names(item), region)
                PlSqlGrammar.METHOD_CALL -> current = callItem(item, current, region, evidence)
                else -> if (item.type !== PlSqlPunctuator.DOT) current = LocalTypeEvidence.unknown("item ${item.type}")
            }
            if (item.type !== PlSqlPunctuator.DOT) evidence[item] = current
        }
        if (receiver.parentOrNull?.type === PlSqlGrammar.CALL_STATEMENT) current = procedureCall(receiver, evidence)
        evidence[receiver] = current
        return Walked(region, evidence)
    }

    /**
     * A receiver read in PL/SQL, or in the `VALUES` of a single-table `INSERT`, which has no table columns in scope.
     * Receivers in other SQL are not modeled.
     */
    private fun inPlSqlOrValues(reference: LocalReference): Boolean =
        reference.sql == null && (reference.lastSegmentContext == LocalReferenceContext.CALL_EXPRESSION || reference.lastSegmentContext == LocalReferenceContext.SQL_CALL)

    /**
     * `name(…)` in SQL that a SQL built-in function of that name takes, whatever local value has the name (the built-in
     * is called): there is no value to categorize.
     */
    private fun sqlFunctionMayTake(reference: LocalReference, outcome: LocalResolution): Boolean =
        reference.segments.isEmpty() && covered(outcome) is LocalResolution.SqlBuiltinOrLocal && SqlBuiltinFunctions.mayTake(reference.name)

    /** A call statement's last item calls a procedure: neither it nor the statement has a value. */
    private fun procedureCall(receiver: AstNode, evidence: MutableMap<AstNode, LocalTypeEvidence>): LocalTypeEvidence {
        val none = LocalTypeEvidence.unknown("a procedure call")
        var call: AstNode = receiver.children.last()
        while (call.type === PlSqlGrammar.METHOD_CALL) {
            evidence[call] = none
            call = call.firstChild
        }
        return none
    }

    /** The innermost name of a call `name(…)(…)`, and its argument lists in written order. */
    private fun callParts(call: AstNode): Pair<AstNode, List<AstNode>> {
        val calls = ArrayList<AstNode>()
        var current = call
        while (current.type === PlSqlGrammar.METHOD_CALL) {
            calls += current
            current = current.firstChild
        }
        return current to calls.asReversed().flatMap { it.getChildren(PlSqlGrammar.ARGUMENTS) }
    }

    private fun names(item: AstNode): List<OracleIdentifier> =
        if (item.type === PlSqlGrammar.VARIABLE_NAME) listOf(LocalSyntax.identifier(item.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)))
        else item.children.filter { it.type === PlSqlGrammar.VARIABLE_NAME || it.type === PlSqlGrammar.IDENTIFIER_NAME }
            .map { LocalSyntax.identifier(it.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME) ?: it) }

    /**
     * The value of the first child of a receiver, `name(…)…`: the result of a call the call analysis binds to one local
     * or project function, else an element of the collection the name gives (one step per list), else the result of a
     * method call `v.m(…)` on a value of a project object type. Text a `$IF` or a substitution touches keeps it undecided.
     */
    private fun head(reference: LocalReference, steps: List<LocalResolution>, lists: List<AstNode>, region: LocalRegionId): LocalTypeEvidence {
        steps.map(::covered).filterIsInstance<LocalResolution.Incomplete>().firstOrNull { it.regionReasons.isNotEmpty() }
            ?.let { return LocalTypeEvidence.unknown("undecided text", incomplete = it) }
        val analysis = calls[reference.range.startLine to reference.range.startColumn]
        when {
            analysis?.outcome == LocalCallOutcome.PROJECT && analysis.typed?.status == ProjectTypedCallResolution.Status.RESOLVED ->
                return elements(walk.projectResult(analysis.typed, region), lists.drop(1), region)
            analysis?.outcome == LocalCallOutcome.LOCAL_CAPTURE && analysis.local != null -> {
                // A call that binds no single candidate is unknown: Oracle rejects it (PLS-00307), or the candidates differ
                // only by argument types, which this analysis does not compare.
                val selected = analysis.local.selected ?: return LocalTypeEvidence.unknown("local call ${analysis.local.status}")
                return elements(walk.result(selected), lists.drop(1), region)
            }
        }
        // An object type's constructor, user-defined or the default one: nothing local takes the name.
        if (LocalReferenceOutcomes.isAbsent(steps.first())) {
            walk.constructorResult(QualifiedName(listOf(reference.name) + reference.segments.map { it.name }), region)
                ?.let { return elements(it, lists.drop(1), region) }
        }
        val value = nameEvidence(reference, steps, reference.segments.size)
        if (value.terminal != LocalTypeEvidence.Terminal.UNKNOWN || value.incomplete != null || reference.segments.isEmpty()) return elements(value, lists, region)
        // `v.m(…)`: a method of the value's object type.
        val receiver = nameEvidence(reference, steps, reference.segments.size - 1)
        val result = walk.methodResult(receiver, reference.segments.last().name, callArguments(lists.first()), region)
        return elements(result, lists.drop(1), region)
    }

    /** A call item `.m(…)…` or `.f(i)…` after [before]: a method's result, or an element of a collection member. */
    private fun callItem(item: AstNode, before: LocalTypeEvidence, region: LocalRegionId, evidence: MutableMap<AstNode, LocalTypeEvidence>): LocalTypeEvidence {
        val (callee, lists) = callParts(item)
        if (callee.type !== PlSqlGrammar.VARIABLE_NAME && callee.type !== PlSqlGrammar.MEMBER_EXPRESSION) return LocalTypeEvidence.unknown("call item ${callee.type}")
        val names = names(callee)
        val owner = walk.members(before, names.dropLast(1), region)
        val member = walk.members(owner, names.takeLast(1), region)
        evidence[callee] = member
        val result = if (member.terminal == LocalTypeEvidence.Terminal.UNKNOWN && member.incomplete == null && owner.terminal == LocalTypeEvidence.Terminal.PROJECT) {
            elements(walk.methodResult(owner, names.last(), callArguments(lists.first()), region), lists.drop(1), region)
        } else {
            elements(member, lists, region)
        }
        var call = callee.parent
        while (call !== item) { evidence[call] = result; call = call.parent }
        return result
    }

    /** An element per argument list: each indexes the collection before it with one positional actual. */
    private fun elements(evidence: LocalTypeEvidence, lists: List<AstNode>, region: LocalRegionId): LocalTypeEvidence =
        lists.fold(evidence) { current, list ->
            val arguments = list.getChildren(PlSqlGrammar.ARGUMENT)
            if (arguments.size != 1 || arguments.single().hasDirectChildren(PlSqlPunctuator.ASSOCIATION)) {
                if (current.terminal == LocalTypeEvidence.Terminal.UNKNOWN) current else LocalTypeEvidence.unknown("not one index")
            } else {
                walk.element(current, region)
            }
        }

    /**
     * Evidence of the value the first [count] segments after the first name of [reference] give: a local value or field,
     * a member of a specification in another file, a package value of another file, a member chain the project types,
     * or a name in SQL a column may take. Both [category] and the first child of a receiver read a name's value here.
     */
    private fun nameEvidence(reference: LocalReference, steps: List<LocalResolution>, count: Int): LocalTypeEvidence {
        val region = reference.region
        val segments = reference.segments.take(count)
        val target = QualifiedName(listOf(reference.name) + segments.map { it.name })
        val first = steps.first()
        if (LocalReferenceOutcomes.isSpecificationMember(first)) {
            walk.specificationMember(target.segments, region)?.let { return walk.projectChain(it, region) }
            return walk.enclosingType(region)?.let { walk.members(it, target.segments, region) } ?: outsideFile()
        }
        if (LocalReferenceOutcomes.isAbsent(first)) return if (count == 0) outsideFile() else walk.projectChain(target, region)
        val relevant = steps.take(count + 1)
        if (relevant.drop(1).any(LocalReferenceOutcomes::isSpecificationMember) && relevant.none { covered(it) is LocalResolution.Incomplete }) {
            return walk.projectChain(target, region)
        }
        projectMemberSteps(relevant, segments, region)?.let { return it }
        val last = covered(relevant.last())
        if (last is LocalResolution.Incomplete) return LocalTypeEvidence.unknown("undecided", incomplete = last)
        if (relevant.size != count + 1) return LocalTypeEvidence.unknown("stopped at $last")
        return lastStep(reference, last)
    }

    /**
     * The member steps the local steps leave to the project: they stop at a member of a value whose type the file does
     * not declare, or at a member of `SELF` in the body of an object type whose specification is in another file. The
     * steps before must be decided. Null when the steps do not stop so.
     */
    private fun projectMemberSteps(relevant: List<LocalResolution>, segments: List<LocalSegment>, region: LocalRegionId): LocalTypeEvidence? {
        val stop = relevant.indexOfFirst { needsProjectMember(it) }
        if (stop < 1 || stop != relevant.lastIndex) return null
        relevant.subList(0, stop).map(::covered).filterIsInstance<LocalResolution.Incomplete>().firstOrNull()
            ?.let { return LocalTypeEvidence.unknown("undecided", incomplete = it) }
        val base = covered(relevant[stop - 1])
        val evidence = when {
            base is LocalResolution.ImplicitSelf || isSelfParameter(base) -> walk.enclosingType(region)
            covered(relevant[stop]) is LocalResolution.Incomplete -> null
            else -> walk.outcome(base)
        } ?: return null
        return walk.members(evidence, segments.drop(stop - 1).map { it.name }, region)
    }

    /** The value the decided last step of a name gives; a name in SQL a column may take stays undecided. */
    private fun lastStep(reference: LocalReference, last: LocalResolution): LocalTypeEvidence = when (last) {
        // In SQL a column of the same name would take it.
        is LocalResolution.SqlColumnOrLocal ->
            if (reference.sql?.excludesColumn(reference.name) == true) walk.value(last.declaration)
            else LocalTypeEvidence.unknown("a column may take it", incomplete = LocalResolution.Incomplete(emptySet(), EnumSet.of(LocalLookupGap.SQL_COLUMN_UNKNOWN), last))
        // `name(…)` in SQL: the local value, whose elements the call reads.
        is LocalResolution.SqlBuiltinOrLocal -> last.declarations.singleOrNull()?.let(walk::value) ?: LocalTypeEvidence.unknown("several declarations")
        else -> walk.outcome(last)
    }

    /**
     * A field of a record type declared in a package body whose specification is in another file: the resolver keeps
     * the specification's gap on the step, but a record's fields are its own, whatever the specification declares.
     */
    private fun fieldStep(step: LocalResolution): LocalResolution =
        if (step is LocalResolution.Incomplete && step.regionReasons.isEmpty() && step.lookupReasons == setOf(LocalLookupGap.SPECIFICATION_NOT_IN_FILE) &&
            step.provisional is LocalResolution.Field
        ) step.provisional else step

    private fun outsideFile() = LocalTypeEvidence.unknown(
        "declared outside the file", incomplete = LocalResolution.Incomplete(emptySet(), EnumSet.of(LocalLookupGap.SPECIFICATION_NOT_IN_FILE), LocalResolution.ProvenAbsent)
    )

    /** `SELF` written as a parameter of a method (`SELF IN OUT t` of a constructor). */
    private fun isSelfParameter(outcome: LocalResolution): Boolean =
        outcome is LocalResolution.Bound && model.declaration(outcome.declaration).let { it.kind == LocalDeclarationKind.PARAMETER && it.name == LocalResolver.SELF }

    /** A member step the local resolver cannot take: a value of a type it does not hold, or a specification in another file. */
    private fun needsProjectMember(step: LocalResolution): Boolean = when (val outcome = covered(step)) {
        is LocalResolution.Unsupported -> outcome.construct == LocalUnsupportedConstruct.MEMBER_NEEDS_TYPING
        is LocalResolution.Incomplete -> outcome.regionReasons.isEmpty() && outcome.lookupReasons.isNotEmpty() &&
            TYPE_GAPS.containsAll(outcome.lookupReasons)
        else -> false
    }

    /** The number of segments after the first that [name] writes; null when it is not a value name of its own. */
    private fun writtenSegments(name: AstNode): Int? = when (name.type) {
        PlSqlGrammar.VARIABLE_NAME ->
            0.takeUnless { name.parentOrNull?.type === PlSqlGrammar.MEMBER_EXPRESSION && name.parentOrNull?.firstChild === name }
        PlSqlGrammar.MEMBER_EXPRESSION -> name.children.count { it.type === PlSqlGrammar.VARIABLE_NAME || it.type === PlSqlGrammar.IDENTIFIER_NAME } - 1
        else -> null
    }

    private fun of(region: LocalRegionId, evidence: LocalTypeEvidence): ValueCategory {
        if (isRefCursorBuiltIn(evidence)) return ValueCategory.CursorVariable
        return terminal(region, evidence)
    }

    /**
     * A value written `SYS_REFCURSOR` that the walk handed out of the file: the predefined cursor type, which the project
     * does not classify. A local declaration of that name is followed locally and never reaches this.
     */
    private fun isRefCursorBuiltIn(evidence: LocalTypeEvidence): Boolean {
        if (evidence.terminal != LocalTypeEvidence.Terminal.PROJECT && evidence.terminal != LocalTypeEvidence.Terminal.BUILT_IN) return false
        val written = evidence.followed.lastOrNull()?.let(model::declaration)?.type?.typeRef as? NamedTypeRef ?: return false
        return written.name.segments.size == 1 && written.name.last.lookupName == "SYS_REFCURSOR"
    }

    private fun terminal(region: LocalRegionId, evidence: LocalTypeEvidence): ValueCategory = when (evidence.terminal) {
        LocalTypeEvidence.Terminal.COMPOSITE -> composite(region, evidence)
        LocalTypeEvidence.Terminal.BUILT_IN ->
            evidence.argumentType.family.orElse(null)?.let { ValueCategory.Scalar(scalarFamily(it)) } ?: unknown(UsageUncertainty.UNSUPPORTED_REFERENCE)
        LocalTypeEvidence.Terminal.PROJECT -> evidence.description?.let { described(region, it) } ?: unknown(UsageUncertainty.DECLARATION_OUTSIDE_FILE)
        LocalTypeEvidence.Terminal.LITERAL -> unavailable()
        LocalTypeEvidence.Terminal.UNKNOWN -> evidence.incomplete?.let { ValueCategory.Unknown(reasons(it)) }
            ?: unknown(UsageUncertainty.UNSUPPORTED_REFERENCE)
    }

    /** A local record, collection or ref cursor type, a cursor's row, a query loop's record, or `SELF`. */
    private fun composite(region: LocalRegionId, evidence: LocalTypeEvidence): ValueCategory {
        val last = evidence.followed.lastOrNull()?.let(model::declaration) ?: return ValueCategory.ObjectInstance
        return when (last.kind) {
            LocalDeclarationKind.RECORD_TYPE -> ValueCategory.Record(RecordOrigin.RECORD_TYPE)
            LocalDeclarationKind.CURSOR, LocalDeclarationKind.LOOP_INDEX -> ValueCategory.Record(RecordOrigin.ROWTYPE)
            LocalDeclarationKind.REF_CURSOR_TYPE -> ValueCategory.CursorVariable
            LocalDeclarationKind.COLLECTION_TYPE -> {
                val kind = when ((last.details as? LocalCollectionType)?.form) {
                    LocalCollectionForm.ASSOCIATIVE_ARRAY -> CollectionKind.ASSOCIATIVE_ARRAY
                    LocalCollectionForm.NESTED_TABLE -> CollectionKind.NESTED_TABLE
                    LocalCollectionForm.VARRAY -> CollectionKind.VARRAY
                    null -> null
                }
                ValueCategory.Collection(kind, of(region, walk.written(last.type)))
            }
            else -> unknown(UsageUncertainty.UNSUPPORTED_REFERENCE)
        }
    }

    /**
     * A type the project describes; only a conclusive description classifies. [seen] holds the project collection types
     * whose element is being classified, so that a cycle stops there.
     */
    private fun described(region: LocalRegionId, description: ProjectExternalTypeDescription, seen: Set<ProjectDeclarationId> = emptySet()): ValueCategory {
        if (description.anchor.orElse(null) == ProjectTypeReference.Anchor.ROWTYPE) return ValueCategory.Record(RecordOrigin.ROWTYPE)
        val source = description.source
        return when (source.status) {
            ProjectTypeDescription.Status.DECLARED_TYPE -> shaped(region, source.declaration.orElse(null) as? ProjectShapedTypeView, seen)
            ProjectTypeDescription.Status.BUILT_IN -> ValueCategory.Scalar(DatatypeFamilies.ofDescription(description, null, null)?.let(::scalarFamily))
            ProjectTypeDescription.Status.ANCHORED_REFERENCE ->
                if (source.anchorStop.orElse(null) == ProjectTypeDescription.AnchorStop.ROWTYPE) ValueCategory.Record(RecordOrigin.ROWTYPE)
                else unknown(UsageUncertainty.DECLARATION_OUTSIDE_FILE)
            else -> unknown(UsageUncertainty.DECLARATION_OUTSIDE_FILE)
        }
    }

    private fun shaped(region: LocalRegionId, type: ProjectShapedTypeView?, seen: Set<ProjectDeclarationId>): ValueCategory = when (type?.shape?.orElse(null)) {
        ProjectTypeShape.RECORD -> ValueCategory.Record(RecordOrigin.RECORD_TYPE)
        ProjectTypeShape.OBJECT -> ValueCategory.ObjectInstance
        ProjectTypeShape.REF_CURSOR -> ValueCategory.CursorVariable
        // The project keeps no collection kind; its element is classified like any type it describes.
        ProjectTypeShape.COLLECTION ->
            // A collection whose element leads back to it (Oracle rejects it, the index may still hold it) is unknown.
            if (type.id in seen) unknown(UsageUncertainty.UNSUPPORTED_REFERENCE)
            else ValueCategory.Collection(null, walk.projectElement(type, region)?.let { described(region, it, seen + type.id) }
                ?: unknown(UsageUncertainty.DECLARATION_OUTSIDE_FILE))
        null -> unknown(UsageUncertainty.DECLARATION_OUTSIDE_FILE)
    }

    private fun scalarFamily(family: ProjectDatatypeFamily): ScalarFamily? = when (family) {
        ProjectDatatypeFamily.NUMERIC -> ScalarFamily.NUMERIC
        ProjectDatatypeFamily.CHARACTER -> ScalarFamily.CHARACTER
        ProjectDatatypeFamily.CLOB -> ScalarFamily.CLOB
        ProjectDatatypeFamily.BLOB -> ScalarFamily.BLOB
        ProjectDatatypeFamily.RAW -> ScalarFamily.RAW
        ProjectDatatypeFamily.DATETIME -> ScalarFamily.DATETIME
        ProjectDatatypeFamily.INTERVAL -> ScalarFamily.INTERVAL
        ProjectDatatypeFamily.BOOLEAN -> ScalarFamily.BOOLEAN
        ProjectDatatypeFamily.JSON -> ScalarFamily.JSON
        ProjectDatatypeFamily.ROWID -> ScalarFamily.ROWID
        ProjectDatatypeFamily.COMPOSITE -> null
    }

    private val typeUses: Map<Pair<Int, Int>, LocalTypeUse> by lazy {
        val uses = model.declarations.mapNotNull { it.type } +
            model.declarations.mapNotNull { (it.details as? LocalSubprogram)?.returnType } +
            model.declarations.flatMap { declaration -> (declaration.details as? LocalSubprogram)?.parameters.orEmpty().mapNotNull { it.type } } +
            model.declarations.flatMap { declaration -> (declaration.details as? LocalRecordType)?.fields.orEmpty().mapNotNull { it.type } }
        uses.associateBy { it.range.startLine to it.range.startColumn }
    }

    /** The category of the type a `DATATYPE` node writes: a declaration's, a parameter's or a function's RETURN type. */
    fun typeCategory(datatype: AstNode): ValueCategory {
        if (datatype.type !== PlSqlGrammar.DATATYPE) return unavailable()
        val use = typeUses[datatype.token.line to datatype.token.column] ?: return unavailable()
        val evidence = walk.written(use)
        val written = use.typeRef as? NamedTypeRef
        val refCursor = written != null && written.name.segments.size == 1 && written.name.last.lookupName == "SYS_REFCURSOR" &&
            (evidence.terminal == LocalTypeEvidence.Terminal.PROJECT || evidence.terminal == LocalTypeEvidence.Terminal.BUILT_IN)
        return if (refCursor) ValueCategory.CursorVariable else of(use.lookupRegion, evidence)
    }

    /**
     * What the named type a `DATATYPE` writes denotes, from the same walk as [typeCategory]: the first declaration it
     * reaches, local or of the project, or why there is none. Text a `$IF` or a substitution touches leaves it undecided.
     */
    fun typeName(datatype: AstNode): LocalTypeName {
        if (datatype.type !== PlSqlGrammar.DATATYPE) return LocalTypeName.Unavailable
        val use = typeUses[datatype.token.line to datatype.token.column] ?: return LocalTypeName.Unavailable
        if (use.typeRef !is NamedTypeRef) return LocalTypeName.Unavailable
        val evidence = walk.written(use)
        evidence.followed.firstOrNull()?.let { return LocalTypeName.Local(it, walk.projectDeclarationOf(it)) }
        return when (evidence.terminal) {
            LocalTypeEvidence.Terminal.BUILT_IN -> LocalTypeName.BuiltIn((use.typeRef as NamedTypeRef).name.last.originalSpelling)
            LocalTypeEvidence.Terminal.PROJECT -> projectTypeName(evidence.description?.source)
            else -> LocalTypeName.Undecided(evidence.incomplete?.let(::reasons) ?: EnumSet.of(UsageUncertainty.UNSUPPORTED_REFERENCE))
        }
    }

    /** The first declaration the project's description reached: its first subtype step, else the type it ends at. */
    private fun projectTypeName(source: ProjectTypeDescription?): LocalTypeName {
        source ?: return LocalTypeName.Undecided(EnumSet.of(UsageUncertainty.DECLARATION_OUTSIDE_FILE))
        (source.steps.firstOrNull() as? ProjectTypeDeclarationView)?.let { return LocalTypeName.Project(it) }
        source.declaration.orElse(null)?.let { return LocalTypeName.Project(it) }
        return source.resolution.orElse(null)?.let { LocalTypeName.Unresolved(it) } ?: LocalTypeName.Undecided(EnumSet.of(UsageUncertainty.DECLARATION_OUTSIDE_FILE))
    }

    private fun covered(outcome: LocalResolution) = (outcome as? LocalResolution.SpecificationCovered)?.outcome ?: outcome

    private fun reasons(outcome: LocalResolution.Incomplete): Set<UsageUncertainty> {
        val reasons = EnumSet.noneOf(UsageUncertainty::class.java)
        outcome.regionReasons.forEach {
            reasons += it.usageUncertainty()
        }
        outcome.lookupReasons.forEach {
            reasons += if (it == LocalLookupGap.SQL_COLUMN_UNKNOWN) UsageUncertainty.SQL_NAME_MAY_BE_COLUMN else UsageUncertainty.DECLARATION_OUTSIDE_FILE
        }
        return reasons
    }

    private fun unknown(reason: UsageUncertainty) = ValueCategory.Unknown(EnumSet.of(reason))

    private fun unavailable() = ValueCategory.Unavailable(UsageUnavailability.NOT_A_COVERED_NAME)

    private companion object {
        /** The gaps of a lookup in an object type whose specification or supertype is in another file. */
        val TYPE_GAPS: Set<LocalLookupGap> = EnumSet.of(LocalLookupGap.SPECIFICATION_NOT_IN_FILE, LocalLookupGap.SUPERTYPE_NOT_IN_FILE)

        /** A name read as a value: in PL/SQL, in SQL, in `VALUES`, or called (an element access or a call). */
        val CONTEXTS = EnumSet.of(
            LocalReferenceContext.VALUE, LocalReferenceContext.SQL_VALUE, LocalReferenceContext.INSERT_VALUE,
            LocalReferenceContext.CALL_EXPRESSION, LocalReferenceContext.SQL_CALL
        )
    }
}
