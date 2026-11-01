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

class TruncateClusterTest : RuleTest() {
    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.TRUNCATE_CLUSTER)
    }

    @Test
    fun matchesStorageClauses() {
        assertThat(p).matches("truncate cluster personnel reuse storage;")
        assertThat(p).matches("truncate cluster personnel drop storage")
        assertThat(p).matches("truncate cluster hr.personnel")
    }

    @Test
    fun matchesDatabaseLinkQualifiedClusters() {
        // Oracle 26 parses these and fails later with ORA-02021.
        assertThat(p).matches("truncate cluster personnel@remote_db")
        assertThat(p).matches("truncate cluster hr.personnel@remote_db reuse storage;")
        assertThat(p).matches("truncate cluster personnel@remote_db.example.com drop storage")
        assertThat(p).matches("truncate cluster personnel@remote_db@conn")
        assertThat(p).matches("truncate cluster personnel@\"Remote\" preserve materialized view log")
        assertThat(p).matches("truncate cluster personnel @ remote_db")
    }

    @Test
    fun rejectsMalformedDatabaseLinkNames() {
        // ORA-01729 / ORA-02227 / ORA-03291
        assertThat(p).notMatches("truncate cluster personnel@")
        assertThat(p).notMatches("truncate cluster personnel@1")
        assertThat(p).notMatches("truncate cluster @remote_db")
        assertThat(p).notMatches("truncate cluster hr.@remote_db")
        assertThat(p).notMatches("truncate cluster a.b.personnel@remote_db")
    }

    @Test
    fun matchesUndocumentedMaterializedViewLogClause() {
        assertThat(p).matches("truncate cluster personnel preserve materialized view log")
        assertThat(p).matches("truncate cluster personnel purge materialized view log reuse storage")
        assertThat(p).matches("truncate cluster personnel reuse storage preserve materialized view log")
    }

    @Test
    fun rejectsInvalidTruncateClusterForms() {
        // ORA-03291
        assertThat(p).notMatches("truncate cluster personnel drop all storage")
        assertThat(p).notMatches("truncate cluster personnel cascade")
        assertThat(p).notMatches("truncate cluster personnel drop storage cascade")
        assertThat(p).notMatches("truncate cluster personnel storage")
        assertThat(p).notMatches("truncate cluster personnel drop")
        assertThat(p).notMatches("truncate cluster personnel reuse storage drop storage")
        assertThat(p).notMatches("truncate cluster a.b.personnel")
        assertThat(p).notMatches("truncate cluster if exists personnel")
        // ORA-00921
        assertThat(p).notMatches("truncate cluster personnel preserve")
        assertThat(p).notMatches("truncate cluster personnel purge")
    }

    @Test
    fun routesThroughDdlCommand() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("truncate cluster personnel reuse storage;")
        assertThat(p).matches("truncate table personnel drop all storage;")
    }
}
