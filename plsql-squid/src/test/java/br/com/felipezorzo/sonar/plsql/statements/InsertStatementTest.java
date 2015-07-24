package br.com.felipezorzo.sonar.plsql.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class InsertStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.INSERT_STATEMENT);
    }
    
    @Test
    public void matchesSimpleInsert() {
        assertThat(p).matches("insert into tab values (1);");
    }
    
    @Test
    public void matchesInsertWithTableAlias() {
        assertThat(p).matches("insert into tab t values (1);");
    }
    
    @Test
    public void matchesInsertWithExplicitColumn() {
        assertThat(p).matches("insert into tab (x) values (1);");
    }
    
    @Test
    public void matchesInsertWithExplicitColumnAlternative() {
        assertThat(p).matches("insert into tab (tab.x) values (1);");
    }
    
    @Test
    public void matchesInsertMultipleColumns() {
        assertThat(p).matches("insert into tab (x, y) values (1, 2);");
    }
    
    @Test
    public void matchesInsertWithSubquery() {
        assertThat(p).matches("insert into tab (select 1, 2 from dual);");
    }
    
    @Test
    public void matchesInsertWithSubqueryInColumns() {
        assertThat(p).matches("insert into tab (x, y) (select 1, 2 from dual);");
    }
    
    @Test
    public void matchesInsertWithSchema() {
        assertThat(p).matches("insert into sch.tab values (1);");
    }
    
    @Test
    public void matchesInsertRecord() {
        assertThat(p).matches("insert into tab values foo;");
    }

}
