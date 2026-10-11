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
import com.felipebz.zpa.api.project.ProjectValueResolution.Status
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.*

@OptIn(ZpaExperimentalApi::class)
class ProjectValueResolutionTest {
    private val specA = """
        CREATE PACKAGE p AS
          g NUMBER;
          c CONSTANT VARCHAR2(30) := 'v';
          "Mixed" NUMBER;
          both NUMBER;
          PROCEDURE both;
          tsame NUMBER;
          TYPE tsame IS RECORD (a NUMBER);
          ssame NUMBER;
          SUBTYPE ssame IS NUMBER;
          PROCEDURE onlyproc;
          TYPE onlytype IS RECORD (a NUMBER);
          PROCEDURE multi;
          PROCEDURE multi(x NUMBER);
          SUBTYPE multi_t IS NUMBER;
        END p;
    """.trimIndent()
    private val specB = "CREATE PACKAGE p AS\n  g DATE;\nEND p;"
    private val other = "CREATE PACKAGE app.q AS qv NUMBER; END q;"

    private fun n(vararg s: String) = ProjectQualifiedName.of(*s)
    private val p = n("p")

    private fun prepare(concurrent: Boolean = false, files: List<Pair<String, String>> = listOf("a.sql" to specA, "b.sql" to specB, "c.sql" to other), vararg extra: ProjectSourceInput) =
        ProjectSnapshots.prepare(files.map { (id, text) -> ProjectSourceInput.ofText(id, text) } + extra, concurrent)

    private val snapshot = prepare()

    /** Candidates must be exactly the existing inventory of the effective name, with the same IDs and order. */
    private fun assertCandidates(result: ProjectValueResolution, effective: ProjectQualifiedName, s: ProjectSnapshot = snapshot) {
        val expected = s.findDeclarations(effective).declarations
        assertThat(result.candidates.map { it.id }).isEqualTo(expected.map { it.id })
        assertThat(result.candidates.map { it.kind to it.role }).isEqualTo(expected.map { it.kind to it.role })
        assertThat(result.candidates.map { it.sourceRange }).isEqualTo(expected.map { it.sourceRange })
    }

    @Test
    fun aVariableAndAConstantResolveWithTheirDeclaredTypes() {
        val g = snapshot.resolvePackageValue(n("c"), n("p")) // constant, only in a.sql
        assertThat(g.status).isEqualTo(Status.RESOLVED)
        val c = g.value.get()
        assertThat(c.isConstant).isTrue()
        assertThat(c.kind).isEqualTo(ProjectDeclarationKind.PACKAGE_CONSTANT)
        assertThat(c.owner).isEqualTo(p)
        assertThat(c.typeRef.name).isEqualTo(n("VARCHAR2"))
        assertThat(c.fileId).isEqualTo("a.sql")
        assertThat(g.candidates).hasSize(1)
        assertThat(g.candidates.single().id).isEqualTo(c.id)
        assertCandidates(g, n("p", "c"))
        assertThat(g.failures).isEmpty()
        assertThat(g.name).isEqualTo(n("c"))
        assertThat(g.packageOwner).isEqualTo(Optional.of(p))

        val q = snapshot.resolvePackageValue(n("qv"), n("app", "q"))
        assertThat(q.status).isEqualTo(Status.RESOLVED)
        assertThat(q.value.get().kind).isEqualTo(ProjectDeclarationKind.PACKAGE_VARIABLE)
        assertThat(q.value.get().isConstant).isFalse()
        assertThat(q.value.get().owner).isEqualTo(n("app", "q"))
    }

