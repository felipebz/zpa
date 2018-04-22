package org.sonar.plugins.plsqlopen.api.units;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class CreateTypeTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.CREATE_TYPE);
    }
    
    @Test
    public void matchesIncompleteType() {
        assertThat(p).matches("create type foo;");
    }
    
    @Test
    public void matchesSimpleCreateTypeObject() {
        assertThat(p).matches("create type foo as object (x number(5));");
    }

    @Test
    public void matchesCreateEditionableType() {
        assertThat(p).matches("create editionable type foo as object (x number(5));");
    }

    @Test
    public void matchesCreateNonEditionableType() {
        assertThat(p).matches("create noneditionable type foo as object (x number(5));");
    }

    @Test
    public void matchesCreateTypeWithSharingMetadata() {
        assertThat(p).matches("create type foo sharing = metadata as object (x number(5));");
    }

    @Test
    public void matchesCreateTypeWithSharingNone() {
        assertThat(p).matches("create type foo sharing = none as object (x number(5));");
    }

    @Test
    public void matchesCreateTypeWithDefaultCollation() {
        assertThat(p).matches("create type foo default collation using_nls_comp as object (x number(5));");
    }

    @Test
    public void matchesCreateTypeWithAccesibleBy() {
        assertThat(p).matches("create type foo accessible by (type other_type) as object (x number(5));");
    }
    
    @Test
    public void matchesCreateTypeForce() {
        assertThat(p).matches("create type foo force as object (x number(5));");
    }
    
    @Test
    public void matchesSimpleCreateTypeUnder() {
        assertThat(p).matches("create type foo under bar (x number(5));");
    }
    
    @Test
    public void matchesSimpleCreateOrReplaceTypeObject() {
        assertThat(p).matches("create or replace type foo as object (x number(5));");
    }
    
    @Test
    public void matchesSimpleCreateTypeVarray() {
        assertThat(p).matches("create type foo is varray(2) of number(5);");
    }
    
    @Test
    public void matchesSimpleCreateTypeNestedTable() {
        assertThat(p).matches("create type foo is table of bar;");
    }
    
    @Test
    public void matchesManyAttributes() {
        assertThat(p).matches("create type foo as object (x number(5), y bar, z baz);");
    }
    
    @Test
    public void matchesCreateTypeFinal() {
        assertThat(p).matches("create type foo as object (x number(5)) final;");
    }
    
    @Test
    public void matchesCreateTypeNotFinal() {
        assertThat(p).matches("create type foo as object (x number(5)) not final;");
    }
    
    @Test
    public void matchesCreateTypeInstantiable() {
        assertThat(p).matches("create type foo as object (x number(5)) instantiable;");
    }
    
    @Test
    public void matchesCreateTypeNotInstantiable() {
        assertThat(p).matches("create type foo as object (x number(5)) not instantiable;");
    }
    
    @Test
    public void matchesCreateTypeNotFinalNotInstantiable() {
        assertThat(p).matches("create type foo as object (x number(5)) not final not instantiable;");
    }
    
    @Test
    public void matchesProcedureMember() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                + "x number(5),"
                + "member procedure bar);");
    }
    
    @Test
    public void matchesFunctionMember() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                + "x number(5),"
                + "member function bar return number);");
    }
    
    @Test
    public void matchesStaticProcedure() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                + "x number(5),"
                + "static procedure bar);");
    }
    
    @Test
    public void matchesStaticFunction() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                + "x number(5),"
                + "static function bar return number);");
    }
    
    @Test
    public void matchesSimpleConstructor() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                + "x number(5),"
                + "constructor function bar return self as result);");
    }
    
    @Test
    public void matchesFinalConstructor() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                + "x number(5),"
                + "final constructor function bar return self as result);");
    }
    
    @Test
    public void matchesInstantiableConstructor() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                + "x number(5),"
                + "instantiable constructor function bar return self as result);");
    }
    
    @Test
    public void matchesFinalInstantiableConstructor() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                + "x number(5),"
                + "final instantiable constructor function bar return self as result);");
    }
    
    @Test
    public void matchesSimpleConstructorWithParameters() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                + "x number(5),"
                + "constructor function bar(x number, y number) return self as result);");
    }
    
    @Test
    public void matchesSimpleConstructorWithExplicitSelf() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                + "x number(5),"
                + "constructor function bar(self in out foo, x number) return self as result);");
    }
    
    @Test
    public void matchesConstructor() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                + "x number(5),"
                + "final constructor function bar return self as result);");
    }
    
    @Test
    public void matchesMapMember() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                + "x number(5),"
                + "map member function bar return number);");
    }

    @Test
    public void matchesOrderMember() {
        assertThat(p).matches(
                "create or replace type foo as object ("
                + "x number(5),"
                + "order member function bar return number);");
    }
}
