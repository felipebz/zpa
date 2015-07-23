package br.com.felipezorzo.sonar.plsql.sql;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class OrderByClauseTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.ORDER_BY_CLAUSE);
    }
    
    @Test
    public void matchesSimpleOrderBy() {
        assertThat(p).matches("order by 1");
    }
    
    @Test
    public void matchesSimpleOrderByAsc() {
        assertThat(p).matches("order by 1 asc");
    }
    
    @Test
    public void matchesSimpleOrderByDesc() {
        assertThat(p).matches("order by 1 desc");
    }
    
    @Test
    public void matchesSimpleOrderByColumn() {
        assertThat(p).matches("order by col");
    }
    
    @Test
    public void matchesOrderByWithMultipleValuesAndOrdering() {
        assertThat(p).matches("order by col1 asc, col2 desc, col3 desc");
    }
    
    @Test
    public void matchesSimpleOrderByTableColumn() {
        assertThat(p).matches("order by tab.col");
    }
    
    @Test
    public void matchesSimpleOrderByTableColumnWithSchema() {
        assertThat(p).matches("order by sch.tab.col");
    }
    
    @Test
    public void matchesSimpleOrderByFunctionCall() {
        assertThat(p).matches("order by func(var)");
    }
    
    @Test
    public void matchesOrderSiblingsBy() {
        assertThat(p).matches("order siblings by col");
    }
    
    @Test
    public void matchesOrderByNullsFirst() {
        assertThat(p).matches("order by col nulls first");
    }
    
    @Test
    public void matchesOrderByNullsLast() {
        assertThat(p).matches("order by col nulls last");
    }

}
