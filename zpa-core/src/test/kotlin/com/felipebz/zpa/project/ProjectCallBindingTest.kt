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

import com.felipebz.zpa.project.ProjectCallCandidate.Applicability.*
import com.felipebz.zpa.project.ProjectCallKind.FUNCTION
import com.felipebz.zpa.project.ProjectCallKind.PROCEDURE
import com.felipebz.zpa.project.ProjectCallResolution.Status.*
import com.felipebz.zpa.project.ProjectCallTestFixtures.adaptedStatus
import com.felipebz.zpa.project.ProjectCallTestFixtures.arguments
import com.felipebz.zpa.project.ProjectCallTestFixtures.context
import com.felipebz.zpa.project.ProjectCallTestFixtures.fingerprint
import com.felipebz.zpa.project.ProjectCallTestFixtures.identifier
import com.felipebz.zpa.project.ProjectCallTestFixtures.member
import com.felipebz.zpa.project.ProjectCallTestFixtures.name
import com.felipebz.zpa.project.ProjectCallTestFixtures.namespaceCases
import com.felipebz.zpa.project.ProjectCallTestFixtures.oldStatus
import com.felipebz.zpa.project.ProjectCallTestFixtures.packageSource
import com.felipebz.zpa.project.ProjectCallTestFixtures.standalone
import com.felipebz.zpa.project.ProjectCallTestFixtures.zeroCases
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import com.felipebz.zpa.project.ProjectCallCandidate.Applicability.WRONG_KIND as CandidateWrongKind

class ProjectCallBindingTest {
    private fun resolve(context: ProjectAnalysisContext, local: String = "run", kind: ProjectCallKind = PROCEDURE,
                        actuals: ProjectCallArguments = ProjectCallArguments.none(), owner: String = "p") =
        ProjectCallBinder(context).resolve(member(owner, local), kind, actuals)

    @Test
    fun namespaceDecisionTableIncludingAllBVariantsFailuresAndPreparationOrders() {
        val target = ProjectCallTarget.written(name("x", "y"))
        for (fixture in namespaceCases()) {
            var reference: String? = null
            for (failed in listOf(false, true)) {
                var failedReference: String? = null
                for (concurrent in listOf(false, true)) for (reversed in listOf(false, true)) {
                    val files = fixture.sources.mapIndexed { ordinal, text -> "$ordinal.sql" to text }
                    val context = context(if (reversed) files.reversed() else files, concurrent, failed)
                    val discovery = ProjectCallCandidateFinder(context).find(target)
                    val result = ProjectCallBinder(context).resolve(target, PROCEDURE, ProjectCallArguments.none())
                    val description = "${fixture.label}, N10=$failed, concurrent=$concurrent, reversed=$reversed"
                    assertThat(discovery.status).describedAs(description).isEqualTo(
                        if (failed) ProjectCallCandidates.Status.INCOMPLETE_INDEX else fixture.status)
                    assertThat(discovery.uncertainties).describedAs(description).isEqualTo(fixture.uncertainties)
                    assertThat(discovery.candidates).describedAs(description).hasSize(fixture.candidateCount)
                    assertThat(discovery.packageSpecifications).describedAs(description).hasSize(fixture.specificationCount)
                    assertThat(result.status.name).describedAs(description).isEqualTo(
                        if (failed) "INCOMPLETE_INDEX" else if (fixture.status == ProjectCallCandidates.Status.FOUND) "RESOLVED" else fixture.status.name)
                    assertThat(result.uncertainties).isEqualTo(fixture.uncertainties)
                    assertThat(result.packageSpecifications).containsExactlyElementsOf(discovery.packageSpecifications)
                    assertThat(result.candidates.map { it.declaration }).containsExactlyElementsOf(discovery.candidates)
                    assertThat(result.candidates.all { it.applicability == APPLICABLE }).isTrue()
                    assertThat(result.candidates.map { it.declaration.declaration.role }).doesNotContain(DeclarationRole.BODY)
                    assertThat(result.selected != null).isEqualTo(!failed && fixture.status == ProjectCallCandidates.Status.FOUND)
                    assertThat(result.failures.isNotEmpty()).isEqualTo(failed)
                    assertUncertaintyProperty(result)
                    val fingerprint = fingerprint(result)
                    if (failed) {
                        if (failedReference == null) failedReference = fingerprint else assertThat(fingerprint).isEqualTo(failedReference)
                    } else {
                        if (reference == null) reference = fingerprint else assertThat(fingerprint).isEqualTo(reference)
                    }
                }
            }
        }
    }

