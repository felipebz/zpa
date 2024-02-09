/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
package org.sonar.plsqlopen.checks.verifier

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.api.Token
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.sonar.plsqlopen.checks.IssueLocation
import org.sonar.plsqlopen.squid.AnalysisException
import org.sonar.plugins.plsqlopen.api.checks.PlSqlCheck

class PlSqlCheckVerifierTest {

    @Test
    fun verify_line_issues() {
        val visitor = FakeCheck().withDefaultIssues()
        PlSqlCheckVerifier.verify(FILENAME_ISSUES, visitor)
    }

    @Test
    fun verify_unexpected_issue() {
        val visitor = FakeCheck().withDefaultIssues().withIssue(4, "extra message")

        try {
            PlSqlCheckVerifier.verify(FILENAME_ISSUES, visitor)
            fail("Test should fail")
        } catch (e: AssertionError) {
            assertThat(e).hasMessage("Unexpected issue at line 4: \"extra message\"")
        }
    }

    @Test
    fun verify_combined_missing_expected_and_unexpected_issues() {
        val visitor = FakeCheck().withDefaultIssues().withIssue(4, "extra message")
                .withoutIssue(1)

        try {
            PlSqlCheckVerifier.verify(FILENAME_ISSUES, visitor)
            fail("Test should fail")
        } catch (e: AssertionError) {
            assertThat(e).hasMessage("Missing issue at line 1")
        }
    }

    @Test
    fun verify_missing_expected_issue() {
        val visitor = FakeCheck().withDefaultIssues().withoutIssue(1)

        try {
            PlSqlCheckVerifier.verify(FILENAME_ISSUES, visitor)
            fail("Test should fail")
        } catch (e: AssertionError) {
            assertThat(e).hasMessage("Missing issue at line 1")
        }
    }


    @Test
    fun verify_no_issue() {
        PlSqlCheckVerifier.verify(FILENAME_NO_ISSUE, noEffectCheck)
    }

    @Test
    fun verify_should_fail_when_using_incorrect_shift() {
        try {
            PlSqlCheckVerifier.verify("src/test/resources/check_verifier_incorrect_shift.sql", noEffectCheck)
            fail("Test should fail")
        } catch (e: AssertionError) {
            assertThat(e).hasMessage("Use only '@+N' or '@-N' to shifts messages.")
        }
    }

    @Test
    fun verify_should_fail_when_using_incorrect_attribute() {
        try {
            PlSqlCheckVerifier.verify("src/test/resources/check_verifier_incorrect_attribute.sql",
                    noEffectCheck)
            fail("Test should fail")
        } catch (e: AnalysisException) {
            assertThat(e).hasMessage("Error executing checks on file check_verifier_incorrect_attribute.sql: Invalid param at line 1: invalid")
        }
    }

    @Test
    fun verify_should_fail_when_using_incorrect_attribute2() {
        try {
            PlSqlCheckVerifier.verify("src/test/resources/check_verifier_incorrect_attribute2.sql",
                    noEffectCheck)
            fail("Test should fail")
        } catch (e: AnalysisException) {
            assertThat(e).hasMessage("Error executing checks on file check_verifier_incorrect_attribute2.sql: Invalid param at line 1: invalid")
        }
    }

    @Test
    fun verify_should_fail_when_using_incorrect_secondaryLocation() {
        val visitor = FakeCheck().withDefaultIssues()
        try {
            PlSqlCheckVerifier.verify("src/test/resources/check_verifier_incorrect_secondary_location.sql", visitor)
            fail("Test should fail")
        } catch (e: AssertionError) {
            assertThat(e).hasMessage("Bad secondary locations at line 8 ==> expected: \"[4]\" but was: \"[3, 4]\"")
        }
    }

    @Test
    fun verify_should_fail_when_using_incorrect_secondaryLocation2() {
        val visitor = FakeCheck().withDefaultIssues()
        try {
            PlSqlCheckVerifier.verify("src/test/resources/check_verifier_incorrect_secondary_location2.sql", visitor)
            fail("Test should fail")
        } catch (e: AssertionError) {
            assertThat(e).hasMessage("Bad secondary locations at line 8 ==> expected: \"[3, 4, 5]\" but was: \"[3, 4]\"")
        }
    }

