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

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.TestPlSqlVisitorRunner
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.annotations.ActivatedByDefault
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.checks.verifier.ProjectPlSqlCheckVerifier
import com.felipebz.zpa.checks.verifier.ProjectTestSource
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.symbols.DefaultTypeSolver
import com.felipebz.zpa.symbols.SymbolVisitor
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class SequenceNextvalInConditionalResultCheckTest : BaseCheckTest() {

    @Test
    fun isActivatedByDefaultAfterSemanticResolution() {
        assertThat(SequenceNextvalInConditionalResultCheck::class.java
            .getAnnotation(ActivatedByDefault::class.java)).isNotNull
    }

    @Test
    fun unresolvedMembersAreNotResolvedAsSequences() {
        val members = mutableListOf<SemanticAstNode>()
        val probe = object : PlSqlCheck() {
            override fun init() {
                subscribeTo(PlSqlGrammar.MEMBER_EXPRESSION)
            }

            override fun visitNode(node: AstNode) {
                if (node.getDescendants(PlSqlKeyword.NEXTVAL).isNotEmpty()) {
                    members += node as SemanticAstNode
                }
            }
        }
        val file = File.createTempFile("sequence-nextval-ambiguous-member", ".sql")
        file.writeText(
            "select case when flag = 1 then t.nextval else 0 end from some_table t;"
        )

        try {
            TestPlSqlVisitorRunner.scanFile(
                file,
                null,
                SymbolVisitor(DefaultTypeSolver(), isGlobalContext = true),
                probe
            )

            assertThat(members).hasSize(1)
            assertThat(members.single().symbol).isNull()
        } finally {
            file.delete()
        }
    }

    @Test
    fun reportsSequenceNextvalOnlyInConditionalResults() {
        val source = File(getPath("sequence_nextval_in_conditional_result.sql")).readText()
        ProjectPlSqlCheckVerifier.verify(
            listOf(
                ProjectTestSource(
                    "sequences.sql",
                    """
                        CREATE SEQUENCE sequence_one;
                        CREATE SEQUENCE sequence_two;
                        CREATE SEQUENCE sequence_three;
                        CREATE SEQUENCE owner_name.sequence_one;
                        CREATE SEQUENCE "SEQUENCE_ONE";
                    """.trimIndent()
                ),
                ProjectTestSource("sequence_nextval_in_conditional_result.sql", source)
            ),
            SequenceNextvalInConditionalResultCheck()
        )
    }

    @Test
    fun reportsEveryNextvalInIndependentConditionalResults() {
        val file = File.createTempFile("sequence-nextval-in-conditional-result", ".sql")
        file.writeText(
            """
            begin
              select case
                       when condition = 1 then sequence_one.nextval
                       when condition = 2 then sequence_two.nextval
                       else sequence_three.nextval
                     end into value from dual;
              select case when condition = 1 then sequence_one.nextval + sequence_two.nextval else 0 end into value from dual;
            end;
            """.trimIndent()
        )

        try {
            val check = SequenceNextvalInConditionalResultCheck()
            val fileId = FileId(file.name)
            val projectContext = ProjectAnalysisContext.prepared(
                ProjectIndexPreparation().prepare(
                    listOf(
                        ProjectSource(FileId("sequence-declarations.sql")) {
                            "CREATE SEQUENCE sequence_one; CREATE SEQUENCE sequence_two; " +
                                "CREATE SEQUENCE sequence_three;"
                        },
                        ProjectSource(fileId) { file.readText() }
                    ),
                    concurrent = false
                )
            )
            AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, projectContext)
                .scanFile(
                    FixtureFile(fileId, file.readText()),
                    listOf(check),
                    fileId
                )

            assertThat(check.issues()).hasSize(5)
            assertThat(check.issues().map { it.primaryLocation().startLine() })
                .containsExactly(3, 4, 5, 7, 7)
        } finally {
            file.delete()
        }
    }

    @Test
    fun doesNotTreatUpdateOrMergeQualifiersAsProjectSequences() {
        ProjectPlSqlCheckVerifier.verify(
            listOf(
                ProjectTestSource(
                    "sequences.sql",
                    "CREATE SEQUENCE t; CREATE SEQUENCE src; CREATE SEQUENCE order_seq;"
                ),
                ProjectTestSource(
                    "dml.sql",
                    """
                    BEGIN
                    UPDATE some_table t
                       SET value = CASE WHEN flag = 1 THEN t.NEXTVAL ELSE 0 END;

                    MERGE INTO target_table t
                    USING source_table src
                       ON (t.id = src.id)
                    WHEN MATCHED THEN
                      UPDATE SET value = CASE
                        WHEN flag = 1 THEN src.NEXTVAL
                        ELSE 0
                      END;

                    UPDATE some_table t
                       SET value = CASE WHEN flag = 1 THEN order_seq.NEXTVAL ELSE 0 END; -- Noncompliant

                    MERGE INTO target_table t
                    USING source_table src
                       ON (t.id = src.id)
                    WHEN MATCHED THEN
                      UPDATE SET value = CASE
                        WHEN flag = 1 THEN order_seq.NEXTVAL -- Noncompliant
                        ELSE 0
                      END;
                    END;
                    """.trimIndent()
                )
            ),
            SequenceNextvalInConditionalResultCheck()
        )
    }

    @Test
    fun doesNotTreatSelectTailOrValuesAliasesAsProjectSequences() {
        ProjectPlSqlCheckVerifier.verify(
            listOf(
                ProjectTestSource(
                    "sequences.sql",
                    "CREATE SEQUENCE t; CREATE SEQUENCE order_seq;"
                ),
                ProjectTestSource(
                    "select.sql",
                    """
                    DECLARE
                      result NUMBER;
                    BEGIN
                      SELECT 1 INTO result
                        FROM some_table t
                       ORDER BY CASE WHEN flag = 1 THEN t.NEXTVAL ELSE 0 END;

                      SELECT 1 INTO result
                        FROM some_table t
                       ORDER BY CASE WHEN flag = 1 THEN order_seq.NEXTVAL ELSE 0 END; -- Noncompliant

                      SELECT 1 INTO result
                        FROM (VALUES (1)) AS t(value)
                       ORDER BY CASE WHEN 1 = 1 THEN t.NEXTVAL ELSE 0 END;

                      SELECT 1 INTO result
                        FROM (VALUES (1)) AS t(value)
                       ORDER BY CASE WHEN 1 = 1 THEN order_seq.NEXTVAL ELSE 0 END; -- Noncompliant
                    END;
                    """.trimIndent()
                )
            ),
            SequenceNextvalInConditionalResultCheck()
        )
    }

    @Test
    fun doesNotReportCorrelatedOuterQualifiersAsProjectSequences() {
        ProjectPlSqlCheckVerifier.verify(
            listOf(
                ProjectTestSource(
                    "sequences.sql",
                    "CREATE SEQUENCE t; CREATE SEQUENCE order_seq;"
                ),
                ProjectTestSource(
                    "correlated.sql",
                    """
                    DECLARE
                      result NUMBER;
                    BEGIN
                      SELECT 1 INTO result
                        FROM some_table t
                       WHERE EXISTS (
                             SELECT CASE WHEN 1 = 1 THEN t.NEXTVAL ELSE 0 END
                               FROM dual
                       );

                      SELECT 1 INTO result
                        FROM some_table t
                       WHERE EXISTS (
                             SELECT CASE WHEN 1 = 1 THEN order_seq.NEXTVAL ELSE 0 END -- Noncompliant
                               FROM dual
                       );
                    END;
                    """.trimIndent()
                )
            ),
            SequenceNextvalInConditionalResultCheck()
        )
    }

    private class FixtureFile(
        private val fileId: FileId,
        private val source: String
    ) : PlSqlFile {
        override fun contents() = source
        override fun fileName() = fileId.value
        override fun path(): Path = Path.of(fileId.value)
        override fun type() = PlSqlFile.Type.MAIN
    }
}
