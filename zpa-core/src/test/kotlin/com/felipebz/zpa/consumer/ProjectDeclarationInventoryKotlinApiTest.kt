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
import com.felipebz.zpa.api.project.ProjectQualifiedName
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
}
