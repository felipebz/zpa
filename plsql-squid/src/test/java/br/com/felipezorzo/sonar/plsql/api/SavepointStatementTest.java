package br.com.felipezorzo.sonar.plsql.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

public class SavepointStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.SAVEPOINT_STATEMENT);
    }
    
    @Test
    public void matchesSavepoint() {
        assertThat(p).matches("savepoint save;");
    }

}
