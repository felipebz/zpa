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

}
