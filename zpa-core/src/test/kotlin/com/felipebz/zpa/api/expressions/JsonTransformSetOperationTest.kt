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
package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import com.felipebz.zpa.api.SingleRowSqlFunctionsGrammar
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JsonTransformSetOperationTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    private fun matches(vararg operations: String) =
        operations.forEach { assertThat(p).describedAs(it).matches("json_transform(d, $it)") }

    private fun notMatches(vararg operations: String) =
        operations.forEach { assertThat(p).describedAs(it).notMatches("json_transform(d, $it)") }

    @Test
    fun matchesMinimalSetOperations() {
        matches(
            "add_set '\$.a' = 4", "remove_set '\$.a' = 2", "ADD_SET '\$.a' = 1 + 3", "add_set '\$.a' = null",
            "add_set '\$.a' = 'x' format json", "add_set '\$.a' = path '\$.b[0]'", "remove_set '\$.a' = path '6'",
            "add_set '\$.a' = f(x)", "remove_set '\$.a' = (select 1 from dual)",
        )
    }

    @Test
    fun matchesIgnoreIfHandlers() {
        matches(
            "add_set '\$.a' = 2 ignore if present", "remove_set '\$.a' = 5 ignore if absent",
            "remove_set '\$.a' = path '6' ignore if absent",
            "add_set '\$.a' = 4 ignore if present ignore on missing", "add_set '\$.a' = 4 ignore on missing ignore if present",
            "remove_set '\$.b' = 5 ignore on missing ignore if absent",
        )
    }

    @Test
    fun matchesDocumentedOnHandlers() {
        matches(
            "add_set '\$.b' = 5 create on missing", "add_set '\$.a' = 4 ignore on missing", "add_set '\$.a' = 4 error on missing",
            "add_set '\$.a' = null ignore on null", "add_set '\$.a' = null error on null", "add_set '\$.a' = null null on null",
            "add_set '\$.a' = 4 null on empty", "add_set '\$.a' = 4 ignore on empty", "add_set '\$.a' = 4 error on empty",
            "add_set '\$.a' = 4 create on missing ignore on null error on empty ignore if present",
            "remove_set '\$.a' = 2 ignore on missing", "remove_set '\$.a' = 2 error on missing",
            "remove_set '\$.a' = null ignore on null null on null error on null",
            "remove_set '\$.a' = 2 null on empty ignore on empty error on empty ignore if absent",
        )
    }

    @Test
    fun acceptsStructurallyValidHandlersTheOperationsRejectLater() {
        matches(
            "remove_set '\$.a' = 2 create on missing", "remove_set '\$.a' = 2 null on missing", "add_set '\$.a' = 4 replace on missing",
            "add_set '\$.a' = 2 remove on null", "remove_set '\$.a' = 2 remove on null", "add_set '\$.a' = 4 ignore on mismatch",
            "add_set '\$.a' = 4 ignore on existing", "add_set '\$.a' = 4 ignore on error",
        )
    }

    @Test
    fun rejectsIgnoreIfHandlersOfTheOtherOperationAndUnknownForms() {
        notMatches(
            "add_set '\$.a' = 2 ignore if absent", "remove_set '\$.a' = 5 ignore if present",
            "remove_set '\$.a' = 5 ignore on absent", "remove_set '\$.b' = 5 ignore if missing ignore if absent",
        )
    }

    @Test
    fun rejectsMalformedSetOperations() {
        notMatches(
            "add_set '\$.a'", "add_set '\$.a' =", "add_set '\$.a' = 4, 5", "add_set '\$.a' 4",
            "remove_set '\$.a'", "remove_set '\$.a' =",
            "add_set '\$.a' = 4 ignore if", "add_set '\$.a' = 4 ignore if foo", "add_set '\$.a' = 4 ignore present",
            "add_set '\$.a' = 4 error if present", "add_set '\$.a' = 4 null if present",
            "add_set '\$.a' = 4 ignore if absent ignore if present",
        )
    }

    @Test
    fun mixesWithOtherOperationsAndClauses() {
        matches(
            "add_set '\$.a' = 4, remove_set '\$.a' = 1", "add_set '\$.a' = 4, set '\$.b' = 1, remove_set '\$.a' = 1",
            "remove_set '\$.a' = 1 ignore if absent, append '\$.c' = 2 create on missing",
            "case when '\$.a' then (add_set '\$.a' = 4) else (remove_set '\$.a' = 4) end",
            "nested path '\$.o' (add_set '\$.a' = 4 ignore if present)",
        )
        assertThat(p).matches("json_transform(d, add_set '\$.a' = 4 returning varchar2(100))")
        assertThat(p).matches("json_transform(d, add_set '\$.a' = 4 ignore if present returning varchar2(100))")
        assertThat(p).matches("json_transform(d, remove_set '\$.a' = :v passing 1 as \"x\" pretty)")
    }

    @Test
    fun matchesOracleDocumentationExamples() {
        listOf(
            "json_transform('{\"a\":[1,2,3]}', add_set '\$.a' = 4)",
            "json_transform('{\"a\":[1,2,3]}', add_set '\$.a' = 2)",
            "json_transform('{\"a\":[1,2,3]}', add_set '\$.a' = 2 ignore if present)",
            "json_transform('{\"a\":[1,2,3]}', remove_set '\$.a' = 2)",
            "json_transform('{\"a\":[1,2,3]}', remove_set '\$.a' = 5)",
            "json_transform('{\"a\":[ 1,2,3 ]}', remove_set '\$.a' = path '6' ignore if absent)",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        assertThat(p).notMatches("json_transform('{\"a\":[1,2,3]}', remove_set '\$.a' = 5 ignore on absent)")
        assertThat(p).notMatches("json_transform('{\"b\":[ 1,2,3 ]}', remove_set '\$.a' = path '6' ignore if missing ignore if absent)")
    }

    @Test
    fun buildsOperationNodes() {
        val tree = p.parse("json_transform(d, add_set '\$.a' = 2 ignore on missing ignore if present, remove_set '\$.b' = path '\$.c' ignore if absent)")
        val add = tree.getFirstDescendant(SingleRowSqlFunctionsGrammar.JSON_ADD_SET_OPERATION)!!
        assertThatAst(add.children.map { it.tokenOriginalValue.lowercase() }).containsExactly(
            "add_set", "'\$.a'", "=", "2", "ignore", "on", "missing", "ignore", "if", "present")
        val remove = tree.getFirstDescendant(SingleRowSqlFunctionsGrammar.JSON_REMOVE_SET_OPERATION)!!
        assertThatAst(remove.children.map { it.name }).containsExactly(
            "REMOVE_SET", "STRING_LITERAL", "EQUALS", "JSON_RHS_EXPRESSION", "IGNORE", "IF", "ABSENT")
        assertThatAst(tree.getDescendants(SingleRowSqlFunctionsGrammar.JSON_TRANSFORM_OPERATION)).hasSize(2)
    }
}
