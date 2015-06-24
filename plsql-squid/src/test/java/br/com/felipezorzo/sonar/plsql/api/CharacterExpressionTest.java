package br.com.felipezorzo.sonar.plsql.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class CharacterExpressionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.CHARACTER_EXPRESSION);
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
    
    @Test @Ignore
    public void matchesFunctionCallConcatenation() {
        assertThat(p).matches("func(var)||func(var)");
    }
    
    @Test @Ignore
    public void matchesHostVariableConcatenation() {
        assertThat(p).matches(":var||:var");
    }
    
    @Test @Ignore
    public void matchesIndicatorVariableConcatenation() {
        assertThat(p).matches(":var:indicator||:var:indicator");
    }
}
