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
package com.felipebz.zpa.api.dml

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class FlashbackQueryTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.FLASHBACK_QUERY_CLAUSE)
    }

    @Test
    fun matchesAsOf() {
        assertThat(p).matches("as of timestamp systimestamp")
        assertThat(p).matches("as of timestamp (systimestamp - interval '1' minute)")
        assertThat(p).matches("as of timestamp systimestamp - interval '1' minute")
        assertThat(p).matches("as of timestamp to_timestamp('2020-01-01', 'YYYY-MM-DD')")
        assertThat(p).matches("as of timestamp :b")
        assertThat(p).matches("as of scn 1")
        assertThat(p).matches("as of scn 1 + 1")
        assertThat(p).matches("as of scn timestamp_to_scn(systimestamp - 1)")
        assertThat(p).matches("as of period for valid_time systimestamp")
        assertThat(p).matches("as of period for valid_time to_timestamp('2020-01-01', 'YYYY-MM-DD')")
    }

    @Test
    fun rejectsMalformedAsOf() {
        assertThat(p).notMatches("as of")
        assertThat(p).notMatches("as of systimestamp")
        assertThat(p).notMatches("as of timestamp")
        assertThat(p).notMatches("as of scn")
        assertThat(p).notMatches("as of scn 1 = 1")
        assertThat(p).notMatches("as of scn 1 and 2")
        assertThat(p).notMatches("as of period for systimestamp")
        assertThat(p).notMatches("as of period valid_time systimestamp")
        assertThat(p).notMatches("as of period for valid_time")
        assertThat(p).notMatches("as of period for valid_time scn 1")
        assertThat(p).notMatches("as of scn 1 as of")
        assertThat(p).notMatches("as of scn 1 as t")
        assertThat(p).notMatches("of scn 1")
        assertThat(p).notMatches("as timestamp systimestamp")
    }

    @Test
    fun matchesVersionsBetween() {
        assertThat(p).matches("versions between timestamp systimestamp - interval '10' minute and systimestamp - interval '1' minute")
        assertThat(p).matches("versions between scn 1 and 2")
        assertThat(p).matches("versions between scn (1) and (2)")
        assertThat(p).matches("versions between scn minvalue and maxvalue")
        assertThat(p).matches("versions between timestamp minvalue and maxvalue")
        assertThat(p).matches("versions between scn minvalue and 5")
        assertThat(p).matches("versions between scn 5 and maxvalue")
        assertThat(p).matches("versions between timestamp :a and :b")
    }

    @Test
    fun matchesVersionsPeriod() {
        assertThat(p).matches("versions period for dt between current_date - 10 and current_date")
        assertThat(p).matches("versions period for dt between minvalue and maxvalue")
        assertThat(p).matches("versions period for dt between minvalue and current_date")
        assertThat(p).matches("versions period for dt between sysdate - 1 and maxvalue")
    }

    @Test
    fun rejectsMalformedVersions() {
        assertThat(p).notMatches("versions between 1 and 2")
        assertThat(p).notMatches("versions between scn 1")
        assertThat(p).notMatches("versions between scn 1 and")
        assertThat(p).notMatches("versions between scn and 2")
        assertThat(p).notMatches("versions between scn 1 and 2 and 3")
        assertThat(p).notMatches("versions between scn minvalue + 1 and 5")
        assertThat(p).notMatches("versions between scn 1 and maxvalue + 1")
        assertThat(p).notMatches("versions between scn 1 or 2 and 3")
        assertThat(p).notMatches("versions between scn 1 = 1 and 2")
        assertThat(p).notMatches("versions scn 1 and 2")
        assertThat(p).notMatches("versions between")
        assertThat(p).notMatches("versions period dt between 1 and 2")
        assertThat(p).notMatches("versions period for between 1 and 2")
        assertThat(p).notMatches("versions period for dt 1 and 2")
        assertThat(p).notMatches("versions period for dt between 1")
        assertThat(p).notMatches("versions period for dt between scn 1 and 2")
        assertThat(p).notMatches("versions")
        assertThat(p).notMatches("versions between scn 1 and 2 as of")
        assertThat(p).notMatches("versions between scn 1 and 2 as t")
        assertThat(p).notMatches("versions between scn 1 and 2 versions")
        assertThat(p).notMatches("as of scn 1 versions between scn 1")
    }

    @Test
    fun matchesCombinedVersionsAndAsOf() {
        val versions = "versions between scn 1 and 2"
        val period = "versions period for vt between minvalue and maxvalue"
        for (asOf in listOf("as of scn 3", "as of timestamp systimestamp", "as of period for vt systimestamp")) {
            assertThat(p).matches("$versions $asOf")
            assertThat(p).matches("$asOf $versions")
            assertThat(p).matches("$period $asOf")
            assertThat(p).matches("$asOf $period")
        }
        assertThat(p).matches("versions between timestamp systimestamp - 1 and systimestamp as of timestamp systimestamp")
        assertThat(p).matches("$versions $period")
        // Repeats parse and fail later (ORA-08187).
        assertThat(p).matches("$versions $versions")
        assertThat(p).matches("as of scn 1 as of scn 2")
        assertThat(p).matches("$versions as of scn 3 $versions")
    }

    private fun parseFile(sql: String) = run {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        p.parse(sql)
    }

    @Test
    fun attachesToTheTableReferenceInSelectAndDml() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).matches("select salary from employees as of timestamp (systimestamp - interval '1' minute) where last_name = 'Chung';")
        assertThat(p).matches("select salary from employees versions between timestamp systimestamp - interval '10' minute and systimestamp - interval '1' minute where last_name = 'Chung';")
        assertThat(p).matches("update employees set salary = (select salary from employees as of timestamp (systimestamp - interval '2' minute) where last_name = 'Chung') where last_name = 'Chung';")
        assertThat(p).matches("select * from sale_op_tbl versions period for dt between current_date - 10 and current_date where id_store = 589 and id_item = 29584;")
        assertThat(p).matches("insert into t select * from e as of scn 1;")
        assertThat(p).matches("create table t as select * from e as of scn 1;")
        assertThat(p).matches("create view v as select * from e as of scn 1;")
        assertThat(p).matches("with q as (select * from e) select * from q as of scn 1;")
        assertThat(p).matches("merge into t using e as of scn 1 s on (t.a = s.a) when matched then update set t.b = s.b;")
    }

    @Test
    fun keepsAliasesJoinsAndSubqueriesWorking() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).matches("select * from e as of scn 1 a;")
        assertThat(p).matches("select * from e as of scn 1 a where a.x = 1;")
        assertThat(p).matches("select * from e a as of scn 1;")
        assertThat(p).matches("select * from e versions between scn 1 and 2 v;")
        assertThat(p).matches("select * from e versions between scn 1 and 2 as of scn 3 v where v.x = 1;")
        assertThat(p).matches("select * from e as of scn 3 versions between scn 1 and 2 v;")
        assertThat(p).matches("select * from e a join d versions between scn 1 and 2 as of scn 3 b on a.x = b.x;")
        assertThat(p).matches("with q as (select * from e) select * from q versions between scn 1 and 2 as of scn 3;")
        assertThat(p).notMatches("select * from e versions between scn 1 and 2 as of scn 3 as v;")
        assertThat(p).matches("select * from e as of scn 1 a join d as of scn 1 b on a.x = b.x;")
        assertThat(p).matches("select * from e a join d versions between scn minvalue and maxvalue b on a.x = b.x;")
        assertThat(p).matches("select * from e as of scn 1, d as of scn 2;")
        assertThat(p).matches("select * from e as of scn 1 left join d on e.x = d.x;")
        assertThat(p).matches("select * from (select * from e) as of scn 1;")
        assertThat(p).matches("select * from (select * from e) t as of scn 1;")
        assertThat(p).matches("select * from (select * from e) versions between scn 1 and 2;")
        assertThat(p).matches("select * from hr.e as of scn 1;")
        assertThat(p).matches("select * from e@lnk as of scn 1;")
        assertThat(p).matches("select * from e partition (p1) as of scn 1;")
        assertThat(p).matches("select * from e sample (10) as of scn 1;")
        assertThat(p).matches("select * from e as of scn 1 pivot (count(*) for x in (1, 2));")
        assertThat(p).matches("select * from e as of timestamp systimestamp where x = 1 and y = 2;")
        assertThat(p).matches("select * from e as of scn 1 order by 1;")
    }

    @Test
    fun keepsVersionsAndPeriodUsableAsAliases() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).matches("select * from e versions;")
        assertThat(p).matches("select * from e versions where versions.x = 1;")
        assertThat(p).matches("select * from e period;")
        assertThat(p).matches("select * from e as versions;")
        assertThat(p).matches("select versions from e versions join d period on versions.x = period.x;")
        assertThat(p).matches("create table t (versions number, period number);")
        assertThat(p).notMatches("select * from e as of;")
        assertThat(p).notMatches("select * from e as of scn 1 as a;")
        assertThat(p).notMatches("select * from e as of scn 1 and 1 = 1;")
        assertThat(p).notMatches("select * from e versions between scn 1 and 2 as v;")
    }

    @Test
    fun exposesTheClauseAsItsOwnNode() {
        val tree = parseFile("select * from e as of scn 1 a join d versions between scn minvalue and maxvalue b on a.x = b.x, " +
            "(select 1 from dual) t as of timestamp systimestamp;")
        val clauses = tree.getDescendants(DmlGrammar.FLASHBACK_QUERY_CLAUSE)
        assertThatAst(clauses).hasSize(3)
        assertThatAst(clauses.map { it.getFirstChild().tokenOriginalValue.lowercase() })
            .containsExactly("as", "versions", "as")
        val tables = tree.getDescendants(DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE)
        assertThatAst(tables.filter { it.hasDirectChildren(DmlGrammar.FLASHBACK_QUERY_CLAUSE) }).hasSize(3)
    }
}
