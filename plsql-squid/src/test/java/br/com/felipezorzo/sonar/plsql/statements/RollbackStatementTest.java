package br.com.felipezorzo.sonar.plsql.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class RollbackStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.ROLLBACK_STATEMENT);
    }
    
    @Test
    public void matchesSimpleRollback() {
        assertThat(p).matches("rollback;");
    }
    
    @Test
    public void matchesRollbackWork() {
        assertThat(p).matches("rollback work;");
    }
    
    @Test
    public void matchesRollbackForce() {
        assertThat(p).matches("rollback force 'test';");
    }
    
    @Test
    public void matchesRollbackToSavepoint() {
        assertThat(p).matches("rollback to save;");
    }
    
    @Test
    public void matchesRollbackToSavepointAlternative() {
        assertThat(p).matches("rollback to savepoint save;");
    }
    
    @Test
    public void matchesLongRollbackStatement() {
        assertThat(p).matches("rollback work to savepoint save;");
    }
    
    @Test
    public void matchesLabeledRollback() {
        assertThat(p).matches("<<foo>> rollback;");
    }

}
