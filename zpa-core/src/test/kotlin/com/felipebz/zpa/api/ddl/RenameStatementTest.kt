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

class RenameStatementTest : RuleTest() {
    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.RENAME_STATEMENT)
    }

    @Test
    fun matchesStandaloneFixturesAndNameBoundaries() {
        assertThat(p).matches("rename departments_new to emp_departments;")
        assertThat(p).matches("rename temporary to job_history;")
        assertThat(p).matches("rename FOO to BAR;")
        assertThat(p).matches("rename old_name to new_name")
        assertThat(p).matches("rename \"Mixed Case\" to \"New Name\";")
        // Oracle parses owner-qualified names, then rejects ownership with ORA-01765.
        assertThat(p).matches("rename hr.departments to new_name")
        assertThat(p).matches("rename old_name to hr.new_name")
        assertThat(p).matches("rename a.b.c.d to e.f.g")
        // Oracle 26 accepts @dblink only on the source, and renames the local object.
        assertThat(p).matches("rename old_name@remote to new_name")
        assertThat(p).matches("rename old_name@a.b.c to new_name")
        assertThat(p).matches("rename old_name@\"Remote\" to new_name")
    }

    @Test
    fun rejectsMalformedStatementAndUnsupportedModifiers() {
        for (sql in listOf(
            "rename",
            "rename a",
            "rename a b",
            "rename a to",
            "rename a to b to c",
            "rename if exists a to b",
            "rename a if exists to b",
            "rename table a to b",
            "rename view a to b",
            "rename a to b@remote",
            "rename a@ to b",
            "rename a@'remote' to b",
            "rename a..b to c",
            "rename a to b, c",
            "rename a, b to c"
        )) assertThat(p).notMatches(sql)
    }

    @Test
    fun routesOnlyStandaloneRenameThroughDdl() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("rename departments_new to emp_departments;")
        assertThat(p).matches("rename temporary to job_history;")
        assertThat(p).matches("rename FOO to BAR;")
        assertThat(p).notMatches("rename column a to b")
        assertThat(p).notMatches("rename a to b to c")
    }
}
