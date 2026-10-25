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
package com.felipebz.zpa.checks.verifier

import com.felipebz.flr.api.Trivia
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.metadata.FormsMetadata
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.squid.ConditionalCompilationSettings
import com.felipebz.zpa.squid.PlSqlConfiguration
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/**
 * Reads the expected issues a fixture writes in its comments (`-- Noncompliant`, with an optional `@+N`/`@-N` shift,
 * `{{message}}`, `[[params]]`, and a `^^^` line for a precise location), and verifies a check against them.
 */
class PlSqlCheckVerifier : PlSqlCheck() {

    internal val expectedIssues = ArrayList<TestIssue>()

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
        check(token.column <= 1) { "Line $line: comments asserting a precise location should start at column 1" }
        val missingAssertionMessage = "Invalid test file: a precise location is provided at line $line but no issue is asserted at line ${line - 1}"
        check(expectedIssues.isNotEmpty()) { missingAssertionMessage }
        val issue = expectedIssues[expectedIssues.size - 1]
        check(issue.line == line - 1) { missingAssertionMessage }
        issue.endLine = issue.line
        issue.startColumn = text.indexOf('^') + 1
        issue.endColumn = text.lastIndexOf('^') + 2
    }

    private fun addParams(issue: TestIssue, params: String) {
        for (param in params.split(';')) {
            val equalIndex = param.indexOf('=')
            check(equalIndex != -1) { "Invalid param at line 1: $param" }
            val name = param.substring(0, equalIndex)
            val value = param.substring(equalIndex + 1)

            when {
                "effortToFix".equals(name, ignoreCase = true) -> issue.effortToFix = Integer.valueOf(value)
                "sc".equals(name, ignoreCase = true) -> issue.startColumn = Integer.valueOf(value)
                "ec".equals(name, ignoreCase = true) -> issue.endColumn = Integer.valueOf(value)
                "el".equals(name, ignoreCase = true) -> issue.endLine = lineValue(issue.line, value)
                "secondary".equals(name, ignoreCase = true) -> addSecondaryLines(issue, value)
                else -> error("Invalid param at line 1: $name")
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

    companion object {

        /**
         * Scans the fixture at [path] as production does (the scanner's semantic pipeline, with a project prepared from
         * this file, or none when [prepared] is false, as in an analysis without a project) and verifies [check]'s
         * issues against the fixture's expectations. The check runs whatever its rule scope. A fixture that does not
         * parse fails.
         */
        @JvmStatic
        @JvmOverloads
        fun verify(
            path: String,
            check: PlSqlCheck,
            metadata: FormsMetadata? = null,
            conditionalCompilation: ConditionalCompilationSettings = ConditionalCompilationSettings.NONE,
            errorRecovery: Boolean = false,
            prepared: Boolean = true
        ) {
            val file = File(path)
            val text = file.readText()
            val configuration = PlSqlConfiguration(StandardCharsets.UTF_8, errorRecovery, conditionalCompilation)
            // The scanner reports a fixture that does not parse instead of failing: parse it first so that it fails.
            PlSqlParser.create(configuration).parse(text)

            val fileId = FileId(path)
            val project = if (!prepared) ProjectAnalysisContext.NOT_PREPARED else ProjectAnalysisContext.prepared(
                ProjectIndexPreparation(ProjectDeclarationExtractor(configuration))
                    .prepare(listOf(ProjectSource(fileId) { text }), concurrent = false)
            )
            val scanner = AstScanner(emptyList(), metadata, errorRecovery, StandardCharsets.UTF_8, project, conditionalCompilation)
            val verifier = PlSqlCheckVerifier()
            scanner.scanFile(FixtureFile(file.name, file.toPath(), text), listOf(verifier, check), fileId)
            IssueAssertions.assertIssues(verifier.expectedIssues, check.issues())
        }
    }

    internal class FixtureFile(private val name: String, private val path: Path, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path() = path
        override fun type() = PlSqlFile.Type.MAIN
    }

}
