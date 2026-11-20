//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class TimeLiteralExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesTimeLiteralsInExpressions() {
        listOf(
            "time '19:00:00'", "TIME'19:00:00'", "TIME    '19:00:00'", "TIME '19:00:00.123456789'", "TIME '19:00:00 +03:00'",
            "TIME '19:00:00' + INTERVAL '9' HOUR", "TIME '19:00:00' - INTERVAL '9' HOUR", "TIME '19:00:00' + INTERVAL '9' DAY",
            "TIME '19:00:00' - INTERVAL '9' DAY", "TIME '19:00:00' + INTERVAL '9' SECOND", "TIME '19:00:00' - INTERVAL '9' SECOND",
            "TIME '19:00:00' = TIME '20:00:00'", "nvl(TIME '19:00:00', TIME '20:00:00')", "case when x then TIME '19:00:00' end",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun parsesInvalidTimeValuesAsLiteralsBecauseOracleValidatesThemLater() {
        listOf(
            "TIME 'x'", "TIME ''", "TIME '19:00'", "TIME '19-00-00'", "TIME '24:00:00'", "TIME '23:60:00'", "TIME '23:59:60'",
            "TIME '19:00:00.1234567890'", "TIME q'[19:00:00]'", "TIME q'[x]'", "TIME N'19:00:00'", "TIME nq'[19:00:00]'",
            "TIME 'x' + INTERVAL '9' HOUR",
        ).forEach {
            assertThat(p).describedAs(it).matches(it)
            assertThatAst(p.parse(it).getDescendants(PlSqlTokenType.TIME_LITERAL)).describedAs(it).hasSize(1)
        }
    }

    @Test
    fun keepsTimeUsableAsAnOrdinaryName() {
        listOf("time", "timevalue", "t.time", "time(1)", "timestamp", "date").forEach { assertThat(p).describedAs(it).matches(it) }
        listOf("time 1", "TIME (3) '19:00:00'", "TIME :b", "TIME '19:00:00' '20:00:00'").forEach { assertThat(p).describedAs(it).notMatches(it) }
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).matches("select time, timevalue from t where time > 1;")
        assertThat(p).matches("select dummy time from dual;")
        assertThat(p).matches("select time from (select 1 time from dual);")
        assertThat(p).matches("create table t (time number);")
    }

    @Test
    fun parsesTheFixtureStatement() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val source = """SELECT TIME '19:00:00'+INTERVAL '9' HOUR, TIME '19:00:00'-INTERVAL '9' HOUR  from dual;
            SELECT TIME '19:00:00'+INTERVAL '9' DAY, TIME '19:00:00'-INTERVAL '9' DAY  from dual;
            SELECT TIME '19:00:00'+INTERVAL '9' SECOND, TIME '19:00:00'-INTERVAL '9' SECOND  from dual;"""
        assertThat(p).matches(source)
        val tree = p.parse(source)
        assertThatAst(tree.getDescendants(PlSqlTokenType.TIME_LITERAL)).hasSize(6)
        assertThatAst(tree.getDescendants(PlSqlKeyword.TIME)).isEmpty()
        assertThatAst(tree.getDescendants(PlSqlGrammar.INTERVAL_LITERAL)).hasSize(6)
    }

    @Test
    fun parsesDateAndTimestampWithAnyStringBecauseOracleValidatesThemLater() {
        mapOf(
            "DATE 'x'" to PlSqlTokenType.DATE_LITERAL, "DATE '01/02/2020'" to PlSqlTokenType.DATE_LITERAL,
            "DATE '2020-01-01 10:00:00'" to PlSqlTokenType.DATE_LITERAL, "DATE q'[x]'" to PlSqlTokenType.DATE_LITERAL,
            "TIMESTAMP 'x'" to PlSqlTokenType.TIMESTAMP_LITERAL, "TIMESTAMP '2020-01-01'" to PlSqlTokenType.TIMESTAMP_LITERAL,
            "TIMESTAMP 'x' + INTERVAL '1' DAY" to PlSqlTokenType.TIMESTAMP_LITERAL,
        ).forEach { (source, type) ->
            assertThat(p).describedAs(source).matches(source)
            assertThatAst(p.parse(source).getDescendants(type)).describedAs(source).hasSize(1)
        }
    }

    @Test
    fun aTimestampOperandAfterAsOfOrInAUnitHeaderStillParses() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        listOf(
            "select 1 from bin as of timestamp '2020-01-01 00:00:00';",
            "select 1 from bin as of timestamp 'x' - 1;",
            "select 1 from bin versions between timestamp 'x' and maxvalue;",
            "create procedure bin_p timestamp '2020-01-01:00:00:00' is begin null; end;\n/\n",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        for (value in listOf("'01-JAN-20'", "'2020-01-01 00:00:00'")) {
            listOf(
                "create restore point bin_rp as of timestamp $value;",
                "flashback table bin to timestamp $value;",
                "flashback database to timestamp $value;",
                "flashback database to before timestamp $value;",
                "alter flashback archive bin_fa purge before timestamp $value;",
                "select * from graph_table(bin_g as of timestamp $value match (a) columns (a.id as id));",
            ).forEach { assertThat(p).describedAs(it).matches(it) }
        }
    }
}
