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
import com.felipebz.zpa.project.ProjectCallTestFixtures.member
import com.felipebz.zpa.project.ProjectCallTestFixtures.name
import com.felipebz.zpa.project.ProjectCallTestFixtures.packageSource
import com.felipebz.zpa.project.ProjectCallTestFixtures.standalone
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProjectCallCoverageTest {
    @Test
    fun droppedOnlyHeaderAndDuplicateEmptySpecificationsTakePrecedenceOverEmptiness() {
        val callSpec = context(packageSource("FUNCTION run(t NUMBER) RETURN NUMBER AS LANGUAGE JAVA NAME 'A.f(int) return int';"))
        val result = ProjectCallBinder(callSpec).resolve(member(), ProjectCallKind.FUNCTION, arguments(null, null))
        assertThat(result.status).isEqualTo(ProjectCallResolution.Status.PARTIAL_DECLARATIONS)
        assertThat(result.candidates).isEmpty()
        assertThat(result.packageSpecifications).hasSize(1)
        assertThat(result.uncertainties).containsExactly(ProjectCallUncertainty.PARTIAL_DECLARATIONS)
        val duplicate = context(listOf("a.sql" to packageSource("PROCEDURE other;"), "b.sql" to packageSource("PROCEDURE other;")))
        val empty = ProjectCallBinder(duplicate).resolve(member(), ProjectCallKind.PROCEDURE, ProjectCallArguments.none())
        assertThat(empty.status).isEqualTo(ProjectCallResolution.Status.AMBIGUOUS_TARGET)
        assertThat(empty.candidates).isEmpty()
        assertThat(empty.packageSpecifications).hasSize(2)
        assertThat(empty.uncertainties).containsExactly(ProjectCallUncertainty.DUPLICATE_SPECIFICATION)
    }

    @Test
    fun gapsAreNameScopedCanonicalAndRetainedCandidatesAreEvaluatedButNotSelected() {
        for (skipped in listOf("run", "\"RUN\"")) {
            val context = context(packageSource("PROCEDURE run(a NUMBER); PROCEDURE $skipped(t TABLE) PROCEDURE coverage_recovery;"))
            val result = ProjectCallBinder(context).resolve(member(), ProjectCallKind.PROCEDURE, arguments(null))
            assertThat(result.status).isEqualTo(ProjectCallResolution.Status.PARTIAL_DECLARATIONS)
            assertThat(result.candidates.single().applicability).isEqualTo(ProjectCallCandidate.Applicability.APPLICABLE)
            assertThat(result.candidates.single().bindings.single().parameter.ordinal).isEqualTo(1)
            assertThat(result.selected).isNull()
        }
        for (skipped in listOf("other", "\"run\"")) {
            val context = context(packageSource("PROCEDURE run(a NUMBER); PROCEDURE $skipped(t TABLE) PROCEDURE coverage_recovery;"))
            val result = ProjectCallBinder(context).resolve(member(), ProjectCallKind.PROCEDURE, arguments(null))
            assertThat(result.status).isEqualTo(ProjectCallResolution.Status.RESOLVED)
            assertThat(result.uncertainties).isEmpty()
        }
    }

    @Test
    fun coverageDominatesEvenAnAlreadyAmbiguousRetainedOverloadSet() {
        val context = context(packageSource("""
            PROCEDURE run(a NUMBER DEFAULT 1);
            PROCEDURE run(a VARCHAR2 DEFAULT 'z');
            FUNCTION run(t NUMBER) RETURN NUMBER AS LANGUAGE JAVA NAME 'A.f(int) return int';
        """.trimIndent()))
        val result = ProjectCallBinder(context).resolve(member(), ProjectCallKind.PROCEDURE, ProjectCallArguments.none())
        assertThat(result.status).isEqualTo(ProjectCallResolution.Status.PARTIAL_DECLARATIONS)
        assertThat(result.uncertainties).containsExactly(ProjectCallUncertainty.PARTIAL_DECLARATIONS)
        assertThat(result.candidates.map { it.applicability }).containsExactly(
            ProjectCallCandidate.Applicability.APPLICABLE, ProjectCallCandidate.Applicability.APPLICABLE)
        assertThat(result.selected).isNull()
    }

    @Test
    fun unknownCoverageFromEverySupportedGapSourceAppliesToAllNames() {
        val sources = listOf(
            packageSource("${'$'}IF ${'$'}${'$'}x ${'$'}THEN PROCEDURE a; ${'$'}ELSE PROCEDURE b; ${'$'}END"),
            packageSource("PROCEDURE run; &extra_members"),
            packageSource("PROCEDURE ;"),
            packageSource("PROCEDURE a.b(x NUMBER) PROCEDURE coverage_recovery;"),
            packageSource("TYPE t IS RECORD(a NUMBER) FUNCTION f RETURN NUMBER; PROCEDURE ok;"),
            packageSource("function weird_t x y;")
        )
        for (source in sources) {
            val context = context(source)
            for (local in listOf("run", "a", "zz")) {
                val target = member(local = local)
                val discovery = ProjectCallCandidateFinder(context).find(target)
                val result = ProjectCallBinder(context).resolve(target, ProjectCallKind.PROCEDURE, ProjectCallArguments.none())
                assertThat(discovery.status).describedAs(source).isEqualTo(ProjectCallCandidates.Status.PARTIAL_DECLARATIONS)
                assertThat(result.status).describedAs(source).isEqualTo(ProjectCallResolution.Status.PARTIAL_DECLARATIONS)
                assertThat(result.uncertainties).containsExactly(ProjectCallUncertainty.PARTIAL_DECLARATIONS)
                assertThat(result.selected).isNull()
            }
        }
    }

    @Test
    fun headerOnlySubstitutionAndDroppedNonCallablesDoNotTaintCallBinding() {
        val sources = listOf(
            "CREATE OR REPLACE PACKAGE p &start_ge_8_1 AUTHID CURRENT_USER &end_ge_8_1 IS PROCEDURE run; END p;",
            packageSource("v weird type here; CURSOR c IS SELECT 1 FROM dual; PRAGMA SERIALLY_REUSABLE; e EXCEPTION; TYPE t IS OBJECT(a NUMBER); PROCEDURE run;")
        )
        for (source in sources) {
            val result = ProjectCallBinder(context(source)).resolve(member(), ProjectCallKind.PROCEDURE, ProjectCallArguments.none())
            assertThat(result.status).describedAs(source).isEqualTo(ProjectCallResolution.Status.RESOLVED)
            assertThat(result.uncertainties).isEmpty()
            assertThat((result.packageSpecifications.single().declaration as PackageDeclaration).callableCoverage.complete).isTrue()
        }
    }

    @Test
    fun allCoexistingUncertaintiesArePreservedUnderTheirTotalStatusPrecedence() {
        for (duplicate in listOf(false, true)) for (gap in listOf(false, true))
            for (competition in listOf(false, true)) for (failed in listOf(false, true)) {
                val files = mutableListOf("a.sql" to packageSource("PROCEDURE run;" + if (gap) " PROCEDURE run(t TABLE) PROCEDURE coverage_recovery;" else ""))
                if (duplicate) files += "b.sql" to packageSource("PROCEDURE other;")
                if (competition) files += "c.sql" to standalone("p.run")
                val context = context(files, failed = failed)
                val target = ProjectCallTarget.written(name("p", "run"))
                val discovery = ProjectCallCandidateFinder(context).find(target)
                val expected = buildSet {
                    if (duplicate) add(ProjectCallUncertainty.DUPLICATE_SPECIFICATION)
                    if (gap) add(ProjectCallUncertainty.PARTIAL_DECLARATIONS)
                    if (competition) add(ProjectCallUncertainty.NAMESPACE_COMPETITION)
                }
                assertThat(discovery.uncertainties).isEqualTo(expected)
                // Kind mismatch and structural rejection cannot outrank any target uncertainty.
                for (kind in ProjectCallKind.entries) for (actuals in listOf(ProjectCallArguments.none(), arguments(null))) {
                    val result = ProjectCallBinder(context).resolve(target, kind, actuals)
                    assertThat(result.uncertainties).isEqualTo(expected)
                    val status = when {
                        failed -> ProjectCallResolution.Status.INCOMPLETE_INDEX
                        duplicate || competition -> ProjectCallResolution.Status.AMBIGUOUS_TARGET
                        gap -> ProjectCallResolution.Status.PARTIAL_DECLARATIONS
                        kind == ProjectCallKind.FUNCTION -> ProjectCallResolution.Status.WRONG_KIND
                        actuals.arguments.isNotEmpty() -> ProjectCallResolution.Status.NO_APPLICABLE_CANDIDATE
                        else -> ProjectCallResolution.Status.RESOLVED
                    }
                    assertThat(result.status).isEqualTo(status)
                    assertThat(result.selected != null).isEqualTo(status == ProjectCallResolution.Status.RESOLVED)
                    assertThat(result.candidates).hasSize(if (competition) 2 else 1)
                    assertThat(result.candidates.map { it.applicability }).containsOnly(when {
                        kind == ProjectCallKind.FUNCTION -> ProjectCallCandidate.Applicability.WRONG_KIND
                        actuals.arguments.isNotEmpty() -> ProjectCallCandidate.Applicability.TOO_MANY_ARGUMENTS
                        else -> ProjectCallCandidate.Applicability.APPLICABLE
                    })
                    assertThat(result.failures.isNotEmpty()).isEqualTo(failed)
                    if (expected.isNotEmpty()) assertThat(result.status).isNotIn(
                        ProjectCallResolution.Status.RESOLVED, ProjectCallResolution.Status.NOT_FOUND_IN_PROJECT, ProjectCallResolution.Status.UNKNOWN_PACKAGE)
                }
            }
    }

    @Test
    fun discoveryOrderIsMemberThenStandaloneEvenWhenStandaloneHasEarlierFileId() {
        val context = context(listOf("z.sql" to packageSource("PROCEDURE run; PROCEDURE run(a NUMBER DEFAULT 1);"),
            "a.sql" to standalone("p.run")))
        val target = ProjectCallTarget.written(name("p", "run"))
        val discovery = ProjectCallCandidateFinder(context).find(target)
        assertThat(discovery.candidates.map { it.fileId.value }).containsExactly("z.sql", "z.sql", "a.sql")
        assertThat(discovery.candidates.map { it.declaration.role }).containsExactly(
            DeclarationRole.SPECIFICATION, DeclarationRole.SPECIFICATION, DeclarationRole.STANDALONE)
        val result = ProjectCallBinder(context).resolve(target, ProjectCallKind.PROCEDURE, ProjectCallArguments.none())
        assertThat(result.candidates.map { it.declaration }).containsExactlyElementsOf(discovery.candidates)
        assertThat(result.selected).isNull()
    }
}
