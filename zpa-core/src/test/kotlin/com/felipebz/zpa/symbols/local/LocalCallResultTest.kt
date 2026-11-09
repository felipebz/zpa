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

/** Actuals that are calls, typed by the RETURN type of the method or function they bind to, as Oracle binds them. */
@OptIn(ZpaExperimentalApi::class)
class LocalCallResultTest {
    private val project = "create or replace type crate_obj_t as object (qty number,\n" +
        "  member function label return varchar2,\n  member function weight return number,\n" +
        "  member function rows_of return sys_refcursor,\n" +
        "  member function pick(p_n number) return varchar2,\n  member function pick(p_s varchar2) return number) not final;\n/\n" +
        "create or replace type big_crate_t under crate_obj_t (depth number);\n/\n" +
        "create or replace type crate_list_t as table of crate_obj_t;\n/\n" +
        "create or replace package pk_src as\n" +
        "  function same(p_x number) return varchar2;\n  function same(p_x varchar2) return varchar2;\n" +
        "  function mixed(p_x number) return varchar2;\n  function mixed(p_x varchar2) return number;\n" +
        "  function cursor_of return sys_refcursor;\nend;\n/\n" +
        "create or replace package pk_take as\n" +
        "  procedure take(p_val number);\n  procedure take(p_val varchar2);\n  procedure take(p_val sys_refcursor);\n" +
        "  procedure take(p_val anydata);\nend;\n/\n"
    private val snapshot = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("project.sql", project)), false)

    private fun selected(declarations: String, actual: String): String {
        val source = "declare\n$declarations\nbegin\n  pk_take.take($actual);\nend;\n/\n"
        val typed = LocalForwardCallTest().analyze(source, snapshot).calls.single { it.target.toString() == "pk_take.take" }.typed!!
        return if (typed.status == ProjectTypedCallResolution.Status.RESOLVED) typed.selected.get().declaration.parameters.single().typeRef.name.toString()
        else typed.status.name
    }

    @Test
    fun aMethodOfAnObjectValueIsItsReturnType() {
        assertThat(selected("  v_crate crate_obj_t;", "v_crate.label()")).isEqualTo("varchar2")
        assertThat(selected("  v_crate crate_obj_t;", "v_crate.weight()")).isEqualTo("number")
        assertThat(selected("  v_crate crate_obj_t;", "v_crate.rows_of()")).isEqualTo("sys_refcursor")
        // The overload the call binds decides: the label names pick(p_n number), which returns VARCHAR2.
        assertThat(selected("  v_crate crate_obj_t;", "v_crate.pick(p_n => 1)")).isEqualTo("varchar2")
        // Overloads with different RETURN types that the call does not tell apart: no type.
        assertThat(selected("  v_crate crate_obj_t;", "v_crate.pick(null)")).isEqualTo("AMBIGUOUS")
        // No such method.
        assertThat(selected("  v_crate crate_obj_t;", "v_crate.missing()")).isEqualTo("AMBIGUOUS")
    }

    @Test
    fun aMethodOfAConstructorElementOrTreatResult() {
        assertThat(selected("", "crate_obj_t(1).label()")).isEqualTo("varchar2")
        assertThat(selected("  v_crates crate_list_t;", "v_crates(1).weight()")).isEqualTo("number")
        // TREAT names a schema-qualified type only; unqualified, it stays untyped.
        assertThat(selected("  v_crate crate_obj_t;", "treat(v_crate as big_crate_t).label()")).isEqualTo("AMBIGUOUS")
        assertThat(selected("", "unknown_t(1).label()")).isEqualTo("AMBIGUOUS")
    }

    @Test
    fun aProjectFunctionIsItsReturnType() {
        // A SYS_REFCURSOR result binds SYS_REFCURSOR, which ANYDATA rejects.
        assertThat(selected("", "pk_src.cursor_of()")).isEqualTo("sys_refcursor")
        // Every candidate returns VARCHAR2: whichever binds (Oracle raises PLS-00307 for NULL), the result is VARCHAR2.
        assertThat(selected("", "pk_src.same(null)")).isEqualTo("varchar2")
        // Candidates with different RETURN types: no type.
        assertThat(selected("", "pk_src.mixed(null)")).isEqualTo("AMBIGUOUS")
    }
}
