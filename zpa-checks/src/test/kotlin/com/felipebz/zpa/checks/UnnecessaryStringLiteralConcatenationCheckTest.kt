//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.zpa.TestPlSqlVisitorRunner
import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier
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
                check
            )

            assertThat(check.issues()).isEmpty()
        } finally {
            file.delete()
        }
    }
}
