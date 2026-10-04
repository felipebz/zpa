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
package com.felipebz.zpa.api.sql

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.Test

class SubqueryRestrictionClauseTest : RuleTest() {

    private val restrictions = listOf("with read only", "with check option", "WITH READ ONLY", "With Check Option")

    @Test
    fun matchesRestrictionInEveryDmlTableExpression() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        restrictions.forEach { r ->
            listOf(
                "select * from (select * from t $r);",
                "select * from (select * from t $r) x where x.a = 1;",
                "select * from (select * from t order by a $r);",
                "select * from (select * from t $r) pivot (count(*) for a in (1));",
                "select * from lateral (select * from t $r);",
                "select * from ((select * from t $r));",
                "select * from a join (select * from t $r) b on a.x = b.x;",
                "insert into (select a, b from t $r) values (1, 2);",
                "insert into (select a, b from t $r) x (a, b) values (1, 2);",
                "insert into zpa_t select * from (select * from t $r);",
                "update (select a, b from t $r) set a = 1;",
                "update (select a, b from t $r) x set a = 1 where x.b = 2;",
                "delete from (select a, b from t $r);",
                "delete (select a, b from t $r) where a = 1;",
                "merge into (select a, b from t $r) x using dual d on (x.a = 1) when matched then update set x.b = 2;",
                "merge into t x using (select 1 a from dual $r) d on (x.a = d.a) when matched then update set x.b = 2;",
            ).forEach { assertThat(p).describedAs(it).matches(it) }
        }
    }

    @Test
    fun matchesTheExactSelect74Fixture() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).matches(
            """
            INSERT INTO (
                SELECT department_id, department_name, location_id
                FROM departments
                WHERE location_id < 2000
                WITH CHECK OPTION
            )
            VALUES (9999, 'Entertainment', 2500);
            """.trimIndent())
    }

    @Test
    fun rejectsWhatOracleRejects() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        listOf(
            "select * from (select * from t with check);",
            "select * from (select * from t with read);",
            "select * from (select * from t with option);",
            "select * from (select * from t with only);",
            "select * from (select * from t with check option read only);",
            "select * from (select * from t with read only check option);",
            "select * from (select * from t with check option with read only);",
            "select * from (select * from t with check option with check option);",
            "select * from (select * from t with read only constraint c);",
            "select * from (select * from t with check option constraint c);",
            "insert into (select * from t with check option constraint c) values (1);",
            "update (select * from t with read only constraint c) set a = 1;",
            "delete from (select * from t with check option constraint c);",
            "merge into (select * from t with check option constraint c) x using dual d on (1 = 1) when matched then update set a = 1;",
            "select * from (select * from t with read only union all select * from t);",
            "select * from (select * from t with read only for update);",
            "select * from t with read only;",
            "select * from t where a in (select a from t with read only);",
            "select (select a from t with check option) from dual;",
            "insert into t values ((select a from t with check option));",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun keepsRestrictionAsATransparentHelper() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val insert = p.parse("insert into (select a from t with check option) values (1);")
        assertThatAst(insert.getDescendants(DmlGrammar.SUBQUERY_RESTRICTION_CLAUSE)).isEmpty()
        assertThatAst(insert.getDescendants(DmlGrammar.DML_SUBQUERY_TARGET)).isEmpty()
        val clause = insert.getFirstDescendant(DmlGrammar.INSERT_INTO_CLAUSE)
        assertThatAst(clause.children.map { it.type }).startsWith(PlSqlKeyword.INTO, com.felipebz.zpa.api.PlSqlPunctuator.LPARENTHESIS, DmlGrammar.SELECT_EXPRESSION)
        assertThatAst(clause.getFirstChild(DmlGrammar.SELECT_EXPRESSION).tokens.map { it.originalValue }).doesNotContain("with", "check", "option")
        val select = p.parse("select * from (select * from t with read only) x;")
        assertThatAst(select.getDescendants(DmlGrammar.SUBQUERY_RESTRICTION_CLAUSE)).isEmpty()
        assertThatAst(select.getFirstDescendant(DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE).tokens.map { it.originalValue })
            .containsExactly("(", "select", "*", "from", "t", "with", "read", "only", ")", "x")
    }

    @Test
    fun preservesParenthesizedSubqueryBehavior() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        listOf(
            "select * from (select * from t) x;",
            "select * from (select * from t) as of timestamp sysdate x;",
            "select * from (select * from t pivot (count(*) for a in (1)));",
            "insert into (select a from t) values (1);",
            "update (select a from t) set a = 1;",
            "delete from (select a from t);",
            "merge into (select a from t) x using dual d on (x.a = 1) when matched then update set x.a = 2;",
            "create view v as select 1 a from dual with read only;",
            "create view v as select 1 a from dual with check option constraint c;",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }
}
