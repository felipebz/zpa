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
package com.felipebz.zpa.project.extraction

import com.felipebz.zpa.lexer.PlSqlLexer
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/** Structural admission of initializer token slices (not an expression parser). */
class InitializerShapeTest {
    private fun admitted(text: String): Boolean {
        val tokens = PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).lex(text)
        return InitializerShape(TokenStream(FileId("init.sql"), tokens)).initializerShapeOk(0, tokens.size - 1)
    }

    @Test
    fun admitsLiteralsIdentifiersCallsAndParenthesizedExpressions() {
        listOf(
            "1", "'a;END;'", "x", "t.c", "f(1, g(2), 'x')", "(1 + 2) * 3", "-1", "NOT (a OR b)", "a || b", "pkg.f(a => 1, b => 2)",
            "t(1, 2, 3)", "t()", "x IS NOT NULL", "SYSDATE", "a[1]", "f((1), [2])"
        ).forEach { assertThat(admitted(it)).describedAs(it).isTrue() }
    }

    @Test
    fun admitsWellFormedCaseExpressionsAndCaseUsedAsAnIdentifier() {
        listOf(
            "CASE WHEN 1 = 1 THEN 1 ELSE 2 END", "CASE x WHEN 1 THEN 2 WHEN 3 THEN 4 END",
            "CASE WHEN 1 = 1 THEN CASE WHEN 2 = 2 THEN 1 ELSE 2 END ELSE 3 END", "CASE (1 + 1) WHEN 2 THEN 1 END",
            "f(CASE WHEN a THEN 1 END, CASE WHEN b THEN 2 END)", "CASE CASE WHEN a THEN 1 END WHEN 1 THEN 2 END",
            "case", "case(1)", "t.case + 1", "case.x", "CASE WHEN 1 = 1 THEN case ELSE 2 END", "function(1)", "FUNCTION(function(1))"
        ).forEach { assertThat(admitted(it)).describedAs(it).isTrue() }
    }

    @Test
    fun rejectsEmptyMisplacedAndMalformedStructure() {
        listOf(
            "", "* 2", "1 +", ", 1", "AND x", "x AND", "f(,1)", "f(1,)", "f(1,,2)", "1, 2", "(1", "1)", "(1]", "f(1, [2)", "[1)",
            "a := 1", "a;b", "f(x => 1) , ", "a => 1"
        ).forEach { assertThat(admitted(it)).describedAs(it).isFalse() }
    }

    @Test
    fun rejectsUnbalancedCaseStructureAndStrayKeywords() {
        listOf(
            "CASE WHEN x THEN 1", "CASE x END", "CASE WHEN x THEN END", "CASE WHEN x 1 END", "x THEN 1", "WHEN x", "1 ELSE 2", "1 END",
            "CASE WHEN a THEN 1 ELSE END", "CASE WHEN a THEN 1 ELSE 2 ELSE 3 END"
        ).forEach { assertThat(admitted(it)).describedAs(it).isFalse() }
    }

    @Test
    fun nestingIsAdmittedUpToTheDocumentedDepthLimit() {
        fun nested(depth: Int) = "(".repeat(depth) + "1" + ")".repeat(depth)
        assertThat(admitted(nested(64))).isTrue()
        assertThat(admitted(nested(65))).isFalse()
        fun cases(depth: Int) = "CASE WHEN 1 = 1 THEN ".repeat(depth) + "1" + " END".repeat(depth)
        assertThat(admitted(cases(5))).isTrue()
        assertThat(admitted(cases(200))).isFalse()
    }

    @Test
    fun caseMemoizationIsPerInvocation() {
        val tokens = PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).lex("CASE WHEN a THEN b")
        val shape = InitializerShape(TokenStream(FileId("init.sql"), tokens))
        // The same shape instance gives the same answer for every call: nothing is retained between invocations.
        repeat(3) { assertThat(shape.initializerShapeOk(0, tokens.size - 1)).isFalse() }
        assertThat(shape.initializerShapeOk(0, 1)).describedAs("a bare CASE identifier").isTrue()
    }
}
