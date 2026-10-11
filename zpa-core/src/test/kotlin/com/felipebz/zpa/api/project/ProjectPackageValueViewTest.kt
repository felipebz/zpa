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
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.*

@OptIn(ZpaExperimentalApi::class)
class ProjectPackageValueViewTest {
    private fun prepare(vararg files: Pair<String, String>): ProjectSnapshot =
        ProjectSnapshots.prepare(files.map { (id, text) -> ProjectSourceInput.ofText(id, text) }, false)

    private fun n(vararg s: String) = ProjectQualifiedName.of(*s)

    private fun values(s: ProjectSnapshot) = s.declarations.filterIsInstance<ProjectPackageValueView>()

    private val spec = "CREATE PACKAGE p AS\n    g NUMBER;\n    c CONSTANT VARCHAR2(20) := 'hello';\nEND p;"

    @Test
    fun variablesAndConstantsAreTypedValueViews() {
        val s = prepare("p.sql" to spec)
        val g = s.findDeclarations(n("p", "g")).declarations.single() as ProjectPackageValueView
        val c = s.findDeclarations(n("p", "c")).declarations.single() as ProjectPackageValueView
        assertThat(g.kind).isEqualTo(ProjectDeclarationKind.PACKAGE_VARIABLE)
        assertThat(c.kind).isEqualTo(ProjectDeclarationKind.PACKAGE_CONSTANT)
        assertThat(g.isConstant).isFalse()
        assertThat(c.isConstant).isTrue()
        assertThat(listOf(g.role, c.role)).containsOnly(ProjectDeclarationRole.SPECIFICATION)
        assertThat(g.owner).isEqualTo(n("p"))
        assertThat(g.localName).isEqualTo(ProjectIdentifier.fromSource("g"))
        assertThat(c.localName.originalSpelling).isEqualTo("c")
        assertThat(g.fileId).isEqualTo("p.sql")
        assertThat(g.sourceRange.startLine).isEqualTo(2)
        assertThat(c.sourceRange.startLine).isEqualTo(3)
        assertThat(g.typeRef.name).isEqualTo(n("NUMBER"))
        assertThat(c.typeRef.name).isEqualTo(n("VARCHAR2"))
        assertThat(values(s).map { it.localName.originalSpelling }).containsExactly("g", "c")
    }

    @Test
    fun typeReferenceVariantsAreRetainedUnresolved() {
        val s = prepare(
            "p.sql" to "CREATE PACKAGE p AS\n a other.t;\n b emp.sal%TYPE;\n c emp%ROWTYPE;\n d REF obj_t;\n e unknown_t;\nEND p;"
        )
        val byName = values(s).associateBy { it.localName.originalSpelling }
        val a = byName.getValue("a").typeRef
        assertThat(a.kind).isEqualTo(ProjectTypeReference.Kind.NAMED)
        assertThat(a.name).isEqualTo(n("other", "t"))
        assertThat(a.anchor).isEmpty
        val b = byName.getValue("b").typeRef
        assertThat(b.kind).isEqualTo(ProjectTypeReference.Kind.ANCHORED)
        assertThat(b.anchor).isEqualTo(Optional.of(ProjectTypeReference.Anchor.TYPE))
        assertThat(b.name).isEqualTo(n("emp", "sal"))
        val c = byName.getValue("c").typeRef
        assertThat(c.anchor).isEqualTo(Optional.of(ProjectTypeReference.Anchor.ROWTYPE))
        assertThat(c.name).isEqualTo(n("emp"))
        assertThat(byName.getValue("e").typeRef.kind).isEqualTo(ProjectTypeReference.Kind.NAMED)
        assertThat(byName.getValue("e").typeRef.name).isEqualTo(n("unknown_t"))
        assertThat(byName.getValue("b").typeRef.sourceRange.startLine).isEqualTo(3)
        assertThat(byName.getValue("d").typeRef.kind).isEqualTo(ProjectTypeReference.Kind.REF)
        assertThat(byName.getValue("d").typeRef.name).isEqualTo(n("obj_t"))
    }

