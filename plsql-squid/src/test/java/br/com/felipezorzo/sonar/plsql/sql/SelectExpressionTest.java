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
        assertThat(p).matches(""
                + "select 1\n"
                + "from dual");
    }
    
    @Test
    public void matchesSimpleSelectInto() {
        assertThat(p).matches(""
                + "select 1\n"
                + "into var\n"
                + "from dual");
    }
    
    @Test
    public void matchesSelectWithWhere() {
        assertThat(p).matches(""
                + "select 1\n"
                + "from dual\n"
                + "where 1 = 1");
    }
    
    @Test
    public void matchesSelectWithMultipleColumns() {
        assertThat(p).matches(""
                + "select 1, 2\n"
                + "from dual");
    }
    
    @Test
    public void matchesSelectWithMultipleColumnsAndIntoClause() {
        assertThat(p).matches(""
                + "select 1, 2\n"
                + "into var1, var2\n"
                + "from dual");
    }
    
    @Test
    public void matchesSelectWithMultipleTables() {
        assertThat(p).matches(""
                + "select 1\n"
                + "from emp, dept");
    }
    
    @Test
    public void matchesSelectAll() {
        assertThat(p).matches(""
                + "select all 1\n"
                + "from dual");
    }
    
    @Test
    public void matchesSelectDistinct() {
        assertThat(p).matches(""
                + "select distinct 1\n"
                + "from dual");
    }
    
    @Test
    public void matchesSelectUnique() {
        assertThat(p).matches(""
                + "select unique 1\n"
                + "from dual");
    }
    
    @Test
    public void matchesSelectWithGroupBy() {
        assertThat(p).matches(""
                + "select 1\n"
                + "from dual\n"
                + "group by 1");
    }

}
