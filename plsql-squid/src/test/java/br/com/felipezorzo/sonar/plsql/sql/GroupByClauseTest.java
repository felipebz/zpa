package br.com.felipezorzo.sonar.plsql.sql;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class GroupByClauseTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.GROUP_BY_CLAUSE);
    }
    
    @Test
    public void matchesSimpleGroupBy() {
        assertThat(p).matches("group by 1");
    }
    
    @Test
    public void matchesSimpleGroupByColumn() {
        assertThat(p).matches("group by col");
    }
    
    @Test
    public void matchesSimpleGroupByTableColumn() {
        assertThat(p).matches("group by tab.col");
    }
    
    @Test
    public void matchesSimpleGroupByTableColumnWithSchema() {
        assertThat(p).matches("group by sch.tab.col");
    }
    
    @Test
    public void matchesSimpleGroupByFunctionCall() {
        assertThat(p).matches("group by func(var)");
    }
    
    @Test
    public void matchesSimpleGroupByWithHaving() {
        assertThat(p).matches("group by col having col > 1");
    }

}
