//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.api.semantics

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.squid.ConditionalCompilationSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** The value of a PL/SQL name written in static SQL, where a column of the same name would take it. */
class ValueCategorySqlNameTest {
    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private val user = "create procedure run_bins is\n" +
        "  v_num number; v_day date; id date; n number;\n" +
        "begin\n" +
        "  select count(*) into n from bin_crate where code = v_num;\n" +
        "  select count(*) into n from bin_crate where code = v_day;\n" +
        "  select count(*) into n from bin_crate where code = id;\n" +
        "end;\n/\n"

    private fun crate(complete: Boolean) = ProjectExternalMetadata.builder("fixture").completeSchema("APP")
        .relation(ProjectExternalRelation.of("APP", "BIN_CRATE", complete, listOf(
            ProjectExternalColumn.of("ID", 1, ProjectExternalDatatype.of("NUMBER", null, null, null, null, null)),
            ProjectExternalColumn.of("CODE", 2, ProjectExternalDatatype.of("VARCHAR2", null, 10, ProjectExternalDatatype.LengthSemantics.BYTE, null, null))
        )))
        .build()

    /** `name=category` of the right side of each `code = …` of [user]. */
    private fun categories(metadata: ProjectExternalMetadata): List<String> {
        val external = ProjectExternalContext.of(metadata, ProjectIdentifier.fromSource("APP"), ProjectSchemaPaths.none())
        val context = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation.withSchemas(ProjectDeclarationExtractor(), external)
                .prepare(listOf(ProjectSource(FileId("u.sql")) { user }), concurrent = false)
        )
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(DmlGrammar.WHERE_CLAUSE).forEach {
                    val value = it.lastChild.lastChild
                    rows += "${value.tokenValue.lowercase()}=${this.context.valueCategory(value)}"
                }
            }
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, context, ConditionalCompilationSettings())
            .scanFile(Source("u.sql", user), listOf(check), FileId("u.sql"))
        return rows
    }

    @Test
    fun aNameNoColumnTakesIsThePlSqlValue() {
        val rows = categories(crate(complete = true))
        assertThat(rows.take(2)).containsExactly("v_num=Scalar(NUMERIC)", "v_day=Scalar(DATETIME)")
        // A column of the same name takes it: never the local's DATE.
        assertThat(rows[2]).startsWith("id=").doesNotContain("DATETIME")
    }

    @Test
    fun aNameAColumnMayTakeStaysUnknown() {
        for (metadata in listOf(crate(complete = false), ProjectExternalMetadata.none())) {
            assertThat(categories(metadata).take(2)).allSatisfy { assertThat(it).contains("Unknown").contains("SQL_NAME_MAY_BE_COLUMN") }
        }
    }
}
