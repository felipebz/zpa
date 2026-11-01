//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.Test

class ProfileTest : RuleTest() {

    @Test
    fun matchesCreateProfile() {
        setRootRule(DdlGrammar.CREATE_PROFILE)
        assertThat(p).matches(
            "create profile app_user limit sessions_per_user unlimited cpu_per_session unlimited " +
                "cpu_per_call 3000 connect_time 45 logical_reads_per_session default " +
                "logical_reads_per_call 1000 private_sga 15K composite_limit 5000000;")
        assertThat(p).matches(
            "create profile app_user2 limit failed_login_attempts 5 password_life_time 60 " +
                "password_verify_function ora12c_verify_function password_lock_time 1/24 " +
                "password_grace_time 10 inactive_account_time 30")
        assertThat(p).matches(
            "create mandatory profile c##cdb_profile limit password_verify_function my_mandatory_function container = all")
        assertThat(p).matches("create profile p limit password_rollover_time 1")
        assertThat(p).matches("create profile p limit private_sga 15 k password_verify_function sys.f")
        assertThat(p).matches("create profile p limit password_verify_function null password_rollover_time unlimited")
    }

    @Test
    fun matchesAlterProfile() {
        setRootRule(DdlGrammar.ALTER_PROFILE)
        assertThat(p).matches("alter profile new_profile limit password_reuse_time 90 password_reuse_max unlimited;")
        assertThat(p).matches("alter profile app_user limit password_reuse_time default")
        assertThat(p).matches("alter profile default limit idle_time 2")
        assertThat(p).matches("alter profile p limit idle_time unlimited container = current")
    }

    @Test
    fun rejectsMalformedProfiles() {
        setRootRule(DdlGrammar.CREATE_PROFILE)
        assertThat(p).notMatches("create profile p")
        assertThat(p).notMatches("create profile p limit")
        assertThat(p).notMatches("create profile p limit idle_time")
        assertThat(p).notMatches("create profile p container = current limit idle_time 2")
        // Oracle 26 rejects an expression for a resource limit.
        assertThat(p).notMatches("create profile p limit cpu_per_call 1+1")
        setRootRule(DdlGrammar.ALTER_PROFILE)
        assertThat(p).notMatches("alter profile p")
        assertThat(p).notMatches("alter mandatory profile p limit idle_time 2")
    }
}
