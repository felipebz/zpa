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
import com.felipebz.zpa.api.project.ProjectBindingCompatibility.Outcome
import com.felipebz.zpa.api.project.ProjectBindingCompatibility.UnknownReason
import com.felipebz.zpa.api.project.ProjectCallNarrower
import com.felipebz.zpa.api.project.ProjectDatatypeFamily
import com.felipebz.zpa.project.*
import com.felipebz.zpa.project.ProjectCallCandidate.Applicability

/** The kind of method a method call is written in: with an instance (a member) or without (a static). */
internal enum class LocalMethodCaller { MEMBER, STATIC }

/** One actual bound to one formal of a local candidate, with the datatype comparison of the two. */
internal class LocalBindingCompatibility(
    /** One-based, in written order. */
    val argumentOrdinal: Int,
    /** One-based, in formal order. */
    val parameterOrdinal: Int,
    val outcome: Outcome,
    val actualFamily: ProjectDatatypeFamily?,
    val formalFamily: ProjectDatatypeFamily?,
    /** Why the outcome is unknown; null otherwise. */
    val reason: UnknownReason?
) {
    override fun toString() = "$argumentOrdinal>$parameterOrdinal $outcome" + (reason?.let { " $it" } ?: "")
}

/** The structural and datatype evaluation of one entity of a local overload set. */
internal class LocalCallCandidate(
    /** The entity's representative, as in [LocalResolution.OverloadSet]. */
    val declaration: LocalDeclarationId,
    val applicability: Applicability,
    /** APPLICABLE only: one per actual, in written order. */
    val bindings: List<LocalBindingCompatibility>,
    /** APPLICABLE: the default-present formals bound to no actual; MISSING_REQUIRED: the formals missing. Ordinals. */
    val omitted: List<Int>,
    val missing: List<Int>,
    val rejectedArgumentOrdinal: Int?
) {
    /** Applicable, with no binding proven incompatible. */
    val retained: Boolean
        get() = applicability == Applicability.APPLICABLE && bindings.none { it.outcome == Outcome.INCOMPATIBLE }

    override fun toString() = "$declaration $applicability" + if (bindings.isEmpty()) "" else " $bindings"
}

/**
 * A call to a local overload set: every entity bound and narrowed as the project binds and narrows its candidates.
 * Narrowing only removes candidates with a binding proven incompatible; source order never chooses and compatible
 * candidates are never ranked.
 */
internal class LocalCallResolution(
    val status: Status,
    /** One per entity of the set, in the set's order; empty for [Status.INCOMPLETE] and [Status.INVALID_NOTATION]. */
    val candidates: List<LocalCallCandidate>,
    /** Present only for [Status.RESOLVED]. */
    val selected: LocalDeclarationId?
) {
    enum class Status {
        /** Exactly one candidate is retained. It may still have unknown bindings. */
        RESOLVED,

        /**
         * Exactly one candidate is retained and it is a static method, called on the instance (named alone or through
         * `SELF` from a member method): Oracle selects it and rejects the call. A rejection, not an absence.
         */
        STATIC_ON_INSTANCE,

        /**
         * Exactly one candidate is retained and it is a member method, called from a static method, which has no
         * instance: Oracle reports wrong number or types of arguments. A rejection, not an absence.
         */
        MEMBER_FROM_STATIC,

        /** Several candidates are retained. */
        AMBIGUOUS,

        /** No candidate is structurally applicable. */
        NO_APPLICABLE_CANDIDATE,

        /** Some candidate is applicable, and every applicable one has a binding proven incompatible. */
        NO_COMPATIBLE_CANDIDATE,

        /** A positional actual follows a named one, or a label is repeated: no candidate is evaluated. */
        INVALID_NOTATION,

        /** The set was found through an incomplete region or a lookup gap: a hidden declaration may join it. */
        INCOMPLETE
    }

    override fun toString() = "$status" + (selected?.let { " $it" } ?: "") + " $candidates"

    companion object {
        val incomplete = LocalCallResolution(Status.INCOMPLETE, emptyList(), null)
    }
}

/**
 * Binds and narrows calls to local overload sets. The structural part is the project's own parameter-list binding,
 * over the parameters every form shows (a call specification's heading included; a wrapped unit is never a member of
 * a set, since it is reached only through the project);
 * a formal's evidence is its written type walked at its declaration site, an actual's the same evidence a forwarded
 * call gets, and the two are compared exactly as the project's narrowing compares them. A package subprogram defined in
 * its body has the defaults of its specification heading too: Oracle applies them to calls inside the body.
 */
