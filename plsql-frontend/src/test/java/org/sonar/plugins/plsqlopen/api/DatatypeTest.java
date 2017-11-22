/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2017 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

public class DatatypeTest extends RuleTest {
    
    @Before
    public void init() {
        setRootRule(PlSqlGrammar.DATATYPE);
    }

    @Test
    public void test() {
        assertThat(p).matches("number");
        assertThat(p).matches("number(10)");
        assertThat(p).matches("number(10, 2)");
        assertThat(p).matches("number($$len)");
        assertThat(p).matches("number($$len, $$decimals)");
        assertThat(p).matches("clob character set any_cs");
        assertThat(p).matches("clob character set str%charset");
        assertThat(p).matches("varchar2(10)");
        assertThat(p).matches("varchar2(10 char)");
        assertThat(p).matches("varchar2(10) character set any_cs");
        assertThat(p).matches("varchar2(10) character set str%charset");
        assertThat(p).matches("varchar2($$len)");
        assertThat(p).matches("date");
        assertThat(p).matches("timestamp");
        assertThat(p).matches("timestamp(3)");
        assertThat(p).matches("timestamp with time zone");
        assertThat(p).matches("timestamp(3) with time zone");
        assertThat(p).matches("timestamp with local time zone");
        assertThat(p).matches("interval year to month");
        assertThat(p).matches("interval year(2) to month");
        assertThat(p).matches("interval day to second");
        assertThat(p).matches("interval day(2) to second");
        assertThat(p).matches("interval day to second(6)");
        assertThat(p).matches("interval day(2) to second(6)");
    }
    
}