    @Test
    fun explicitNamespacesIgnoreCompetingEvidenceAndEmptyMemberEvidenceIsNotAbsence() {
        for (fixture in namespaceCases().filter { it.label in setOf("N4", "N6", "N7b-neither", "N8b-skipped", "N9b") }) {
            val context = context(fixture.sources.mapIndexed { index, text -> "$index.sql" to text })
            val standalone = ProjectCallBinder(context).resolve(ProjectCallTarget.standalone(name("x", "y")), PROCEDURE, ProjectCallArguments.none())
            assertThat(standalone.status).describedAs(fixture.label).isEqualTo(RESOLVED)
            assertThat(standalone.packageSpecifications).isEmpty()
            assertThat(standalone.uncertainties).isEmpty()
            assertThat(standalone.selected!!.declaration.declaration.role).isEqualTo(DeclarationRole.STANDALONE)
            val member = resolve(context, "y", owner = "x")
            val expected = when (fixture.label) {
                "N4" -> NOT_FOUND_IN_PROJECT
                "N6" -> RESOLVED
                "N7b-neither" -> AMBIGUOUS_TARGET
                "N8b-skipped" -> PARTIAL_DECLARATIONS
                else -> UNKNOWN_PACKAGE
            }
            assertThat(member.status).isEqualTo(expected)
            assertThat(member.uncertainties).doesNotContain(ProjectCallUncertainty.NAMESPACE_COMPETITION)
        }
    }

    @Test
    fun ownerEvidenceIncludesOwnedValuesTypesSubtypesBodySubprogramsAndExactDeclarations() {
        val evidence = listOf(
            "CREATE PACKAGE BODY x AS PROCEDURE other IS BEGIN NULL; END; END x;",
            "CREATE TYPE x AS OBJECT(a NUMBER);",
            "CREATE SEQUENCE x;",
            standalone("x")
        )
        val contexts = evidence.map { source -> source to context(listOf("owner.sql" to source, "standalone.sql" to standalone())) }.toMutableList()
        // Owned value/type/subtype facts can establish owner evidence without a retained specification.
        // The current body extractor retains subprograms only, so exercise these cases on internal facts.
        for (body in listOf("g NUMBER;", "TYPE t IS RECORD(a NUMBER);", "SUBTYPE s IS NUMBER;")) {
            val ownerFile = FileId("owner.sql")
            val standaloneFile = FileId("standalone.sql")
            val extractor = ProjectDeclarationExtractor()
            val facts = extractor.extract(ownerFile, packageSource(body, "x")).filterNot { it is PackageDeclaration }
            assertThat(facts).hasSize(1)
            val index = ProjectSymbolIndexBuilder().also {
                it.add(ownerFile, facts)
                it.add(standaloneFile, extractor.extract(standaloneFile, standalone()))
            }.build()
            contexts += body to ProjectAnalysisContext.prepared(ProjectIndexPreparationResult(index, 2, emptyList()))
        }
        for ((source, context) in contexts) {
            val result = ProjectCallBinder(context).resolve(ProjectCallTarget.written(name("x", "y")), PROCEDURE, ProjectCallArguments.none())
            assertThat(result.status).describedAs(source).isEqualTo(AMBIGUOUS_TARGET)
            assertThat(result.uncertainties).containsExactly(ProjectCallUncertainty.NAMESPACE_COMPETITION)
            assertThat(result.packageEvidence).isTrue()
            assertThat(result.candidates).hasSize(1)
            assertThat(result.selected).isNull()
        }
    }

