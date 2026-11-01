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

class OutOfLineConstraintTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.OUT_OF_LINE_CONSTRAINT)
    }

    @Test
    fun matchesUnique() {
        assertThat(p).matches("unique (foo)")
    }

    @Test
    fun matchesUniqueWithMoreColumns() {
        assertThat(p).matches("unique (foo, bar, baz)")
    }

    @Test
    fun matchesPrimaryKey() {
        assertThat(p).matches("primary key (foo)")
    }

    @Test
    fun matchesPrimaryKeyWithMoreColumns() {
        assertThat(p).matches("primary key (foo, bar, baz)")
    }

    @Test
    fun matchesForeignKey() {
        assertThat(p).matches("foreign key (col) references tab (col)")
    }

    @Test
    fun matchesReferencesWithMoreColumns() {
        assertThat(p).matches("foreign key (col, col2, col3) references tab (col, col2, col3)")
    }

    @Test
    fun matchesCheck() {
        assertThat(p).matches("check (x > 1)")
        assertThat(p).matches("check (x > 1) disable precheck")
        assertThat(p).matches("check (x > 1) precheck")
    }

    @Test
    fun matchesConstraintWithName() {
        assertThat(p).matches("constraint pk primary key (foo)")
        assertThat(p).matches("constraints pk primary key (foo)")
    }

    @Test

    fun matchesUniqueWithMultipleStates() {
        assertThat(p).matches("unique (foo) initially deferred")
        assertThat(p).matches("unique (foo) initially immediate")
        assertThat(p).matches("unique (foo) initially deferred deferrable")
        assertThat(p).matches("unique (foo) initially deferred not deferrable")
        assertThat(p).matches("unique (foo) deferrable")
        assertThat(p).matches("unique (foo) not deferrable")
        assertThat(p).matches("unique (foo) deferrable initially deferred")
        assertThat(p).matches("unique (foo) not deferrable initially deferred")
        assertThat(p).matches("unique (foo) rely")
        assertThat(p).matches("unique (foo) norely")
        assertThat(p).matches("unique (foo) enable")
        assertThat(p).matches("unique (foo) disable")
        assertThat(p).matches("unique (foo) validate")
        assertThat(p).matches("unique (foo) novalidate")
        assertThat(p).matches("unique (foo) exceptions into sch.tab")
        assertThat(p).matches("unique (foo) not deferrable initially deferred rely disable validate exceptions into sch.tab")
    }

    @Test
    fun matchesUniqueUsingIndex() {
        assertThat(p).matches("unique (foo) using index")
    }

    @Test
    fun matchesUniqueUsingNamedIndex() {
        assertThat(p).matches("unique (foo) using index index_name")
        assertThat(p).matches("unique (foo) using index sch.index_name")
    }

    @Test
    fun matchesUniqueUsingIndexProperties() {
        assertThat(p).matches("unique (foo) using index pctfree 5 tablespace stocks storage (initial 8m)")
    }

    @Test
    fun matchesUniqueUsingCreateIndex() {
        assertThat(p).matches("unique (foo) using index (create unique index index_name on tab (foo))")
    }

    @Test
    fun acceptsLegacyComputeStatisticsInUnnamedAndInlineIndexes() {
        assertThat(p).matches("unique (foo) using index compute statistics")
        assertThat(p).matches("primary key (foo) using index pctfree 10 compute statistics tablespace users")
        assertThat(p).matches("primary key (foo) using index compute statistics pctfree 10")
        assertThat(p).matches("primary key (foo) using index compute statistics compute statistics")
        assertThat(p).matches("primary key (foo) using index (create unique index ix on tab(foo) compute statistics)")
        assertThat(p).matches("primary key (foo) using index (create index ix on tab(foo) compute statistics pctfree 5)")
    }

    @Test
    fun rejectsComputeStatisticsAfterNamedIndexAndMalformedClauses() {
        assertThat(p).notMatches("primary key (foo) using index ix compute statistics")
        assertThat(p).notMatches("primary key (foo) using index ix pctfree 10 compute statistics")
        assertThat(p).notMatches("primary key (foo) using index compute")
        assertThat(p).notMatches("primary key (foo) using index statistics")
        assertThat(p).notMatches("primary key (foo) using index compute pctfree 10 statistics")
        assertThat(p).notMatches("primary key (foo) using index (create index ix on tab(foo) compute)")
    }

    @Test
    fun rejectsUsingIndexOnUnsupportedConstraint() {
        assertThat(p).notMatches("foreign key (foo) references tab (foo) using index")
        assertThat(p).notMatches("check (foo > 1) using index")
    }

    @Test
    fun rejectsMalformedUsingIndex() {
        assertThat(p).notMatches("unique (foo) using index (")
        assertThat(p).notMatches("unique (foo) using index pctfree tablespace")
    }

    @Test
    fun rejectsProhibitedNestedCreateIndexSyntax() {
        assertThat(p).notMatches("unique (foo) using index (create unique index i on tab(foo) parallel 2)")
        assertThat(p).notMatches("unique (foo) using index (create index i on tab(foo) indextype is schema.my_indextype)")
        assertThat(p).notMatches("unique (foo) using index (create unique index i on tab(foo);)")
    }
}
