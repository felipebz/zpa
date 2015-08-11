/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015 Felipe Zorzo
 * felipe.b.zorzo@gmail.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package br.com.felipezorzo.sonar.plsql.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class ForStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.FOR_STATEMENT);
    }

    @Test
    public void matchesForLoop() {
        assertThat(p).matches(""
                + "for i in 1..2 loop "
                + "null; "
                + "end loop;");
    }
    
    @Test
    public void matchesReverseForLoop() {
        assertThat(p).matches(""
                + "for i in reverse 1..2 loop "
                + "null; "
                + "end loop;");
    }
    
    @Test
    public void matchesForInCursor() {
        assertThat(p).matches(""
                + "for i in cur loop "
                + "null; "
                + "end loop;");
    }
    
    @Test
    public void matchesForInCursorWithPackage() {
        assertThat(p).matches(""
                + "for i in pack.cur loop "
                + "null; "
                + "end loop;");
    }
    
    @Test
    public void matchesForInCursorWithPackageAndSchema() {
        assertThat(p).matches(""
                + "for i in sch.pack.cur loop "
                + "null; "
                + "end loop;");
    }
    
    @Test
    public void matchesForInCursorWithParameters() {
        assertThat(p).matches(""
                + "for i in cur(x) loop "
                + "null; "
                + "end loop;");
    }
    
    @Test
    public void matchesForInCursorWithMultipleParameters() {
        assertThat(p).matches(""
                + "for i in cur(x, y) loop "
                + "null; "
                + "end loop;");
    }
    
    @Test
    public void matchesForInCursorWithExplicitParameters() {
        assertThat(p).matches(""
                + "for i in cur(p1 => x) loop "
                + "null; "
                + "end loop;");
    }
    
    @Test
    public void matchesNestedForLoop() {
        assertThat(p).matches(""
                + "for i in 1..2 loop "
                + "for i in 1..2 loop "
                + "null; "
                + "end loop; "
                + "end loop;");
    }
    
    @Test
    public void matchesForLoopWithExpressionInBothSides() {
        assertThat(p).matches(""
                + "for i in 1 + 1 .. 2 + 2 loop "
                + "null; "
                + "end loop;");
    }
    
    @Test
    public void matchesLabeledForLoop() {
        assertThat(p).matches(""
                + "<<foo>> for i in 1..2 loop "
                + "null; "
                + "end loop foo;");
    }

}
