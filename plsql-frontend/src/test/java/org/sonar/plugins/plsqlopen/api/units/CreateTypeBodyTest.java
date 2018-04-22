package org.sonar.plugins.plsqlopen.api.units;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class CreateTypeBodyTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.CREATE_TYPE_BODY);
    }

    @Test
    public void matchesEditionableTypeBody() {
        assertThat(p).matches(
            "create editionable type body foo as "
                + "member procedure foo is "
                + "begin null; end; "
                + "end;");
    }

    @Test
    public void matchesNonEditionableTypeBody() {
        assertThat(p).matches(
            "create noneditionable type body foo as "
                + "member procedure foo is "
                + "begin null; end; "
                + "end;");
    }

    @Test
    public void matchesTypeBodyWithProcedure() {
        assertThat(p).matches(
                "create type body foo as "
                + "member procedure foo is "
                + "begin null; end; "
                + "end;");
    }
    
    @Test
    public void matchesTypeBodyWithFunction() {
        assertThat(p).matches(
                "create type body foo is "
                + "member function foo return number is "
                + "begin null; end; "
                + "end;");
    }
    
    @Test
    public void matchesTypeBodyWithStaticFunction() {
        assertThat(p).matches(
                "create type body foo is "
                + "static function foo return number is "
                + "begin null; end; "
                + "end;");
    }
    
    @Test
    public void matchesTypeBodyWithConstructor() {
        assertThat(p).matches(
                "create type body foo is "
                + "constructor function bar return self as result as "
                + "begin null; end; "
                + "end;");
    }
    
    @Test
    public void matchesTypeBodyWithOverridenFunction() {
        assertThat(p).matches(
                "create type body foo is "
                + "overriding member function foo return number is "
                + "begin null; end; "
                + "end;");
    }
}
