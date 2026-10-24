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
package com.felipebz.zpa.project

/** How an object method call names its receiver. */
internal enum class ProjectMethodReceiver {
    /** `v.m(…)`: a value of an object type. */
    VALUE,

    /** `self.m(…)`: the implicit or a written `SELF`. */
    SELF,

    /** `t.s(…)`: the type name. */
    TYPE_NAME,

    /** `t(…)`: a constructor call. */
    CONSTRUCTOR
}

/**
 * One candidate of a method call: a method of the set, or the attribute-value constructor ([method] null), with its
 * parameters (an explicit `SELF` excluded) and the structural binding of the call's actuals.
 */
internal class ProjectMethodCallCandidate(
    val type: StandaloneTypeDeclaration,
    val method: ProjectTypeMethod?,
    parameters: List<ProjectParameter>,
    val applicability: ProjectCallCandidate.Applicability,
    /** APPLICABLE only: for each actual, in written order, the zero-based index of its formal. */
    val formalIndices: IntArray
) {
    val parameters: List<ProjectParameter> = immutableList(parameters)

    val isAttributeConstructor: Boolean get() = method == null

    override fun toString() = (method?.let { "${type.name}.${it.name.originalSpelling}/${parameters.size}" } ?: "${type.name}(attributes)/${parameters.size}") +
        " $applicability"
}

internal class ProjectMethodCall(
    val status: Status,
    val receiver: ProjectMethodReceiver,
    val kind: ProjectCallKind,
    candidates: List<ProjectMethodCallCandidate>,
    /** Present for [Status.RESOLVED], [Status.STATIC_ON_INSTANCE] and [Status.MEMBER_ON_TYPE]: the candidate Oracle selects. */
    val selected: ProjectMethodCallCandidate?,
    /** False when a list the candidates come from is incomplete: a hidden overload may exist. */
    val complete: Boolean
) {
    enum class Status {
        RESOLVED,

        /**
         * Several candidates apply and each actual binds to formals of the same written type in all of them, so no
         * datatype tells them apart (PLS-00307). Only with a complete set.
         */
        AMBIGUOUS,

        /** Several candidates apply and their formals differ in type: Oracle chooses by datatypes, which this binding does not compare. */
        SEVERAL_APPLICABLE,

        /** No candidate applies. Only with a complete set. */
        NO_APPLICABLE_CANDIDATE,

        /** Every candidate is of the other kind: a procedure used as a function, or the reverse (PLS-00222, PLS-00221). */
        WRONG_KIND,

        /** The one applicable candidate is a static method called through a value (PLS-00587). A rejection. */
        STATIC_ON_INSTANCE,

        /** The one applicable candidate is a member method called through the type name (PLS-00306). A rejection. */
        MEMBER_ON_TYPE,

        /** The set is incomplete and the call does not bind exactly one candidate: nothing is proven. */
        UNDECIDED,

        /** A positional actual follows a named one, or a label repeats. */
        INVALID_NOTATION
    }

    val candidates: List<ProjectMethodCallCandidate> = immutableList(candidates)

    val isRejection: Boolean
        get() = status == Status.WRONG_KIND || status == Status.STATIC_ON_INSTANCE || status == Status.MEMBER_ON_TYPE

    override fun toString() = "$receiver $kind $status" + (selected?.let { " $it" } ?: "") + (if (complete) "" else " (incomplete)")
}

/**
 * Binds an object method call to one candidate with the structural rules of package calls (kind, arity, named notation,
 * defaults; no datatypes). For a member method `SELF` is the receiver, never an actual.
 */
internal class ProjectMethodCallBinder(private val memberResolver: ProjectRecordMemberResolver) {

    /** A call of the method set [methods] through a value, `SELF` or the type name. */
    fun bind(
        methods: ProjectRecordMemberResolution.Method,
        receiver: ProjectMethodReceiver,
        kind: ProjectCallKind,
        arguments: ProjectCallArguments
    ): ProjectMethodCall {
        require(receiver != ProjectMethodReceiver.CONSTRUCTOR)
        if (arguments.notation != ProjectCallArguments.Notation.VALID) {
            return ProjectMethodCall(ProjectMethodCall.Status.INVALID_NOTATION, receiver, kind, emptyList(), null, methods.complete)
        }
        val candidates = methods.overloads.map { candidate(it.declaringType, it.method, it.method.parameters, it.method.callKind(), kind, arguments) }
        val fitting = candidates.filter { it.applicability == ProjectCallCandidate.Applicability.APPLICABLE }
        // Fitting overloads of several types: the nearest type's win only when no datatype tells them apart; otherwise
        // Oracle chooses by datatype. The set lists the nearest type first.
        val applicable = if (fitting.distinctBy { System.identityHashCode(it.type) }.size > 1 && !indistinguishable(fitting)) fitting
        else fitting.filter { it.type === fitting.firstOrNull()?.type }
        val single = applicable.singleOrNull()
        val status = when {
            single != null && single.method!!.methodKind == ProjectMethodKind.STATIC && receiver != ProjectMethodReceiver.TYPE_NAME ->
                if (methods.complete) ProjectMethodCall.Status.STATIC_ON_INSTANCE else ProjectMethodCall.Status.UNDECIDED
            single != null && single.method!!.methodKind != ProjectMethodKind.STATIC && receiver == ProjectMethodReceiver.TYPE_NAME ->
                if (methods.complete) ProjectMethodCall.Status.MEMBER_ON_TYPE else ProjectMethodCall.Status.UNDECIDED
            single != null -> ProjectMethodCall.Status.RESOLVED
            !methods.complete -> ProjectMethodCall.Status.UNDECIDED
            applicable.size > 1 -> several(applicable)
            candidates.all { it.applicability == ProjectCallCandidate.Applicability.WRONG_KIND } -> ProjectMethodCall.Status.WRONG_KIND
            else -> ProjectMethodCall.Status.NO_APPLICABLE_CANDIDATE
        }
        return ProjectMethodCall(status, receiver, kind, candidates, single.takeIf { status.selects() }, methods.complete)
    }

