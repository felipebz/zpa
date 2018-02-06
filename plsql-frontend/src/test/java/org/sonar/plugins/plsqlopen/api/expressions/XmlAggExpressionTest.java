package org.sonar.plugins.plsqlopen.api.expressions;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class XmlAggExpressionTest extends RuleTest {
    
    @Before
    public void init() {
        setRootRule(PlSqlGrammar.EXPRESSION);
    }
    
    @Test
    public void matchesSimpleXmlAgg() {
        assertThat(p).matches("xmlagg(foo)");
    }
    
    @Test
    public void matchesXmlAggWithOrderBy() {
        assertThat(p).matches("xmlagg(foo order by bar)");
    }

}
