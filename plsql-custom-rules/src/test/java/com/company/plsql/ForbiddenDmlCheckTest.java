package com.company.plsql;

import org.junit.Test;
import org.sonar.plsqlopen.checks.verifier.PlSqlCheckVerifier;

public class ForbiddenDmlCheckTest {

    @Test
    public void test() {
        PlSqlCheckVerifier.verify("src/test/resources/forbidden-dml.sql", new ForbiddenDmlCheck());
    }
    
}
