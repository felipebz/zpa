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
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** The decision of the local analysis for each call, answered by the call's name. */
class CallBindingTest {
    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private val project = "create package pk_audit as\n" +
        "  procedure log_value(p_val number);\n  procedure log_value(p_val varchar2);\n  function fmt return varchar2;\nend;\n/\n"

    private val user = "create procedure run_job is\n" +
        "  v_amount number;\n" +
        "  type nums is table of number;\n" +
        "  v_nums nums;\n" +
        "  x varchar2(10);\n" +
        "  procedure note(p number) is begin null; end;\n" +
        "  procedure note(p date) is begin null; end;\n" +
        "begin\n" +
        "  pk_audit.log_value(v_amount);\n" +
        "  pk_audit.log_value(v_amount + 1);\n" +
        "  note(sysdate);\n" +
        "  x := pk_audit.fmt;\n" +
        "  x := v_nums(1);\n" +
        "  x := v_amount;\n" +
        "  select pk_audit.fmt into x from dual;\n" +
        "end;\n/\n"

    private fun render(binding: CallBinding): String = when (binding) {
        is CallBinding.Project -> {
            val resolution = binding.resolution
            val selected = resolution.selected.map { " (" + it.declaration.parameters.joinToString { p -> "${p.name} ${p.typeRef.name}" } + ")" }.orElse("")
            "Project(${resolution.status}$selected of ${resolution.structural.candidates.size})"
        }
        is CallBinding.Local -> "Local(${binding.status} ${binding.selected} of ${binding.candidates})"
        else -> binding.toString()
    }

    /** `line:name=answer` for every `VARIABLE_NAME` and `MEMBER_EXPRESSION` of the procedure body. */
    private fun answers(): List<String> {
        val sources = mapOf("project.sql" to project, "u.sql" to user)
        val context = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false))
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION).filter { it.token.line >= 9 }.forEach {
                    rows += "${it.token.line}:${it.tokens.joinToString("") { t -> t.originalValue }}=${render(this.context.callBinding(it))}"
                }
            }
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, context).scanFile(Source("u.sql", user), listOf(check), FileId("u.sql"))
        return rows
    }

    @Test
    fun theDecisionOfEachCall() {
        assertThat(answers()).containsExactly(
            // Two overloads that differ only by a parameter type: the actual's type selects one.
            "9:pk_audit.log_value=Project(RESOLVED (p_val number) of 2)",
            "9:pk_audit=Unavailable(NOT_A_COVERED_NAME)",
            "9:v_amount=Unavailable(NOT_A_COVERED_NAME)",
            // An actual of no known type leaves both.
            "10:pk_audit.log_value=Project(AMBIGUOUS of 2)",
            "10:pk_audit=Unavailable(NOT_A_COVERED_NAME)",
            "10:v_amount=Unavailable(NOT_A_COVERED_NAME)",
            // A local overloaded procedure binds its declaration, among both of its set.
            "11:note=Local(RESOLVED PROCEDURE@7:12 of [PROCEDURE@6:12, PROCEDURE@7:12])",
            "11:sysdate=Unavailable(NOT_A_COVERED_NAME)",
            "12:x=Unavailable(NOT_A_COVERED_NAME)",
            // A function named without parentheses.
            "12:pk_audit.fmt=Project(RESOLVED () of 1)",
            "12:pk_audit=Unavailable(NOT_A_COVERED_NAME)",
            "13:x=Unavailable(NOT_A_COVERED_NAME)",
            // An element of a local collection is kept local.
            "13:v_nums=KeptLocal(LOCAL_CAPTURE)",
            "14:x=Unavailable(NOT_A_COVERED_NAME)",
            "14:v_amount=Unavailable(NOT_A_COVERED_NAME)",
            // A function named in SQL is not forwarded.
            "15:pk_audit.fmt=Unavailable(NOT_A_COVERED_NAME)",
            "15:pk_audit=Unavailable(NOT_A_COVERED_NAME)",
            "15:x=Unavailable(NOT_A_COVERED_NAME)"
        )
    }

    @Test
    fun aFileThatWasNotAnalyzed() {
        val context = PlSqlVisitorContext(PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(user), null, null)
        val name = context.rootTree()!!.getDescendants(PlSqlGrammar.MEMBER_EXPRESSION).first()
        assertThat(context.callBinding(name)).isEqualTo(CallBinding.Unavailable(UsageUnavailability.FILE_NOT_ANALYZED))
    }
}
