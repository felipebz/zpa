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

class AlterAnalyticViewTest : RuleTest() {

    @Test
    fun matchesRenameAndCompile() {
        setRootRule(DdlGrammar.ALTER_ANALYTIC_VIEW)
        assertThat(p).matches("alter analytic view sales_av rename to mysales_av;")
        assertThat(p).matches("alter analytic view av rename to \"New Av\"")
        assertThat(p).matches("alter analytic view av compile")
    }

    @Test
    fun matchesNamesAndIfExists() {
        setRootRule(DdlGrammar.ALTER_ANALYTIC_VIEW)
        assertThat(p).matches("alter analytic view if exists av compile")
        assertThat(p).matches("alter analytic view if exists s.av compile")
        assertThat(p).matches("alter analytic view s.av compile")
        assertThat(p).matches("alter analytic view \"S\".\"My Av\" compile")
        assertThat(p).matches("alter analytic view if exists av drop cache measure group (m) levels (x)")
    }

    @Test
    fun rejectsInvalidHeader() {
        setRootRule(DdlGrammar.ALTER_ANALYTIC_VIEW)
        assertThat(p).notMatches("alter analytic view a.b.c compile")
        assertThat(p).notMatches("alter analytic view av@dbl compile")
        assertThat(p).notMatches("alter analytic view if not exists av compile")
        assertThat(p).notMatches("alter analytic view av if exists compile")
        assertThat(p).notMatches("alter if exists analytic view av compile")
        assertThat(p).notMatches("alter or replace analytic view av compile")
        assertThat(p).notMatches("alter analytic view compile")
    }

    @Test
    fun rejectsInvalidRenameAndCompile() {
        setRootRule(DdlGrammar.ALTER_ANALYTIC_VIEW)
        assertThat(p).notMatches("alter analytic view av")
        assertThat(p).notMatches("alter analytic view av rename to s.av2")
        assertThat(p).notMatches("alter analytic view av rename to av2@dbl")
        assertThat(p).notMatches("alter analytic view av rename to av2, av3")
        assertThat(p).notMatches("alter analytic view av rename to")
        assertThat(p).notMatches("alter analytic view av rename av2")
        assertThat(p).notMatches("alter analytic view av compile debug")
        assertThat(p).notMatches("alter analytic view av compile reuse settings")
        assertThat(p).notMatches("alter analytic view av compile compile")
        assertThat(p).notMatches("alter analytic view av compile rename to av2")
        assertThat(p).notMatches("alter analytic view av rename to av2 compile")
    }

    @Test
    fun matchesAddCache() {
        setRootRule(DdlGrammar.ALTER_ANALYTIC_VIEW)
        assertThat(p).matches(
            "alter analytic view av add cache measure group (sales, units, cost) " +
                "levels (time.fiscal.fiscal_quarter, warehouse) materialized;")
        assertThat(p).matches("alter analytic view av add cache measure group all levels (x) materialized")
        assertThat(p).matches("alter analytic view av add cache measure group all levels (h.l) materialized using t")
        assertThat(p).matches("alter analytic view av add cache measure group all levels (x) materialized using s.t")
        assertThat(p).matches("alter analytic view av add cache measure group all levels (x) materialized using \"T x\"")
    }

    @Test
    fun matchesDropCache() {
        setRootRule(DdlGrammar.ALTER_ANALYTIC_VIEW)
        assertThat(p).matches(
            "alter analytic view av drop cache measure group (sales, units, cost) " +
                "levels (time.fiscal.fiscal_quarter, warehouse);")
        assertThat(p).matches("alter analytic view av drop cache measure group all levels (d.h.l, \"X\")")
    }

    @Test
    fun matchesMeasureAndLevelLists() {
        setRootRule(DdlGrammar.ALTER_ANALYTIC_VIEW)
        assertThat(p).matches("alter analytic view av add cache measure group (\"Sales\", sales) levels (x) materialized")
        // Duplicates parse; Oracle validates them against the analytic view.
        assertThat(p).matches("alter analytic view av add cache measure group (sales, sales) levels (x, x) materialized")
        assertThat(p).matches("alter analytic view av add cache measure group all levels (\"D\".\"H\".\"L\", \"l\") materialized")
        // An empty LEVELS list is accepted by Oracle 26, unlike an empty measure list.
        assertThat(p).matches("alter analytic view av add cache measure group (m) levels () materialized")
        assertThat(p).matches("alter analytic view av drop cache measure group all levels ()")
    }

