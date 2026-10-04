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
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
}
