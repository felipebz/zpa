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
package com.felipebz.zpa.api.statements

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StackedLabelsTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.STATEMENT)
    }

    private fun wrap(statement: String) = "<<a>> <<b>> $statement"

    private val labelableStatements = listOf(
        "begin null; end;",
        "declare x number; begin null; end;",
        "x := 1;",
        "if x = 1 then null; end if;",
        "loop exit; end loop;",
        "for i in 1..2 loop null; end loop;",
        "while x < 0 loop null; end loop;",
        "forall i in 1..2 update t set x = i;",
        "case when x = 1 then null; else null; end case;",
        "exit;",
        "continue when x = 0;",
        "goto z;",
        "return;",
        "raise no_data_found;",
        "commit;",
        "rollback;",
        "savepoint s;",
        "null;",
        "select 1 into x from dual;",
        "insert into t values (1);",
        "update t set x = 1;",
        "delete from t;",
        "merge into t using dual on (1 = 1) when matched then update set x = 2;",
        "execute immediate 'begin null; end;';",
        "open c;",
        "open rc for select 1 from dual;",
        "fetch c into x;",
        "close c;",
        "proc;",
        "pipe row (1);",
        "set transaction read only;",
        "lock table t in exclusive mode;",
    )

    @Test
    fun matchesStackedLabelsBeforeEveryLabelableStatement() {
        labelableStatements.forEach { statement ->
            listOf("<<a>> $statement", "<<a>> <<b>> $statement", "<<a>> <<b>> <<c>> $statement", "<<a>>\n<<b>>\n$statement")
                .forEach { assertThat(p).describedAs(it).matches(it) }
            assertThat(p).describedAs(statement).matches(statement)
        }
    }

    @Test
    fun keepsPragmasUnlabeledAndRejectsDanglingLabels() {
        listOf(
            wrap("pragma inline(proc, 'YES');"),
            wrap("pragma autonomous_transaction;"),
            "<<a>> <<b>>",
            "<<a>> << b",
            "<<a>> <b>> null;",
            "<<>> null;",
            "<<a b>> null;",
            "<<a>>, <<b>> null;",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun allowsDuplicateAndEndLabelNamesBecauseOracleValidatesThemLater() {
        listOf(
            "<<a>> <<a>> begin null; end;",
            "<<a>> <<b>> begin null; end a;",
            "<<a>> <<b>> begin null; end b;",
            "<<a>> <<b>> begin null; end c;",
            "<<a>> begin <<a>> null; end;",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun matchesTheExactOracleFixture() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).matches(
            """
            <<compute_ratio>>
            <<another_label>>
            DECLARE
              numerator   NUMBER := 22;
              denominator NUMBER := 7;
            BEGIN
              <<another_label>>
              DECLARE
                denominator NUMBER := 0;
              BEGIN
                DBMS_OUTPUT.PUT_LINE('Ratio with compute_ratio.denominator = ');
                DBMS_OUTPUT.PUT_LINE(numerator/compute_ratio.denominator);

                DBMS_OUTPUT.PUT_LINE('Ratio with another_label.denominator = ');
                DBMS_OUTPUT.PUT_LINE(numerator/another_label.denominator);

              EXCEPTION
                WHEN ZERO_DIVIDE THEN
                  DBMS_OUTPUT.PUT_LINE('Divide-by-zero error: can''t divide '
                    || numerator || ' by ' || denominator);
                WHEN OTHERS THEN
                  DBMS_OUTPUT.PUT_LINE('Unexpected error.');
              END another_label;
            END compute_ratio;
            /
            """.trimIndent())
    }

    @Test
    fun exposesEachLabelAsADirectChildWithoutAWrapperNode() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("<<outer>> <<alias>> declare x number; begin <<inner>> begin <<l1>> <<l2>> <<l3>> null; end; end;")
        val outer = tree.getFirstDescendant(PlSqlGrammar.BLOCK_STATEMENT)
        assertThatAst(outer.getChildren(PlSqlGrammar.LABEL).map { it.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenOriginalValue })
            .containsExactly("outer", "alias")
        assertThatAst(tree.getDescendants(PlSqlGrammar.LABELS)).isEmpty()
        val nullStatement = tree.getFirstDescendant(PlSqlGrammar.NULL_STATEMENT)
        assertThatAst(nullStatement.children.map { it.type }.take(3)).containsOnly(PlSqlGrammar.LABEL)
        assertThatAst(nullStatement.getChildren(PlSqlGrammar.LABEL)).hasSize(3)
        assertThatAst(nullStatement.lastChild.tokenOriginalValue).isEqualTo(";")
    }

    @Test
    fun keepsSingleAndUnlabeledStatementsUnchanged() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("begin <<a>> x := 1; y := 2; <<b>> null; null; end;")
        val assignments = tree.getDescendants(PlSqlGrammar.ASSIGNMENT_STATEMENT)
        assertThatAst(assignments[0].getChildren(PlSqlGrammar.LABEL)).hasSize(1)
        assertThatAst(assignments[1].getChildren(PlSqlGrammar.LABEL)).isEmpty()
        val nulls = tree.getDescendants(PlSqlGrammar.NULL_STATEMENT)
        assertThatAst(nulls[0].getChildren(PlSqlGrammar.LABEL)).hasSize(1)
        assertThatAst(nulls[1].getChildren(PlSqlGrammar.LABEL)).isEmpty()
    }
}
