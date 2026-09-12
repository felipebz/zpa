package com.company.plsql;

import com.felipebz.zpa.checks.verifier.ProjectPlSqlCheckVerifier;
import com.felipebz.zpa.checks.verifier.ProjectTestSource;
import org.junit.jupiter.api.Test;

import java.util.List;

public class PackageSubprogramShouldBePublicCheckTest {

    @Test
    public void reportsPackageBodyHelpersMissingFromTheSpecification() {
        ProjectPlSqlCheckVerifier.verify(
            List.of(
                new ProjectTestSource(
                    "p-spec.sql",
                    """
                    CREATE PACKAGE p AS
                      PROCEDURE public_work;
                    END p;
                    """
                ),
                new ProjectTestSource(
                    "p-body.sql",
                    """
                    CREATE PACKAGE BODY p AS
                      PROCEDURE public_work IS
                      BEGIN
                        NULL;
                      END public_work;

                      PROCEDURE private_helper IS -- Noncompliant {{Declare this package subprogram in the package specification.}}
                      BEGIN
                        NULL;
                      END private_helper;
                    END p;
                    """
                )
            ),
            new PackageSubprogramShouldBePublicCheck()
        );
    }
}
