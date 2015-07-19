package br.com.felipezorzo.sonar.plsql.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class ContinueStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.CONTINUE_STATEMENT);
    }

    @Test
    public void matchesContinue() {
        assertThat(p).matches("continue;");
    }
    
    @Test
    public void matchesContinueWhen() {
        assertThat(p).matches("continue when true;");
    }

}
