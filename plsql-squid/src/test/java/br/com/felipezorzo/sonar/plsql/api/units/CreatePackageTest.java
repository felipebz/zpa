package br.com.felipezorzo.sonar.plsql.api.units;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class CreatePackageTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.CREATE_PACKAGE);
    }

    @Test
    public void matchesSimplePackage() {
        assertThat(p).matches(""
                + "create package test is\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesSimplePackageAlternative() {
        assertThat(p).matches(""
                + "create package test as\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesSimplePackageWithNameAtEnd() {
        assertThat(p).matches(""
                + "create package test is\n"
                + "null;\n"
                + "end test;");
    }
    
    @Test
    public void matchesSimpleCreateOrReplacePackage() {
        assertThat(p).matches(""
                + "create or replace package test is\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesPackageWithSchema() {
        assertThat(p).matches(""
                + "create package schema.test is\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesPackageWithAuthidCurrentUser() {
        assertThat(p).matches(""
                + "create package test authid current_user is\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesPackageWithAuthidDefiner() {
        assertThat(p).matches(""
                + "create package test authid definer is\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesPackageWithProcedure() {
        assertThat(p).matches(""
                + "create package test authid definer is\n"
                + "procedure proc;\n"
                + "end;");
    }
    
    @Test
    public void matchesPackageWithFunction() {
        assertThat(p).matches(""
                + "create package test authid definer is\n"
                + "function func return number;\n"
                + "end;");
    }

}
