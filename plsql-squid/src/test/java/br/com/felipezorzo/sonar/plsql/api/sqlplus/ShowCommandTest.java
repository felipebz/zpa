package br.com.felipezorzo.sonar.plsql.api.sqlplus;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class ShowCommandTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.SQLPLUS_SHOW);
    }
    
    @Test
    public void matchesShowErrors() {
        assertThat(p).matches("show errors\n");
    }

}
