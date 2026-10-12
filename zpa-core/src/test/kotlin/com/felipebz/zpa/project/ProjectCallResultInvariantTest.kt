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

import com.felipebz.zpa.project.ProjectCallTestFixtures.arguments
import com.felipebz.zpa.project.ProjectCallTestFixtures.context
import com.felipebz.zpa.project.ProjectCallTestFixtures.index
import com.felipebz.zpa.project.ProjectCallTestFixtures.member
import com.felipebz.zpa.project.ProjectCallTestFixtures.packageSource
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import java.util.*

class ProjectCallResultInvariantTest {
    private fun resolved() = ProjectCallBinder(context(packageSource("PROCEDURE run(a NUMBER, b NUMBER DEFAULT 1);")))
        .resolve(member(), ProjectCallKind.PROCEDURE, arguments(null))

    private fun resolutionCopy(
        result: ProjectCallResolution,
        status: ProjectCallResolution.Status = result.status,
        candidates: Collection<ProjectCallCandidate> = result.candidates,
        uncertainties: Set<ProjectCallUncertainty> = result.uncertainties,
        specifications: Collection<IndexedDeclaration> = result.packageSpecifications,
        selected: ProjectCallCandidate? = result.selected,
        failures: Collection<ProjectIndexPreparationFailure> = result.failures,
        packageEvidence: Boolean = result.packageEvidence,
        actuals: ProjectCallArguments = result.arguments
    ) = ProjectCallResolution(status, result.target, result.kind, actuals, candidates, uncertainties, specifications, selected, failures, packageEvidence)

    private fun candidateCopy(
        candidate: ProjectCallCandidate,
        applicability: ProjectCallCandidate.Applicability = candidate.applicability,
        bindings: Collection<ProjectArgumentBinding> = candidate.bindings,
        omitted: Collection<ProjectParameter> = candidate.omittedParameters,
        missing: Collection<ProjectParameter> = candidate.missingParameters,
        rejectedOrdinal: Int? = candidate.rejectedArgumentOrdinal,
        declaration: IndexedDeclaration = candidate.declaration,
        actuals: ProjectCallArguments = candidate.arguments,
        kind: ProjectCallKind = candidate.kind
    ) = ProjectCallCandidate(declaration, kind, actuals, applicability, bindings, omitted, missing, rejectedOrdinal)

    private fun illegal(action: () -> Unit) = assertThatThrownBy(action).isInstanceOf(IllegalArgumentException::class.java)

