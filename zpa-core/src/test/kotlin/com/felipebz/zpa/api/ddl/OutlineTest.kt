//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.Test

class OutlineTest : RuleTest() {

    @Test
    fun matchesCreateOutline() {
        setRootRule(DdlGrammar.CREATE_OUTLINE)
        assertThat(p).matches("create outline salaries for category special " +
            "on select last_name, salary from employees;")
        assertThat(p).matches("create or replace private outline my_salaries from salaries;")
        assertThat(p).matches("create or replace outline public_salaries from private my_salaries;")
        assertThat(p).matches("create public outline copied from public source_outline for category batch")
        assertThat(p).matches("create outline from source_outline")
        assertThat(p).matches("create outline on select 1 from dual")
    }

    @Test
    fun matchesEveryDocumentedOnStatementKind() {
        setRootRule(DdlGrammar.CREATE_OUTLINE)
        assertThat(p).matches("create outline o on delete from employees where employee_id = 1")
        assertThat(p).matches("create outline o on update employees set salary = salary + 1")
        assertThat(p).matches("create outline o on insert into employees_archive select * from employees")
        assertThat(p).matches("create outline o on create table employees_copy as select * from employees")
    }

    @Test
    fun rejectsInvalidCreateOutlineBoundaries() {
        setRootRule(DdlGrammar.CREATE_OUTLINE)
        assertThat(p).notMatches("create outline o")
        assertThat(p).notMatches("create outline o for category special")
        assertThat(p).notMatches("create outline o from source_outline on select 1 from dual")
        assertThat(p).notMatches("create outline o for category special from source_outline")
        assertThat(p).notMatches("create replace outline o from source_outline")
        assertThat(p).notMatches("create or outline o from source_outline")
        assertThat(p).notMatches("create outline private o from source_outline")
        assertThat(p).notMatches("create outline app.o from source_outline")
        assertThat(p).notMatches("create outline o from app.source_outline")
        assertThat(p).notMatches("create outline o for category app.special on select 1 from dual")
    }

    @Test
    fun rejectsUnsupportedOnStatements() {
        setRootRule(DdlGrammar.CREATE_OUTLINE)
        assertThat(p).notMatches("create outline o on insert into employees values (1)")
        assertThat(p).notMatches("create outline o on insert all into employees values (1) select 1 from dual")
        assertThat(p).notMatches("create outline o on create table employees_copy (id number)")
        assertThat(p).notMatches("create outline o on merge into employees using dual on (1 = 1) when matched then delete")
    }

    @Test
    fun matchesAlterOutlineActions() {
        setRootRule(DdlGrammar.ALTER_OUTLINE)
        assertThat(p).matches("alter outline salaries rebuild;")
        assertThat(p).matches("alter public outline salaries rename to current_salaries")
        assertThat(p).matches("alter private outline salaries change category to special")
        assertThat(p).matches("alter outline salaries rebuild rename to current_salaries " +
            "change category to special disable enable")
    }

    @Test
    fun rejectsInvalidAlterOutlineBoundaries() {
        setRootRule(DdlGrammar.ALTER_OUTLINE)
        assertThat(p).notMatches("alter outline salaries")
        assertThat(p).notMatches("alter outline private salaries rebuild")
        assertThat(p).notMatches("alter or replace outline salaries rebuild")
        assertThat(p).notMatches("alter outline app.salaries rebuild")
        assertThat(p).notMatches("alter outline salaries rename to app.current_salaries")
        assertThat(p).notMatches("alter outline salaries change category to app.special")
    }
}
