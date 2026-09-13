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

import com.felipebz.zpa.checks.verifier.ProjectPlSqlCheckVerifier
import com.felipebz.zpa.checks.verifier.ProjectTestSource
import org.junit.jupiter.api.Test

class UnresolvedUtPlSqlSetupCleanupReferenceCheckTest : BaseCheckTest() {

    @Test
    fun reportsOnlyDefinitivelyUnresolvedProjectReferences() {
        ProjectPlSqlCheckVerifier.verify(
            listOf(
                ProjectTestSource("test_pkg.pks", testPackageSpecification),
                ProjectTestSource("test_pkg.pkb", testPackageBody),
                ProjectTestSource("helper_pkg.pks", helperPackageSpecification),
                ProjectTestSource("owner_helper_pkg.pks", ownerHelperPackageSpecification),
                ProjectTestSource("ambiguous_pkg_a.pks", ambiguousPackageSpecification),
                ProjectTestSource("ambiguous_pkg_b.pks", ambiguousPackageSpecification),
                ProjectTestSource("no_suite.pks", noSuitePackageSpecification)
            ),
            UnresolvedUtPlSqlSetupCleanupReferenceCheck()
        )
    }

    private companion object {
        private val testPackageSpecification = """
            CREATE PACKAGE test_pkg AS
              -- %suite

              -- Noncompliant@+1 {{This utPLSQL setup/cleanup reference "missing_setup" does not resolve to a callable package procedure.}}
              -- %beforeall(missing_setup)

              -- %beforeall(setup, helper_pkg . setup)

              -- %beforeall(ambiguous_pkg.setup)

              -- Noncompliant@+1 {{This utPLSQL setup/cleanup reference "ambiguous_setup" does not resolve to a callable package procedure.}}
              -- %beforeall(ambiguous_setup)

              -- Noncompliant@+2 {{This utPLSQL setup/cleanup reference "missing_one" does not resolve to a callable package procedure.}}
              -- Noncompliant@+1 {{This utPLSQL setup/cleanup reference "missing_two" does not resolve to a callable package procedure.}}
              -- %beforeall(missing_one, setup, missing_two)

              -- Noncompliant@+1 {{This utPLSQL setup/cleanup reference "missing_mixed" does not resolve to a callable package procedure.}}
              -- %beforeall(setup, missing_mixed, helper_pkg.setup)

              -- Noncompliant@+1 {{This utPLSQL setup/cleanup reference "helper_pkg.missing_setup" does not resolve to a callable package procedure.}}
              -- %beforeall(helper_pkg.missing_setup)

              -- %beforeall(external_util.setup)

              -- Noncompliant@+1 {{This utPLSQL setup/cleanup reference "required_setup" does not resolve to a callable package procedure.}}
              -- %beforeeach(required_setup)

              -- %beforeall(defaulted_setup)

              -- Noncompliant@+1 {{This utPLSQL setup/cleanup reference "function_target" does not resolve to a callable package procedure.}}
              -- %afterall(function_target)

              -- Noncompliant@+1 {{This utPLSQL setup/cleanup reference "missing_aftereach" does not resolve to a callable package procedure.}}
              -- %aftereach(missing_aftereach)

              -- Noncompliant@+1 {{This utPLSQL setup/cleanup reference "private_setup" does not resolve to a callable package procedure.}}
              -- %beforeall(private_setup)

              -- %afterall(owner . helper_pkg . setup)

              -- %test
              -- %beforetest(setup)
              -- %aftertest(helper_pkg.setup)
              PROCEDURE some_test;

              -- Noncompliant@+2 {{This utPLSQL setup/cleanup reference "missing_test_setup" does not resolve to a callable package procedure.}}
              -- %test
              -- %beforetest(missing_test_setup)
              PROCEDURE another_test;

              -- Noncompliant@+2 {{This utPLSQL setup/cleanup reference "missing_after_test" does not resolve to a callable package procedure.}}
              -- %test
              -- %aftertest(missing_after_test)
              PROCEDURE third_test;

              -- %beforeall
              PROCEDURE hook_declared_by_annotation;

              -- %context(Context)

              -- %beforeall(setup)

              -- %test
              PROCEDURE context_test;

              -- %endcontext

              PROCEDURE setup;
              PROCEDURE ambiguous_setup;
              PROCEDURE ambiguous_setup(value NUMBER DEFAULT 1);
              PROCEDURE required_setup(value NUMBER);
              PROCEDURE defaulted_setup(value NUMBER DEFAULT 1);
              FUNCTION function_target RETURN NUMBER;
            END test_pkg;
        """.trimIndent()

        private val testPackageBody = """
            CREATE PACKAGE BODY test_pkg AS
              PROCEDURE hook_declared_by_annotation IS BEGIN NULL; END;
              PROCEDURE private_setup IS BEGIN NULL; END;
            END test_pkg;
        """.trimIndent()

        private val helperPackageSpecification = """
            CREATE PACKAGE helper_pkg AS
              PROCEDURE setup;
            END helper_pkg;
        """.trimIndent()

        private val ownerHelperPackageSpecification = """
            CREATE PACKAGE owner.helper_pkg AS
              PROCEDURE setup;
            END helper_pkg;
        """.trimIndent()

        private val ambiguousPackageSpecification = """
            CREATE PACKAGE ambiguous_pkg AS
              PROCEDURE setup;
            END ambiguous_pkg;
        """.trimIndent()

        private val noSuitePackageSpecification = """
            CREATE PACKAGE no_suite AS
              -- %beforeall(missing_setup)

              PROCEDURE helper;
            END no_suite;
        """.trimIndent()
    }
}
