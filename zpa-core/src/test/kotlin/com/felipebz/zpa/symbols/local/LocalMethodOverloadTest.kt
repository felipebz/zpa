//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.project.ProjectSnapshots
import com.felipebz.zpa.api.project.ProjectSourceInput
import com.felipebz.zpa.api.project.ProjectTypedCallResolution
import com.felipebz.zpa.api.semantics.ValueCategory
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** Object method calls with several applicable overloads, told apart by their actuals' datatypes as Oracle binds them. */
class LocalMethodOverloadTest {
    private val project = "create or replace type num_box_t as object (qty number);\n/\n" +
        "create or replace type text_box_t as object (qty varchar2(10));\n/\n" +
        "create or replace type gauge_t as object (lvl number,\n" +
        "  member function m(p number) return number,\n  member function m(p varchar2) return varchar2,\n" +
        "  member function w(p number) return varchar2,\n  member function w(p pls_integer) return number,\n" +
        "  member function s(self in gauge_t, p number) return number,\n  member function s(p varchar2) return varchar2,\n" +
        "  static function k(p number) return number,\n  member function k(p varchar2) return varchar2,\n" +
        "  member function me return gauge_t,\n" +
        "  member function box(p number) return num_box_t,\n  member function box(p varchar2) return text_box_t) not final;\n/\n" +
        "create or replace type base_dial_t as object (lvl number, member function m(p number) return varchar2) not final;\n/\n" +
        "create or replace type sub_dial_t under base_dial_t (member function m(p varchar2) return number);\n/\n" +
        "create or replace type over_dial_t under base_dial_t (\n" +
        "  overriding member function m(p number) return varchar2,\n  member function m(p varchar2) return number);\n/\n" +
        "create or replace type sup_t as object (n number,\n" +
        "  member function t(p timestamp) return number,\n  member function i(p interval day to second) return number) not final;\n/\n" +
        "create or replace type sub_t under sup_t (\n" +
        "  member function t(p timestamp with time zone) return varchar2,\n  member function i(p interval year to month) return varchar2) not final;\n/\n" +
        "create or replace type sub3_t under sup_t (\n" +
        "  member function t(p timestamp with time zone) return varchar2,\n  member function t(p number) return varchar2);\n/\n" +
        "create or replace type one_t as object (n number,\n" +
        "  member function t(p timestamp) return number,\n  member function t(p timestamp with time zone) return varchar2);\n/\n" +
        "create or replace package pk_take as\n  procedure take(p_val number);\n  procedure take(p_val varchar2);\nend;\n/\n"

    private val declarations = "  v gauge_t;\n  b base_dial_t;\n  d sub_dial_t;\n  o over_dial_t;\n  v_amount number;\n" +
        "  s sub_t;\n  s3 sub3_t;\n  w one_t;\n  ts timestamp;\n  tz timestamp with time zone;\n  l timestamp with local time zone;\n" +
        "  ds interval day to second;\n  ym interval year to month;"

