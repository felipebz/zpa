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

import com.felipebz.zpa.TestPlSqlVisitorRunner
import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier
import com.felipebz.zpa.symbols.DefaultTypeSolver
import com.felipebz.zpa.symbols.SymbolVisitor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class UnnecessaryStringLiteralConcatenationCheckTest : BaseCheckTest() {

    @Test
    fun reportsOnlyEligibleLiteralRuns() {
        PlSqlCheckVerifier.verify(
            getPath("unnecessary_string_literal_concatenation.sql"),
            UnnecessaryStringLiteralConcatenationCheck()
        )
    }

    @Test
    fun reportsIndependentRunsInOneExpression() {
        val check = UnnecessaryStringLiteralConcatenationCheck()
        TestPlSqlVisitorRunner.scanFile(
            File(getPath("unnecessary_string_literal_concatenation_multiple_runs.sql")),
            null,
            SymbolVisitor(DefaultTypeSolver(), isGlobalContext = true),
            check
        )

        assertThat(check.issues()).hasSize(2)
        assertThat(check.issues().map { it.primaryLocation().startLine() }).containsExactly(2, 2)
        assertThat(check.issues().map { it.primaryLocation().startLineOffset() }).containsExactly(11, 30)
    }

    @Test
    fun suppressesARunThatCannotBeProvenSafeToMerge() {
        val literal = "a".repeat(334)
        val file = File.createTempFile("unnecessary-string-literal-concatenation", ".sql")
        file.writeText("begin\n  v := '$literal' || '$literal';\nend;")

        try {
            val check = UnnecessaryStringLiteralConcatenationCheck()
            TestPlSqlVisitorRunner.scanFile(
                file,
                null,
                SymbolVisitor(DefaultTypeSolver(), isGlobalContext = true),
                check
            )

            assertThat(check.issues()).isEmpty()
        } finally {
            file.delete()
        }
    }

    @Test
    fun evaluatesSizeForTheCompleteLiteralRun() {
        val literal = "a".repeat(221)
        val file = File.createTempFile("unnecessary-string-literal-concatenation", ".sql")
        file.writeText("begin\n  v := '$literal' || '$literal' || '$literal';\nend;")

        try {
            val check = UnnecessaryStringLiteralConcatenationCheck()
            TestPlSqlVisitorRunner.scanFile(
                file,
                null,
                SymbolVisitor(DefaultTypeSolver(), isGlobalContext = true),
                check
            )

            assertThat(check.issues()).isEmpty()
        } finally {
            file.delete()
        }
    }
}
