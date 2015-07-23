package br.com.felipezorzo.sonar.plsql.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class PipeRowStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.PIPE_ROW_STATAMENT);
    }

    @Test
    public void matchesSimpleSearchedCase() {
        assertThat(p).matches("pipe row (value);");
    }

}
