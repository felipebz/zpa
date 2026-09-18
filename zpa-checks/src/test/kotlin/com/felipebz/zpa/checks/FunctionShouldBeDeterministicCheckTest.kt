/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.checks

import com.felipebz.zpa.TestPlSqlVisitorRunner
import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier
import com.felipebz.zpa.checks.verifier.ProjectPlSqlCheckVerifier
import com.felipebz.zpa.checks.verifier.ProjectTestSource
import com.felipebz.zpa.metadata.FormsMetadata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class FunctionShouldBeDeterministicCheckTest : BaseCheckTest() {

    @Test
    fun reportsOnlyHighConfidenceCandidates() {
        PlSqlCheckVerifier.verify(
            getPath("function_should_be_deterministic.sql"),
            FunctionShouldBeDeterministicCheck()
        )
    }

    @Test
    fun doesNotRecommendDeterministicForOracleFormsCode() {
        PlSqlCheckVerifier.verify(
            getPath("function_should_be_deterministic_forms.sql"),
            FunctionShouldBeDeterministicCheck(),
            FormsMetadata()
        )
    }

    @Test
    fun formsMetadataDoesNotPreventOtherChecksFromRunning() {
        val check = VariableNameCheck()

        TestPlSqlVisitorRunner.scanFile(
            File(getPath("function_should_be_deterministic_forms.sql")),
            FormsMetadata(),
            check
        )

        assertThat(check.issues()).isNotEmpty
    }

    @Test
    fun reportsEligiblePublicPackageFunctions() {
        ProjectPlSqlCheckVerifier.verify(
            listOf(
                ProjectTestSource(
                    "candidate_package_spec.sql",
                    """
                    CREATE PACKAGE candidate_package AS
                      package_state NUMBER;
                      FUNCTION candidate_value(p_value NUMBER) RETURN NUMBER;
                      FUNCTION already_deterministic(p_value NUMBER) RETURN NUMBER DETERMINISTIC;
                      FUNCTION package_state_value RETURN NUMBER;
                      FUNCTION default_date_value(p_date DATE DEFAULT SYSDATE) RETURN DATE;
                      FUNCTION default_number_value(p_value NUMBER DEFAULT 1) RETURN NUMBER;
                    END candidate_package;
                    /
                    """.trimIndent()
                ),
                ProjectTestSource(
                    "candidate_package_body.sql",
                    """
                    CREATE PACKAGE BODY candidate_package AS
                      FUNCTION candidate_value(p_value NUMBER) RETURN NUMBER IS -- Noncompliant
                      BEGIN
                        RETURN p_value * 2;
                      END candidate_value;

                      FUNCTION already_deterministic(p_value NUMBER) RETURN NUMBER IS
                      BEGIN
                        RETURN SYSDATE;
                      END already_deterministic;

                      FUNCTION package_state_value RETURN NUMBER IS
                      BEGIN
                        RETURN package_state;
                      END package_state_value;

                      FUNCTION default_date_value(p_date DATE) RETURN DATE IS
                      BEGIN
                        RETURN p_date;
                      END default_date_value;

                      FUNCTION default_number_value(p_value NUMBER) RETURN NUMBER IS
                      BEGIN
                        RETURN p_value;
                      END default_number_value;

                      FUNCTION private_value RETURN NUMBER IS
                      BEGIN
                        RETURN 1;
                      END private_value;
                    END candidate_package;
                    /
                    """.trimIndent()
                )
            ),
            FunctionShouldBeDeterministicCheck()
        )
    }

    @Test
    fun doesNotTreatPackageDeclaredRoutinesAsOracleBuiltins() {
        ProjectPlSqlCheckVerifier.verify(
            listOf(
                ProjectTestSource(
                    "builtin_name_shadow_spec.sql",
                    """
                    CREATE PACKAGE builtin_name_shadow AS
                      FUNCTION NVL(p_value NUMBER, p_default NUMBER) RETURN NUMBER DETERMINISTIC;
                      FUNCTION LTRIM(p_value VARCHAR2) RETURN VARCHAR2 DETERMINISTIC;
                      FUNCTION RTRIM(p_value VARCHAR2) RETURN VARCHAR2 DETERMINISTIC;
                      FUNCTION TRIM(p_value VARCHAR2) RETURN VARCHAR2 DETERMINISTIC;
                      FUNCTION candidate_nvl(p_value NUMBER) RETURN NUMBER;
                      FUNCTION candidate_ltrim(p_value VARCHAR2) RETURN VARCHAR2;
                      FUNCTION candidate_rtrim(p_value VARCHAR2) RETURN VARCHAR2;
                      FUNCTION candidate_trim(p_value VARCHAR2) RETURN VARCHAR2;
                    END builtin_name_shadow;
                    /
                    """.trimIndent()
                ),
                ProjectTestSource(
                    "builtin_name_shadow_body.sql",
                    """
                    CREATE PACKAGE BODY builtin_name_shadow AS
                      FUNCTION candidate_nvl(p_value NUMBER) RETURN NUMBER IS
                      BEGIN
                        RETURN NVL(p_value, 0);
                      END candidate_nvl;

                      FUNCTION candidate_ltrim(p_value VARCHAR2) RETURN VARCHAR2 IS
                      BEGIN
                        RETURN LTRIM(p_value);
                      END candidate_ltrim;

                      FUNCTION candidate_rtrim(p_value VARCHAR2) RETURN VARCHAR2 IS
                      BEGIN
                        RETURN RTRIM(p_value);
                      END candidate_rtrim;

                      FUNCTION candidate_trim(p_value VARCHAR2) RETURN VARCHAR2 IS
                      BEGIN
                        RETURN TRIM(p_value);
                      END candidate_trim;

                      FUNCTION NVL(p_value NUMBER, p_default NUMBER) RETURN NUMBER IS
                      BEGIN
                        RETURN p_value;
                      END NVL;

                      FUNCTION LTRIM(p_value VARCHAR2) RETURN VARCHAR2 IS
                      BEGIN
                        RETURN p_value;
                      END LTRIM;

                      FUNCTION RTRIM(p_value VARCHAR2) RETURN VARCHAR2 IS
                      BEGIN
                        RETURN p_value;
                      END RTRIM;

                      FUNCTION TRIM(p_value VARCHAR2) RETURN VARCHAR2 IS
                      BEGIN
                        RETURN p_value;
                      END TRIM;
                    END builtin_name_shadow;
                    /
                    """.trimIndent()
                ),
                ProjectTestSource(
                    "builtin_name_clear_spec.sql",
                    """
                    CREATE PACKAGE builtin_name_clear AS
                      FUNCTION candidate_number(p_value NUMBER) RETURN NUMBER;
                      FUNCTION candidate_text(p_value VARCHAR2) RETURN VARCHAR2;
                    END builtin_name_clear;
                    /
                    """.trimIndent()
                ),
                ProjectTestSource(
                    "builtin_name_clear_body.sql",
                    """
                    CREATE PACKAGE BODY builtin_name_clear AS
                      FUNCTION candidate_number(p_value NUMBER) RETURN NUMBER IS -- Noncompliant
                      BEGIN
                        RETURN NVL(p_value, 0);
                      END candidate_number;

                      FUNCTION candidate_text(p_value VARCHAR2) RETURN VARCHAR2 IS -- Noncompliant
                      BEGIN
                        RETURN TRIM(RTRIM(LTRIM(p_value)));
                      END candidate_text;
                    END builtin_name_clear;
                    /
                    """.trimIndent()
                )
            ),
            FunctionShouldBeDeterministicCheck()
        )
    }

    @Test
    fun doesNotTreatLocallyDeclaredTrimRoutinesAsTheOracleBuiltin() {
        ProjectPlSqlCheckVerifier.verify(
            listOf(
                ProjectTestSource(
                    "local_trim_shadow.sql",
                    """
                    CREATE FUNCTION local_trim_shadow(p_value VARCHAR2)
                    RETURN VARCHAR2 IS
                      FUNCTION TRIM(p_value VARCHAR2) RETURN VARCHAR2 IS
                      BEGIN
                        RETURN p_value;
                      END TRIM;
                    BEGIN
                      RETURN TRIM(p_value);
                    END local_trim_shadow;
                    /
                    """.trimIndent()
                ),
                ProjectTestSource(
                    "trim_private_spec.sql",
                    """
                    CREATE PACKAGE trim_private AS
                      FUNCTION candidate(p_value VARCHAR2) RETURN VARCHAR2;
                    END trim_private;
                    /
                    """.trimIndent()
                ),
                ProjectTestSource(
                    "trim_private_body.sql",
                    """
                    CREATE PACKAGE BODY trim_private AS
                      FUNCTION TRIM(p_value VARCHAR2) RETURN VARCHAR2;

                      FUNCTION candidate(p_value VARCHAR2) RETURN VARCHAR2 IS
                      BEGIN
                        RETURN TRIM(p_value);
                      END candidate;

                      FUNCTION TRIM(p_value VARCHAR2) RETURN VARCHAR2 IS
                      BEGIN
                        RETURN p_value;
                      END TRIM;
                    END trim_private;
                    /
                    """.trimIndent()
                )
            ),
            FunctionShouldBeDeterministicCheck()
        )
    }

    @Test
    fun ignoresUnresolvedPackageFunctions() {
        ProjectPlSqlCheckVerifier.verify(
            listOf(
                ProjectTestSource(
                    "missing_package_body.sql",
                    """
                    CREATE PACKAGE BODY missing_package AS
                      FUNCTION missing_spec_value RETURN NUMBER IS
                      BEGIN
                        RETURN 1;
                      END missing_spec_value;
                    END missing_package;
                    /
                    """.trimIndent()
                ),
                ProjectTestSource(
                    "ambiguous_package_spec_one.sql",
                    """
                    CREATE PACKAGE ambiguous_package AS
                      FUNCTION ambiguous_value RETURN NUMBER;
                    END ambiguous_package;
                    /
                    """.trimIndent()
                ),
                ProjectTestSource(
                    "ambiguous_package_spec_two.sql",
                    """
                    CREATE OR REPLACE PACKAGE ambiguous_package AS
                      FUNCTION ambiguous_value RETURN NUMBER;
                    END ambiguous_package;
                    /
                    """.trimIndent()
                ),
                ProjectTestSource(
                    "ambiguous_package_body.sql",
                    """
                    CREATE PACKAGE BODY ambiguous_package AS
                      FUNCTION ambiguous_value RETURN NUMBER IS
                      BEGIN
                        RETURN 1;
                      END ambiguous_value;
                    END ambiguous_package;
                    /
                    """.trimIndent()
                )
            ),
            FunctionShouldBeDeterministicCheck()
        )
    }
}
