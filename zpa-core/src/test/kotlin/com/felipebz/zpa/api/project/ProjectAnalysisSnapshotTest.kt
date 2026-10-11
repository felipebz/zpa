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

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectIndexPreparationFailure
import com.felipebz.zpa.project.ProjectIndexPreparationResult
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.project.ProjectSymbolIndexBuilder
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ZpaExperimentalApi::class)
class ProjectAnalysisSnapshotTest {
    private val spec = "CREATE PACKAGE p AS TYPE t IS RECORD (a NUMBER); PROCEDURE work(a NUMBER); END p;"
    private val body = "CREATE PACKAGE BODY p AS PROCEDURE work(a NUMBER) IS BEGIN NULL; END work; END p;"
    private val reads = AtomicInteger()

    private fun prepare(vararg sources: Pair<String, String>): ProjectIndexPreparationResult =
        ProjectIndexPreparation().prepare(
            sources.map { (id, text) -> ProjectSource(FileId(id)) { reads.incrementAndGet(); text } },
            concurrent = false
        )

    private fun scan(context: ProjectAnalysisContext, probe: SnapshotProbe, source: String = body) {
        AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)
            .scanFile(Fixture("b.sql", source), listOf(probe), FileId("b.sql"))
    }

    private class Fixture(private val id: String, private val source: String) : PlSqlFile {
        override fun contents() = source
        override fun fileName() = id
        override fun path(): Path = Path.of(id)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private class SnapshotProbe : PlSqlCheck() {
        val snapshots = mutableListOf<List<ProjectSnapshot>>()
        val contextual = mutableListOf<PackageSpecificationResolution.Status>()
        private var current = mutableListOf<ProjectSnapshot>()

        init {
            subscribeTo(PlSqlGrammar.PROCEDURE_DECLARATION)
        }

        override fun startScan() {
            super.startScan()
            current = mutableListOf()
            snapshots += current
        }

        override fun visitNode(node: AstNode) {
            current += projectAnalysis().snapshot
            current += projectAnalysis().snapshot
            contextual += projectAnalysis().resolvePackageSpecification(node).status
        }
    }

    private fun n(vararg s: String) = ProjectQualifiedName.of(*s)

    @Test
    fun preparedScanExposesOneStableSnapshotBackedByThePreparedResult() {
        val result = prepare("s.sql" to spec, "b.sql" to body)
        val readsAfterPreparation = reads.get()
        val probe = SnapshotProbe()
        scan(ProjectAnalysisContext.prepared(result), probe)

        val snapshot = probe.snapshots.single().first()
        assertThat(probe.snapshots.single()).allSatisfy { assertThat(it).isSameAs(snapshot) }
        assertThat(reads.get()).isEqualTo(readsAfterPreparation)
        assertThat(snapshot.preparationState).isEqualTo(ProjectPreparationState.PREPARED_WITH_DECLARATIONS)
        assertThat(snapshot.attemptedFileCount).isEqualTo(2)
        assertThat(snapshot.fileIds).containsExactly("b.sql", "s.sql")

        // Same query semantics as independent preparation; only the ID scope differs.
        val independent = ProjectSnapshots.prepare(
            listOf(ProjectSourceInput.ofText("s.sql", spec), ProjectSourceInput.ofText("b.sql", body)), false
        )
        assertThat(snapshot.declarations.map { it.kind to it.qualifiedName })
            .isEqualTo(independent.declarations.map { it.kind to it.qualifiedName })
        assertThat(snapshot.declarations.map { it.id }).doesNotContainAnyElementsOf(independent.declarations.map { it.id })
        assertThat(snapshot.resolveNamedType(n("t"), n("p")).status)
            .isEqualTo(independent.resolveNamedType(n("t"), n("p")).status)
        assertThat(snapshot.resolveNamedType(n("t"), n("p")).declaration.get().id)
            .isEqualTo(snapshot.findDeclarations(n("p", "t")).declarations.single().id)
        val candidates = snapshot.findPackageSubprograms(n("p"), ProjectIdentifier.fromSource("work"))
        assertThat(candidates.declarations).hasSize(2)
        val bodyId = candidates.declarations.single { it.role == ProjectDeclarationRole.BODY }.id
        val correlation = snapshot.correlateSpecification(bodyId)
        assertThat(correlation.status).isEqualTo(ProjectSpecificationCorrelation.Status.RESOLVED)
        assertThat(candidates.declarations.map { it.id }).contains(correlation.specification.get().id)
        assertThat(reads.get()).isEqualTo(readsAfterPreparation)

        // Contextual callback behavior is unchanged.
        assertThat(probe.contextual).containsExactly(PackageSpecificationResolution.Status.RESOLVED)
    }

    @Test
    fun mapsEveryPreparationState() {
        val empty = ProjectIndexPreparationResult(ProjectSymbolIndexBuilder().build(), 1, emptyList())
        val failed = ProjectIndexPreparationResult(
            prepare("s.sql" to spec).index, 2, listOf(ProjectIndexPreparationFailure(FileId("x.sql"), "test.failure"))
        )
        val emptySnapshot = SnapshotProbe().also { scan(ProjectAnalysisContext.prepared(empty), it) }.snapshots.single().first()
        assertThat(emptySnapshot.preparationState).isEqualTo(ProjectPreparationState.PREPARED_EMPTY)
        assertThat(emptySnapshot.declarations).isEmpty()

        val failedSnapshot = SnapshotProbe().also { scan(ProjectAnalysisContext.prepared(failed), it) }.snapshots.single().first()
        assertThat(failedSnapshot.preparationState).isEqualTo(ProjectPreparationState.PREPARED_WITH_FAILURES)
        assertThat(failedSnapshot.failures.single().fileId).isEqualTo("x.sql")
        assertThat(failedSnapshot.failures.single().exceptionType).isEqualTo("test.failure")
        assertThat(failedSnapshot.successfulFileCount).isEqualTo(1)
        assertThat(failedSnapshot.declarations).isNotEmpty
        assertThat(failedSnapshot.resolveNamedType(n("t"), n("p")).status)
            .isEqualTo(ProjectNamedTypeResolution.Status.INCOMPLETE_INDEX)

        val unprepared = SnapshotProbe().also { scan(ProjectAnalysisContext.NOT_PREPARED, it) }.snapshots.single().first()
        assertThat(unprepared.preparationState).isEqualTo(ProjectPreparationState.NOT_PREPARED)
        assertThat(unprepared.declarations).isEmpty()
        assertThat(unprepared.failures).isEmpty()
        assertThat(unprepared.attemptedFileCount).isZero
        assertThat(unprepared.successfulFileCount).isZero
        assertThat(unprepared.declarationsFor("b.sql").status).isEqualTo(ProjectInventory.Status.NOT_PREPARED)
        assertThat(unprepared.resolveNamedType(n("t")).status).isEqualTo(ProjectNamedTypeResolution.Status.NOT_PREPARED)
        assertThat(unprepared.findPackageSubprograms(n("p"), ProjectIdentifier.fromSource("work")).status)
            .isEqualTo(ProjectInventory.Status.NOT_PREPARED)
    }

    @Test
    fun preparedThenUnpreparedThenPreparedScansKeepIndependentSnapshots() {
        val probe = SnapshotProbe()
        scan(ProjectAnalysisContext.prepared(prepare("s.sql" to spec, "b.sql" to body)), probe)
        val a = probe.snapshots.last().first()
        val aIds = a.declarations.map { it.id }
        val aKinds = a.declarations.map { it.kind }

        scan(ProjectAnalysisContext.NOT_PREPARED, probe)
        val b = probe.snapshots.last().first()

        scan(ProjectAnalysisContext.prepared(prepare("s.sql" to spec, "b.sql" to body)), probe)
        val c = probe.snapshots.last().first()

        assertThat(b).isNotSameAs(a)
        assertThat(c).isNotSameAs(a).isNotSameAs(b)
        assertThat(b.preparationState).isEqualTo(ProjectPreparationState.NOT_PREPARED)
        assertThat(b.declarations).isEmpty()
        assertThat(a.declarations.map { it.id }).isEqualTo(aIds)
        assertThat(a.declarations.map { it.kind }).isEqualTo(aKinds)
        assertThat(a.preparationState).isEqualTo(ProjectPreparationState.PREPARED_WITH_DECLARATIONS)
        assertThat(c.declarations.map { it.id.ordinal }).isEqualTo(aIds.map { it.ordinal })
        assertThat(c.declarations.map { it.id }).doesNotContainAnyElementsOf(aIds)
        assertThat(probe.contextual).containsExactly(
            PackageSpecificationResolution.Status.RESOLVED,
            PackageSpecificationResolution.Status.NOT_PREPARED,
            PackageSpecificationResolution.Status.RESOLVED
        )
        // A body ID from A is rejected by C.
        val aBody = a.declarations.first { it.role == ProjectDeclarationRole.BODY && it is ProjectPackageSubprogramView }.id
        org.assertj.core.api.Assertions.assertThatThrownBy { c.correlateSpecification(aBody) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun directAnalysisWithoutProjectExposesAnUnpreparedSnapshotPerRun() {
        val probe = SnapshotProbe()
        val root = com.felipebz.zpa.parser.PlSqlParser.create(
            com.felipebz.zpa.squid.PlSqlConfiguration(StandardCharsets.UTF_8, true)
        ).parse(body)
        probe.scanFile(com.felipebz.zpa.api.PlSqlVisitorContext(root, Fixture("b.sql", body), null))
        probe.scanFile(com.felipebz.zpa.api.PlSqlVisitorContext(root, Fixture("b.sql", body), null))
        val first = probe.snapshots[0].first()
        val second = probe.snapshots[1].first()
        assertThat(first.preparationState).isEqualTo(ProjectPreparationState.NOT_PREPARED)
        assertThat(second.preparationState).isEqualTo(ProjectPreparationState.NOT_PREPARED)
        assertThat(second).isNotSameAs(first)
        assertThat(probe.contextual).containsOnly(PackageSpecificationResolution.Status.NOT_PREPARED)
    }
}
