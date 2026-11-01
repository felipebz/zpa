//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier
import com.felipebz.zpa.squid.AnalysisException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
