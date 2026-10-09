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
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

class ParenthesizedExpressionScalabilityTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
    }

    private fun rejectsQuickly(source: String) {
        assertTimeoutPreemptively(Duration.ofSeconds(30), { assertThat(p).notMatches(source) }, source.take(60))
    }

    @Test
    fun rejectsUnclosedParenthesesAtAnyDepth() {
        for (depth in listOf(1, 3, 8, 40)) {
            val open = "(".repeat(depth)
            rejectsQuickly("select * from t where $open\n")
            rejectsQuickly("select $open\nfrom t;")
            rejectsQuickly("select * from t where a in $open\n")
            rejectsQuickly("select * from t where ${"(1, ".repeat(depth)}\n")
            rejectsQuickly("select * from t join u on $open\n")
            rejectsQuickly("begin x := $open\nend;\n/")
        }
    }

    @Test
    fun acceptsNestedParenthesesTupleAndSubqueryForms() {
        for (source in listOf(
            "select * from t where " + "(".repeat(40) + "a = 1" + ")".repeat(40) + ";",
            "select * from t where (a, b) in ((1, 2), (3, 4)) and (x as t) is not null;",
            "select * from t where ((select 1 from dual) union (select 2 from dual)) is not null;",
            "select * from t where (a, b) overlaps (c, d) and ((a)(b)) = 1;",
        )) assertThat(p).describedAs(source).matches(source)
    }
}
