//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InlineConstraintTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.INLINE_CONSTRAINT)
    }

    @Test
    fun matchesNotNull() {
        assertThat(p).matches("not null")
    }

    @Test
    fun matchesNull() {
        assertThat(p).matches("null")
    }

    @Test
    fun matchesUnique() {
        assertThat(p).matches("unique")
    }

    @Test
    fun matchesPrimaryKey() {
        assertThat(p).matches("primary key")
    }

    @Test
    fun matchesReferences() {
        assertThat(p).matches("references tab (col)")
    }

    @Test
    fun matchesReferencesWithSchema() {
        assertThat(p).matches("references sch.tab (col)")
    }

    @Test
    fun matchesReferencesWithMoreColumns() {
        assertThat(p).matches("references tab (col, col2, col3)")
    }

    @Test
    fun matchesReferencesOnDeleteCascade() {
        assertThat(p).matches("references tab (col) on delete cascade")
    }

    @Test
    fun matchesReferencesOnDeleteSetNull() {
        assertThat(p).matches("references tab (col) on delete set null")
    }

    @Test
    fun matchesCheck() {
        assertThat(p).matches("check (x > 1)")
        assertThat(p).matches("check (x > 1) disable precheck")
        assertThat(p).matches("check (x > 1) precheck")
    }

    @Test
    fun matchesConstraintWithName() {
        assertThat(p).matches("constraint pk primary key")
        assertThat(p).matches("constraints pk primary key")
    }

    @Test
    fun matchesUniqueWithMultipleStates() {
        assertThat(p).matches("unique initially deferred")
        assertThat(p).matches("unique initially immediate")
        assertThat(p).matches("unique initially deferred deferrable")
        assertThat(p).matches("unique initially deferred not deferrable")
        assertThat(p).matches("unique deferrable")
        assertThat(p).matches("unique not deferrable")
        assertThat(p).matches("unique deferrable initially deferred")
        assertThat(p).matches("unique not deferrable initially deferred")
        assertThat(p).matches("unique rely")
        assertThat(p).matches("unique norely")
        assertThat(p).matches("unique enable")
        assertThat(p).matches("unique disable")
        assertThat(p).matches("unique validate")
        assertThat(p).matches("unique novalidate")
        assertThat(p).matches("unique exceptions into sch.tab")
        assertThat(p).matches("unique not deferrable initially deferred rely disable validate exceptions into sch.tab")
    }

    @Test
    fun matchesKeyUsingIndex() {
        assertThat(p).matches("unique using index")
        assertThat(p).matches("primary key using index")
    }

    @Test
    fun rejectsUsingIndexOnUnsupportedInlineConstraint() {
        assertThat(p).notMatches("references tab (col) using index")
        assertThat(p).notMatches("check (x > 1) using index")
    }
}
