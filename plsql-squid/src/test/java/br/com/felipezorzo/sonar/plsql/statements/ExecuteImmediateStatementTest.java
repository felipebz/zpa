package br.com.felipezorzo.sonar.plsql.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class ExecuteImmediateStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.EXECUTE_IMMEDIATE_STATEMENT);
    }

    @Test
    public void matchesSimpleExecuteImmediate() {
        assertThat(p).matches("execute immediate 'command';");
    }
    
    @Test
    public void matchesExecuteImmediateIntoVariable() {
        assertThat(p).matches("execute immediate 'command' into var;");
    }
    
    @Test
    public void matchesExecuteImmediateIntoMultipleVariables() {
        assertThat(p).matches("execute immediate 'command' into var, var2;");
    }
    
    @Test
    public void matchesExecuteImmediateUsingWithVariable() {
        assertThat(p).matches("execute immediate 'command' using var;");
    }
    
    @Test
    public void matchesExecuteImmediateUsingWithMultipleVariables() {
        assertThat(p).matches("execute immediate 'command' using var, var2;");
    }
    
    @Test
    public void matchesExecuteImmediateUsingWithExplicitInVariable() {
        assertThat(p).matches("execute immediate 'command' using in var;");
    }
    
    @Test
    public void matchesExecuteImmediateUsingWithOutVariable() {
        assertThat(p).matches("execute immediate 'command' using out var;");
    }
    
    @Test
    public void matchesExecuteImmediateUsingWithInOutVariable() {
        assertThat(p).matches("execute immediate 'command' using in out var;");
    }
    
    @Test
    public void matchesLabeledExecuteImmediate() {
        assertThat(p).matches("<<label>> execute immediate 'command';");
    }

}
