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
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

/** Oracle 26 extended simple CASE: comma-separated choices and dangling predicates after WHEN. */
class ExtendedCaseTest : RuleTest() {

    private fun expression(vararg whenClauses: String) = "case x ${whenClauses.joinToString(" ")} end"

    private fun statement(vararg whenClauses: String) =
        "case x ${whenClauses.joinToString(" ")} end case;"

    private fun matchesBoth(choice: String) {
        setRootRule(PlSqlGrammar.CASE_EXPRESSION)
        assertThat(p).describedAs("expression: $choice").matches(expression("when $choice then 1"))
        setRootRule(PlSqlGrammar.CASE_STATEMENT)
        assertThat(p).describedAs("statement: $choice").matches(statement("when $choice then foo := 1;"))
    }

    private fun rejectsBoth(choice: String) {
        setRootRule(PlSqlGrammar.CASE_EXPRESSION)
        assertThat(p).describedAs("expression: $choice").notMatches(expression("when $choice then 1"))
        setRootRule(PlSqlGrammar.CASE_STATEMENT)
        assertThat(p).describedAs("statement: $choice").notMatches(statement("when $choice then foo := 1;"))
    }

    @Test
    fun matchesCommaSeparatedSelectorValues() {
        for (choice in listOf("1, 2", "1, 2, 3", "'a', 'b'", "foo, bar.baz, f(1)", "1 + 1, x * 2", "null, 1")) {
            matchesBoth(choice)
        }
    }

    @Test
    fun matchesRelationalDanglingPredicates() {
        for (choice in listOf("< 0", "> 50", "<= 5", ">= 5", "= 5", "<> 5", "!= 5", "^= 5", "~= 5", "< 1 + 2 * x",
            "> 'a' || 'b'", "< - x", "< (1 + 2)", "< case when y then 1 end", "= null", "< x")) {
            matchesBoth(choice)
        }
    }

    @Test
    fun matchesDanglingPredicateFamilies() {
        for (choice in listOf(
            "between 10 and 30", "not between 10 and 30", "between 1 + 1 and x * 2",
            "in (1, 2)", "not in (1, 2)", "in ((1), (2))", "in (1 + 1, x)",
            "like 'a%'", "not like 'a%'", "like 'a%' escape '\\'", "like 'a' || 'b' escape 'c'",
            "is null", "is not null", "is nan", "is not nan", "is infinite", "is not infinite",
            "is a set", "is not a set", "is empty", "is not empty",
            "member of t", "not member of t", "member t", "submultiset of t", "not submultiset t")) {
            matchesBoth(choice)
        }
    }

    @Test
    fun matchesMixedChoices() {
        for (choice in listOf("< 0, > 100", "1, < 5, > 7", "< 5, 7", "between 1 and 3, 5, is null",
            "is null, is not null", "between 1 and 3, 4", "1, 2, in (3, 4), like 'a%', not between 5 and 6")) {
            matchesBoth(choice)
        }
    }

    @Test
    fun matchesDocumentedExamples() {
        setRootRule(PlSqlGrammar.CASE_STATEMENT)
        assertThat(p).matches(
            "case grade when < 0, > 100 then dbms_output.put_line('No such grade');" +
                " when > 89 then dbms_output.put_line('A'); else dbms_output.put_line('F'); end case;")
        setRootRule(PlSqlGrammar.CASE_EXPRESSION)
        assertThat(p).matches("case salary when 1000, 2000 then 'low' when 3000, 4000, 5000 then 'normal' else 'x' end")
        assertThat(p).matches(
            "case data_val/2 when < 0, > 50 then 'outlier' when between 10 and 30 then 'good' else 'bad' end")
    }

    @Test
    fun matchesChoicesInSeveralWhenClausesAndNestedCase() {
        setRootRule(PlSqlGrammar.CASE_EXPRESSION)
        assertThat(p).matches(expression("when < 0, 1 then 'a'", "when between 2 and 3 then 'b'", "else 'c'"))
        assertThat(p).matches("case x when < 0 then case y when > 1, < -1 then 1 end else 2 end")
        assertThat(p).matches("case x when 1 then 'a' else case when y then 'b' end end")
    }

