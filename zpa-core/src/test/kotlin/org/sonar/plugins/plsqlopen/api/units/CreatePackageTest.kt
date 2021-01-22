/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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

import org.junit.Before
import org.junit.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import org.sonar.sslr.tests.Assertions.assertThat

class CreatePackageTest : RuleTest() {

    @Before
    fun init() {
        setRootRule(PlSqlGrammar.CREATE_PACKAGE)
    }

    @Test
    fun matchesSimplePackage() {
        assertThat(p).matches(""
                + "create package test is\n"
                + "end;")
    }

    @Test
    fun matchesSimplePackageAlternative() {
        assertThat(p).matches(""
                + "create package test as\n"
                + "end;")
    }

    @Test
    fun matchesSimplePackageWithNameAtEnd() {
        assertThat(p).matches(""
                + "create package test is\n"
                + "end test;")
    }

    @Test
    fun matchesSimpleCreateOrReplacePackage() {
        assertThat(p).matches(""
                + "create or replace package test is\n"
                + "end;")
    }

    @Test
    fun matchesPackageWithSchema() {
        assertThat(p).matches(""
                + "create package schema.test is\n"
                + "end;")
    }

    @Test
    fun matchesEditionablePackage() {
        assertThat(p).matches(""
                + "create editionable package test is\n"
                + "end;")
    }

    @Test
    fun matchesNonEditionablePackage() {
        assertThat(p).matches(""
                + "create noneditionable package test is\n"
                + "end;")
    }

    @Test
    fun matchesPackageWithSharingMetadata() {
        assertThat(p).matches(""
                + "create package test sharing = metadata is\n"
                + "end;")
    }

    @Test
    fun matchesPackageWithSharingNone() {
        assertThat(p).matches(""
                + "create package test sharing = none is\n"
                + "end;")
    }

    @Test
    fun matchesPackageWithAuthidCurrentUser() {
        assertThat(p).matches(""
                + "create package test authid current_user is\n"
                + "end;")
    }

    @Test
    fun matchesPackageWithAuthidDefiner() {
        assertThat(p).matches(""
                + "create package test authid definer is\n"
                + "end;")
    }

    @Test
    fun matchesPackageWithDefaultCollation() {
        assertThat(p).matches(""
                + "create package test default collation using_nls_comp is\n"
                + "end;")
    }

    @Test
    fun matchesPackageWithAccessibleBy() {
        assertThat(p).matches(""
                + "create package test accessible by (package other_package) is\n"
                + "end;")
    }

    @Test
    fun matchesPackageWithProcedure() {
        assertThat(p).matches(""
                + "create package test is\n"
                + "procedure proc;\n"
                + "end;")
    }

    @Test
    fun matchesPackageWithFunction() {
        assertThat(p).matches(""
                + "create package test is\n"
                + "function func return number;\n"
                + "end;")
    }

    @Test
    fun matchesPackageWithTimestamp() {
        assertThat(p).matches(""
                + "create package test timestamp '2015-01-01' is\n"
                + "end;")
    }

}