    @Test
    fun oneSegmentNamesNeedAnExplicitOwnerAndQualifiedNamesUseTheirOwnPrefix() {
        val noOwner = snapshot.resolvePackageValue(n("c"), null)
        assertThat(noOwner.status).isEqualTo(Status.NOT_FOUND_IN_PROJECT)
        assertThat(noOwner.candidates).isEmpty()
        assertThat(noOwner.packageOwner).isEmpty
        val qualified = snapshot.resolvePackageValue(n("p", "c"), null)
        assertThat(qualified.status).isEqualTo(Status.RESOLVED)
        assertThat(qualified.packageOwner).isEmpty
        // A qualified name overrides a different explicit owner, which is still reported.
        val overriding = snapshot.resolvePackageValue(n("p", "c"), n("app", "q"))
        assertThat(overriding.status).isEqualTo(Status.RESOLVED)
        assertThat(overriding.value.get().id).isEqualTo(qualified.value.get().id)
        assertThat(overriding.packageOwner).isEqualTo(Optional.of(n("app", "q")))
        // The owner applies to one-segment names only, with no fallback to other packages.
        assertThat(snapshot.resolvePackageValue(n("c"), n("app", "q")).status).isEqualTo(Status.NOT_FOUND_IN_PROJECT)
        assertThat(snapshot.resolvePackageValue(n("qv"), n("p")).status).isEqualTo(Status.NOT_FOUND_IN_PROJECT)
        assertThat(snapshot.resolvePackageValue(n("app", "q", "qv"), null).status).isEqualTo(Status.RESOLVED)
    }

    @Test
    fun quotedAndUnquotedNamesKeepTheirDistinction() {
        assertThat(snapshot.resolvePackageValue(n("\"Mixed\""), p).status).isEqualTo(Status.RESOLVED)
        assertThat(snapshot.resolvePackageValue(n("mixed"), p).status).isEqualTo(Status.NOT_FOUND_IN_PROJECT)
        assertThat(snapshot.resolvePackageValue(n("G"), p).status).isEqualTo(Status.AMBIGUOUS)
        assertThat(snapshot.resolvePackageValue(n("\"g\""), p).status).isEqualTo(Status.NOT_FOUND_IN_PROJECT)
    }

    @Test
    fun duplicateValuesAndKindCollisionsAreAmbiguousWithAllCandidatesInIndexOrder() {
        val duplicate = snapshot.resolvePackageValue(n("g"), p)
        assertThat(duplicate.status).isEqualTo(Status.AMBIGUOUS)
        assertThat(duplicate.value).isEmpty
        assertThat(duplicate.candidates.map { it.fileId }).containsExactly("a.sql", "b.sql")
        assertThat(duplicate.candidates.map { (it as ProjectPackageValueView).typeRef.name }).containsExactly(n("NUMBER"), n("DATE"))
        assertCandidates(duplicate, n("p", "g"))

        val procedure = snapshot.resolvePackageValue(n("both"), p)
        assertThat(procedure.status).isEqualTo(Status.AMBIGUOUS)
        assertThat(procedure.candidates.map { it.kind }).containsExactly(ProjectDeclarationKind.PACKAGE_VARIABLE, ProjectDeclarationKind.PACKAGE_PROCEDURE)
        assertCandidates(procedure, n("p", "both"))
        val type = snapshot.resolvePackageValue(n("tsame"), p)
        assertThat(type.status).isEqualTo(Status.AMBIGUOUS)
        assertThat(type.candidates.map { it.kind }).containsExactly(ProjectDeclarationKind.PACKAGE_VARIABLE, ProjectDeclarationKind.PACKAGE_TYPE)
        val subtype = snapshot.resolvePackageValue(n("ssame"), p)
        assertThat(subtype.status).isEqualTo(Status.AMBIGUOUS)
        assertThat(subtype.candidates.map { it.kind }).containsExactly(ProjectDeclarationKind.PACKAGE_VARIABLE, ProjectDeclarationKind.PACKAGE_SUBTYPE)
        assertCandidates(subtype, n("p", "ssame"))
    }

