package br.com.felipezorzo.sonar.plsql.sql;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class ConnectByClauseTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.CONNECT_BY_CLAUSE);
    }

    @Test
    public void matchesSimpleConnectBy() {
        assertThat(p).matches("connect by foo = bar");
    }
    
    @Test
    public void matchesConnectByWithPrior() {
        assertThat(p).matches("connect by prior foo = bar");
    }
    
    @Test
    public void matchesConnectByWithPriorAlternative() {
        assertThat(p).matches("connect by foo = prior bar");
    }
    
    @Test
    public void matchesConnectByNoCycle() {
        assertThat(p).matches("connect by nocycle foo = bar");
    }

}