    @Test
    fun quotedAndUnquotedIdentifiersKeepTheirDistinction() {
        val s = prepare("p.sql" to "CREATE PACKAGE \"My.Pkg\" AS\n \"Mixed\" NUMBER;\n lower_v NUMBER;\n \"a.b\" CONSTANT NUMBER := 1;\nEND;")
        val v = values(s)
        assertThat(v.map { it.localName.originalSpelling }).containsExactly("\"Mixed\"", "lower_v", "\"a.b\"")
        assertThat(v.map { it.localName.isQuoted }).containsExactly(true, false, true)
        assertThat(v.map { it.owner.last.originalSpelling }).containsOnly("\"My.Pkg\"")
        assertThat(v[0].localName).isNotEqualTo(ProjectIdentifier.fromSource("mixed"))
        assertThat(v[1].localName).isEqualTo(ProjectIdentifier.fromSource("LOWER_V"))
    }

    @Test
    fun inventoryPathsAgreeAndDuplicatesStayDistinct() {
        val s = prepare("a.sql" to spec, "b.sql" to spec)
        val all = values(s)
        assertThat(all).hasSize(4)
        assertThat(all.map { it.id }.toSet()).hasSize(4)
        val byFile = s.declarationsFor("a.sql").declarations.filterIsInstance<ProjectPackageValueView>()
        val byName = s.findDeclarations(n("p", "g")).declarations
        assertThat(byName).hasSize(2)
        assertThat(byName.map { it.fileId }).containsExactly("a.sql", "b.sql")
        assertThat(byName.first().id).isEqualTo(byFile.first { it.localName.originalSpelling == "g" }.id)
        assertThat(byName.first().id).isEqualTo(all.first { it.fileId == "a.sql" && it.localName.originalSpelling == "g" }.id)
        assertThat(byName[0].id).isNotEqualTo(byName[1].id)
        assertThat(s.findDeclarations(n("p", "missing")).declarations).isEmpty()
    }

    @Test
    fun valuesAreIsolatedFromTypeAndSubprogramQueries() {
        val s = prepare("p.sql" to "CREATE PACKAGE p AS\n g NUMBER;\n PROCEDURE g;\n TYPE t IS RECORD (a NUMBER);\n v t;\nEND p;")
        val same = s.findDeclarations(n("p", "g")).declarations
        assertThat(same.map { it.kind }).containsExactly(ProjectDeclarationKind.PACKAGE_VARIABLE, ProjectDeclarationKind.PACKAGE_PROCEDURE)
        val value = same[0]
        assertThat(value).isInstanceOf(ProjectPackageValueView::class.java)
        assertThat(value).isNotInstanceOf(ProjectTypeDeclarationView::class.java)
        assertThat(value).isNotInstanceOf(ProjectShapedTypeView::class.java)
        assertThat(value).isNotInstanceOf(ProjectPackageTypeView::class.java)
        assertThat(value).isNotInstanceOf(ProjectSubtypeView::class.java)
        assertThat(value).isNotInstanceOf(ProjectSubprogramView::class.java)
        assertThat(value).isNotInstanceOf(ProjectPackageSubprogramView::class.java)
        assertThat(value).isNotInstanceOf(ProjectFunctionView::class.java)
        assertThat(s.findPackageSubprograms(n("p"), ProjectIdentifier.fromSource("g")).declarations.map { it.kind })
            .containsExactly(ProjectDeclarationKind.PACKAGE_PROCEDURE)
        assertThat(s.resolveNamedType(n("p", "v")).candidates).isEmpty()
        assertThatThrownBy { s.correlateSpecification(value.id) }.isInstanceOf(IllegalArgumentException::class.java)
        val other = prepare("p.sql" to "CREATE PACKAGE p AS g NUMBER; END p;")
        assertThatThrownBy { other.correlateSpecification(value.id) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun javaAccessorsUseTheApprovedNames() {
        val methods = ProjectPackageValueView::class.java.declaredMethods.map { it.name to it.returnType }
        assertThat(methods).containsExactlyInAnyOrder(
            "getOwner" to ProjectQualifiedName::class.java,
            "getLocalName" to ProjectIdentifier::class.java,
            "isConstant" to Boolean::class.javaPrimitiveType,
            "getTypeRef" to ProjectTypeReference::class.java
        )
        assertThat(ProjectPackageValueView::class.java.interfaces.toList()).containsExactly(ProjectDeclarationView::class.java)
    }

    @Test
    fun publicSignaturesExposeOnlyPublicApiTypes() {
        val types = ProjectPackageValueView::class.java.methods.flatMap { listOf(it.returnType) + it.parameterTypes }
        assertThat(types.filter { it.name.startsWith("com.felipebz") }.map { it.name })
            .allMatch { it.startsWith("com.felipebz.zpa.api.project.") }
    }
}
