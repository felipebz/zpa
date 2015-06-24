package br.com.felipezorzo.sonar.plsql.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

public class BooleanExpressionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.BOOLEAN_EXPRESSION);
    }
    
    @Test
    public void matchesSimpleAndExpression() {
        assertThat(p).matches("TRUE AND TRUE");
    }
    
    @Test
    public void matchesSimpleOrExpression() {
        assertThat(p).matches("TRUE OR TRUE");
    }
    
    @Test
    public void matchesSimpleNotExpression() {
        assertThat(p).matches("NOT TRUE");
    }
    
    @Test
    public void matchesMultipleExpression() {
        assertThat(p).matches("TRUE AND TRUE OR TRUE");
    }
    
    @Test
    public void matchesMultipleExpressionWithNotOperator() {
        assertThat(p).matches("TRUE AND NOT FALSE");
    }
    
}
