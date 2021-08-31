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
package org.sonar.plugins.plsqlopen.api.ddl

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.DdlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import org.sonar.sslr.tests.Assertions.assertThat

class AlterPlsqlUnitTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_PLSQL_UNIT)
    }

    @Test
    fun matchesAlterTriggerDisable() {
        assertThat(p).matches("alter trigger foo disable;")
    }

    @Test
    fun matchesAlterTriggerEnable() {
        assertThat(p).matches("alter trigger foo enable;")
    }

    @Test
    fun matchesAlterTriggerDisableWithSchema() {
        assertThat(p).matches("alter trigger foo.bar disable;")
    }

    @Test
    fun matchesAlterTriggerDisableWithQuotedIdentifier() {
        assertThat(p).matches("alter trigger \"foo\" rename to \"bar\";")
    }

    @Test
    fun matchesAlterTriggerCompile() {
        assertThat(p).matches("alter trigger foo compile;")
    }

    @Test
    fun matchesAlterProcedureCompile() {
        assertThat(p).matches("alter procedure foo compile;")
    }

    @Test
    fun matchesAlterFunctionCompile() {
        assertThat(p).matches("alter function foo compile;")
    }

    @Test
    fun matchesAlterPackageCompile() {
        assertThat(p).matches("alter package foo compile;")
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
}