    @Test
    fun nonValueDeclarationsAloneAreNotATargetNorAmbiguous() {
        listOf("onlyproc" to listOf(ProjectDeclarationKind.PACKAGE_PROCEDURE), "onlytype" to listOf(ProjectDeclarationKind.PACKAGE_TYPE),
            "multi" to listOf(ProjectDeclarationKind.PACKAGE_PROCEDURE, ProjectDeclarationKind.PACKAGE_PROCEDURE),
            "multi_t" to listOf(ProjectDeclarationKind.PACKAGE_SUBTYPE)).forEach { (name, kinds) ->
            val result = snapshot.resolvePackageValue(n(name), p)
            assertThat(result.status).describedAs(name).isEqualTo(Status.NOT_FOUND_IN_PROJECT)
            assertThat(result.value).isEmpty
            assertThat(result.candidates.map { it.kind }).describedAs(name).isEqualTo(kinds)
            assertCandidates(result, n("p", name))
        }
        val absent = snapshot.resolvePackageValue(n("nothing"), p)
        assertThat(absent.status).isEqualTo(Status.NOT_FOUND_IN_PROJECT)
        assertThat(absent.candidates).isEmpty()
    }

    @Test
    fun preparationFailuresTakePrecedenceInEveryContext() {
        val failed = prepare(false, listOf("a.sql" to specA, "c.sql" to other), ProjectSourceInput.ofReader("bad.sql") { throw java.io.IOException("x") })
        assertThat(failed.preparationState).isEqualTo(ProjectPreparationState.PREPARED_WITH_FAILURES)
        val unique = failed.resolvePackageValue(n("c"), p)
        assertThat(unique.status).isEqualTo(Status.INCOMPLETE_INDEX)
        assertThat(unique.value).isEmpty
        assertThat(unique.candidates.map { it.kind }).containsExactly(ProjectDeclarationKind.PACKAGE_CONSTANT)
        assertThat(unique.failures.map { it.fileId }).containsExactly("bad.sql")
        // No candidate, no owner, a qualified name and non-value-only candidates are all incomplete too.
        assertThat(failed.resolvePackageValue(n("nothing"), p).status).isEqualTo(Status.INCOMPLETE_INDEX)
        assertThat(failed.resolvePackageValue(n("nothing"), null).status).isEqualTo(Status.INCOMPLETE_INDEX)
        assertThat(failed.resolvePackageValue(n("c"), null).status).isEqualTo(Status.INCOMPLETE_INDEX)
        assertThat(failed.resolvePackageValue(n("p", "nothing"), null).candidates).isEmpty()
        assertThat(failed.resolvePackageValue(n("p", "onlyproc"), null).status).isEqualTo(Status.INCOMPLETE_INDEX)
        assertThat(failed.resolvePackageValue(n("p", "both"), null).status).isEqualTo(Status.INCOMPLETE_INDEX)
        assertThat(failed.resolvePackageValue(n("p", "nothing"), null).failures).hasSize(1)
    }

    @Test
    fun anUnpreparedSnapshotIsReportedBeforeAnythingElse() {
        val result = unpreparedSnapshot().resolvePackageValue(n("g"), p)
        assertThat(result.status).isEqualTo(Status.NOT_PREPARED)
        assertThat(result.candidates).isEmpty()
        assertThat(result.failures).isEmpty()
        assertThat(result.value).isEmpty
        assertThat(unpreparedSnapshot().resolvePackageValue(n("p", "g"), null).status).isEqualTo(Status.NOT_PREPARED)
    }

    @Test
    fun resultsAreIdenticalForSerialConcurrentAndReversedPreparation() {
        fun summary(s: ProjectSnapshot) = listOf(n("g") to p, n("c") to p, n("both") to p, n("onlyproc") to p, n("nothing") to p, n("p", "g") to null, n("c") to null).map { (name, owner) ->
            val r = s.resolvePackageValue(name, owner)
            listOf(r.status, r.value.map { it.id.fileId to it.id.ordinal }, r.candidates.map { Triple(it.id.fileId, it.id.ordinal, it.kind) })
        }
        val files = listOf("a.sql" to specA, "b.sql" to specB, "c.sql" to other)
        val serial = summary(prepare(false, files))
        assertThat(summary(prepare(true, files))).isEqualTo(serial)
        assertThat(summary(prepare(false, files.reversed()))).isEqualTo(serial)
    }

