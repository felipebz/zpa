package br.com.felipezorzo.sonar.plsql.api.expressions;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class CharacterExpressionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.EXPRESSION);
    }
    
    @Test
    public void matchesSimpleConcatenation() {
        assertThat(p).matches("'a'||'b'");
    }
    
    @Test
    public void matchesMultipleConcatenation() {
        assertThat(p).matches("'a'||'b'||'c'");
    }
    
    @Test
    public void matchesVariableConcatenation() {
        assertThat(p).matches("var||var");
    }
    
    @Test
    public void matchesFunctionCallConcatenation() {
        assertThat(p).matches("func(var)||func(var)");
    }
    
    @Test
    public void matchesHostVariableConcatenation() {
        assertThat(p).matches(":var||:var");
    }
    
    @Test
    public void matchesIndicatorVariableConcatenation() {
        assertThat(p).matches(":var:indicator||:var:indicator");
    }
    
    @Test
    public void matchesReplace() {
        assertThat(p).matches("replace(var, 'x', 'y')");
    }
    
}
