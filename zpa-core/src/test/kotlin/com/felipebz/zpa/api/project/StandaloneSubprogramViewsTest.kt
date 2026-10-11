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
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.NamedTypeRef
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparationResult
import com.felipebz.zpa.project.ProjectSymbolIndexBuilder
import com.felipebz.zpa.project.QualifiedName
import com.felipebz.zpa.project.SourceRange
import com.felipebz.zpa.project.StandaloneFunctionDeclaration
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/** Typed public views of extracted standalone procedures and functions. */
@OptIn(ZpaExperimentalApi::class)
class StandaloneSubprogramViewsTest {
    private fun prepare(vararg files: Pair<String, String>): ProjectSnapshot = ProjectSnapshots.prepare(
        files.map { (id, text) -> ProjectSourceInput.ofText(id, text) }, false
    )

    private fun n(vararg s: String) = ProjectQualifiedName.of(*s)

    private val source = """
        CREATE OR REPLACE PROCEDURE app.run(a NUMBER, b IN OUT NOCOPY CLOB, "C" VARCHAR2 DEFAULT 'x', d t.col%TYPE, e REF obj_t) IS BEGIN NULL; END;
        /
        CREATE FUNCTION calc(x NUMBER) RETURN NUMBER DETERMINISTIC IS BEGIN RETURN x; END;
        /
        CREATE FUNCTION "Sch"."Fn" RETURN t%ROWTYPE IS BEGIN RETURN NULL; END;
        /
        CREATE PROCEDURE plain IS BEGIN NULL; END;
    """.trimIndent()

    private fun ProjectSnapshot.view(vararg name: String) = findDeclarations(n(*name)).declarations.single()

    @Test
    fun procedureViewsExposeSubprogramFacetOnly() {
        val s = prepare("s.sql" to source)
        val run = s.view("app", "run")
        assertThat(run).isInstanceOf(ProjectSubprogramView::class.java)
            .isNotInstanceOf(ProjectFunctionView::class.java).isNotInstanceOf(ProjectPackageSubprogramView::class.java)
        run as ProjectSubprogramView
        assertThat(run.kind).isEqualTo(ProjectDeclarationKind.STANDALONE_PROCEDURE)
        assertThat(run.role).isEqualTo(ProjectDeclarationRole.STANDALONE)
        assertThat(run.qualifiedName).isEqualTo(n("app", "run"))
        assertThat(run.localName).isEqualTo(ProjectIdentifier.fromSource("RUN"))
        assertThat(run.fileId).isEqualTo("s.sql")
        assertThat(run.sourceRange.startLine).isEqualTo(1)
        val ps = run.parameters
        assertThat(ps.map { it.name.originalSpelling }).containsExactly("a", "b", "\"C\"", "d", "e")
        assertThat(ps.map { it.ordinal }).containsExactly(1, 2, 3, 4, 5)
        assertThat(ps.map { it.mode }).containsExactly(ProjectParameterMode.IN, ProjectParameterMode.IN_OUT,
            ProjectParameterMode.IN, ProjectParameterMode.IN, ProjectParameterMode.IN)
        assertThat(ps.map { it.isNocopy }).containsExactly(false, true, false, false, false)
        assertThat(ps.map { it.isDefaultPresent }).containsExactly(false, false, true, false, false)
        assertThat(ps[3].typeRef.anchor).contains(ProjectTypeReference.Anchor.TYPE)
        assertThat(ps[4].typeRef.kind).isEqualTo(ProjectTypeReference.Kind.REF)
        assertThat(ps[0].sourceRange.fileId).isEqualTo("s.sql")
        assertThat(ps[0].sourceRange.startLine).isEqualTo(1)
        val plain = s.view("plain") as ProjectSubprogramView
        assertThat(plain.parameters).isEmpty()
        assertThat(plain.localName).isEqualTo(plain.qualifiedName.last)
    }

    @Test
    fun functionViewsExposeFunctionFacetNotPackageMembership() {
        val s = prepare("s.sql" to source)
        val calc = s.view("calc")
        assertThat(calc).isInstanceOf(ProjectFunctionView::class.java).isInstanceOf(ProjectSubprogramView::class.java)
            .isNotInstanceOf(ProjectPackageSubprogramView::class.java)
        calc as ProjectFunctionView
        assertThat(calc.kind).isEqualTo(ProjectDeclarationKind.STANDALONE_FUNCTION)
        assertThat(calc.role).isEqualTo(ProjectDeclarationRole.STANDALONE)
        assertThat(calc.isDeterministic).isTrue()
        assertThat(calc.returnType.name).isEqualTo(n("NUMBER"))
        assertThat(calc.returnType.kind).isEqualTo(ProjectTypeReference.Kind.NAMED)
        assertThat(calc.localName).isEqualTo(ProjectIdentifier.fromSource("calc"))
        val quoted = s.view("\"Sch\"", "\"Fn\"")
        assertThat(quoted).isNotInstanceOf(ProjectPackageSubprogramView::class.java)
        quoted as ProjectFunctionView
        assertThat(quoted.isDeterministic).isFalse()
        assertThat(quoted.qualifiedName.segments).hasSize(2)
        assertThat(quoted.localName.originalSpelling).isEqualTo("\"Fn\"")
        assertThat(quoted.localName.isQuoted).isTrue()
        assertThat(quoted.returnType.anchor).contains(ProjectTypeReference.Anchor.ROWTYPE)
    }

