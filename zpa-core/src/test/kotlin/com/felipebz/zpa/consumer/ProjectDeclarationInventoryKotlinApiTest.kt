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
package com.felipebz.zpa.consumer

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectDeclarationKind
import com.felipebz.zpa.api.project.ProjectInventory
import com.felipebz.zpa.api.project.ProjectPackageTypeView
import com.felipebz.zpa.api.project.ProjectFunctionView
import com.felipebz.zpa.api.project.ProjectPackageSubprogramView
import com.felipebz.zpa.api.project.ProjectParameterMode
import com.felipebz.zpa.api.project.ProjectNamedTypeResolution
import com.felipebz.zpa.api.project.ProjectQualifiedName
import com.felipebz.zpa.api.project.ProjectSubtypeView
import com.felipebz.zpa.api.project.ProjectTypeReference
import com.felipebz.zpa.api.project.ProjectTypeShape
import com.felipebz.zpa.api.project.ProjectSnapshots
import com.felipebz.zpa.api.project.ProjectSourceInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@OptIn(ZpaExperimentalApi::class)
class ProjectDeclarationInventoryKotlinApiTest {
    @Test
    fun enumeratesGenericDeclarationsUsingOnlySupportedImports() {
        val snapshot = ProjectSnapshots.prepare(
            listOf(
                ProjectSourceInput.ofText("spec.sql", "CREATE PACKAGE shop AS FUNCTION total RETURN NUMBER; END shop;"),
                ProjectSourceInput.ofText("seq.sql", "CREATE SEQUENCE app.orders;")
            )
        )

        val byId = snapshot.declarations.associateBy { it.id }
        assertThat(byId).hasSize(3)
        val inventory = snapshot.findDeclarations(ProjectQualifiedName.of("SHOP", "TOTAL"))
        assertThat(inventory.status).isEqualTo(ProjectInventory.Status.COMPLETE)
        val function = inventory.declarations.single()
        assertThat(function.kind).isEqualTo(ProjectDeclarationKind.PACKAGE_FUNCTION)
        assertThat(byId[function.id]!!.qualifiedName).isEqualTo(function.qualifiedName)
        assertThat(snapshot.declarationsFor("seq.sql").declarations.single().kind)
            .isEqualTo(ProjectDeclarationKind.SEQUENCE)
    }

    @Test
    fun inspectsSpecializedTypeViewsWithOrdinaryTypeChecks() {
        val snapshot = ProjectSnapshots.prepare(
            listOf(ProjectSourceInput.ofText("t.sql",
                "CREATE PACKAGE t AS TYPE r IS RECORD (a NUMBER, b x.y%ROWTYPE); END t;"))
        )
        val type = snapshot.declarations.filterIsInstance<ProjectPackageTypeView>().single()
        assertThat(type.shape.orElseThrow()).isEqualTo(ProjectTypeShape.RECORD)
        assertThat(type.recordFields.map { it.name.originalSpelling }).containsExactly("a", "b")
        val ref = type.recordFields[1].typeRef
        assertThat(ref.kind).isEqualTo(ProjectTypeReference.Kind.ANCHORED)
        assertThat(ref.anchor.orElseThrow()).isEqualTo(ProjectTypeReference.Anchor.ROWTYPE)
    }

    @Test
    fun inspectsCallableSignaturesThroughGenericInventory() {
        val snapshot = ProjectSnapshots.prepare(
            listOf(ProjectSourceInput.ofText("c.sql",
                "CREATE PACKAGE c AS PROCEDURE pr(a IN NUMBER, b OUT NOCOPY VARCHAR2 DEFAULT 'x'); " +
                    "FUNCTION fn RETURN NUMBER DETERMINISTIC; END c;"))
        )
        val callables = snapshot.declarations.filterIsInstance<ProjectPackageSubprogramView>()
        assertThat(callables.map { it.packageOwner.toString() }).containsOnly("c")
        val proc = callables.single { it !is ProjectFunctionView }
        assertThat(proc.parameters.map { it.mode }).containsExactly(ProjectParameterMode.IN, ProjectParameterMode.OUT)
        assertThat(proc.parameters[1].isNocopy).isTrue()
        assertThat(proc.parameters[1].isDefaultPresent).isTrue()
        val fn = callables.filterIsInstance<ProjectFunctionView>().single()
        assertThat(fn.isDeterministic).isTrue()
        assertThat(fn.returnType.name.last.originalSpelling).isEqualTo("NUMBER")
    }

    @Test
    fun resolvesNamedTypesAndComparesIdsWithInventory() {
        val snapshot = ProjectSnapshots.prepare(
            listOf(ProjectSourceInput.ofText("t.sql", "CREATE PACKAGE t AS SUBTYPE s IS NUMBER; TYPE r IS RECORD (a NUMBER); END t;"))
        )
        val sub = snapshot.resolveNamedType(ProjectQualifiedName.of("s"), ProjectQualifiedName.of("t"))
        assertThat(sub.status).isEqualTo(ProjectNamedTypeResolution.Status.RESOLVED)
        assertThat(sub.declaration.get()).isInstanceOf(ProjectSubtypeView::class.java)
        val viaInventory = snapshot.findDeclarations(ProjectQualifiedName.of("t", "s")).declarations.single()
        assertThat(sub.declaration.get().id).isEqualTo(viaInventory.id)
        val missing = snapshot.resolveNamedType(ProjectQualifiedName.of("s"), null)
        assertThat(missing.status).isEqualTo(ProjectNamedTypeResolution.Status.NOT_FOUND_IN_PROJECT)
        assertThat(missing.candidates).isEmpty()
    }
}
