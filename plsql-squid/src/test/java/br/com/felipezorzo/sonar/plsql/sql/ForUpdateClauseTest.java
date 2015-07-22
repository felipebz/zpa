package br.com.felipezorzo.sonar.plsql.sql;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class ForUpdateClauseTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.FOR_UPDATE_CLAUSE);
    }
    
    @Test
    public void matchesSimpleForUpdate() {
        assertThat(p).matches("for update");
    }
    
    @Test
    public void matchesForUpdateOfColumn() {
        assertThat(p).matches("for update of col");
    }
    
    @Test
    public void matchesForUpdateOfColumnWithTable() {
        assertThat(p).matches("for update of tab.col");
    }
    
    @Test
    public void matchesForUpdateOfColumnWithSchemaAndTable() {
        assertThat(p).matches("for update of sch.tab.col");
    }
    
    @Test
    public void matchesForUpdateOfMultipleColumns() {
        assertThat(p).matches("for update of col, col2, col3");
    }
    
    @Test
    public void matchesForUpdateNoWait() {
        assertThat(p).matches("for update nowait");
    }
    
    @Test
    public void matchesForUpdateWait() {
        assertThat(p).matches("for update wait 1");
    }
    
    @Test
    public void matchesForUpdateSkipLocked() {
        assertThat(p).matches("for update skip locked");
    }
    
    @Test
    public void matchesLongForUpdate() {
        assertThat(p).matches("for update of col skip locked");
    }

}
