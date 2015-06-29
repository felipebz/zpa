package br.com.felipezorzo.sonar.plsql.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

public class ForStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.FOR_STATEMENT);
    }

    @Test
    public void matchesForLoop() {
        assertThat(p).matches(""
                + "for i in 1..2 loop "
                + "null; "
                + "end loop;");
    }
    
    @Test
    public void matchesReverseForLoop() {
        assertThat(p).matches(""
                + "for i in reverse 1..2 loop "
                + "null; "
                + "end loop;");
    }
    
    @Test
    public void matchesNestedForLoop() {
        assertThat(p).matches(""
                + "for i in 1..2 loop "
                + "for i in 1..2 loop "
                + "null; "
                + "end loop; "
                + "end loop;");
    }

}
