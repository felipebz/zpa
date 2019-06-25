/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.declarations

import org.junit.Before
import org.junit.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import org.sonar.sslr.tests.Assertions.assertThat

class VariableDeclarationTest : RuleTest() {

    @Before
    fun init() {
        setRootRule(PlSqlGrammar.VARIABLE_DECLARATION)
    }

    @Test
    fun matchesSimpleDeclaration() {
        assertThat(p).matches("var number;")
    }

    @Test
    fun matchesDeclarationWithPrecision() {
        assertThat(p).matches("var number(1);")
    }

    @Test
    fun matchesDeclarationWithPrecisionAndScale() {
        assertThat(p).matches("var number(1,1);")
    }

    @Test
    fun matchesDeclarationWithInitialization() {
        assertThat(p).matches("var number := 1;")
        assertThat(p).matches("var varchar2(1) := 'a';")
        assertThat(p).matches("var boolean := true;")
        assertThat(p).matches("var boolean := var2;")
    }

    @Test
    fun matchesDeclarationWithDefaultValue() {
        assertThat(p).matches("var number default 1;")
        assertThat(p).matches("var varchar2(1) default 'a';")
        assertThat(p).matches("var boolean default true;")
        assertThat(p).matches("var boolean default var2;")
    }

    @Test
    fun matchesDeclarationWithNotNullConstraint() {
        assertThat(p).matches("var number not null := 1;")
        assertThat(p).matches("var number not null default 1;")
    }

    @Test
    fun matchesDeclarationExplicitNullable() {
        assertThat(p).matches("var number null := 1;")
        assertThat(p).matches("var number null default 1;")
    }

    @Test
    fun matchesTypeAnchoredDeclaration() {
        assertThat(p).matches("var custom%type;")
    }

    @Test
    fun matchesObjectDeclaration() {
        assertThat(p).matches("var custom;")
    }

    @Test
    fun matchesObjectDeclarationWithPackage() {
        assertThat(p).matches("var pack.custom;")
    }

    @Test
    fun matchesObjectDeclarationWithPackageAndSchema() {
        assertThat(p).matches("var sch.pack.custom;")
    }

    @Test
    fun matchesTableAnchoredDeclaration() {
        assertThat(p).matches("var tab%rowtype;")
    }

    @Test
    fun matchesTableColumnAnchoredDeclaration() {
        assertThat(p).matches("var tab.column%type;")
    }

    @Test
    fun matchesRefObjectDeclaration() {
        assertThat(p).matches("var ref custom;")
    }

    @Test
    fun matchesSimpleConstant() {
        assertThat(p).matches("var constant number := 1;")
        assertThat(p).matches("var constant number default 1;")
    }

    @Test
    fun matchesSimpleConstantWithConstraints() {
        assertThat(p).matches("var constant number not null := 1;")
        assertThat(p).matches("var constant number not null default 1;")
    }

    @Test
    fun matchesRemoteTableColumnAnchoredDeclaration() {
        assertThat(p).matches("var tab.col@link%type;")
        assertThat(p).matches("var tab.col@link.domain.com%type;")
    }

    @Test
    fun matchesExceptionDeclaration() {
        assertThat(p).matches("var exception;")
    }

}
