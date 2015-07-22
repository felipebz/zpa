package br.com.felipezorzo.sonar.plsql.declarations;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class PragmaDeclarationTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.PRAGMA_DECLARATION);
    }
    
    @Test
    public void matchesAutonomousTransaction() {
        assertThat(p).matches("pragma autonomous_transaction;");
    }
    
    @Test
    public void matchesExceptionInit() {
        assertThat(p).matches("pragma exception_init(1, 1);");
    }
    
    @Test
    public void matchesSeriallyReusable() {
        assertThat(p).matches("pragma serially_reusable;");
    }

}
