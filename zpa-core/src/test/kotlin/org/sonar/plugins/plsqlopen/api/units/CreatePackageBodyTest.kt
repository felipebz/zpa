/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2022 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.units

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest

class CreatePackageBodyTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.CREATE_PACKAGE_BODY)
    }

    @Test
    fun matchesSimplePackageBody() {
        assertThat(p).matches(""
                + "create package body test is\n"
                + "end;")
    }

    @Test
    fun matchesSimplePackageBodyAlternative() {
        assertThat(p).matches(""
                + "create package body test as\n"
                + "end;")
    }

    @Test
    fun matchesSimplePackageBodyWithNameAtEnd() {
        assertThat(p).matches(""
                + "create package body test is\n"
                + "end test;")
    }

    @Test
    fun matchesEditionablePackageBody() {
        assertThat(p).matches(""
                + "create editionable package body test is\n"
                + "end;")
    }

    @Test
    fun matchesNonEditionablePackageBody() {
        assertThat(p).matches(""
                + "create noneditionable package body test is\n"
                + "end;")
    }

    @Test
    fun matchesSimpleCreateOrReplacePackageBody() {
        assertThat(p).matches(""
                + "create or replace package body test is\n"
                + "end;")
    }

    @Test
    fun matchesPackageBodyWithSchema() {
        assertThat(p).matches(""
                + "create package body schema.test is\n"
                + "end;")
    }

    @Test
    fun matchesPackageBodyWithProcedure() {
        assertThat(p).matches(""
                + "create package body test is\n"
                + "procedure proc is\n"
                + "begin\n"
                + "null;\n"
                + "end;\n"
                + "end;")
    }

    @Test
    fun matchesPackageWithFunction() {
        assertThat(p).matches(""
                + "create package body test is\n"
                + "function func return number is\n"
                + "begin\n"
                + "return null;\n"
                + "end;\n"
                + "end;")
    }

    @Test
    fun matchesPackageWithFunctionAndResultCache() {
        assertThat(p).matches(""
                + "create package body test is\n"
                + "function func return number result_cache relies_on (tbl_test1,tbl_test2) is\n"
                + "begin\n"
                + "return null;\n"
                + "end;\n"
                + "end;")
    }

    @Test
    fun matchesPackageWithFunctionAndCursor() {
        assertThat(p).matches(""
                + "create package body test is\n"
                + "function func return number as\n"
                + "cursor c_data is\n"
                + "		(\n"
                + "		select abc from dual\n"
                + "		) order by abc;\n"
                + "begin\n"
                + "return null;\n"
                + "end;\n"
                + "end;")
    }

    @Test
    fun matchesPackageWithFunctionNamedExists() {
        assertThat(p).matches(""
                + "create package body test is\n"
                + "function exists return number is\n"
                + "begin\n"
                + "return null;\n"
                + "end;\n"
                + "end;")
    }

    @Test
    fun matchesPackageWithInitializationSection() {
        assertThat(p).matches(""
                + "create package body test is\n"
                + "var number;\n"
                + "begin\n"
                + "var := 0;\n"
                + "end;")
    }

    @Test
    fun matchesPackageBodyWithTimestamp() {
        assertThat(p).matches(""
                + "create package body test timestamp '2015-01-01' is\n"
                + "end;")
    }

}
