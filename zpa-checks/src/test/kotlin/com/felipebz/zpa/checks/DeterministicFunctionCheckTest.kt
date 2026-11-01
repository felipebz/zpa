//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier
import com.felipebz.zpa.checks.verifier.ProjectPlSqlCheckVerifier
import com.felipebz.zpa.checks.verifier.ProjectTestSource
import org.junit.jupiter.api.Test

class DeterministicFunctionCheckTest : BaseCheckTest() {

    @Test
    fun reportsDirectNonDeterministicBehavior() {
        PlSqlCheckVerifier.verify(getPath("deterministic_function.sql"), DeterministicFunctionCheck())
    }

    @Test
    fun reportsProjectSequences() {
        ProjectPlSqlCheckVerifier.verify(
            listOf(
                ProjectTestSource("sequences.sql", "CREATE SEQUENCE sequence_test;"),
                ProjectTestSource(
                    "deterministic_sequence_function.sql",
                    """
                    CREATE FUNCTION next_value RETURN NUMBER DETERMINISTIC IS -- Noncompliant
                    BEGIN
                    RETURN sequence_test.NEXTVAL;
                    END;
                    /

                    CREATE FUNCTION current_value RETURN NUMBER DETERMINISTIC IS -- Noncompliant
                    BEGIN
                    RETURN sequence_test.CURRVAL;
                    END;
                    /
                    """.trimIndent()
                )
            ),
            DeterministicFunctionCheck()
        )
    }

    @Test
    fun reportsPackageFunctions() {
        ProjectPlSqlCheckVerifier.verify(
            listOf(
                ProjectTestSource(
                    "deterministic_package_spec.sql",
                    """
                    CREATE PACKAGE deterministic_package AS
                      FUNCTION package_value RETURN DATE DETERMINISTIC;
                      FUNCTION ordinary_value RETURN DATE;
                    END deterministic_package;
                    /
                    """.trimIndent()
                ),
                ProjectTestSource(
                    "deterministic_package_body.sql",
                    """
                    CREATE PACKAGE BODY deterministic_package AS
                      FUNCTION package_value RETURN DATE IS -- Noncompliant
                      BEGIN
                        RETURN SYSDATE;
                      END package_value;

                      FUNCTION ordinary_value RETURN DATE IS
                      BEGIN
                        RETURN SYSDATE;
                      END ordinary_value;

                      FUNCTION private_value RETURN DATE IS
                      BEGIN
                        RETURN SYSDATE;
                      END private_value;
                    END deterministic_package;
                    /
                    """.trimIndent()
                )
            ),
            DeterministicFunctionCheck()
        )
    }
}
