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
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.squid.ConditionalCompilationSettings
import com.felipebz.zpa.squid.PlSqlConfiguration
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
    private fun fields(file: String, vararg sources: Pair<String, String>): List<String> =
        fields(file, ConditionalCompilationSettings.NONE, *sources)

    private fun fields(file: String, settings: ConditionalCompilationSettings, vararg sources: Pair<String, String>): List<String> {
        val configuration = PlSqlConfiguration(StandardCharsets.UTF_8, false, settings)
        val project = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation(ProjectDeclarationExtractor(configuration)).prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
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
        val scanner = AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, project, settings)
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

    private val splitSpec = "create or replace package pk_z is\n" +
        "  type t_nums is table of number;\n" +
        "  type t_row is record (items t_nums, code varchar2(10), n number);\n" +
        "  type t_outer is record (inner t_row);\n" +
        "  s_rec t_row;\n" +
        "  s_out t_outer;\n" +
        "end pk_z;\n/\n"

    private val splitBody = "create or replace package body pk_z is\n" +
        "  type t_img is record (w number);\n" +
        "  procedure run(p_img in t_img) is\n" +
        "    l_out t_outer;\n" +
        "    l_row t_row;\n" +
        "    l_i t_img;\n" +
        "  begin\n" +
        "    s_rec.code := 'a';\n" +
        "    s_rec.items.extend;\n" +
        "    s_rec.items(1) := 5;\n" +
        "    pk_z.s_rec.n := 1;\n" +
        "    l_out.inner.items(1) := 3;\n" +
        "    l_out.inner.code := 'b';\n" +
        "    l_row.items(1) := 3;\n" +
        "    l_i.w := p_img.w;\n" +
        "  end;\n" +
        "end pk_z;\n/\n"

    private val splitUser = "create or replace procedure use_z is\nbegin\n" +
        "  pk_z.s_out.inner.code := 'x';\n" +
        "  pk_z.s_out.inner.items(1) := 1;\n" +
        "end;\n/\n"

    @Test
    fun aBodyWhoseSpecificationIsInAnotherFile() {
        val items = "Field(RECORD_FIELD#0@pk_z.pks:3:24)"
        val code = "Field(RECORD_FIELD#1@pk_z.pks:3:38)"
        val inner = "Field(RECORD_FIELD#0@pk_z.pks:4:26)"
        val notAField = "Unavailable(NOT_A_COVERED_NAME)"
        val sources = arrayOf("pk_z.pks" to splitSpec, "pk_z.pkb" to splitBody, "use_z.sql" to splitUser)
        assertThat(fields("pk_z.pkb", *sources)).containsExactly(
            // A variable of the specification, alone and through the package's own name.
            "8:10:CODE=$code",
            "9:10:ITEMS=$items", "9:16:EXTEND=$notAField",
            "10:10:ITEMS=$items",
            "11:9:S_REC=$notAField", "11:15:N=Field(RECORD_FIELD#2@pk_z.pks:3:57)",
            // A field of a field, followed by an element.
            "12:10:INNER=$inner", "12:16:ITEMS=$items",
            "13:10:INNER=$inner", "13:16:CODE=$code",
            "14:10:ITEMS=$items",
            // A record type of the body that the specification does not declare.
            "15:8:W=Field(RECORD_FIELD#0@2:24)", "15:19:W=Field(RECORD_FIELD#0@2:24)"
        )
        assertThat(fields("use_z.sql", *sources)).containsExactly(
            "3:7:S_OUT=$notAField", "3:13:INNER=$inner", "3:19:CODE=$code",
            "4:7:S_OUT=$notAField", "4:13:INNER=$inner", "4:19:ITEMS=$items"
        )
    }

    @Test
    fun theSameBodyWithItsSpecificationInTheSameFile() {
        val file = splitSpec + splitBody
        // The body starts after the specification's 8 lines; positions in this file have no file.
        assertThat(fields("pk_z.sql", "pk_z.sql" to file).filter { it.startsWith("16:") || it.startsWith("20:") || it.startsWith("23:") }).containsExactly(
            "16:10:CODE=Field(RECORD_FIELD#1@3:38)",
            "20:10:INNER=Field(RECORD_FIELD#0@4:26)", "20:16:ITEMS=Field(RECORD_FIELD#0@3:24)",
            "23:8:W=Field(RECORD_FIELD#0@10:24)", "23:19:W=Field(RECORD_FIELD#0@10:24)"
        )
    }

    @Test
    fun aBodyTypeTheSpecificationAlsoDeclaresStaysUndecided() {
        val spec = splitSpec.replace("  s_rec t_row;\n", "  type t_img is record (w number);\n  s_rec t_row;\n")
        assertThat(fields("pk_z.pkb", "pk_z.pks" to spec, "pk_z.pkb" to splitBody).filter { it.startsWith("15:") }).containsExactly(
            "15:8:W=Unknown[DECLARATION_OUTSIDE_FILE]", "15:19:W=Unknown[DECLARATION_OUTSIDE_FILE]"
        )
    }

    @Test
    fun aFieldIsNeverMoreCertainThanTheNameItsChainStartsFrom() {
        val spec = "create or replace package pk_c is\n" +
            "  type t_tab is table of number index by pls_integer;\n" +
            "  type t_cov is record (objects t_tab, n number);\n" +
            "  type t_alt is record (objects t_tab);\n" +
            "end pk_c;\n/\n"
        val body = "create or replace package body pk_c is\n" +
            "  function f2 return boolean is\n" +
            "    \$if dbms_db_version.version > 12 \$then\n" +
            "      l_r pk_c.t_cov;\n" +
            "    \$else\n" +
            "      l_r pk_c.t_alt;\n" +
            "    \$end\n" +
            "  begin\n" +
            "    if l_r.objects.exists(1) then null; end if;\n" +
            "    l_r.objects(1) := 1;\n" +
            "    return true;\n" +
            "  end;\n" +
            "end pk_c;\n/\n"
        fun objects(version: String?) = fields(
            "pk_c.pkb", ConditionalCompilationSettings.of(null, version), "pk_c.pks" to spec, "pk_c.pkb" to body
        ).filter { it.contains(":OBJECTS=") }

        // Without settings Oracle may compile either branch: the type of `l_r`, and so the field, is undecided.
        val undecided = "Unknown[CONDITIONAL_COMPILATION]"
        assertThat(objects(null)).containsExactly("9:11:OBJECTS=$undecided", "10:8:OBJECTS=$undecided")
        val cov = "Field(RECORD_FIELD#0@pk_c.pks:3:24)"
        assertThat(objects("19")).containsExactly("9:11:OBJECTS=$cov", "10:8:OBJECTS=$cov")
        val alt = "Field(RECORD_FIELD#0@pk_c.pks:4:24)"
        assertThat(objects("11")).containsExactly("9:11:OBJECTS=$alt", "10:8:OBJECTS=$alt")
    }
}
