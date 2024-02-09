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

import com.felipebz.flr.api.Trivia
import org.sonar.plsqlopen.TestPlSqlVisitorRunner
import org.sonar.plsqlopen.metadata.FormsMetadata
import org.sonar.plsqlopen.symbols.DefaultTypeSolver
import org.sonar.plsqlopen.symbols.ScopeImpl
import org.sonar.plsqlopen.symbols.SymbolVisitor
import org.sonar.plugins.plsqlopen.api.checks.PlSqlCheck
import java.io.File

class PlSqlCheckVerifier : PlSqlCheck() {

    private val expectedIssues = ArrayList<TestIssue>()

    override fun visitComment(trivia: Trivia, content: String) {
        val text = content.trim { it <= ' ' }
        val marker = "Noncompliant"

        if (text.startsWith(marker)) {
            var issueLine = trivia.token.line
            var paramsAndMessage = text.substring(marker.length).trim { it <= ' ' }

            if (paramsAndMessage.startsWith("@")) {
                val spaceSplit = paramsAndMessage.split("[\\s\\[{]".toRegex(), 2).toTypedArray()

                val shiftValue = spaceSplit[0]

                if (shiftValue[1] != '+' && shiftValue[1] != '-') {
                    throw AssertionError("Use only '@+N' or '@-N' to shifts messages.")
                }

                issueLine += Integer.valueOf(shiftValue.substring(1))
                paramsAndMessage = if (spaceSplit.size > 1) spaceSplit[1] else ""
            }

            val issue = TestIssue(null, issueLine)

            if (paramsAndMessage.startsWith("{{")) {
                val endIndex = paramsAndMessage.indexOf("}}")
                val message = paramsAndMessage.substring(2, endIndex)
                issue.message = message
                paramsAndMessage = paramsAndMessage.substring(endIndex + 2).trim { it <= ' ' }
            }

            if (paramsAndMessage.startsWith("[[")) {
                val endIndex = paramsAndMessage.indexOf("]]")
                addParams(issue, paramsAndMessage.substring(2, endIndex))
            }

            expectedIssues.add(issue)

        } else if (text.startsWith("^")) {
            addPreciseLocation(trivia)
        }
    }

    private fun addPreciseLocation(trivia: Trivia) {
        val token = trivia.token
        val line = token.line
        val text = token.value
        if (token.column > 1) {
            throw IllegalStateException("Line $line: comments asserting a precise location should start at column 1")
        }
        val missingAssertionMessage = "Invalid test file: a precise location is provided at line $line but no issue is asserted at line ${line - 1}"
        if (expectedIssues.isEmpty()) {
            throw IllegalStateException(missingAssertionMessage)
        }
        val issue = expectedIssues[expectedIssues.size - 1]
        if (issue.line != line - 1) {
            throw IllegalStateException(missingAssertionMessage)
        }
        issue.endLine = issue.line
        issue.startColumn = text.indexOf('^') + 1
        issue.endColumn = text.lastIndexOf('^') + 2
    }

    companion object {

        @JvmStatic
        @JvmOverloads
        fun verify(path: String, check: PlSqlCheck, metadata: FormsMetadata? = null) {
            val verifier = PlSqlCheckVerifier()
            val file = File(path)

            TestPlSqlVisitorRunner.scanFile(file, metadata, SymbolVisitor(DefaultTypeSolver(), ScopeImpl()), verifier, check)
            val issues = check.issues()

            val actualIssues = issues.sortedBy { it.primaryLocation().startLine() }.iterator()
            val expectedIssues = verifier.expectedIssues.sortedBy { it.line }

            for (expected in expectedIssues) {
                if (actualIssues.hasNext()) {
                    verifyIssue(expected, actualIssues.next())
                } else {
                    throw AssertionError("Missing issue at line ${expected.line}")
                }
            }

            if (actualIssues.hasNext()) {
                val issue = actualIssues.next()
                throw AssertionError("Unexpected issue at line ${line(issue)}: \"${issue.primaryLocation().message()}\"")
            }

        }

        private fun verifyIssue(expected: TestIssue, actual: PreciseIssue) {
            if (line(actual) > expected.line) {
                throw AssertionError("Missing issue at line ${expected.line}")
            }
            if (line(actual) < expected.line) {
                throw AssertionError("Unexpected issue at line ${line(actual)}: \"${actual.primaryLocation().message()}\"")
            }
            assertEquals(expected.message, actual.primaryLocation().message(), "Bad message at line ${expected.line}")
            assertEquals(expected.effortToFix, actual.cost(), "Bad effortToFix at line ${expected.line}")
            assertEquals(expected.startColumn, actual.primaryLocation().startLineOffset() + 1, "Bad start column at line ${expected.line}")
            assertEquals(expected.endColumn, actual.primaryLocation().endLineOffset() + 1, "Bad end column at line ${expected.line}")
            assertEquals(expected.endLine, actual.primaryLocation().endLine(), "Bad end line at line ${expected.line}")
            assertEquals(expected.secondaryLines, secondary(actual), "Bad secondary locations at line ${expected.line}")
        }

        private fun assertEquals(expected: Any?, actual: Any?, message: String) {
            if (expected != null && expected != actual) {
                throw AssertionError("$message ==> expected: \"${expected}\" but was: \"${actual}\"")
            }
        }

        private fun secondary(issue: PreciseIssue): List<Int> {
            val result = ArrayList<Int>()

            for (issueLocation in issue.secondaryLocations()) {
                result.add(issueLocation.startLine())
            }

            return result.sorted()
        }

        private fun line(issue: PreciseIssue): Int {
            return issue.primaryLocation().startLine()
        }

        private fun addParams(issue: TestIssue, params: String) {
            for (param in params.split(';')) {
                val equalIndex = param.indexOf('=')
                if (equalIndex == -1) {
                    throw IllegalStateException("Invalid param at line 1: $param")
                }
                val name = param.substring(0, equalIndex)
                val value = param.substring(equalIndex + 1)

                when {
                    "effortToFix".equals(name, ignoreCase = true) -> issue.effortToFix = Integer.valueOf(value)
                    "sc".equals(name, ignoreCase = true) -> issue.startColumn = Integer.valueOf(value)
                    "ec".equals(name, ignoreCase = true) -> issue.endColumn = Integer.valueOf(value)
                    "el".equals(name, ignoreCase = true) -> issue.endLine = lineValue(issue.line, value)
                    "secondary".equals(name, ignoreCase = true) -> addSecondaryLines(issue, value)
                    else -> throw IllegalStateException("Invalid param at line 1: $name")
                }
            }
        }

        private fun addSecondaryLines(issue: TestIssue, value: String) {
            val secondaryLines = ArrayList<Int>()
            if ("" != value) {
                for (secondary in value.split(',')) {
                    secondaryLines.add(lineValue(issue.line, secondary))
                }
            }
            issue.secondaryLines = secondaryLines
        }

        private fun lineValue(baseLine: Int, shift: String): Int {
            if (shift.startsWith("+")) {
                return baseLine + Integer.valueOf(shift.substring(1))
            }
            return if (shift.startsWith("-")) {
                baseLine - Integer.valueOf(shift.substring(1))
            } else Integer.valueOf(shift)
        }
    }

}
