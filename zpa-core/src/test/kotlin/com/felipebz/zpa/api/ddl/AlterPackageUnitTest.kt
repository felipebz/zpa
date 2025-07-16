/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest

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

}
