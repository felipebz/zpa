package br.com.felipezorzo.sonar.plsql.api.expressions;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class DateExpressionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.EXPRESSION);
    }
    
    @Test
    public void matchesDateLiteralAddition() {
        assertThat(p).matches("DATE '2015-01-01' + 1");
    }
    
    @Test
    public void matchesDateLiteralSubtraction() {
        assertThat(p).matches("DATE '2015-01-01' - 1");
    }
    
}
