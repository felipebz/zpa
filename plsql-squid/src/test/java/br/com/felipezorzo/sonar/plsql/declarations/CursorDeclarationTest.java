package br.com.felipezorzo.sonar.plsql.declarations;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class CursorDeclarationTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.CURSOR_DECLARATION);
    }

    @Test
    public void matchesSimpleCursor() {
        assertThat(p).matches("cursor cur is select 1 from dual;");
    }
    
    @Test
    public void matchesCursorWithParameter() {
        assertThat(p).matches("cursor cur(x number) is select 1 from dual;");
    }
    
    @Test
    public void matchesCursorWithMultipleParameters() {
        assertThat(p).matches("cursor cur(x number, y number) is select 1 from dual;");
    }
    
    @Test
    public void matchesCursorWithExplicitInParameter() {
        assertThat(p).matches("cursor cur(x in number) is select 1 from dual;");
    }
    
    @Test
    public void matchesCursorWithDefaultParameter() {
        assertThat(p).matches("cursor cur(x number default 1) is select 1 from dual;");
    }
    
    @Test
    public void matchesCursorWithDefaultParameterAlternative() {
        assertThat(p).matches("cursor cur(x number := 1) is select 1 from dual;");
    }

}
