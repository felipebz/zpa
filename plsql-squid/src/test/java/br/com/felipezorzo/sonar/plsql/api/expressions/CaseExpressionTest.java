package br.com.felipezorzo.sonar.plsql.api.expressions;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class CaseExpressionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.CASE_EXPRESSION);
    }

    @Test
    public void matchesSimpleSearchedCase() {
        assertThat(p).matches("case when x = 1 then 1 end");
    }
    
    @Test
    public void matchesSimpleCase() {
        assertThat(p).matches("case x when 1 then 1 end");
    }
    
    @Test
    public void matchesCaseWithMultipleWhen() {
        assertThat(p).matches("case x when 1 then 1 when 2 then 2 end");
    }
    
    @Test
    public void matchesCaseWithElse() {
        assertThat(p).matches("case x when 1 then 1 else 2 end");
    }
    
    @Test
    public void matchesCaseWithMemberIdentifier() {
        assertThat(p).matches("case foo.bar when 1 then 1 end");
    }

}
