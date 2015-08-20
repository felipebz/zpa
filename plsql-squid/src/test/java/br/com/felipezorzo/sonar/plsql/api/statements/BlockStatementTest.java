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

public class BlockStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.BLOCK_STATEMENT);
    }

    @Test
    public void matchesSimpleBlock() {
        assertThat(p).matches("begin null; end;");
        assertThat(p).matches("BEGIN NULL; END;");
    }
    
    @Test
    public void matchesNestedBlock() {
        assertThat(p).matches("begin begin null; end; end;");
    }
    
    @Test
    public void matchesBlockWithMultipleStatements() {
        assertThat(p).matches("begin null; null; end;");
    }
    
    @Test
    public void matchesBlockWithOneExceptionHandler() {
        assertThat(p).matches("begin null; exception when others then null; end;");
    }
    
    @Test
    public void matchesBlockWithMultipleExceptionHandler() {
        assertThat(p).matches("begin null; exception when others then null; when others then null; end;");
    }
    
    @Test
    public void matchesBlockWithNameAtEnd() {
        assertThat(p).matches("begin null; end block_name;");
    }
    
    @Test
    public void matchesBlockWithDeclareSection() {
        assertThat(p).matches("declare var number; begin null; end block_name;");
    }
    
    @Test
    public void matchesBlockWithDeclareSectionWithoutDeclarations() {
        assertThat(p).matches("declare begin null; end block_name;");
    }
    
    @Test
    public void notMatchesBlockWithoutStatements() {
        assertThat(p).notMatches("begin end;");
    }
    
    @Test
    public void notMatchesBlockWithIncompleteExceptionHandler() {
        assertThat(p).notMatches("begin null; exception end;");
    }
    
    @Test
    public void matchesLabeledBlock() {
        assertThat(p).matches("<<foo>> begin null; end foo;");
    }
}