    @Test
    fun requestsCandidatePayloadsAndResultCollectionsAreCopiedAndUnmodifiable() {
        val mutableActuals = mutableListOf(ProjectCallArgument.positional())
        val actuals = ProjectCallArguments.of(mutableActuals)
        mutableActuals.clear()
        assertThat(actuals.arguments).hasSize(1)
        mutationFails(actuals.arguments)
        val original = resolved()
        val candidate = original.selected!!
        val bindings = candidate.bindings.toMutableList()
        val omitted = candidate.omittedParameters.toMutableList()
        val candidateCopy = candidateCopy(candidate, bindings = bindings, omitted = omitted)
        bindings.clear()
        omitted.clear()
        assertThat(candidateCopy.bindings).hasSize(1)
        assertThat(candidateCopy.omittedParameters).hasSize(1)
        mutationFails(candidateCopy.bindings)
        mutationFails(candidateCopy.omittedParameters)
        mutationFails(candidateCopy.missingParameters)

        val candidates = mutableListOf(candidateCopy)
        val specifications = original.packageSpecifications.toMutableList()
        val uncertainties = mutableSetOf<ProjectCallUncertainty>()
        val failures = mutableListOf<ProjectIndexPreparationFailure>()
        val copy = resolutionCopy(original, candidates = candidates, specifications = specifications,
            uncertainties = uncertainties, selected = candidateCopy, failures = failures)
        candidates.clear()
        specifications.clear()
        uncertainties += ProjectCallUncertainty.PARTIAL_DECLARATIONS
        failures += ProjectIndexPreparationFailure(FileId("failure.sql"), "ExampleException")
        assertThat(copy.candidates).containsExactly(candidateCopy)
        assertThat(copy.packageSpecifications).hasSize(1)
        assertThat(copy.uncertainties).isEmpty()
        assertThat(copy.failures).isEmpty()
        mutationFails(copy.candidates)
        mutationFails(copy.packageSpecifications)
        mutationFails(copy.failures)
        assertThatThrownBy { (copy.uncertainties as MutableSet<ProjectCallUncertainty>).add(ProjectCallUncertainty.PARTIAL_DECLARATIONS) }
            .isInstanceOf(UnsupportedOperationException::class.java)

        val failedContext = context(listOf("spec.sql" to packageSource("PROCEDURE run; PROCEDURE run(t TABLE) PROCEDURE coverage_recovery;")), failed = true)
        val discovery = ProjectCallCandidateFinder(failedContext).find(member())
        val discoveryCandidates = discovery.candidates.toMutableList()
        val discoverySpecs = discovery.packageSpecifications.toMutableList()
        val discoveryUncertainties = discovery.uncertainties.toMutableSet()
        val discoveryFailures = discovery.failures.toMutableList()
        val discoveryCopy = ProjectCallCandidates(discovery.status, discovery.target, discoveryCandidates,
            discoveryUncertainties, discoverySpecs, discoveryFailures, discovery.packageEvidence)
        discoveryCandidates.clear()
        discoverySpecs.clear()
        discoveryUncertainties.clear()
        discoveryFailures.clear()
        assertThat(discoveryCopy.candidates).hasSize(1)
        assertThat(discoveryCopy.packageSpecifications).hasSize(1)
        assertThat(discoveryCopy.uncertainties).containsExactly(ProjectCallUncertainty.PARTIAL_DECLARATIONS)
        assertThat(discoveryCopy.failures).hasSize(1)
        mutationFails(discoveryCopy.candidates)
        mutationFails(discoveryCopy.packageSpecifications)
        mutationFails(discoveryCopy.failures)
        assertThatThrownBy { (discoveryCopy.uncertainties as MutableSet<ProjectCallUncertainty>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)
        val missing = ProjectCallBinder(failedContext).resolve(member(), ProjectCallKind.PROCEDURE, arguments(null))
        mutationFails(missing.candidates)
        mutationFails(missing.failures)
    }

    private fun <T> mutationFails(values: List<T>) {
        assertThatThrownBy { (values as MutableList<T>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun candidateConstructorsEnforcePartitionOrdinalsIdentityAndRejectionPayloads() {
        val result = resolved()
        val candidate = result.selected!!
        val parameters = (candidate.declaration.declaration as PackageSubprogramDeclaration).parameters
        illegal { candidateCopy(candidate, bindings = emptyList()) }
        illegal { candidateCopy(candidate, omitted = emptyList()) }
        illegal { candidateCopy(candidate, omitted = parameters) }
        illegal { candidateCopy(candidate, bindings = candidate.bindings + candidate.bindings.single()) }
        illegal { candidateCopy(candidate, missing = listOf(parameters.first())) }
        illegal { candidateCopy(candidate, rejectedOrdinal = 1) }
        illegal { candidateCopy(candidate, actuals = arguments("a", null)) }
        illegal { candidateCopy(candidate, bindings = listOf(ProjectArgumentBinding(2, candidate.arguments.arguments.single(), parameters.first()))) }
        illegal { candidateCopy(candidate, bindings = listOf(ProjectArgumentBinding(1, ProjectCallArgument.positional(), parameters.first()))) }
        illegal { candidateCopy(candidate, bindings = listOf(ProjectArgumentBinding(1, candidate.arguments.arguments.single(), parameters.first().copy()))) }
        illegal { candidateCopy(candidate, applicability = ProjectCallCandidate.Applicability.MISSING_REQUIRED, bindings = emptyList(), omitted = emptyList()) }
        illegal { ProjectArgumentBinding(0, ProjectCallArgument.positional(), parameters.first()) }
        illegal { ProjectArgumentBinding(1, ProjectCallArgument.named(ProjectCallTestFixtures.identifier("unknown")), parameters.first()) }

        val malformedFact = PackageProcedureDeclaration(ProjectCallTestFixtures.name("p"), ProjectCallTestFixtures.identifier("run"),
            listOf(parameters.first().copy(ordinal = 2)), FileId("bad.sql"), candidate.declaration.declaration.sourceRange)
        illegal { ProjectCallCandidate(IndexedDeclaration(malformedFact, 1), ProjectCallKind.PROCEDURE,
            arguments(null), ProjectCallCandidate.Applicability.APPLICABLE) }
        val bodyContext = context("CREATE PACKAGE BODY p AS PROCEDURE run IS BEGIN NULL; END; END p;")
        val body = index(bodyContext).occurrences.single { it.declaration is PackageProcedureDeclaration }
        illegal { ProjectCallCandidate(body, ProjectCallKind.PROCEDURE, ProjectCallArguments.none(), ProjectCallCandidate.Applicability.APPLICABLE) }
        val nonCallableContext = context("CREATE SEQUENCE run;")
        illegal { ProjectCallCandidate(index(nonCallableContext).occurrences.single(), ProjectCallKind.PROCEDURE,
            ProjectCallArguments.none(), ProjectCallCandidate.Applicability.APPLICABLE) }

        val rejectionContext = context(packageSource("PROCEDURE run(a NUMBER, b NUMBER);"))
        val binder = ProjectCallBinder(rejectionContext)
        for (actuals in listOf(arguments(null, null, null), arguments("unknown"), arguments(null, "a"), ProjectCallArguments.none())) {
            val rejection = binder.resolve(member(), ProjectCallKind.PROCEDURE, actuals).candidates.single()
            illegal { candidateCopy(rejection, bindings = listOf(candidate.bindings.single())) }
            illegal { candidateCopy(rejection, omitted = listOf(parameters[1])) }
            if (rejection.applicability == ProjectCallCandidate.Applicability.MISSING_REQUIRED) {
                illegal { candidateCopy(rejection, missing = emptyList()) }
                val missingInput = rejection.missingParameters.toMutableList()
                val copied = candidateCopy(rejection, missing = missingInput)
                missingInput.clear()
                assertThat(copied.missingParameters).hasSize(2)
                mutationFails(copied.missingParameters)
            } else {
                illegal { candidateCopy(rejection, rejectedOrdinal = null) }
                illegal { candidateCopy(rejection, rejectedOrdinal = 0) }
                illegal { candidateCopy(rejection, rejectedOrdinal = actuals.arguments.size + 1) }
            }
        }
    }

    @Test
    fun resolutionConstructorsEnforceRequestFailuresAndSelectionShape() {
        val original = resolved()
        illegal { resolutionCopy(original, selected = null) }
        illegal { resolutionCopy(original, selected = candidateCopy(original.selected!!)) }
        illegal { resolutionCopy(original, candidates = emptyList()) }
        illegal { resolutionCopy(original, failures = listOf(ProjectIndexPreparationFailure(FileId("failed.sql"), "Failure"))) }
        illegal { resolutionCopy(original, actuals = arguments(null)) }

        val context = context(listOf("a.sql" to packageSource("PROCEDURE run;"), "b.sql" to packageSource("PROCEDURE run;")))
        val ambiguous = ProjectCallBinder(context).resolve(member(), ProjectCallKind.PROCEDURE, ProjectCallArguments.none())
        illegal { resolutionCopy(ambiguous, selected = ambiguous.candidates.first()) }
        val failed = ProjectCallBinder(ProjectCallTestFixtures.context(listOf("spec.sql" to packageSource("PROCEDURE run;")), failed = true))
            .resolve(member(), ProjectCallKind.PROCEDURE, ProjectCallArguments.none())
        illegal { resolutionCopy(failed, failures = emptyList()) }
        illegal { resolutionCopy(failed, selected = failed.candidates.single()) }
        val invalid = ProjectCallBinder(ProjectAnalysisContext.NOT_PREPARED).resolve(member(), ProjectCallKind.PROCEDURE, arguments("a", null))
        illegal { resolutionCopy(invalid, candidates = original.candidates) }
        illegal { resolutionCopy(invalid, specifications = original.packageSpecifications) }
        illegal { resolutionCopy(invalid, packageEvidence = original.packageEvidence) }
        illegal { resolutionCopy(invalid, actuals = ProjectCallArguments.none()) }
    }

    @Test
    fun discoveryConstructorsEnforceOccurrenceOrderingAndTransientStatePayloads() {
        val context = context(packageSource("PROCEDURE run; PROCEDURE run(a NUMBER DEFAULT 1);"))
        val original = ProjectCallCandidateFinder(context).find(member())
        fun copy(status: ProjectCallCandidates.Status = original.status,
                 candidates: Collection<IndexedDeclaration> = original.candidates,
                 uncertainties: Set<ProjectCallUncertainty> = original.uncertainties,
                 specifications: Collection<IndexedDeclaration> = original.packageSpecifications,
                 failures: Collection<ProjectIndexPreparationFailure> = original.failures,
                 packageEvidence: Boolean = original.packageEvidence) =
            ProjectCallCandidates(status, original.target, candidates, uncertainties, specifications, failures, packageEvidence)
        illegal { copy(candidates = original.candidates.reversed()) }
        illegal { copy(candidates = original.candidates + original.candidates.first()) }
        illegal { copy(specifications = emptyList()) }
        illegal { copy(packageEvidence = false) }
        illegal { copy(failures = listOf(ProjectIndexPreparationFailure(FileId("bad.sql"), "Failure"))) }
        illegal { copy(status = ProjectCallCandidates.Status.NOT_PREPARED, candidates = emptyList(), specifications = emptyList(), packageEvidence = original.packageEvidence) }
    }

    @Test
    fun resultGraphRetainsOnlyDurableFactsNotContextIndexReadersExceptionsAstOrSourceText() {
        val marker = "call-retention-comment-887917bd-91dd-435a-928e-1d5a0f56f4d3"
        val source = "-- $marker\n" + packageSource("PROCEDURE run(a VARCHAR2 DEFAULT '$marker');")
        val sentinel = Any()
        val reader = ProjectSourceReader { sentinel.hashCode(); source }
        val exception = IllegalStateException("$marker failure", IllegalArgumentException(marker))
        val failedReader = ProjectSourceReader { throw exception }
        val sources = listOf(ProjectSource(FileId("spec.sql"), reader), ProjectSource(FileId("failed.sql"), failedReader))
        val prepared = ProjectIndexPreparation().prepare(sources, false)
        val context = ProjectAnalysisContext.prepared(prepared)
        val duplicateContext = ProjectCallTestFixtures.context(listOf(
            "a.sql" to packageSource("PROCEDURE run;"),
            "b.sql" to packageSource("PROCEDURE run;")
        ))
        val roots = listOf(
            ProjectCallCandidateFinder(context).find(member()),
            ProjectCallBinder(context).resolve(member(), ProjectCallKind.PROCEDURE, ProjectCallArguments.none()),
            resolved(),
            ProjectCallBinder(duplicateContext).resolve(member(), ProjectCallKind.PROCEDURE, ProjectCallArguments.none())
        )
        val forbidden = listOf(sentinel, source, reader, failedReader, exception, exception.cause!!, sources, prepared, prepared.index, context)
        val seen = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        fun visit(value: Any?) {
            if (value == null || !seen.add(value)) return
            assertThat(forbidden.any { it === value }).describedAs(value.javaClass.name).isFalse()
            val className = value.javaClass.name
            assertThat(value).isNotInstanceOf(ProjectAnalysisContext::class.java)
                .isNotInstanceOf(ProjectSymbolIndex::class.java).isNotInstanceOf(ProjectIndexPreparation::class.java)
                .isNotInstanceOf(ProjectSourceReader::class.java).isNotInstanceOf(ProjectSource::class.java)
                .isNotInstanceOf(Throwable::class.java).isNotInstanceOf(java.io.Reader::class.java)
                .isNotInstanceOf(java.io.InputStream::class.java).isNotInstanceOf(ByteArray::class.java)
                .isNotInstanceOf(CharArray::class.java)
            assertThat(className).doesNotStartWith("com.felipebz.flr.")
                .doesNotStartWith("com.felipebz.zpa.parser.").doesNotStartWith("com.felipebz.zpa.lexer.")
                .doesNotStartWith("com.felipebz.zpa.symbols.").doesNotStartWith("com.felipebz.zpa.project.extraction.")
            if (value is String) { assertThat(value).doesNotContain(marker); return }
            if (value is Number || value is Boolean || value is Char || value is Enum<*>) return
            when (value) {
                is Collection<*> -> { value.forEach(::visit); return }
                is Map<*, *> -> { value.forEach { (key, item) -> visit(key); visit(item) }; return }
            }
            assertThat(className).startsWith("com.felipebz.zpa.project.")
            var type: Class<*>? = value.javaClass
            while (type != null && type != Any::class.java) {
                for (field in type.declaredFields.filterNot { Modifier.isStatic(it.modifiers) }) {
                    assertThat(field.trySetAccessible()).describedAs(field.toString()).isTrue()
                    visit(field.get(value))
                }
                type = type.superclass
            }
        }
        roots.forEach(::visit)
        assertThat(seen.filterIsInstance<IndexedDeclaration>()).hasSize(8)
        assertThat(seen.filterIsInstance<ProjectIndexPreparationFailure>()).hasSize(1)
    }
}
