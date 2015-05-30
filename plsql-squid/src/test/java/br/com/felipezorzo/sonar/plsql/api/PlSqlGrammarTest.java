package br.com.felipezorzo.sonar.plsql.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

public class PlSqlGrammarTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.BLOCK_STATEMENT);
    }

    @Test
    public void ok() {
        assertThat(p).matches("begin null; end;");
        assertThat(p).matches("BEGIN NULL; END;");
    }
    
    @Test
    public void fail() {
        assertThat(p).notMatches("begin end;");
    }
}
