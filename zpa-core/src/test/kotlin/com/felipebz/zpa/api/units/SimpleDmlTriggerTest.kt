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
package com.felipebz.zpa.api.units

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class SimpleDmlTriggerTest : RuleTest() {

    private val body = " begin null; end;"

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.SIMPLE_DML_TRIGGER)
    }

    @Test
    fun matchesBefore() {
        assertThat(p).matches("before insert on tab$body")
    }

    @Test
    fun matchesAfter() {
        assertThat(p).matches("after insert on tab$body")
    }

    @Test
    fun matchesUpdate() {
        assertThat(p).matches("before update on tab$body")
    }

    @Test
    fun matchesUpdateWithOneColumn() {
        assertThat(p).matches("before update of col1 on tab$body")
    }

    @Test
    fun matchesInsertOfWithOneColumn() {
        assertThat(p).matches("before insert of col1 on tab$body")
    }

    @Test
    fun matchesDeleteOfWithOneColumn() {
        assertThat(p).matches("before delete of col1 on tab$body")
    }

    @Test
    fun matchesUpdateWithMultipleColumns() {
        assertThat(p).matches("before update of col1, col2 on tab$body")
    }

    @Test
    fun matchesDelete() {
        assertThat(p).matches("before delete on tab$body")
    }

    @Test
    fun matchesReferencing() {
        assertThat(p).matches("after insert on tab referencing old as foo$body")
    }

    @Test
    fun matchesMultipleReferencingClause() {
        assertThat(p).matches("after insert on tab referencing old foo new as bar parent as bar$body")
    }

    @Test
    fun matchesEmptyReferencing() {
        assertThat(p).matches("after insert on tab referencing$body")
    }

    @Test
    fun matchesForEachRow() {
        assertThat(p).matches("after insert on tab for each row$body")
    }

    @Test
    fun matchesTriggerEditionForward() {
        assertThat(p).matches("after insert on tab forward crossedition$body")
    }

    @Test
    fun matchesTriggerEditionReverse() {
        assertThat(p).matches("after insert on tab reverse crossedition$body")
    }

    @Test
    fun matchesTriggerOrderingFollows() {
        assertThat(p).matches("after insert on tab follows foo$body")
    }

    @Test
    fun matchesTriggerOrderingPrecedes() {
        assertThat(p).matches("after insert on tab precedes foo$body")
    }

    @Test
    fun matchesTriggerOrderingFollowsMultiple() {
        assertThat(p).matches("after insert on tab follows foo, bar$body")
    }

}
