package br.com.felipezorzo.sonar.plsql.api.expressions;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class XmlForestExpressionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.EXPRESSION);
    }
    
    @Test
    public void matchesSimpleXmlForest() {
        assertThat(p).matches("xmlforest(foo)");
    }
    
    @Test
    public void matchesXmlForestWithExpression() {
        assertThat(p).matches("xmlforest(foo as \"bar\")");
    }
    
    @Test
    public void matchesXmlForestWithEvalNameExpression() {
        assertThat(p).matches("xmlforest(foo as evalname bar)");
    }
    
    @Test
    public void matchesXmlForestWithMultipleAttributes() {
        assertThat(p).matches("xmlforest(foo as \"bar\", foo2 as \"bar2\")");
    }

}
