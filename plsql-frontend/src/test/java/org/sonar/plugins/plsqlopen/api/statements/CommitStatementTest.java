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

public class CommitStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.COMMIT_STATEMENT);
    }
    
    @Test
    public void matchesSimpleCommit() {
        assertThat(p).matches("commit;");
    }
    
    @Test
    public void matchesCommitWork() {
        assertThat(p).matches("commit work;");
    }
    
    @Test
    public void matchesCommitForce() {
        assertThat(p).matches("commit force 'test';");
    }
    
    @Test
    public void matchesCommitForceWithScn() {
        assertThat(p).matches("commit force 'test',1;");
    }
    
    @Test
    public void matchesCommitWithComment() {
        assertThat(p).matches("commit comment 'test';");
    }
    
    @Test
    public void matchesCommitWrite() {
        assertThat(p).matches("commit write;");
    }
    
    @Test
    public void matchesCommitWriteImmediate() {
        assertThat(p).matches("commit write immediate;");
    }
    
    @Test
    public void matchesCommitWriteBatch() {
        assertThat(p).matches("commit write batch;");
    }
    
    @Test
    public void matchesCommitWriteWait() {
        assertThat(p).matches("commit write wait;");
    }
    
    @Test
    public void matchesCommitWriteNoWait() {
        assertThat(p).matches("commit write nowait;");
    }
    
    @Test
    public void matchesLongCommitStatement() {
        assertThat(p).matches("commit work comment 'teste' write immediate wait;");
    }
    
    @Test
    public void matchesLabeledCommit() {
        assertThat(p).matches("<<foo>> commit;");
    }

}
