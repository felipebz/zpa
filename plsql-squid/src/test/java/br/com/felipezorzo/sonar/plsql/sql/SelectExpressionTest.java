package br.com.felipezorzo.sonar.plsql.sql;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class SelectExpressionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.SELECT_EXPRESSION);
    }
    
    @Test
    public void matchesSimpleSelect() {
        assertThat(p).matches("select 1 from dual");
    }
    
    @Test
    public void matchesSimpleSelectInto() {
        assertThat(p).matches("select 1 into var from dual");
    }
    
    @Test
    public void matchesSelectWithWhere() {
        assertThat(p).matches("select 1 from dual where 1 = 1");
    }
    
    @Test
    public void matchesSelectWithMultipleColumns() {
        assertThat(p).matches("select 1, 2 from dual");
    }
    
    @Test
    public void matchesSelectWithMultipleColumnsAndIntoClause() {
        assertThat(p).matches("select 1, 2 into var1, var2 from dual");
    }
    
    @Test
    public void matchesSelectWithMultipleTables() {
        assertThat(p).matches("select 1 from emp, dept");
    }
    
    @Test
    public void matchesSelectAll() {
        assertThat(p).matches("select all 1 from dual");
    }
    
    @Test
    public void matchesSelectDistinct() {
        assertThat(p).matches("select distinct 1 from dual");
    }
    
    @Test
    public void matchesSelectUnique() {
        assertThat(p).matches("select unique 1 from dual");
    }
    
    @Test
    public void matchesSelectWithGroupBy() {
        assertThat(p).matches("select 1 from dual group by 1");
    }
    
    @Test
    public void matchesSelectWithOrderBy() {
        assertThat(p).matches("select 1 from dual order by 1");
    }

    @Test
    public void matchesSelectWithParenthesis() {
        assertThat(p).matches("(select 1 from dual)");
    }
    
    @Test
    public void matchesSelectWithUnion() {
        assertThat(p).matches("select 1 from dual union select 2 from dual");
        assertThat(p).matches("(select 1 from dual) union (select 2 from dual)");
    }
    
    @Test
    public void matchesSelectWithUnionAll() {
        assertThat(p).matches("select 1 from dual union all select 2 from dual");
        assertThat(p).matches("(select 1 from dual) union all (select 2 from dual)");
    }
    
    @Test
    public void matchesSelectWithMinus() {
        assertThat(p).matches("(select 1 from dual) minus (select 2 from dual)");
    }
    
    @Test
    public void matchesSelectCountDistinct() {
        assertThat(p).matches("select count(distinct foo) from dual");
    }

}
