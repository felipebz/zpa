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
package org.sonar.plugins.plsqlopen.api.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class SetTransactionStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.SET_TRANSACTION_STATEMENT);
    }
    
    @Test
    public void matchesSetUnnamedReadOnlyTransaction() {
        assertThat(p).matches("set transaction read only;");
    }
    
    @Test
    public void matchesSetNamedReadOnlyTransaction() {
        assertThat(p).matches("set transaction read only name 'foo';");
    }
    
    @Test
    public void matchesSetUnnamedRedWriteTransaction() {
        assertThat(p).matches("set transaction read write;");
    }
    
    @Test
    public void matchesSetNamedRedWriteTransaction() {
        assertThat(p).matches("set transaction read write name 'foo';");
    }
    
    @Test
    public void matchesSetUnnamedSerializableTransaction() {
        assertThat(p).matches("set transaction isolation level serializable;");
    }
    
    @Test
    public void matchesSetNamedSerializableTransaction() {
        assertThat(p).matches("set transaction isolation level serializable name 'foo';");
    }
    
    @Test
    public void matchesSetUnnamedReadCommitedTransaction() {
        assertThat(p).matches("set transaction isolation level read committed;");
    }
    
    @Test
    public void matchesSetNamedReadCommitedTransaction() {
        assertThat(p).matches("set transaction isolation level read committed name 'foo';");
    }
    
    @Test
    public void matchesSetUnnamedTransactionWithFixedRollbackSegment() {
        assertThat(p).matches("set transaction use rollback segment foo;");
    }
    
    @Test
    public void matchesSetNamedTransactionWithFixedRollbackSegment() {
        assertThat(p).matches("set transaction use rollback segment foo name 'bar';");
    }
    
    @Test
    public void matchesSimpleSetTransactionName() {
        assertThat(p).matches("set transaction name 'foo';");
    }

    @Test
    public void doesNotMatchEmptySetTransaction() {
        assertThat(p).notMatches("set transaction;");
    }
    
    @Test
    public void matchesLabeledSetTransactionName() {
        assertThat(p).matches("<<foo>> set transaction name 'foo';");
    }

   
   
   
    
}