    @Test
    fun writtenNamesUseOnlySpecifiedSegmentInterpretationsAndNeverInferSchemaOrCurrentPackage() {
        val context = context(listOf(
            "p.sql" to packageSource("PROCEDURE run;"),
            "app.sql" to "CREATE FUNCTION app.f(x NUMBER) RETURN NUMBER IS BEGIN RETURN x; END;",
            "a.sql" to packageSource("PROCEDURE run;", "a.p"),
            "b.sql" to packageSource("PROCEDURE run;", "b.p")
        ))
        val binder = ProjectCallBinder(context)
        assertThat(binder.resolve(ProjectCallTarget.written(name("run")), PROCEDURE, ProjectCallArguments.none()).status).isEqualTo(NOT_FOUND_IN_PROJECT)
        assertThat(binder.resolve(ProjectCallTarget.standalone(name("f")), FUNCTION, arguments(null)).status).isEqualTo(NOT_FOUND_IN_PROJECT)
        assertThat(binder.resolve(ProjectCallTarget.standalone(name("app", "f")), FUNCTION, arguments(null)).status).isEqualTo(RESOLVED)
        val three = binder.resolve(ProjectCallTarget.written(name("a", "p", "run")), PROCEDURE, ProjectCallArguments.none())
        assertThat(three.status).isEqualTo(RESOLVED)
        assertThat(three.selected!!.declaration.fileId).isEqualTo(FileId("a.sql"))
        assertThat(binder.resolve(ProjectCallTarget.written(name("a", "b", "p", "run")), PROCEDURE, ProjectCallArguments.none()).status).isEqualTo(NOT_FOUND_IN_PROJECT)
        assertThat(binder.resolve(ProjectCallTarget.standalone(name("a", "p", "run")), PROCEDURE, ProjectCallArguments.none()).status).isEqualTo(NOT_FOUND_IN_PROJECT)
        assertThat(resolve(context, owner = "external").status).isEqualTo(UNKNOWN_PACKAGE)
        assertThat(binder.resolve(ProjectCallTarget.standalone(name("external")), PROCEDURE, ProjectCallArguments.none()).status).isEqualTo(NOT_FOUND_IN_PROJECT)
    }

    private fun bindingContext() = context(packageSource("""
        TYPE rec_t IS RECORD(a NUMBER);
        PROCEDURE run(a NUMBER, b NUMBER DEFAULT 7, c VARCHAR2 DEFAULT 'z');
        FUNCTION f(x NUMBER DEFAULT 1) RETURN NUMBER;
        PROCEDURE copy(src IN rec_t, dst OUT NOCOPY rec_t);
        PROCEDURE lab(a NUMBER);
        PROCEDURE lab(b NUMBER);
        PROCEDURE pick(n NUMBER);
        PROCEDURE pick(s VARCHAR2);
        PROCEDURE typed(x NUMBER);
        PROCEDURE typed(x VARCHAR2);
        PROCEDURE same(a NUMBER);
        PROCEDURE same(a NUMBER);
        PROCEDURE m(a NUMBER, b NUMBER);
        PROCEDURE m(b NUMBER, a NUMBER);
        PROCEDURE boot;
        PROCEDURE boot(x NUMBER DEFAULT 1);
        PROCEDURE both(x NUMBER);
        FUNCTION both(x NUMBER) RETURN NUMBER;
        PROCEDURE q("Lbl" NUMBER, y NUMBER);
        PROCEDURE malformed(a NUMBER, "A" NUMBER);
        PROCEDURE noargs;
        PROCEDURE modes(a OUT NUMBER, b IN OUT NUMBER DEFAULT 1);
        PROCEDURE several(a NUMBER, b NUMBER DEFAULT 1, c NUMBER, d NUMBER);
    """.trimIndent()))

