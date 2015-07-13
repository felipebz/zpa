package br.com.felipezorzo.sonar.plsql.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class VariableDeclarationTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.VARIABLE_DECLARATION);
    }

    @Test
    public void matchesSimpleDeclaration() {
        assertThat(p).matches("var number;");
    }
    
    @Test
    public void matchesDeclarationWithPrecision() {
        assertThat(p).matches("var number(1);");
    }
    
    @Test
    public void matchesDeclarationWithPrecisionAndScale() {
        assertThat(p).matches("var number(1,1);");
    }
    
    @Test
    public void matchesDeclarationWithInitialization() {
        assertThat(p).matches("var number := 1;");
        assertThat(p).matches("var varchar2(1) := 'a';");
        assertThat(p).matches("var boolean := true;");
    }
    
    @Test
    public void matchesDeclarationWithDefaultValue() {
        assertThat(p).matches("var number default 1;");
        assertThat(p).matches("var varchar2(1) default 'a';");
        assertThat(p).matches("var boolean default true;");
    }
    
    @Test
    public void matchesDeclarationWithNotNullConstraint() {
        assertThat(p).matches("var number not null := 1;");
        assertThat(p).matches("var number not null default 1;");
    }
    
    @Test
    public void matchesTypeAnchoredDeclaration() {
        assertThat(p).matches("var custom%type;");
    }
    
    @Test @Ignore
    public void matchesObjectDeclaration() {
        assertThat(p).matches("var custom;");
    }

    @Test @Ignore
    public void matchesTableAnchoredDeclaration() {
        assertThat(p).matches("var table%rowtype;");
    }
    
    @Test
    public void matchesTableColumnAnchoredDeclaration() {
        assertThat(p).matches("var table.column%type;");
    }
    
    @Test @Ignore
    public void matchesRefObjectDeclaration() {
        assertThat(p).matches("var ref custom;");
    }
    
    @Test
    public void matchesSimpleConstant() {
        assertThat(p).matches("var constant number := 1;");
        assertThat(p).matches("var constant number default 1;");
    }
    
    @Test
    public void matchesSimpleConstantWithConstraints() {
        assertThat(p).matches("var constant number not null := 1;");
        assertThat(p).matches("var constant number not null default 1;");
    }
    
    @Test
    public void matchesExceptionDeclaration() {
        assertThat(p).matches("var exception;");
    }
    
    @Test
    public void notMatchesDeclarationWithIncompleteNotNullConstraint() {
        assertThat(p).notMatches("var number not null;");
    }
    
}
