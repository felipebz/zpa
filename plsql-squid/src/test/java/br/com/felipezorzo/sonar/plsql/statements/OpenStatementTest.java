package br.com.felipezorzo.sonar.plsql.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class OpenStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.OPEN_STATEMENT);
    }
    
    @Test
    public void matchesSimpleOpen() {
        assertThat(p).matches("open cur;");
    }
    
    @Test
    public void matchesOpenWithParameter() {
        assertThat(p).matches("open cur(foo);");
    }
    
    @Test
    public void matchesOpenWithMultipleParameters() {
        assertThat(p).matches("open cur(foo, bar);");
    }
    
    @Test
    public void matchesLabeledOpen() {
        assertThat(p).matches("<<foo>> open cur;");
    }

}
