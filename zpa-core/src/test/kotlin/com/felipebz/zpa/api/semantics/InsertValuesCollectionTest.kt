//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.api.semantics

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** Names read in the VALUES of a single-table INSERT, where no column of the table can take a name. */
class InsertValuesCollectionTest {
    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    /** For each element read `x(i).f` in a VALUES list: the queries on `x`, on the whole reference and on `f`. */
    private fun values(file: String, sources: Map<String, String>, context: ProjectAnalysisContext = prepared(sources)): List<String> {
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                for (insert in node.getDescendants(DmlGrammar.VALUES_CLAUSE)) {
                    for (reference in insert.getDescendants(PlSqlGrammar.OBJECT_REFERENCE)) {
                        val call = reference.firstChild
                        val name = if (call.type == PlSqlGrammar.METHOD_CALL) call.firstChild else call
                        val field = reference.children.last { it.type != PlSqlPunctuator.DOT }
                        val text = reference.tokens.joinToString("") { it.originalValue }
                        rows += "${reference.token.line}:$text name=${context(name)} call=${this.context.callBinding(name)} " +
                            "value=${this.context.valueCategory(reference)} field=${this.context.fieldBinding(field).javaClass.simpleName}"
                    }
                }
            }

            private fun context(name: AstNode) =
                "${this.context.nameBinding(name).let { if (it is NameBinding.Local) "Local" else it.toString() }}/${this.context.valueCategory(name)}"
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, context)
            .scanFile(Source(file, sources.getValue(file)), listOf(check), FileId(file))
        return rows
    }

    private fun prepared(sources: Map<String, String>): ProjectAnalysisContext = ProjectAnalysisContext.prepared(
        ProjectIndexPreparation().prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
    )

    private val spec = "create package analysis as\n  procedure run;\nend;\n/\n"

    private val types = "  type t_sample is record (item_id number, sample_no number);\n" +
        "  type t_samples is table of t_sample index by pls_integer;\n"

    private val insert = "    for i in 1 .. samples.count loop\n" +
        "      insert into sample_item (item_id, sample_no) values (samples(i).item_id, samples(i).sample_no);\n" +
        "    end loop;\n"

    private val body = "create package body analysis as\n" + types +
        "  samples t_samples;\n" +
        "  procedure run is\n" +
        "  begin\n" + insert +
        "  end;\n" +
        "end;\n/\n"

    /** The rows of an element read bound to a local associative array of records: an element access, not a call. */
    private fun local(line: Int) = listOf("item_id", "sample_no").map {
        "$line:samples(i).$it name=Local/Collection(ASSOCIATIVE_ARRAY of Record(RECORD_TYPE)) call=Unavailable(NOT_A_COVERED_NAME) " +
            "value=Scalar(NUMERIC) field=Field"
    }

    @Test
    fun aCollectionOfThePackageBody() {
        // The specification, in another file, declares no member of the name: the body's variable.
        assertThat(values("b.sql", mapOf("s.sql" to spec, "b.sql" to body))).containsExactlyElementsOf(local(8))
    }

    @Test
    fun aCollectionOfTheProcedure() {
        val inBody = "create package body analysis as\n" + types +
            "  procedure run is\n" +
            "    samples t_samples;\n" +
            "  begin\n" + insert +
            "  end;\n" +
            "end;\n/\n"
        assertThat(values("b.sql", mapOf("s.sql" to spec, "b.sql" to inBody))).containsExactlyElementsOf(local(8))
        val standalone = "create procedure run_analysis is\n" + types +
            "  samples t_samples;\n" +
            "begin\n" + insert +
            "end;\n/\n"
        assertThat(values("p.sql", mapOf("p.sql" to standalone))).containsExactlyElementsOf(local(7))
    }

    @Test
    fun aCollectionOfTheSpecificationInAnotherFile() {
        val declaring = "create package analysis as\n" + types + "  samples t_samples;\n  procedure run;\nend;\n/\n"
        val user = "create package body analysis as\n" +
            "  procedure run is\n" +
            "  begin\n" + insert +
            "  end;\n" +
            "end;\n/\n"
        // The name stays outside the file; its type comes from the project's specification.
        assertThat(values("b.sql", mapOf("s.sql" to declaring, "b.sql" to user))).containsExactly(
            "5:samples(i).item_id name=Unknown[DECLARATION_OUTSIDE_FILE]/Collection(ASSOCIATIVE_ARRAY of Record(RECORD_TYPE)) call=Unavailable(NOT_A_COVERED_NAME) " +
                "value=Scalar(NUMERIC) field=Field",
            "5:samples(i).sample_no name=Unknown[DECLARATION_OUTSIDE_FILE]/Collection(ASSOCIATIVE_ARRAY of Record(RECORD_TYPE)) call=Unavailable(NOT_A_COVERED_NAME) " +
                "value=Scalar(NUMERIC) field=Field"
        )
    }
}
