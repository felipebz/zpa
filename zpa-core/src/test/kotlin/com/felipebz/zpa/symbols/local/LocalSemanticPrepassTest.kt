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
package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.Token
import com.felipebz.zpa.TestPlSqlVisitorRunner
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.squid.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.*

/** The pipeline's local pre-pass: its facts are present on every pipeline path, deterministic and AST-detached. */
@OptIn(ZpaExperimentalApi::class)
class LocalSemanticPrepassTest {

    private class Source(private val name: String, private val text: String, private val failure: (() -> Throwable)? = null) : PlSqlFile {
        override fun contents(): String = failure?.let { throw it() } ?: text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    /** Captures the facts the pre-pass left, once every visitor has run, and the scan's project snapshot. */
    private class Capture : PlSqlCheck() {
        var facts: LocalSemanticFacts? = null
        var symbols = -1
        var snapshot: ProjectSnapshot? = null
        override fun leaveFile(node: AstNode) {
            facts = context.localSemanticFacts
            symbols = context.symbolTable.symbols.size
            snapshot = projectAnalysis().snapshot
        }
    }

    private fun prepared(text: String) =
        ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(listOf(ProjectSource(FileId("shadow.sql")) { text }), concurrent = false))

    /** The pipeline with [text] as its own project; returns what the capture saw. */
    private fun run(text: String, failure: (() -> Throwable)? = null): Capture {
        val project = prepared(text)
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).parse(text)
        val context = PlSqlVisitorContext(root, Source("shadow.sql", text, failure), null)
        val capture = Capture()
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("shadow.sql")).all + capture)
            .walk(context, SemanticScanCapabilities(createProjectAnalysis(project)))
        return capture
    }

    @Test
    fun aFailureLeavesTheFactsAbsentAndTheLegacyOutputUnchanged() {
        val source = "declare\n  v number;\nbegin\n  v := 1;\nend;\n/\n"
        val healthy = run(source)
        val failed = run(source) { IllegalStateException("injected failure") }
        assertThat(healthy.facts).isNotNull
        assertThat(failed.facts).isNull()
        assertThat(failed.symbols).isEqualTo(healthy.symbols).isPositive
    }

    @Test
    fun aStackOverflowLeavesTheFactsAbsentAndTheLegacyOutputUnchanged() {
        val source = "declare\n  v number;\nbegin\n  v := 1;\nend;\n/\n"
        val healthy = run(source)
        val overflowed = run(source) { StackOverflowError("injected") }
        assertThat(overflowed.facts).isNull()
        assertThat(overflowed.symbols).isEqualTo(healthy.symbols).isPositive
    }

    private val processar = "create package pkg as\n  procedure processar(p number);\n  procedure processar(p date);\nend;\n/\n"

    private fun forwarded(facts: LocalSemanticFacts) = facts.calls.single { it.outcome == LocalCallOutcome.PROJECT && it.target.toString() == "pkg.processar" }.typed!!

    @Test
    fun forwardedCallsUseTheScansOwnSnapshot() {
        val source = processar + "declare\n  d date;\nbegin\n  pkg.processar(d);\nend;\n/\n"
        val capture = run(source)
        val typed = forwarded(capture.facts!!)
        assertThat(typed.status).isEqualTo(ProjectTypedCallResolution.Status.RESOLVED)
        // Issued in the scan's declaration-ID scope: the snapshot checks see knows the selected declaration.
        assertThat(capture.snapshot!!.occurrenceOf(typed.selected.get().declaration.id)).isNotNull
    }

    @Test
    fun forwardedCallsUseTheRunsExternalMetadata() {
        // A column of a table only the metadata describes, read in the unit's compilation schema: with the run's
        // external context the actual is a NUMBER, without it unknown.
        val source = processar + "create procedure p is\n  v employees.salary%type;\nbegin\n  pkg.processar(v);\nend;\n/\n"
        val metadata = ProjectExternalMetadata.builder("fixture").completeSchema("HR").relation(
            ProjectExternalRelation.of("HR", "EMPLOYEES", true, listOf(ProjectExternalColumn.of("SALARY", 1, ProjectExternalDatatype.of("NUMBER", null, null, null, 10, 2))))
        ).build()
        fun scan(context: ProjectAnalysisContext): ProjectTypedCallResolution {
            val capture = Capture()
            AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context).scanFile(Source("shadow.sql", source), listOf(capture), FileId("shadow.sql"))
            return forwarded(capture.facts!!)
        }
        assertThat(scan(prepared(source).withExternalContext(ProjectExternalContext.of(metadata, ProjectIdentifier.fromSource("HR")))).status).isEqualTo(ProjectTypedCallResolution.Status.RESOLVED)
        assertThat(scan(prepared(source)).status).isEqualTo(ProjectTypedCallResolution.Status.AMBIGUOUS)
    }

    @Test
    fun thePrepassRunsInProductionTestkitAndToolingPaths(@TempDir dir: File) {
        val source = "declare\n  v number;\nbegin\n  v := 1;\nend;\n/\n"
        // Production: the scanner.
        val production = Capture()
        AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, prepared(source)).scanFile(Source("shadow.sql", source), listOf(production), FileId("shadow.sql"))
        // Testkit: the runner the check verifier uses.
        val testkit = Capture()
        val file = File(dir, "shadow.sql").apply { writeText(source) }
        TestPlSqlVisitorRunner.scanFile(file, null, testkit)
        // Tooling: the pipeline the inspection service creates.
        val tooling = Capture()
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8)).parse(source)
        PlSqlAstWalker(SemanticAnalysisPipeline.forTooling(ProjectAnalysisContext.NOT_PREPARED).create(FileId("shadow.sql")).all + tooling)
            .walk(PlSqlVisitorContext(root, Source("shadow.sql", source), null))
        for (capture in listOf(production, testkit, tooling)) {
            assertThat(capture.facts!!.references.map { it.name.originalSpelling }).contains("v")
        }
    }

    @Test
    fun factsAreDeterministicAndDetached() {
        val source = "create type t as object (a number);\n/\ncreate package body b is\n  procedure p(x t) is\n    subtype s is date;\n    d s;\n" +
            "  begin\n    p(x);\n    pkg.processar(d);\n  end;\nend;\n/\n"
        val first = run(source).facts!!
        val second = run(source).facts!!
        assertThat(second.references.map { it.toString() }).isEqualTo(first.references.map { it.toString() })
        assertThat(second.resolutions.map { it.toString() }).isEqualTo(first.resolutions.map { it.toString() })
        assertThat(second.calls.map { it.toString() }).isEqualTo(first.calls.map { it.toString() })
        val reached = reachable(first)
        assertThat(reached.filter { it is AstNode || it is Token || it is Symbol || it is ProjectSnapshot }).isEmpty()
    }

    /** Objects reachable through fields, stopping at library internals and at the project's API values. */
    private fun reachable(root: Any): List<Any> {
        val seen = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        val pending = ArrayDeque(listOf(root))
        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            if (!seen.add(current)) continue
            if (current.javaClass.name.startsWith("com.felipebz.zpa.api.project.")) continue
            when (current) {
                is Iterable<*> -> current.filterNotNull().forEach(pending::add)
                is Map<*, *> -> (current.keys + current.values).filterNotNull().forEach(pending::add)
                else -> {
                    var type: Class<*>? = current.javaClass
                    while (type != null && !type.name.startsWith("java.") && !type.name.startsWith("kotlin.")) {
                        for (field in type.declaredFields) {
                            if (field.type.isPrimitive || java.lang.reflect.Modifier.isStatic(field.modifiers)) continue
                            field.isAccessible = true
                            field.get(current)?.let(pending::add)
                        }
                        type = type.superclass
                    }
                }
            }
        }
        return seen.toList()
    }
}