    @Test
    fun rejectsMalformedMeasureGroup() {
        setRootRule(DdlGrammar.ALTER_ANALYTIC_VIEW)
        assertThat(p).notMatches("alter analytic view av add cache measure group (all) levels (x) materialized")
        assertThat(p).notMatches("alter analytic view av add cache measure group (all, sales) levels (x) materialized")
        assertThat(p).notMatches("alter analytic view av add cache measure group all, (sales) levels (x) materialized")
        assertThat(p).notMatches("alter analytic view av add cache measure group () levels (x) materialized")
        assertThat(p).notMatches("alter analytic view av drop cache measure group () levels (x)")
        assertThat(p).notMatches("alter analytic view av add cache measure group (sales,) levels (x) materialized")
        assertThat(p).notMatches("alter analytic view av add cache measure group (, sales) levels (x) materialized")
        assertThat(p).notMatches("alter analytic view av add cache measure group (sales,, units) levels (x) materialized")
        assertThat(p).notMatches("alter analytic view av add cache measure group (a.sales) levels (x) materialized")
        assertThat(p).notMatches("alter analytic view av add cache measure group sales levels (x) materialized")
        assertThat(p).notMatches("alter analytic view av add cache measure (sales) levels (x) materialized")
        assertThat(p).notMatches("alter analytic view av add cache group (sales) levels (x) materialized")
    }

    @Test
    fun rejectsMalformedLevels() {
        setRootRule(DdlGrammar.ALTER_ANALYTIC_VIEW)
        assertThat(p).notMatches("alter analytic view av add cache measure group all levels (a.b.c.d) materialized")
        assertThat(p).notMatches("alter analytic view av drop cache measure group all levels (a.b.c.d)")
        assertThat(p).notMatches("alter analytic view av add cache measure group all levels (x@dbl) materialized")
        assertThat(p).notMatches("alter analytic view av add cache measure group all levels (x,) materialized")
        assertThat(p).notMatches("alter analytic view av add cache measure group all levels (, x) materialized")
        assertThat(p).notMatches("alter analytic view av add cache measure group all levels (x,, y) materialized")
        assertThat(p).notMatches("alter analytic view av add cache measure group all levels x materialized")
        assertThat(p).notMatches("alter analytic view av add cache measure group (sales) levels ((d.h.l), (w)) materialized")
    }

    @Test
    fun rejectsMalformedCacheStructure() {
        setRootRule(DdlGrammar.ALTER_ANALYTIC_VIEW)
        // The documented ADD CACHE example omits MATERIALIZED, which Oracle 26 requires (ORA-02000).
        assertThat(p).notMatches(
            "alter analytic view av add cache measure group (sales, units, cost) levels (time.fiscal.fiscal_quarter, warehouse)")
        assertThat(p).notMatches("alter analytic view av add cache measure group all levels (x) nomaterialized")
        assertThat(p).notMatches("alter analytic view av add cache measure group all levels (x) materialized materialized")
        assertThat(p).notMatches("alter analytic view av add cache measure group all levels (x) using t materialized")
        assertThat(p).notMatches("alter analytic view av add cache measure group all levels (x) materialized using")
        assertThat(p).notMatches("alter analytic view av add cache measure group all levels (x) materialized using a.b.c")
        assertThat(p).notMatches("alter analytic view av add cache measure group all levels (x) materialized using t@dbl")
        assertThat(p).notMatches("alter analytic view av add cache measure group all levels (x) materialized using t, u")
        assertThat(p).notMatches("alter analytic view av drop cache measure group all levels (x) materialized")
        assertThat(p).notMatches("alter analytic view av drop cache measure group all levels (x) using t")
        assertThat(p).notMatches("alter analytic view av add cache")
        assertThat(p).notMatches("alter analytic view av drop cache")
        assertThat(p).notMatches("alter analytic view av add cache measure group all materialized")
        assertThat(p).notMatches("alter analytic view av drop cache measure group (sales)")
        assertThat(p).notMatches("alter analytic view av add cache levels (x) materialized")
        assertThat(p).notMatches("alter analytic view av drop cache levels (x) measure group (sales)")
        assertThat(p).notMatches("alter analytic view av add cache measure group (sales) measure group (units) levels (y) materialized")
        assertThat(p).notMatches("alter analytic view av drop cache measure group (sales) levels (x) levels (y)")
        assertThat(p).notMatches(
            "alter analytic view av add cache measure group (sales) levels (x) materialized, " +
                "measure group (units) levels (y) materialized")
        assertThat(p).notMatches("alter analytic view av drop cache measure group (sales) levels (x), measure group (units) levels (y)")
        assertThat(p).notMatches(
            "alter analytic view av add cache measure group (sales) levels (x) materialized " +
                "measure group (units) levels (y) materialized")
    }

    @Test
    fun rejectsMultipleActions() {
        setRootRule(DdlGrammar.ALTER_ANALYTIC_VIEW)
        assertThat(p).notMatches("alter analytic view av compile add cache measure group all levels (x) materialized")
        assertThat(p).notMatches(
            "alter analytic view av add cache measure group all levels (x) materialized drop cache measure group all levels (x)")
        assertThat(p).notMatches("alter analytic view av add cache measure group all levels (x) materialized compile")
        assertThat(p).notMatches("alter analytic view av drop cache measure group all levels (x) compile")
    }

    @Test
    fun routesThroughDdlCommand() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("alter analytic view sales_av rename to mysales_av;")
        assertThat(p).matches("alter analytic view av add cache measure group all levels (x) materialized;")
        assertThat(p).matches("alter analytic view av drop cache measure group (m) levels (d.h.l);")
    }
}
