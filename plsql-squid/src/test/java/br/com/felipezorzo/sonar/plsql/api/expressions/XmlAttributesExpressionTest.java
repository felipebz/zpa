package br.com.felipezorzo.sonar.plsql.api.expressions;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class XmlAttributesExpressionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.XMLATTRIBUTES_EXPRESSION);
    }
    
    @Test
    public void matchesSimpleXmlAttributes() {
        assertThat(p).matches("xmlattributes(foo)");
    }
    
    @Test
    public void matchesXmlAttributesEntityEscaping() {
        assertThat(p).matches("xmlattributes(entityescaping foo)");
    }
    
    @Test
    public void matchesXmlAttributesNoEntityEscaping() {
        assertThat(p).matches("xmlattributes(noentityescaping foo)");
    }
    
    @Test
    public void matchesXmlAttributesSchemaCheck() {
        assertThat(p).matches("xmlattributes(schemacheck foo)");
    }
    
    @Test
    public void matchesXmlAttributesNoSchemaCheck() {
        assertThat(p).matches("xmlattributes(noschemacheck foo)");
    }
    
    @Test
    public void matchesXmlAttributesWithExpression() {
        assertThat(p).matches("xmlattributes(foo as \"bar\")");
    }
    
    @Test
    public void matchesXmlAttributesWithEvalNameExpression() {
        assertThat(p).matches("xmlattributes(foo as evalname bar)");
    }
    
    @Test
    public void matchesXmlAttributesWithMultipleAttributes() {
        assertThat(p).matches("xmlattributes(foo as \"bar\", foo2 as \"bar2\")");
    }

}