internal class LocalOverloads(
    private val model: LocalSemanticModel,
    private val walk: LocalTypeWalk,
    private val shadowed: (QualifiedName, OracleIdentifier) -> Boolean,
    /** For a body definition whose specification heading is in another file, the heading's defaults by parameter. */
    private val specificationDefaults: (LocalDeclarationId) -> List<Boolean>? = { null }
) {
    fun resolve(
        entities: List<LocalDeclarationId>,
        kind: ProjectCallKind,
        arguments: ProjectCallArguments,
        actuals: List<LocalTypeEvidence>,
        /** The kind of method the call is named alone or through `SELF` from; null outside methods. */
        caller: LocalMethodCaller? = null
    ): LocalCallResolution {
        if (arguments.notation != ProjectCallArguments.Notation.VALID) {
            return LocalCallResolution(LocalCallResolution.Status.INVALID_NOTATION, emptyList(), null)
        }
        val actualEvidence = actuals.map { ProjectCallNarrower.actual(it.argumentType, shadowed) }
        val candidates = entities.map { candidate(model.declaration(it), kind, arguments, actualEvidence) }
        val retained = candidates.filter { it.retained }
        val status = when {
            retained.size == 1 && caller == LocalMethodCaller.MEMBER && LocalTypeScope.isStatic(model.declaration(retained.single().declaration)) ->
                LocalCallResolution.Status.STATIC_ON_INSTANCE
            retained.size == 1 && caller == LocalMethodCaller.STATIC && LocalTypeScope.isInstanceMethod(model.declaration(retained.single().declaration)) ->
                LocalCallResolution.Status.MEMBER_FROM_STATIC
            retained.size == 1 -> LocalCallResolution.Status.RESOLVED
            retained.size > 1 -> LocalCallResolution.Status.AMBIGUOUS
            candidates.none { it.applicability == Applicability.APPLICABLE } -> LocalCallResolution.Status.NO_APPLICABLE_CANDIDATE
            else -> LocalCallResolution.Status.NO_COMPATIBLE_CANDIDATE
        }
        return LocalCallResolution(status, candidates, retained.singleOrNull()?.declaration?.takeIf { status == LocalCallResolution.Status.RESOLVED })
    }

    private fun candidate(
        declaration: LocalDeclaration,
        kind: ProjectCallKind,
        arguments: ProjectCallArguments,
        actuals: List<ProjectCallNarrower.Evidence>
    ): LocalCallCandidate {
        val subprogram = declaration.details as LocalSubprogram
        val declaredKind = if (declaration.kind == LocalDeclarationKind.FUNCTION) ProjectCallKind.FUNCTION else ProjectCallKind.PROCEDURE
        // A written SELF never binds to an actual (a label `self =>` is rejected): it is the method's instance.
        val parameters = if (subprogram.methodKind == null) subprogram.parameters else subprogram.parameters.filter { it.name != LocalResolver.SELF }
        val headingDefaults = headingDefaults(declaration, subprogram)
        val hasDefault: (LocalParameter) -> Boolean =
            if (headingDefaults == null) LocalParameter::hasDefault else { parameter -> parameter.hasDefault || headingDefaults.getOrElse(parameter.ordinal - 1) { false } }
        val bound = ParameterListBinding.bind(declaredKind, kind, parameters, LocalParameter::name, hasDefault, arguments)
        val bindings = if (bound.applicability != Applicability.APPLICABLE) emptyList() else actuals.mapIndexed { index, actual ->
            val parameter = parameters[bound.formalIndices[index]]
            val formal = formal(walk.written(parameter.type))
            ProjectCallNarrower.compare(actual, formal) { outcome, reason ->
                LocalBindingCompatibility(index + 1, parameter.ordinal, outcome, actual.family, formal.family, reason)
            }
        }
        return LocalCallCandidate(
            declaration.id, bound.applicability, bindings, bound.omitted.map { it.ordinal }, bound.missing.map { it.ordinal },
            bound.rejectedArgumentOrdinal
        )
    }

    /** The defaults of the package specification heading [declaration] implements, by parameter ordinal; null for none. */
    private fun headingDefaults(declaration: LocalDeclaration, subprogram: LocalSubprogram): List<Boolean>? {
        if (subprogram.methodKind != null || subprogram.form != LocalSubprogramForm.BODY) return null
        subprogram.specificationCounterpart?.let { heading -> return (model.declaration(heading).details as LocalSubprogram).parameters.map { it.hasDefault } }
        return specificationDefaults(declaration.id)
    }

    /** A formal's evidence: the family of its walked type, or inconclusive (the project's reasons for a formal). */
    private fun formal(type: LocalTypeEvidence): ProjectCallNarrower.Evidence {
        val evidence = ProjectCallNarrower.actual(type.argumentType, shadowed)
        if (evidence.family != null) return evidence
        return ProjectCallNarrower.Evidence(
            null, if (type.argumentType.unclassified) UnknownReason.UNCLASSIFIED_TYPE else UnknownReason.FORMAL_INCONCLUSIVE
        )
    }
}
