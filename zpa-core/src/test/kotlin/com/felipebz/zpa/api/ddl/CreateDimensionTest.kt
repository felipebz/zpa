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

class CreateDimensionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_DIMENSION)
    }

    @Test
    fun matchesDimension() {
        assertThat(p).matches("create dimension customers_dim " +
            "level customer is (customers.cust_id) " +
            "level status is (customers.cust_marital_status) skip when null " +
            "level country is (countries.country_id) " +
            "hierarchy geog_rollup (customer child of status child of country " +
            "join key (customers.country_id) references country) " +
            "attribute customer determines (cust_first_name, cust_last_name) " +
            "attribute country determines (countries.country_name);")
        assertThat(p).matches("create dimension hr.d level l1 is t.a level l2 is (hr.t.b, t.c)")
        assertThat(p).matches("create dimension d level l1 is (t.a) level l2 is (t.b) " +
            "attribute ai level l1 determines t.c level l2 determines (c, d) " +
            "hierarchy h (l1 child of l2 join key b references l2 join key (t.c, d) references l2) " +
            "attribute l2 determines d hierarchy h2 (l1 child of l2)")
    }

    @Test
    fun rejectsFormsOracleRejects() {
        // ORA-02000 / ORA-30347: at least one level first, with table-qualified columns.
        assertThat(p).notMatches("create dimension d")
        assertThat(p).notMatches("create dimension d hierarchy h (l1 child of l2)")
        assertThat(p).notMatches("create dimension d level l1 is (a)")
        assertThat(p).notMatches("create dimension d level l1 is (t.a) skip")
        // ORA-03048: levels cannot follow a hierarchy.
        assertThat(p).notMatches("create dimension d level l1 is (t.a) hierarchy h (l1 child of l2) level l2 is (t.b)")
        // ORA-02000 / ORA-00906: a parenthesized chain with at least one CHILD OF.
        assertThat(p).notMatches("create dimension d level l1 is (t.a) hierarchy h (l1)")
        assertThat(p).notMatches("create dimension d level l1 is (t.a) hierarchy h l1 child of l2")
        // ORA-00905 / ORA-02000: both attribute forms need DETERMINES.
        assertThat(p).notMatches("create dimension d level l1 is (t.a) attribute l1")
        assertThat(p).notMatches("create dimension d level l1 is (t.a) attribute ai level l1")
        // ORA-00922 / ORA-11600.
        assertThat(p).notMatches("create or replace dimension d level l1 is (t.a)")
        assertThat(p).notMatches("create dimension if not exists d level l1 is (t.a)")
    }
}
