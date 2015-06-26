package br.com.felipezorzo.sonar.plsql.api.expressions;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class BooleanExpressionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.BOOLEAN_EXPRESSION);
    }
    
    @Test
    public void matchesSimpleAndExpression() {
        assertThat(p).matches("true and true");
    }
    
    @Test
    public void matchesSimpleOrExpression() {
        assertThat(p).matches("true or true");
    }
    
    @Test
    public void matchesSimpleNotExpression() {
        assertThat(p).matches("not true");
    }
    
    @Test
    public void matchesMultipleExpression() {
        assertThat(p).matches("true and true or true");
    }
    
    @Test
    public void matchesMultipleExpressionWithNotOperator() {
        assertThat(p).matches("true and not false");
    }
    
    @Test
    public void matchesExpressionWithVariables() {
        assertThat(p).matches("var and var");
    }
    
    @Test @Ignore
    public void matchesExpressionWithFunctionCall() {
        assertThat(p).matches("func(var) and func(var)");
    }
    
}
