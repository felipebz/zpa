package org.sonar.plugins.plsqlopen.api.session;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.RuleTest;
import org.sonar.plugins.plsqlopen.api.SessionControlGrammar;

public class SessionControlGrammarTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(SessionControlGrammar.SESSION_CONTROL_COMMAND);
    }
    
    @Test
    public void matchesAlterSession() {
        assertThat(p).matches("alter session set nls_date_format = 'dd/mm/yyyy';");
    }
    
    @Test
    public void matchesSetRole() {
        assertThat(p).matches("set role foo;");
        assertThat(p).matches("set role foo, bar, baz;");
    }
    
    @Test
    public void matchesSetRoleNone() {
        assertThat(p).matches("set role none;");
    }
    
    @Test
    public void matchesSetRoleAll() {
        assertThat(p).matches("set role all;");
    }
    
    @Test
    public void matchesSetRoleAllExcept() {
        assertThat(p).matches("set role all except foo;");
        assertThat(p).matches("set role all except foo, bar, baz;");
    }
    
    @Test
    public void matchesSetRoleWithPassword() {
        assertThat(p).matches("set role foo identified by pass;");
        assertThat(p).matches("set role foo identified by pass, bar, baz;");
    }

}
