/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.api.statements

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class IfStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.IF_STATEMENT)
    }

    @Test
    fun matchesSimpleIf() {
        assertThat(p).matches(""
                + "if true then "
                + "null; "
                + "end if;")
    }

    @Test
    fun matchesIfWithElsif() {
        assertThat(p).matches(""
                + "if true then "
                + "null; "
                + "elsif true then "
                + "null; "
                + "end if;")
    }

    @Test
    fun matchesIfWithMultipleElsif() {
        assertThat(p).matches(""
                + "if true then "
                + "null; "
                + "elsif true then "
                + "null; "
                + "elsif true then "
                + "null; "
                + "end if;")
    }

    @Test
    fun matchesIfWithElse() {
        assertThat(p).matches(""
                + "if true then "
                + "null; "
                + "else "
                + "null; "
                + "end if;")
    }

    @Test
    fun matchesIfWithElsifAndElse() {
        assertThat(p).matches(""
                + "if true then "
                + "null; "
                + "elsif true then "
                + "null; "
                + "else "
                + "null; "
                + "end if;")
    }

    @Test
    fun matchesNestedIf() {
        assertThat(p).matches(""
                + "if true then "
                + "if true then "
                + "null; "
                + "end if;"
                + "end if;")
    }

    @Test
    fun matchesLabeledIf() {
        assertThat(p).matches(""
                + "<<foo>> if true then "
                + "null; "
                + "end if foo;")
    }

}
