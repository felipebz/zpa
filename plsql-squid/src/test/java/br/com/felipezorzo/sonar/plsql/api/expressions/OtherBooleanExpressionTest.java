package br.com.felipezorzo.sonar.plsql.api.expressions;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class OtherBooleanExpressionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.EXPRESSION);
    }
    
    @Test
    public void matchesCollectionExists() {
        assertThat(p).matches("collection.exists(0)");
    }
    
    @Test
    public void matchesCursorMethods() {
        assertThat(p).matches("cur%found");
        assertThat(p).matches("cur%notfound");
        assertThat(p).matches("cur%isopen");
    }
    
    @Test
    public void matchesHostCursorMethods() {
        assertThat(p).matches(":cur%found");
        assertThat(p).matches(":cur%notfound");
        assertThat(p).matches(":cur%isopen");
    }
    
    @Test
    public void matchesSqlMethods() {
        assertThat(p).matches("sql%found");
        assertThat(p).matches("sql%notfound");
        assertThat(p).matches("sql%isopen");
    }
    
    @Test
    public void matchesIsNull() {
        assertThat(p).matches("var is null");
    }
    
    @Test
    public void matchesIsNotNull() {
        assertThat(p).matches("var is not null");
    }
    
    @Test
    public void matchesLike() {
        assertThat(p).matches("var like 'test'");
    }
    
    @Test
    public void matchesNotLike() {
        assertThat(p).matches("var not like 'test'");
    }
    
    @Test
    public void matchesBetween() {
        assertThat(p).matches("var between 1 and 2");
    }
    
    @Test
    public void matchesNotBetween() {
        assertThat(p).matches("var not between 1 and 2");
    }
    
    @Test
    public void matchesBasicIn() {
        assertThat(p).matches("var in (1)");
    }
    
    @Test
    public void matchesBasicInWithMultipleValues() {
        assertThat(p).matches("var in (1, 2, 3)");
    }
    
    @Test
    public void matchesBasicInWithoutParenthesis() {
        assertThat(p).matches("var in 1");
    }
    
    @Test
    public void matchesEqualTo() {
        assertThat(p).matches("1 = 1");
    }
    
    @Test
    public void matchesNotEqualTo() {
        assertThat(p).matches("1 <> 2");
        assertThat(p).matches("1 != 2");
        assertThat(p).matches("1 ~= 2");
        assertThat(p).matches("1 ^= 2");
    }
    
    @Test
    public void matchesLessThan() {
        assertThat(p).matches("1 < 2");
    }
    
    @Test
    public void matchesGreaterThen() {
        assertThat(p).matches("2 > 1");
    }
    
    @Test
    public void matchesLessThanOrEqualTo() {
        assertThat(p).matches("1 <= 2");
    }
    
    @Test
    public void matchesGreaterThenOrEqualTo() {
        assertThat(p).matches("2 >= 1");
    }

}
