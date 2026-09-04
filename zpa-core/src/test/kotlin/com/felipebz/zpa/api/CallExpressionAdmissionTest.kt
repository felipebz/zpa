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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CallExpressionAdmissionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesCustomAndGenericCalls() {
        listOf(
            "custom_function()",
            "custom_function(1)",
            "custom_function(a, b, c)",
            "package.custom_function()",
            "package.custom_function(1, 'two')",
            "\"schema\".\"package\".custom_function(1)",
            "obj.method()",
            "obj.method(x => 1)"
        ).forEach { source ->
            assertThat(p).describedAs(source).matches(source)
            val node = p.parse(source)
            assertThatAst(node.getDescendants(PlSqlGrammar.METHOD_CALL))
                .describedAs(source)
                .isNotEmpty
            assertThatAst(node.getDescendants(SingleRowSqlFunctionsGrammar.SINGLE_ROW_SQL_FUNCTION))
                .describedAs(source)
                .isEmpty()
            assertThatAst(node.getDescendants(AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION))
                .describedAs(source)
                .isEmpty()
        }
    }

    @Test
    fun matchesStandardAggregateAndAnalyticFunctionsViaGenericCall() {
        listOf(
            "sum(salary)",
            "count(*)",
            "count(distinct emp_id)",
            "avg(points)",
            "min(created_at)",
            "max(val)"
        ).forEach { source ->
            assertThat(p).describedAs(source).matches(source)
            val node = p.parse(source)
            assertThatAst(node.getDescendants(PlSqlGrammar.METHOD_CALL))
                .describedAs(source)
                .isNotEmpty
            assertThatAst(node.getDescendants(SingleRowSqlFunctionsGrammar.SINGLE_ROW_SQL_FUNCTION))
                .describedAs(source)
                .isEmpty()
            assertThatAst(node.getDescendants(AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION))
                .describedAs(source)
                .isEmpty()
        }
    }

    @Test
    fun matchesSingleRowSqlFunctions() {
        val expressions = listOf(
            "to_date('2026-09-03', 'YYYY-MM-DD')" to SingleRowSqlFunctionsGrammar.TO_DATE_EXPRESSION,
            "to_number('123')" to SingleRowSqlFunctionsGrammar.TO_NUMBER_EXPRESSION,
            "to_char(sysdate)" to PlSqlGrammar.METHOD_CALL,
            "cast(x as varchar2(10))" to SingleRowSqlFunctionsGrammar.CAST_EXPRESSION,
            "trim('  hello  ')" to SingleRowSqlFunctionsGrammar.TRIM_EXPRESSION,
            "extract(year from hire_date)" to SingleRowSqlFunctionsGrammar.EXTRACT_DATETIME_EXPRESSION,
            "set(multiset_col)" to SingleRowSqlFunctionsGrammar.SET_EXPRESSION,
            "cursor(select 1 from dual)" to SingleRowSqlFunctionsGrammar.CURSOR_EXPRESSION
        )

        expressions.forEach { (source, expectedDescendant) ->
            assertThat(p).describedAs(source).matches(source)
            val node = p.parse(source)
            assertThatAst(node.getDescendants(expectedDescendant))
                .describedAs(source)
                .isNotEmpty
        }
    }

    @Test
    fun matchesTreatAsVariants() {
        assertThat(p).matches("treat(foo as bar)")
        val treatWithKeyword = p.parse("treat(foo as bar)")
        assertThatAst(treatWithKeyword.getDescendants(SingleRowSqlFunctionsGrammar.TREAT_AS_EXPRESSION))
            .hasSize(1)

        assertThat(p).matches("(foo as bar)")
        val treatWithoutKeyword = p.parse("(foo as bar)")
        assertThatAst(treatWithoutKeyword.getDescendants(SingleRowSqlFunctionsGrammar.TREAT_AS_EXPRESSION))
            .hasSize(1)
    }

    @Test
    fun matchesJsonExpressionsAndShorthands() {
        assertThat(p).matches("json_array(1, 2, 3)")
        val jsonArray = p.parse("json_array(1, 2, 3)")
        assertThatAst(jsonArray.getDescendants(SingleRowSqlFunctionsGrammar.JSON_ARRAY_EXPRESSION))
            .hasSize(1)

        assertThat(p).matches("[1, 2, 3]")
        val jsonBracketArray = p.parse("[1, 2, 3]")
        assertThatAst(jsonBracketArray.getDescendants(SingleRowSqlFunctionsGrammar.JSON_ARRAY_EXPRESSION))
            .hasSize(1)

        assertThat(p).matches("json_object('a' value 1)")
        val jsonObject = p.parse("json_object('a' value 1)")
        assertThatAst(jsonObject.getDescendants(SingleRowSqlFunctionsGrammar.JSON_OBJECT_EXPRESSION))
            .hasSize(1)

        assertThat(p).matches("{'a': 1}")
        val jsonBraceObject = p.parse("{'a': 1}")
        assertThatAst(jsonBraceObject.getDescendants(SingleRowSqlFunctionsGrammar.JSON_OBJECT_EXPRESSION))
            .hasSize(1)
    }

    @Test
    fun matchesAggregateSqlFunctions() {
        assertThat(p).matches("listagg(foo) within group (order by bar)")
        val listagg = p.parse("listagg(foo) within group (order by bar)")
        assertThatAst(listagg.getDescendants(AggregateSqlFunctionsGrammar.LISTAGG_EXPRESSION)).hasSize(1)
        assertThatAst(listagg.getDescendants(AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION)).hasSize(1)

        assertThat(p).matches("xmlagg(xmlelement(e, emp_name))")
        val xmlagg = p.parse("xmlagg(xmlelement(e, emp_name))")
        assertThatAst(xmlagg.getDescendants(AggregateSqlFunctionsGrammar.XMLAGG_EXPRESSION)).hasSize(1)
        assertThatAst(xmlagg.getDescendants(AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION)).hasSize(1)

        assertThat(p).matches("collect(foo)")
        val collect = p.parse("collect(foo)")
        assertThatAst(collect.getDescendants(AggregateSqlFunctionsGrammar.COLLECT_EXPRESSION)).hasSize(1)
        assertThatAst(collect.getDescendants(AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION)).hasSize(1)

        assertThat(p).matches("json_arrayagg(foo)")
        val jsonArrayagg = p.parse("json_arrayagg(foo)")
        assertThatAst(jsonArrayagg.getDescendants(AggregateSqlFunctionsGrammar.JSON_ARRAYAGG_EXPRESSION)).hasSize(1)
        assertThatAst(jsonArrayagg.getDescendants(AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION)).hasSize(1)

        assertThat(p).matches("json_objectagg(key foo value bar)")
        val jsonObjectagg = p.parse("json_objectagg(key foo value bar)")
        assertThatAst(jsonObjectagg.getDescendants(AggregateSqlFunctionsGrammar.JSON_OBJECTAGG_EXPRESSION)).hasSize(1)
        assertThatAst(jsonObjectagg.getDescendants(AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION)).hasSize(1)
    }

    @Test
    fun preservesAstPropertiesAndSpans() {
        val customCall = p.parse("custom_function(1, 'a')")
        assertThatAst(customCall.tokens.joinToString(" ") { it.originalValue })
            .isEqualTo("custom_function ( 1 , 'a' )")

        val toDate = p.parse("to_date('2026-09-03')")
        val toDateNode = toDate.getFirstDescendant(SingleRowSqlFunctionsGrammar.TO_DATE_EXPRESSION)
        assertThatAst(toDateNode.tokens.map { it.originalValue }).containsExactly("to_date", "(", "'2026-09-03'", ")")

        val sumCall = p.parse("sum(salary)")
        val methodCallNode = sumCall.getFirstDescendant(PlSqlGrammar.METHOD_CALL)
        assertThatAst(methodCallNode.tokens.map { it.originalValue }).containsExactly("sum", "(", "salary", ")")
    }

    @Test
    fun preservesRejectionOfMalformedCalls() {
        listOf(
            "custom_function(",
            "custom_function(1,",
            "to_date(",
            "to_date(1,",
            "listagg(",
            "listagg(foo",
            "treat(",
            "treat(foo as",
            "json_array(",
            "[1, 2,"
        ).forEach { source ->
            assertThat(p).describedAs(source).notMatches(source)
        }
    }

    @Test
    fun keepsOrdinaryMemberExpressionsUnchanged() {
        listOf(
            "a.b",
            "a.b.c",
            "table_name.column_name",
            "\"schema\".\"table\".\"column\""
        ).forEach { source ->
            assertThat(p).describedAs(source).matches(source)
            val node = p.parse(source)
            assertThatAst(node.getDescendants(PlSqlGrammar.METHOD_CALL)).isEmpty()
            assertThatAst(node.getDescendants(SingleRowSqlFunctionsGrammar.SINGLE_ROW_SQL_FUNCTION)).isEmpty()
            assertThatAst(node.getDescendants(AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION)).isEmpty()
        }
    }

    @Test
    fun matchesKeywordLikeAndQuotedCustomCalls() {
        listOf(
            "coalesce(1, 2)",
            "nullif(a, b)",
            "decode(x, 1, 'a', 'b')",
            "\"custom_function\"(1)",
            "\"to_date\"('2026-09-03')",
            "\"sum\"(1)"
        ).forEach { source ->
            assertThat(p).describedAs(source).matches(source)
            val node = p.parse(source)
            assertThatAst(node.getDescendants(PlSqlGrammar.METHOD_CALL))
                .describedAs(source)
                .isNotEmpty
            assertThatAst(node.getDescendants(SingleRowSqlFunctionsGrammar.SINGLE_ROW_SQL_FUNCTION))
                .describedAs(source)
                .isEmpty()
            assertThatAst(node.getDescendants(AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION))
                .describedAs(source)
                .isEmpty()
        }
    }

    @Test
    fun matchesAnalyticFunctions() {
        listOf(
            "sum(salary) over ()",
            "sum(salary) over (partition by dept_id order by hire_date)",
            "row_number() over ()",
            "count(*) over ()"
        ).forEach { source ->
            assertThat(p).describedAs(source).matches(source)
            val node = p.parse(source)
            assertThatAst(node.getDescendants(PlSqlGrammar.METHOD_CALL))
                .describedAs(source)
                .isNotEmpty
            assertThatAst(node.getDescendants(PlSqlGrammar.POSTFIX_EXPRESSION))
                .describedAs(source)
                .isNotEmpty
        }

        val listaggAnalytic = "listagg(foo, ',') within group (order by bar) over (partition by dept_id)"
        assertThat(p).matches(listaggAnalytic)
        val listaggNode = p.parse(listaggAnalytic)
        assertThatAst(listaggNode.getDescendants(AggregateSqlFunctionsGrammar.LISTAGG_EXPRESSION)).hasSize(1)
        assertThatAst(listaggNode.getDescendants(PlSqlGrammar.POSTFIX_EXPRESSION)).isNotEmpty
    }

    @Test
    fun preservesModelAndJsonObjectAccessInteractions() {
        // JSON object access on tables/records
        listOf(
            "t.data[0]",
            "t.data.a[0]",
            "t.data.a[*]",
            "t.data.a[*].sum()"
        ).forEach { source ->
            assertThat(p).describedAs(source).matches(source)
            val node = p.parse(source)
            assertThatAst(node.getDescendants(SingleRowSqlFunctionsGrammar.JSON_OBJECT_ACCESS_EXPRESSION))
                .describedAs(source)
                .isNotEmpty
            assertThatAst(node.getDescendants(SingleRowSqlFunctionsGrammar.SINGLE_ROW_SQL_FUNCTION))
                .describedAs(source)
                .isEmpty()
            assertThatAst(node.getDescendants(AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION))
                .describedAs(source)
                .isEmpty()
        }

        // MODEL clause interaction
        setRootRule(DmlGrammar.SELECT_EXPRESSION)
        val modelQuery = """
            select val from tab
            model
              dimension by (x)
              measures (val)
              rules (
                val[1] = to_date('2026-09-03', 'YYYY-MM-DD'),
                val[2] = ref.measure[0],
                val[3] = sum(val)[1]
              )
        """.trimIndent()
        assertThat(p).matches(modelQuery)
        val modelNode = p.parse(modelQuery)
        assertThatAst(modelNode.getDescendants(SingleRowSqlFunctionsGrammar.TO_DATE_EXPRESSION)).hasSize(1)
        assertThatAst(modelNode.getDescendants(DmlGrammar.MODEL_CELL_REFERENCE_SUFFIX)).isNotEmpty
    }

    @Test
    fun computesCorpusAstHash() {
        val corpusDir = java.io.File("../../perf-corpus").canonicalFile
        if (!corpusDir.exists()) return
        val files = corpusDir.listFiles { f -> f.extension.equals("sql", ignoreCase = true) }?.sortedBy { it.name } ?: return
        val conf = com.felipebz.zpa.squid.PlSqlConfiguration(java.nio.charset.StandardCharsets.UTF_8)
        val parser = com.felipebz.zpa.parser.PlSqlParser.create(conf)
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        for (file in files) {
            val root = parser.parse(file)
            val xml = com.felipebz.flr.impl.ast.AstXmlPrinter.print(root)
            digest.update(xml.toByteArray(java.nio.charset.StandardCharsets.UTF_8))
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        assertThatAst(hash).isEqualTo("e223d288b68ebdc9b3a49aca088bab4961d0c57894bdde313c4d2bc3b8e19ec1")
    }
}


