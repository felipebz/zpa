//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AlterMaterializedViewTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.ALTER_MATERIALIZED_VIEW)
    }

    @Test
    fun matchesRefresh() {
        assertThat(p).matches("alter materialized view sales_by_month_by_state refresh fast;")
        assertThat(p).matches("alter materialized view mv refresh next sysdate+7")
        assertThat(p).matches("alter materialized view emp_data refresh complete start with trunc(sysdate+1) + 9/24 next sysdate+7;")
        assertThat(p).matches("alter materialized view mv refresh next sysdate + 7 start with sysdate complete")
        assertThat(p).matches("alter materialized view mv refresh on demand force with primary key")
        assertThat(p).matches("alter materialized view mv refresh using default master rollback segment")
        assertThat(p).matches("alter materialized view mv refresh using trusted constraints")
        assertThat(p).matches("alter materialized view mv never refresh")
    }

    @Test
    fun matchesOtherActions() {
        assertThat(p).matches("alter materialized view mv consider fresh;")
        assertThat(p).matches("alter materialized view order_data compile;")
        assertThat(p).matches("alter materialized view emp_data enable query rewrite;")
        assertThat(p).matches("alter materialized view mv disable query rewrite unusable before current edition")
        assertThat(p).matches("alter materialized view mv enable concurrent refresh")
        assertThat(p).matches("alter materialized view MView1 annotations(drop Snapshot);")
        assertThat(p).matches("alter materialized view if exists hr.mv pctfree 5 pctused 40 nocache parallel 2")
        assertThat(p).matches("alter materialized view mv nocompress nologging shrink space compact")
        assertThat(p).matches("alter materialized view mv no inmemory;")
        assertThat(p).matches("alter materialized view mv inmemory priority high no inmemory (c) refresh complete")
        assertThat(p).matches("alter materialized view mv allocate extent (size 1m) deallocate unused keep 1m")
        assertThat(p).matches("alter materialized view mv using index initrans 2 storage (next 1m) refresh fast")
        assertThat(p).matches("alter materialized view mv parallel 2 refresh complete evaluate using current edition " +
            "disable on query computation enable query rewrite annotations (a 'b')")
    }

    @Test
    fun rejectsFormsOracleRejects() {
        // ORA-03048 / ORA-00905 / ORA-00922.
        assertThat(p).notMatches("alter materialized view mv;")
        assertThat(p).notMatches("alter materialized view mv refresh")
        assertThat(p).notMatches("alter materialized view mv query rewrite")
        assertThat(p).notMatches("alter materialized view mv no inmemory priority high")
        // The groups keep their order and the final action is a single choice (ORA-03049 / ORA-00922).
        assertThat(p).notMatches("alter materialized view mv refresh complete parallel 2")
        assertThat(p).notMatches("alter materialized view mv compile refresh complete")
        assertThat(p).notMatches("alter materialized view mv enable query rewrite compile")
        assertThat(p).notMatches("alter materialized view mv compile consider fresh")
        assertThat(p).notMatches("alter materialized view mv annotations (a 'b') compile")
        // ORA-02243: PCTFREE is not an index option here.
        assertThat(p).notMatches("alter materialized view mv using index pctfree 5")
        // ALTER MATERIALIZED VIEW LOG is a different statement.
        assertThat(p).notMatches("alter materialized view log on t force")
    }
}
