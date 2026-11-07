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
                } + node.getDescendants(PlSqlGrammar.OBJECT_REFERENCE).flatMap { reference ->
                    // A member after an expression receiver, alone (`.f`) or with an argument list (`.f(1)`).
                    reference.getChildren(PlSqlGrammar.VARIABLE_NAME) + reference.children.drop(1)
                        .filter { it.type == PlSqlGrammar.METHOD_CALL && it.firstChild.type == PlSqlGrammar.VARIABLE_NAME }.map { it.firstChild }
                }
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
        // `t_ext` is in no file of the project: its members are declared outside it.
        val text = "create or replace procedure use_ext(p_rec t_ext) is\n  n number;\nbegin\n  n := p_rec.code;\nend;\n/\n"
        assertThat(fields("u.sql", "u.sql" to text)).containsExactly("4:13:CODE=Unknown[DECLARATION_OUTSIDE_FILE]")
    }

    @Test
    fun aMemberOfAProjectTypedValueUnderConditionalTextIsUndecided() {
        val spec = "create or replace package pk_c is\n  type t_row is record (code varchar2(10));\nend pk_c;\n/\n"
        val text = "create or replace procedure use_c(p_rec pk_c.t_row) is\n" +
            "  \$if \$\$flag \$then n number; \$else p_rec number; \$end\n" +
            "begin\n" +
            "  p_rec.code := 'x';\n" +
            "end;\n/\n"
        assertThat(fields("u.sql", "s.sql" to spec, "u.sql" to text).filter { ":CODE=" in it }).containsExactly("4:8:CODE=Unknown[CONDITIONAL_COMPILATION]")
        // The same with a `${'$'}IF` that never writes `p_rec`, or without it: the field the specification declares.
        val unrelated = text.replace("\$else p_rec number; ", "")
        assertThat(fields("u.sql", "s.sql" to spec, "u.sql" to unrelated).filter { ":CODE=" in it }).containsExactly("4:8:CODE=Field(RECORD_FIELD#0@s.sql:2:24)")
        val plain = text.replace("  \$if \$\$flag \$then n number; \$else p_rec number; \$end\n", "  n number;\n")
        assertThat(fields("u.sql", "s.sql" to spec, "u.sql" to plain).filter { ":CODE=" in it }).containsExactly("4:8:CODE=Field(RECORD_FIELD#0@s.sql:2:24)")
    }

    @Test
    fun aQuotedFieldNameIsItsOwnIdentifier() {
        val spec = "create or replace package pk_q is\n  type t_row is record (\"Name\" varchar2(10), name number);\nend pk_q;\n/\n"
        val text = "create or replace procedure use_q(p_rec pk_q.t_row) is\nbegin\n  p_rec.\"Name\" := 'x';\n  p_rec.name := 1;\nend;\n/\n"
        assertThat(fields("u.sql", "s.sql" to spec, "u.sql" to text).filter { it.startsWith("3:") || it.startsWith("4:") })
            .containsExactly("3:8:\"Name\"=Field(RECORD_FIELD#0@s.sql:2:24)", "4:8:NAME=Field(RECORD_FIELD#1@s.sql:2:45)")
    }

    private val linesSpec = "create or replace package pk_o is\n" +
        "  type t_inner is record (order_id number);\n" +
        "  type t_line is record (order_id number, qty number, inner t_inner);\n" +
        "  type t_lines is table of t_line index by pls_integer;\n" +
        "  procedure run(p_lines t_lines);\n" +
        "end pk_o;\n/\n"

    private val linesBody = "create or replace package body pk_o is\n" +
        "  procedure run(p_lines t_lines) is\n" +
        "    v_lines t_lines;\n" +
        "    n number;\n" +
        "  begin\n" +
        "    insert into orders (order_id, qty) values (v_lines(1).order_id, v_lines(1).qty);\n" +
        "    update orders set qty = p_lines(1).qty where order_id = p_lines(1).order_id;\n" +
        "    select count(*) into n from orders where order_id = p_lines(1).order_id;\n" +
        "    delete from orders where order_id = p_lines(1).order_id;\n" +
        "    merge into orders o using src s on (o.order_id = p_lines(1).order_id) when matched then update set o.qty = p_lines(1).qty;\n" +
        "    select count(*) into n from orders where order_id = v_lines(1).inner.order_id;\n" +
        "    n := v_lines(1).qty;\n" +
        "  end;\n" +
        "end pk_o;\n/\n"

    @Test
    fun aMemberAfterAnElementInSqlIsTheFieldWhereverTheRecordTypeIsDeclared() {
        val offset = linesSpec.count { it == '\n' }
        val oneFile = fields("pk_o.sql", "pk_o.sql" to linesSpec + linesBody)
            .map { it.substringBefore(':').toInt().minus(offset).toString() + ":" + it.substringAfter(':') }
        val ownFile = fields("b.sql", "s.sql" to linesSpec, "b.sql" to linesBody).map { it.replace("@s.sql:", "@") }
        val orderId = "Field(RECORD_FIELD#0@3:25)"
        val qty = "Field(RECORD_FIELD#1@3:42)"
        assertThat(oneFile).isEqualTo(ownFile)
        assertThat(oneFile.filter { ":ORDER_ID=" in it || ":QTY=" in it || ":INNER=" in it }).containsExactly(
            // The VALUES of a single-table INSERT.
            "6:58:ORDER_ID=$orderId", "6:79:QTY=$qty",
            // The SET and the WHERE of an UPDATE, the WHERE of a SELECT and of a DELETE. A column of the name would not
            // take `p_lines(1)`: Oracle binds the collection, a column cannot be indexed.
            "7:39:QTY=$qty", "7:71:ORDER_ID=$orderId",
            "8:67:ORDER_ID=$orderId",
            "9:51:ORDER_ID=$orderId",
            // A MERGE: its ON condition and the SET of its update; `o.order_id` and `o.qty` are columns.
            "10:42:ORDER_ID=Unavailable(NOT_A_COVERED_NAME)", "10:64:ORDER_ID=$orderId",
            "10:105:QTY=Unavailable(NOT_A_COVERED_NAME)", "10:122:QTY=$qty",
            // A field between the element and the member.
            "11:67:INNER=Field(RECORD_FIELD#2@3:54)", "11:73:ORDER_ID=Field(RECORD_FIELD#0@2:26)",
            "12:20:QTY=$qty"
        )
    }

    private val optsType = "create or replace type t_opts as object (include_expr varchar2(100));\n/\n"

    private val optsSpec = "create or replace package pk_c is\n  procedure a(a_opts t_opts);\n  procedure b;\nend;\n/\n"

    private fun optsBody(declarations: String = "", bodyLevel: String = "", use: String = "s := a_opts.include_expr;", inB: String = "null;") =
        "create or replace package body pk_c is\n" + bodyLevel +
            "  procedure a(a_opts t_opts) is\n    s varchar2(100);\n" + declarations + "  begin\n    " + use + "\n  end;\n" +
            "  procedure b is\n  begin\n    " + inB + "\n  end;\nend;\n/\n"

    private fun includeExpr(body: String) =
        fields("b.sql", "t.sql" to optsType, "s.sql" to optsSpec, "b.sql" to body).single { ":INCLUDE_EXPR=" in it }.substringAfter('=')

    @Test
    fun conditionalTextLeavesAMemberUndecidedOnlyWhereItCanChangeItsLookups() {
        val field = "Field(ATTRIBUTE#0@t.sql:1:41)"
        val selection = "\$if dbms_db_version.version >= 12 \$then null; \$else null; \$end"
        assertThat(includeExpr(optsBody())).isEqualTo(field)
        // Among the statements of another subprogram: it declares nothing `a` sees.
        assertThat(includeExpr(optsBody(inB = selection))).isEqualTo(field)
        // In the declarations of the same subprogram, or of the package body, text that writes `a_opts` or `t_opts`
        // may declare it; text that writes neither declares nothing the lookups find.
        assertThat(includeExpr(optsBody(declarations = "    \$if \$\$flag \$then \$else a_opts number; \$end\n"))).contains("CONDITIONAL_COMPILATION")
        assertThat(includeExpr(optsBody(bodyLevel = "  \$if \$\$flag \$then \$else subtype t_opts is number; \$end\n"))).contains("CONDITIONAL_COMPILATION")
        assertThat(includeExpr(optsBody(declarations = "    \$if \$\$flag \$then \$end\n"))).isEqualTo(field)
        assertThat(includeExpr(optsBody(bodyLevel = "  \$if \$\$flag \$then \$end\n"))).isEqualTo(field)
        // The use itself in an undecided selection.
        assertThat(includeExpr(optsBody(use = "\$if \$\$flag \$then s := a_opts.include_expr; \$end"))).contains("CONDITIONAL_COMPILATION")
        // A dropped branch that ends `b` and declares in the package body (Oracle compiles it): it may hide `t_opts`.
        val closing = "\$if \$\$flag \$then null; \$else null; end; type t_opts is record (x number); procedure c is begin null; \$end"
        assertThat(includeExpr(optsBody(inB = closing))).contains("CONDITIONAL_COMPILATION")
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

    /** `line:column:NAME=answer` for each name of [file] that starts an expression (no dot before it), ordered by position. */
    private fun firstNames(file: String, vararg sources: Pair<String, String>): List<String> {
        val configuration = PlSqlConfiguration(StandardCharsets.UTF_8, false, ConditionalCompilationSettings.NONE)
        val project = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation(ProjectDeclarationExtractor(configuration)).prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
        )
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.VARIABLE_NAME)
                    .filter { it.previousSiblingOrNull?.tokenValue != "." }
                    .sortedWith(compareBy({ it.token.line }, { it.token.column }))
                    .forEach { rows += "${it.token.line}:${it.token.column}:${it.tokenValue}=${this.context.fieldBinding(it)}" }
            }
        }
        val scanner = AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, project, ConditionalCompilationSettings.NONE)
        scanner.scanFile(Source(file, sources.toMap().getValue(file)), listOf(check), FileId(file))
        return rows
    }

    private val crateBase = "create or replace type ty_base as object (base_n number, member procedure show) not final;\n/\n"
    private val crateList = "create or replace type ty_list as table of number;\n/\n" +
        "create or replace type ty_peg as object (peg_tag varchar2(10));\n/\n" +
        "create or replace type ty_peg_list as table of ty_peg;\n/\n"
    private val crateSpec = "create or replace type ty_crate under ty_base (\n" +
        "  items ty_list, tag varchar2(10), inner ty_base, pegs ty_peg_list,\n" +
        "  constructor function ty_crate return self as result,\n" +
        "  member procedure fill, member procedure clash(items ty_list), member procedure shadow,\n" +
        "  static procedure make, member function describe return varchar2, member procedure query\n" +
        ");\n/\n"
    private val crateBody = "create or replace type body ty_crate is\n" +
        "  constructor function ty_crate return self as result is\n" +
        "  begin\n" +
        "    items := ty_list();\n" +                                                            // 4
        "    return;\n" +
        "  end;\n" +
        "  member procedure fill is\n" +
        "  begin\n" +
        "    items.extend;\n" +                                                                  // 9
        "    items(1) := base_n;\n" +                                                            // 10
        "    show;\n" +                                                                          // 11
        "    tag := describe();\n" +                                                             // 12
        "  end;\n" +
        "  member procedure clash(items ty_list) is\n" +
        "  begin\n" +
        "    items.extend;\n" +                                                                  // 16
        "  end;\n" +
        "  member procedure shadow is\n" +
        "    tag varchar2(10);\n" +
        "  begin\n" +
        "    tag := 'x';\n" +                                                                    // 21
        "  end;\n" +
        "  static procedure make is\n" +
        "  begin\n" +
        "    tag := 'x';\n" +                                                                    // 25
        "  end;\n" +
        "  member function describe return varchar2 is begin return tag; end;\n" +              // 27
        "  member procedure query is\n" +
        "    n number;\n" +
        "  begin\n" +
        "    select count(*) into n from all_objects where object_name = tag;\n" +               // 31
        "    select count(*) into n from dual where dummy = tag;\n" +                            // 32
        "    n := inner.base_n; n := self.inner.base_n;\n" +                                     // 33
        "    pegs(1).peg_tag := 'x'; self.pegs(1).peg_tag := 'x';\n" +                           // 34
        "  end;\n" +
        "end;\n/\n"

    /** The same type body with a `$IF` in one method. */
    private val crateBodyIf = crateBody.replace(
        "    select count(*) into n from dual where dummy = tag;\n",
        "    select count(*) into n from dual where dummy = tag;\n    \$if dbms_db_version.ver_le_10 \$then\n    n := base_n;\n    n := self.base_n;\n    \$end\n"
    )

    @Test
    fun anAttributeNamedWithoutSelfIsAnsweredAsSelfDotNameWouldBe() {
        val rows = firstNames("crate_body.sql", "base.sql" to crateBase, "list.sql" to crateList, "crate.sql" to crateSpec, "crate_body.sql" to crateBody)
        val items = "Field(ATTRIBUTE#0@crate.sql:2:2)"
        val tag = "Field(ATTRIBUTE#1@crate.sql:2:17)"
        val baseN = "Field(ATTRIBUTE#0@base.sql:1:42)"
        val none = "Unavailable(NOT_A_COVERED_NAME)"
        fun at(line: Int) = rows.filter { it.startsWith("$line:") }
        // In a constructor and in a member method, the attribute, an inherited one with the supertype's declaration.
        assertThat(at(4)).contains("4:4:ITEMS=$items")
        assertThat(at(9)).containsExactly("9:4:ITEMS=$items")
        assertThat(at(10)).containsExactly("10:4:ITEMS=$items", "10:16:BASE_N=$baseN")
        // A method named alone is not an attribute.
        assertThat(at(11)).containsExactly("11:4:SHOW=$none")
        assertThat(at(12)).containsExactly("12:4:TAG=$tag", "12:11:DESCRIBE=$none")
        // A parameter or a local of the attribute's name hides it.
        assertThat(at(16)).containsExactly("16:4:ITEMS=$none")
        assertThat(at(21)).containsExactly("21:4:TAG=$none")
        // A static method has no instance.
        assertThat(at(25)).containsExactly("25:4:TAG=$none")
        assertThat(at(27)).contains("27:59:TAG=$tag")
        // In SQL a column may take the name first, unless the only relation is DUAL.
        assertThat(at(31).single { it.contains(":TAG=") }).isEqualTo("31:64:TAG=Unknown[SQL_NAME_MAY_BE_COLUMN, DECLARATION_OUTSIDE_FILE]")
        assertThat(at(32).single { it.contains(":TAG=") }).isEqualTo("32:51:TAG=$tag")
        // A chain started by the attribute, a field of it, an element's member: as after SELF.
        assertThat(at(33).single { it.contains(":INNER=") }).isEqualTo("33:9:INNER=Field(ATTRIBUTE#2@crate.sql:2:35)")
        assertThat(at(34).single { it.contains(":PEGS=") }).isEqualTo("34:4:PEGS=Field(ATTRIBUTE#3@crate.sql:2:50)")
        val after = fields("crate_body.sql", "base.sql" to crateBase, "list.sql" to crateList, "crate.sql" to crateSpec, "crate_body.sql" to crateBody)
        fun member(line: Int, name: String) = after.filter { it.startsWith("$line:") && it.contains(":$name=") }.map { it.substringAfter('=') }
        assertThat(member(33, "BASE_N")).containsExactly(baseN, baseN)
        assertThat(member(34, "PEG_TAG")).containsExactly("Field(ATTRIBUTE#0@list.sql:3:41)", "Field(ATTRIBUTE#0@list.sql:3:41)")
    }

    @Test
    fun withAnUndecidedIfAnAttributeNamedAloneIsNoMoreCertainThanSelfDotName() {
        val sources = arrayOf("base.sql" to crateBase, "list.sql" to crateList, "crate.sql" to crateSpec, "crate_body.sql" to crateBodyIf)
        val bare = firstNames("crate_body.sql", *sources).single { it.startsWith("34:") && it.contains(":BASE_N=") }.substringAfter('=')
        val qualified = fields("crate_body.sql", *sources).single { it.startsWith("35:") && it.contains(":BASE_N=") }.substringAfter('=')
        assertThat(bare).isEqualTo(qualified).startsWith("Unknown")
        // Elsewhere in the body the ${'$'}IF, among another method's statements, changes nothing.
        assertThat(firstNames("crate_body.sql", *sources).single { it.startsWith("9:") }).isEqualTo("9:4:ITEMS=Field(ATTRIBUTE#0@crate.sql:2:2)")
    }

    @Test
    fun anAttributeNamedWithoutSelfWithTheSpecificationInTheSameFile() {
        val rows = firstNames("crate.sql", "base.sql" to crateBase, "list.sql" to crateList, "crate.sql" to crateSpec + crateBody)
        val offset = crateSpec.count { it == '\n' }
        fun at(line: Int) = rows.filter { it.startsWith("${line + offset}:") }
        assertThat(at(9)).containsExactly("${9 + offset}:4:ITEMS=Field(ATTRIBUTE#0@2:2)")
        assertThat(at(10)).containsExactly("${10 + offset}:4:ITEMS=Field(ATTRIBUTE#0@2:2)", "${10 + offset}:16:BASE_N=Field(ATTRIBUTE#0@base.sql:1:42)")
        assertThat(at(16)).containsExactly("${16 + offset}:4:ITEMS=Unavailable(NOT_A_COVERED_NAME)")
        assertThat(at(25)).containsExactly("${25 + offset}:4:TAG=Unavailable(NOT_A_COVERED_NAME)")
    }

    private val boxTypes = "create or replace type ty_list as table of number;\n/\n" +
        "create or replace type ty_bx as object (\n" +
        "  items ty_list, tag varchar2(10), n number,\n" +
        "  member procedure m1\n" +
        ");\n/\n" +
        "alter type ty_bx add attribute (extra number) cascade;\n/\n"
    private val boxBody = "create or replace type body ty_bx is\n" +
        "  member procedure m1 is\n" +
        "  begin\n" +
        "    items.extend;\n" +
        "    self.items.extend;\n" +
        "    extra := 1;\n" +
        "    self.extra := 1;\n" +
        "  end;\n" +
        "end;\n/\n"

    /** The answer for the bare name of [bareLine] and for the same name after `SELF.` on [selfLine]. */
    private fun bareAndSelf(name: String, bareLine: Int, selfLine: Int, vararg sources: Pair<String, String>): Pair<String, String> {
        val bare = firstNames("body.tpb", *sources).single { it.startsWith("$bareLine:") && it.contains(":$name=") }.substringAfter('=')
        val qualified = fields("body.tpb", *sources).single { it.startsWith("$selfLine:") && it.contains(":$name=") }.substringAfter('=')
        return bare to qualified
    }

    @Test
    fun afterAnAlterTypeAnAttributeNamedAloneAnswersAsSelfDotName() {
        val sources = arrayOf("types.sql" to boxTypes, "body.tpb" to boxBody)
        // Listed by the CREATE TYPE: the attribute, as SELF.ITEMS.
        assertThat(bareAndSelf("ITEMS", 4, 5, *sources)).isEqualTo("Field(ATTRIBUTE#0@types.sql:4:2)" to "Field(ATTRIBUTE#0@types.sql:4:2)")
        // Added by the ALTER TYPE: undecided, as SELF.EXTRA, not "no attribute".
        assertThat(bareAndSelf("EXTRA", 6, 7, *sources)).isEqualTo("Unknown[DECLARATION_OUTSIDE_FILE]" to "Unknown[DECLARATION_OUTSIDE_FILE]")
    }

    @Test
    fun withoutTheTypeSpecificationAnAttributeNamedAloneIsUndecidedAsSelfDotName() {
        assertThat(bareAndSelf("ITEMS", 4, 5, "body.tpb" to boxBody)).isEqualTo("Unknown[DECLARATION_OUTSIDE_FILE]" to "Unknown[DECLARATION_OUTSIDE_FILE]")
        assertThat(bareAndSelf("EXTRA", 6, 7, "body.tpb" to boxBody)).isEqualTo("Unknown[DECLARATION_OUTSIDE_FILE]" to "Unknown[DECLARATION_OUTSIDE_FILE]")
    }

    /** The spec is needed: without it, a name of the body may be the spec's (Unknown). */
    private val shelfSpec = "create or replace package pk_shelf is\n  procedure run;\nend;\n/\n"
    private val shelfBody = "create or replace package body pk_shelf is\n" +
        "  type t_tag is record (code varchar2(10));\n" +
        "  type t_item is record (n number, tag t_tag);\n" +
        "  type t_items is table of t_item index by pls_integer;\n" +
        "  g_items t_items;\n" +
        "  procedure run is\n" +
        "    l_items t_items;\n" +
        "  begin\n" +
        "    l_items(1).n := 1;\n" +                                  // 9
        "    l_items(1).tag.code := g_items(2).tag.code;\n" +       // 10
        "    l_items(1).missing := 1;\n" +                            // 11
        "  end;\n" +
        "end;\n/\n"

    @Test
    fun aMemberAfterAnElementOfALocalCollectionOfLocalRecordsIsItsField() {
        val rows = fields("shelf.pkb", "shelf.pks" to shelfSpec, "shelf.pkb" to shelfBody)
        val n = "Field(RECORD_FIELD#0@3:25)"
        val tag = "Field(RECORD_FIELD#1@3:35)"
        val code = "Field(RECORD_FIELD#0@2:24)"
        assertThat(rows.filter { it.startsWith("9:") }).containsExactly("9:15:N=$n")
        assertThat(rows.filter { it.startsWith("10:") }).containsExactly("10:15:TAG=$tag", "10:19:CODE=$code", "10:38:TAG=$tag", "10:42:CODE=$code")
        // A name the record does not have is left undecided, as before.
        assertThat(rows.filter { it.startsWith("11:") }).containsExactly("11:15:MISSING=Unknown[UNSUPPORTED_REFERENCE]")
    }

    @Test
    fun theFieldsOfARecordTypeAndTheTypeADatatypeNamesAreTheFilesDeclarations() {
        val configuration = PlSqlConfiguration(StandardCharsets.UTF_8, false, ConditionalCompilationSettings.NONE)
        val project = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation(ProjectDeclarationExtractor(configuration))
                .prepare(listOf(ProjectSource(FileId("shelf.pks")) { shelfSpec }, ProjectSource(FileId("shelf.pkb")) { shelfBody }), concurrent = false)
        )
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.RECORD_DECLARATION).forEach {
                    val name = it.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
                    rows += "${name.tokenValue}=${this.context.recordFields(name)}"
                }
                node.getDescendants(PlSqlGrammar.CUSTOM_DATATYPE).forEach {
                    rows += "${it.token.line}:${it.token.column}=${this.context.typeBinding(it.firstChild)}"
                }
            }
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, project, ConditionalCompilationSettings.NONE)
            .scanFile(Source("shelf.pkb", shelfBody), listOf(check), FileId("shelf.pkb"))
        assertThat(rows).containsExactly(
            "T_TAG=[code=RECORD_FIELD#0@2:24]",
            "T_ITEM=[n=RECORD_FIELD#0@3:25, tag=RECORD_FIELD#1@3:35]",
            "3:39=Local(TYPE@2:7)", "4:27=Local(TYPE@3:7)", "5:10=Local(TYPE@4:7)", "7:12=Local(TYPE@4:7)"
        )
    }

    /**
     * `line:column:NAME=answer` for every name of [file] ([PlSqlGrammar.VARIABLE_NAME], or an [PlSqlGrammar.IDENTIFIER_NAME]
     * that is not one), with `F` its field binding, and `T=` instead for the name of a `CUSTOM_DATATYPE`, its type binding.
     */
    private fun answers(file: String, vararg sources: Pair<String, String>): List<String> {
        val configuration = PlSqlConfiguration(StandardCharsets.UTF_8, false, ConditionalCompilationSettings.NONE)
        val project = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation(ProjectDeclarationExtractor(configuration)).prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
        )
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.CUSTOM_DATATYPE).forEach {
                    rows += "${it.token.line}:${it.token.column}:${it.tokenValue}:T=${this.context.typeBinding(it.firstChild)}"
                }
                node.getDescendants(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.IDENTIFIER_NAME)
                    .filter { it.type == PlSqlGrammar.VARIABLE_NAME || it.parent.type != PlSqlGrammar.VARIABLE_NAME }
                    .forEach { rows += "${it.token.line}:${it.token.column}:${it.tokenValue}=${this.context.fieldBinding(it)}" }
            }
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, project, ConditionalCompilationSettings.NONE)
            .scanFile(Source(file, sources.toMap().getValue(file)), listOf(check), FileId(file))
        return rows
    }

    private val vSpec = "create or replace package pk_v is\n  procedure run;\nend;\n/\n"
    private val vBody = "create or replace package body pk_v is\n" +
        "  type t_r is record (x number, z number);\n" +
        "  type t_rs is table of t_r index by pls_integer;\n" +
        "  type t_rr is record (items t_rs, one t_r);\n" +
        "  type t_rrs is table of t_rr index by pls_integer;\n" +
        "  g_rr t_rr;\n" +
        "  g_rrs t_rrs;\n" +
        "  procedure run is\n" +
        "    l_rr t_rr;\n" +
        "    n number;\n" +
        "  begin\n" +
        "    l_rr.items(1).x := 2;\n" +            // 12
        "    n := l_rr.items(1).x;\n" +            // 13
        "    g_rr.items(2).x := 1;\n" +            // 14
        "    g_rrs(1).items(2).x := 3;\n" +        // 15
        "    n := g_rrs(1).items.count;\n" +       // 16
        "    g_rrs(1).items(2).one.x := 4;\n" +    // 17
        "  end;\n" +
        "  procedure run2(p_rr in out t_rr) is\n" +
        "  begin\n" +
        "    p_rr.items(1).z := 1;\n" +            // 21
        "  end;\n" +
        "  procedure run3 is\n" +
        "  begin\n" +
        "    g_rrs(1).one.x := 5;\n" +             // 25
        "  end;\n" +
        "end;\n/\n"

    @Test
    fun aFieldAfterAnElementAnswersFromARecordLocalOrParameterAndAcrossASecondElement() {
        val rows = answers("pk_v.pkb", "pk_v.pks" to vSpec, "pk_v.pkb" to vBody)
        fun at(line: Int, name: String) = rows.single { it.startsWith("$line:") && it.substringBefore('=').endsWith(":$name") }.substringAfter('=')
        val x = "Field(RECORD_FIELD#0@2:22)"
        val items = "Field(RECORD_FIELD#0@4:23)"
        // A record local or parameter, then an element: as a body global (line 14).
        for (line in listOf(12, 13, 14)) assertThat(at(line, "X")).describedAs("line $line").isEqualTo(x)
        assertThat(at(21, "Z")).isEqualTo("Field(RECORD_FIELD#1@2:32)")
        // A field with its own element, after an element.
        assertThat(at(15, "ITEMS")).isEqualTo(items)
        assertThat(at(15, "X")).isEqualTo(x)
        assertThat(at(25, "ONE")).isEqualTo("Field(RECORD_FIELD#1@4:35)")
        assertThat(at(25, "X")).isEqualTo(x)
        // An element of ITEMS is a T_R, which has no ONE (PLS-00302): undecided, as any name a record lacks.
        assertThat(at(17, "ITEMS")).isEqualTo(items)
        assertThat(at(17, "ONE")).isEqualTo("Unknown[UNSUPPORTED_REFERENCE]")
        assertThat(at(17, "X")).isEqualTo("Unknown[UNSUPPORTED_REFERENCE]")
        // A method of the local collection type ITEMS has: not a field.
        assertThat(at(16, "ITEMS")).isEqualTo(items)
        assertThat(at(16, "COUNT")).isEqualTo("Unavailable(NOT_A_COVERED_NAME)")
    }

    private val wSpec = "create or replace package pk_w is\n  type t_s is record (y number);\n  procedure run;\nend;\n/\n"
    private val wBody = "create or replace package body pk_w is\n" +
        "  type t_r is record (x number);\n" +
        "  type t_rs is table of pk_w.t_r index by pls_integer;\n" +   // 3
        "  g_r pk_w.t_r;\n" +                                          // 4
        "  procedure run is\n" +
        "    l_r pk_w.t_r;\n" +                                        // 6
        "    l_o pk_other.t_r;\n" +                                    // 7
        "  begin\n" +
        "    l_r.x := 1;\n" +                                          // 9
        "    g_r.x := 2;\n" +                                          // 10
        "    l_o.x := 3;\n" +                                          // 11
        "  end;\n" +
        "end;\n/\n"

    @Test
    fun aDatatypeQualifiedByTheFilesOwnPackageNamesTheBodysType() {
        val rows = answers("pk_w.pkb", "pk_w.pks" to wSpec, "pk_w.pkb" to wBody)
        val local = "T=Local(TYPE@2:7)"
        assertThat(rows.filter { ":T=" in it }).containsExactly(
            "3:24:PK_W:$local", "4:6:PK_W:$local", "6:8:PK_W:$local",
            // Another package: no declaration of the file takes it.
            "7:8:PK_OTHER:T=NothingLocal"
        )
        assertThat(rows).contains("9:8:X=Field(RECORD_FIELD#0@2:22)", "10:8:X=Field(RECORD_FIELD#0@2:22)", "11:8:X=Unknown[DECLARATION_OUTSIDE_FILE]")
    }
    @Test
    fun aFieldAfterAnElementInStaticSql() {
        val spec = "create or replace package pk_lines is\n" +
            "  type t_nums is table of number;\n" +
            "  type t_row is record (items t_nums);\n" +
            "  type t_rows is table of t_row index by pls_integer;\n" +
            "  procedure run(p_rows t_rows);\n" +
            "end pk_lines;\n/\n"
        val body = "create or replace package body pk_lines is\n" +
            "  procedure run(p_rows t_rows) is\n" +
            "    l number;\n" +
            "  begin\n" +
            "    insert into line_tab (x) values (p_rows(1).items(1));\n" +
            "    update line_tab set x = 1 where x = p_rows(1).items(1);\n" +
            "    select count(*) into l from line_tab p_rows where x = p_rows(1).items(1);\n" +
            "    l := p_rows(1).items(1);\n" +
            "  end;\n" +
            "end pk_lines;\n/\n"
        val items = "Field(RECORD_FIELD#0@spec.sql:3:24)"
        assertThat(fields("body.sql", "spec.sql" to spec, "body.sql" to body).filter { ":ITEMS=" in it }).containsExactly(
            "5:47:ITEMS=$items",
            "6:50:ITEMS=$items",
            // A relation named like the collection does not take `p_rows(1)`.
            "7:68:ITEMS=$items",
            "8:19:ITEMS=$items"
        )
    }
}

