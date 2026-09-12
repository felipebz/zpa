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
package com.felipebz.zpa.symbols

import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.project.DeclarationRole
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.project.QualifiedName
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class ProjectSemanticSeparationTest {

    @Test
    fun lexicalSymbolsRemainFileLocalWhileProjectFactsAreIndexed() {
        val source = """
            CREATE PACKAGE p AS
              PROCEDURE work(value NUMBER);
              FUNCTION value RETURN NUMBER;
            END p;
            CREATE PACKAGE BODY p AS
              PROCEDURE work(value NUMBER) IS
              BEGIN
                NULL;
              END work;
              FUNCTION value RETURN NUMBER IS
              BEGIN
                RETURN 1;
              END value;
            END p;
        """.trimIndent()
        val fileId = FileId("p.sql")
        val file = TestPlSqlFile(source, fileId)

        val scanner = AstScanner(emptyList(), null, true)
        val result = scanner.scanFile(file, fileId = fileId)

        assertThat(result.symbols.filter {
            it.kind == Symbol.Kind.PROCEDURE || it.kind == Symbol.Kind.FUNCTION
        }.map { it.name })
            .containsExactlyInAnyOrder("WORK", "VALUE", "WORK", "VALUE")
        assertThat(result.symbols.filter {
            it.kind == Symbol.Kind.PROCEDURE || it.kind == Symbol.Kind.FUNCTION
        }).allSatisfy { assertThat(it.isGlobal).isTrue }

        val index = ProjectIndexPreparation().prepare(
            listOf(ProjectSource(fileId) { source }),
            concurrent = false
        ).index
        val projectSubprograms = index.findSubprograms(
            QualifiedName(OracleIdentifier.fromSource("p")),
            OracleIdentifier.fromSource("work")
        )
        assertThat(projectSubprograms).hasSize(2)
        assertThat(projectSubprograms.map { it.role })
            .containsExactlyInAnyOrder(DeclarationRole.SPECIFICATION, DeclarationRole.BODY)
        assertThat(projectSubprograms).allSatisfy { declaration ->
            assertThat(declaration.fileId).isEqualTo(fileId)
        }
    }

    private class TestPlSqlFile(
        private val source: String,
        private val fileId: FileId
    ) : PlSqlFile {
        override fun contents() = source
        override fun fileName() = fileId.value
        override fun path() = Paths.get(fileId.value)
        override fun type() = PlSqlFile.Type.MAIN
    }
}
