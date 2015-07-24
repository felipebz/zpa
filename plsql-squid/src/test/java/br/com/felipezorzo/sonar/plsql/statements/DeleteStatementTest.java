package br.com.felipezorzo.sonar.plsql.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class DeleteStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.DELETE_STATEMENT);
    }

    @Test
    public void matchesSimpleDelete() {
        assertThat(p).matches("delete tab;");
    }
    
    @Test
    public void matchesDeleteFrom() {
        assertThat(p).matches("delete from tab;");
    }
    
    @Test
    public void matchesDeleteWithWhere() {
        assertThat(p).matches("delete from tab where x = 1;");
    }
    
    @Test
    public void matchesDeleteWithAlias() {
        assertThat(p).matches("delete tab t;");
    }
    
    @Test
    public void matchesDeleteWithSchema() {
        assertThat(p).matches("delete sch.tab;");
    }
    
    @Test
    public void matchesDeleteCurrentOf() {
        assertThat(p).matches("delete tab where current of cur;");
    }
    
    @Test
    public void matchesLabeledDelete() {
        assertThat(p).matches("<<foo>> delete tab;");
    }

}
