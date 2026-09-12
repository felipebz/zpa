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

import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.project.PackageSpecificationResolution
import com.felipebz.flr.api.AstNode
import org.junit.jupiter.api.Test

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
