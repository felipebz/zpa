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
package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectCallCandidates.Status.*
import com.felipebz.zpa.api.project.ProjectCallUncertainty.DUPLICATE_SPECIFICATION
import com.felipebz.zpa.api.project.ProjectCallUncertainty.NAMESPACE_COMPETITION
import com.felipebz.zpa.project.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/** Public discovery, checked scenario by scenario against the authoritative internal finder. */
@OptIn(ZpaExperimentalApi::class)
class ProjectCallCandidatesTest {
    private fun n(vararg segments: String) = ProjectQualifiedName.of(*segments)
    private fun member(pkg: String, name: String) = ProjectCallTarget.packageMember(n(pkg), ProjectIdentifier.fromSource(name))
    private fun written(vararg segments: String) = ProjectCallTarget.written(n(*segments))
    private fun standalone(vararg segments: String) = ProjectCallTarget.standalone(n(*segments))

    private class Project(val snapshot: ProjectSnapshot, val context: ProjectAnalysisContext)

    private fun project(vararg files: Pair<String, String>, failed: Boolean = false, concurrent: Boolean = false): Project {
        val sources = files.map { (id, text) -> ProjectSource(FileId(id)) { text } }.toMutableList()
        if (failed) sources += ProjectSource(FileId("failed.sql")) { throw IllegalStateException("reader failed") }
        val context = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(sources, concurrent))
        return Project(ProjectSnapshot.create(context), context)
    }

    /** The public result must carry exactly the finder's status, occurrences, uncertainties and evidence. */
    private fun discover(project: Project, target: ProjectCallTarget): ProjectCallCandidates {
        val internal = ProjectCallCandidateFinder(project.context).find(target.toInternal())
        val public = project.snapshot.findCallCandidates(target)
        assertThat(public.status.name).describedAs("status of $target").isEqualTo(internal.status.name)
        assertThat(public.uncertainties.map { it.name }).containsExactlyInAnyOrderElementsOf(internal.uncertainties.map { it.name })
        assertThat(public.candidates.map { it.id.fileId to it.id.ordinal })
            .containsExactlyElementsOf(internal.candidates.map { it.fileId.value to it.ordinal })
        assertThat(public.packageSpecifications.map { it.id.fileId to it.id.ordinal })
            .containsExactlyElementsOf(internal.packageSpecifications.map { it.fileId.value to it.ordinal })
        assertThat(public.failures.map { it.fileId to it.exceptionType })
            .containsExactlyElementsOf(internal.failures.map { it.fileId.value to it.exceptionType })
        assertThat(public.target).isSameAs(target)
        return public
    }

    @Test
    fun packageMemberOverloadsAreSpecificationOccurrencesWithInventoryIds() {
        val p = project(
            "spec.sql" to "CREATE PACKAGE p AS PROCEDURE run(a NUMBER); PROCEDURE run(a NUMBER, b VARCHAR2 DEFAULT 'x'); " +
                "FUNCTION run RETURN NUMBER; PROCEDURE other; END p;",
            "body.sql" to "CREATE PACKAGE BODY p AS PROCEDURE run(a NUMBER) IS BEGIN NULL; END; END p;"
        )
        val result = discover(p, member("p", "run"))
        assertThat(result.status).isEqualTo(FOUND)
        assertThat(result.candidates.map { it.kind }).containsExactly(
            ProjectDeclarationKind.PACKAGE_PROCEDURE, ProjectDeclarationKind.PACKAGE_PROCEDURE, ProjectDeclarationKind.PACKAGE_FUNCTION
        )
        assertThat(result.candidates).allMatch { it is ProjectPackageSubprogramView && it.role == ProjectDeclarationRole.SPECIFICATION }
        assertThat(result.candidates[2]).isInstanceOf(ProjectFunctionView::class.java)
        assertThat(result.candidates[1].parameters.map { it.isDefaultPresent }).containsExactly(false, true)
        assertThat(result.candidates.map { it.fileId }).containsOnly("spec.sql")
        val inventory = p.snapshot.findPackageSubprograms(n("p"), ProjectIdentifier.fromSource("run")).declarations
        assertThat(inventory.map { it.role }).contains(ProjectDeclarationRole.BODY)
        assertThat(result.candidates.map { it.id })
            .containsExactlyElementsOf(inventory.filter { it.role == ProjectDeclarationRole.SPECIFICATION }.map { it.id })
        assertThat(result.packageSpecifications.single().id)
            .isEqualTo(p.snapshot.declarations.single { it.kind == ProjectDeclarationKind.PACKAGE }.id)
        assertThat(discover(p, written("p", "run")).candidates.map { it.id }).isEqualTo(result.candidates.map { it.id })
    }

    @Test
    fun standaloneAndWrittenSegmentCounts() {
        val p = project(
            "s.sql" to "CREATE PROCEDURE simple IS BEGIN NULL; END;\n/\nCREATE FUNCTION app.f RETURN NUMBER IS BEGIN RETURN 1; END;",
            "a.sql" to "CREATE PACKAGE a.b AS PROCEDURE c; END;"
        )
        assertThat(discover(p, standalone("simple")).status).isEqualTo(FOUND)
        assertThat(discover(p, standalone("app", "f")).candidates.single().role).isEqualTo(ProjectDeclarationRole.STANDALONE)
        // No schema inference in either direction.
        assertThat(discover(p, standalone("f")).status).isEqualTo(NOT_FOUND_IN_PROJECT)
        assertThat(discover(p, standalone("x", "simple")).status).isEqualTo(NOT_FOUND_IN_PROJECT)
        assertThat(discover(p, written("simple")).status).isEqualTo(FOUND)
        assertThat(discover(p, written("app", "f")).status).isEqualTo(FOUND)
        assertThat(discover(p, written("a", "b", "c")).status).isEqualTo(FOUND)
        // Unsupported shapes have no interpretation.
        assertThat(discover(p, written("q", "a", "b", "c")).status).isEqualTo(NOT_FOUND_IN_PROJECT)
        assertThat(discover(p, standalone("a", "b", "c")).status).isEqualTo(NOT_FOUND_IN_PROJECT)
        // A one-segment written name is never a member of a package.
        assertThat(discover(p, written("c")).status).isEqualTo(NOT_FOUND_IN_PROJECT)
    }

    @Test
    fun namespaceCompetitionIsAmbiguousWithOneCandidateAndExplicitFormsKeepTheirNamespace() {
        val p = project("pkg.sql" to "CREATE PACKAGE x AS PROCEDURE other; END x;", "y.sql" to "CREATE PROCEDURE x.y IS BEGIN NULL; END;")
        val result = discover(p, written("x", "y"))
        assertThat(result.status).isEqualTo(AMBIGUOUS_TARGET)
        assertThat(result.uncertainties).containsExactly(NAMESPACE_COMPETITION)
        assertThat(result.candidates.single().role).isEqualTo(ProjectDeclarationRole.STANDALONE)
        assertThat(result.packageSpecifications).hasSize(1)
        assertThat(discover(p, standalone("x", "y")).status).isEqualTo(FOUND)
        assertThat(discover(p, member("x", "y")).status).isEqualTo(NOT_FOUND_IN_PROJECT)
    }

    @Test
    fun duplicateSpecificationsAreAmbiguousWithZeroOrManyCandidates() {
        val empty = project("a.sql" to "CREATE PACKAGE p AS PROCEDURE other; END p;", "b.sql" to "CREATE PACKAGE p AS PROCEDURE other; END p;")
        val none = discover(empty, member("p", "run"))
        assertThat(none.status).isEqualTo(AMBIGUOUS_TARGET)
        assertThat(none.uncertainties).containsExactly(DUPLICATE_SPECIFICATION)
        assertThat(none.candidates).isEmpty()
        assertThat(none.packageSpecifications.map { it.fileId }).containsExactly("a.sql", "b.sql")

        val same = project("a.sql" to "CREATE PACKAGE p AS PROCEDURE run; END p;", "b.sql" to "CREATE PACKAGE p AS PROCEDURE run; END p;")
        val both = discover(same, member("p", "run"))
        assertThat(both.status).isEqualTo(AMBIGUOUS_TARGET)
        // Canonically equal names in distinct occurrences stay distinct.
        assertThat(both.candidates.map { it.id }).doesNotHaveDuplicates().hasSize(2)
        assertThat(both.candidates.map { it.qualifiedName }.distinct()).hasSize(1)
    }

    @Test
    fun bodyWithoutSpecificationAndCleanAbsence() {
        val body = project("b.sql" to "CREATE PACKAGE BODY h AS PROCEDURE hidden IS BEGIN NULL; END; END h;")
        for (target in listOf(member("h", "hidden"), written("h", "hidden"))) {
            val result = discover(body, target)
            assertThat(result.status).isEqualTo(UNKNOWN_PACKAGE)
            assertThat(result.candidates).isEmpty()
            assertThat(result.uncertainties).isEmpty()
        }
        val clean = discover(project("p.sql" to "CREATE PACKAGE p AS PROCEDURE other; END p;"), member("p", "missing"))
        assertThat(clean.status).isEqualTo(NOT_FOUND_IN_PROJECT)
        assertThat(clean.packageSpecifications).hasSize(1)
        assertThat(discover(project("p.sql" to "CREATE PACKAGE p AS PROCEDURE other; END p;"), member("q", "run")).status)
            .isEqualTo(UNKNOWN_PACKAGE)
    }

    @Test
    fun coverageGapsArePartialEvenWithoutCandidates() {
        val ptf = "FUNCTION run(t TABLE) RETURN TABLE PIPELINED ROW POLYMORPHIC USING p;"
        val retained = discover(project("p.sql" to "CREATE PACKAGE p AS PROCEDURE run(a NUMBER); $ptf END p;"), member("p", "run"))
        assertThat(retained.status).isEqualTo(PARTIAL_DECLARATIONS)
        assertThat(retained.uncertainties).containsExactly(ProjectCallUncertainty.PARTIAL_DECLARATIONS)
        assertThat(retained.candidates).hasSize(1)

        val dropped = discover(project("p.sql" to "CREATE PACKAGE p AS $ptf END p;"), member("p", "run"))
        assertThat(dropped.status).isEqualTo(PARTIAL_DECLARATIONS)
        assertThat(dropped.candidates).isEmpty()

        // The gap is name-scoped.
        assertThat(discover(project("p.sql" to "CREATE PACKAGE p AS PROCEDURE other; $ptf END p;"), member("p", "other")).status)
            .isEqualTo(FOUND)

        val conditional = project("p.sql" to "CREATE PACKAGE p AS PROCEDURE a; \$IF \$\$flag \$THEN PROCEDURE b; \$END END p;")
        for (name in listOf("a", "zz")) {
            val result = discover(conditional, member("p", name))
            assertThat(result.status).describedAs(name).isEqualTo(PARTIAL_DECLARATIONS)
        }
    }

    @Test
    fun identifierIdentityFollowsOracleRules() {
        val p = project("p.sql" to "CREATE PACKAGE p AS PROCEDURE \"RUN\"; PROCEDURE \"run\"; END p;")
        val upper = discover(p, member("p", "run"))
        assertThat(upper.candidates.single().localName.originalSpelling).isEqualTo("\"RUN\"")
        assertThat(discover(p, ProjectCallTarget.packageMember(n("\"P\""), ProjectIdentifier.fromSource("\"RUN\""))).candidates.map { it.id })
            .isEqualTo(upper.candidates.map { it.id })
        val lower = discover(p, member("p", "\"run\""))
        assertThat(lower.candidates.single().localName.originalSpelling).isEqualTo("\"run\"")
        assertThat(discover(p, member("p", "\"Run\"")).status).isEqualTo(NOT_FOUND_IN_PROJECT)
    }

    @Test
    fun targetValueEquality() {
        val plain = member("p", "run")
        val quoted = ProjectCallTarget.packageMember(n("\"P\""), ProjectIdentifier.fromSource("\"RUN\""))
        assertThat(plain).isEqualTo(quoted).hasSameHashCodeAs(quoted)
        assertThat(plain).isNotEqualTo(member("p", "\"run\""))
        assertThat(plain).isNotEqualTo(written("p", "run"))
        assertThat(plain.name).isEqualTo(n("p", "run"))
        assertThat(plain.packageName).contains(n("p"))
        assertThat(written("p", "run").packageName).isEmpty
        assertThat(ProjectCallTarget.packageMember(n("a", "p"), ProjectIdentifier.fromSource("f")).name).isEqualTo(n("a", "p", "f"))
    }

    @Test
    fun readerFailurePreservesCandidatesAndEveryUncertainty() {
        val p = project(
            "p.sql" to "CREATE PACKAGE x AS PROCEDURE run; FUNCTION run(t TABLE) RETURN TABLE PIPELINED ROW POLYMORPHIC USING x; END x;",
            "s.sql" to "CREATE PROCEDURE x.run IS BEGIN NULL; END;",
            failed = true
        )
        val result = discover(p, written("x", "run"))
        assertThat(result.status).isEqualTo(INCOMPLETE_INDEX)
        assertThat(result.uncertainties).containsExactlyInAnyOrder(NAMESPACE_COMPETITION, ProjectCallUncertainty.PARTIAL_DECLARATIONS)
        assertThat(result.candidates.map { it.role }).containsExactly(ProjectDeclarationRole.SPECIFICATION, ProjectDeclarationRole.STANDALONE)
        assertThat(result.failures.single().fileId).isEqualTo("failed.sql")
        assertThat(discover(p, member("nothing", "here")).status).isEqualTo(INCOMPLETE_INDEX)
    }

    @Test
    fun notPreparedAndPreparedEmptySnapshots() {
        val notPrepared = ProjectSnapshot.create(ProjectAnalysisContext.NOT_PREPARED)
        val result = discover(Project(notPrepared, ProjectAnalysisContext.NOT_PREPARED), member("p", "run"))
        assertThat(result.status).isEqualTo(NOT_PREPARED)
        assertThat(result.candidates).isEmpty()
        assertThat(result.packageSpecifications).isEmpty()
        assertThat(result.failures).isEmpty()

        val empty = project()
        assertThat(empty.snapshot.preparationState).isEqualTo(ProjectPreparationState.PREPARED_EMPTY)
        assertThat(discover(empty, written("f")).status).isEqualTo(NOT_FOUND_IN_PROJECT)
        assertThat(discover(empty, member("p", "f")).status).isEqualTo(UNKNOWN_PACKAGE)
    }

    @Test
    fun collectionsAreUnmodifiable() {
        val result = discover(project("p.sql" to "CREATE PACKAGE p AS PROCEDURE run; END p;", failed = true), member("p", "run"))
        @Suppress("UNCHECKED_CAST")
        listOf(result.candidates, result.packageSpecifications, result.failures).forEach {
            assertThatThrownBy { (it as MutableList<Any?>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        }
        assertThatThrownBy { (result.uncertainties as MutableSet<ProjectCallUncertainty>).add(NAMESPACE_COMPETITION) }
            .isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun serialConcurrentAndReversedPreparationAgree() {
        val files = listOf(
            "a.sql" to "CREATE PACKAGE x AS PROCEDURE run(a NUMBER); PROCEDURE run(b NUMBER); END x;",
            "b.sql" to "CREATE PACKAGE x AS PROCEDURE run; END x;",
            "c.sql" to "CREATE PROCEDURE x.run IS BEGIN NULL; END;",
            "d.sql" to "CREATE PACKAGE BODY x AS PROCEDURE run(a NUMBER) IS BEGIN NULL; END; END x;"
        )
        val targets = listOf(written("x", "run"), member("x", "run"), standalone("x", "run"))
        fun summary(p: Project) = targets.map { target ->
            discover(p, target).let { r -> Triple(r.status, r.uncertainties, r.candidates.map { it.id.fileId to it.id.ordinal }) }
        }
        val serial = summary(project(*files.toTypedArray()))
        assertThat(summary(project(*files.toTypedArray(), concurrent = true))).isEqualTo(serial)
        assertThat(summary(project(*files.reversed().toTypedArray()))).isEqualTo(serial)
    }
}
