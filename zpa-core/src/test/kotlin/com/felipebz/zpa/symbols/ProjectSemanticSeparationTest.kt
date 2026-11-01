//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:Suppress("DEPRECATION")

package com.felipebz.zpa.symbols

import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.project.*
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
