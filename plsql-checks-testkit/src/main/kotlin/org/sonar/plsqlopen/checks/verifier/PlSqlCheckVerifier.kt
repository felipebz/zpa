/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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

import com.google.common.base.Splitter
import com.sonar.sslr.api.Trivia
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.sonar.plsqlopen.TestPlSqlVisitorRunner
import org.sonar.plsqlopen.metadata.FormsMetadata
import org.sonar.plsqlopen.squid.PlSqlAstWalker
import org.sonar.plsqlopen.symbols.DefaultTypeSolver
import org.sonar.plsqlopen.symbols.SymbolVisitor
import org.sonar.plugins.plsqlopen.api.checks.PlSqlCheck
import java.io.File
import java.util.*

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
                    fail("Use only '@+N' or '@-N' to shifts messages.")
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
            throw IllegalStateException(
                    "Line $line: comments asserting a precise location should start at column 1")
        }
        val missingAssertionMessage = String.format(
                "Invalid test file: a precise location is provided at line %s but no issue is asserted at line %s",
                line, line - 1)
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
        fun scanFileForIssues(file: File, metadata: FormsMetadata?, check: PlSqlCheck): List<PreciseIssue> {
            val walker = PlSqlAstWalker(Arrays.asList(SymbolVisitor(DefaultTypeSolver()), check))
            walker.walk(TestPlSqlVisitorRunner.createContext(file, metadata))
            return check.issues()
        }

        @JvmStatic
        @JvmOverloads
        fun verify(path: String, check: PlSqlCheck, metadata: FormsMetadata? = null) {
            val verifier = PlSqlCheckVerifier()
            val file = File(path)
            TestPlSqlVisitorRunner.scanFile(file, metadata, verifier)

            val actualIssues = getActualIssues(file, metadata, check)
            val expectedIssues = verifier.expectedIssues.sortedBy { it.line }

            for (expected in expectedIssues) {
                if (actualIssues.hasNext()) {
                    verifyIssue(expected, actualIssues.next())
                } else {
                    throw AssertionError("Missing issue at line " + expected.line)
                }
            }

            if (actualIssues.hasNext()) {
                val issue = actualIssues.next()
                throw AssertionError(
                        "Unexpected issue at line " + line(issue) + ": \"" + issue.primaryLocation().message() + "\"")
            }

        }

        private fun verifyIssue(expected: TestIssue, actual: PreciseIssue) {
            if (line(actual) > expected.line) {
                fail("Missing issue at line " + expected.line)
            }
            if (line(actual) < expected.line) {
                fail("Unexpected issue at line " + line(actual) + ": \"" + actual.primaryLocation().message() + "\"")
            }
            expected.message?.let {
                assertThat(actual.primaryLocation().message()).`as`("Bad message at line " + expected.line)
                        .isEqualTo(it)
            }
            expected.effortToFix?.let {
                assertThat(actual.cost()).`as`("Bad effortToFix at line " + expected.line)
                        .isEqualTo(it)
            }
            expected.startColumn?.let {
                assertThat(actual.primaryLocation().startLineOffset() + 1).`as`("Bad start column at line " + expected.line)
                        .isEqualTo(it)
            }
            expected.endColumn?.let {
                assertThat(actual.primaryLocation().endLineOffset() + 1).`as`("Bad end column at line " + expected.line)
                        .isEqualTo(it)
            }
            expected.endLine?.let {
                assertThat(actual.primaryLocation().endLine()).`as`("Bad end line at line " + expected.line)
                        .isEqualTo(it)
            }
            expected.secondaryLines?.let {
                assertThat(secondary(actual)).`as`("Bad secondary locations at line " + expected.line)
                        .isEqualTo(it)
            }
        }

        private fun secondary(issue: PreciseIssue): List<Int> {
            val result = ArrayList<Int>()

            for (issueLocation in issue.secondaryLocations()) {
                result.add(issueLocation.startLine())
            }

            return result.sorted()
        }

        private fun getActualIssues(file: File, metadata: FormsMetadata?, check: PlSqlCheck): Iterator<PreciseIssue> {
            val issues = scanFileForIssues(file, metadata, check)
            val sortedIssues = issues.sortedBy { it.primaryLocation().startLine() }
            return sortedIssues.iterator()
        }

        private fun line(issue: PreciseIssue): Int {
            return issue.primaryLocation().startLine()
        }

        private fun addParams(issue: TestIssue, params: String) {
            for (param in Splitter.on(';').split(params)) {
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
                for (secondary in Splitter.on(',').split(value)) {
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
