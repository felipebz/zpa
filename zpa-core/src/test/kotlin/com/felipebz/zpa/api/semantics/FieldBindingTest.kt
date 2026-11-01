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
@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.api.semantics

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
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

class FieldBindingTest {

    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    /**
     * `line:column:NAME=answer` for each segment after the first of the qualified names of [file], and for each member
     * written after an element or a call result, with every source in the prepared project.
     */
    private fun fields(file: String, vararg sources: Pair<String, String>): List<String> {
        val project = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation().prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
        )
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                val segments = node.getDescendants(PlSqlGrammar.MEMBER_EXPRESSION).flatMap { member ->
                    val names = member.getChildren(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.IDENTIFIER_NAME)
                    // A member written after an expression receiver starts with a member.
                    if (member.parent.type == PlSqlGrammar.OBJECT_REFERENCE && member.parent.firstChild !== member) names else names.drop(1)
                } + node.getDescendants(PlSqlGrammar.OBJECT_REFERENCE).flatMap { it.getChildren(PlSqlGrammar.VARIABLE_NAME) }
                segments.sortedWith(compareBy({ it.token.line }, { it.token.column }))
                    .forEach { rows += "${it.token.line}:${it.token.column}:${it.tokenValue}=${this.context.fieldBinding(it)}" }
            }
        }
        val scanner = AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, project)
        scanner.scanFile(Source(file, sources.toMap().getValue(file)), listOf(check), FileId(file))
        return rows
    }

    private val spec = "create or replace package pk_f is\n" +
        "  type t_nums is table of number;\n" +
        "  type t_row is record (items t_nums, code varchar2(10));\n" +
        "  type t_rows is table of t_row index by pls_integer;\n" +
        "  type t_outer is record (inner t_row);\n" +
        "  s_rec t_row;\n" +
        "  function f_row return t_row;\n" +
        "end pk_f;\n/\n"

    private val body = "create or replace package body pk_f is\n" +
        "  g_rows t_rows;\n" +
        "  g_rec t_row;\n" +
        "  function f_row return t_row is begin return g_rec; end;\n" +
        "  procedure run(p_rec t_row) is\n" +
        "    type t_loc is record (n number);\n" +
        "    l_loc t_loc;\n" +
        "    l_out t_outer;\n" +
        "    l_row dual%rowtype;\n" +
        "  begin\n" +
        "    g_rec.code := 'x';\n" +
        "    g_rows(1).items.delete;\n" +
        "    l_out.inner.code := 'y';\n" +
        "    l_loc.n := 1;\n" +
        "    pk_f.s_rec.code := 'z';\n" +
        "    l_loc.n := f_row().code;\n" +
        "    select dummy into l_row.dummy from dual;\n" +
        "    if p_rec.code = 'a' then null; end if;\n" +
        "    select count(*) into l_loc.n from dual where g_rec.code = 'b';\n" +
        "  end;\n" +
        "end pk_f;\n/\n"

    private val user = "create or replace procedure use_f is\n" +
        "  l_r pk_f.t_row;\n" +
        "begin\n" +
        "  l_r.items := pk_f.t_nums();\n" +
        "  pk_f.s_rec.code := 'w';\n" +
        "end;\n/\n"

    @Test
    fun aSegmentAfterAValueNamesTheFieldOfItsType() {
        val code = "Field(RECORD_FIELD#1@3:38)"
        val items = "Field(RECORD_FIELD#0@3:24)"
        val notAField = "Unavailable(NOT_A_COVERED_NAME)"
        assertThat(fields("pk_f.sql", "pk_f.sql" to spec + body)).containsExactly(
            "20:10:CODE=$code",
            // Through an element; DELETE is a collection method.
            "21:14:ITEMS=$items", "21:20:DELETE=$notAField",
            // Through another field.
            "22:10:INNER=Field(RECORD_FIELD#0@5:26)", "22:16:CODE=$code",
            "23:10:N=Field(RECORD_FIELD#0@15:26)",
            // Through the package's own name: `s_rec` is a name, not a field.
            "24:9:S_REC=$notAField", "24:15:CODE=$code",
            // Through a call result.
            "25:10:N=Field(RECORD_FIELD#0@15:26)", "25:23:CODE=$code",
            // A column of a %ROWTYPE is not a field of a record type.
            "26:28:DUMMY=$notAField",
            "27:13:CODE=$code",
            // In SQL.
            "28:31:N=Field(RECORD_FIELD#0@15:26)", "28:55:CODE=$code"
        )
    }

    @Test
    fun aFieldOfATypeOfAnotherFileIsPlacedInThatFile() {
        assertThat(fields("use_f.sql", "pk_f.sql" to spec + body, "use_f.sql" to user)).containsExactly(
            // The type name and a constructor are names, not fields.
            "2:11:T_ROW=Unavailable(NOT_A_COVERED_NAME)",
            "4:6:ITEMS=Field(RECORD_FIELD#0@pk_f.sql:3:24)", "4:20:T_NUMS=Unavailable(NOT_A_COVERED_NAME)",
            // A package variable of another file.
            "5:7:S_REC=Unavailable(NOT_A_COVERED_NAME)", "5:13:CODE=Field(RECORD_FIELD#1@pk_f.sql:3:38)"
        )
    }

    @Test
    fun objectTypeAttributes() {
        val type = "create or replace type ty_box as object (w number, h number, member function area return number);\n/\n" +
            "create or replace type body ty_box as\n  member function area return number is begin return self.w * h; end;\nend;\n/\n"
        val proc = "create or replace procedure use_box is\n  l_b ty_box;\n  n number;\nbegin\n  n := l_b.h;\nend;\n/\n"
        assertThat(fields("u.sql", "u.sql" to type + proc)).containsExactly(
            "4:58:W=Field(ATTRIBUTE#0@1:41)", "11:11:H=Field(ATTRIBUTE#1@1:51)"
        )
        assertThat(fields("p.sql", "t.sql" to type, "p.sql" to proc)).containsExactly("5:11:H=Field(ATTRIBUTE#1@t.sql:1:51)")
    }

    private val conditionalSpec = "create or replace package pk_cc is\n" +
        "  type t_cc is record (\n" +
        "    \$IF \$\$wide \$THEN amount number, \$END\n" +
        "    label varchar2(10)\n" +
        "  );\n" +
        "  g_cc t_cc;\n" +
        "end pk_cc;\n/\n"

    @Test
    fun aConditionalTypeIsUndecided() {
        val conditional = "Unknown[CONDITIONAL_COMPILATION]"
        val body = "create or replace package body pk_cc is\n  procedure run is\n  begin\n    g_cc.label := 'a';\n  end;\nend pk_cc;\n/\n"
        assertThat(fields("cc.sql", "cc.sql" to conditionalSpec + body)).containsExactly("12:9:LABEL=$conditional")
        val type = "create or replace type ty_cc as object (\n  \$IF \$\$wide \$THEN w number, \$END\n  z number\n);\n/\n"
        val user = "create or replace procedure use_cc is\n  l_c pk_cc.t_cc;\n  l_z ty_cc;\nbegin\n" +
            "  l_c.label := 'b';\n  pk_cc.g_cc.label := 'c';\n  l_z.z := 1;\nend;\n/\n"
        assertThat(fields("u.sql", "cc.sql" to conditionalSpec, "t.sql" to type, "u.sql" to user)).containsExactly(
            "2:12:T_CC=Unavailable(NOT_A_COVERED_NAME)",
            "5:6:LABEL=$conditional", "6:8:G_CC=Unavailable(NOT_A_COVERED_NAME)", "6:13:LABEL=$conditional", "7:6:Z=$conditional"
        )
    }

    @Test
    fun aValueOfAnUndecidedTypeIsUndecided() {
        // `t_ext` is in no file of the project.
        val text = "create or replace procedure use_ext(p_rec t_ext) is\n  n number;\nbegin\n  n := p_rec.code;\nend;\n/\n"
        assertThat(fields("u.sql", "u.sql" to text)).containsExactly("4:13:CODE=Unknown[UNSUPPORTED_REFERENCE]")
    }
}
