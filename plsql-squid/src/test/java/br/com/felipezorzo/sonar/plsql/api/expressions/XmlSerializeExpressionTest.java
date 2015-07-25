package br.com.felipezorzo.sonar.plsql.api.expressions;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class XmlSerializeExpressionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.EXPRESSION);
    }
    
    @Test
    public void matchesSimpleXmlSerializeDocument() {
        assertThat(p).matches("xmlserialize(document 'foo')");
    }
    
    @Test
    public void matchesSimpleXmlSerializeContent() {
        assertThat(p).matches("xmlserialize(content 'foo')");
    }
    
    @Test
    public void matchesXmlSerializeAsDatatype() {
        assertThat(p).matches("xmlserialize(document 'foo' as clob)");
    }
    
    @Test
    public void matchesXmlSerializeWithEncoding() {
        assertThat(p).matches("xmlserialize(document 'foo' encoding 'utf-8')");
    }
    
    @Test
    public void matchesXmlSerializeWithVersion() {
        assertThat(p).matches("xmlserialize(document 'foo' version '1.0')");
    }
    
    @Test
    public void matchesXmlSerializeWithNoIdent() {
        assertThat(p).matches("xmlserialize(document 'foo' no ident)");
    }
    
    @Test
    public void matchesXmlSerializeWithIdent() {
        assertThat(p).matches("xmlserialize(document 'foo' ident)");
    }
    
    @Test
    public void matchesXmlSerializeWithIdentAndSize() {
        assertThat(p).matches("xmlserialize(document 'foo' ident size = 1)");
    }
    
    @Test
    public void matchesXmlSerializeWithHideDefaults() {
        assertThat(p).matches("xmlserialize(document 'foo' hide defaults)");
    }
    
    @Test
    public void matchesXmlSerializeWithShowDefaults() {
        assertThat(p).matches("xmlserialize(document 'foo' show defaults)");
    }
    
    @Test
    public void matchesLongXmlSerializeExpression() {
        assertThat(p).matches("xmlserialize(document 'foo' as clob encoding 'utf-8' version '1.0' ident size = 1 show defaults)");
    }

}
