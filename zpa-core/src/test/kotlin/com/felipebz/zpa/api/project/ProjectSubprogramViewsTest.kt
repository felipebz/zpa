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
import com.felipebz.zpa.project.DeclarationRole
import com.felipebz.zpa.project.PackageSubprogramDeclaration
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparationResult
import com.felipebz.zpa.project.ProjectSubprogramSpecificationResolver
import com.felipebz.zpa.project.ProjectSubprogramSpecificationResolution as Resolution
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

@OptIn(ZpaExperimentalApi::class)
class ProjectSubprogramViewsTest {
    private fun prepare(vararg files: Pair<String, String>, extra: List<ProjectSourceInput> = emptyList()) =
        ProjectSnapshots.prepare(files.map { (id, text) -> ProjectSourceInput.ofText(id, text) } + extra, false)

    private val spec = "CREATE PACKAGE p AS\n" +
        "  PROCEDURE none;\n" +
        "  PROCEDURE run(a IN NUMBER, b OUT VARCHAR2, \"C\" IN OUT NOCOPY CLOB, d NUMBER DEFAULT 1, " +
        "e t.col%TYPE, f t%ROWTYPE, g REF obj_t);\n" +
        "  FUNCTION f1(x NUMBER) RETURN VARCHAR2 DETERMINISTIC;\n" +
        "  FUNCTION f2 RETURN q.rec_t;\n" +
        "  PROCEDURE ov(a NUMBER);\n" +
        "  PROCEDURE ov(a VARCHAR2);\n" +
        "END p;"
    private val body = "CREATE PACKAGE BODY p AS\n" +
        "  FUNCTION f1(x NUMBER) RETURN VARCHAR2 IS BEGIN RETURN NULL; END;\n" +
        "END p;"

    private fun ProjectSnapshot.sub(name: String) =
        findDeclarations(ProjectQualifiedName.of("p", name)).declarations

    @Test
    fun procedureSignaturesExposeOrderedParametersAndMetadata() {
        val snapshot = prepare("p.sql" to spec)
        val none = snapshot.sub("none").single() as ProjectPackageSubprogramView
        assertThat(none).isNotInstanceOf(ProjectFunctionView::class.java)
        assertThat(none.parameters).isEmpty()
        assertThat(none.packageOwner).isEqualTo(ProjectQualifiedName.of("p"))
        assertThat(none.localName).isEqualTo(ProjectIdentifier.fromSource("none"))
        assertThat(none.kind).isEqualTo(ProjectDeclarationKind.PACKAGE_PROCEDURE)
        assertThat(none.role).isEqualTo(ProjectDeclarationRole.SPECIFICATION)
        assertThat(none.qualifiedName).isEqualTo(ProjectQualifiedName.of("p", "none"))

        val run = snapshot.sub("run").single() as ProjectSubprogramView
        val ps = run.parameters
        assertThat(ps.map { it.ordinal }).containsExactly(1, 2, 3, 4, 5, 6, 7)
        assertThat(ps.map { it.name.originalSpelling }).containsExactly("a", "b", "\"C\"", "d", "e", "f", "g")
        assertThat(ps[2].name.isQuoted).isTrue()
        assertThat(ps.map { it.mode }).containsExactly(
            ProjectParameterMode.IN, ProjectParameterMode.OUT, ProjectParameterMode.IN_OUT,
            ProjectParameterMode.IN, ProjectParameterMode.IN, ProjectParameterMode.IN, ProjectParameterMode.IN
        )
        assertThat(ps.map { it.isNocopy }).containsExactly(false, false, true, false, false, false, false)
        assertThat(ps.map { it.isDefaultPresent }).containsExactly(false, false, false, true, false, false, false)
        assertThat(ps[0].typeRef.kind).isEqualTo(ProjectTypeReference.Kind.NAMED)
        assertThat(ps[4].typeRef.anchor).contains(ProjectTypeReference.Anchor.TYPE)
        assertThat(ps[5].typeRef.anchor).contains(ProjectTypeReference.Anchor.ROWTYPE)
        assertThat(ps[6].typeRef.kind).isEqualTo(ProjectTypeReference.Kind.REF)
        assertThat(ps[0].sourceRange.fileId).isEqualTo("p.sql")
        assertThat(ps[1].sourceRange.startColumn).isGreaterThan(ps[0].sourceRange.startColumn)
    }

