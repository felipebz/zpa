//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
