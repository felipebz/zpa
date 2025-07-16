package com.company.plsql;

import org.junit.jupiter.api.Test;
import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier;

public class ForbiddenDmlCheckTest {

    @Test
    public void test() {
        PlSqlCheckVerifier.verify("src/test/resources/forbidden-dml.sql", new ForbiddenDmlCheck());
    }
    
}
