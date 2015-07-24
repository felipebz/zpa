package br.com.felipezorzo.sonar.plsql.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class FetchStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.FETCH_STATEMENT);
    }
    
    @Test
    public void matchesSimpleFetchInto() {
        assertThat(p).matches("fetch foo into bar;");
    }
    
    @Test
    public void matchesFetchHostCursorInto() {
        assertThat(p).matches("fetch :foo into bar;");
    }
    
    @Test
    public void matchesFetchIntoMultipleVariables() {
        assertThat(p).matches("fetch foo into bar, baz;");
    }
    
    @Test
    public void matchesSimpleFetchBulkCollectInto() {
        assertThat(p).matches("fetch foo bulk collect into bar;");
    }
    
    @Test
    public void matchesFetchHostCursorBulkCollectInto() {
        assertThat(p).matches("fetch :foo bulk collect into bar;");
    }
    
    @Test
    public void matchesFetchBulkCollectIntoMultipleVariables() {
        assertThat(p).matches("fetch foo bulk collect into bar, baz;");
    }
    
    @Test
    public void matchesSimpleFetchBulkCollectIntoHostVariable() {
        assertThat(p).matches("fetch foo bulk collect into :bar;");
    }
    
    @Test
    public void matchesSimpleFetchBulkCollectIntoWithLimit() {
        assertThat(p).matches("fetch foo bulk collect into bar limit 10;");
    }
    
    @Test
    public void matchesLabeledFetchInto() {
        assertThat(p).matches("<<foo>> fetch foo into bar;");
    }

}
