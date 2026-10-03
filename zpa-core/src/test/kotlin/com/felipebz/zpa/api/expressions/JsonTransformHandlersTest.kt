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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class JsonTransformHandlersTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    private fun op(operation: String) = "json_transform(d, $operation)"

    private fun matches(vararg operations: String) {
        for (o in operations) assertThat(p).describedAs(o).matches(op(o))
    }

    private fun notMatches(vararg operations: String) {
        for (o in operations) assertThat(p).describedAs(o).notMatches(op(o))
    }

    @Test
    fun matchesRenameWithEqualsAndRightHandSide() {
        matches(
            "rename '\$.a' = 'b'", "rename '\$.a' = ('b')", "rename '\$.a' = path '\$.c'", "rename '\$.a' = 'b' || 'c'",
            "rename '\$.a' = 'b' error on missing", "rename '\$.a' = 'b' ignore on missing", "rename '\$.a' = 'b', set '\$.b' = 2",
            "rename '\$.abc' = 'xyz', set '\$.xyz' = 2"
        )
        notMatches(
            "rename '\$.a' with 'b'", "rename '\$.a' to 'b'", "rename '\$.a'", "rename '\$.a' =",
            "rename '\$.a' = 'b' create on missing", "rename '\$.a' = 'b' null on missing", "rename '\$.a' = 'b' error on null",
            "rename '\$.a' = 'b' error on mismatch", "rename '\$.a' = ('b' format json)"
        )
    }

    @Test
    fun matchesDocumentedHandlerSetsPerOperation() {
        matches(
            "set '\$.a' = 1 replace on existing create on missing remove on null null on empty ignore on error",
            "set '\$.a' = 1 ignore on existing error on missing null on null error on empty error on error",
            "insert '\$.a' = 1 replace on existing remove on null null on empty ignore on error",
            "append '\$.a' = 1 null on missing replace on mismatch null on null error on empty",
            "append '\$.a' = 1 create on missing create on mismatch ignore on null ignore on empty",
            "prepend '\$.a' = 1 create on missing create on mismatch", "prepend '\$.a' = 1 replace on mismatch",
            "replace '\$.a' = 1 create on missing remove on null null on empty error on error",
            "copy '\$.b' = path '\$.a' create on missing null on null error on empty",
            "merge '\$.a' = 1 create on missing error on mismatch null on null ignore on empty",
            "minus '\$.a' = 1 create on missing null on null", "union '\$.a' = 1 null on missing null on null",
            "intersect '\$.a' = 1 create on missing null on null", "remove '\$.a' error on missing", "keep '\$.a', '\$.b' error on missing",
            "sort '\$.a' null on missing null on mismatch error on empty ignore on error"
        )
    }

    @Test
    fun rejectsOptionsOutsideTheDocumentedSets() {
        notMatches(
            "append '\$.a' = 1 null on mismatch", "insert '\$.a' = 1 null on existing", "replace '\$.a' = 1 null on missing",
            "set '\$.a' = 1 create on null", "set '\$.a' = 1 create on empty", "set '\$.a' = 1 create on existing",
            "set '\$.a' = 1 ignore on mismatch", "insert '\$.a' = 1 create on missing", "replace '\$.a' = 1 replace on existing",
            "copy '\$.b' = path '\$.a' ignore on mismatch", "merge '\$.a' = 1 null on mismatch", "merge '\$.a' = 1 create on empty",
            "minus '\$.a' = 1 error on mismatch", "union '\$.a' = 1 ignore on empty", "intersect '\$.a' = 1 error on error",
            "remove '\$.a' null on missing", "keep '\$.a' create on missing", "keep '\$.a' null on missing", "sort '\$.a' create on missing",
            "sort '\$.a' replace on mismatch", "sort '\$.a' null on empty", "append '\$.a' = 1 remove on null"
        )
    }

    @Test
    fun matchesHandlersInAnyOrderAndRepeated() {
        matches(
            "set '\$.a' = 1 error on null ignore on missing", "set '\$.a' = 1 ignore on error ignore on missing",
            "set '\$.a' = 1 error on error null on empty", "set '\$.a' = 1 ignore on missing ignore on missing",
            "set '\$.a' = 1 ignore on missing error on missing", "append '\$.a' = 1 error on mismatch ignore on missing",
            "append '\$.a' = 1 ignore on missing error on mismatch null on null error on empty",
            "rename '\$.a' = 'b' error on missing error on missing"
        )
    }

    @Test
    fun rejectsMalformedHandlers() {
        notMatches(
            "set '\$.a' = 1 on missing", "set '\$.a' = 1 ignore on", "set '\$.a' = 1 ignore missing", "set '\$.a' = 1 ignore on bogus",
            "set '\$.a' = 1 bogus on missing", "set '\$.a' = 1 ignore, error on missing", "keep '\$.a' error on missing, '\$.b'",
            "remove '\$.a' = 2", "add set '\$.a' = 1"
        )
    }

    @Test
    fun matchesDocumentedHandlerExamples() {
        matches("rename '\$.a' = 'b' error on missing", "append '\$.a' = 'cat' create on mismatch")
        assertThat(p).matches(
            "json_transform('{\"x\":null}', rename '\$.a' = 'b' error on missing)")
        assertThat(p).matches(
            "json_transform('{\"a\":\"dog\"}', append '\$.a' = 'cat' create on mismatch)")
    }

    @Test
    fun buildsOperationNodes() {
        val tree = p.parse("json_transform(d, rename '\$.a' = 'b' error on missing, append '\$.c' = 1 create on mismatch)")
        val rename = tree.getFirstDescendant(SingleRowSqlFunctionsGrammar.JSON_RENAME_OPERATION)!!
        assertThatAst(rename.children.map { it.tokenOriginalValue.lowercase() }).containsExactly(
            "rename", "'\$.a'", "=", "'b'", "error", "on", "missing")
        val append = tree.getFirstDescendant(SingleRowSqlFunctionsGrammar.JSON_APPEND_OPERATION)!!
        assertThatAst(append.children.map { it.tokenOriginalValue.lowercase() }).containsExactly(
            "append", "'\$.c'", "=", "1", "create", "on", "mismatch")
    }
}
