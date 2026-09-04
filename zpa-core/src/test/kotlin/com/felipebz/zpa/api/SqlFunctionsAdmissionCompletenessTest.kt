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

import com.felipebz.flr.api.Grammar
import com.felipebz.flr.api.TokenType
import com.felipebz.flr.grammar.GrammarRuleKey
import com.felipebz.flr.impl.matcher.RuleDefinition
import com.felipebz.flr.internal.vm.*
import com.felipebz.flr.internal.vm.lexerful.TokenTypeExpression
import com.felipebz.flr.internal.vm.lexerful.TokenTypesExpression
import com.felipebz.flr.tests.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SqlFunctionsAdmissionCompletenessTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun admissionTokensAreDerivedDirectlyFromAlternatives() {
        val singleRowDerived = SingleRowSqlFunctionsGrammar.ALTERNATIVES
            .flatMap { it.admissionTokens }
            .distinct()
        assertThatAst(SingleRowSqlFunctionsGrammar.admissionTokens.toList())
            .containsExactlyElementsOf(singleRowDerived)

        val aggregateDerived = AggregateSqlFunctionsGrammar.ALTERNATIVES
            .flatMap { it.admissionTokens }
            .distinct()
        assertThatAst(AggregateSqlFunctionsGrammar.admissionTokens.toList())
            .containsExactlyElementsOf(aggregateDerived)
    }

    @Test
    fun alternativesHaveValidNonEmptyTokensAndUniqueKeys() {
        SingleRowSqlFunctionsGrammar.ALTERNATIVES.forEach { alt ->
            assertThatAst(alt.admissionTokens)
                .describedAs("Alternative ${alt.ruleKey} has empty admission tokens")
                .isNotEmpty
        }
        val singleRowKeys = SingleRowSqlFunctionsGrammar.ALTERNATIVES.map { it.ruleKey }
        assertThatAst(singleRowKeys)
            .describedAs("Duplicate rule keys in SingleRowSqlFunctionsGrammar.ALTERNATIVES")
            .doesNotHaveDuplicates()

        AggregateSqlFunctionsGrammar.ALTERNATIVES.forEach { alt ->
            assertThatAst(alt.admissionTokens)
                .describedAs("Alternative ${alt.ruleKey} has empty admission tokens")
                .isNotEmpty
        }
        val aggregateKeys = AggregateSqlFunctionsGrammar.ALTERNATIVES.map { it.ruleKey }
        assertThatAst(aggregateKeys)
            .describedAs("Duplicate rule keys in AggregateSqlFunctionsGrammar.ALTERNATIVES")
            .doesNotHaveDuplicates()
    }

    @Test
    fun allFunctionRulesInGrammarEnumsAreRepresentedInAlternatives() {
        // In AggregateSqlFunctionsGrammar, all expression rules must be in ALTERNATIVES
        val expectedAggregateRules = AggregateSqlFunctionsGrammar.entries
            .filterNot { it == AggregateSqlFunctionsGrammar.FILTER_CLAUSE || it == AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION }
            .toSet()
        val actualAggregateRules = AggregateSqlFunctionsGrammar.ALTERNATIVES.map { it.ruleKey }.toSet()
        assertThatAst(actualAggregateRules)
            .describedAs("All function rules in AggregateSqlFunctionsGrammar must be in ALTERNATIVES")
            .isEqualTo(expectedAggregateRules)

        // In SingleRowSqlFunctionsGrammar, all function expressions must be in ALTERNATIVES.
        // The only expressions in SingleRowSqlFunctionsGrammar that are NOT standalone function alternatives
        // are internal clauses/paths or handled specifically elsewhere (e.g. JSON_OBJECT_ACCESS_EXPRESSION).
        val internalNonFunctionExpressions = setOf(
            SingleRowSqlFunctionsGrammar.JSON_BASIC_PATH_EXPRESSION,
            SingleRowSqlFunctionsGrammar.JSON_PATH_EXPRESSION,
            SingleRowSqlFunctionsGrammar.JSON_OBJECT_ACCESS_EXPRESSION,
            SingleRowSqlFunctionsGrammar.JSON_RHS_EXPRESSION,
        )
        val expectedSingleRowRules = SingleRowSqlFunctionsGrammar.entries
            .filter { it.name.endsWith("_EXPRESSION") || it.name.endsWith("_CONSTRUCTOR") }
            .filterNot { it in internalNonFunctionExpressions }
            .toSet()
        val actualSingleRowRules = SingleRowSqlFunctionsGrammar.ALTERNATIVES.map { it.ruleKey }.toSet()
        assertThatAst(actualSingleRowRules)
            .describedAs("All function rules in SingleRowSqlFunctionsGrammar must be in ALTERNATIVES")
            .isEqualTo(expectedSingleRowRules)
    }

    @Test
    fun liveGrammarSingleRowFunctionRuleMatchesAlternativesAndAdmissionSet() {
        val singleRowRule = p.grammar.rule(SingleRowSqlFunctionsGrammar.SINGLE_ROW_SQL_FUNCTION) as RuleDefinition
        val expr = singleRowRule.expression
        assertThatAst(expr).isInstanceOf(FirstOfExpression::class.java)

        val subExpressions = getSubExpressions(expr as FirstOfExpression)
        val actualRuleKeys = subExpressions.map { (it as RuleDefinition).ruleKey }

        // Live grammar alternatives match exactly the declared ALTERNATIVES
        val declaredRuleKeys = SingleRowSqlFunctionsGrammar.ALTERNATIVES.map { it.ruleKey }
        assertThatAst(actualRuleKeys)
            .describedAs("Live SINGLE_ROW_SQL_FUNCTION alternatives do not match declared ALTERNATIVES")
            .containsExactlyElementsOf(declaredRuleKeys)

        val admissionSet = SingleRowSqlFunctionsGrammar.admissionTokens.toSet()

        // Inspect every alternative in the live grammar:
        // 1. Its declared admission tokens in ALTERNATIVES must match the rule's starting tokens
        // 2. Its starting tokens must be in admissionTokens
        for (sub in subExpressions) {
            val ruleDef = sub as RuleDefinition
            val firstTokens = extractFirstTokens(p.grammar, ruleDef)
            assertThatAst(firstTokens)
                .describedAs("First tokens for rule ${ruleDef.ruleKey} could not be determined")
                .isNotEmpty

            val alt = SingleRowSqlFunctionsGrammar.ALTERNATIVES.first { it.ruleKey == ruleDef.ruleKey }
            assertThatAst(alt.admissionTokens)
                .describedAs("Alternative ${ruleDef.ruleKey} admission tokens do not match live grammar starting tokens")
                .containsExactlyInAnyOrderElementsOf(firstTokens)

            for (token in firstTokens) {
                assertThatAst(admissionSet)
                    .describedAs("First token $token of alternative ${ruleDef.ruleKey} is missing from admissionTokens")
                    .contains(token)
            }
        }
    }

    @Test
    fun liveGrammarAggregateFunctionRuleMatchesAlternativesAndAdmissionSet() {
        val aggregateRule = p.grammar.rule(AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION) as RuleDefinition
        val expr = aggregateRule.expression
        assertThatAst(expr).isInstanceOf(FirstOfExpression::class.java)

        val subExpressions = getSubExpressions(expr as FirstOfExpression)
        val actualRuleKeys = subExpressions.map { (it as RuleDefinition).ruleKey }

        val declaredRuleKeys = AggregateSqlFunctionsGrammar.ALTERNATIVES.map { it.ruleKey }
        assertThatAst(actualRuleKeys)
            .describedAs("Live AGGREGATE_SQL_FUNCTION alternatives do not match declared ALTERNATIVES")
            .containsExactlyElementsOf(declaredRuleKeys)

        val admissionSet = AggregateSqlFunctionsGrammar.admissionTokens.toSet()

        for (sub in subExpressions) {
            val ruleDef = sub as RuleDefinition
            val firstTokens = extractFirstTokens(p.grammar, ruleDef)
            assertThatAst(firstTokens)
                .describedAs("First tokens for rule ${ruleDef.ruleKey} could not be determined")
                .isNotEmpty

            val alt = AggregateSqlFunctionsGrammar.ALTERNATIVES.first { it.ruleKey == ruleDef.ruleKey }
            assertThatAst(alt.admissionTokens)
                .describedAs("Alternative ${ruleDef.ruleKey} admission tokens do not match live grammar starting tokens")
                .containsExactlyInAnyOrderElementsOf(firstTokens)

            for (token in firstTokens) {
                assertThatAst(admissionSet)
                    .describedAs("First token $token of alternative ${ruleDef.ruleKey} is missing from admissionTokens")
                    .contains(token)
            }
        }
    }

    @Test
    fun parsesEverySingleRowFunctionAlternativeAndEveryAdmissionTokenThroughCallExpression() {
        data class SingleRowTestCase(
            val ruleKey: GrammarRuleKey,
            val token: TokenType,
            val snippet: String
        )

        val testCases = listOf(
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.EXTRACT_DATETIME_EXPRESSION, PlSqlKeyword.EXTRACT, "extract(year from dt)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.JSON_CONSTRUCTOR, PlSqlKeyword.JSON, "json('{}')"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.JSON_ARRAY_EXPRESSION, PlSqlKeyword.JSON_ARRAY, "json_array(1, 2)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.JSON_ARRAY_EXPRESSION, PlSqlKeyword.JSON, "json[1, 2]"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.JSON_ARRAY_EXPRESSION, PlSqlPunctuator.LBRACKET, "[1, 2]"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.JSON_DATAGUIDE_EXPRESSION, PlSqlKeyword.JSON_DATAGUIDE, "json_dataguide(col)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.JSON_MERGEPATCH_EXPRESSION, PlSqlKeyword.JSON_MERGEPATCH, "json_mergepatch(doc1, doc2)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.JSON_OBJECT_EXPRESSION, PlSqlKeyword.JSON_OBJECT, "json_object('k' value 'v')"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.JSON_OBJECT_EXPRESSION, PlSqlKeyword.JSON, "json{'k': 'v'}"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.JSON_OBJECT_EXPRESSION, PlSqlPunctuator.LBRACE, "{'k': 'v'}"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.JSON_SCALAR_EXPRESSION, PlSqlKeyword.JSON_SCALAR, "json_scalar(1)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.JSON_SERIALIZE_EXPRESSION, PlSqlKeyword.JSON_SERIALIZE, "json_serialize(data)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.JSON_QUERY_EXPRESSION, PlSqlKeyword.JSON_QUERY, "json_query(data, '$.a')"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.JSON_TABLE_EXPRESSION, PlSqlKeyword.JSON_TABLE, "json_table(data, '$' columns (id number path '$.id'))"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.JSON_TRANSFORM_EXPRESSION, PlSqlKeyword.JSON_TRANSFORM, "json_transform(data, set '$.a' = 1)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.JSON_VALUE_EXPRESSION, PlSqlKeyword.JSON_VALUE, "json_value(data, '$.a')"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.XMLATTRIBUTES_EXPRESSION, PlSqlKeyword.XMLATTRIBUTES, "xmlattributes(1 as a)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.XMLCAST_EXPRESSION, PlSqlKeyword.XMLCAST, "xmlcast(x as varchar2(10))"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.XMLCDATA_EXPRESSION, PlSqlKeyword.XMLCDATA, "xmlcdata('hello')"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.XMLCOLATTVAL_EXPRESSION, PlSqlKeyword.XMLCOLATTVAL, "xmlcolattval(1, 2)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.XMLCOMMENT_EXPRESSION, PlSqlKeyword.XMLCOMMENT, "xmlcomment('hello')"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.XMLCONCAT_EXPRESSION, PlSqlKeyword.XMLCONCAT, "xmlconcat(x, y)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.XMLDIFF_EXPRESSION, PlSqlKeyword.XMLDIFF, "xmldiff(x, y)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.XMLELEMENT_EXPRESSION, PlSqlKeyword.XMLELEMENT, "xmlelement(e, 'hello')"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.XMLEXISTS_EXPRESSION, PlSqlKeyword.XMLEXISTS, "xmlexists('/a' passing data)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.XMLFOREST_EXPRESSION, PlSqlKeyword.XMLFOREST, "xmlforest(1 as a)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.XMLISVALID_EXPRESSION, PlSqlKeyword.XMLISVALID, "xmlisvalid(x)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.XMLPARSE_EXPRESSION, PlSqlKeyword.XMLPARSE, "xmlparse(document '<a></a>')"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.XMLPATCH_EXPRESSION, PlSqlKeyword.XMLPATCH, "xmlpatch(x, y)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.XMLPI_EXPRESSION, PlSqlKeyword.XMLPI, "xmlpi(name pi)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.XMLQUERY_EXPRESSION, PlSqlKeyword.XMLQUERY, "xmlquery('/a' passing data returning content)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.XMLROOT_EXPRESSION, PlSqlKeyword.XMLROOT, "xmlroot(x, version '1.0')"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.XMLSEQUENCE_EXPRESSION, PlSqlKeyword.XMLSEQUENCE, "xmlsequence(x)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.XMLSERIALIZE_EXPRESSION, PlSqlKeyword.XMLSERIALIZE, "xmlserialize(document x)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.XMLTABLE_EXPRESSION, PlSqlKeyword.XMLTABLE, "xmltable('/foo' passing bar)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.XMLTRANSFORM_EXPRESSION, PlSqlKeyword.XMLTRANSFORM, "xmltransform(x, y)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.TREAT_AS_EXPRESSION, PlSqlKeyword.TREAT, "treat(x as my_type)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.TREAT_AS_EXPRESSION, PlSqlPunctuator.LPARENTHESIS, "(x as my_type)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.SET_EXPRESSION, PlSqlKeyword.SET, "set(s)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.CAST_EXPRESSION, PlSqlKeyword.CAST, "cast(x as number)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.TO_BINARY_DOUBLE_EXPRESSION, PlSqlKeyword.TO_BINARY_DOUBLE, "to_binary_double('1.0')"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.TO_BINARY_FLOAT_EXPRESSION, PlSqlKeyword.TO_BINARY_FLOAT, "to_binary_float('1.0')"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.TO_BOOLEAN_EXPRESSION, PlSqlKeyword.TO_BOOLEAN, "to_boolean('true')"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.TO_DATE_EXPRESSION, PlSqlKeyword.TO_DATE, "to_date('2026-09-03')"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.TO_DSINTERVAL_EXPRESSION, PlSqlKeyword.TO_DSINTERVAL, "to_dsinterval('1 00:00:00')"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.TO_NUMBER_EXPRESSION, PlSqlKeyword.TO_NUMBER, "to_number('123')"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.TO_TIMESTAMP_EXPRESSION, PlSqlKeyword.TO_TIMESTAMP, "to_timestamp('2026-09-03 12:00:00')"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.TO_TIMESTAMP_TZ_EXPRESSION, PlSqlKeyword.TO_TIMESTAMP_TZ, "to_timestamp_tz('2026-09-03 12:00:00 +00:00')"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.TO_YMINTERVAL_EXPRESSION, PlSqlKeyword.TO_YMINTERVAL, "to_yminterval('01-02')"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.TRIM_EXPRESSION, PlSqlKeyword.TRIM, "trim(' abc ')"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.TABLE_EXPRESSION, PlSqlKeyword.TABLE, "table(my_collection)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.THE_EXPRESSION, PlSqlKeyword.THE, "the(select col from tab)"),
            SingleRowTestCase(SingleRowSqlFunctionsGrammar.CURSOR_EXPRESSION, PlSqlKeyword.CURSOR, "cursor(select 1 from dual)")
        )

        // Ensure every alternative and every token is represented in test cases
        val coveredPairs = testCases.map { it.ruleKey to it.token }.toSet()
        val allExpectedPairs = SingleRowSqlFunctionsGrammar.ALTERNATIVES
            .flatMap { alt -> alt.admissionTokens.map { alt.ruleKey to it } }
            .toSet()
        assertThatAst(coveredPairs)
            .describedAs("Some single row function alternatives or tokens are not covered by test cases")
            .isEqualTo(allExpectedPairs)

        // Parse each case through CALL_EXPRESSION and assert AST structure
        for ((ruleKey, token, snippet) in testCases) {
            assertThat(p).describedAs(snippet).matches(snippet)
            val node = p.parse(snippet)
            assertThatAst(node.getDescendants(SingleRowSqlFunctionsGrammar.SINGLE_ROW_SQL_FUNCTION))
                .describedAs("Parsed AST for '$snippet' does not contain SINGLE_ROW_SQL_FUNCTION")
                .isNotEmpty
            assertThatAst(node.getDescendants(ruleKey))
                .describedAs("Parsed AST for '$snippet' does not contain $ruleKey")
                .isNotEmpty
            assertThatAst(node.tokens.first().type)
                .describedAs("First token type for '$snippet' does not match expected admission token")
                .isEqualTo(token)
        }
    }

    @Test
    fun parsesEveryAggregateFunctionAlternativeAndEveryAdmissionTokenThroughCallExpression() {
        data class AggregateTestCase(
            val ruleKey: GrammarRuleKey,
            val token: TokenType,
            val snippet: String
        )

        val testCases = listOf(
            AggregateTestCase(AggregateSqlFunctionsGrammar.LISTAGG_EXPRESSION, PlSqlKeyword.LISTAGG, "listagg(x) within group (order by y)"),
            AggregateTestCase(AggregateSqlFunctionsGrammar.XMLAGG_EXPRESSION, PlSqlKeyword.XMLAGG, "xmlagg(x)"),
            AggregateTestCase(AggregateSqlFunctionsGrammar.COLLECT_EXPRESSION, PlSqlKeyword.COLLECT, "collect(x)"),
            AggregateTestCase(AggregateSqlFunctionsGrammar.JSON_ARRAYAGG_EXPRESSION, PlSqlKeyword.JSON_ARRAYAGG, "json_arrayagg(x)"),
            AggregateTestCase(AggregateSqlFunctionsGrammar.JSON_OBJECTAGG_EXPRESSION, PlSqlKeyword.JSON_OBJECTAGG, "json_objectagg(k value 'v')")
        )

        val coveredPairs = testCases.map { it.ruleKey to it.token }.toSet()
        val allExpectedPairs = AggregateSqlFunctionsGrammar.ALTERNATIVES
            .flatMap { alt -> alt.admissionTokens.map { alt.ruleKey to it } }
            .toSet()
        assertThatAst(coveredPairs)
            .describedAs("Some aggregate function alternatives or tokens are not covered by test cases")
            .isEqualTo(allExpectedPairs)

        for ((ruleKey, token, snippet) in testCases) {
            assertThat(p).describedAs(snippet).matches(snippet)
            val node = p.parse(snippet)
            assertThatAst(node.getDescendants(AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION))
                .describedAs("Parsed AST for '$snippet' does not contain AGGREGATE_SQL_FUNCTION")
                .isNotEmpty
            assertThatAst(node.getDescendants(ruleKey))
                .describedAs("Parsed AST for '$snippet' does not contain $ruleKey")
                .isNotEmpty
            assertThatAst(node.tokens.first().type)
                .describedAs("First token type for '$snippet' does not match expected admission token")
                .isEqualTo(token)
        }
    }

    // --- Helper functions for grammar introspection via reflection ---

    private fun getSubExpressions(expr: FirstOfExpression): Array<ParsingExpression> {
        val field = FirstOfExpression::class.java.getDeclaredField("subExpressions").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        return field.get(expr) as Array<ParsingExpression>
    }

    private fun extractFirstTokens(
        grammar: Grammar,
        expr: ParsingExpression,
        visitedRules: MutableSet<GrammarRuleKey> = mutableSetOf()
    ): Set<TokenType> {
        return when (expr) {
            is TokenTypeExpression -> {
                val field = TokenTypeExpression::class.java.getDeclaredField("type").apply { isAccessible = true }
                setOf(field.get(expr) as TokenType)
            }
            is TokenTypesExpression -> {
                val field = TokenTypesExpression::class.java.getDeclaredField("types").apply { isAccessible = true }
                @Suppress("UNCHECKED_CAST")
                (field.get(expr) as Set<TokenType>).toSet()
            }
            is FirstOfExpression -> {
                val subExprs = getSubExpressions(expr)
                subExprs.flatMap { extractFirstTokens(grammar, it, visitedRules) }.toSet()
            }
            is SequenceExpression -> {
                val field = SequenceExpression::class.java.getDeclaredField("subExpressions").apply { isAccessible = true }
                @Suppress("UNCHECKED_CAST")
                val subExprs = field.get(expr) as Array<ParsingExpression>
                val tokens = mutableSetOf<TokenType>()
                for (sub in subExprs) {
                    tokens.addAll(extractFirstTokens(grammar, sub, visitedRules))
                    if (!canBeEmpty(sub)) {
                        break
                    }
                }
                tokens
            }
            is OptionalExpression -> {
                val field = OptionalExpression::class.java.getDeclaredField("subExpression").apply { isAccessible = true }
                val sub = field.get(expr) as ParsingExpression
                extractFirstTokens(grammar, sub, visitedRules)
            }
            is ZeroOrMoreExpression -> {
                val field = ZeroOrMoreExpression::class.java.getDeclaredField("subExpression").apply { isAccessible = true }
                val sub = field.get(expr) as ParsingExpression
                extractFirstTokens(grammar, sub, visitedRules)
            }
            is OneOrMoreExpression -> {
                val field = OneOrMoreExpression::class.java.getDeclaredField("subExpression").apply { isAccessible = true }
                val sub = field.get(expr) as ParsingExpression
                extractFirstTokens(grammar, sub, visitedRules)
            }
            is RuleRefExpression -> {
                val key = expr.getRuleKey()
                if (key != null && visitedRules.add(key)) {
                    val rule = grammar.rule(key) as? CompilableGrammarRule
                    rule?.expression?.let { extractFirstTokens(grammar, it, visitedRules) } ?: emptySet()
                } else {
                    emptySet()
                }
            }
            is RuleDefinition -> {
                if (visitedRules.add(expr.ruleKey)) {
                    expr.expression?.let { extractFirstTokens(grammar, it, visitedRules) } ?: emptySet()
                } else {
                    emptySet()
                }
            }
            else -> {
                val subField = expr.javaClass.declaredFields.firstOrNull { it.name == "subExpression" }
                if (subField != null) {
                    subField.isAccessible = true
                    val sub = subField.get(expr) as ParsingExpression
                    extractFirstTokens(grammar, sub, visitedRules)
                } else {
                    emptySet()
                }
            }
        }
    }

    private fun canBeEmpty(expr: ParsingExpression): Boolean =
        expr is OptionalExpression ||
        expr is ZeroOrMoreExpression ||
        expr is NextExpression ||
        expr is NextNotExpression
}
