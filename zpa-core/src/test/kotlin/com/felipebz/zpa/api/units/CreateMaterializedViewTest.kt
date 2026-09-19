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
}
