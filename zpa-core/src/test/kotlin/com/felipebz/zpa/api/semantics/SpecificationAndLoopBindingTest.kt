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
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** The cursor of a cursor FOR loop, and the types of a package specification used in its body in another file. */
class SpecificationAndLoopBindingTest {
    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private val spec = "create package pk_ship as\n" +
        "  type t_line is record (line_no number, qty number);\n" +
        "  cursor c_open is select 1 as id from dual;\n" +
        "end;\n/\n"

    private val body = "create package body pk_ship as\n" +
        "  cursor c_body is select 1 as id from dual;\n" +
        "  v_line t_line;\n" +
        "  v_next pk_ship.t_line;\n" +
        "  procedure run is\n" +
        "    cursor c_items(p_min number) is select 1 as id from dual;\n" +
        "    v_min number;\n" +
        "  begin\n" +
        "    for r in c_items(v_min) loop null; end loop;\n" +
        "    for r in c_body loop null; end loop;\n" +
        "    for r in c_open loop null; end loop;\n" +
        "  end;\n" +
        "  procedure hide is\n" +
        "    cursor c_body is select 2 as id from dual;\n" +
        "    type t_line is record (other varchar2(10));\n" +
        "    v_own t_line;\n" +
        "  begin\n" +
        "    for r in c_body loop null; end loop;\n" +
        "  end;\n" +
        "end;\n/\n"

    private val report = "create procedure p_report is\n" +
        "  cursor c_items(p_min number) is select item_id from item where qty >= p_min;\n" +
        "  cursor c_all is select item_id from item;\n" +
        "begin\n" +
        "  for r in c_items(10) loop null; end loop;\n" +
        "  for r in c_all loop null; end loop;\n" +
        "  for r in c_missing loop null; end loop;\n" +
        "end;\n/\n"

    private val orphan = "create package body pk_lost as\n  v_line t_line;\nend;\n/\n"

    private val sources = mapOf("ship.pks" to spec, "ship.pkb" to body, "report.sql" to report, "lost.pkb" to orphan)

    private fun scan(file: String, answer: (PlSqlVisitorContext, AstNode) -> String?): List<String> {
        val context = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false))
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION, PlSqlGrammar.DATATYPE).forEach { name ->
                    answer(this.context, name)?.let { rows += "${name.token.line}:${name.tokens.joinToString("") { it.originalValue }}=$it" }
                }
            }
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, context).scanFile(Source(file, sources.getValue(file)), listOf(check), FileId(file))
        return rows
    }

    private fun names(file: String) = scan(file) { context, node -> if (node.type === PlSqlGrammar.DATATYPE) null else context.nameBinding(node).toString() }

    private fun types(file: String) = scan(file) { context, node ->
        if (node.parent.type === PlSqlGrammar.CUSTOM_DATATYPE) context.typeBinding(node).toString() else null
    }

    @Test
    fun theCursorOfACursorForLoop() {
        assertThat(names("report.sql").filter { it.startsWith("5:") || it.startsWith("6:") || it.startsWith("7:") }).containsExactly(
            "5:c_items=Local(CURSOR@2:9)",
            "6:c_all=Local(CURSOR@3:9)",
            "7:c_missing=NothingLocal"
        )
        assertThat(names("ship.pkb").filter { line -> listOf("9:", "10:", "11:", "18:").any { line.startsWith(it) } }).containsExactly(
            // A cursor of the procedure, with a local variable as its actual.
            "9:c_items=Local(CURSOR@6:11)",
            "9:v_min=Local(VARIABLE@7:4)",
            // A cursor of the package body, and one of the specification in another file.
            "10:c_body=Local(CURSOR@2:9)",
            "11:c_open=Local(CURSOR@ship.pks:3:2)",
            // A cursor of the procedure hides the body's.
            "18:c_body=Local(CURSOR@14:11)"
        )
    }

    @Test
    fun aTypeOfTheSpecificationInAnotherFile() {
        assertThat(types("ship.pkb")).containsExactly(
            "3:t_line=Local(TYPE@ship.pks:2:2)",
            "4:pk_ship.t_line=Local(TYPE@ship.pks:2:2)",
            // A type of the procedure hides the specification's.
            "16:t_line=Local(TYPE@15:9)"
        )
        // The specification is not in the project: undecided.
        assertThat(types("lost.pkb")).containsExactly("2:t_line=Unknown[DECLARATION_OUTSIDE_FILE]")
    }

    @Test
    fun theOtherAnswersAgreeWithTheBinding() {
        val answers = scan("ship.pkb") { context, node ->
            if (node.type !== PlSqlGrammar.DATATYPE || node.token.line != 3) null
            else {
                val declared = context.declaredType(node) as DeclaredType.Project
                val range = declared.declaration.sourceRange
                "${context.typeCategory(node)} ${declared.declaration.fileId}:${range.startLine}:${range.startColumn} " +
                    "${context.recordFields(node.firstChild.firstChild)}"
            }
        }
        // The same declaration the binding names; its fields are the specification's, not a record type of this file.
        assertThat(answers).containsExactly("3:t_line=Record(RECORD_TYPE) ship.pks:2:2 null")
    }
}
