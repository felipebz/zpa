package br.com.felipezorzo.sonar.plsql.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

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
