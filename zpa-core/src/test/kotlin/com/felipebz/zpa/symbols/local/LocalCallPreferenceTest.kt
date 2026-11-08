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

/** Calls from local code bound to the overload Oracle prefers, with literal and declared actuals. */
@OptIn(ZpaExperimentalApi::class)
class LocalCallPreferenceTest {
    private val spec = "create or replace package app_owner.pk_pick as\n" +
        "  function take(p_val number) return varchar2;\n" +
        "  function take(p_val varchar2) return varchar2;\n" +
        "  function take(p_val pls_integer) return varchar2;\n" +
        "  function take(p_val timestamp_unconstrained) return varchar2;\n" +
        "  function take(p_val shape_obj_t) return varchar2;\n" +
        "end;\n/\n"
    private val snapshot = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("pk_pick.sql", spec)), false)

    private fun selected(declarations: String, actual: String): String {
        val source = "declare\n$declarations\n  r varchar2(10);\nbegin\n  r := app_owner.pk_pick.take($actual);\nend;\n/\n"
        val call = LocalForwardCallTest().analyze(source, snapshot).calls.single()
        assertThat(call.outcome).isEqualTo(LocalCallOutcome.PROJECT)
        val typed = call.typed!!
        return if (typed.status == ProjectTypedCallResolution.Status.RESOLVED) typed.selected.get().declaration.parameters.single().typeRef.name.toString()
        else typed.status.name
    }

    @Test
    fun aSchemaQualifiedCallReachesItsPackageAndBindsTheClosestOverload() {
        // An integer literal binds PLS_INTEGER, a decimal one NUMBER, a character literal VARCHAR2.
        assertThat(selected("", "1")).isEqualTo("pls_integer")
        assertThat(selected("", "1.5")).isEqualTo("number")
        assertThat(selected("", "'abc'")).isEqualTo("varchar2")
        assertThat(selected("  v_amount integer;", "v_amount")).isEqualTo("number")
        assertThat(selected("  v_at timestamp(6);", "v_at")).isEqualTo("timestamp_unconstrained")
    }

    @Test
    fun anActualWithoutAnExactTypeLeavesTheCallAmbiguous() {
        // A timestamp literal may carry a time zone; NULL matches every formal (PLS-00307).
        assertThat(selected("", "timestamp '2020-01-01 00:00:00'")).isEqualTo("AMBIGUOUS")
        assertThat(selected("", "null")).isEqualTo("AMBIGUOUS")
    }
}
