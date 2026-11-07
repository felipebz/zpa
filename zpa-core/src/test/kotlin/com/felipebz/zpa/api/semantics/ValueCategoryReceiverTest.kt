//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.api.semantics

import com.felipebz.flr.api.AstNode
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

/** Members written after an element access or a call result. */
class ValueCategoryReceiverTest {
    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private val packageSpec = "create package p as\n" +
        "  type t_in is record (d date);\n" +
        "  type t_tab is table of t_in;\n" +
        "  type t_rec is record (n number, i t_in);\n" +
        "  g_tab t_tab;\n" +
        "  function f(a number) return t_rec;\n" +
        "end;\n/\n"

    private val types = "create type t_obj as object (a number, member function mf return t_obj);\n/\n" +
        "create type t_nt as table of t_obj;\n/\n"

    private val callerSpec = "create package q as\n  procedure r;\nend;\n/\n"

    private val callerBody = "create package body q as\n" +
        "  type lr is record (n number);\n" +
        "  type lt is table of lr index by pls_integer;\n" +
        "  type lw is record (r p.t_rec);\n" +
        "  type ltt is table of lt index by pls_integer;\n" +
        "  function lf(a number) return lr is begin return null; end;\n" +
        "  function lo(a number) return lr is begin return null; end;\n" +
        "  function lo(a varchar2) return lr is begin return null; end;\n" +
        "  function la(a number) return lr is begin return null; end;\n" +
        "  function la(b number) return lr is begin return null; end;\n" +
        "  procedure r is\n" +
        "    t lt;\n" +
        "    w p.t_tab;\n" +
        "    objs t_nt;\n" +
        "    o t_obj;\n" +
        "    l lw;\n" +
        "    tt ltt;\n" +
        "    x number;\n" +
        "  begin\n" +
        "    x := t(1).n;\n" +
        "    x := w(1).d;\n" +
        "    x := p.g_tab(1).d;\n" +
        "    x := p.f(1).n;\n" +
        "    x := p.f(1).i.d;\n" +
        "    x := lf(1).n;\n" +
        "    x := objs(1).a;\n" +
        "    x := o.mf().a;\n" +
        "    x := l.r.n;\n" +
        "    x := tt(1)(2).n;\n" +
        "    x := lo(1).n;\n" +
        "    x := p.f(1).i;\n" +
        "    x := la(1).n;\n" +
        "  end;\n" +
        "end;\n/\n"

    private val sources = mapOf("p.sql" to packageSpec, "t.sql" to types, "qs.sql" to callerSpec, "u.sql" to callerBody)

    private fun prepared(sources: Map<String, String>): ProjectAnalysisContext = ProjectAnalysisContext.prepared(
        ProjectIndexPreparation().prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
    )

