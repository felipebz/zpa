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
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.Test

class SampleClauseTest : RuleTest() {

    @Test
    fun matchesPercentForms() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)
        assertThat(p).matches("select count(*) * 10 from orders sample (10)")
        assertThat(p).matches("select * from t sample block (10)")
        assertThat(p).matches("select * from t sample (10.5)")
        assertThat(p).matches("select * from t sample (1e1)")
        assertThat(p).matches("select * from t sample (.5)")
        assertThat(p).matches("select * from t sample (5.)")
        assertThat(p).matches("select * from t sample (:b)")
        assertThat(p).matches("select * from t sample (:b:i)")
        // Out-of-range percentages fail later (ORA-30562).
        assertThat(p).matches("select * from t sample (0)")
        assertThat(p).matches("select * from t sample (100.1)")
    }

    @Test
    fun matchesGroupSizeValue() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)
        assertThat(p).matches("select * from t sample block (14.285714, 1) seed (1)")
        assertThat(p).matches("select * from t sample (10, 1.5)")
        assertThat(p).matches("select * from t sample (:a, :b)")
        assertThat(p).matches("select * from t sample (10, 0)")
    }

    @Test
    fun matchesSeed() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)
        assertThat(p).matches("select count(*) * 10 from orders sample(10) seed (1)")
        assertThat(p).matches("select * from t sample (10) seed(4)")
        assertThat(p).matches("select * from t sample (10) seed (0)")
        assertThat(p).matches("select * from t sample (10) seed (1.5)")
        assertThat(p).matches("select * from t sample (10) seed (:b)")
        assertThat(p).matches("select * from t sample (10) seed (4294967296)")
    }

    @Test
    fun rejectsMalformedClauses() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)
        assertThat(p).notMatches("select * from t sample (10, 1, 2)")
        assertThat(p).notMatches("select * from t sample (10,)")
        assertThat(p).notMatches("select * from t sample (-5)")
        assertThat(p).notMatches("select * from t sample (+5)")
        assertThat(p).notMatches("select * from t sample (a)")
        assertThat(p).notMatches("select * from t sample (1+1)")
        assertThat(p).notMatches("select * from t sample ('10')")
        assertThat(p).notMatches("select * from t sample 10")
        assertThat(p).notMatches("select * from t sample ()")
        assertThat(p).notMatches("select * from t sample (10")
        assertThat(p).notMatches("select * from t sample block")
        assertThat(p).notMatches("select * from t sample block 10")
        assertThat(p).notMatches("select * from t sample block block (10)")
        assertThat(p).notMatches("select * from t sample row (10)")
        assertThat(p).notMatches("select * from t sample system (10)")
        assertThat(p).notMatches("select * from t sample (10) seed (-1)")
        assertThat(p).notMatches("select * from t sample (10) seed ()")
        assertThat(p).notMatches("select * from t sample (10) seed 1")
        assertThat(p).notMatches("select * from t sample (10) seed (1, 2)")
        assertThat(p).notMatches("select * from t sample (10) seed ('x')")
        assertThat(p).notMatches("select * from t sample (10, 1) seed (1, 2)")
        assertThat(p).notMatches("select * from t seed (1)")
    }

    @Test
    fun rejectsRepeatedClauses() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)
        assertThat(p).notMatches("select * from t sample (10) sample (10)")
        assertThat(p).notMatches("select * from t sample (10) seed (1) seed (2)")
        assertThat(p).notMatches("select * from t sample (10) seed (1) sample (10)")
    }

    @Test
    fun keepsSampleAndSeedUsableAsAliases() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)
        assertThat(p).matches("select sample.a from t sample")
        assertThat(p).matches("select block.a from t block")
        assertThat(p).matches("select seed.a from t seed")
        assertThat(p).matches("select * from t as sample")
        assertThat(p).matches("select * from t sample (10) seed")
        assertThat(p).matches("select seed.a from t sample (10) seed")
        assertThat(p).matches("select * from t sample (10) block")
        assertThat(p).matches("select * from t sample (10) sample")
        assertThat(p).matches("select * from t sample (10) seed (1) sample")
        assertThat(p).matches("select * from t sample (10) \"seed\"")
        assertThat(p).matches("select sample from sample")
        // An alias directly after SAMPLE, SAMPLE BLOCK or SEED is not another alias (ORA-03049).
        assertThat(p).notMatches("select * from t sample z")
        assertThat(p).notMatches("select * from t sample block")
        assertThat(p).notMatches("select * from t sample seed")
        assertThat(p).notMatches("select * from t sample seed (1)")
        assertThat(p).notMatches("select * from t sample (10) seed z")
        assertThat(p).notMatches("select * from t sample (10) seed seed")
        assertThat(p).notMatches("select * from t sample (10) block (5)")
    }

    @Test
    fun placesAliasAfterTheClause() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)
        assertThat(p).matches("select * from t sample (10) z")
        assertThat(p).matches("select * from t sample block (0.5) seed (3) z")
        assertThat(p).matches("select * from t sample (10) seed (1) \"o\"")
        // ORA-03049/ORA-03048: no alias before the clause and no AS after it.
        assertThat(p).notMatches("select * from t z sample (10)")
        assertThat(p).notMatches("select * from t as z sample (10)")
        assertThat(p).notMatches("select * from t sample (10) as z")
        assertThat(p).notMatches("select * from t sample (10) seed (1) as z")
        assertThat(p).notMatches("select * from t sample as z")
    }

    @Test
    fun placesClauseAmongTableExpressionParts() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)
        assertThat(p).matches("select * from t partition (p1) sample (10)")
        assertThat(p).matches("select * from t partition (p1) sample (10) seed (1) z")
        assertThat(p).matches("select * from t partition for (5) sample (10)")
        assertThat(p).matches("select * from t@dbl sample (10)")
        assertThat(p).matches("select * from s.v sample (10)")
        assertThat(p).matches("select * from t sample (10) pivot (count(*) for a in (1))")
        assertThat(p).notMatches("select * from t sample (10) partition (p1)")
        assertThat(p).notMatches("select * from t sample (10)@dbl")
        assertThat(p).notMatches("select * from t pivot (count(*) for a in (1)) sample (10)")
    }

    @Test
    fun matchesJoinsAndMultipleRowSources() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)
        assertThat(p).matches("select * from t sample (10) z, u sample block (5) y")
        assertThat(p).matches("select * from t sample (10) z join u sample (5) y on 1 = 1")
        assertThat(p).matches("select * from t join u sample (5) on 1 = 1")
        assertThat(p).matches("select * from t join u sample (5) using (a)")
        assertThat(p).matches("select * from t cross join u sample (5)")
        assertThat(p).matches("select * from t natural join u sample (5)")
        assertThat(p).matches("select * from t left outer join u sample (5) on 1 = 1")
        assertThat(p).notMatches("select * from t z join u y sample (5) on 1 = 1")
    }

    @Test
    fun matchesTableNamesAndQueryNames() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)
        assertThat(p).matches("select * from dual sample (10)")
        assertThat(p).matches("with q as (select a from t) select * from q sample (10) z")
        assertThat(p).matches("select * from (t sample (10))")
        assertThat(p).matches("select * from (t sample (10)) z")
        assertThat(p).matches("select * from (select a from t sample (10)) z")
        assertThat(p).matches("select * from t sample (10) where a > 1 order by a")
        assertThat(p).matches("select * from t sample (10) union all select * from u")
        assertThat(p).matches("select * from t sample (10) for update")
    }

    @Test
    fun rejectsOtherRowSources() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)
        assertThat(p).notMatches("select * from (select a from t) sample (10)")
        assertThat(p).notMatches("select * from (select a from t) z sample (10)")
        assertThat(p).notMatches("select * from table(sys.odcinumberlist(1, 2)) sample (10)")
        assertThat(p).notMatches("select * from lateral (select a from t) sample (10)")
    }

    @Test
    fun rejectsSampleOutsideQueries() {
        setRootRule(DmlGrammar.DML_COMMAND)
        // ORA-30560: SAMPLE clause not allowed.
        assertThat(p).notMatches("delete from t sample (10);")
        assertThat(p).notMatches("update t sample (10) set a = 1;")
        assertThat(p).notMatches("merge into u x using t sample (10) s on (x.a = s.a) when matched then update set x.a = 1;")
        assertThat(p).matches("insert into u select a from t sample (10);")
        assertThat(p).matches("delete from u where a in (select a from t sample (10));")
    }

    @Test
    fun matchesPlsqlSelectIntoAndCtas() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).matches("begin select salary bulk collect into sals from employees sample (10); end;")
        assertThat(p).matches(
            "create table \"DVSADM\".CMP3\$58238005 nocompress tablespace \"DVS_PROOF\" nologging " +
                "as select /*+ DYNAMIC_SAMPLING(0) FULL(\"DVSADM\".\"DVS_ARCHIVE\") */ * " +
                "from \"DVSADM\".\"DVS_ARCHIVE\" sample block( 6.734) mytab;")
    }
}
