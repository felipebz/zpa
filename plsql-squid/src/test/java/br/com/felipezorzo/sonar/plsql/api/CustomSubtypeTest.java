package br.com.felipezorzo.sonar.plsql.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

public class CustomSubtypeTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.CUSTOM_SUBTYPE);
    }

    @Test
    public void matchesSimpleSubtype() {
        assertThat(p).matches("subtype sub is char;");
    }
    
    @Test
    public void matchesSubtypeWithScale() {
        assertThat(p).matches("subtype sub is number(5);");
    }
    
    @Test
    public void matchesSubtypeWithScaleAndPrecision() {
        assertThat(p).matches("subtype sub is number(5,1);");
    }
    
    @Test
    public void matchesSubtypeWithNotNullConstraint() {
        assertThat(p).matches("subtype sub is number not null;");
    }

}
