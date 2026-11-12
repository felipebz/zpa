//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.QualifiedName
import com.felipebz.zpa.project.SourceRange
import com.felipebz.zpa.project.immutableList
import com.felipebz.zpa.symbols.local.LocalReferenceContext as Ctx

/** One written actual with its datatype evidence. */
internal class LocalActualEvidence(val actual: LocalActual, val evidence: LocalTypeEvidence) {
    override fun toString() = "$actual: $evidence"
}

internal enum class LocalCallOutcome {
    /** The first segment is proven absent: the project resolved and narrowed the call. */
    PROJECT,

    /**
     * The first segment is declared locally (a callee, or a capture of the qualifier): the project is not consulted.
     * A local callee's resolution is in [LocalCallAnalysis.local].
     */
    LOCAL_CAPTURE,

    /** The first segment's outcome is incomplete: a region or a lookup gap may hide a local declaration. */
    INCOMPLETE,

    /** The first segment's outcome, or the call's form, is not modeled. */
    UNSUPPORTED,

    /** In SQL, a relation of the statement takes the first segment: `alias.column.method(x)`, or `alias.m(x)` with no function `alias.m` in the project. */
    SQL_RELATION,

    /**
     * In SQL, `alias.m(x)` where the project has a function of that name: Oracle calls the object's method when the
     * relation is an object table, and the package's function otherwise. The relation's kind is not known here.
     */
    SQL_RELATION_OR_PROJECT,

    /**
     * In SQL, the name binds functions of the file that are not stored (declared in a subprogram or block, or private to
     * a package body): Oracle rejects the statement (PLS-00231).
     */
    NOT_CALLABLE_IN_SQL
}

/**
 * A call from local code: its written target, the first segment's outcome, the evidence of each actual, the
 * project's typed resolution when the call was forwarded, and the local resolution when the callee is a local
 * overload set.
 */
internal class LocalCallAnalysis(
    val reference: LocalReference,
    val target: QualifiedName,
    val kind: ProjectCallKind,
    val firstSegment: LocalResolution,
    val outcome: LocalCallOutcome,
    /** Empty when the call form is not modeled. */
    actuals: List<LocalActualEvidence>,
    /** Present only for [LocalCallOutcome.PROJECT]. */
    val typed: ProjectTypedCallResolution?,
    /**
     * Present when the whole name resolves to a local overload set of subprograms (not object type methods), or to one
     * provisionally ([LocalCallResolution.Status.INCOMPLETE]).
     */
    val local: LocalCallResolution?
) {
    val actuals: List<LocalActualEvidence> = immutableList(actuals)

    override fun toString() = "$kind $target: $outcome ($firstSegment) $actuals" + (local?.let { " -> $it" } ?: "")
}

/**
 * Forwards calls whose callee is not local to the Project System: [ProjectSnapshot.resolveCall] on the written target,
 * then [ProjectSnapshot.narrowCall] with one piece of local evidence per written actual. Only a first segment proven
 * absent reaches the project (by the file alone, or with the project's coverage of a package body's specification in
 * another file), and a member of that specification, named through its package; any other outcome keeps the call local,
 * and the outcome says why.
 */
