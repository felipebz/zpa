//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.sql

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
