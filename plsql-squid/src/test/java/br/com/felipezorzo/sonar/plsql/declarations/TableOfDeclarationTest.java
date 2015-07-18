package br.com.felipezorzo.sonar.plsql.declarations;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class TableOfDeclarationTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.TABLE_OF_DECLARATION);
    }
    
    @Test
    public void matchesSimpleDeclaration() {
        assertThat(p).matches("type foo is table of bar;");
    }
    
    @Test
    public void matchesIndexedDeclaration() {
        assertThat(p).matches("type foo is table of bar index by binary_integer;");
    }
    
    @Test
    public void matchesDeclarationWithBuiltinDatatypes() {
        assertThat(p).matches("type foo is table of varchar2(10) index by number(5);");
    }

}
