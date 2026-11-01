//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.Test

class LibraryTest : RuleTest() {

    @Test
    fun matchesCreateLibrary() {
        setRootRule(DdlGrammar.CREATE_LIBRARY)
        assertThat(p).matches("create library if not exists ext_lib as 'ddl_1' in ddl_dir;")
        assertThat(p).matches("create or replace library ext_lib as 'ddl_1' in ddl_dir credential ddl_cred;")
        assertThat(p).matches("create library ext_lib as '/OR/lib/ext_lib.so';")
        assertThat(p).matches("create or replace library ext_lib is '/OR/newlib/ext_lib.so';")
        assertThat(p).matches("create library app_lib as '\${ORACLE_HOME}/lib/app_lib.so' agent 'sales.hq.example.com';")
        assertThat(p).matches("create or replace editionable library hr.lib sharing = metadata as 'x.so' " +
            "in dir agent 'db' credential hr.cred")
        assertThat(p).matches("create noneditionable library if not exists lib sharing = none is 'x.so'")
    }

    @Test
    fun rejectsInvalidCreateLibrary() {
        setRootRule(DdlGrammar.CREATE_LIBRARY)
        // ORA-11541
        assertThat(p).notMatches("create or replace library if not exists lib as 'x.so'")
        assertThat(p).notMatches("create library if exists lib as 'x.so'")
        assertThat(p).notMatches("create library lib as x_so")
        assertThat(p).notMatches("create library lib as 'x.so' in")
        assertThat(p).notMatches("create library lib as 'x.so' credential ddl_cred agent 'db'")
        assertThat(p).notMatches("create library lib as 'x.so' agent db")
        assertThat(p).notMatches("create library lib sharing = data as 'x.so'")
        assertThat(p).notMatches("create library a.b.lib as 'x.so'")
    }

    @Test
    fun matchesAlterLibrary() {
        setRootRule(DdlGrammar.ALTER_LIBRARY)
        assertThat(p).matches("alter library if exists hr.my_ext_lib compile;")
        assertThat(p).matches("alter library hr.my_ext_lib compile")
        assertThat(p).matches("alter library lib compile debug plsql_optimize_level = 2 reuse settings")
        assertThat(p).matches("alter library lib editionable")
        assertThat(p).matches("alter library lib noneditionable;")
    }

    @Test
    fun rejectsInvalidAlterLibrary() {
        setRootRule(DdlGrammar.ALTER_LIBRARY)
        // ORA-00922 / ORA-03049 / ORA-11544 / ORA-02000
        assertThat(p).notMatches("alter library lib")
        assertThat(p).notMatches("alter library lib recompile")
        assertThat(p).notMatches("alter library a.b.c compile")
        assertThat(p).notMatches("alter library lib compile editionable")
        assertThat(p).notMatches("alter library lib editionable compile")
        assertThat(p).notMatches("alter library lib compile package")
        assertThat(p).notMatches("alter library lib compile reuse")
        assertThat(p).notMatches("alter library if not exists lib compile")
    }

    @Test
    fun routesThroughDdlCommand() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("create library ext_lib as '/OR/lib/ext_lib.so';")
        assertThat(p).matches("alter library hr.my_ext_lib compile;")
    }
}
