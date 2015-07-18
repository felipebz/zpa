package br.com.felipezorzo.sonar.plsql.api.expressions;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class NumericExpressionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.EXPRESSION);
    }
    
    @Test
    public void matchesNumericAddition() {
        assertThat(p).matches("1 + 1");
        assertThat(p).matches("1+1");
    }
    
    @Test
    public void matchesNumericSubtraction() {
        assertThat(p).matches("1 - 1");
        assertThat(p).matches("1-1");
    }
    
    @Test
    public void matchesNumericMultiplication() {
        assertThat(p).matches("1 * 1");
    }
    
    @Test
    public void matchesNumericDivision() {
        assertThat(p).matches("1 / 1");
    }
    
    @Test
    public void matchesNumericExponentiation() {
        assertThat(p).matches("1 ** 1");
    }
    
    @Test
    public void matchesCursorRowcount() {
        assertThat(p).matches("cur%rowcount + 1");
    }
    
    @Test
    public void matchesHostCursorRowcount() {
        assertThat(p).matches(":cur%rowcount + 1");
    }
    
    @Test
    public void matchesSqlRowcount() {
        assertThat(p).matches("sql%rowcount + 1");
    }
    
    @Test
    public void matchesSqlBulkRowcount() {
        assertThat(p).matches("sql%bulk_rowcount(1) + 1");
    }
    
    @Test
    public void matchesHostVariableExpression() {
        assertThat(p).matches(":var + 1");
    }
    
    @Test
    public void matchesIndicatorVariableExpression() {
        assertThat(p).matches(":var:indicator + 1");
    }
    
    @Test
    public void matchesVariableExpression() {
        assertThat(p).matches("var + 1");
    }
    
    @Test
    public void matchesFunctionCallExpression() {
        assertThat(p).matches("func(var) + 1");
    }
    
    @Test
    public void matchesCollectionCount() {
        assertThat(p).matches("collection.count");
    }
    
    @Test
    public void matchesCollectionFirst() {
        assertThat(p).matches("collection.first + 1");
    }
    
    @Test
    public void matchesCollectionLast() {
        assertThat(p).matches("collection.last + 1");
    }
    
    @Test
    public void matchesCollectionLimit() {
        assertThat(p).matches("collection.limit + 1");
    }
    
    @Test
    public void matchesCollectionNext() {
        assertThat(p).matches("collection.next(1) + 1");
    }
    
    @Test
    public void matchesCollectionPrior() {
        assertThat(p).matches("collection.prior(1) + 1");
    }
    
}
