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
package org.sonar.plugins.plsqlopen.api.declarations;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class CallSpecificationTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.CALL_SPECIFICATION);
    }

    @Test
    public void matchesJavaCallSpec() {
        assertThat(p).matches("language java name 'foo';");
    }

    @Test
    public void matchesLanguageCNameLibrary() {
        assertThat(p).matches("language c name \"foo\" library bar;");
    }

    @Test
    public void matchesLanguageCLibraryName() {
        assertThat(p).matches("language c library bar name \"foo\";");
    }

    @Test
    public void matchesExternal() {
        assertThat(p).matches("external name \"foo\" library bar;");
    }

    @Test
    public void matchesAgentIn() {
        assertThat(p).matches("language c library bar name \"foo\" agent in (agent);");
    }

    @Test
    public void matchesAgentInMultiple() {
        assertThat(p).matches("language c library bar name \"foo\" agent in (agent, agent2);");
    }

    @Test
    public void matchesWithContext() {
        assertThat(p).matches("language c library bar name \"foo\" with context;");
    }

    @Test
    public void matchesParameterContext() {
        assertThat(p).matches("language c library bar name \"foo\" parameters (context);");
    }

    @Test
    public void matchesParameterSelf() {
        assertThat(p).matches("language c library bar name \"foo\" parameters (self);");
        assertThat(p).matches("language c library bar name \"foo\" parameters (self tdo);");
        assertThat(p).matches("language c library bar name \"foo\" parameters (self indicator);");
        assertThat(p).matches("language c library bar name \"foo\" parameters (self indicator struct);");
        assertThat(p).matches("language c library bar name \"foo\" parameters (self indicator tdo);");
        assertThat(p).matches("language c library bar name \"foo\" parameters (self length);");
        assertThat(p).matches("language c library bar name \"foo\" parameters (self duration);");
        assertThat(p).matches("language c library bar name \"foo\" parameters (self maxlen);");
        assertThat(p).matches("language c library bar name \"foo\" parameters (self charsetid);");
        assertThat(p).matches("language c library bar name \"foo\" parameters (self charsetform);");
    }

    @Test
    public void matchesParameterSimple() {
        assertThat(p).matches("language c library bar name \"foo\" parameters (x int);");
    }

    @Test
    public void matchesMultipleParameters() {
        assertThat(p).matches("language c library bar name \"foo\" parameters (x int, y int);");
    }

    @Test
    public void matchesParameterReturn() {
        assertThat(p).matches("language c library bar name \"foo\" parameters (return int);");
    }

    @Test
    public void matchesParameterWithProperty() {
        assertThat(p).matches("language c library bar name \"foo\" parameters (x length int);");
    }

    @Test
    public void matchesParameterByReference() {
        assertThat(p).matches("language c library bar name \"foo\" parameters (x by reference int);");
    }

}
