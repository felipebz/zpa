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
import org.junit.jupiter.api.Test

class AttributeDimensionTest : RuleTest() {

    @Test
    fun matchesAttributeDimension() {
        setRootRule(DdlGrammar.CREATE_ATTRIBUTE_DIMENSION)
        assertThat(p).matches("create or replace attribute dimension product_attr_dim using product_dim " +
            "attributes (department_id, department_name) " +
            "level department key department_id alternate key department_name member name department_name " +
            "member caption department_name order by department_name " +
            "level category key category_id determines(department_id) " +
            "all member name 'ALL PRODUCTS';")
        assertThat(p).matches("create or replace attribute dimension time_attr_dim dimension type time using time_dim " +
            "attributes (year_id classification caption value 'YEAR_ID' classification description value 'YEAR ID') " +
            "level month level type months classification caption value 'MONTH' key month_id " +
            "member name month_name member caption month_name member description month_long_name " +
            "order by month_end_date determines (month_end_date, quarter_id)")
        assertThat(p).matches("create force attribute dimension if not exists hr.d " +
            "caption 'c' description 'd' classification k value 'v' language 'AMERICAN' " +
            "using hr.t1 remote z1, t2 as z2 join path p1 on z1.a = z2.a and z1.b = z2.b join path p2 on z1.a = z2.c " +
            "attributes (z1.a as aa caption 'x', z2.b bb) " +
            "level l not null key (a, b) alternate key (c, d) member name a || 'x' " +
            "order by min b desc nulls last, max a " +
            "level m skip when null level type standard description 'd' key c " +
            "all member caption 'c' member description 'd'")
        assertThat(p).matches("create attribute dimension d using t attributes (a) level l key a all member description 'd'")
    }

    @Test
    fun rejectsMalformedAttributeDimension() {
        setRootRule(DdlGrammar.CREATE_ATTRIBUTE_DIMENSION)
        // ORA-02000: a level with a KEY is required, also before ALL MEMBER.
        assertThat(p).notMatches("create attribute dimension d using t attributes (a)")
        assertThat(p).notMatches("create attribute dimension d using t attributes (a) level l")
        assertThat(p).notMatches("create attribute dimension d using t attributes (a) all member name 'x'")
        // ORA-02000 / ORA-03048 / ORA-03049: the level parts keep their order.
        assertThat(p).notMatches("create attribute dimension d using t attributes (a) level l member name a key a")
        assertThat(p).notMatches("create attribute dimension d using t attributes (a) level l key a member caption a member name a")
        assertThat(p).notMatches("create attribute dimension d using t attributes (a) level l key a determines (b) order by b")
        assertThat(p).notMatches("create attribute dimension d using t attributes (a) level l classification k level type standard key a")
        assertThat(p).notMatches("create attribute dimension d using t attributes (a) level l description 'd' caption 'c' key a")
        assertThat(p).notMatches("create attribute dimension d using t attributes (a) level l not null skip when null key a")
        // ORA-01780: literal values; ORA-00931 / ORA-02000: bare sources and equality joins only.
        assertThat(p).notMatches("create attribute dimension d using t attributes (a) level l classification k value 1 key a")
        assertThat(p).notMatches("create attribute dimension d using (t) attributes (a) level l key a")
        assertThat(p).notMatches("create attribute dimension d using t z1, t z2 join path p on z1.a > z2.a attributes (a) level l key a")
        assertThat(p).notMatches("create attribute dimension d using t attributes () level l key a")
    }

    @Test
    fun matchesHierarchy() {
        setRootRule(DdlGrammar.CREATE_HIERARCHY)
        assertThat(p).matches("create or replace hierarchy time_hier using time_attr_dim (month child of quarter child of year);")
        assertThat(p).matches("create hierarchy h using d (l)")
        assertThat(p).matches("create force hierarchy if not exists hr.h caption 'c' description 'd' using hr.d (a child of b) " +
            "hierarchical attributes (member_name caption 'x', hier_order classification k value 'v', depth)")
    }

    @Test
    fun rejectsMalformedHierarchy() {
        setRootRule(DdlGrammar.CREATE_HIERARCHY)
        assertThat(p).notMatches("create hierarchy h using d ()")
        assertThat(p).notMatches("create hierarchy h using d a child of b")
        assertThat(p).notMatches("create hierarchy h using d (a child b)")
        assertThat(p).notMatches("create hierarchy h using d (a, b)")
        assertThat(p).notMatches("create hierarchy h using d (a) hierarchical attributes ()")
    }
}
