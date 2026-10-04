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
package com.felipebz.zpa.api

import com.felipebz.flr.tests.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.Test

class VectorSyntaxTest : RuleTest() {

    @Test
    fun matchesVectorDatatypes() {
        setRootRule(PlSqlGrammar.DATATYPE)
        listOf(
            "vector", "vector(100)", "vector(*)", "vector(*, *)", "vector(100, int8)", "vector(*, int8)", "vector(100, *)",
            "vector(1024, binary)", "vector(100, float32, dense)", "vector(100, float32, sparse)", "vector(100, float64)",
            "vector(*, *, sparse)", "vector(*, *, dense)", "vector(100, *, sparse)", "vector(*, float32, sparse)",
            "VECTOR(100, FLOAT32, DENSE)", "vector (100, int8)",
            "vector(100, sparse)", "vector(sparse)", "vector(100, float16)", "vector(*, *, *)",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun rejectsMalformedVectorDatatypes() {
        setRootRule(PlSqlGrammar.DATATYPE)
        listOf(
            "vector()", "vector(100,)", "vector(100, float32,)", "vector(100, float32, dense, x)",
            "vector(100, float32, sparse, sparse)", "vector(100 + 1)", "vector(:n)", "vector(100, 'float32')",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun matchesVectorColumnsAndDeclarations() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        listOf(
            "create table t (v1 vector, v2 vector(100), v3 vector(*, int8), v6 vector(100, float32, dense), v7 vector(100, float32, sparse));",
            "declare vs1 vector(*, *, sparse) := vector('[10, [0, 3], [1.9, 4]]', *, *, sparse); begin null; end;",
            "declare vec1 pls_vec_tab.v1%type; vec0 vector; begin null; end;",
            "begin select v1 <=> v2 into dist from dual; end;",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun matchesVectorConstructors() {
        setRootRule(PlSqlGrammar.EXPRESSION)
        listOf(
            "vector('[1,2]')", "vector('[1,2]', 2)", "vector('[1,2]', *)", "vector('[1,2]', 2, float32)", "vector('[1,2]', *, *)",
            "vector('[1,2]', *, *, dense)", "vector('[1,2]', *, *, sparse)", "vector('[1,2]', 2, float32, dense)",
            "vector('[1,2]', 2, *)", "vector('[1,2]', *, float32)", "vector(:v, *, *)",
            "to_vector('[1,2]')", "to_vector('[1,2]', 2, float32)", "to_vector('[1,2]', *, *, sparse)", "to_vector('[1,2]', *, *, *)",
            "vector('[1,2]', dense)", "vector('[1,2]', int8, 2)",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "vector()", "to_vector()", "vector(*)", "to_vector(*)", "vector('[1]', 1, float32, dense, x)",
            "to_vector('[1]', 1, float32, dense, x)", "vector('[1]', 1, float32, dense, dense)", "vector('[1]', 1, float32, dense,)",
            "vector(, 1)",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
        listOf("pkg.vector('[1]', 1, 2, 3, 4)", "pkg.vector()", "vector('[1]', 1, 2, 3, 4)", "\"VECTOR\"('[1]', 1, 2, 3, 4)", "vector.foo")
            .forEach { assertThat(p).describedAs(it).matches(it) }
        assertThatAst(p.parse("to_vector('[1]', 1, float32)").getFirstDescendant(SingleRowSqlFunctionsGrammar.VECTOR_CONSTRUCTOR_EXPRESSION)
            .tokens.map { it.originalValue }).containsExactly("to_vector", "(", "'[1]'", ",", "1", ",", "float32", ")")
    }

    @Test
    fun matchesVectorDistanceOperators() {
        setRootRule(PlSqlGrammar.EXPRESSION)
        listOf(
            "a <=> b", "a <-> b", "a <#> b", "a<=>b", "a<->b", "a<#>b", "(a <=> b)", "a <=> (b)", "a <=> b <=> c", "a <-> b <-> c",
            "a <=> b + 1", "a + b <=> c * 2", "a <=> b || 'x'", "a <=> b = 1", "a <=> b < 0.5", "a <=> b between 0 and 1",
            "a <=> b is not null", "vector('[1]') <=> vector('[2]')", "-a <=> b", "fn(a <#> b)",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "a < = > b", "a < - > b", "a <=>", "<=> a", "a <=> <=> b", "a <> <=> b",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).matches("select * from t order by v <=> :q fetch first 5 rows only;")
        assertThat(p).matches("select a <=> b from t where a <-> b < 1;")
    }

    @Test
    fun bindsDistanceLooserThanConcatenation() {
        setRootRule(PlSqlGrammar.EXPRESSION)
        assertThat(p).matches("v <-> '[' || '1,2,3' || ']'")
        val concat = p.parse("v <-> '[' || '1,2,3' || ']'").getFirstDescendant(PlSqlGrammar.VECTOR_DISTANCE_EXPRESSION)
        assertThatAst(concat.children.map { it.type }).containsExactly(
            PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.VECTOR_DISTANCE_OPERATOR, PlSqlGrammar.CONCATENATION_EXPRESSION)
        assertThatAst(concat.getFirstChild(PlSqlGrammar.CONCATENATION_EXPRESSION).tokens.map { it.originalValue })
            .containsExactly("'['", "|", "|", "'1,2,3'", "|", "|", "']'")
        val leading = p.parse("a || b <=> c + 1 * 2").getFirstDescendant(PlSqlGrammar.VECTOR_DISTANCE_EXPRESSION)
        assertThatAst(leading.children.map { it.type }).containsExactly(
            PlSqlGrammar.CONCATENATION_EXPRESSION, PlSqlGrammar.VECTOR_DISTANCE_OPERATOR, PlSqlGrammar.ADDITIVE_EXPRESSION)
        val parenthesized = p.parse("(v <-> '[') || '1,2,3' || ']'")
        assertThatAst(parenthesized.getFirstDescendant(PlSqlGrammar.CONCATENATION_EXPRESSION)
            .getFirstDescendant(PlSqlGrammar.VECTOR_DISTANCE_EXPRESSION)).isNotNull()
        val chain = p.parse("a <=> b <-> c").getFirstDescendant(PlSqlGrammar.VECTOR_DISTANCE_EXPRESSION)
        assertThatAst(chain.getChildren(PlSqlGrammar.VECTOR_DISTANCE_OPERATOR).map { it.tokenOriginalValue }).containsExactly("<=>", "<->")
        assertThatAst(p.parse("a + b").getDescendants(PlSqlGrammar.VECTOR_DISTANCE_EXPRESSION)).isEmpty()
        val datatype = p.parse("cast(x as vector(*, *, sparse))").getFirstDescendant(PlSqlGrammar.VECTOR_DATATYPE)
        assertThatAst(datatype.tokens.map { it.originalValue }).containsExactly("vector", "(", "*", ",", "*", ",", "sparse", ")")
    }
}
