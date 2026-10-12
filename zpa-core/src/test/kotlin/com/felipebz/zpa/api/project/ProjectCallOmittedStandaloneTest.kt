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
import com.felipebz.zpa.project.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import com.felipebz.zpa.project.ProjectCallKind as InternalCallKind

/**
 * Standalone callables whose heading names them but whose signature cannot be represented (an Oracle-valid
 * polymorphic table function with `TABLE` formals): they are namespace evidence and incomplete coverage, never
 * candidates. Every case is checked through the internal finder and binder and the public snapshot API.
 */
@OptIn(ZpaExperimentalApi::class)
class ProjectCallOmittedStandaloneTest {
    private val spec = "CREATE PACKAGE app AS FUNCTION fn(a NUMBER) RETURN NUMBER; END app;"
    private val omittedPtf = "CREATE FUNCTION app.fn(tab TABLE) RETURN TABLE PIPELINED ROW POLYMORPHIC USING impl;"
    private val retained = "CREATE FUNCTION app.fn(a NUMBER) RETURN NUMBER IS BEGIN RETURN a; END;"

    private class Project(val snapshot: ProjectSnapshot, val context: ProjectAnalysisContext)

    private fun project(vararg files: Pair<String, String>, failed: Boolean = false, concurrent: Boolean = false): Project {
        val sources = files.map { (id, text) -> ProjectSource(FileId(id)) { text } }.toMutableList()
        if (failed) sources += ProjectSource(FileId("failed.sql")) { throw IllegalStateException("reader failed") }
        val context = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(sources, concurrent))
        return Project(ProjectSnapshot.create(context), context)
    }

    private fun n(vararg segments: String) = ProjectQualifiedName.of(*segments)
    private fun written(vararg segments: String) = ProjectCallTarget.written(n(*segments))
    private fun standalone(vararg segments: String) = ProjectCallTarget.standalone(n(*segments))
    private fun member(pkg: String, name: String) = ProjectCallTarget.packageMember(n(pkg), ProjectIdentifier.fromSource(name))

    private data class Outcome(
        val discovery: String,
        val resolution: String,
        val uncertainties: Set<String>,
        val candidates: List<String>,
        val selected: String?
    )

    /** One positional actual in a FUNCTION context; public and internal results must agree field by field. */
    private fun outcome(project: Project, target: ProjectCallTarget): Outcome {
        val arguments = ProjectCallArguments.of(listOf(ProjectCallArgument.positional()))
        val internalDiscovery = ProjectCallCandidateFinder(project.context).find(target.toInternal())
        val publicDiscovery = project.snapshot.findCallCandidates(target)
        assertThat(publicDiscovery.status.name).isEqualTo(internalDiscovery.status.name)
        assertThat(publicDiscovery.uncertainties.map { it.name }).containsExactlyInAnyOrderElementsOf(internalDiscovery.uncertainties.map { it.name })
        assertThat(publicDiscovery.candidates.map { "${it.id.fileId}#${it.id.ordinal}" })
            .containsExactlyElementsOf(internalDiscovery.candidates.map { "${it.fileId.value}#${it.ordinal}" })

        val internal = ProjectCallBinder(project.context).resolve(target.toInternal(), InternalCallKind.FUNCTION, arguments.internal)
        val public = project.snapshot.resolveCall(target, ProjectCallKind.FUNCTION, arguments)
        assertThat(public.status.name).isEqualTo(internal.status.name)
        assertThat(public.uncertainties.map { it.name }).containsExactlyInAnyOrderElementsOf(internal.uncertainties.map { it.name })
        val candidates = public.candidates.map { "${it.declaration.id.fileId}#${it.declaration.id.ordinal}:${it.declaration.role}" }
        assertThat(candidates).containsExactlyElementsOf(
            internal.candidates.map { "${it.declaration.fileId.value}#${it.declaration.ordinal}:${it.declaration.declaration.role}" }
        )
        val selected = public.selected.map { "${it.declaration.id.fileId}#${it.declaration.id.ordinal}" }.orElse(null)
        assertThat(selected).isEqualTo(internal.selected?.let { "${it.declaration.fileId.value}#${it.declaration.ordinal}" })
        return Outcome(publicDiscovery.status.name, public.status.name, public.uncertainties.map { it.name }.toSet(), candidates, selected)
    }

    @Test
    fun onlyStrictlyNamedHeadingsWithoutAFactAreRecorded() {
        val extraction = ProjectDeclarationExtractor().extractFile(
            FileId("a.sql"),
            """
            $omittedPtf
            /
            CREATE OR REPLACE FUNCTION "Quoted"(tab TABLE, c COLUMNS DEFAULT NULL) RETURN TABLE PIPELINED ROW POLYMORPHIC USING impl;
            /
            CREATE FUNCTION a.b.c(x NUMBER) RETURN NUMBER IS BEGIN RETURN x; END;
            /
            CREATE FUNCTION kept(x NUMBER) RETURN NUMBER IS BEGIN RETURN x; END;
            /
            CREATE PACKAGE p AS FUNCTION f(t TABLE) RETURN TABLE PIPELINED ROW POLYMORPHIC USING p; END p;
            """.trimIndent()
        )
        assertThat(extraction.omittedStandaloneCallables.map { it.toString() }).containsExactlyInAnyOrder("app.fn", "\"Quoted\"")
        assertThat(extraction.declarations.map { it.qualifiedName().toString() }).containsExactly("kept", "p")
    }

    @Test
    fun writtenNameWithRetainedMemberAndOmittedStandaloneIsNotSettled() {
        val result = outcome(project("spec.sql" to spec, "ptf.sql" to omittedPtf), written("app", "fn"))
        assertThat(result).isEqualTo(
            Outcome("AMBIGUOUS_TARGET", "AMBIGUOUS_TARGET", setOf("NAMESPACE_COMPETITION", "PARTIAL_DECLARATIONS"), listOf("spec.sql#1:SPECIFICATION"), null)
        )
    }

    @Test
    fun explicitPackageMemberStillExcludesTheStandaloneReading() {
        val result = outcome(project("spec.sql" to spec, "ptf.sql" to omittedPtf), member("app", "fn"))
        assertThat(result).isEqualTo(Outcome("FOUND", "RESOLVED", emptySet(), listOf("spec.sql#1:SPECIFICATION"), "spec.sql#1"))
    }

    @Test
    fun explicitStandaloneOfAnOmittedHeadingHasNoCandidateAndNoCertainty() {
        val withPackage = outcome(project("spec.sql" to spec, "ptf.sql" to omittedPtf), standalone("app", "fn"))
        assertThat(withPackage).isEqualTo(Outcome("PARTIAL_DECLARATIONS", "PARTIAL_DECLARATIONS", setOf("PARTIAL_DECLARATIONS"), emptyList(), null))
        // A retained duplicate of the same standalone name does not make the incomplete evidence certain.
        val withRetained = outcome(project("ptf.sql" to omittedPtf, "retained.sql" to retained), standalone("app", "fn"))
        assertThat(withRetained).isEqualTo(
            Outcome("PARTIAL_DECLARATIONS", "PARTIAL_DECLARATIONS", setOf("PARTIAL_DECLARATIONS"), listOf("retained.sql#0:STANDALONE"), null)
        )
    }

    @Test
    fun omittedStandaloneWithoutPackageIsNotDefinitiveAbsence() {
        val single = project("ptf.sql" to "CREATE FUNCTION fn(tab TABLE) RETURN TABLE PIPELINED ROW POLYMORPHIC USING impl;")
        assertThat(outcome(single, written("fn"))).isEqualTo(
            Outcome("PARTIAL_DECLARATIONS", "PARTIAL_DECLARATIONS", setOf("PARTIAL_DECLARATIONS"), emptyList(), null)
        )
        assertThat(outcome(project("ptf.sql" to omittedPtf), written("app", "fn")).resolution).isEqualTo("PARTIAL_DECLARATIONS")
        // The omitted object `fn` is evidence for the package reading of `fn.x`, exactly like a retained `fn`.
        val retainedFn = project("fn.sql" to "CREATE FUNCTION fn(a NUMBER) RETURN NUMBER IS BEGIN RETURN a; END;")
        assertThat(outcome(single, written("fn", "x"))).isEqualTo(outcome(retainedFn, written("fn", "x")))
        assertThat(outcome(single, written("fn", "x")).resolution).isEqualTo("UNKNOWN_PACKAGE")
    }

    @Test
    fun retainedStandaloneComparisonCasesAreUnchanged() {
        val competing = outcome(project("spec.sql" to spec, "retained.sql" to retained), written("app", "fn"))
        assertThat(competing).isEqualTo(
            Outcome("AMBIGUOUS_TARGET", "AMBIGUOUS_TARGET", setOf("NAMESPACE_COMPETITION"),
                listOf("spec.sql#1:SPECIFICATION", "retained.sql#0:STANDALONE"), null)
        )
        val alone = outcome(project("retained.sql" to retained), written("app", "fn"))
        assertThat(alone).isEqualTo(Outcome("FOUND", "RESOLVED", emptySet(), listOf("retained.sql#0:STANDALONE"), "retained.sql#0"))
    }

    @Test
    fun unrelatedOmittedNamesHaveNoEffect() {
        val other = "CREATE FUNCTION app.other(tab TABLE) RETURN TABLE PIPELINED ROW POLYMORPHIC USING impl;\n/\n" +
            "CREATE FUNCTION fn(tab TABLE) RETURN TABLE PIPELINED ROW POLYMORPHIC USING impl;\n/\n" +
            "CREATE FUNCTION x.app.fn(a NUMBER) RETURN NUMBER IS BEGIN RETURN a; END;"
        val project = project("spec.sql" to spec, "retained.sql" to "CREATE FUNCTION solo(a NUMBER) RETURN NUMBER IS BEGIN RETURN a; END;", "other.sql" to other)
        assertThat(outcome(project, written("app", "fn"))).isEqualTo(
            Outcome("FOUND", "RESOLVED", emptySet(), listOf("spec.sql#1:SPECIFICATION"), "spec.sql#1")
        )
        assertThat(outcome(project, written("solo")).resolution).isEqualTo("RESOLVED")
        assertThat(outcome(project, written("missing")).resolution).isEqualTo("NOT_FOUND_IN_PROJECT")
    }

    @Test
    fun failuresAndDuplicatesKeepTheOmittedEvidence() {
        val failed = outcome(project("spec.sql" to spec, "ptf.sql" to omittedPtf, failed = true), written("app", "fn"))
        assertThat(failed.resolution).isEqualTo("INCOMPLETE_INDEX")
        assertThat(failed.uncertainties).containsExactlyInAnyOrder("NAMESPACE_COMPETITION", "PARTIAL_DECLARATIONS")

        val duplicates = outcome(
            project("spec.sql" to spec, "spec2.sql" to spec, "ptf.sql" to omittedPtf, "ptf2.sql" to omittedPtf),
            written("app", "fn")
        )
        assertThat(duplicates).isEqualTo(
            Outcome("AMBIGUOUS_TARGET", "AMBIGUOUS_TARGET",
                setOf("NAMESPACE_COMPETITION", "DUPLICATE_SPECIFICATION", "PARTIAL_DECLARATIONS"),
                listOf("spec.sql#1:SPECIFICATION", "spec2.sql#1:SPECIFICATION"), null)
        )
    }

    @Test
    fun serialConcurrentAndReversedPreparationAgree() {
        val files = listOf(
            "spec.sql" to spec,
            "ptf.sql" to omittedPtf,
            "retained.sql" to retained,
            "fn.sql" to "CREATE FUNCTION fn(tab TABLE) RETURN TABLE PIPELINED ROW POLYMORPHIC USING impl;"
        )
        val targets = listOf(written("app", "fn"), member("app", "fn"), standalone("app", "fn"), written("fn"), written("fn", "x"))
        fun summary(project: Project) = targets.map { outcome(project, it) }
        val serial = summary(project(*files.toTypedArray()))
        assertThat(summary(project(*files.toTypedArray(), concurrent = true))).isEqualTo(serial)
        assertThat(summary(project(*files.reversed().toTypedArray(), concurrent = true))).isEqualTo(serial)
        assertThat(serial.map { it.resolution }).containsExactly(
            "AMBIGUOUS_TARGET", "RESOLVED", "PARTIAL_DECLARATIONS", "PARTIAL_DECLARATIONS", "UNKNOWN_PACKAGE"
        )
    }
}
