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
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** Cursors named by `OPEN`, `FETCH`, `CLOSE`, cursor `FOR` loops and `%ROWTYPE`, their actuals and the fields of their rows. */
class CursorRowTest {

    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private val spec = """
        create package pk_ord as
          cursor c_lines(p_order number, p_min number default 0) is
            select l.line_no, l.qty as quantity, l.qty * l.price total, l.price + 1
              from order_line l
             where l.order_id = p_order and l.qty >= p_min;
          cursor c_full is select l.line_no, l.qty quantity from order_line l;
          cursor c_star is select l.*, l.qty extra from order_line l;
          cursor c_dup is select l.qty, l.qty from order_line l;
          cursor c_quoted is select l.qty "Mixed Case" from order_line l;
          cursor c_head(p_order number) return order_head%rowtype;
          cursor c_both return order_head%rowtype is select order_id a, head_no b, customer c from order_head;
          g_rc sys_refcursor;
        end;
        /
        """.trimIndent() + "\n"

    /** Each line of [text] the [query] answers for its nodes, analyzed with [others] as the rest of the project. */
    private fun analyze(text: String, vararg others: Pair<String, String>, query: PlSqlVisitorContext.(AstNode) -> List<String>): List<String> {
        val project = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation().prepare((others.toList() + ("u.sql" to text)).map { (id, t) -> ProjectSource(FileId(id)) { t } }, concurrent = false)
        )
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(text)
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                rows += context.query(node)
            }
        }
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("u.sql")).all + check)
            .walk(PlSqlVisitorContext(root, Source("u.sql", text), null), SemanticScanCapabilities(createProjectAnalysis(project)))
        return rows
    }

    /** `line:name=binding` for each cursor position of [text]. */
    private fun cursors(text: String, vararg others: Pair<String, String>) = analyze(text, *others) { root ->
        val names = root.getDescendants(PlSqlGrammar.OPEN_STATEMENT, PlSqlGrammar.FETCH_STATEMENT, PlSqlGrammar.CLOSE_STATEMENT)
            .map { it.getFirstChild(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION) } +
            root.getDescendants(PlSqlGrammar.SINGLE_EXPRESSION_CONTROL).map { it.lastChild.let { c -> if (c.type === PlSqlGrammar.METHOD_CALL) c.firstChild else c } } +
            root.getDescendants(PlSqlGrammar.ANCHORED_DATATYPE).filter { it.lastChild.type === PlSqlKeyword.ROWTYPE }.map { it.firstChild }
        names.sortedWith(compareBy({ it.token.line }, { it.token.column })).map { "${it.token.line}:${text(it)}=${cursorBinding(it)}" }
    }

    /** `line:name=category` for each value of an `INSERT … VALUES name` of [text]. */
    private fun categories(text: String, vararg others: Pair<String, String>) = analyze(text, *others) { root ->
        root.getDescendants(DmlGrammar.VALUES_CLAUSE).map { it.lastChild }.map { "${it.token.line}:${text(it)}=${valueCategory(it)}" }
    }

    /** `line:value.field=binding` for each field written after a plain name in [text]. */
    private fun fields(text: String, vararg others: Pair<String, String>) = analyze(text, *others) { root ->
        root.getDescendants(PlSqlGrammar.MEMBER_EXPRESSION).filter { it.firstChild.type === PlSqlGrammar.VARIABLE_NAME && it.children.size == 3 }
            .filter { it.firstChild.tokenValue.lowercase() in setOf("r", "v", "v_line") }
            .map { "${it.token.line}:${text(it)}=${fieldBinding(it.lastChild)}" }
    }

    private fun text(node: AstNode) = node.tokens.joinToString("") { it.originalValue }

    @Test
    fun theCursorOfOpenFetchCloseALoopAndARowtype() {
        val report = """
            create procedure p_report(p_id number) is
              v_line pk_ord.c_lines%rowtype;
            begin
              open pk_ord.c_lines(p_id);
              fetch pk_ord.c_lines into v_line;
              close pk_ord.c_lines;
              for r in pk_ord.c_lines(p_min => 5, p_order => p_id) loop
                null;
              end loop;
              for r in pk_ord.c_full loop
                null;
              end loop;
            end;
            /
            """.trimIndent() + "\n"
        assertThat(cursors(report, "pk_ord.pks" to spec)).containsExactly(
            "2:pk_ord.c_lines=Project(pk_ord.c_lines)",
            "4:pk_ord.c_lines=Project(pk_ord.c_lines, BOUND[#1->p_order#1] omitted [p_min#2])",
            "5:pk_ord.c_lines=Project(pk_ord.c_lines)",
            "6:pk_ord.c_lines=Project(pk_ord.c_lines)",
            "7:pk_ord.c_lines=Project(pk_ord.c_lines, BOUND[#1->p_min#2, #2->p_order#1])",
            "10:pk_ord.c_full=Project(pk_ord.c_full, BOUND[])"
        )
    }

    @Test
    fun actualsOracleRejects() {
        // Oracle rejects each OPEN with PLS-00306.
        val text = """
            create procedure p_bind is
            begin
              open pk_ord.c_lines(1, 2, 3);
              open pk_ord.c_lines(p_nope => 1);
              open pk_ord.c_lines;
              open pk_ord.c_head;
              open pk_ord.c_lines(p_order => 1, 2);
            end;
            /
            """.trimIndent() + "\n"
        assertThat(cursors(text, "pk_ord.pks" to spec)).containsExactly(
            "3:pk_ord.c_lines=Project(pk_ord.c_lines, TOO_MANY_ARGUMENTS #3)",
            "4:pk_ord.c_lines=Project(pk_ord.c_lines, UNKNOWN_LABEL #1)",
            "5:pk_ord.c_lines=Project(pk_ord.c_lines, MISSING_REQUIRED [p_order#1])",
            "6:pk_ord.c_head=Project(pk_ord.c_head, MISSING_REQUIRED [p_order#1])",
            "7:pk_ord.c_lines=Project(pk_ord.c_lines, INVALID_NOTATION #2)"
        )
    }

    @Test
    fun aCursorOfTheSpecificationNamedAloneInTheBody() {
        val body = """
            create package body pk_ord as
              procedure p_inner is
              begin
                for r in c_lines(10) loop
                  null;
                end loop;
                fetch c_full into g_rc;
              end;
            end;
            /
            """.trimIndent() + "\n"
        assertThat(cursors(body, "pk_ord.pks" to spec)).containsExactly(
            "4:c_lines=Project(pk_ord.c_lines, BOUND[#1->p_order#1] omitted [p_min#2])",
            "7:c_full=Project(pk_ord.c_full)"
        )
    }

    @Test
    fun aNameThatIsNoCursor() {
        val text = """
            create procedure p_other is
              v number;
            begin
              fetch pk_ord.g_rc into v;
              open pk_none.c_x;
            end;
            /
            """.trimIndent() + "\n"
        assertThat(cursors(text, "pk_ord.pks" to spec)).containsExactly(
            "4:pk_ord.g_rc=Unavailable(NOT_A_COVERED_NAME)",
            "5:pk_none.c_x=Unknown[DECLARATION_OUTSIDE_FILE]"
        )
    }

    @Test
    fun aCursorOfTheFileBindsItsActualsToo() {
        // Oracle rejects `open c(1, 2)` (PLS-00306).
        val text = """
            create procedure p_local is
              cursor c(p number, q number default 1) is select l.qty q, l.qty * 2 from order_line l where l.order_id = p;
              v c%rowtype;
            begin
              open c(10);
              fetch c into v;
              close c;
              open c(q => 1, p => 2);
              open c(1, 2, 3);
              insert into order_line values v;
            end;
            /
            """.trimIndent() + "\n"
        assertThat(cursors(text)).containsExactly(
            "3:c=Local(CURSOR@2:9)",
            "5:c=Local(CURSOR@2:9, BOUND[#1->p#1] omitted [q#2])",
            "6:c=Local(CURSOR@2:9)",
            "7:c=Local(CURSOR@2:9)",
            "8:c=Local(CURSOR@2:9, BOUND[#1->q#2, #2->p#1])",
            "9:c=Local(CURSOR@2:9, TOO_MANY_ARGUMENTS #3)"
        )
        assertThat(categories(text)).containsExactly("10:v=Record(ROWTYPE, QueryColumns, [Q, ?])")
    }

    @Test
    fun theFieldsOfAPackageCursorRow() {
        // Oracle prints r.line_no, r.quantity and r.total; r.qty, the column behind the alias, is PLS-00302.
        val report = """
            create procedure p_report(p_id number) is
              v_line pk_ord.c_lines%rowtype;
            begin
              v_line.total := 1;
              for r in pk_ord.c_lines(p_min => 5, p_order => p_id) loop
                dbms_output.put_line(r.line_no || r.quantity || r.total || r.qty);
                insert into order_line values r;
              end loop;
              insert into order_line values v_line;
            end;
            /
            """.trimIndent() + "\n"
        assertThat(fields(report, "pk_ord.pks" to spec)).containsExactly(
            "4:v_line.total=Field(SELECT_ITEM#2@pk_ord.pks:3:57)",
            "6:r.line_no=Field(SELECT_ITEM#0@pk_ord.pks:3:13)",
            "6:r.quantity=Field(SELECT_ITEM#1@pk_ord.pks:3:31)",
            "6:r.total=Field(SELECT_ITEM#2@pk_ord.pks:3:57)",
            // `l.price + 1` is a field Oracle names by its text: the list is incomplete.
            "6:r.qty=Unknown[UNSUPPORTED_REFERENCE]"
        )
        assertThat(categories(report, "pk_ord.pks" to spec)).containsExactly(
            "7:r=Record(ROWTYPE, [LINE_NO, QUANTITY, TOTAL, ?])",
            "9:v_line=Record(ROWTYPE, [LINE_NO, QUANTITY, TOTAL, ?])"
        )
    }

    @Test
    fun aCompleteListAQuotedAliasAStarAndARepeatedName() {
        val text = """
            create procedure p_rows is
            begin
              for r in pk_ord.c_full loop
                dbms_output.put_line(r.quantity || r.qty);
              end loop;
              for r in pk_ord.c_quoted loop
                dbms_output.put_line(r."Mixed Case" || r.mixed_case);
              end loop;
              for r in pk_ord.c_star loop
                dbms_output.put_line(r.extra || r.line_no);
              end loop;
              for r in pk_ord.c_dup loop
                dbms_output.put_line(r.qty);
              end loop;
            end;
            /
            """.trimIndent() + "\n"
        assertThat(fields(text, "pk_ord.pks" to spec)).containsExactly(
            // Oracle: r.qty and r.mixed_case are PLS-00302.
            "4:r.quantity=Field(SELECT_ITEM#1@pk_ord.pks:6:43)",
            "4:r.qty=Unavailable(NOT_A_COVERED_NAME)",
            "7:r.\"Mixed Case\"=Field(SELECT_ITEM#0@pk_ord.pks:9:34)",
            "7:r.mixed_case=Unavailable(NOT_A_COVERED_NAME)",
            // Oracle prints r.extra and r.line_no, a column `l.*` brings.
            "10:r.extra=Field(SELECT_ITEM#1@pk_ord.pks:7:37)",
            "10:r.line_no=Unknown[UNSUPPORTED_REFERENCE]",
            // Oracle rejects the loop over a select list that repeats a name (PLS-00402).
            "13:r.qty=Unavailable(NOT_A_COVERED_NAME)"
        )
    }

    @Test
    fun aReturnTypeGivesTheLoopItsFields() {
        // Oracle: a loop over c_head or c_both has ORDER_HEAD's columns; c_both%ROWTYPE has the query's A, B, C, and
        // c_head%ROWTYPE, with no query in the specification, ORDER_HEAD's.
        val text = """
            create procedure p_head is
              v_head pk_ord.c_head%rowtype;
              v pk_ord.c_both%rowtype;
            begin
              for r in pk_ord.c_head(10) loop
                insert into order_head values r;
              end loop;
              for r in pk_ord.c_both loop
                insert into order_head values r;
              end loop;
              insert into order_head values v_head;
              insert into order_head values v;
              v.b := 1;
            end;
            /
            """.trimIndent() + "\n"
        assertThat(categories(text, "pk_ord.pks" to spec)).containsExactly(
            "6:r=Record(ROWTYPE, Named(order_head))",
            "9:r=Record(ROWTYPE, Named(order_head))",
            "11:v_head=Record(ROWTYPE, Named(order_head))",
            "12:v=Record(ROWTYPE, [A, B, C])"
        )
        assertThat(fields(text, "pk_ord.pks" to spec)).containsExactly("13:v.b=Field(SELECT_ITEM#1@pk_ord.pks:11:72)")
    }

    @Test
    fun aForwardCursorHasItsReturnTypesRow() {
        // Oracle: g_h, declared where c_h has no query yet, has ORDER_HEAD's columns; v, in the body after the query,
        // has A, B, C (v.head_no is PLS-00302).
        val text = """
            create package pk_f as
              cursor c_h return order_head%rowtype;
              g_h c_h%rowtype;
              procedure p;
            end;
            /
            create package body pk_f as
              cursor c_h return order_head%rowtype is select order_id a, head_no b, customer c from order_head;
              procedure p is
                v c_h%rowtype;
              begin
                insert into order_head values v;
                insert into order_head values g_h;
              end;
            end;
            /
            """.trimIndent() + "\n"
        assertThat(categories(text)).containsExactly(
            "12:v=Record(ROWTYPE, QueryColumns, [A, B, C])",
            "13:g_h=Record(ROWTYPE, Named(order_head))"
        )
    }

    @Test
    fun aKeywordAfterAnExpressionNamesNoField() {
        val probe = """
            create package pk_probe as
              cursor c_items is
                select interval '1' day,
                       interval '1-2' year to month,
                       l.created at time zone 'UTC',
                       l.created at local,
                       date '2020-01-01',
                       timestamp '2020-01-01 00:00:00',
                       l.qty
                  from order_line l;
            end;
            /
            """.trimIndent() + "\n"
        // Oracle prints r.qty and rejects r.day and r.local (PLS-00302): those items have no alias.
        val text = """
            create procedure p_items is
            begin
              for r in pk_probe.c_items loop
                dbms_output.put_line(r.qty || r.day || r.local);
                insert into order_line values r;
              end loop;
              for r in (select interval '1' day, l.created at local, l.name collate binary_ci, l.qty from order_line l) loop
                dbms_output.put_line(r.qty || r.day || r.local);
                insert into order_line values r;
              end loop;
            end;
            /
            """.trimIndent() + "\n"
        assertThat(categories(text, "pk_probe.pks" to probe)).containsExactly(
            "5:r=Record(ROWTYPE, [?, ?, ?, ?, ?, ?, QTY])",
            "9:r=Record(ROWTYPE, QueryColumns, [?, ?, ?, QTY])"
        )
        assertThat(fields(text, "pk_probe.pks" to probe)).containsExactly(
            "4:r.qty=Field(SELECT_ITEM#6@pk_probe.pks:9:13)",
            "4:r.day=Unknown[UNSUPPORTED_REFERENCE]",
            "4:r.local=Unknown[UNSUPPORTED_REFERENCE]",
            "8:r.qty=Field(SELECT_ITEM#3@7:85)",
            "8:r.day=Unknown[UNSUPPORTED_REFERENCE]",
            "8:r.local=Unknown[UNSUPPORTED_REFERENCE]"
        )
    }

    @Test
    fun theFieldsOfALoopOverAQueryOrALocalCursor() {
        val text = """
            create procedure p_loop is
              cursor c is select l.qty amount, l.* from order_line l;
            begin
              for r in (select l.qty amount from order_line l) loop
                dbms_output.put_line(r.amount || r.qty);
              end loop;
              for r in c loop
                dbms_output.put_line(r.amount || r.qty);
              end loop;
            end;
            /
            """.trimIndent() + "\n"
        assertThat(fields(text)).containsExactly(
            "5:r.amount=Field(SELECT_ITEM#0@4:25)",
            "5:r.qty=Unavailable(NOT_A_COVERED_NAME)",
            "8:r.amount=Field(SELECT_ITEM#0@2:27)",
            "8:r.qty=Unknown[UNSUPPORTED_REFERENCE]"
        )
    }
}
