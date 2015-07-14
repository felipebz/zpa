package br.com.felipezorzo.sonar.plsql.sql;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class WhereClauseTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.WHERE_CLAUSE);
    }
    
    @Test
    public void matchesSimpleWhere() {
        assertThat(p).matches("where 1 = 1");
    }
    
    @Test
    public void matchesColumnComparation() {
        assertThat(p).matches("where tab.col = tab2.col2");
    }
    
    @Test
    public void matchesMultipleColumnComparation() {
        assertThat(p).matches("where tab.col = tab2.col2 and tab.col = tab2.col2");
    }
    
    @Test
    public void matchesComparationWithSubquery() {
        assertThat(p).matches("where tab.col = (select 1 from dual)");
    }

}
