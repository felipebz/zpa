package br.com.felipezorzo.sonar.plsql.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

public class BlockStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.BLOCK_STATEMENT);
    }

    @Test
    public void matchesSimpleBlock() {
        assertThat(p).matches("begin null; end;");
        assertThat(p).matches("BEGIN NULL; END;");
    }
    
    @Test
    public void matchesNestedBlock() {
        assertThat(p).matches("begin begin null; end; end;");
    }
    
    @Test
    public void matchesBlockWithMultipleStatements() {
        assertThat(p).matches("begin null; null; end;");
    }
    
    @Test
    public void matchesBlockWithOneExceptionHandler() {
        assertThat(p).matches("begin null; exception when others then null; end;");
    }
    @Test
    public void matchesBlockWithMultipleExceptionHandler() {
        assertThat(p).matches("begin null; exception when others then null; when others then null; end;");
    }
    
    @Test
    public void notMatchesBlockWithoutStatements() {
        assertThat(p).notMatches("begin end;");
    }
    
    @Test
    public void notMatchesBlockWithIncompleteExceptionHandler() {
        assertThat(p).notMatches("begin null; exception end;");
    }
}
