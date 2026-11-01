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

class AlterPackageUnitTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_PACKAGE)
    }

    @Test
    fun matchesAlterPackageCompile() {
        assertThat(p).matches("alter package foo compile;")
    }

    @Test
    fun matchesAlterPackageIfExists() {
        assertThat(p).matches("alter package if exists foo compile;")
    }

    @Test
    fun matchesAlterPackageCompileBody() {
        assertThat(p).matches("alter package foo compile body;")
    }

    @Test
    fun matchesAlterPackageCompileBodyDebug() {
        assertThat(p).matches("alter package foo compile debug body;")
    }

    @Test
    fun matchesAlterPackageCompilePackage() {
        assertThat(p).matches("alter package foo compile package;")
    }

    @Test
    fun matchesAlterPackageCompileSpecification() {
        assertThat(p).matches("alter package foo compile specification;")
    }

    @Test
    fun matchesAlterPackageCompileSpecificationReuseSettings() {
        assertThat(p).matches("alter package foo compile debug specification reuse settings;")
    }

    @Test
    fun matchesAlterPackageCompileSpecificationWithOptionAndReuseSettings() {
        assertThat(p).matches("alter package foo compile debug specification plsql_ccflags='no_op:true' plsql_ccflags='no_op:true' reuse settings;")
    }


    @Test
    fun matchesCompilerParameterWithoutQuotes() {
        assertThat(p).matches("alter package pkg compile body plsql_optimize_level=3")
        assertThat(p).matches("alter package pkg compile body plsql_code_type = native reuse settings")
    }

    @Test
    fun doesNotMatchACompilerParameterWithoutAValue() {
        assertThat(p).notMatches("alter package pkg compile body plsql_optimize_level=")
    }
}