    /** `line:value=category` for the value of every assignment of `u.sql`, and the category of each member item of a receiver. */
    private fun assigned(context: ProjectAnalysisContext, text: String = callerBody, items: MutableList<String>? = null): List<String> {
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                for (assignment in node.getDescendants(PlSqlGrammar.ASSIGNMENT_STATEMENT)) {
                    val value = assignment.children[2].let { if (it.type == PlSqlGrammar.EXPRESSION || it.firstChild?.type == PlSqlGrammar.OBJECT_REFERENCE) it.firstChild else it }
                    if (value.tokens.first().originalValue == "x") continue
                    rows += "${assignment.token.line}:${value.tokens.joinToString("") { it.originalValue }}=${this.context.valueCategory(value)}"
                    if (value.type == PlSqlGrammar.OBJECT_REFERENCE) {
                        value.children.drop(1).filter { it.type != PlSqlPunctuator.DOT }.forEach {
                            items?.add("${it.token.line}:${it.tokens.joinToString("") { t -> t.originalValue }}=${this.context.valueCategory(it)}")
                        }
                        items?.add("${value.token.line}:${value.firstChild.tokens.joinToString("") { it.originalValue }}=${this.context.valueCategory(value.firstChild)}")
                    }
                }
            }
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, context).scanFile(Source("u.sql", text), listOf(check), FileId("u.sql"))
        return rows
    }

    @Test
    fun membersAfterAnElementOrACallResult() {
        assertThat(assigned(prepared(sources))).containsExactly(
            // An element of a local collection of a local record, of a project collection, of a package variable.
            "20:t(1).n=Scalar(NUMERIC)",
            "21:w(1).d=Scalar(DATETIME)",
            "22:p.g_tab(1).d=Scalar(DATETIME)",
            // The result of a project function, then a member path after it.
            "23:p.f(1).n=Scalar(NUMERIC)",
            "24:p.f(1).i.d=Scalar(DATETIME)",
            // The result of a local function.
            "25:lf(1).n=Scalar(NUMERIC)",
            // An element of an object table; the result of a method.
            "26:objs(1).a=Scalar(NUMERIC)",
            "27:o.mf().a=Scalar(NUMERIC)",
            // A field of a local record typed by a project record.
            "28:l.r.n=Scalar(NUMERIC)",
            // One element step per argument list.
            "29:tt(1)(2).n=Scalar(NUMERIC)",
            // Oracle picks lo(number) for the literal, but zpa does not choose overloads by argument type.
            "30:lo(1).n=Unknown[UNSUPPORTED_REFERENCE]",
            "31:p.f(1).i=Record(RECORD_TYPE)",
            // Two declarations no call can tell apart: Oracle rejects the call (PLS-00307).
            "32:la(1).n=Unknown[UNSUPPORTED_REFERENCE]"
        )
    }

    @Test
    fun eachItemAnswersTheChainUpToIt() {
        val items = ArrayList<String>()
        assigned(prepared(sources), items = items)
        // The first child (`p.f(1)`) is not such a node.
        assertThat(items).contains("24:i.d=Scalar(DATETIME)", "24:p.f(1)=Unavailable(NOT_A_COVERED_NAME)", "27:a=Scalar(NUMERIC)")
    }

    @Test
    fun withoutAPreparedProjectOnlyLocalTypesAnswer() {
        // The package's specification in the same file: no lookup waits for another file.
        val withSpec = callerSpec + callerBody
        val shift = callerSpec.count { it == '\n' }
        val rows = assigned(ProjectAnalysisContext.NOT_PREPARED, withSpec).map { row ->
            "${row.substringBefore(':').toInt() - shift}:${row.substringAfter(':')}"
        }
        assertThat(rows.filter { "=Unknown" !in it }).containsExactly("20:t(1).n=Scalar(NUMERIC)", "25:lf(1).n=Scalar(NUMERIC)", "29:tt(1)(2).n=Scalar(NUMERIC)")
    }

    @Test
    fun conditionalCompilationKeepsReceiversUndecided() {
        // A dropped branch that ends a block it does not open may restructure the body: every receiver stays undecided.
        val conditional = callerBody.replace("    x number;\n", "    x number; \$if \$\$flag \$then y number; \$else end; \$end\n")
        val rows = assigned(prepared(sources + ("u.sql" to conditional)), conditional)
        assertThat(rows).hasSize(13).allMatch { it.substringAfter('=').startsWith("Unknown[") && "CONDITIONAL_COMPILATION" in it }
        // Conditional text that writes none of their names leaves them decided.
        val unrelated = conditional.replace("\$else end; ", "")
        assertThat(assigned(prepared(sources + ("u.sql" to unrelated)), unrelated)).hasSize(13).noneMatch { "CONDITIONAL_COMPILATION" in it }
    }

    /** `line:receiver=category` for every receiver of [file]. */
    private fun receivers(file: String, sources: Map<String, String>): List<String> {
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.OBJECT_REFERENCE).forEach {
                    rows += "${it.token.line}:${it.tokens.joinToString("") { t -> t.originalValue }}=${this.context.valueCategory(it)}"
                }
            }
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, prepared(sources)).scanFile(Source(file, sources.getValue(file)), listOf(check), FileId(file))
        return rows
    }

    @Test
    fun receiversInTheValuesOfAnInsert() {
        val body = "create package body q as\n" +
            "  type lr is record (n number);\n" +
            "  type lt is table of lr index by pls_integer;\n" +
            "  procedure r is\n" +
            "    t lt;\n" +
            "    upper lt;\n" +
            "    x number;\n" +
            "  begin\n" +
            "    insert into line_tab (x) values (t(1).n);\n" +
            "    insert into line_tab (x) values (upper(1).n);\n" +
            "    select t(1).n into x from dual;\n" +
            "  end;\n" +
            "end;\n/\n"
        assertThat(receivers("u.sql", mapOf("qs.sql" to callerSpec, "u.sql" to body))).containsExactly(
            // No table column is in scope in the VALUES of a single-table INSERT: the local collection.
            "9:t(1).n=Scalar(NUMERIC)",
            // A SQL built-in of the name is called there, whatever the local collection.
            "10:upper(1).n=Unavailable(NOT_A_COVERED_NAME)",
            // Receivers in other SQL are not modeled.
            "11:t(1).n=Unavailable(NOT_A_COVERED_NAME)"
        )
    }

    private val constructed = "create type t_other as object (a number);\n/\n" +
        "create type t_made as object (a number, constructor function t_made(n number, m number) return self as result);\n/\n" +
        "create type t_method as object (k number, member function t_other(n number) return number, member function run return number);\n/\n" +
        "create type t_attribute as object (t_other number, member function run return number);\n/\n"

    @Test
    fun theResultOfAConstructor() {
        val user = "create procedure u is x number; begin x := t_other(3).a; x := t_made(1, 2).a; end;\n/\n"
        assertThat(receivers("u.sql", mapOf("c.sql" to constructed, "u.sql" to user))).containsExactly(
            // The default constructor, and a user-defined one, build a value of the type.
            "1:t_other(3).a=Scalar(NUMERIC)", "1:t_made(1,2).a=Scalar(NUMERIC)"
        )
    }

    @Test
    fun aMemberOrAFunctionOfTheNameHidesTheConstructor() {
        val bodies = "create type body t_method as\n" +
            "  member function t_other(n number) return number is begin return n + 100; end;\n" +
            "  member function run return number is x number; begin x := t_other(5).a; return 1; end;\n" +
            "end;\n/\n" +
            "create type body t_attribute as\n" +
            "  member function run return number is x number; begin x := t_other(1).a; return 1; end;\n" +
            "end;\n/\n" +
            "create package body q as\n" +
            "  type lr is record (n number);\n" +
            "  function t_other(n number) return lr is v lr; begin return v; end;\n" +
            "  procedure r is x number; begin x := t_other(7).n; end;\n" +
            "end;\n/\n"
        assertThat(receivers("b.sql", mapOf("c.sql" to constructed, "qs.sql" to callerSpec, "b.sql" to bodies))).containsExactly(
            // Oracle binds the method SELF.T_OTHER: not the constructor. The method is defined in this body, so with the
            // specification in another file the name stays undecided.
            "3:t_other(5).a=Unknown[DECLARATION_OUTSIDE_FILE]",
            // Oracle binds the attribute SELF.T_OTHER, a number (PLS-00224 on the call).
            "7:t_other(1).a=Unknown[UNSUPPORTED_REFERENCE]",
            // Oracle binds the package body's function.
            "13:t_other(7).n=Scalar(NUMERIC)"
        )
    }
}
