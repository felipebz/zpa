package br.com.felipezorzo.sonar.plsql.api.units;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class AnonymousBlockTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.ANONYMOUS_BLOCK);
    }

    @Test
    public void matchesSimpleBlock() {
        assertThat(p).matches(""
                + "BEGIN\n"
                + "NULL;\n"
                + "END;");
    }
    
    @Test
    public void matchesBlockWithDeclareSection() {
        assertThat(p).matches(""
                + "DECLARE\n"
                + "VAR NUMBER;\n"
                + "BEGIN\n"
                + "NULL;\n"
                + "END;");
    }

}