    /**
     * A constructor call `t(…)`. The user constructors are the candidates; the attribute-value constructor (the
     * attributes, inherited first) applies only when no user constructor does: Oracle prefers a user constructor that
     * fits, and reaches the attribute-value one by named notation when the user's labels differ.
     */
    fun construct(type: StandaloneTypeDeclaration, arguments: ProjectCallArguments): ProjectMethodCall {
        val receiver = ProjectMethodReceiver.CONSTRUCTOR
        val kind = ProjectCallKind.FUNCTION
        val attributes = memberResolver.attributeChain(type)
        val complete = attributes != null && type.methodsComplete && !memberResolver.methodsAltered(type)
        if (arguments.notation != ProjectCallArguments.Notation.VALID) {
            return ProjectMethodCall(ProjectMethodCall.Status.INVALID_NOTATION, receiver, kind, emptyList(), null, complete)
        }
        val users = type.methods.filter { it.methodKind == ProjectMethodKind.CONSTRUCTOR }
            .map { candidate(type, it, it.parameters, ProjectCallKind.FUNCTION, kind, arguments) }
        val defaultConstructor = attributes?.let { fields ->
            val parameters = fields.mapIndexed { index, field ->
                ProjectParameter(field.name, index + 1, ParameterMode.IN, false, field.typeRef, false, field.sourceRange)
            }
            candidate(type, null, parameters, ProjectCallKind.FUNCTION, kind, arguments)
        }
        val applicableUsers = users.filter { it.applicability == ProjectCallCandidate.Applicability.APPLICABLE }
        val candidates = users + listOfNotNull(defaultConstructor)
        val (status, selected) = when {
            // A fitting user constructor wins over the attribute-value one only when no datatype tells them apart.
            applicableUsers.size == 1 && defaultConstructor != null &&
                defaultConstructor.applicability == ProjectCallCandidate.Applicability.APPLICABLE && !indistinguishable(applicableUsers + defaultConstructor) ->
                (if (complete) ProjectMethodCall.Status.SEVERAL_APPLICABLE else ProjectMethodCall.Status.UNDECIDED) to null
            applicableUsers.size == 1 -> ProjectMethodCall.Status.RESOLVED to applicableUsers.single()
            applicableUsers.size > 1 -> (if (complete) several(applicableUsers) else ProjectMethodCall.Status.UNDECIDED) to null
            !complete || defaultConstructor == null -> ProjectMethodCall.Status.UNDECIDED to null
            defaultConstructor.applicability == ProjectCallCandidate.Applicability.APPLICABLE -> ProjectMethodCall.Status.RESOLVED to defaultConstructor
            else -> ProjectMethodCall.Status.NO_APPLICABLE_CANDIDATE to null
        }
        return ProjectMethodCall(status, receiver, kind, candidates, selected, complete)
    }

    private fun candidate(
        type: StandaloneTypeDeclaration,
        method: ProjectTypeMethod?,
        parameters: List<ProjectParameter>,
        declaredKind: ProjectCallKind,
        kind: ProjectCallKind,
        arguments: ProjectCallArguments
    ): ProjectMethodCallCandidate {
        val bound = ParameterListBinding.bind(declaredKind, kind, parameters, ProjectParameter::name, ProjectParameter::defaultPresent, arguments)
        return ProjectMethodCallCandidate(type, method, parameters, bound.applicability, bound.formalIndices)
    }

    private fun several(applicable: List<ProjectMethodCallCandidate>): ProjectMethodCall.Status =
        if (indistinguishable(applicable)) ProjectMethodCall.Status.AMBIGUOUS else ProjectMethodCall.Status.SEVERAL_APPLICABLE

    /** Each actual binds to formals of the same written type in every applicable candidate. */
    private fun indistinguishable(applicable: List<ProjectMethodCallCandidate>): Boolean {
        val first = applicable.first()
        return applicable.all { candidate ->
            candidate.formalIndices.indices.all { actual ->
                candidate.parameters[candidate.formalIndices[actual]].typeRef.structuralKey() ==
                    first.parameters[first.formalIndices[actual]].typeRef.structuralKey()
            }
        }
    }

    private fun ProjectTypeMethod.callKind() = if (isFunction) ProjectCallKind.FUNCTION else ProjectCallKind.PROCEDURE

    private fun ProjectMethodCall.Status.selects() = this == ProjectMethodCall.Status.RESOLVED ||
        this == ProjectMethodCall.Status.STATIC_ON_INSTANCE || this == ProjectMethodCall.Status.MEMBER_ON_TYPE
}