    @Test
    fun positionalMixedNamedAndDefaultBindingPreserveWrittenOrderAndFormalMetadata() {
        val context = bindingContext()
        val first = resolve(context, actuals = arguments(null))
        assertThat(first.status).isEqualTo(RESOLVED)
        assertThat(first.selected!!.bindings.map { it.argumentOrdinal to it.parameter.ordinal }).containsExactly(1 to 1)
        assertThat(first.selected.omittedParameters.map { it.name.lookupName }).containsExactly("B", "C")
        val mixed = resolve(context, actuals = arguments(null, "c"))
        assertThat(mixed.status).isEqualTo(RESOLVED)
        assertThat(mixed.selected!!.bindings.map { it.argumentOrdinal to it.parameter.ordinal }).containsExactly(1 to 1, 2 to 3)
        assertThat(mixed.selected.omittedParameters.map { it.ordinal }).containsExactly(2)
        val reordered = resolve(context, "copy", actuals = arguments("dst", "src"))
        assertThat(reordered.status).isEqualTo(RESOLVED)
        assertThat(reordered.selected!!.bindings.map { it.argumentOrdinal to it.parameter.ordinal }).containsExactly(1 to 2, 2 to 1)
        val destination = reordered.selected.bindings.first().parameter
        assertThat(destination.mode).isEqualTo(ParameterMode.OUT)
        assertThat(destination.nocopy).isTrue()
        assertThat(destination.typeRef).isInstanceOf(NamedTypeRef::class.java)
        assertThat((destination.typeRef as NamedTypeRef).name).isEqualTo(name("rec_t"))
        val fact = reordered.selected.declaration.declaration as PackageSubprogramDeclaration
        assertThat(destination).isSameAs(fact.parameters[1])
        assertThat(resolve(context, "f", FUNCTION).selected!!.omittedParameters.map { it.ordinal }).containsExactly(1)
        // Only structural information is retained: OUT assignability and expression datatypes are not inferred.
        assertThat(resolve(context, "modes", actuals = arguments(null)).status).isEqualTo(RESOLVED)
    }

    @Test
    fun allRejectionKindsHaveTheApprovedFirstFailureAndPayload() {
        val context = bindingContext()
        data class Rejection(val local: String, val actuals: ProjectCallArguments, val reason: ProjectCallCandidate.Applicability,
                             val ordinal: Int? = null, val missing: List<Int> = emptyList())
        val cases = listOf(
            Rejection("run", arguments(null, "unknown"), UNKNOWN_LABEL, 2),
            Rejection("run", arguments(null, "a"), DUPLICATE_BINDING, 2),
            Rejection("run", arguments(null, null, null, null), TOO_MANY_ARGUMENTS, 4),
            Rejection("run", arguments("c"), MISSING_REQUIRED, missing = listOf(1)),
            Rejection("run", ProjectCallArguments.none(), MISSING_REQUIRED, missing = listOf(1)),
            Rejection("several", arguments("b"), MISSING_REQUIRED, missing = listOf(1, 3, 4)),
            Rejection("malformed", arguments(null, null, null, "unknown"), INVALID_SIGNATURE),
            Rejection("run", arguments(null, null, null, null, "unknown"), TOO_MANY_ARGUMENTS, 4),
            Rejection("run", arguments("unknown", "a"), UNKNOWN_LABEL, 1),
            Rejection("run", arguments(null, "a", "unknown"), DUPLICATE_BINDING, 2)
        )
        for (case in cases) {
            val result = resolve(context, case.local, actuals = case.actuals)
            assertThat(result.status).describedAs(case.toString()).isEqualTo(NO_APPLICABLE_CANDIDATE)
            val candidate = result.candidates.single()
            assertThat(candidate.applicability).isEqualTo(case.reason)
            assertThat(candidate.rejectedArgumentOrdinal).isEqualTo(case.ordinal)
            assertThat(candidate.missingParameters.map { it.ordinal }).isEqualTo(case.missing)
            assertThat(candidate.bindings).isEmpty()
            assertThat(candidate.omittedParameters).isEmpty()
            assertThat(result.selected).isNull()
        }
        // Kind precedes invalid signature, arity, labels and omissions.
        val wrong = resolve(context, "malformed", FUNCTION, arguments(null, null, null, "unknown"))
        assertThat(wrong.status).isEqualTo(WRONG_KIND)
        assertThat(wrong.candidates.single().applicability).isEqualTo(CandidateWrongKind)
        assertThat(resolve(context, "f", PROCEDURE).status).isEqualTo(WRONG_KIND)
        assertThat(resolve(context, "noargs", FUNCTION).status).isEqualTo(WRONG_KIND)
    }

