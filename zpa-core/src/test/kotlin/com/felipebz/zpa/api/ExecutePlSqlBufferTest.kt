//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExecutePlSqlBufferTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXECUTE_PLSQL_BUFFER)
    }

    @Test
    fun matchesStandaloneSlashWithLeadingWhitespace() {
        listOf("/", " /", "    /", "\t/", "/   ", "  /   ", "\n  /").forEach { source ->
            assertThat(p).describedAs(source).matches(source)
        }
    }

    @Test
    fun rejectsSlashWithAnotherTokenOnTheSameLine() {
        assertThat(p).notMatches("/ select 1 from dual;")
        assertThat(p).notMatches("/;")
    }

    @Test
    fun matchesIndentedSlashAfterPlSqlUnit() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).matches(
            "begin\n" +
                "  null;\n" +
                "end;\n" +
                "  /"
        )
    }

    @Test
    fun preservesDivisionOperator() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).matches("select 10 / 2 from dual;")
        assertThat(p).notMatches("begin\n  null;\nend; /")
    }
}
