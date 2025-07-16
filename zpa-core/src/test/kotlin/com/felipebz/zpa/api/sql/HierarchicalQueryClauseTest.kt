/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
package com.felipebz.zpa.api.sql

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.RuleTest

class HierarchicalQueryClauseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.HIERARCHICAL_QUERY_CLAUSE)
    }

    @Test
    fun matchesSimpleHierarchical() {
        assertThat(p).matches("connect by foo = bar")
    }

    @Test
    fun matchesHierarchicalQueryConnectByFirst() {
        assertThat(p).matches("connect by foo = bar start with foo = bar")
    }

    @Test
    fun matchesHierarchicalQueryStartWithFirst() {
        assertThat(p).matches("start with foo = bar connect by foo = bar")
    }

    @Test
    fun notMatchesStartWithFirstWithoutConnectBy() {
        assertThat(p).notMatches("start with foo = bar")
    }

}