    private fun selected(actual: String): String {
        val snapshot = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("project.sql", project)), false)
        val source = "declare\n$declarations\nbegin\n  pk_take.take($actual);\nend;\n/\n"
        val typed = LocalForwardCallTest().analyze(source, snapshot).calls.single { it.target.toString() == "pk_take.take" }.typed!!
        return if (typed.status == ProjectTypedCallResolution.Status.RESOLVED) typed.selected.get().declaration.parameters.single().typeRef.name.toString()
        else typed.status.name
    }

    @Test
    fun theDatatypeOfTheActualPicksTheOverload() {
        assertThat(selected("v.m(1)")).isEqualTo("number")
        assertThat(selected("v.m('a')")).isEqualTo("varchar2")
        // A DATE converts to VARCHAR2 and not to NUMBER.
        assertThat(selected("v.m(sysdate)")).isEqualTo("varchar2")
        // Oracle prefers PLS_INTEGER for an integer literal, NUMBER for a decimal one.
        assertThat(selected("v.w(1)")).isEqualTo("number")
        assertThat(selected("v.w(1.5)")).isEqualTo("varchar2")
        // NULL fits both (PLS-00307), and an actual of no type tells nothing.
        assertThat(selected("v.m(null)")).isEqualTo("AMBIGUOUS")
        assertThat(selected("v.m(v_amount + 1)")).isEqualTo("AMBIGUOUS")
    }

    @Test
    fun anExplicitSelfIsTheReceiver() {
        assertThat(selected("v.s(1)")).isEqualTo("number")
        assertThat(selected("v.s('a')")).isEqualTo("varchar2")
    }

    @Test
    fun aStaticOverloadChosenThroughAValueIsRejected() {
        assertThat(selected("v.k('a')")).isEqualTo("varchar2")
        // Oracle picks the static k(number), then rejects it on a value (PLS-00587): no type.
        assertThat(selected("v.k(1)")).isEqualTo("AMBIGUOUS")
    }

    @Test
    fun overloadsAcrossTheTypeChain() {
        assertThat(selected("d.m(1)")).isEqualTo("varchar2")
        assertThat(selected("d.m('a')")).isEqualTo("number")
        // A value declared as the supertype sees the supertype's methods only.
        assertThat(selected("b.m('a')")).isEqualTo("varchar2")
        // The overriding method stands for the one it overrides.
        assertThat(selected("o.m(1)")).isEqualTo("varchar2")
        assertThat(selected("o.m('a')")).isEqualTo("number")
    }

    @Test
    fun formalsOfOneTypeNameWithDifferentWrittenTypes() {
        // A TIMESTAMP WITH TIME ZONE or a YEAR TO MONTH formal overloads, and does not override, an inherited TIMESTAMP or
        // DAY TO SECOND one.
        assertThat(selected("s.t(ts)")).isEqualTo("number")
        assertThat(selected("s.t(tz)")).isEqualTo("varchar2")
        assertThat(selected("s.i(ds)")).isEqualTo("number")
        assertThat(selected("s.i(ym)")).isEqualTo("varchar2")
        assertThat(selected("s3.t(ts)")).isEqualTo("number")
        assertThat(selected("s3.t(1)")).isEqualTo("varchar2")
        assertThat(selected("w.t(ts)")).isEqualTo("number")
        assertThat(selected("w.t(tz)")).isEqualTo("varchar2")
        // WITH LOCAL TIME ZONE prefers neither (PLS-00307).
        assertThat(selected("w.t(l)")).isEqualTo("AMBIGUOUS")
    }

    @Test
    fun aMethodOfAConstructorResult() {
        assertThat(selected("gauge_t(1).m(1)")).isEqualTo("number")
        assertThat(selected("gauge_t(1).m(null)")).isEqualTo("AMBIGUOUS")
    }

    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    /** The category of the member `qty` after each receiver, in written order. */
    private fun quantities(vararg receivers: String): List<String> {
        val user = "create procedure u is\n  v gauge_t;\n  x varchar2(10);\nbegin\n" + receivers.joinToString("") { "  x := $it.qty;\n" } + "end;\n/\n"
        val sources = mapOf("project.sql" to project, "u.sql" to user)
        val context = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false))
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.VARIABLE_NAME).filter { it.tokenValue == "QTY" }.forEach {
                    val category = this.context.valueCategories!!.category(it)
                    rows += (category as? ValueCategory.Scalar)?.family?.name ?: category::class.simpleName!!
                }
            }
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, context).scanFile(Source("u.sql", user), listOf(check), FileId("u.sql"))
        return rows
    }

    @Test
    fun aReceiverAfterAMethodCall() {
        assertThat(quantities("v.box(1)", "v.box('a')", "v.me().box(1)", "v.me().box('a')", "v.box(null)", "v.me().box(null)"))
            .containsExactly("NUMERIC", "CHARACTER", "NUMERIC", "CHARACTER", "Unknown", "Unknown")
    }
}
