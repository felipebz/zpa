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

class CreatePropertyGraphTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_PROPERTY_GRAPH)
    }

    @Test
    fun matchesVertexTables() {
        assertThat(p).matches("create property graph \"myGraph\" vertex tables (my_table_1);")
        assertThat(p).matches("create property graph g vertex tables (other_schema.my_table_1, t2 as a2)")
        assertThat(p).matches(
            "create property graph g vertex tables (vt1, vt2 key(pk2), vt3 key(pk31, pk32), vt2 as altvt2 key(pk4))")
        assertThat(p).matches("create property graph g vertex tables (\"myschema\". \"mytable\" label \"person\")")
        assertThat(p).matches("create or replace property graph if not exists s.g vertex tables (t)")
    }

    @Test
    fun matchesLabelsAndProperties() {
        assertThat(p).matches(
            "create property graph g vertex tables (persons key (person_id) " +
                "label person properties (person_id, name, birthdate as dob) label person_ht properties (height))")
        assertThat(p).matches("create property graph g vertex tables (t label e properties are all columns)")
        assertThat(p).matches("create property graph g vertex tables (t properties all columns except (name))")
        assertThat(p).matches("create property graph g vertex tables (t properties (upper(name) as uname, id))")
        assertThat(p).matches("create property graph g vertex tables (t no properties label a label b properties (id))")
        assertThat(p).matches("create property graph g vertex tables (t default label properties (id) label b)")
        assertThat(p).matches("create property graph g vertex tables (t label a no properties properties (id))")
    }

    @Test
    fun matchesEdgeTablesAndOptions() {
        assertThat(p).matches(
            "create property graph g vertex tables (vt1, vt2) edge tables (" +
                "e1 source vt1 destination vt2, " +
                "e2 source key(fk1) references vt1 (pk1) destination key(fk2) references vt2 (pk2), " +
                "v1 as manager key(k) source key(k) references e(k) destination vt2 no properties)")
        assertThat(p).matches(
            "create property graph g vertex tables (t) edge tables (e source t destination t label e properties (c))")
        assertThat(p).matches("create property graph g vertex tables (t) options (enforced mode)")
        assertThat(p).matches(
            "create property graph g vertex tables (t) options (trusted mode, disallow mixed property types)")
    }

    @Test
    fun rejectsMalformedGraphs() {
        assertThat(p).notMatches("create property graph g")
        assertThat(p).notMatches("create property graph g vertex tables ()")
        assertThat(p).notMatches("create property graph g vertex tables (t key ())")
        assertThat(p).notMatches("create property graph g vertex tables (t label)")
        assertThat(p).notMatches("create property graph g vertex tables (t properties ())")
        assertThat(p).notMatches("create property graph g vertex tables (t,)")
        assertThat(p).notMatches("create property graph g vertex tables (t) edge tables (e source t)")
    }

    @Test
    fun rejectsFormsOracleRejects() {
        // ORA-42424 / ORA-02000: an expression property needs AS, and AS is not optional.
        assertThat(p).notMatches("create property graph g vertex tables (t properties (id + 1))")
        assertThat(p).notMatches("create property graph g vertex tables (t properties (id nid))")
        // ORA-42408: a second default-label properties clause.
        assertThat(p).notMatches("create property graph g vertex tables (t properties (id) no properties)")
        // ORA-00907: AS must precede KEY, needs AS, and definitions need commas.
        assertThat(p).notMatches("create property graph g vertex tables (t key (id) as v)")
        assertThat(p).notMatches("create property graph g vertex tables (t v key (id))")
        assertThat(p).notMatches("create property graph g vertex tables (t key (id) t as v2 key (id))")
        // ORA-02000 / ORA-00906: clause order and REFERENCES columns.
        assertThat(p).notMatches("create property graph g edge tables (e source t destination t) vertex tables (t)")
        assertThat(p).notMatches("create property graph g vertex tables (t) edge tables (e destination t source t)")
        assertThat(p).notMatches(
            "create property graph g vertex tables (t) edge tables (e source key (s) references t destination t)")
        // ORA-02000 / ORA-42419: options are a non-empty comma-separated list.
        assertThat(p).notMatches("create property graph g vertex tables (t) options (trusted mode allow mixed property types)")
        assertThat(p).notMatches("create property graph g vertex tables (t) options ()")
    }
}
