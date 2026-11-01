//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.declarations

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VariableDeclarationTest : RuleTest() {

    @BeforeEach
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
    fun matchesOutIdentifierDeclaration() {
        assertThat(p).matches("out number;")
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
