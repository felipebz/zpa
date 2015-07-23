package br.com.felipezorzo.sonar.plsql.sql;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class StartWithClauseTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.START_WITH_CLAUSE);
    }
    
    @Test
    public void matchesSimpleConnectBy() {
        assertThat(p).matches("start with foo = bar");
    }

}
