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

class XmlTypeVirtualColumnsTest : RuleTest() {

    private val binary = "create table t of xmltype xmltype store as binary xml"
    private val cast = "xmlcast(xmlquery('/a/@b' passing object_value returning content) as date)"

    @Test
    fun matchesVirtualColumns() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches(
            "create table po_binaryxml of xmltype xmltype store as binary xml virtual columns " +
                "(date_col as (xmlcast(xmlquery('/PurchaseOrder/@orderDate' passing object_value returning content) as date)));")
        assertThat(p).matches("$binary virtual columns (c1 as ($cast), c2 as (1 + 1))")
        assertThat(p).matches("$binary virtual columns (\"C 1\" as ($cast))")
        assertThat(p).matches("create table t of xmltype virtual columns (c1 as ($cast))")
        assertThat(p).matches("create table t of xmltype xmltype store as securefile binary xml virtual columns (c1 as ($cast))")
        assertThat(p).matches("create table t of xmltype xmltype store as not transportable binary xml virtual columns (c1 as ($cast))")
        assertThat(p).matches("$binary (tablespace users) virtual columns (c1 as ($cast))")
    }

    @Test
    fun matchesColumnKeywords() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches("$binary virtual columns (c1 generated always as ($cast))")
        assertThat(p).matches("$binary virtual columns (c1 as ($cast) virtual)")
        assertThat(p).matches("$binary virtual columns (c1 as ($cast) visible)")
        assertThat(p).matches("$binary virtual columns (c1 generated always as ($cast) virtual visible)")
    }

    @Test
    fun matchesPlacementAndRepetition() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches("create global temporary table t of xmltype on commit preserve rows virtual columns (c1 as ($cast))")
        assertThat(p).matches("create table t of xmltype object identifier is system generated virtual columns (c1 as ($cast))")
        assertThat(p).matches("$binary virtual columns (c1 as ($cast)) virtual columns (c2 as ($cast))")
        assertThat(p).matches("$binary virtual columns (c1 as ($cast)) xmlschema \"s\" element \"e\"")
        assertThat(p).matches("$binary virtual columns (c1 as ($cast)) tablespace users enable row movement")
        assertThat(p).matches("$binary virtual columns (c1 as ($cast)) partition by hash (c1) partitions 2")
        // Binary XML storage is not enforced syntactically; object relational storage fails later (ORA-19002).
        assertThat(p).matches("create table t of xmltype xmltype store as object relational virtual columns (c1 as ($cast))")
    }

    @Test
    fun rejectsMalformedColumnLists() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).notMatches("$binary virtual columns ()")
        assertThat(p).notMatches("$binary virtual columns (c1 as ($cast),)")
        assertThat(p).notMatches("$binary virtual columns (, c1 as ($cast))")
        assertThat(p).notMatches("$binary virtual columns (c1 as ($cast) c2 as ($cast))")
        assertThat(p).notMatches("$binary virtual columns c1 as ($cast)")
        assertThat(p).notMatches("$binary virtual column (c1 as ($cast))")
        assertThat(p).notMatches("$binary virtual (c1 as ($cast))")
        assertThat(p).notMatches("$binary virtual columns")
    }

    @Test
    fun rejectsMalformedColumns() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).notMatches("$binary virtual columns (c1)")
        assertThat(p).notMatches("$binary virtual columns (c1 as ())")
        assertThat(p).notMatches("$binary virtual columns (c1 as $cast)")
        assertThat(p).notMatches("$binary virtual columns (c1 number as ($cast))")
        assertThat(p).notMatches("$binary virtual columns (c1.x as ($cast))")
        assertThat(p).notMatches("$binary virtual columns (c1 generated as ($cast))")
        assertThat(p).notMatches("$binary virtual columns (c1 always as ($cast))")
        assertThat(p).notMatches("$binary virtual columns (c1 as ($cast) not null)")
        assertThat(p).notMatches("$binary virtual columns (c1 as ($cast) constraint k unique)")
        assertThat(p).notMatches("$binary virtual columns (c1 as ($cast) default 1)")
        assertThat(p).notMatches("$binary virtual columns (c1 as ($cast) invisible)")
        assertThat(p).notMatches("$binary virtual columns (c1 as ($cast) visible virtual)")
        assertThat(p).notMatches("$binary virtual columns (c1 as ($cast) virtual virtual)")
    }

    @Test
    fun rejectsMisplacedClause() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).notMatches("create global temporary table t of xmltype virtual columns (c1 as ($cast)) on commit preserve rows")
        assertThat(p).notMatches("create table t of xmltype virtual columns (c1 as ($cast)) object identifier is system generated")
        assertThat(p).notMatches("$binary virtual columns (c1 as ($cast)) garbage")
        // After CLOB, VIRTUAL is read as the LOB segment name, so the clause cannot follow directly.
        assertThat(p).notMatches("create table t of xmltype xmltype store as clob virtual columns (c1 as ($cast))")
        assertThat(p).matches("create table t of xmltype xmltype store as clob seg1 virtual columns (c1 as ($cast))")
    }
}
