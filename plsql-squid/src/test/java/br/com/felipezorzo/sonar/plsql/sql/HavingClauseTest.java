package br.com.felipezorzo.sonar.plsql.sql;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class HavingClauseTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.HAVING_CLAUSE);
    }

    @Test
    public void matchesSimpleHaving() {
        assertThat(p).matches("having col > 1");
    }
}
