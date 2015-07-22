package br.com.felipezorzo.sonar.plsql.declarations;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class RefCursorDeclarationTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.REF_CURSOR_DECLARATION);
    }

    @Test
    public void matchesSimpleRefCursor() {
        assertThat(p).matches("type myref is ref cursor;");
    }
    
    @Test
    public void matchesSimpleRefCursorWithReturn() {
        assertThat(p).matches("type myref is ref cursor return number;");
    }

}
