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

class CreateSearchIndexTest : RuleTest() {
    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_SEARCH_INDEX)
    }

    @Test
    fun matchesJsonTextAndXmlSearchIndexes() {
        assertThat(p).matches("create search index po_search_idx on j_purchaseorder (data) for json;")
        assertThat(p).matches("create search index po_search_idx on j_purchaseorder (data)")
        assertThat(p).matches("create search index idx on docs (txt) for text;")
        assertThat(p).matches("create search index idx on docs (xmldoc) for xml parameters ('SEARCH_ON TEXT');")
        assertThat(p).matches("create search index if not exists app.po_search_idx on app.j_purchaseorder (data) for json")
        assertThat(p).matches("create search index idx on j_purchaseorder t (data) for json")
    }

    @Test
    fun matchesFormsOracleRejectsOnlyAfterParsing() {
        // ORA-29851 (several targets), ORA-29958 (expression) and ORA-00904 (unresolved qualified column).
        assertThat(p).matches("create search index idx on t (data, txt) for json")
        assertThat(p).matches("create search index idx on t (upper(txt))")
        assertThat(p).matches("create search index idx on t (n + 1)")
        assertThat(p).matches("create search index idx on t (t.txt)")
        assertThat(p).matches("create search index idx on t (txt) filter by t.n, app.t.d order by t.n desc")
        assertThat(p).matches("create search index idx on t (txt) order by app.t.n asc")
    }

    @Test
    fun keepsParametersOpaque() {
        assertThat(p).matches("create search index idx on t (data) for json parameters ('SYNC (ON COMMIT)');")
        assertThat(p).matches("create search index idx on t (data) for json parameters ('SEARCH_ON\n" +
            "  TEXT INCLUDE ($.SpecialInstructions, $.LineItems.Part.Description)\n" +
            "  VALUE(NUMBER) INCLUDE ($.PONumber)');")
        assertThat(p).matches("create search index idx on t (txt) parameters ('SYNC (EVERY \"freq=daily; byhour=1\")')")
    }

    @Test
    fun matchesIndexOptionsInOracleOrder() {
        assertThat(p).matches("create search index idx on t (txt) online")
        assertThat(p).matches("create search index idx on t (data) for json parameters ('x') parallel 2 unusable")
        assertThat(p).matches("create search index idx on t (data) for json unusable parallel parameters ('x')")
        assertThat(p).matches("create search index idx on t (data) for json noparallel")
        assertThat(p).matches("create search index idx on t (txt) filter by n, d order by n desc, d")
        assertThat(p).matches("create search index idx on t (txt) order by n asc")
        assertThat(p).matches("create search index idx on t (txt) online filter by n online parameters ('x') online")
        assertThat(p).matches("create search index idx on t (txt) parallel 2 filter by n")
        assertThat(p).matches("create search index idx on t (b) for json local")
        assertThat(p).matches("create search index idx on t (b) for json local (partition i1, partition i2)")
        assertThat(p).matches("create search index idx on t (b) for json local (partition, partition)")
        assertThat(p).matches(
            "create search index idx on t (b) for json local (partition i1 parameters ('x'), partition i2) parameters ('y')")
        assertThat(p).matches("create search index idx on t (b) for json parameters ('x') local")
        assertThat(p).matches("create search index idx on t (c) filter by a local")
    }

    @Test
    fun rejectsInvalidSearchIndexForms() {
        // ORA-00922 / ORA-00968: no OR REPLACE, UNIQUE or other FOR targets.
        assertThat(p).notMatches("create or replace search index idx on t (data) for json")
        assertThat(p).notMatches("create unique search index idx on t (data) for json")
        assertThat(p).notMatches("create search index idx on t (data) for vector")
        // ORA-00936: the target list needs an expression.
        assertThat(p).notMatches("create search index idx on t () for json")
        assertThat(p).notMatches("create search index idx on t (txt,) for json")
        // ORA-29850: ASC/DESC is not accepted after a target.
        assertThat(p).notMatches("create search index idx on t (txt desc)")
        assertThat(p).notMatches("create search index idx on t (upper(txt) asc)")
        // ORA-02158: FOR comes first, and FILTER BY / ORDER BY accept only columns.
        assertThat(p).notMatches("create search index idx on t (data) parameters ('x') for json")
        assertThat(p).notMatches("create search index idx on t (data) online for json")
        assertThat(p).notMatches("create search index idx on t (data) local for json")
        assertThat(p).notMatches("create search index idx on t (txt) order by n desc nulls last")
        assertThat(p).notMatches("create search index idx on t (txt) order by n + 1")
        assertThat(p).notMatches("create search index idx on t (txt) order by upper(txt)")
        assertThat(p).notMatches("create search index idx on t (txt) filter by n + 1")
        assertThat(p).notMatches("create search index idx on t (txt) filter by n asc")
        // ORA-29850: FILTER BY / ORDER BY order, placement and repetition; PARAMETERS once.
        assertThat(p).notMatches("create search index idx on t (txt) order by n filter by d")
        assertThat(p).notMatches("create search index idx on t (txt) filter by n online order by d")
        assertThat(p).notMatches("create search index idx on t (txt) filter by n filter by d")
        assertThat(p).notMatches("create search index idx on t (txt) parameters ('x') filter by n")
        assertThat(p).notMatches("create search index idx on t (txt) parameters ('x') order by n")
        assertThat(p).notMatches("create search index idx on t (data) for json parameters ('x') parameters ('y')")
        assertThat(p).notMatches("create search index idx on t (data) for json tablespace users")
        assertThat(p).notMatches("create search index idx on t (data) for json storage (initial 1m)")
        // ORA-01780 / ORA-00906 / ORA-00907: PARAMETERS takes exactly one parenthesized literal.
        assertThat(p).notMatches("create search index idx on t (data) for json parameters ()")
        assertThat(p).notMatches("create search index idx on t (data) for json parameters 'x'")
        assertThat(p).notMatches("create search index idx on t (data) for json parameters ('x' || 'y')")
        // ORA-14004: LOCAL partition lists need PARTITION.
        assertThat(p).notMatches("create search index idx on t (b) for json local ()")
        assertThat(p).notMatches("create search index idx on t (txt) filter by")
    }

    @Test
    fun keepsOrdinaryCreateIndexSeparate() {
        setRootRule(DdlGrammar.CREATE_INDEX)
        assertThat(p).notMatches("create search index search_ix on documents(content);")

        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("create search index search_ix on documents(content) for json;")
        assertThat(p).matches("create index search on documents(content);")
    }
}
