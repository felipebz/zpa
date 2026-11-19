//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.api.semantics

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
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

/** The value of a declaration anchored to a column's `%TYPE`, typed by the column the metadata gives. */
class ValueCategoryColumnTypeTest {
    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private fun type(name: String, owner: String? = null) = ProjectExternalDatatype.of(name, owner, null, null, null, null)

    /** A relation whose columns are not complete and that lists none: no column is proven. */
    private val partial = ProjectExternalMetadata.builder("fixture").completeSchema("APP")
        .relation(ProjectExternalRelation.of("APP", "BIN_CRATE", false, emptyList())).build()

    private fun metadata() = ProjectExternalMetadata.builder("fixture").completeSchema("APP")
        .relation(ProjectExternalRelation.of("APP", "BIN_CRATE", true, listOf(
            ProjectExternalColumn.of("ID", 1, type("NUMBER")),
            ProjectExternalColumn.of("CODE", 2, type("VARCHAR2")),
            ProjectExternalColumn.of("PACKED_ON", 3, type("DATE")),
            ProjectExternalColumn.of("NOTE", 4, type("CLOB")),
            ProjectExternalColumn.of("SPOT", 5, type("BIN_POINT", "APP")),
            ProjectExternalColumn.of("DOC", 6, type("XMLTYPE"))
        )))
        .build()

    /** An object type and a table created in the sources. */
    private val tables = "create type bin_mark as object (x number);\n/\n" +
        "create table src_bin (spot bin_mark, own app.bin_mark, doc xmltype, tag varchar2(5));\n"

    private val spec = "create package pk_bins as\n  g_id bin_crate.id%type;\n  g_len number;\n  g_size g_len%type;\nend;\n/\n"

    private val user = "create procedure run_bins(p_id bin_crate.id%type) is\n" +
        "  v_id bin_crate.id%type;\n" +
        "  c_code constant bin_crate.code%type := 'A';\n" +
        "  v_day bin_crate.packed_on%type;\n" +
        "  v_copy v_id%type;\n" +
        "  v_note bin_crate.note%type;\n" +
        "  v_spot bin_crate.spot%type;\n" +
        "  v_doc bin_crate.doc%type;\n" +
        "  v_src_spot src_bin.spot%type;\n" +
        "  v_src_own src_bin.own%type;\n" +
        "  v_src_doc src_bin.doc%type;\n" +
        "  v_src_tag src_bin.tag%type;\n" +
        "  v_gone bin_crate.missing%type;\n" +
        "  v_far bin_crate.id@far_db%type;\n" +
        "  v_row bin_crate%rowtype;\n" +
        "  x number;\n" +
        "begin\n" +
        "  x := p_id;\n" +
        "  x := v_id;\n" +
        "  x := c_code;\n" +
        "  x := v_day;\n" +
        "  x := v_copy;\n" +
        "  x := pk_bins.g_id;\n" +
        "  x := v_note;\n" +
        "  x := v_spot;\n" +
        "  x := v_doc;\n" +
        "  x := v_src_spot;\n" +
        "  x := v_src_own;\n" +
        "  x := v_src_doc;\n" +
        "  x := v_src_tag;\n" +
        "  x := v_gone;\n" +
        "  x := v_far;\n" +
        "  x := pk_bins.g_size;\n" +
        "  x := v_row.id;\n" +
        "end;\n/\n"

    /** `name=category` of the value of each assignment of [user]. */
    private fun categories(metadata: ProjectExternalMetadata): Map<String, String> {
        val sources = mapOf("tables.sql" to tables, "spec.sql" to spec, "u.sql" to user)
        val external = ProjectExternalContext.of(metadata, ProjectIdentifier.fromSource("APP"), ProjectSchemaPaths.none())
        val context = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation.withSchemas(ProjectDeclarationExtractor(), external)
                .prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
        )
        val rows = LinkedHashMap<String, String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                for (assignment in node.getDescendants(PlSqlGrammar.ASSIGNMENT_STATEMENT)) {
                    val value = assignment.children[2]
                    rows[value.tokens.joinToString("") { it.originalValue }.lowercase()] = this.context.valueCategory(value).toString()
                }
            }
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, context, ConditionalCompilationSettings())
            .scanFile(Source("u.sql", user), listOf(check), FileId("u.sql"))
        return rows
    }

    @Test
    fun aColumnTheMetadataProvesGivesItsFamily() {
        val rows = categories(metadata())
        assertThat(rows).containsEntry("p_id", "Scalar(NUMERIC)").containsEntry("v_id", "Scalar(NUMERIC)")
            .containsEntry("c_code", "Scalar(CHARACTER)").containsEntry("v_day", "Scalar(DATETIME)")
            .containsEntry("v_copy", "Scalar(NUMERIC)").containsEntry("pk_bins.g_id", "Scalar(NUMERIC)")
            .containsEntry("v_note", "Scalar(CLOB)")
        // A VARCHAR2 column of a table created in the sources.
        assertThat(rows).containsEntry("v_src_tag", "Scalar(CHARACTER)")
        // A %TYPE the sources resolve keeps its answer.
        assertThat(rows).containsEntry("pk_bins.g_size", "Scalar(NUMERIC)")
    }

    @Test
    fun anythingTheMetadataDoesNotProveStaysUnknown() {
        val rows = categories(metadata())
        // A column of an object type, with or without its owner, XMLTYPE, a column not found and a remote anchor.
        for (name in listOf("v_spot", "v_doc", "v_gone", "v_far", "v_src_spot", "v_src_own", "v_src_doc")) assertThat(rows[name]).startsWith("Unknown")
        for (metadata in listOf(partial, ProjectExternalMetadata.none())) {
            val unproven = categories(metadata)
            for (name in listOf("p_id", "v_id", "c_code", "v_day", "v_copy", "pk_bins.g_id")) assertThat(unproven[name]).describedAs("$name $metadata").startsWith("Unknown")
        }
    }

    @Test
    fun aFieldOfARowtypeOfTheMetadata() {
        assertThat(categories(metadata())["v_row.id"]).isEqualTo("Scalar(NUMERIC)")
        assertThat(categories(partial)["v_row.id"]).startsWith("Unknown")
    }
}
