package br.com.felipezorzo.sonar.plsql.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class CloseStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.CLOSE_STATEMENT);
    }
    
    @Test
    public void matchesSimpleClose() {
        assertThat(p).matches("close foo;");
    }
    
    @Test
    public void matchesSimpleCloseHostCursor() {
        assertThat(p).matches("close :foo;");
    }
    
    @Test
    public void matchesLabeledClose() {
        assertThat(p).matches("<<foo>> close foo;");
    }

}
