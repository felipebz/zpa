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
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/** External metadata reaching checks through the scanner, resolved against the prepared project. */
@OptIn(ZpaExperimentalApi::class)
class ProjectExternalContextTest {
    private val hr = ProjectIdentifier.fromSource("\"HR\"")
    private val metadata = ProjectExternalMetadata.builder("fixture").completeSchema("HR").relation(
        ProjectExternalRelation.of(
            "HR", "EMPLOYEES", true, listOf(
                ProjectExternalColumn.of("ID", 1, ProjectExternalDatatype.of("NUMBER", null, null, null, 8, 0)),
                ProjectExternalColumn.of("SALARY", 2, ProjectExternalDatatype.of("NUMBER", null, null, null, 10, 2))
            )
        )
    ).build()

    private class Fixture(private val id: String, private val source: String) : PlSqlFile {
        override fun contents() = source
        override fun fileName() = id
        override fun path(): Path = Path.of(id)
        override fun type() = PlSqlFile.Type.MAIN
    }

    /** A check that describes every package value of the specification it visits, as a semantic check would. */
    private class AnchorProbe : PlSqlCheck() {
        val results = mutableMapOf<String, ProjectExternalTypeDescription>()
        val contexts = mutableListOf<ProjectExternalContext>()

        init {
            subscribeTo(PlSqlGrammar.CREATE_PACKAGE)
        }

        override fun visitNode(node: AstNode) {
            val analysis = projectAnalysis()
            val external = analysis.externalContext
            contexts += external
            for (view in analysis.snapshot.declarations.filterIsInstance<ProjectPackageValueView>()) {
                results[view.qualifiedName.toString()] = analysis.snapshot.describeExternalType(
                    view.typeRef, view.owner, external.metadata, external.compilationSchema(view.owner).orElse(null)
                )
            }
        }
    }

    private val source = "CREATE PACKAGE p AS sal employees.salary%TYPE; r employees%ROWTYPE; END p;\n/\n" +
        "CREATE PACKAGE app.q AS sal employees.salary%TYPE; END q;"

    private fun scan(context: ProjectAnalysisContext): AnchorProbe {
        val probe = AnchorProbe()
        AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)
            .scanFile(Fixture("p.sql", source), listOf(probe), FileId("p.sql"))
        return probe
    }

    @Test
    fun checksResolveAnchorsWithTheSuppliedMetadataOverTheSamePreparedIndex() {
        val reads = AtomicInteger()
        val prepared = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation().prepare(listOf(ProjectSource(FileId("p.sql")) { reads.incrementAndGet(); source }), concurrent = false)
        )
        val context = prepared.withExternalContext(ProjectExternalContext.of(metadata, hr))
        assertThat(context.state).isSameAs(prepared.state)
        // A run-owned snapshot sees the same prepared index without reading again.
        val ownSnapshot = context.createSnapshot()
        assertThat(ownSnapshot.preparationState).isEqualTo(ProjectPreparationState.PREPARED_WITH_DECLARATIONS)
        assertThat(ownSnapshot.findDeclarations(ProjectQualifiedName.of("p", "sal")).declarations).hasSize(1)
        val probe = scan(context)
        assertThat(reads.get()).isEqualTo(1)
        assertThat(probe.contexts).isNotEmpty().allSatisfy { assertThat(it).isSameAs(context.externalContext) }

        val sal = probe.results.getValue("p.sal")
        assertThat(sal.status).isEqualTo(ProjectExternalTypeDescription.Status.RESOLVED)
        val type = sal.lookup.get().column.get().datatype.get()
        assertThat(type.precision).isEqualTo(OptionalInt.of(10))
        assertThat(type.scale).isEqualTo(OptionalInt.of(2))
        val row = probe.results.getValue("p.r")
        assertThat(row.status).isEqualTo(ProjectExternalTypeDescription.Status.RESOLVED)
        assertThat(row.lookup.get().relation.get().rowFields.map { it.name.originalSpelling }).containsExactly("\"ID\"", "\"SALARY\"")
        // app.q compiles in APP, whatever the default schema: APP is not covered by the metadata.
        assertThat(probe.results.getValue("app.q.sal").status).isEqualTo(ProjectExternalTypeDescription.Status.INCOMPLETE)
    }

    @Test
    fun withoutAnExternalContextNothingIsResolvedExternally() {
        val prepared = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation().prepare(listOf(ProjectSource(FileId("p.sql")) { source }), concurrent = false)
        )
        val probe = scan(prepared)
        assertThat(probe.contexts).allSatisfy { assertThat(it).isSameAs(ProjectExternalContext.none()) }
        assertThat(probe.results.getValue("p.sal").status).isEqualTo(ProjectExternalTypeDescription.Status.UNAVAILABLE)
        // With metadata but no default schema, unqualified units keep the uncertainty.
        val noSchema = scan(prepared.withExternalContext(ProjectExternalContext.of(metadata, null)))
        assertThat(noSchema.results.getValue("p.sal").status).isEqualTo(ProjectExternalTypeDescription.Status.SCHEMA_CONTEXT_MISSING)
    }

    @Test
    fun compilationSchemaPrefersTheWrittenOwner() {
        val context = ProjectExternalContext.of(metadata, hr)
        assertThat(context.compilationSchema(ProjectQualifiedName.of("p"))).isEqualTo(Optional.of(hr))
        assertThat(context.compilationSchema(ProjectQualifiedName.of("app", "q"))).isEqualTo(Optional.of(ProjectIdentifier.fromSource("APP")))
        assertThat(context.compilationSchema(ProjectQualifiedName.of("a", "b", "c"))).isEmpty
        assertThat(ProjectExternalContext.of(metadata, null).compilationSchema(ProjectQualifiedName.of("p"))).isEmpty
        assertThat(ProjectExternalContext.none().metadata).isSameAs(ProjectExternalMetadata.none())
        assertThat(ProjectExternalContext.none().defaultCompilationSchema).isEmpty
    }
}
