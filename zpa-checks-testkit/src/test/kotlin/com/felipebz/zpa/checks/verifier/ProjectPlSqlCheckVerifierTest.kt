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
package com.felipebz.zpa.checks.verifier

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.RecognitionException
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.project.PackageSpecificationResolution
import com.felipebz.zpa.squid.ConditionalCompilationSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProjectPlSqlCheckVerifierTest {

    @Test
    fun verifiesAProjectAwareCheckAgainstMultipleSources() {
        ProjectPlSqlCheckVerifier.verify(
            listOf(
                ProjectTestSource(
                    "p-spec.sql",
                    """
                        CREATE PACKAGE p AS
                          PROCEDURE work(value IN OUT NOCOPY CLOB);
                        END p;
                    """.trimIndent()
                ),
                ProjectTestSource(
                    "p-body.sql",
                    """
                        CREATE PACKAGE BODY p AS
                          PROCEDURE work(value IN OUT CLOB) IS -- Noncompliant {{NOCOPY mismatch}}
                          BEGIN
                            NULL;
                          END work;
                        END p;
                    """.trimIndent()
                )
            ),
            NocopyCheck()
        )
    }

    @Test
    fun checksThePreciseDetailsAFixtureWrites() {
        val source = "begin\n  null; -- Noncompliant {{null statement}} [[sc=3;ec=8;el=+0]]\nend;\n/\n"
        ProjectPlSqlCheckVerifier.verify(listOf(ProjectTestSource("precise.sql", source)), NullStatementCheck())

        val wrong = source.replace("ec=8", "ec=9")
        val failure = assertThrows<AssertionError> {
            ProjectPlSqlCheckVerifier.verify(listOf(ProjectTestSource("precise.sql", wrong)), NullStatementCheck())
        }
        assertThat(failure).hasMessage("precise.sql: Bad end column at line 2 ==> expected: \"9\" but was: \"8\"")
    }

    @Test
    fun settingsApplyToThePreparationAndTheScan() {
        // The settings remove the whole $IF from the specification: w is no longer declared for the other file.
        val spec = ProjectTestSource("pkg.pks", "create package pkg is\n  v number;\n  \$if \$\$extra \$then\n  w varchar2(10);\n  \$end\nend;\n/\n")
        val use = ProjectTestSource("use.sql", "begin\n  select 1 into pkg.w from dual; -- Noncompliant {{scalar destination}}\nend;\n/\n")
        ProjectPlSqlCheckVerifier.verify(listOf(spec, use), ScalarDestinationCheck())

        val failure = assertThrows<AssertionError> {
            ProjectPlSqlCheckVerifier.verify(listOf(spec, use), ScalarDestinationCheck(), conditionalCompilation = ConditionalCompilationSettings.of("extra:false", null))
        }
        assertThat(failure).hasMessage("use.sql: Missing issue at line 2")
    }

    @Test
    fun aSourceThatDoesNotParseFailsWithRecoveryOff() {
        val broken = ProjectTestSource("broken.sql", "begin\n  this is not plsql;\nend;\n/\n")
        ProjectPlSqlCheckVerifier.verify(listOf(broken), NullStatementCheck())
        assertThrows<RecognitionException> {
            ProjectPlSqlCheckVerifier.verify(listOf(broken), NullStatementCheck(), errorRecovery = false)
        }
    }

    private class NullStatementCheck : PlSqlCheck() {
        override fun init() {
            subscribeTo(PlSqlGrammar.NULL_STATEMENT)
        }

        override fun visitNode(node: AstNode) {
            addIssue(node, "null statement")
        }
    }

    @OptIn(ZpaExperimentalApi::class)
    private class NocopyCheck : PlSqlCheck() {
        override fun init() {
            subscribeTo(PlSqlGrammar.PROCEDURE_DECLARATION)
        }

        override fun visitNode(node: AstNode) {
            val resolution = projectAnalysis().resolvePackageSpecification(node)
            if (resolution.status != PackageSpecificationResolution.Status.RESOLVED) {
                return
            }

            val body = resolution.getBody().orElseThrow()
            val specification = resolution.getSpecification().orElseThrow()
            val specificationByOrdinal = specification.parameters.associateBy { it.ordinal }
            if (body.parameters.any { parameter ->
                    val specificationParameter = specificationByOrdinal[parameter.ordinal]
                    specificationParameter != null && specificationParameter.isNocopy && !parameter.isNocopy
                }) {
                addIssue(node, "NOCOPY mismatch")
            }
        }
    }
}
