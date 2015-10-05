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
package br.com.felipezorzo.sonar.plsql.api.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class AssignmentStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.ASSIGNMENT_STATEMENT);
    }
    
    @Test
    public void assignmentToVariable() {
        assertThat(p).matches("var := 1;");
    }
    
    @Test
    public void assignmentToRecordAttribute() {
        assertThat(p).matches("rec.attribute := 1;");
    }
    
    @Test
    public void assignmentToCollectionElement() {
        assertThat(p).matches("collection(1) := 1;");
    }
    
    @Test
    public void assignmentToItemInRecordCollection() {
        assertThat(p).matches("collection(1).field := 1;");
    }
    
    @Test
    public void assignmentToHostVariable() {
        assertThat(p).matches(":var := 1;");
    }
    
    @Test
    public void assignmentToIndicatorVariable() {
        assertThat(p).matches(":var:indicator := 1;");
    }
    
    @Test
    public void matchesLabeledAssignment() {
        assertThat(p).matches("<<foo>> var := 1;");
    }

}
