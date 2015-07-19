package br.com.felipezorzo.sonar.plsql.sql;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class UpdateExpressionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.UPDATE_EXPRESSION);
    }
    
    @Test
    public void matchesSimpleUpdate() {
        assertThat(p).matches("update tab set x = 1;");
    }
    
    @Test
    public void matchesUpdateWithWhere() {
        assertThat(p).matches("update tab set x = 1 where y = 1;");
    }
    
    @Test
    public void matchesUpdateMultipleColumns() {
        assertThat(p).matches("update tab set x = 1, y = 1;");
    }

}
