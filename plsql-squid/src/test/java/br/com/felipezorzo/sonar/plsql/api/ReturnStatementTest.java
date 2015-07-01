package br.com.felipezorzo.sonar.plsql.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

public class ReturnStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.RETURN_STATEMENT);
    }

    @Test
    public void matchesSimpleReturn() {
        assertThat(p).matches("return;");
    }
    
    @Test
    public void matchesReturnWithValue() {
        assertThat(p).matches("return 1;");
    }

}
