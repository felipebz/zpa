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
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import com.felipebz.zpa.api.SingleRowSqlFunctionsGrammar

class JsonTableTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesJsonWithValueColumn() {
        assertThat(p).matches("json_table(doc, '$' columns (c1 path '$.c1'))")
    }

    @Test
    fun matchesJsonValueColumnWithOnErrorBeforeOnEmpty() {
        assertThat(p).matches("json_table(doc, '$' columns (c1 path '$.c1' null on error null on empty))")
        assertThat(p).matches("json_table(doc, '$' columns (c1 path '$.c1' null on empty null on error))")
    }

    @Test
    fun doesNotMatchJsonValueColumnWithRepeatedOnErrorOrOnEmpty() {
        assertThat(p).notMatches("json_table(doc, '$' columns (c1 path '$.c1' null on error error on error))")
        assertThat(p).notMatches("json_table(doc, '$' columns (c1 path '$.c1' null on empty error on empty))")
    }

    @Test
    fun matchesJsonWithExistsColumn() {
        assertThat(p).matches("json_table(doc, '$' columns (c1 exists path '$.c1'))")
    }

    @Test
    fun matchesJsonWithQueryColumn() {
        assertThat(p).matches("json_table(doc, '$' columns (c1 format json path '$.c1'))")
    }

    @Test
    fun matchesJsonWithNestedColumn() {
        assertThat(p).matches("json_table(doc, '$' columns (nested '$.c1' columns (c2 path '$.c2')))")
    }

    @Test
    fun matchesJsonWithOrdinalityColumn() {
        assertThat(p).matches("json_table(doc, '$' columns (c1 for ordinality))")
    }

    @Test
    fun matchesJsonWithColumnWithoutParenthesis() {
        assertThat(p).matches("json_table(doc, '$' columns c1 for ordinality)")
    }

    @Test
    fun matchesLongJsonTable() {
        assertThat(p).matches("""
            json_table(doc, '$'
            error on error
            error on empty
            columns (
              c1 clob truncate path '$.c1' error on error error on empty error on mismatch(missing data),
              c1 varchar2(10) format json allow scalars without wrapper path '$.c1' error on error,
              c1 number exists path '$.c1' error on error error on empty,
              nested path subDoc[*] columns (c2 path '$.c2'),
              c1 for ordinality
            ))
            """)
    }


    @Test
    fun matchesJsonQueryColumnWithBothOnErrorAndOnEmpty() {
        assertThat(p).matches("json_table(doc, '$' columns (c1 clob format json path '$.c1' null on error null on empty))")
        assertThat(p).matches("json_table(doc, '$' columns (c1 clob format json path '$.c1' null on empty null on error))")
    }

    @Test
    fun doesNotMatchJsonQueryColumnWithRepeatedOnError() {
        assertThat(p).notMatches("json_table(doc, '$' columns (c1 clob format json path '$.c1' null on error error on error))")
    }

    private fun column(definition: String) = "json_table(d, '$' columns ($definition))"

    @Test
    fun matchesNativeJsonColumns() {
        listOf(
            "a json path '$.a'", "a json", "a json path '$.a' type (strict)", "a json path '$.a' type (lax)",
            "a json(object) path '$.a'", "a json path '$.a' null on error", "a json path '$.a' error on mismatch",
            "a json path '$.a' type (strict) null on error null on empty", "a json path '$.a', b number path '$.b', c json",
            "nested path '$.n[*]' columns (c json path '$.c' type (lax))", "a json, b number",
            "a json with wrapper path '$.a'", "a json without wrapper path '$.a'", "a json with array wrapper path '$.a'",
            "a json allow scalars path '$.a'", "a json disallow scalars path '$.a'",
            "a json allow scalars with wrapper path '$.a' null on error", "a json with wrapper", "a json with wrapper path '$.a' type (strict)",
        ).forEach { assertThat(p).describedAs(it).matches(column(it)) }
    }

    @Test
    fun matchesNativeJsonColumnsOracleRejectsLater() {
        listOf(
            "a json truncate path '$.a'", "a json truncate with wrapper path '$.a'",
            "a json format json path '$.a'", "a json format json with wrapper path '$.a'",
            "a json exists path '$.a'", "a json exists", "a json(object) exists path '$.a'", "a number format json path '$.a'",
        ).forEach { assertThat(p).describedAs(it).matches(column(it)) }
        assertThat(p).matches("json_value(d, '$.a' returning json)")
        assertThat(p).matches("json_value(d, '$.a' returning json(object) type (strict))")
        assertThat(p).matches("json_value(d, '$.a' returning json truncate)")
    }

    @Test
    fun matchesUnsupportedReturnDatatypesOracleRejectsLater() {
        listOf("definitely_not_a_type", "schema_name.definitely_not_a_type", "blob", "bfile", "nclob", "xmltype", "schema_name.xmltype", "raw")
            .forEach {
                assertThat(p).describedAs(it).matches("json_value(d, '$.a' returning $it)")
                assertThat(p).describedAs(it).matches(column("a $it path '$.a'"))
                assertThat(p).describedAs(it).matches(column("a $it exists path '$.a'"))
            }
    }

    @Test
    fun rejectsMalformedReturnDatatypes() {
        listOf(".", "schema_name.", "foo(", "()").forEach {
            assertThat(p).describedAs(it).notMatches("json_value(d, '$.a' returning $it)")
            assertThat(p).describedAs(it).notMatches(column("a $it path '$.a'"))
        }
    }

    @Test
    fun buildsCustomDatatypeReturnTypeAsObjectInstance() {
        val node = p.parse(column("a schema_name.t path '$.a'")).getFirstDescendant(SingleRowSqlFunctionsGrammar.JSON_VALUE_COLUMN)!!
        val instance = node.getFirstChild(SingleRowSqlFunctionsGrammar.JSON_VALUE_RETURN_TYPE)
            .getFirstChild(SingleRowSqlFunctionsGrammar.JSON_VALUE_RETURN_OBJECT_INSTANCE)
        assertThatAst(instance.getFirstChild(PlSqlGrammar.CUSTOM_DATATYPE).tokens.map { it.originalValue }).containsExactly("schema_name", ".", "t")
    }

    @Test
    fun rejectsMalformedNativeJsonColumns() {
        listOf(
            "a json type (strict)", "a json path '$.a' null on error type (strict)", "a json exists foo '$.a'",
            "a json format path '$.a'", "a json for ordinality", "a json path '$.a' path '$.b'", "a json path '$.a' with wrapper",
            "a json keep quotes path '$.a'", "nested json path '$.n' columns (c number path '$.c')",
        ).forEach { assertThat(p).describedAs(it).notMatches(column(it)) }
    }

    @Test
    fun keepsOrdinaryColumnTypesUnchanged() {
        assertThat(p).matches("json_query(d, '$.a' returning json)")
        assertThat(p).matches(column("a number path '$.a' type (strict)"))
        assertThat(p).notMatches(column("a number type (strict)"))
    }

    @Test
    fun buildsNativeJsonColumnWithoutWrapperNodes() {
        val value = p.parse(column("a json path '$.a' type (strict)")).getFirstDescendant(SingleRowSqlFunctionsGrammar.JSON_VALUE_COLUMN)!!
        assertThatAst(value.children.map { it.name }).containsExactly(
            "IDENTIFIER_NAME", "JSON_VALUE_RETURN_TYPE", "PATH", "JSON_PATH", "JSON_TYPE_CLAUSE")
        assertThatAst(value.getFirstDescendant(PlSqlGrammar.JSON_DATATYPE)!!.tokens.map { it.originalValue }).containsExactly("json")
        val query = p.parse(column("a json with wrapper path '$.a'")).getFirstDescendant(SingleRowSqlFunctionsGrammar.JSON_QUERY_COLUMN)!!
        assertThatAst(query.children.map { it.name }).containsExactly(
            "IDENTIFIER_NAME", "JSON_VALUE_RETURN_TYPE", "JSON_QUERY_WRAPPER_CLAUSE", "PATH", "JSON_PATH")
    }
}
