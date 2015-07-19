package br.com.felipezorzo.sonar.plsql.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class ExitStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.EXIT_STATEMENT);
    }

    @Test
    public void matchesExit() {
        assertThat(p).matches("exit;");
    }
    
    @Test
    public void matchesExitWhen() {
        assertThat(p).matches("exit when true;");
    }

}
