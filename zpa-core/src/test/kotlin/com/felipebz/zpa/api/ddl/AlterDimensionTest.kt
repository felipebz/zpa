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

class AlterDimensionTest : RuleTest() {

    @Test
    fun matchesAlterDimension() {
        setRootRule(DdlGrammar.ALTER_DIMENSION)
        assertThat(p).matches("alter dimension customers_dim drop attribute country;")
        assertThat(p).matches("alter dimension customers_dim add level zone is customers.cust_postal_code " +
            "add attribute zone determines (cust_city);")
        assertThat(p).matches("alter dimension hr.d add level l is (t.c) skip when null add hierarchy h (l1 child of l2) " +
            "add attribute ai level l1 determines t.d level l2 determines (c)")
        assertThat(p).matches("alter dimension d drop level l1 restrict drop level l2 cascade drop hierarchy h " +
            "drop attribute a level l2 column t.d")
        assertThat(p).matches("alter dimension d compile")
        assertThat(p).matches("alter dimension d compile compile drop hierarchy h compile drop hierarchy h2")
        assertThat(p).matches("alter dimension d add level l6 is (t.c) compile add level l7 is (t.d)")
    }

    @Test
    fun rejectsAlterDimensionFormsOracleRejects() {
        setRootRule(DdlGrammar.ALTER_DIMENSION)
        // ORA-02000: an action is required, and ADD/DROP need an object kind.
        assertThat(p).notMatches("alter dimension d")
        assertThat(p).notMatches("alter dimension d add")
        assertThat(p).notMatches("alter dimension d rename to d2")
        // ORA-30348: ADD and DROP cannot be mixed, even around COMPILE.
        assertThat(p).notMatches("alter dimension d add level l is (t.c) drop attribute a")
        assertThat(p).notMatches("alter dimension d compile drop attribute a add level l is (t.c)")
        // ORA-03048: one LEVEL and one COLUMN per DROP ATTRIBUTE.
        assertThat(p).notMatches("alter dimension d drop attribute a level l2 level l1")
        assertThat(p).notMatches("alter dimension d drop attribute a level l2 column d column c")
        assertThat(p).notMatches("alter dimension d drop attribute a column d")
        // ORA-11600.
        assertThat(p).notMatches("alter dimension if exists d compile")
    }

    @Test
    fun matchesAlterAttributeDimension() {
        setRootRule(DdlGrammar.ALTER_ATTRIBUTE_DIMENSION)
        assertThat(p).matches("alter attribute dimension product_attr_dim rename to my_product_attr_dim;")
        assertThat(p).matches("alter attribute dimension if exists hr.d compile")
        // ORA-02000 / ORA-03048.
        assertThat(p).notMatches("alter attribute dimension d")
        assertThat(p).notMatches("alter attribute dimension d rename d2")
        assertThat(p).notMatches("alter attribute dimension d rename to hr.d2")
        assertThat(p).notMatches("alter attribute dimension d compile rename to d2")
    }
}