    @Test
    fun overloadsAreNeverDeduplicatedOrRankedByTypesOrInputOrder() {
        val context = bindingContext()
        assertThat(resolve(context, "same", actuals = arguments(null)).status).isEqualTo(AMBIGUOUS)
        assertThat(resolve(context, "same", actuals = arguments(null)).candidates).hasSize(2)
        assertThat(resolve(context, "lab", actuals = arguments(null)).status).isEqualTo(AMBIGUOUS)
        assertThat(resolve(context, "lab", actuals = arguments("b")).selected!!.bindings.single().parameter.name.lookupName).isEqualTo("B")
        assertThat(resolve(context, "pick", actuals = arguments("s")).status).isEqualTo(RESOLVED)
        assertThat(resolve(context, "pick", actuals = arguments(null)).status).isEqualTo(AMBIGUOUS)
        assertThat(resolve(context, "typed", actuals = arguments("x")).status).isEqualTo(AMBIGUOUS)
        val m = resolve(context, "m", actuals = arguments(null, "a"))
        assertThat(m.status).isEqualTo(RESOLVED)
        assertThat(m.candidates.map { it.applicability }).containsExactly(DUPLICATE_BINDING, APPLICABLE)
        assertThat(m.selected!!.bindings.map { it.parameter.name.lookupName }).containsExactly("B", "A")
        val boot = resolve(context, "boot")
        assertThat(boot.status).isEqualTo(AMBIGUOUS)
        assertThat(boot.candidates.map { it.applicability }).containsExactly(APPLICABLE, APPLICABLE)
        assertThat(boot.selected).isNull()
        for (kind in listOf(PROCEDURE, FUNCTION)) {
            val both = resolve(context, "both", kind, arguments(null))
            assertThat(both.status).isEqualTo(RESOLVED)
            assertThat(both.candidates.map { it.applicability }).containsExactlyInAnyOrder(APPLICABLE, CandidateWrongKind)
        }
    }

    @Test
    fun canonicalLabelIdentityAndTargetIdentityUseLookupNameOnly() {
        val context = bindingContext()
        assertThat(resolve(context, "q", actuals = arguments("\"Lbl\"", "y")).status).isEqualTo(RESOLVED)
        assertThat(resolve(context, "q", actuals = arguments("lbl", "y")).candidates.single().applicability).isEqualTo(UNKNOWN_LABEL)
        val canonical = resolve(context, "\"RUN\"", actuals = arguments("\"A\""), owner = "\"P\"")
        assertThat(canonical.status).isEqualTo(RESOLVED)
        assertThat(resolve(context, "\"run\"", actuals = arguments("a")).status).isEqualTo(NOT_FOUND_IN_PROJECT)
        val duplicates = arguments("a", "\"A\"")
        assertThat(duplicates.notation).isEqualTo(ProjectCallArguments.Notation.DUPLICATE_LABEL)
        assertThat(duplicates.invalidArgumentOrdinal).isEqualTo(2)
        assertThat(arguments("a", "\"a\"").notation).isEqualTo(ProjectCallArguments.Notation.VALID)
        assertThat(arguments("a", "\"A\"", null).invalidArgumentOrdinal).isEqualTo(2)
        assertThat(arguments("a", null, "a").notation).isEqualTo(ProjectCallArguments.Notation.POSITIONAL_AFTER_NAMED)
        assertThat(arguments("a", null, "a").invalidArgumentOrdinal).isEqualTo(2)
        val merged = context(packageSource("PROCEDURE run(a NUMBER); PROCEDURE \"RUN\"(b NUMBER);"))
        assertThat(resolve(merged, actuals = arguments(null)).status).isEqualTo(AMBIGUOUS)
        assertThat(resolve(merged, "\"RUN\"", actuals = arguments("b")).status).isEqualTo(RESOLVED)
    }

