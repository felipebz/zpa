package br.com.felipezorzo.sonar.plsql.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class RaiseStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.RAISE_STATEMENT);
    }

    @Test
    public void matchesSimpleRaise() {
        assertThat(p).matches("raise;");
    }
    
    @Test
    public void matchesRaiseException() {
        assertThat(p).matches("raise ex;");
    }
    
    @Test
    public void matchesLabeledRaise() {
        assertThat(p).matches("<<foo>> raise;");
    }

}
