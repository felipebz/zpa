//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.RecognitionException
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.StandardCharsets

class ParsingErrorCheckTest : BaseCheckTest() {

    @Test
    fun test() {
        val file = File("src/test/resources/checks/parsing_error.sql")

        val parser = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8))
        val context: PlSqlVisitorContext
        try {
            parser.parse(file)
            throw IllegalStateException("Expected RecognitionException")
        } catch (e: RecognitionException) {
            context = PlSqlVisitorContext(null, e, null)
        }

        val check = ParsingErrorCheck()
        val issues = check.scanFileForIssues(context)
        assertThat(issues).hasSize(1)
        assertThat(issues[0].primaryLocation().startLine()).isEqualTo(1)
    }

    @Test
    fun testTolerantParsing() {
        val file = File("src/test/resources/checks/parsing_error.sql")

        val parser = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, isErrorRecoveryEnabled = true))

        val rootTree = parser.parse(file)
        val context = PlSqlVisitorContext(rootTree, null, null)

        val check = ParsingErrorCheck()
        val issues = check.scanFileForIssues(context)
        assertThat(issues).hasSize(1)
        assertThat(issues[0].primaryLocation().startLine()).isEqualTo(1)
    }

}
