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
package com.felipebz.zpa.api.units

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateMaterializedViewTest : RuleTest() {
    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.CREATE_MATERIALIZED_VIEW)
    }

    @Test
    fun matchesSimpleMaterializedView() {
        assertThat(p).matches("create materialized view foo as select 1 from dual;")
        assertThat(p).matches("create materialized view v as select 1 from dual;")
    }

    @Test
    fun matchesMaterializedViewStorageAndRefresh() {
        assertThat(p).matches(
            "create materialized view foo pctfree 0 tablespace dat3 refresh complete " +
                "start with sysdate+2/24 next trunc(sysdate)+1 as select 1 from dual;"
        )
        assertThat(p).matches(
            "create materialized view foo tablespace dat3 refresh complete as select 1 from dual;"
        )
    }

    @Test
    fun matchesIfNotExistsObjectTypeAndColumnAliases() {
        assertThat(p).matches(
            "create materialized view if not exists sch.foo of object_type (id, \"prod\") " +
                "default collation binary build immediate refresh fast on commit " +
                "as select 1, 2 from dual;"
        )
    }

    @Test
    fun matchesRefreshModes() {
        assertThat(p).matches(
            "create materialized view foo refresh force on demand with primary key " +
                "as select 1 from dual;"
        )
        assertThat(p).matches(
            "create materialized view foo refresh complete start with sysdate next trunc(sysdate)+1 " +
                "as select 1 from dual;"
        )
        assertThat(p).matches("create materialized view foo never refresh as select 1 from dual;")
        assertThat(p).matches(
            "create materialized view foo refresh fast on statement using trusted constraints " +
                "as select 1 from dual;"
        )
    }

    @Test
    fun matchesMaterializedViewAttributes() {
        assertThat(p).matches(
            "create materialized view foo pctfree 5 pctused 70 initrans 2 storage (initial 1k) " +
                "tablespace users using index initrans 2 storage (initial 1k) parallel 4 " +
                "build deferred refresh complete enable query rewrite as select 1 from dual;"
        )
        assertThat(p).matches(
            "create materialized view foo on prebuilt table with reduced precision using no index " +
                "enable on query computation enable concurrent refresh as select 1 from dual;"
        )
        assertThat(p).matches(
            "create materialized view foo segment creation deferred partition by range (id) " +
                "(partition p1 values less than (maxvalue)) as select 1 from dual;"
        )
    }

    @Test
    fun matchesRollbackAndEditionOptions() {
        assertThat(p).matches(
            "create materialized view foo refresh using default local rollback segment " +
                "enable query rewrite unusable before current edition " +
                "unusable beginning with null edition evaluate using edition v1 " +
                "as select 1 from dual;"
        )
        assertThat(p).matches(
            "create materialized view foo refresh using master rollback segment rb " +
                "as select 1 from dual;"
        )
    }

    @Test
    fun matchesEncryptedAndScopedColumns() {
        assertThat(p).matches(
            "create materialized view sales_mv " +
                "(year encrypt annotations (hidden), " +
                "prod encrypt using 'some_algorithm' identified by pass 'int_algorithm' no salt, " +
                "scope for (ref_col) is schema.table_or_alias) " +
                "build immediate refresh fast on commit as select 1, 2, 3 from dual;"
        )
    }

    @Test
    fun rejectsIncompleteMaterializedViewOptions() {
        assertThat(p).notMatches("create materialized view foo refresh as select 1 from dual;")
        assertThat(p).notMatches("create materialized view foo on prebuilt as select 1 from dual;")
        assertThat(p).notMatches("create materialized view foo (id,) as select 1 from dual;")
        assertThat(p).notMatches(
            "create materialized view foo (scope for (ref_col) is) as select 1 from dual;"
        )
    }
}
