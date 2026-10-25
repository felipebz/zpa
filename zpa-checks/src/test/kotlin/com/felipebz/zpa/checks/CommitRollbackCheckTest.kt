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
package com.felipebz.zpa.checks

import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.squid.ConditionalCompilationSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class CommitRollbackCheckTest : BaseCheckTest() {

    @Test
    fun test() {
        PlSqlCheckVerifier.verify(getPath("commit_rollback.sql"), CommitRollbackCheck())
    }

    @Test
    fun aPragmaTheSettingsRemoveIsNotCompiled() {
        val source = """
            create procedure flagged is
              ${'$'}if ${'$'}${'$'}flag ${'$'}then pragma autonomous_transaction; ${'$'}end
            begin
              commit;
            end;
            /
            """.trimIndent()
        val scanner = AstScanner(
            listOf(CommitRollbackCheck()), null, false, StandardCharsets.UTF_8,
            conditionalCompilation = ConditionalCompilationSettings.of("flag:false", null)
        )
        val issues = scanner.scanFile(Source(source)).issues
        assertThat(issues.map { it.primaryLocation.startLine() }).containsExactly(4)
    }

    private class Source(private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = "flagged.sql"
        override fun path(): Path = Path.of("flagged.sql")
        override fun type() = PlSqlFile.Type.MAIN
    }

}
