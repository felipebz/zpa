package org.sonar.plugins.plsqlopen.api.declarations;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class VarrayDeclarationTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.VARRAY_DECLARATION);
    }
    
    @Test
    public void matchesSimpleVarray() {
        assertThat(p).matches("type foo is varray(5) of number(2);");
    }
    
    @Test
    public void matchesVaryingArray() {
        assertThat(p).matches("type foo is varying array(5) of number(2);");
    }
    
    @Test
    public void matchesArray() {
        assertThat(p).matches("type foo is array(5) of number(2);");
    }

}
