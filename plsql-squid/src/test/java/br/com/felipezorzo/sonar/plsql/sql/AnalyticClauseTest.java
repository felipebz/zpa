package br.com.felipezorzo.sonar.plsql.sql;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class AnalyticClauseTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.ANALYTIC_CLAUSE);
    }

    @Test
    public void matchesSimpleOver() {
        assertThat(p).matches("over ()");
    }
    
    @Test
    public void matchesOverPartitionBy() {
        assertThat(p).matches("over (partition by foo)");
    }
    
    @Test
    public void matchesOverPartitionByWithMultipleExpressions() {
        assertThat(p).matches("over (partition by foo, bar)");
    }
    
    @Test
    public void matchesOverOrderBy() {
        assertThat(p).matches("over (order by foo)");
    }
    
    @Test
    public void matchesOverWithWindowingUnboundedPreceding() {
        assertThat(p).matches("over (order by foo rows unbounded preceding)");
        assertThat(p).matches("over (order by foo range unbounded preceding)");
    }
    
    @Test
    public void matchesOverWithWindowingCurrentRow() {
        assertThat(p).matches("over (order by foo rows current row)");
    }
    
    @Test
    public void matchesOverWithWindowingExpressionPreceding() {
        assertThat(p).matches("over (order by foo rows 1 preceding)");
    }
    
    @Test
    public void matchesOverWithWindowingWithBetween() {
        assertThat(p).matches("over (order by foo rows between unbounded preceding and current row)");
    }
    
    @Test
    public void matchesLongAnalyticClause() {
        assertThat(p).matches("over (partition by foo order by foo rows between unbounded preceding and unbounded following)");
    }

}