    @Test
    fun keepsOrdinaryChoicesAsPlainExpressionsAndDanglingPredicatesAsNodes() {
        setRootRule(PlSqlGrammar.CASE_EXPRESSION)
        val tree = p.parse("case x when 1, < 0, not x, between 2 and 3 then 1 end")
        assertThatAst(tree.getDescendants(ConditionsGrammar.DANGLING_PREDICATE)).hasSize(2)
        val choices = tree.children.filter { it.type !== PlSqlKeyword.CASE && it.type !== PlSqlKeyword.END }
        assertThatAst(choices.map { it.type }).containsExactly(
            PlSqlGrammar.VARIABLE_NAME, PlSqlKeyword.WHEN, PlSqlGrammar.LITERAL, PlSqlPunctuator.COMMA,
            ConditionsGrammar.DANGLING_PREDICATE, PlSqlPunctuator.COMMA, PlSqlGrammar.NOT_EXPRESSION,
            PlSqlPunctuator.COMMA, ConditionsGrammar.DANGLING_PREDICATE, PlSqlKeyword.THEN, PlSqlGrammar.LITERAL)

        setRootRule(PlSqlGrammar.CASE_STATEMENT)
        val statement = p.parse("case x when < 0, 1 then foo := 1; end case;")
        assertThatAst(statement.getDescendants(ConditionsGrammar.DANGLING_PREDICATE)).hasSize(1)
    }

    @Test
    fun rejectsUnsupportedDanglingPredicates() {
        for (choice in listOf(
            "is json", "is not json", "is json strict", "is of (number)", "is not of (number)", "is of type (only t)",
            "is true", "is false", "is not true", "is present", "is not a", "is",
            "< 5 and > 2", "< 5 or > 7", "not < 5", "(< 5)", "(1), (< 5)",
            "< 5 < 6", "= = 5", "< < 5", "< not 1", "<",
            "< 5 is null", "< 5 between 1 and 2", "< 5 in (1)", "< 5 like 'a'", "< 1 = 1",
            "in (select 1 from dual)",
            "in ()", "in (1,)", "in 1", "between 1", "between 1 and", "between 1 and 2 and 3", "like", "like 'a' escape",
            "1,", ", 1", "< 1,", ",", "|| 'a'", "* 1", "regexp_like 'a'")) {
            rejectsBoth(choice)
        }
    }

    @Test
    fun rejectsTrailingAndMissingChoicesPerWhen() {
        setRootRule(PlSqlGrammar.CASE_EXPRESSION)
        assertThat(p).notMatches("case x when 1, then 1 end")
        assertThat(p).notMatches("case x when then 1 end")
        assertThat(p).notMatches("case x when 1, 2 1 end")
        assertThat(p).notMatches("case x when 1 then 1, 2 end")
        setRootRule(PlSqlGrammar.CASE_STATEMENT)
        assertThat(p).notMatches("case x when 1, 2 then foo := 1, bar := 2; end case;")
    }

    @Test
    fun keepsSearchedCaseStrict() {
        setRootRule(PlSqlGrammar.CASE_EXPRESSION)
        for (condition in listOf("< 1", "> 5", "is null", "is not null", "between 1 and 2", "in (1, 2)", "like 'a'",
            "x = 1, x = 2", "1, 2", "x < 1, x > 5", "x = 1, < 2")) {
            assertThat(p).describedAs("expression: $condition").notMatches("case when $condition then 1 end")
        }
        assertThat(p).matches("case when x < 1 then 1 when x between 1 and 2 then 2 end")
        assertThat(p).matches("case when x is null or y in (1, 2) then 1 end")

        setRootRule(PlSqlGrammar.CASE_STATEMENT)
        for (condition in listOf("< 1", "is null", "between 1 and 2", "x = 1, x = 2", "1, 2")) {
            assertThat(p).describedAs("statement: $condition")
                .notMatches("case when $condition then foo := 1; end case;")
        }
        assertThat(p).matches("case when x < 1 then foo := 1; when x between 1 and 2 then foo := 2; end case;")
    }

    @Test
    fun parsesInsideStatements() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).matches(
            "declare d number := 30; s varchar2(20); begin\n" +
                "  s := case d / 2 when < 0, > 50 then 'outlier' when between 10 and 30 then 'good' else 'bad' end;\n" +
                "  case d when < 0, > 100 then null; when 1, 2, > 89 then null; else null; end case;\n" +
                "end;\n/\n")
    }
}