    @Test
    fun functionsImplementBothFacetsAndExposeReturnTypeAndDeterminism() {
        val snapshot = prepare("p.sql" to spec)
        val f1 = snapshot.sub("f1").single()
        assertThat(f1).isInstanceOf(ProjectFunctionView::class.java)
        assertThat(f1).isInstanceOf(ProjectPackageSubprogramView::class.java)
        f1 as ProjectFunctionView
        assertThat(f1.returnType.name).isEqualTo(ProjectQualifiedName.of("VARCHAR2"))
        assertThat(f1.isDeterministic).isTrue()
        assertThat(f1.parameters.single().ordinal).isEqualTo(1)
        val f2 = snapshot.sub("f2").single() as ProjectFunctionView
        assertThat(f2.isDeterministic).isFalse()
        assertThat(f2.returnType.name).isEqualTo(ProjectQualifiedName.of("q", "rec_t"))
        assertThat(f2.parameters).isEmpty()
    }

    @Test
    fun rolesOverloadsAndIdsAcrossInventories() {
        val snapshot = prepare("p.sql" to spec, "b.sql" to body, extra = listOf(
            ProjectSourceInput.ofReader("z.sql", ProjectSourceReader { throw IllegalStateException("x") })
        ))
        val f1s = snapshot.sub("f1")
        assertThat(f1s.map { it.role }).containsExactly(ProjectDeclarationRole.BODY, ProjectDeclarationRole.SPECIFICATION)
        assertThat(snapshot.findDeclarations(ProjectQualifiedName.of("p", "f1")).status)
            .isEqualTo(ProjectInventory.Status.INCOMPLETE_INDEX)
        val ovs = snapshot.sub("ov")
        assertThat(ovs).hasSize(2)
        assertThat(ovs.map { it.id }.distinct()).hasSize(2)
        assertThat(ovs.map { (it as ProjectSubprogramView).parameters.single().typeRef.name.last.originalSpelling })
            .containsExactly("NUMBER", "VARCHAR2")

        val byId = snapshot.declarations.associateBy { it.id }
        (f1s + ovs).forEach {
            assertThat(byId[it.id]).isInstanceOf(ProjectPackageSubprogramView::class.java)
            assertThat(byId[it.id]!!.sourceRange).isEqualTo(it.sourceRange)
        }
        val perFile = snapshot.declarationsFor("b.sql").declarations.single()
        assertThat(perFile.id).isEqualTo(f1s[0].id)
        assertThat(perFile).isInstanceOf(ProjectFunctionView::class.java)
    }

    @Test
    fun parameterListsRejectMutation() {
        val run = prepare("p.sql" to spec).sub("run").single() as ProjectSubprogramView
        @Suppress("UNCHECKED_CAST")
        val ps = run.parameters as MutableList<ProjectParameterView>
        assertThatThrownBy { ps.clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { ps.removeAt(0) }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThat(run.parameters).hasSize(7)
    }

    @Test
    fun inspectingPublicSignaturesDoesNotChangePrivateHeadingCorrelation() {
        val src = "CREATE PACKAGE p AS FUNCTION f(x IN NUMBER(10,2), y VARCHAR2) RETURN NUMBER(5); " +
            "FUNCTION g(x NUMBER(10,2)) RETURN NUMBER; END p;" 
        val bodySrc = "CREATE PACKAGE BODY p AS FUNCTION f(x IN NUMBER(10,2), y VARCHAR2) RETURN NUMBER(5) IS BEGIN RETURN 1; END; " +
            "FUNCTION g(x NUMBER(9,2)) RETURN NUMBER IS BEGIN RETURN 1; END; END p;"
        val snapshot = prepare("s.sql" to src, "b.sql" to bodySrc)
        val result = ProjectSnapshot::class.java.getDeclaredField("preparationResult")
            .apply { isAccessible = true }.get(snapshot) as ProjectIndexPreparationResult
        val bodies = result.index.declarations.filterIsInstance<PackageSubprogramDeclaration>()
            .filter { it.role == DeclarationRole.BODY }
        val resolver = ProjectSubprogramSpecificationResolver(ProjectAnalysisContext.prepared(result))
        val before = bodies.map { it.headerIdentity() to resolver.resolve(it)::class }

        snapshot.declarations.filterIsInstance<ProjectSubprogramView>().forEach { v ->
            v.parameters.forEach { it.typeRef; it.name; it.mode; it.sourceRange }
            (v as? ProjectFunctionView)?.returnType
        }

        val after = bodies.map { it.headerIdentity() to resolver.resolve(it)::class }
        assertThat(after).isEqualTo(before)
        assertThat(bodies.map { resolver.resolve(it)::class })
            .containsExactly(Resolution.Resolved::class, Resolution.NotFound::class)
    }
}
