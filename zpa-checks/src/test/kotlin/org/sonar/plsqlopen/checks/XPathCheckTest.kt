/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2023 Felipe Zorzo
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
package org.sonar.plsqlopen.checks

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.sonar.plsqlopen.checks.verifier.PlSqlCheckVerifier
import org.sonar.plsqlopen.squid.AnalysisException

class XPathCheckTest : BaseCheckTest() {

    @Test
    fun line_level_issue() {
        analyze("xpath_statement.sql", "//STATEMENT")
    }

    @Test
    fun boolean_true_result() {
        analyze("xpath_count_statement.sql", "count(//STATEMENT) > 0")
    }

    @Test
    fun boolean_false_result() {
        analyze("xpath.sql", "count(//STATEMENT) < 0")
    }

    @Test
    fun integer_xpath_result() {
        analyze("xpath.sql", "count(//STATEMENT)")
    }

    @Test
    fun empty_query() {
        analyze("xpath.sql", "")
    }

    @Test
    fun invalid_query() {
        assertThrows<AnalysisException> {
            analyze("xpath.sql", "+++")
        }
    }

    private fun analyze(fileName: String, xpathQuery: String) {
        val check = XPathCheck()
        check.xpathQuery = xpathQuery
        check.message = MESSAGE
        PlSqlCheckVerifier.verify("src/test/resources/checks/$fileName", check)
    }

    companion object {
        private const val MESSAGE = "Avoid statements"
    }

}
