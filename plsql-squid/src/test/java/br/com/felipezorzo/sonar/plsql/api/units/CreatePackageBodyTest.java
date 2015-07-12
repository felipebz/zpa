package br.com.felipezorzo.sonar.plsql.api.units;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class CreatePackageBodyTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.CREATE_PACKAGE_BODY);
    }

    @Test
    public void matchesSimplePackageBody() {
        assertThat(p).matches(""
                + "create package body test is\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesSimplePackageBodyAlternative() {
        assertThat(p).matches(""
                + "create package body test as\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesSimplePackageBodyWithNameAtEnd() {
        assertThat(p).matches(""
                + "create package body test is\n"
                + "null;\n"
                + "end test;");
    }
    
    @Test
    public void matchesSimpleCreateOrReplacePackageBody() {
        assertThat(p).matches(""
                + "create or replace package body test is\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesPackageBodyWithSchema() {
        assertThat(p).matches(""
                + "create package body schema.test is\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesPackageBodyWithProcedure() {
        assertThat(p).matches(""
                + "create package body test is\n"
                + "procedure proc is\n"
                + "begin\n"
                + "null;\n"
                + "end;\n"
                + "end;");
    }
    
    @Test
    public void matchesPackageWithFunction() {
        assertThat(p).matches(""
                + "create package body test is\n"
                + "function func return number is\n"
                + "begin\n"
                + "return null;\n"
                + "end;\n"
                + "end;");
    }
    
    @Test
    public void matchesPackageWithInitializationSection() {
        assertThat(p).matches(""
                + "create package body test is\n"
                + "var number;\n"
                + "begin\n"
                + "var := 0;\n"
                + "end;");
    }

}
