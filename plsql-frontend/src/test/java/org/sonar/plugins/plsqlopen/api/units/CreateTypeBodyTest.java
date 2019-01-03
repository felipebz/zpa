/*
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
package org.sonar.plugins.plsqlopen.api.units;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class CreateTypeBodyTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.CREATE_TYPE_BODY);
    }

    @Test
    public void matchesEditionableTypeBody() {
        assertThat(p).matches(
            "create editionable type body foo as "
                + "member procedure foo is "
                + "begin null; end; "
                + "end;");
    }

    @Test
    public void matchesNonEditionableTypeBody() {
        assertThat(p).matches(
            "create noneditionable type body foo as "
                + "member procedure foo is "
                + "begin null; end; "
                + "end;");
    }

    @Test
    public void matchesTypeBodyWithProcedure() {
        assertThat(p).matches(
                "create type body foo as "
                + "member procedure foo is "
                + "begin null; end; "
                + "end;");
    }
    
    @Test
    public void matchesTypeBodyWithFunction() {
        assertThat(p).matches(
                "create type body foo is "
                + "member function foo return number is "
                + "begin null; end; "
                + "end;");
    }
    
    @Test
    public void matchesTypeBodyWithStaticFunction() {
        assertThat(p).matches(
                "create type body foo is "
                + "static function foo return number is "
                + "begin null; end; "
                + "end;");
    }
    
    @Test
    public void matchesTypeBodyWithConstructor() {
        assertThat(p).matches(
                "create type body foo is "
                + "constructor function bar return self as result as "
                + "begin null; end; "
                + "end;");
    }
    
    @Test
    public void matchesTypeBodyWithOverridenFunction() {
        assertThat(p).matches(
                "create type body foo is "
                + "overriding member function foo return number is "
                + "begin null; end; "
                + "end;");
    }
}
