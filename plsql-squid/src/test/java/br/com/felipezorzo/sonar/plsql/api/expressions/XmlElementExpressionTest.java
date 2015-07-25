package br.com.felipezorzo.sonar.plsql.api.expressions;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class XmlElementExpressionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.EXPRESSION);
    }
    
    @Test
    public void matchesSimpleXmlElement() {
        assertThat(p).matches("xmlelement(\"xml\")");
    }
    
    @Test
    public void matchesXmlElementEntityEscaping() {
        assertThat(p).matches("xmlelement(entityescaping \"xml\")");
    }
    
    @Test
    public void matchesXmlElementEntityNoEscaping() {
        assertThat(p).matches("xmlelement(noentityescaping \"xml\")");
    }
    
    @Test
    public void matchesXmlElementExplicitNameKeyword() {
        assertThat(p).matches("xmlelement(name \"xml\")");
    }
    
    @Test
    public void matchesXmlElementEvalNameKeyword() {
        assertThat(p).matches("xmlelement(evalname \"xml\")");
    }
    
    @Test
    public void matchesXmlElementWithXmlAttributes() {
        assertThat(p).matches("xmlelement(\"xml\", xmlattributes(foo as \"bar\"))");
    }
    
    @Test
    public void matchesXmlElementWithValue() {
        assertThat(p).matches("xmlelement(\"xml\", foo)");
    }
    
    @Test
    public void matchesXmlElementWithValueAndIdentifier() {
        assertThat(p).matches("xmlelement(\"xml\", foo as \"bar\")");
    }
    
    @Test
    public void matchesXmlElementWithMultipleValues() {
        assertThat(p).matches("xmlelement(\"xml\", foo, bar, baz)");
    }

}
