package br.com.felipezorzo.sonar.plsql.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class OpenForStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.OPEN_FOR_STATEMENT);
    }
    
    @Test
    public void matchesSimpleOpen() {
        assertThat(p).matches("open cur for myquery;");
    }
    
    @Test
    public void matchesOpenForSelectExpression() {
        assertThat(p).matches("open cur for select 1 from dual;");
    }
    
    @Test
    public void matchesOpenForHostCursor() {
        assertThat(p).matches("open :cur for myquery;");
    }
    
    @Test
    public void matchesOpenWithUsingClause() {
        assertThat(p).matches("open cur for myquery using foo;");
    }
    
    @Test
    public void matchesOpenWithInParameterInUsingClause() {
        assertThat(p).matches("open cur for myquery using in foo;");
    }
    
    @Test
    public void matchesOpenWithInParameterInOutUsingClause() {
        assertThat(p).matches("open cur for myquery using in out foo;");
    }
    
    @Test
    public void matchesOpenWithInParameterOutUsingClause() {
        assertThat(p).matches("open cur for myquery using out foo;");
    }
    
    @Test
    public void matchesOpenWithMultipleParameters() {
        assertThat(p).matches("open cur for myquery using foo, bar, baz;");
    }
    
    @Test
    public void matcheslabeledOpen() {
        assertThat(p).matches("<<foo>> open cur for myquery;");
    }

}