    @Test
    fun parameterListsRejectMutation() {
        val run = prepare("s.sql" to source).view("app", "run") as ProjectSubprogramView
        @Suppress("UNCHECKED_CAST")
        val list = run.parameters as MutableList<ProjectParameterView>
        assertThatThrownBy { list.clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { list.removeAt(0) }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThat(run.parameters).hasSize(5)
    }

    @Test
    fun idsAreCanonicalAcrossInventoriesAndDuplicatesStayDistinct() {
        val dup = "CREATE FUNCTION calc RETURN NUMBER IS BEGIN RETURN 1; END;"
        val s = prepare("a.sql" to dup, "b.sql" to dup, "c.sql" to "$dup\n/\n$dup")
        val byName = s.findDeclarations(n("calc")).declarations
        assertThat(byName).hasSize(4).allMatch { it is ProjectFunctionView }
        assertThat(byName.map { it.id }).doesNotHaveDuplicates()
        assertThat(s.declarations.map { it.id }).containsAll(byName.map { it.id })
        val perFile = s.declarationsFor("c.sql").declarations
        assertThat(perFile.map { it.id }.map { it.ordinal }).containsExactly(0, 1)
        assertThat(perFile.map { it.id }).isEqualTo(byName.filter { it.fileId == "c.sql" }.map { it.id })
        // Independent queries, taken separately, agree on every occurrence ID (views are recreated per call).
        val first = s.declarations.map { it.id }
        val second = s.declarations.map { it.id }
        assertThat(second).isEqualTo(first)
        assertThat(s.declarationsFor("a.sql").declarations.map { it.id })
            .isEqualTo(s.declarations.filter { it.fileId == "a.sql" }.map { it.id })
        assertThat(byName.map { it.id }).isEqualTo(s.declarations.filter { it.qualifiedName == n("calc") }.map { it.id })
    }

    @Test
    fun snapshotsAreIsolatedAndStaleIdsAreRejected() {
        val a = prepare("s.sql" to source)
        val b = prepare("s.sql" to source)
        val idA = a.view("calc").id
        val idB = b.view("calc").id
        assertThat(idA).isNotEqualTo(idB)
        assertThat(idA.fileId).isEqualTo(idB.fileId)
        assertThat(idA.ordinal).isEqualTo(idB.ordinal)
        assertThatThrownBy { b.correlateSpecification(idA) }.isInstanceOf(IllegalArgumentException::class.java)
        // A standalone declaration is never a correlation input, even in its own snapshot.
        assertThatThrownBy { a.correlateSpecification(idA) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { a.correlateSpecification(a.view("app", "run").id) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun standaloneAndPackageMemberWithTheSameTextualNameKeepDistinctFacets() {
        val s = prepare(
            "a.sql" to "CREATE FUNCTION app.f RETURN NUMBER DETERMINISTIC IS BEGIN RETURN 1; END;",
            "b.sql" to "CREATE PACKAGE app AS FUNCTION f RETURN VARCHAR2; END app;",
            "c.sql" to "CREATE PACKAGE BODY app AS FUNCTION f RETURN VARCHAR2 IS BEGIN RETURN 'x'; END; END app;"
        )
        val both = s.findDeclarations(n("app", "f")).declarations
        assertThat(both.map { it.kind }).containsExactly(
            ProjectDeclarationKind.STANDALONE_FUNCTION, ProjectDeclarationKind.PACKAGE_FUNCTION, ProjectDeclarationKind.PACKAGE_FUNCTION)
        assertThat(both.filterIsInstance<ProjectPackageSubprogramView>()).hasSize(2)
        val standalone = both.first()
        assertThat(standalone).isNotInstanceOf(ProjectPackageSubprogramView::class.java)
        assertThat((standalone as ProjectFunctionView).localName).isEqualTo(ProjectIdentifier.fromSource("f"))
        // Package-only queries and correlation are unchanged.
        val members = s.findPackageSubprograms(n("app"), ProjectIdentifier.fromSource("f"))
        assertThat(members.declarations).hasSize(2).allMatch { it !is ProjectFunctionView || it.returnType.name == n("VARCHAR2") }
        assertThat(members.declarations.map { it.id }).doesNotContain(standalone.id)
        val bodyId = members.declarations.single { it.role == ProjectDeclarationRole.BODY }.id
        val correlation = s.correlateSpecification(bodyId)
        assertThat(correlation.status).isEqualTo(ProjectSpecificationCorrelation.Status.RESOLVED)
        assertThat(correlation.specification.get().id).isNotEqualTo(standalone.id)
    }

    @Test
    fun handBuiltFactsRepeatedReferencesRemainSeparateOccurrences() {
        val file = FileId("r.sql")
        val range = SourceRange(file, 1, 0, 1, 10)
        val fact = StandaloneFunctionDeclaration(QualifiedName(listOf(OracleIdentifier.fromSource("f"))), emptyList(),
            NamedTypeRef(QualifiedName(listOf(OracleIdentifier.fromSource("NUMBER"))), range), file, range, true)
        val builder = ProjectSymbolIndexBuilder()
        builder.add(file, listOf(fact, fact))
        val snapshot = ProjectSnapshot.create(
            ProjectIndexPreparationResult(builder.build(), 1, emptyList()),
            ProjectPreparationState.PREPARED_WITH_DECLARATIONS, emptyList(), listOf("r.sql"))
        val views = snapshot.findDeclarations(n("f")).declarations
        assertThat(views.map { it.id.ordinal }).containsExactly(0, 1)
        assertThat(views).allMatch { it is ProjectFunctionView && it.isDeterministic }
        assertThat(ProjectAnalysisContext.prepared(ProjectIndexPreparationResult(builder.build(), 1, emptyList())).state.kind)
            .isEqualTo(ProjectAnalysisContext.State.Kind.PREPARED_WITH_DECLARATIONS)
    }
}