    @Test
    fun verify_should_fail_when_precision_location_comment_is_invalid() {
        try {
            PlSqlCheckVerifier.verify("src/test/resources/check_verifier_incorrect_comment.sql", FakeCheck())
            fail("Test should fail")
        } catch (e: AnalysisException) {
            assertThat(e).hasMessage("Error executing checks on file check_verifier_incorrect_comment.sql: Line 3: comments asserting a precise location should start at column 1")
        }
    }

    @Test
    fun verify_unexpected_precise_location() {
        try {
            PlSqlCheckVerifier.verify("src/test/resources/check_verifier_unexpected_precise_location.sql", FakeCheck())
        } catch (e: AnalysisException) {
            assertThat(e).hasMessage("Error executing checks on file check_verifier_unexpected_precise_location.sql: Invalid test file: a precise location is provided at line 3 but no issue is asserted at line 2")
        }
    }

    @Test
    fun verify_unexpected_precise_location2() {
        try {
            PlSqlCheckVerifier.verify("src/test/resources/check_verifier_unexpected_precise_location2.sql", FakeCheck())
        } catch (e: AnalysisException) {
            assertThat(e).hasMessage("Error executing checks on file check_verifier_unexpected_precise_location2.sql: Invalid test file: a precise location is provided at line 3 but no issue is asserted at line 2")
        }
    }

    private class FakeCheck : PlSqlCheck() {

        var issues = hashMapOf<Int, MutableList<String>>()
        var preciseIssues = linkedMapOf<Int, MutableList<IssueLocation>>()

        fun withDefaultIssues(): FakeCheck {
            return this.withIssue(1, "message")
                    .withIssue(2, "message1")
                    .withIssue(5, "message2")
                    .withIssue(6, "message3")
                    .withIssue(6, "message3")
                    .withPreciseIssue(
                            IssueLocation.preciseLocation(mockNode(8, 9, 8, 10), "message4"),
                            IssueLocation.atLineLevel("no message", 3),
                            IssueLocation.atLineLevel("no message", 4)
                    )
                    .withPreciseIssue(IssueLocation.atLineLevel("no message", 9))
                    .withPreciseIssue(IssueLocation.preciseLocation(mockNode(11, 5, 12, 11), "message12"))
                    .withIssue(14, "message17")
                    .withPreciseIssue(IssueLocation.preciseLocation(mockNode(15, 5, 15, 9), "baseline"))
        }

        fun withIssue(line: Int, message: String) = apply {
            issues.getOrPut(line) { mutableListOf() }.add(message)
        }

        fun withPreciseIssue(vararg message: IssueLocation) = apply {
            preciseIssues.getOrPut(message[0].startLine()) { mutableListOf() }.addAll(message.toList())
        }

        fun withoutIssue(line: Int) = apply {
            issues.remove(line)
            preciseIssues.remove(line)
        }

        override fun visitFile(node: AstNode) {
            for ((line, messages) in issues) {
                for (message in messages) {
                    addLineIssue(message, line)
                }
            }

            for (locations in preciseIssues.values) {
                var issue: PreciseIssue? = null
                for (location in locations) {
                    if (issue == null) {
                        issue = addIssue(location).withCost(3)
                    } else {
                        issue.secondary(location)
                    }
                }
            }
        }

        fun mockNode(startLine: Int, startCharacter: Int, endLine: Int, endCharacter: Int): AstNode {
            val token = Token.builder()
                    .setLine(startLine)
                    .setColumn(startCharacter - 1)
                    .setValueAndOriginalValue("")
                    .setType(GenericTokenType.IDENTIFIER)
                    .build()

            val lastToken = Token.builder()
                    .setLine(endLine)
                    .setColumn(endCharacter - 1)
                    .setValueAndOriginalValue("")
                    .setType(GenericTokenType.IDENTIFIER)
                    .build()

            val node = AstNode(token)
            node.addChild(AstNode(token))
            node.addChild(AstNode(lastToken))

            return node
        }

    }

    companion object {
        private const val FILENAME_ISSUES = "src/test/resources/check_verifier.sql"
        private const val FILENAME_NO_ISSUE = "src/test/resources/check_verifier_no_issue.sql"

        private val noEffectCheck: PlSqlCheck
            get() = object : PlSqlCheck() { }
    }

}