    @Test
    fun invalidNotationIsProjectIndependentAndPrecedesUnpreparedFailuresAndEveryUncertainty() {
        val contexts = listOf(ProjectAnalysisContext.NOT_PREPARED, context(emptyList())) + namespaceCases().flatMap { fixture ->
            val files = fixture.sources.mapIndexed { i, source -> "$i.sql" to source }
            listOf(context(files), context(files, failed = true))
        }
        for (context in contexts) for (actuals in listOf(arguments("a", null), arguments("a", "\"A\""))) {
            val result = ProjectCallBinder(context).resolve(ProjectCallTarget.written(name("x", "y")), PROCEDURE, actuals)
            assertThat(result.status).isEqualTo(INVALID_NOTATION)
            assertThat(result.arguments).isSameAs(actuals)
            assertThat(result.candidates).isEmpty()
            assertThat(result.packageSpecifications).isEmpty()
            assertThat(result.uncertainties).isEmpty()
            assertThat(result.failures).isEmpty()
            assertThat(result.selected).isNull()
        }
        val unprepared = resolve(ProjectAnalysisContext.NOT_PREPARED)
        assertThat(unprepared.status).isEqualTo(NOT_PREPARED)
        assertThat(unprepared.candidates).isEmpty()
        assertThat(ProjectCallCandidateFinder(ProjectAnalysisContext.NOT_PREPARED).find(member()).status).isEqualTo(ProjectCallCandidates.Status.NOT_PREPARED)
    }

    @Test
    fun specificationDefaultsAreUsedAndBodiesAreNeverCandidates() {
        val context = context(listOf(
            "spec.sql" to packageSource("PROCEDURE run(a NUMBER DEFAULT 1);"),
            "body.sql" to "CREATE PACKAGE BODY p AS PROCEDURE run(a NUMBER) IS BEGIN NULL; END; PROCEDURE hidden IS BEGIN NULL; END; END p;"
        ))
        val result = resolve(context)
        assertThat(result.status).isEqualTo(RESOLVED)
        assertThat(result.candidates).hasSize(1)
        assertThat(result.selected!!.declaration.fileId).isEqualTo(FileId("spec.sql"))
        assertThat(result.selected.declaration.declaration.role).isEqualTo(DeclarationRole.SPECIFICATION)
        assertThat(result.selected.omittedParameters.single().defaultPresent).isTrue()
        assertThat(resolve(context, "hidden").status).isEqualTo(NOT_FOUND_IN_PROJECT)
    }

    @Test
    fun standaloneProceduresAndFunctionsBindWithoutPackageOwnership() {
        val context = context("CREATE PROCEDURE run(a NUMBER, b NUMBER) IS BEGIN NULL; END;\n/\nCREATE FUNCTION f(x NUMBER) RETURN NUMBER IS BEGIN RETURN x; END;")
        for ((local, kind, actuals) in listOf(Triple("run", PROCEDURE, arguments(null, null)), Triple("f", FUNCTION, arguments(null)))) {
            val result = ProjectCallBinder(context).resolve(ProjectCallTarget.standalone(name(local)), kind, actuals)
            assertThat(result.status).isEqualTo(RESOLVED)
            assertThat(result.packageSpecifications).isEmpty()
            assertThat(result.selected!!.declaration.declaration.role).isEqualTo(DeclarationRole.STANDALONE)
        }
    }

    @Test
    fun zFixturesCompareTheUnchangedLegacyResolverWithD5IncludingSelectionIdentity() {
        for (fixture in zeroCases()) {
            val context = if (fixture.unprepared) ProjectAnalysisContext.NOT_PREPARED else
                context(fixture.sources.mapIndexed { i, text -> "$i.sql" to text }, failed = fixture.failed)
            val old = ProjectPackageProcedureResolver(context).resolve(name("p"), identifier("setup"))
            val engine = resolve(context, "setup")
            assertThat(oldStatus(old)).describedAs(fixture.label).isEqualTo(fixture.oldStatus)
            assertThat(adaptedStatus(engine)).describedAs(fixture.label).isEqualTo(fixture.adaptedStatus)
            if (fixture.oldStatus == fixture.adaptedStatus && old is ProjectPackageProcedureResolution.Resolved) {
                assertThat(engine.selected!!.declaration.declaration).isSameAs(old.declaration)
            }
            if (fixture.oldStatus != fixture.adaptedStatus) {
                assertThat(fixture.label).isIn("Z2", "Z3", "Z10")
                assertThat(engine.status).isEqualTo(PARTIAL_DECLARATIONS)
                assertThat(engine.selected).isNull()
            }
        }
    }

