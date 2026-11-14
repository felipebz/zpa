//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BindVariableNameTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
    }

    @Test
    fun matchesAReservedWordInPlSql() {
        assertThat(p).matches("begin\n  :b1.table := 1;\n  :b1.select := 2;\n  :table := 3;\nend;\n/\n")
        assertThat(p).matches("begin :table.select := :b1.from + :select; end;\n/\n")
        assertThat(p).matches("begin select 4 into :b1.select from dual where :b1.from = :table; end;\n/\n")
        assertThat(p).matches("procedure p_item is begin :ctl.date := sysdate; :ctl.order := null; end;")
        assertThat(p).matches("begin :b1.item := :b1.date; end;\n/\n")
    }

    @Test
    fun doesNotMatchAReservedWordInASqlStatementAlone() {
        // ORA-01745 for the bind variable's name, ORA-00919 after it.
        assertThat(p).notMatches("select :select from dual;")
        assertThat(p).notMatches("select :b1.select from dual;")
        assertThat(p).notMatches("update tab set val = :table;")
        assertThat(p).matches("select :b1, :b1.item from dual;")
    }
}
