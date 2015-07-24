package br.com.felipezorzo.sonar.plsql.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class SavepointStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.SAVEPOINT_STATEMENT);
    }
    
    @Test
    public void matchesSavepoint() {
        assertThat(p).matches("savepoint save;");
    }
    
    @Test
    public void matchesLabeledSavepoint() {
        assertThat(p).matches("<<foo>> savepoint save;");
    }

}