    @Test
    fun structuralMatrixHasIdenticalOccurrencesBindingsAndSelectionsInConcurrentReversedPreparation() {
        val files = listOf(
            "spec.sql" to packageSource("""
                PROCEDURE run(a NUMBER, b NUMBER DEFAULT 1, c NUMBER DEFAULT 2);
                PROCEDURE lab(a NUMBER);
                PROCEDURE lab(b NUMBER);
                PROCEDURE m(a NUMBER, b NUMBER);
                PROCEDURE m(b NUMBER, a NUMBER);
                PROCEDURE boot;
                PROCEDURE boot(x NUMBER DEFAULT 1);
                PROCEDURE both(x NUMBER);
                FUNCTION both(x NUMBER) RETURN NUMBER;
                PROCEDURE q("Lbl" NUMBER, y NUMBER);
                PROCEDURE invalid(a NUMBER, "A" NUMBER);
            """.trimIndent()),
            "body.sql" to "CREATE PACKAGE BODY p AS PROCEDURE run(a NUMBER, b NUMBER, c NUMBER) IS BEGIN NULL; END; END p;",
            "standalone.sql" to standalone("app.run")
        )
        val requests = listOf(
            Triple("run", PROCEDURE, arguments(null)),
            Triple("run", PROCEDURE, arguments(null, "c")),
            Triple("run", PROCEDURE, arguments("c", "a")),
            Triple("run", PROCEDURE, arguments(null, "unknown")),
            Triple("run", PROCEDURE, arguments(null, "a")),
            Triple("run", PROCEDURE, arguments(null, null, null, null)),
            Triple("run", PROCEDURE, arguments("c")),
            Triple("run", PROCEDURE, arguments("a", null)),
            Triple("run", PROCEDURE, arguments("a", "\"A\"")),
            Triple("run", FUNCTION, arguments(null)),
            Triple("lab", PROCEDURE, arguments(null)),
            Triple("lab", PROCEDURE, arguments("b")),
            Triple("m", PROCEDURE, arguments(null, "a")),
            Triple("boot", PROCEDURE, ProjectCallArguments.none()),
            Triple("both", PROCEDURE, arguments(null)),
            Triple("both", FUNCTION, arguments(null)),
            Triple("q", PROCEDURE, arguments("\"Lbl\"", "y")),
            Triple("q", PROCEDURE, arguments("lbl", "y")),
            Triple("invalid", PROCEDURE, arguments(null)),
            Triple("absent", PROCEDURE, ProjectCallArguments.none())
        )
        for (failed in listOf(false, true)) {
            val serial = context(files, failed = failed)
            val expected = requests.map { (local, kind, actuals) -> fingerprint(resolve(serial, local, kind, actuals)) }
            for (concurrent in listOf(false, true)) for (reversed in listOf(false, true)) {
                val prepared = context(if (reversed) files.reversed() else files, concurrent, failed)
                val actual = requests.map { (local, kind, actuals) -> fingerprint(resolve(prepared, local, kind, actuals)) }
                assertThat(actual).describedAs("failed=$failed concurrent=$concurrent reversed=$reversed").isEqualTo(expected)
            }
        }
    }

    private fun assertUncertaintyProperty(result: ProjectCallResolution) {
        if (result.uncertainties.isNotEmpty()) {
            assertThat(result.status).isNotIn(RESOLVED, NOT_FOUND_IN_PROJECT, UNKNOWN_PACKAGE)
            assertThat(result.selected).isNull()
        }
    }
}
