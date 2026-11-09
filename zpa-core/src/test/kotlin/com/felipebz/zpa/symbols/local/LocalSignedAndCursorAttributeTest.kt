//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Signed numbers and cursor attributes as actuals, typed as Oracle binds them. */
@OptIn(ZpaExperimentalApi::class)
class LocalSignedAndCursorAttributeTest {
    private val project = "create or replace package pk_gauge as\n" +
        "  procedure take(p_val number);\n  procedure take(p_val pls_integer);\n  procedure take(p_val binary_float);\n" +
        "  procedure take(p_val boolean);\nend;\n/\n"
    private val snapshot = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("project.sql", project)), false)
    private val declarations = "  cursor c_orders is select 1 as qty from dual;\n  v_rows sys_refcursor;\n" +
        "  v_whole pls_integer;\n  v_amount number;\n  v_ratio binary_float;\n  v_text varchar2(10);"

    private fun selected(actual: String): String {
        val source = "declare\n$declarations\nbegin\n  pk_gauge.take($actual);\nend;\n/\n"
        val typed = LocalForwardCallTest().analyze(source, snapshot).calls.single { it.target.toString() == "pk_gauge.take" }.typed!!
        return if (typed.status == ProjectTypedCallResolution.Status.RESOLVED) typed.selected.get().declaration.parameters.single().typeRef.name.toString()
        else typed.status.name
    }

    @Test
    fun aSignedNumberHasItsOperandsType() {
        // Oracle prefers PLS_INTEGER for an integer literal, signed or not, and NUMBER for a decimal one.
        assertThat(selected("-1")).isEqualTo("pls_integer")
        assertThat(selected("+1")).isEqualTo("pls_integer")
        assertThat(selected("-1.5")).isEqualTo("number")
        assertThat(selected("-v_whole")).isEqualTo("pls_integer")
        assertThat(selected("-v_amount")).isEqualTo("number")
        assertThat(selected("-v_ratio")).isEqualTo("binary_float")
        // An operand that is not a number: no type.
        assertThat(selected("-v_text")).isEqualTo("AMBIGUOUS")
    }

    @Test
    fun aCursorAttributeIsABooleanOrItsRowCountANumber() {
        assertThat(selected("c_orders%isopen")).isEqualTo("boolean")
        assertThat(selected("v_rows%found")).isEqualTo("boolean")
        assertThat(selected("sql%notfound")).isEqualTo("boolean")
        assertThat(selected("c_orders%rowcount")).isEqualTo("number")
        assertThat(selected("sql%rowcount")).isEqualTo("number")
        // An element of another attribute: no type.
        assertThat(selected("sql%bulk_rowcount(1)")).isEqualTo("AMBIGUOUS")
    }
}
