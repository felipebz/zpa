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
package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateFlexibleDomainTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_FLEXIBLE_DOMAIN)
    }

    @Test
    fun matchesDecodeAndCaseSelectors() {
        assertThat(p).matches("create flexible domain expense_details (val1, val2, val3, val4) " +
            "choose domain using (typ varchar2(10)) " +
            "from decode(typ, 'Flight', flight_details(val1, val2, val3), 'Meals', meals_details(val1, val2, val4), " +
            "lodging_details(val1, val4));")
        assertThat(p).matches("create flexible domain expense_details (val1, val2, val3, val4) " +
            "choose domain using(typ varchar2(10)) " +
            "from case when typ between 'A' and 'G' then flight_details(val1, val2, val3) " +
            "when typ like 'Lodg%' then hr.lodging_details(val1, val4) else meals_details(val1, val2, val4) end;")
        assertThat(p).matches("create flexible domain d (v1, v2) choose domain using (t varchar2(10)) " +
            "from case t when 'A' then d1(v1, v2) end")
    }

    @Test
    fun matchesListVariants() {
        assertThat(p).matches("create usecase flexible domain if not exists hr.d (v1, v2,) " +
            "choose domain using (t varchar2(10), u number) from case when t = 'A' and u = 1 then d1(v1, v2) end")
        assertThat(p).matches("create flexible domain d () choose domain using () from d1(v1)")
    }

    @Test
    fun rejectsFormsOracleRejects() {
        // ORA-03050 / ORA-00902: bare column names, typed discriminants.
        assertThat(p).notMatches("create flexible domain d (v1 number) choose domain using (t varchar2(1)) from d1(v1)")
        assertThat(p).notMatches("create flexible domain d (v1) choose domain using (t) from d1(v1)")
        // ORA-00904: the parenthesized lists and CHOOSE DOMAIN USING ... FROM are required.
        assertThat(p).notMatches("create flexible domain d as (v1) choose domain using (t number) from d1(v1)")
        assertThat(p).notMatches("create flexible domain d (v1) choose domain using t number from d1(v1)")
        assertThat(p).notMatches("create flexible domain d (v1) choose domain using (t number), (u number) from d1(v1)")
        assertThat(p).notMatches("create flexible domain d (v1) choose domain using (t number)")
        assertThat(p).notMatches("create flexible domain d (v1) from d1(v1)")
        // ORA-03049: no domain properties after the selector.
        assertThat(p).notMatches("create flexible domain d (v1) choose domain using (t number) from d1(v1) display v1")
    }
}
