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

import com.felipebz.zpa.api.checks.PlSqlCheck.PreciseIssue

/** Compares a check's issues with a fixture's expectations; only the details a fixture writes are checked. */
internal object IssueAssertions {

    /** [file], when given, names the fixture in the failure messages. */
    fun assertIssues(expected: List<TestIssue>, issues: List<PreciseIssue>, file: String? = null) {
        val prefix = file?.let { "$it: " }.orEmpty()
        val actualIssues = issues.sortedBy { it.primaryLocation().startLine() }.iterator()

        for (issue in expected.sortedBy { it.line }) {
            if (actualIssues.hasNext()) {
                verifyIssue(issue, actualIssues.next(), prefix)
            } else {
                throw AssertionError("${prefix}Missing issue at line ${issue.line}")
            }
        }

        if (actualIssues.hasNext()) {
            val issue = actualIssues.next()
            throw AssertionError("${prefix}Unexpected issue at line ${line(issue)}: \"${issue.primaryLocation().message()}\"")
        }
    }

    private fun verifyIssue(expected: TestIssue, actual: PreciseIssue, prefix: String) {
        if (line(actual) > expected.line) {
            throw AssertionError("${prefix}Missing issue at line ${expected.line}")
        }
        if (line(actual) < expected.line) {
            throw AssertionError("${prefix}Unexpected issue at line ${line(actual)}: \"${actual.primaryLocation().message()}\"")
        }
        assertEquals(expected.message, actual.primaryLocation().message(), "${prefix}Bad message at line ${expected.line}")
        assertEquals(expected.effortToFix, actual.cost(), "${prefix}Bad effortToFix at line ${expected.line}")
        assertEquals(expected.startColumn, actual.primaryLocation().startLineOffset() + 1, "${prefix}Bad start column at line ${expected.line}")
        assertEquals(expected.endColumn, actual.primaryLocation().endLineOffset() + 1, "${prefix}Bad end column at line ${expected.line}")
        assertEquals(expected.endLine, actual.primaryLocation().endLine(), "${prefix}Bad end line at line ${expected.line}")
        assertEquals(expected.secondaryLines, secondary(actual), "${prefix}Bad secondary locations at line ${expected.line}")
    }

    private fun assertEquals(expected: Any?, actual: Any?, message: String) {
        if (expected != null && expected != actual) {
            throw AssertionError("$message ==> expected: \"${expected}\" but was: \"${actual}\"")
        }
    }

    private fun secondary(issue: PreciseIssue): List<Int> =
        issue.secondaryLocations().map { it.startLine() }.sorted()

    private fun line(issue: PreciseIssue): Int = issue.primaryLocation().startLine()
}
