//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.api.semantics

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.project.ProjectTypedCallResolution
import com.felipebz.zpa.metadata.FormsMetadata
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** Host variables of an Oracle Forms module typed from the Forms metadata: values and call actuals. */
class FormsHostVariableTest {

    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private val metadata = FormsMetadata.loadFromFile("src/test/resources/metadata/typed_metadata.json")!!

    private val spec = "create package pk_ord as\n  procedure load(p number);\n  procedure load(p varchar2);\nend;\n/\n"

    private fun analyze(text: String, forms: FormsMetadata?, query: PlSqlVisitorContext.(AstNode) -> List<String>): List<String> {
        val project = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation().prepare(listOf("pk_ord.pks" to spec, "u.sql" to text).map { (id, t) -> ProjectSource(FileId(id)) { t } }, concurrent = false)
        )
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(text)
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                rows += context.query(node)
            }
        }
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("u.sql")).all + check)
            .walk(PlSqlVisitorContext(root, Source("u.sql", text), forms), SemanticScanCapabilities(createProjectAnalysis(project)))
        return rows
    }

    private fun text(node: AstNode) = node.tokens.joinToString("") { it.originalValue }

    /** `line:host variable=category` for each host variable `:qualifier.name` of [text]. */
    private fun categories(text: String, forms: FormsMetadata?) = analyze(text, forms) { root ->
        root.getDescendants(PlSqlGrammar.MEMBER_EXPRESSION).filter { it.firstChild.firstChild?.type === PlSqlGrammar.HOST_AND_INDICATOR_VARIABLE }
            .map { "${it.token.line}:${text(it)}=${valueCategory(it)}" }
    }

    /** `line:actual -> status formal` for each call of `pk_ord.load` of [text]. */
    private fun calls(text: String, forms: FormsMetadata?) = analyze(text, forms) { root ->
        root.getDescendants(PlSqlGrammar.METHOD_CALL).filter { text(it.firstChild).equals("pk_ord.load", ignoreCase = true) }.map { call ->
            val typed = (callBinding(call.firstChild) as CallBinding.Project).resolution
            val formal = typed.selected.map { " " + it.declaration.parameters.single().typeRef.name }.orElse("")
            "${call.token.line}:${text(call.getFirstChild(PlSqlGrammar.ARGUMENTS)).drop(1).dropLast(1)} -> ${typed.status}$formal"
        }
    }

    private val values = """
        declare
          v varchar2(100);
        begin
          v := :orders.order_id;
          v := :ORDERS.Placed_On;
          v := :orders.label;
          v := :orders.customer;
          v := :orders.note;
          v := :orders.legacy_qty;
          v := :orders.status;
          v := :parameter.p_order;
          v := :parameter.p_mode;
          v := :parameter.p_any;
          v := :global.user_id;
          v := :system.cursor_record;
          v := :system.tab_new_page;
        end;
        /
        """.trimIndent() + "\n"

    @Test
    fun theCategoryOfAHostVariable() {
        assertThat(categories(values, metadata)).containsExactly(
            "4::orders.order_id=Scalar(NUMERIC)",
            "5::ORDERS.Placed_On=Scalar(DATETIME)",
            "6::orders.label=Scalar(CHARACTER)",
            // No data type, a legacy data type whose reference type is not documented, an item the metadata does not list.
            "7::orders.customer=Unavailable(NOT_A_COVERED_NAME)",
            "8::orders.note=Unavailable(NOT_A_COVERED_NAME)",
            "9::orders.legacy_qty=Unavailable(NOT_A_COVERED_NAME)",
            "10::orders.status=Unavailable(NOT_A_COVERED_NAME)",
            "11::parameter.p_order=Scalar(NUMERIC)",
            "12::parameter.p_mode=Scalar(CHARACTER)",
            "13::parameter.p_any=Unavailable(NOT_A_COVERED_NAME)",
            // Forms holds every global as a character string, and documents the system variable as one.
            "14::global.user_id=Scalar(CHARACTER)",
            "15::system.cursor_record=Scalar(CHARACTER)",
            "16::system.tab_new_page=Unavailable(NOT_A_COVERED_NAME)"
        )
        // Without metadata nothing is typed.
        assertThat(categories(values, null)).allMatch { it.endsWith("=Unavailable(NOT_A_COVERED_NAME)") }
    }

    @Test
    fun anItemsTypeChoosesTheOverload() {
        val text = """
            begin
              pk_ord.load(:orders.order_id);
              pk_ord.load(:orders.label);
              pk_ord.load(:parameter.p_order);
              pk_ord.load(:orders.customer);
              pk_ord.load(:orders.status);
            end;
            /
            """.trimIndent() + "\n"
        assertThat(calls(text, metadata)).containsExactly(
            "2::orders.order_id -> RESOLVED number",
            "3::orders.label -> RESOLVED varchar2",
            "4::parameter.p_order -> RESOLVED number",
            // An item without a data type, and one the metadata does not list, narrow nothing.
            "5::orders.customer -> AMBIGUOUS",
            "6::orders.status -> AMBIGUOUS"
        )
        assertThat(calls(text, null)).allMatch { it.endsWith("-> AMBIGUOUS") }
    }
}
