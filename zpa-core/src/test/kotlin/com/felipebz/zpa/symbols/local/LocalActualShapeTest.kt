//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectCandidateCompatibility
import com.felipebz.zpa.api.project.ProjectSnapshots
import com.felipebz.zpa.api.project.ProjectSourceInput
import com.felipebz.zpa.api.project.ProjectTypedCallResolution
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Actuals typed from their shape: cursor variables, members of a value, COUNT of a collection; Oracle 26ai bindings. */
@OptIn(ZpaExperimentalApi::class)
class LocalActualShapeTest {
    private val project = "create or replace type shape_obj_t as object (label varchar2(30), size_n number, tags tag_list_t);\n/\n" +
        "create or replace type tag_list_t as table of varchar2(30);\n/\n" +
        "create or replace package pk_take as\n" +
        "  procedure take(p_val number);\n  procedure take(p_val pls_integer);\n  procedure take(p_val varchar2);\n" +
        "  procedure take(p_val boolean);\n  procedure take(p_val sys_refcursor);\n  procedure take(p_val anydata);\n" +
        "  procedure take(p_val json_element_t);\nend;\n/\n"
    private val snapshot = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("project.sql", project)), false)

    private fun selected(declarations: String, actual: String): String {
        val source = "declare\n$declarations\nbegin\n  pk_take.take($actual);\nend;\n/\n"
        val typed = LocalForwardCallTest().analyze(source, snapshot).calls.single { it.target.toString() == "pk_take.take" }.typed!!
        return if (typed.status == ProjectTypedCallResolution.Status.RESOLVED) typed.selected.get().declaration.parameters.single().typeRef.name.toString()
        else typed.status.name
    }

    @Test
    fun aCursorVariableBindsSysRefcursor() {
        // Oracle: a SYS_REFCURSOR, a weak and a strong ref cursor variable each bind SYS_REFCURSOR; ANYDATA and
        // JSON_ELEMENT_T reject them.
        assertThat(selected("  v_cur sys_refcursor;", "v_cur")).isEqualTo("sys_refcursor")
        assertThat(selected("  type cur_t is ref cursor;\n  v_cur cur_t;", "v_cur")).isEqualTo("sys_refcursor")
        assertThat(selected("  type row_t is record (n number);\n  type cur_t is ref cursor return row_t;\n  v_cur cur_t;", "v_cur")).isEqualTo("sys_refcursor")
    }

    @Test
    fun aSchemaObjectValueRejectsScalarsAndCursorsButNotOtherObjectTypes() {
        // Oracle binds ANYDATA here, but JSON_ELEMENT_T names another schema object, which a synonym may relate to it.
        assertThat(selected("  v_data anydata;", "v_data")).isEqualTo("AMBIGUOUS")
        val typed = LocalForwardCallTest().analyze("declare\n  v_data anydata;\nbegin\n  pk_take.take(v_data);\nend;\n/\n", snapshot)
            .calls.single().typed!!
        assertThat(typed.evaluations.filter { it.outcome == ProjectCandidateCompatibility.Outcome.RETAINED }
            .map { it.candidate.declaration.parameters.single().typeRef.name.toString() }).containsExactly("anydata", "json_element_t")
    }

    @Test
    fun aMemberOfAValueIsTypedFromItsDeclaredType() {
        // An attribute of a project object type: VARCHAR2 binds VARCHAR2, NUMBER binds NUMBER.
        assertThat(selected("  v_shape shape_obj_t;", "v_shape.label")).isEqualTo("varchar2")
        assertThat(selected("  v_shape shape_obj_t;", "v_shape.size_n")).isEqualTo("number")
        // No such attribute: no type.
        assertThat(selected("  v_shape shape_obj_t;", "v_shape.missing")).isEqualTo("AMBIGUOUS")
    }

    @Test
    fun countOfACollectionIsAPlsInteger() {
        // Oracle: COUNT binds PLS_INTEGER over NUMBER and VARCHAR2, for a project and a local collection.
        assertThat(selected("  v_tags tag_list_t;", "v_tags.count")).isEqualTo("pls_integer")
        assertThat(selected("  v_shape shape_obj_t;", "v_shape.tags.count")).isEqualTo("pls_integer")
        assertThat(selected("  type num_list_t is table of number;\n  v_nums num_list_t;", "v_nums.count")).isEqualTo("pls_integer")
        // FIRST depends on the index type (VARCHAR2 for an array indexed by VARCHAR2): not typed.
        assertThat(selected("  v_tags tag_list_t;", "v_tags.first")).isEqualTo("AMBIGUOUS")
    }
}
