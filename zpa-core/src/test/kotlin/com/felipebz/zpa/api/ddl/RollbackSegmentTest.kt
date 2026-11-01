//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.Test

class RollbackSegmentTest : RuleTest() {

    @Test
    fun matchesCreateRollbackSegment() {
        setRootRule(DdlGrammar.CREATE_ROLLBACK_SEGMENT)
        assertThat(p).matches("create rollback segment rbs_one;")
        assertThat(p).matches("create public rollback segment rbs_one tablespace rbs_ts")
        assertThat(p).matches("create rollback segment rbs_one tablespace rbs_ts storage (initial 10k next 10k maxextents unlimited);")
        assertThat(p).matches("create rollback segment rbs_one storage (optimal 1m minextents 2) tablespace rbs_ts")
        assertThat(p).matches("create rollback segment rbs_one storage (initial 10) storage (next 10k)")
    }

    @Test
    fun matchesAlterRollbackSegment() {
        setRootRule(DdlGrammar.ALTER_ROLLBACK_SEGMENT)
        assertThat(p).matches("alter rollback segment rbs_one online;")
        assertThat(p).matches("alter rollback segment rbs_one offline")
        assertThat(p).matches("alter rollback segment rbs_one shrink")
        assertThat(p).matches("alter rollback segment rbs_one shrink to 100m")
        assertThat(p).matches("alter rollback segment rbs_one storage (next 1m optimal null)")
        assertThat(p).matches("alter public rollback segment rbs_one online")
    }

    @Test
    fun rejectsFormsOracleRejects() {
        setRootRule(DdlGrammar.CREATE_ROLLBACK_SEGMENT)
        // ORA-02176 / ORA-02145: only TABLESPACE and a non-empty STORAGE clause are options.
        assertThat(p).notMatches("create rollback segment rbs_one logging")
        assertThat(p).notMatches("create rollback segment rbs_one storage ()")

        setRootRule(DdlGrammar.ALTER_ROLLBACK_SEGMENT)
        // ORA-02244 without an option or with TABLESPACE; ORA-03048/ORA-03049 at a second option.
        assertThat(p).notMatches("alter rollback segment rbs_one")
        assertThat(p).notMatches("alter rollback segment rbs_one tablespace ts")
        assertThat(p).notMatches("alter rollback segment rbs_one online offline")
        assertThat(p).notMatches("alter rollback segment rbs_one online storage (next 1m)")
        // ORA-01657: TO needs a size.
        assertThat(p).notMatches("alter rollback segment rbs_one shrink to")
    }
}
