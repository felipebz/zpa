//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.*
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.api.project.ProjectTypeShape
import com.felipebz.zpa.api.semantics.*
import com.felipebz.zpa.project.*
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

    fun category(name: AstNode): ValueCategory = valueOf(name)?.let { (region, evidence) -> of(region, evidence) } ?: unavailable()

    /**
     * [category] of a member expression or a member item of a receiver whose last name is one field or attribute of a
     * value whose type the project declares; null for any other node.
     */
    fun projectMemberCategory(name: AstNode): ValueCategory? {
        val last = when (name.type) {
            PlSqlGrammar.MEMBER_EXPRESSION -> name.children.lastOrNull { it.type === PlSqlGrammar.VARIABLE_NAME || it.type === PlSqlGrammar.IDENTIFIER_NAME }
            PlSqlGrammar.VARIABLE_NAME -> name.takeIf { receiverOf(it) != null }
            else -> null
        } ?: return null
        val (_, base) = memberBase(last) ?: return null
        val member = LocalSyntax.identifier(last.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME) ?: last)
        if (walk.projectMember(base, member) !is ProjectRecordMemberResolution.Resolved) return null
        return valueOf(name)?.let { (region, evidence) -> of(region, evidence) }
    }

    /** The region a value name is read from and the walk's evidence of its value; null for a name this does not cover. */
    private fun valueOf(name: AstNode): Pair<LocalRegionId, LocalTypeEvidence>? {
        receiverOf(name)?.let { receiver ->
            val walked = walked(receiver) ?: return null
            return walked.evidence[name]?.let { walked.region to it }
        }
        val written = writtenSegments(name) ?: return null
        val index = referencesByStart[name.token.line to name.token.column] ?: return null
        val reference = facts.references[index]
        if (reference.segments.size != written || reference.segments.any { it.isAttribute }) return null
        if (sqlFunctionMayTake(reference, facts.resolutions[index].last())) return null
        return reference.region to nameEvidence(reference, facts.resolutions[index].map(::fieldStep), reference.segments.size)
    }

    private fun walked(receiver: AstNode): Walked? = receivers.getOrPut(receiver) { walkReceiver(receiver, inSql = false) }

    /**
     * A receiver walked in SQL too, for the member a name of it names: `name(…)` in SQL is a call or an element, which
     * no column takes, so the members after it are those of that value.
     */
    private fun walkedInSql(receiver: AstNode): Walked? = sqlReceivers.getOrPut(receiver) { walkReceiver(receiver, inSql = true) }

    /** The receivers walked with those in SQL, by their `OBJECT_REFERENCE`. */
    private val sqlReceivers = IdentityHashMap<AstNode, Walked?>()

    /** The segments after the first of every qualified name, by the start of their name: the reference and the segment's position (1 for the second). */
    private val segmentsByStart: Map<Pair<Int, Int>, Pair<Int, Int>> by lazy(LazyThreadSafetyMode.NONE) {
        facts.references.indices.filter { facts.references[it].segments.isNotEmpty() }.flatMap { index ->
            facts.references[index].segments.mapIndexed { position, segment -> (segment.range.startLine to segment.range.startColumn) to (index to position + 1) }
        }.toMap()
    }

    /**
     * The region and the walk's evidence of the value whose member the name [segment] is: in a qualified name, the value
     * of the names before it; in a member item of an expression receiver (`f(1).a.b`, `v(i).m().c`), the value of what
     * precedes it in the receiver. Null when [segment] is no such name, or the receiver is one this does not walk.
     */
    fun memberBase(segment: AstNode): Pair<LocalRegionId, LocalTypeEvidence>? {
        segmentsByStart[segment.token.line to segment.token.column]?.let { (index, position) ->
            val reference = facts.references[index]
            if (reference.segments.take(position).any { it.isAttribute }) return null
            return reference.region to ownerEvidence(reference, facts.resolutions[index].map(::fieldStep), position - 1)
        }
        var item = segment
        while (item.parentOrNull?.type.let { it === PlSqlGrammar.MEMBER_EXPRESSION || it === PlSqlGrammar.METHOD_CALL }) item = item.parent
        val receiver = item.parentOrNull?.takeIf { it.type === PlSqlGrammar.OBJECT_REFERENCE && it.firstChild !== item } ?: return null
        val walked = walkedInSql(receiver) ?: return null
        val before = receiver.children.subList(0, receiver.children.indexOf(item)).lastOrNull { it.type !== PlSqlPunctuator.DOT } ?: return null
        val evidence = walked.evidence[before] ?: return null
        val callee = if (item.type === PlSqlGrammar.METHOD_CALL) callParts(item).first else item
        val written = if (callee.type === PlSqlGrammar.VARIABLE_NAME) listOf(callee)
        else callee.children.filter { it.type === PlSqlGrammar.VARIABLE_NAME || it.type === PlSqlGrammar.IDENTIFIER_NAME }
        val position = written.indexOfFirst { it === segment || it === segment.parentOrNull }.takeIf { it >= 0 } ?: return null
        return walked.region to walk.members(evidence, names(callee).take(position), walked.region)
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
     * the result of a call bound to one function or method), then each member item after it. Receivers in SQL outside
     * the `VALUES` of a single-table `INSERT` are walked only when [inSql]; first children of another form never are.
     */
    private fun walkReceiver(receiver: AstNode, inSql: Boolean): Walked? {
        val head = receiver.firstChild.takeIf { it.type === PlSqlGrammar.METHOD_CALL } ?: return null
        val (callee, lists) = callParts(head)
        if (callee.type !== PlSqlGrammar.VARIABLE_NAME && callee.type !== PlSqlGrammar.MEMBER_EXPRESSION) return null
        val index = referencesByStart[callee.token.line to callee.token.column] ?: return null
        val reference = facts.references[index]
        if (!walksHead(reference, callee, index, inSql)) return null
        val region = reference.region
        val evidence = IdentityHashMap<AstNode, LocalTypeEvidence>()
        var current = head(reference, facts.resolutions[index].map(::fieldStep), lists, region)
        evidence[head] = current
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

    /** Whether the name [callee] of the first child of a receiver, the reference at [index], is one the walk types. */
    private fun walksHead(reference: LocalReference, callee: AstNode, index: Int, inSql: Boolean): Boolean =
        (if (inSql) isCall(reference) else inPlSqlOrValues(reference)) && writtenSegments(callee) == reference.segments.size &&
            reference.segments.none { it.isAttribute } && !sqlFunctionMayTake(reference, facts.resolutions[index].last())

    /**
     * A receiver read in PL/SQL, or in the `VALUES` of a single-table `INSERT`, which has no table columns in scope.
     * Receivers in other SQL are not modeled.
     */
    private fun inPlSqlOrValues(reference: LocalReference): Boolean = reference.sql == null && isCall(reference)

    private fun isCall(reference: LocalReference): Boolean =
        reference.lastSegmentContext == LocalReferenceContext.CALL_EXPRESSION || reference.lastSegmentContext == LocalReferenceContext.SQL_CALL

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
        val receiver = ownerEvidence(reference, steps, reference.segments.size - 1)
        val types = analysis?.actuals?.map { it.evidence.argumentType }
        val result = walk.methodResult(receiver, reference.segments.last().name, callArguments(lists.first()), types, region)
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
            val types = facts.itemActualTypes[callee.token.line to callee.token.column]
            elements(walk.methodResult(owner, names.last(), callArguments(lists.first()), types, region), lists.drop(1), region)
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
     * Evidence of the value whose member the segment after the first [count] ones is: [nameEvidence], except that `SELF`
     * alone gives the type the body implements, its specification and supertypes included, as the project describes it.
     */
    private fun ownerEvidence(reference: LocalReference, steps: List<LocalResolution>, count: Int): LocalTypeEvidence {
        val first = covered(steps.first())
        if (count == 0 && (first is LocalResolution.ImplicitSelf || isSelfParameter(first))) walk.enclosingType(reference.region)?.let { return it }
        return nameEvidence(reference, steps, count)
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
            LocalDeclarationKind.CURSOR -> {
                // A loop over a cursor with a RETURN type has the RETURN type's row.
                val loop = evidence.followed.first().let(model::declaration).kind == LocalDeclarationKind.LOOP_INDEX
                (last.details as? LocalCursor)?.returnType?.takeIf { loop }?.let { of(region, walk.written(it)) } ?: queryRow(last)
            }
            LocalDeclarationKind.LOOP_INDEX -> loopRow((last.details as? LocalLoopIndex)?.control)
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
        if (description.anchor.orElse(null) == ProjectTypeReference.Anchor.ROWTYPE) return rowtype(region, description)
        val source = description.source
        return when (source.status) {
            ProjectTypeDescription.Status.DECLARED_TYPE -> shaped(region, source.declaration.orElse(null) as? ProjectShapedTypeView, seen)
            ProjectTypeDescription.Status.BUILT_IN -> ValueCategory.Scalar(DatatypeFamilies.ofDescription(description, null, null)?.let(::scalarFamily))
            ProjectTypeDescription.Status.ANCHORED_REFERENCE ->
                if (source.anchorStop.orElse(null) == ProjectTypeDescription.AnchorStop.ROWTYPE) rowtype(region, description)
                else unknown(UsageUncertainty.DECLARATION_OUTSIDE_FILE)
            else -> unknown(UsageUncertainty.DECLARATION_OUTSIDE_FILE)
        }
    }

    /**
     * A `%ROWTYPE` record the project describes, with the relation written before `%ROWTYPE` when the project shows no
     * package cursor that name may denote. A package cursor's row is not known here: no relation.
     */
    private fun rowtype(region: LocalRegionId, description: ProjectExternalTypeDescription): ValueCategory.Record {
        val name = description.anchorName.orElse(null)?.takeIf { description.anchor.orElse(null) == ProjectTypeReference.Anchor.ROWTYPE }
        return ValueCategory.Record(RecordOrigin.ROWTYPE, name?.takeIf { walk.namesNoPackageCursor(it, region) }?.let(RecordRelation::Named))
    }

    /**
     * `c%ROWTYPE` of the local cursor [cursor]: the row of its query, a RETURN type notwithstanding. Its fields are the
     * query's select list (`select b, a` gives the fields `b, a`), which a fetch fills by position.
     */
    private fun queryRow(cursor: LocalDeclaration) = ValueCategory.Record(RecordOrigin.ROWTYPE, relation((cursor.details as? LocalCursor)?.row))

    private fun relation(row: LocalQueryRow?): RecordRelation? = when (row) {
        is LocalQueryRow.AllOf -> RecordRelation.Named(ProjectQualifiedName.wrap(row.relation))
        LocalQueryRow.OwnColumns -> RecordRelation.QueryColumns
        null -> null
    }

    /** The record of a query loop; the walk follows a cursor loop's index to its cursor. */
    private fun loopRow(control: LocalLoopControl?) =
        ValueCategory.Record(RecordOrigin.ROWTYPE, relation(control?.takeIf { it.form == LocalLoopControl.Form.QUERY }?.row))

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
            model.declarations.flatMap { declaration -> (declaration.details as? LocalRecordType)?.fields.orEmpty().mapNotNull { it.type } } +
            // Index-by key types and REF CURSOR RETURN types: PL/SQL declarations, looked up as a variable's type.
            model.declarations.mapNotNull { (it.details as? LocalCollectionType)?.indexType } +
            model.declarations.mapNotNull { (it.details as? LocalRefCursorType)?.returnType }
        uses.associateBy { it.range.startLine to it.range.startColumn }
    }

    /**
     * The category of the type a `DATATYPE` node writes: a declaration's, a parameter's or a function's RETURN type, an
     * index-by key or a REF CURSOR RETURN type, or a type written in an expression (`TREAT`, `IS OF`, `CAST`) or a SQL
     * column definition, looked up as [typeName] says.
     */
    fun typeCategory(datatype: AstNode): ValueCategory = when (val written = written(datatype)) {
        null -> unavailable()
        else -> if (isRefCursorName(written)) ValueCategory.CursorVariable else of(written.use.lookupRegion, written.evidence)
    }

    /**
     * What the named type a `DATATYPE` writes denotes, from the same walk as [typeCategory]: the first declaration it
     * reaches, local or of the project, or why there is none. Text a `$IF` or a substitution touches leaves it undecided.
     *
     * Where Oracle looks the name up depends on the position. A declaration's type, an index-by key, a REF CURSOR RETURN
     * type, and `TREAT`/`IS OF` in PL/SQL follow PL/SQL scope. `TREAT`, `IS OF`, `CAST` and column types inside SQL see
     * schema types only: neither a local nor a package type (ORA-00902), and a local declaration does not hide a schema
     * type there. `CAST` in PL/SQL takes the same types (PLS-00382 for a local or package subtype). An
     * external parameter's type is an external (C) datatype of a call specification, never looked up.
     */
    fun typeName(datatype: AstNode): LocalTypeName {
        val written = written(datatype) ?: return LocalTypeName.Unavailable
        val name = (written.use.typeRef as? NamedTypeRef)?.name ?: return LocalTypeName.Unavailable
        if (written.external || isRefCursorName(written)) return LocalTypeName.BuiltIn(name.last.originalSpelling)
        val evidence = written.evidence
        evidence.followed.firstOrNull()?.let { return LocalTypeName.Local(it, walk.projectDeclarationOf(it)) }
        return when (evidence.terminal) {
            LocalTypeEvidence.Terminal.BUILT_IN -> LocalTypeName.BuiltIn(name.last.originalSpelling)
            LocalTypeEvidence.Terminal.PROJECT -> projectTypeName(evidence.description?.source)
            else -> LocalTypeName.Undecided(evidence.incomplete?.let(::reasons) ?: EnumSet.of(UsageUncertainty.UNSUPPORTED_REFERENCE))
        }
    }

    /**
     * The project type [declaration] is directly declared with: the first project declaration its own written type (or
     * function result) names, as [typeName] answers it. Null for a local, predefined, anchored or undecided type.
     */
    fun declaredProjectType(declaration: LocalDeclaration): ProjectTypeDeclarationView? {
        val use = declaration.type ?: (declaration.details as? LocalSubprogram)?.returnType ?: return null
        if (use.typeRef !is NamedTypeRef) return null
        val evidence = walk.written(use)
        if (evidence.followed.isNotEmpty() || evidence.terminal != LocalTypeEvidence.Terminal.PROJECT) return null
        return (projectTypeName(evidence.description?.source) as? LocalTypeName.Project)?.declaration
    }

    /** The schema the unit holding [node] compiles in (see [LocalTypeWalk.compilationSchema]); null when unknown. */
    fun compilationSchema(node: AstNode): ProjectIdentifier? =
        walk.compilationSchema(LocalReferenceCollector.innermostRegion(model, LocalSyntax.range(model.fileId, node)))

    /** A written type, the use it records and the walk's evidence of it. */
    private class WrittenType(val use: LocalTypeUse, val evidence: LocalTypeEvidence, val external: Boolean = false)

    /** `SYS_REFCURSOR` that no local declaration takes: the predefined weak cursor type. */
    private fun isRefCursorName(written: WrittenType): Boolean {
        val name = (written.use.typeRef as? NamedTypeRef)?.name ?: return false
        return name.segments.size == 1 && name.last.lookupName == "SYS_REFCURSOR" && written.evidence.followed.isEmpty() &&
            (written.evidence.terminal == LocalTypeEvidence.Terminal.PROJECT || written.evidence.terminal == LocalTypeEvidence.Terminal.BUILT_IN)
    }

    private fun written(datatype: AstNode): WrittenType? {
        if (datatype.type !== PlSqlGrammar.DATATYPE) return null
        typeUses[datatype.token.line to datatype.token.column]?.let { return WrittenType(it, walk.written(it)) }
        val position = datatype.parentOrNull?.type ?: return null
        if (position !in EXPRESSION_TYPES) return null
        val region = LocalReferenceCollector.innermostRegion(model, LocalSyntax.range(model.fileId, datatype))
        val use = LocalSyntax.typeUse(model.fileId, datatype, region)
        // Text this file may hold differently leaves the position undecided, whatever scope the name is looked up in.
        regionIncomplete(region, use.range)?.let { return WrittenType(use, LocalTypeEvidence.unknown("undecided text", incomplete = it)) }
        return when {
            position === PlSqlGrammar.EXTERNAL_PARAMETER -> WrittenType(use, LocalTypeEvidence.unknown("an external datatype"), external = true)
            // CAST takes a predefined or a schema type, in PL/SQL as in SQL.
            inSql(datatype) || position === SingleRowSqlFunctionsGrammar.CAST_EXPRESSION -> WrittenType(use, walk.schemaType(use))
            else -> WrittenType(use, walk.written(use))
        }
    }

    /**
     * The reasons hidden text gives a name written at [range] and looked up through [region]: in a declarative part of
     * the regions it passes through, or at the use itself; as an incomplete outcome, null when there is none.
     */
    private fun regionIncomplete(region: LocalRegionId, range: SourceRange): LocalResolution.Incomplete? {
        val reasons = EnumSet.noneOf(LocalIncompleteness::class.java)
        LocalResolver.chain(model, region).forEach { reasons += it.completeness.lookupReasons }
        reasons += model.hiddenAt(range)
        return if (reasons.isEmpty()) null else LocalResolution.Incomplete(reasons, emptySet(), LocalResolution.ProvenAbsent)
    }

    /** Inside SQL, outside an `INTO` clause: a SQL statement, a table definition, or an `XMLTABLE`. */
    private fun inSql(node: AstNode): Boolean {
        if (node.parent.type === DdlGrammar.TABLE_COLUMN_DEFINITION || node.parent.type === SingleRowSqlFunctionsGrammar.XML_TABLE_COLUMN) return true
        var current = node.parentOrNull
        while (current != null) {
            if (current.type === DmlGrammar.INTO_CLAUSE) return false
            if (current.type in SQL_STATEMENTS) return true
            current = current.parentOrNull
        }
        return false
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
        /** Positions whose written type is not a declaration of the model: expressions, SQL columns, external parameters. */
        val EXPRESSION_TYPES = setOf(
            SingleRowSqlFunctionsGrammar.TREAT_AS_EXPRESSION, ConditionsGrammar.IS_OF_CONDITION, SingleRowSqlFunctionsGrammar.CAST_EXPRESSION,
            SingleRowSqlFunctionsGrammar.XML_TABLE_COLUMN, DdlGrammar.TABLE_COLUMN_DEFINITION, PlSqlGrammar.EXTERNAL_PARAMETER
        )
        val SQL_STATEMENTS = setOf(
            DmlGrammar.SELECT_EXPRESSION, DmlGrammar.INSERT_EXPRESSION, DmlGrammar.UPDATE_EXPRESSION, DmlGrammar.DELETE_EXPRESSION,
            DmlGrammar.MERGE_EXPRESSION
        )

        /** The gaps of a lookup in an object type whose specification or supertype is in another file. */
        val TYPE_GAPS: Set<LocalLookupGap> = EnumSet.of(LocalLookupGap.SPECIFICATION_NOT_IN_FILE, LocalLookupGap.SUPERTYPE_NOT_IN_FILE)

        /** A name read as a value: in PL/SQL, in SQL, in `VALUES`, or called (an element access or a call). */
        val CONTEXTS = EnumSet.of(
            LocalReferenceContext.VALUE, LocalReferenceContext.SQL_VALUE, LocalReferenceContext.INSERT_VALUE,
            LocalReferenceContext.CALL_EXPRESSION, LocalReferenceContext.SQL_CALL
        )
    }
}
