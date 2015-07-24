package br.com.felipezorzo.sonar.plsql.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class WhileStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.WHILE_STATEMENT);
    }

    @Test
    public void matchesWhileLoop() {
        assertThat(p).matches(""
                + "while true loop "
                + "null; "
                + "end loop;");
    }
    
    @Test
    public void matchesNestedWhileLoop() {
        assertThat(p).matches(""
                + "while true loop "
                + "while true loop "
                + "null; "
                + "end loop; "
                + "end loop;");
    }
    
    @Test
    public void matchesLabeledLoop() {
        assertThat(p).matches(""
                + "<<foo>> while true loop "
                + "null; "
                + "end loop foo;");
    }

}