    @Test
    fun theJavaAbiAndImmutability() {
        val method = ProjectSnapshot::class.java.getMethod("resolvePackageValue", ProjectQualifiedName::class.java, ProjectQualifiedName::class.java)
        assertThat(method.returnType).isEqualTo(ProjectValueResolution::class.java)
        val getters = ProjectValueResolution::class.java.declaredMethods.filter { Modifier.isPublic(it.modifiers) && !it.isSynthetic }
        assertThat(getters.map { it.name }).containsExactlyInAnyOrder("getStatus", "getName", "getPackageOwner", "getValue", "getCandidates", "getFailures")
        val types = getters.associate { it.name to it.genericReturnType.typeName }
        assertThat(types["getValue"]).isEqualTo("java.util.Optional<com.felipebz.zpa.api.project.ProjectPackageValueView>")
        assertThat(types["getPackageOwner"]).isEqualTo("java.util.Optional<com.felipebz.zpa.api.project.ProjectQualifiedName>")
        assertThat(types["getCandidates"]).isEqualTo("java.util.List<com.felipebz.zpa.api.project.ProjectDeclarationView>")
        assertThat(types["getFailures"]).isEqualTo("java.util.List<com.felipebz.zpa.api.project.ProjectPreparationFailure>")
        assertThat((ProjectValueResolution::class.java.getMethod("getCandidates").genericReturnType as ParameterizedType).actualTypeArguments.single()).isEqualTo(ProjectDeclarationView::class.java)
        assertThat(ProjectValueResolution::class.java.constructors.filter { !it.isSynthetic && it.parameterTypes.none { t -> t.name.endsWith("DefaultConstructorMarker") } }).isEmpty()
        assertThat(ProjectValueResolution::class.java.methods.filter { it.name.startsWith("set") || Modifier.isStatic(it.modifiers) && it.declaringClass == ProjectValueResolution::class.java }).isEmpty()
        assertThat(Status.entries.map { it.ordinal to it.name }).containsExactly(0 to "RESOLVED", 1 to "AMBIGUOUS", 2 to "NOT_FOUND_IN_PROJECT", 3 to "INCOMPLETE_INDEX", 4 to "NOT_PREPARED")
        assertThat(types.values.filter { it.contains("com.felipebz") }).allMatch { it.contains("com.felipebz.zpa.api.project.") }

        val result = snapshot.resolvePackageValue(n("g"), p)
        assertThatThrownBy { (result.candidates as MutableList<ProjectDeclarationView>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        val incomplete = prepare(false, listOf("a.sql" to specA), ProjectSourceInput.ofReader("bad.sql") { throw java.io.IOException("x") }).resolvePackageValue(n("c"), p)
        assertThat(incomplete.failures).hasSize(1)
        assertThatThrownBy { (incomplete.failures as MutableList<ProjectPreparationFailure>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { (incomplete.failures as MutableList<ProjectPreparationFailure>).add(incomplete.failures[0]) }.isInstanceOf(UnsupportedOperationException::class.java)
        // IDs agree with the existing inventory even though views are created per call.
        assertThat(snapshot.resolvePackageValue(n("g"), p).candidates.map { it.id }).isEqualTo(result.candidates.map { it.id })
    }

    private fun unpreparedSnapshot(): ProjectSnapshot {
        val probe = object : PlSqlCheck() {
            var captured: ProjectSnapshot? = null
            init { subscribeTo(PlSqlGrammar.PROCEDURE_DECLARATION) }
            override fun visitNode(node: AstNode) { captured = projectAnalysis().snapshot }
        }
        val file = object : PlSqlFile {
            override fun contents() = "CREATE PACKAGE BODY b AS PROCEDURE w IS BEGIN NULL; END w; END b;"
            override fun fileName() = "b.sql"
            override fun path(): Path = Path.of("b.sql")
            override fun type() = PlSqlFile.Type.MAIN
        }
        AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, ProjectAnalysisContext.NOT_PREPARED).scanFile(file, listOf(probe), FileId("b.sql"))
        return probe.captured!!
    }
}
