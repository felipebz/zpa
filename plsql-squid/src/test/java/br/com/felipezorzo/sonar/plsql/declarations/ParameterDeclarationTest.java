package br.com.felipezorzo.sonar.plsql.declarations;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class ParameterDeclarationTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.PARAMETER_DECLARATION);
    }

    @Test
    public void matchesSimpleParameter() {
        assertThat(p).matches("parameter number");
    }
    
    @Test
    public void matchesExplicitInParameter() {
        assertThat(p).matches("parameter in number");
    }
    
    @Test
    public void matchesExplicitInParameterWithDefaultValue() {
        assertThat(p).matches("parameter in number := 1");
    }
    
    @Test
    public void matchesExplicitInParameterWithDefaultValueAlternative() {
        assertThat(p).matches("parameter in number default 1");
    }
    
    @Test
    public void matchesOutParameter() {
        assertThat(p).matches("parameter out number");
    }
    
    @Test
    public void matchesInOutParameter() {
        assertThat(p).matches("parameter in out number");
    }
    
    @Test
    public void matchesOutParameterWithNocopy() {
        assertThat(p).matches("parameter out nocopy number");
    }
    
    @Test
    public void matchesInOutParameterWithNocopy() {
        assertThat(p).matches("parameter in out nocopy number");
    }
    
}
