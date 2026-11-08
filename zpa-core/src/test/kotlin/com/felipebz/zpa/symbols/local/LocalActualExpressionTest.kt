//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectSnapshots
import com.felipebz.zpa.api.project.ProjectSourceInput
import com.felipebz.zpa.api.project.ProjectTypedCallResolution
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Actuals written as elements and their members, conditions, built-ins without parentheses, TREAT and concatenations,
 * each bound as Oracle bound it.
 */
@OptIn(ZpaExperimentalApi::class)
class LocalActualExpressionTest {
    private val project = "create or replace type crate_obj_t as object (label varchar2(30), qty number, tags crate_tags_t) not final;\n/\n" +
        "create or replace type crate_tags_t as table of varchar2(30);\n/\n" +
        "create or replace type big_crate_t under crate_obj_t (depth number);\n/\n" +
        "create or replace type crate_list_t as table of crate_obj_t;\n/\n" +
        "create or replace package pk_take as\n" +
        "  procedure take(p_val number);\n  procedure take(p_val pls_integer);\n  procedure take(p_val varchar2);\n" +
        "  procedure take(p_val clob);\n  procedure take(p_val date);\n  procedure take(p_val timestamp with time zone);\n" +
        "  procedure take(p_val boolean);\nend;\n/\n"
    private val snapshot = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("project.sql", project)), false)

    private fun selected(declarations: String, actual: String): String {
        val source = "declare\n$declarations\nbegin\n  pk_take.take($actual);\nend;\n/\n"
        val typed = LocalForwardCallTest().analyze(source, snapshot).calls.single { it.target.toString() == "pk_take.take" }.typed!!
        return if (typed.status == ProjectTypedCallResolution.Status.RESOLVED) typed.selected.get().declaration.parameters.single().typeRef.name.toString()
        else typed.status.name
    }

    private val crates = "  v_crates crate_list_t;"

    @Test
    fun anElementAndItsMembersAreTyped() {
        assertThat(selected(crates, "v_crates(1).label")).isEqualTo("varchar2")
        assertThat(selected(crates, "v_crates(1).qty")).isEqualTo("number")
        assertThat(selected(crates, "v_crates(1).tags.count")).isEqualTo("pls_integer")
        assertThat(selected("  type local_list_t is table of crate_obj_t;\n  v_crates local_list_t;", "v_crates(1).qty")).isEqualTo("number")
        assertThat(selected("  v_tags crate_tags_t;", "v_tags(1)")).isEqualTo("varchar2")
        // A member that is not an attribute: no type.
        assertThat(selected(crates, "v_crates(1).missing")).isEqualTo("AMBIGUOUS")
    }

    @Test
    fun aConditionIsABoolean() {
        assertThat(selected("  v_label varchar2(10);", "v_label in ('a', 'b')")).isEqualTo("boolean")
        assertThat(selected("  v_qty number;", "v_qty between 0 and 2")).isEqualTo("boolean")
        assertThat(selected("  v_qty number;", "v_qty is null")).isEqualTo("boolean")
        assertThat(selected("  v_label varchar2(10);", "v_label like 'a%'")).isEqualTo("boolean")
    }

    @Test
    fun aBuiltInWithoutParenthesesIsTypedUnlessALocalDeclarationTakesTheName() {
        assertThat(selected("", "sysdate")).isEqualTo("date")
        assertThat(selected("", "systimestamp")).isEqualTo("timestamp")
        assertThat(selected("", "sqlcode")).isEqualTo("pls_integer")
        assertThat(selected("", "sqlerrm")).isEqualTo("varchar2")
        assertThat(selected("", "user")).isEqualTo("varchar2")
        // A local declaration takes the name (Oracle binds it, not STANDARD's function).
        assertThat(selected("  function sysdate return number is begin return 1; end;", "sysdate")).isEqualTo("number")
        assertThat(selected("  sqlerrm number := 1;", "sqlerrm")).isEqualTo("number")
        // A built-in written with parentheses is a call: not typed here.
        assertThat(selected("", "sqlerrm(1)")).isEqualTo("AMBIGUOUS")
    }

    @Test
    fun treatIsAValueOfTheTypeItNames() {
        assertThat(selected("  v_crate crate_obj_t;", "treat(v_crate as big_crate_t).depth")).isEqualTo("AMBIGUOUS")
        val owned = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("project.sql", project.replace("big_crate_t under", "app_owner.big_crate_t under"))), false)
        val source = "declare\n  v_crate crate_obj_t;\nbegin\n  pk_take.take(treat(v_crate as app_owner.big_crate_t).depth);\n  " +
            "pk_take.take(treat(v_crate as app_owner.big_crate_t).tags.count);\nend;\n/\n"
        val calls = LocalForwardCallTest().analyze(source, owned).calls.filter { it.target.toString() == "pk_take.take" }
        assertThat(calls.map { it.typed!!.selected.get().declaration.parameters.single().typeRef.name.toString() }).containsExactly("number", "pls_integer")
    }

    @Test
    fun aConcatenationIsAVarchar2OrAClob() {
        assertThat(selected("  v_label varchar2(10);\n  v_qty number;", "v_label || v_qty")).isEqualTo("varchar2")
        assertThat(selected("  v_day date;", "v_day || 'x' || null")).isEqualTo("varchar2")
        assertThat(selected("  v_text clob;", "'a' || v_text")).isEqualTo("clob")
        // An operand of no type, or a CLOB with a number: not typed.
        assertThat(selected("  v_text clob;\n  v_qty number;", "v_text || v_qty")).isEqualTo("AMBIGUOUS")
        assertThat(selected("", "'a' || pk_unknown.v_some")).isEqualTo("AMBIGUOUS")
    }
}