internal class LocalForwardCalls(
    private val model: LocalSemanticModel,
    private val references: List<LocalReference>,
    calls: List<LocalCall>,
    private val snapshot: ProjectSnapshot,
    /** The run's external metadata and compilation schema. */
    private val external: ProjectExternalContext = ProjectExternalContext.none(),
    /** The references' outcomes; by default the resolver's, refined with the project's specification coverage. */
    private val outcomes: LocalReferenceOutcomes = LocalReferenceOutcomes(model, LocalSpecificationCoverage.of(model, snapshot))
) {
    private val walk = LocalTypeWalk(model, snapshot, external, outcomes)
    private val overloads = LocalOverloads(model, walk, ProjectCallNarrower.shadowCheck(snapshot), outcomes::specificationDefaults)
    private val callsByPosition = calls.associateBy { it.line to it.column }
    private val namesByPosition = references.filter { it.context != Ctx.TYPE && it.context != Ctx.ANCHOR_TYPE && it.context != Ctx.ANCHOR_ROWTYPE }
        .groupBy { it.range.startLine to it.range.startColumn }

    private val analyses = HashMap<LocalReference, LocalCallAnalysis?>()

    /** Every call of the file, in reference order. */
    fun analyze(): List<LocalCallAnalysis> = references.mapNotNull(::callOf)

    /**
     * The types of the actuals of each call whose name no reference starts at, a method called after another call or
     * an element (`f(…).m(…)`), by the position of that name; each actual's evidence is read from the innermost region
     * holding the call.
     */
    fun itemActualTypes(): Map<Pair<Int, Int>, List<ProjectArgumentType>> =
        callsByPosition.filter { (position, call) -> call.actuals != null && position !in namesByPosition }.mapValues { (position, call) ->
            val region = LocalReferenceCollector.innermostRegion(model, SourceRange(model.fileId, position.first, position.second, position.first, position.second))
            call.actuals!!.map { evidence(it, region).argumentType }
        }

    /** The analysis of [reference] when it is a call, computed once: an actual may be the result of another call. */
    private fun callOf(reference: LocalReference): LocalCallAnalysis? {
        if (reference in analyses) return analyses[reference]
        analyses[reference] = null // a call never contains itself; this only stops a malformed tree
        return kindOf(reference)?.let { analyze(reference, it) }.also { analyses[reference] = it }
    }

    private fun kindOf(reference: LocalReference): ProjectCallKind? {
        if (reference.segments.any { it.isAttribute }) return null
        return when (reference.lastSegmentContext) {
            Ctx.CALL_STATEMENT -> ProjectCallKind.PROCEDURE
            Ctx.CALL_EXPRESSION -> ProjectCallKind.FUNCTION
            // A SQL built-in takes an unqualified name written with arguments, whatever the file declares.
            Ctx.SQL_CALL -> if (reference.segments.isEmpty() && SqlBuiltinFunctions.mayTake(reference.name)) null else ProjectCallKind.FUNCTION
            // A name without parentheses: a call of a local function, or of a project function when the project shows a
            // function and no value of that name.
            Ctx.VALUE -> if (localCallee(reference) != null || reference.segments.isNotEmpty() &&
                LocalReferenceOutcomes.isAbsent(outcomes.first(reference)) && isProjectFunction(target(reference), reference.region)
            ) ProjectCallKind.FUNCTION else null
            else -> null
        }
    }

    /**
     * The local overload set the whole name resolves to, with whether it was found through complete regions only.
     * Methods qualify only inside a type body, named alone (`m(…)`) or through `SELF` (`self.m(…)`); constructors never
     * do. Methods reached through a value or a type name are not resolved here.
     */
    private fun localCallee(reference: LocalReference): Pair<LocalResolution.OverloadSet, Boolean>? {
        val steps = outcomes.steps(reference)
        if (steps.size != reference.segments.size + 1) return null
        val set = when (val last = provisional(steps.last())) {
            is LocalResolution.OverloadSet -> last
            is LocalResolution.SqlBuiltinOrLocal -> LocalResolution.OverloadSet(last.declarations).takeIf { set ->
                set.declarations.all { model.declaration(it).kind == LocalDeclarationKind.FUNCTION }
            }
            else -> null
        } ?: return null
        val methods = set.declarations.map { (model.declaration(it).details as LocalSubprogram).methodKind }
        val complete = steps.none { it is LocalResolution.Incomplete }
        if (methods.none { it != null }) return set to complete
        if (methods.any { it == null || it == LocalMethodKind.CONSTRUCTOR } || !inTypeBody(steps)) return null
        val body = LocalResolver.chain(model, reference.region).firstOrNull { it.kind == LocalRegionKind.TYPE_BODY } ?: return null
        return LocalResolution.OverloadSet(LocalTypeScope.withoutOverridden(model, body, set.declarations)) to complete
    }

    /** For a method set (named alone or through `SELF`), the kind of method the call is written in. */
    private fun caller(reference: LocalReference, set: LocalResolution.OverloadSet): LocalMethodCaller? {
        if ((model.declaration(set.declarations.first()).details as LocalSubprogram).methodKind == null) return null
        val method = LocalTypeScope.method(model, LocalResolver.chain(model, reference.region)) ?: return null
        return if (method.isStatic) LocalMethodCaller.STATIC else LocalMethodCaller.MEMBER
    }

    private fun provisional(outcome: LocalResolution) = when (outcome) {
        is LocalResolution.Incomplete -> outcome.provisional
        is LocalResolution.SpecificationCovered -> outcome.outcome
        else -> outcome
    }

    /** A method named alone, or through the implicit `SELF` or a written `SELF` parameter, inside a type body. */
    private fun inTypeBody(steps: List<LocalResolution>): Boolean = when (steps.size) {
        1 -> true // a method set named alone is found only from the type scope of a type body
        2 -> when (val first = provisional(steps.first())) {
            is LocalResolution.ImplicitSelf -> true
            is LocalResolution.Bound -> model.declaration(first.declaration).let { it.kind == LocalDeclarationKind.PARAMETER && it.name == LocalResolver.SELF }
            else -> false
        }
        else -> false
    }

    private fun ProjectCallKind.internal() = when (this) {
        ProjectCallKind.PROCEDURE -> com.felipebz.zpa.project.ProjectCallKind.PROCEDURE
        ProjectCallKind.FUNCTION -> com.felipebz.zpa.project.ProjectCallKind.FUNCTION
    }

    private fun target(reference: LocalReference) = QualifiedName(listOf(reference.name) + reference.segments.map { it.name })

    /** A member of the enclosing package's specification in another file, named as the project knows it. */
    private fun memberTarget(reference: LocalReference): QualifiedName? = walk.specificationMember(target(reference).segments, reference.region)

    /** Whether the project shows a function or procedure named [name]. */
    private fun isProjectCallable(name: QualifiedName, region: LocalRegionId): Boolean {
        val candidates = snapshot.preferring(walk.compilationSchema(region)).findCallCandidates(ProjectCallTarget.written(ProjectQualifiedName.wrap(name)))
        return candidates.status == ProjectCallCandidates.Status.FOUND && candidates.candidates.isNotEmpty()
    }

    /**
     * Whether the project knows [name] as a function and not as a package value: a certain candidate search with a
     * function candidate, and no package value of that name.
     */
    private fun isProjectFunction(name: QualifiedName, region: LocalRegionId): Boolean {
        val written = ProjectQualifiedName.wrap(name)
        val candidates = snapshot.preferring(walk.compilationSchema(region)).findCallCandidates(ProjectCallTarget.written(written))
        if (candidates.status != ProjectCallCandidates.Status.FOUND || candidates.uncertainties.isNotEmpty() ||
            candidates.candidates.none { it is ProjectFunctionView }
        ) return false
        val value = snapshot.preferring(walk.compilationSchema(region)).resolvePackageValue(written, walk.packageOwner(region))
        return value.status == ProjectValueResolution.Status.NOT_FOUND_IN_PROJECT && value.candidates.none { it is ProjectPackageValueView }
    }

    private fun analyze(reference: LocalReference, kind: ProjectCallKind): LocalCallAnalysis {
        val first = outcomes.first(reference)
        val call = callsByPosition[reference.range.startLine to reference.range.startColumn]
        val (outcome, viaSpecification) =
            if (reference.lastSegmentContext == Ctx.SQL_CALL) sqlDecision(reference, first, call) else plsqlDecision(reference, first, call)
        val target = (if (viaSpecification) memberTarget(reference) else null) ?: target(reference)
        val actuals = call?.actuals?.map { LocalActualEvidence(it, evidence(it, reference.region)) }.orEmpty()
        val notation = ProjectCallArguments.of(actuals.map { actual ->
            actual.actual.label?.let { ProjectCallArgument.named(ProjectIdentifier.fromSource(it.originalSpelling)) } ?: ProjectCallArgument.positional()
        })
        if (outcome != LocalCallOutcome.PROJECT) {
            val local = if (call != null && call.actuals == null) null else localCallee(reference)?.let { (set, complete) ->
                if (!complete) LocalCallResolution.incomplete
                else overloads.resolve(set.declarations, kind.internal(), notation.internal, actuals.map { it.evidence }, caller(reference, set))
            }
            return LocalCallAnalysis(reference, target, kind, first, outcome, actuals, null, local)
        }
        val structural = snapshot.preferring(walk.compilationSchema(reference.region)).resolveCall(ProjectCallTarget.written(ProjectQualifiedName.wrap(target)), kind, notation)
        val typed = snapshot.narrowCall(structural, actuals.map { it.evidence.argumentType }, external.metadata, walk.compilationSchema(reference.region))
        return LocalCallAnalysis(reference, target, kind, first, outcome, actuals, typed, null)
    }

    /** The outcome of a call written in PL/SQL, and whether it names a member of the specification in another file. */
    private fun plsqlDecision(reference: LocalReference, first: LocalResolution, call: LocalCall?): Pair<LocalCallOutcome, Boolean> {
        val member = LocalReferenceOutcomes.isSpecificationMember(first)
        val outcome = when {
            // A member of an object type's specification has no package target: a method set this analysis does not bind.
            member && memberTarget(reference) == null -> LocalCallOutcome.UNSUPPORTED
            LocalReferenceOutcomes.isAbsent(first) || member -> if (call == null || call.actuals != null) LocalCallOutcome.PROJECT else LocalCallOutcome.UNSUPPORTED
            first is LocalResolution.Incomplete -> LocalCallOutcome.INCOMPLETE
            first is LocalResolution.Unsupported -> LocalCallOutcome.UNSUPPORTED
            else -> LocalCallOutcome.LOCAL_CAPTURE
        }
        return outcome to member
    }

    /** As [plsqlDecision], for a call written in SQL; see [sqlOutcome]. */
    private fun sqlDecision(reference: LocalReference, first: LocalResolution, call: LocalCall?): Pair<LocalCallOutcome, Boolean> {
        if (specificationUndecided(first)) return LocalCallOutcome.INCOMPLETE to false
        val viaSpecification = LocalReferenceOutcomes.isSpecificationMember(first) || sqlSpecificationMember(first) || publicInOtherSpecification(first)
        val outcome = sqlOutcome(reference, first, viaSpecification)
        return (if (outcome == LocalCallOutcome.PROJECT && call?.actuals == null) LocalCallOutcome.UNSUPPORTED else outcome) to viaSpecification
    }

    /**
     * The outcome of a call written in SQL. A column never takes the first segment of a call: a column's method is
     * written after the relation's alias. A relation of the statement takes it before a member and a method
     * (`alias.column.method(x)`). `alias.m(x)` is the method of an object table's rows, else a function of the package
     * of that name: without such a function in the project the relation takes it, with one the kind of the relation
     * decides.
     */
    private fun sqlOutcome(reference: LocalReference, first: LocalResolution, member: Boolean): LocalCallOutcome {
        if (member) return if (memberTarget(reference) == null) LocalCallOutcome.UNSUPPORTED else LocalCallOutcome.PROJECT
        return when (val decided = covered(first)) {
            LocalResolution.ProvenAbsent -> LocalCallOutcome.PROJECT
            LocalResolution.SqlRelation ->
                if (reference.segments.size == 1 && isProjectCallable(target(reference), reference.region)) LocalCallOutcome.SQL_RELATION_OR_PROJECT
                else LocalCallOutcome.SQL_RELATION
            is LocalResolution.LocalFunctionInSql -> LocalCallOutcome.NOT_CALLABLE_IN_SQL
            is LocalResolution.Incomplete ->
                if (onlyColumnGap(decided) && decided.provisional == LocalResolution.ProvenAbsent) LocalCallOutcome.PROJECT else LocalCallOutcome.INCOMPLETE
            is LocalResolution.Unsupported -> LocalCallOutcome.UNSUPPORTED
            else -> LocalCallOutcome.LOCAL_CAPTURE
        }
    }

    /**
     * Functions of a package body whose specification is in another file: callable in SQL when the specification
     * declares them, as a call of that specification's function.
     */
    private fun bodyFunctions(first: LocalResolution): List<LocalDeclarationId>? =
        (covered(first) as? LocalResolution.LocalFunctionInSql)?.declarations?.takeIf { declarations ->
            declarations.all { id ->
                val region = model.region(model.declaration(id).region)
                region.kind == LocalRegionKind.PACKAGE_BODY && LocalTypeScope.unitOf(model, region.id)?.counterpart == null
            }
        }

    private fun publicInOtherSpecification(first: LocalResolution) = bodyFunctions(first)?.all { outcomes.declaredBySpecification(it) == true } == true

    private fun specificationUndecided(first: LocalResolution) = bodyFunctions(first)?.any { outcomes.declaredBySpecification(it) == null } == true

    /** An unqualified SQL call whose only reading besides a column is a member of the specification in another file. */
    private fun sqlSpecificationMember(first: LocalResolution): Boolean =
        (covered(first) as? LocalResolution.Incomplete)?.let { onlyColumnGap(it) && it.provisional == LocalResolution.SpecificationMember } == true

    private fun onlyColumnGap(outcome: LocalResolution.Incomplete) =
        outcome.regionReasons.isEmpty() && outcome.lookupReasons == setOf(LocalLookupGap.SQL_COLUMN_UNKNOWN)

    private fun covered(outcome: LocalResolution) = (outcome as? LocalResolution.SpecificationCovered)?.outcome ?: outcome

    /**
     * A name `v.m1.m2…` written without parentheses whose first segment is a local value and whose members the
     * resolver leaves to typing: the members are walked from the value's type ([LocalTypeWalk.memberPath]). COUNT of a
     * collection the resolver reached is a PLS_INTEGER, as there. Null for any other shape.
     */
    private fun memberPath(reference: LocalReference, steps: List<LocalResolution>, actual: LocalActual): LocalTypeEvidence? {
        if (reference.segments.isEmpty() || reference.segments.any { it.isAttribute } || steps.size < 2) return null
        if (actual.isCall) {
            // `v.m(…)`: an element of the member when it is a collection indexed once, else the result of the method.
            val call = callsByPosition[actual.nameLine to actual.nameColumn]?.actuals ?: return null
            val path = memberPath(reference, steps, LocalActual(null, null, actual.nameLine, actual.nameColumn)) ?: return null
            if (path.terminal != LocalTypeEvidence.Terminal.UNKNOWN && call.size == 1) return walk.element(path, reference.region)
            val first = steps.first().let { (it as? LocalResolution.SpecificationCovered)?.outcome ?: it } as? LocalResolution.Bound ?: return null
            val owner = walk.memberPath(walk.outcome(first), reference.segments.dropLast(1).map { it.name }, reference.region)
            return walk.methodResult(owner, reference.segments.last().name, arguments(call.map { it.label }), call.map { evidence(it, reference.region).argumentType }, reference.region)
        }
        // COUNT of a local collection, which the resolver already found to be a collection method.
        if (steps.size == reference.segments.size + 1 && steps.last() is LocalResolution.CollectionMethod && reference.segments.last().name.lookupName == "COUNT") {
            return LocalTypeEvidence(ProjectArgumentType.builtIn("PLS_INTEGER"), LocalTypeEvidence.Terminal.BUILT_IN, emptyList(), null)
        }
        if ((steps[1] as? LocalResolution.Unsupported)?.construct != LocalUnsupportedConstruct.MEMBER_NEEDS_TYPING) return null
        val first = steps.first().let { (it as? LocalResolution.SpecificationCovered)?.outcome ?: it } as? LocalResolution.Bound ?: return null
        return walk.memberPath(walk.outcome(first), reference.segments.map { it.name }, reference.region)
    }

    /**
     * Evidence of one actual: a literal as Oracle types it; a name, an element or a call's result through the resolver,
     * the type walk and, for a project callee, the call's own resolution.
     */
    private fun evidence(actual: LocalActual, region: LocalRegionId): LocalTypeEvidence {
        actual.form?.let { return form(it, region) }
        if (!actual.isName) return LocalTypeEvidence.literal(actual.literal).takeIf { actual.literal != null }
            ?: LocalTypeEvidence.unknown("expression not modeled")
        val reference = namesByPosition[actual.nameLine to actual.nameColumn]?.singleOrNull()
            ?: return LocalTypeEvidence.unknown("name not collected")
        if (reference.segments.any { it.isAttribute }) return LocalTypeEvidence.unknown("attribute")
        val steps = outcomes.steps(reference)
        val first = steps.first()
        if (LocalReferenceOutcomes.isSpecificationMember(first)) {
            callOf(reference)?.let { return walk.projectResult(it.typed, reference.region) }
            return memberTarget(reference)?.let { walk.projectValue(it, reference.region) } ?: LocalTypeEvidence.unknown("no package")
        }
        if (LocalReferenceOutcomes.isAbsent(first)) return absentName(reference, actual)
        memberPath(reference, steps, actual)?.let { return it }
        if (steps.size != reference.segments.size + 1) return LocalTypeEvidence.unknown("stopped at ${steps.last()}")
        // `pkg.member` through the package's own name, the member in the specification in another file.
        if (LocalReferenceOutcomes.isSpecificationMember(steps.last())) {
            callOf(reference)?.let { return walk.projectResult(it.typed, reference.region) }
            return walk.projectValue(target(reference), reference.region)
        }
        callOf(reference)?.local?.let { local ->
            return local.selected?.let(walk::result) ?: LocalTypeEvidence.unknown("the local call is ${local.status}")
        }
        // A decision made with the project's specification coverage is still the body's: its declaration is local.
        val last = steps.last().let { (it as? LocalResolution.SpecificationCovered)?.outcome ?: it }.let { decided ->
            // Over DUAL alone no column but DUMMY exists: the local declaration takes the name, as nameBinding decides.
            if (decided is LocalResolution.SqlColumnOrLocal && reference.segments.isEmpty() && reference.sql?.excludesColumn(reference.name) == true) {
                LocalResolution.Bound(decided.declaration)
            } else decided
        }
        if (!actual.isCall) return walk.outcome(last)
        val arguments = callsByPosition[actual.nameLine to actual.nameColumn]?.actuals?.size ?: 0
        val element = walk.call(last, arguments)
        // `v(i)` of a value of a project collection type: its element.
        if (element.terminal == LocalTypeEvidence.Terminal.UNKNOWN && last is LocalResolution.Bound && arguments == 1) {
            return walk.element(walk.outcome(last), reference.region)
        }
        return element
    }

    /** Evidence of a name or call whose first segment no local declaration takes. */
    private fun absentName(reference: LocalReference, actual: LocalActual): LocalTypeEvidence {
        builtInValue(reference, actual)?.let { return it }
        callOf(reference)?.let { call ->
            val result = walk.projectResult(call.typed, reference.region)
            // An object type's constructor, user-defined or the default one: nothing local takes the name.
            if (result.terminal == LocalTypeEvidence.Terminal.UNKNOWN && actual.isCall) walk.constructorResult(target(reference), reference.region)?.let { return it }
            return result
        }
        // A one-segment name could be a parameterless function or a built-in: only a qualified one names a value.
        return if (reference.segments.isEmpty() || actual.isCall) LocalTypeEvidence.unknown("not a local name")
        else walk.projectValue(target(reference), reference.region)
    }

    /**
     * Evidence of an actual written as a condition, a concatenation, a `TREAT`, members after an element, a method
     * called on a constructor, an element or a `TREAT`, an element of a call's result, a signed number or a cursor
     * attribute.
     */
    private fun form(form: LocalActualForm, region: LocalRegionId): LocalTypeEvidence = when (form) {
        LocalActualForm.Predicate -> builtIn("BOOLEAN")
        is LocalActualForm.Concatenation -> concatenation(form.operands.map { evidence(it, region) })
        is LocalActualForm.Treat -> treat(form.type, region)
        is LocalActualForm.Members -> walk.memberPath(evidence(form.base, region), form.members, region)
        is LocalActualForm.Method -> {
            val owner = walk.memberPath(evidence(form.base, region), form.members, region)
            walk.methodResult(owner, form.method, arguments(form.actuals.map { it.label }), form.actuals.map { evidence(it, region).argumentType }, region)
        }
        // Typed only through the call's result: the RETURN type of the function it binds, as for any call actual.
        is LocalActualForm.Element -> (1..form.indexes).fold(evidence(form.base, region)) { current, _ -> walk.element(current, region) }
        is LocalActualForm.Signed -> signed(evidence(form.operand, region))
        // Oracle rejects an attribute of anything but a cursor (PLS-00324), and no declaration takes an attribute's name.
        is LocalActualForm.CursorAttribute -> builtIn(if (form.rowCount) "NUMBER" else "BOOLEAN")
    }

    /**
     * `-x` or `+x` has the type of a numeric `x`, its exact type included: Oracle prefers PLS_INTEGER for `-1` and for
     * `-n` of a PLS_INTEGER, NUMBER for `-1.5`. Any other operand is not typed.
     */
    private fun signed(operand: LocalTypeEvidence): LocalTypeEvidence =
        if (operand.argumentType.familyOrNull != ProjectDatatypeFamily.NUMERIC) LocalTypeEvidence.unknown("a signed operand that is not a number")
        else LocalTypeEvidence(operand.argumentType, operand.terminal, emptyList(), null, operand.description)

    /** The notation of a call's actuals, positional or named by [labels]. */
    private fun arguments(labels: List<OracleIdentifier?>) = ProjectCallArguments.of(labels.map { label ->
        label?.let { ProjectCallArgument.named(ProjectIdentifier.fromSource(it.originalSpelling)) } ?: ProjectCallArgument.positional()
    }).internal

    /**
     * The type of `a || b`: VARCHAR2 when every operand is a character value, a number, a date or a time,
     * or NULL; CLOB when some operand is a CLOB and the others are character values or NULL. Anything else (an operand
     * of no type, a BLOB, a RAW, an interval) is not typed.
     */
    private fun concatenation(operands: List<LocalTypeEvidence>): LocalTypeEvidence {
        val families = operands.map { if (it.argumentType.kind == ProjectArgumentType.Kind.NULL_LITERAL) null else it.argumentType.familyOrNull ?: return LocalTypeEvidence.unknown("an operand of no type") }
        val known = families.filterNotNull()
        return when {
            known.all { it in VARCHAR2_OPERANDS } -> builtIn("VARCHAR2")
            ProjectDatatypeFamily.CLOB in known && known.all { it == ProjectDatatypeFamily.CLOB || it == ProjectDatatypeFamily.CHARACTER } -> builtIn("CLOB")
            else -> LocalTypeEvidence.unknown("a concatenation of ${known.distinct()}")
        }
    }

    /**
     * `TREAT(x AS t)` is a value of `t`, whatever `x` is (NULL when `x` is not one). Typed only for a qualified name the
     * project describes as a type created with CREATE TYPE: a local or package declaration never takes such a name.
     */
    private fun treat(type: QualifiedName, region: LocalRegionId): LocalTypeEvidence {
        if (type.segments.size < 2) return LocalTypeEvidence.unknown("a TREAT type that a local declaration may take")
        return walk.schemaTypeValue(type, region) ?: LocalTypeEvidence.unknown("a TREAT type the project does not describe")
    }

    /**
     * A built-in function written without parentheses in PL/SQL, when no local or package declaration takes the name. A
     * schema-level function of the same name never does: STANDARD's is called (and SYSDATE and USER cannot even be
     * created unquoted).
     */
    private fun builtInValue(reference: LocalReference, actual: LocalActual): LocalTypeEvidence? {
        // In SQL a column may take the name.
        if (reference.segments.isNotEmpty() || actual.isCall || reference.name.quoted || reference.context != Ctx.VALUE) return null
        return BUILT_IN_VALUES[reference.name.lookupName]?.let(::builtIn)
    }

    private fun builtIn(typeName: String) = LocalTypeEvidence(ProjectArgumentType.builtIn(typeName), LocalTypeEvidence.Terminal.BUILT_IN, emptyList(), null)

    private companion object {
        /** The type of each built-in. */
        val BUILT_IN_VALUES = mapOf(
            "SYSDATE" to "DATE", "CURRENT_DATE" to "DATE", "SYSTIMESTAMP" to "TIMESTAMP WITH TIME ZONE",
            "SQLCODE" to "PLS_INTEGER", "SQLERRM" to "VARCHAR2", "USER" to "VARCHAR2"
        )
        val VARCHAR2_OPERANDS = setOf(ProjectDatatypeFamily.CHARACTER, ProjectDatatypeFamily.NUMERIC, ProjectDatatypeFamily.DATETIME)
    }
}
