//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.units

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreatePackageTest : RuleTest() {

    @BeforeEach
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
