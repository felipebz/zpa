/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.checks

import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier
import com.felipebz.zpa.checks.verifier.ProjectPlSqlCheckVerifier
import com.felipebz.zpa.checks.verifier.ProjectTestSource
import org.junit.jupiter.api.Test
import java.io.File

class SelectAllColumnsCheckTest : BaseCheckTest() {

    @Test
    fun test() {
        // As a project, so that `pkg.var` is resolved through the package specification of the same file.
        ProjectPlSqlCheckVerifier.verify(
            listOf(ProjectTestSource("select_all_columns.sql", File(getPath("select_all_columns.sql")).readText())),
            SelectAllColumnsCheck()
        )
    }

    @Test
    fun conditionalCompilation() {
        PlSqlCheckVerifier.verify(getPath("select_all_columns_conditional.sql"), SelectAllColumnsCheck())
    }

    @Test
    fun specificationInAnotherFile() {
        ProjectPlSqlCheckVerifier.verify(listOf(SPECIFICATION, ProjectTestSource("pkg.pkb", BODY.replace("-- finding", "-- Noncompliant"))), SelectAllColumnsCheck())
    }

    @Test
    fun specificationInAnotherFileWithoutProject() {
        // Without the specification nothing proves what the destinations are: no finding.
        ProjectPlSqlCheckVerifier.verify(listOf(ProjectTestSource("pkg.pkb", BODY.replace("-- finding", ""))), SelectAllColumnsCheck())
    }

    @Test
    fun cyclicCollectionType() {
        // Oracle rejects the type, but a project may still hold it: its element is unknown.
        ProjectPlSqlCheckVerifier.verify(
            listOf(
                ProjectTestSource("a.sql", "create type a as table of a;\n/\n"),
                ProjectTestSource("p.sql", "create procedure p is\n  l a;\nbegin\n  select * bulk collect into l from emp;\nend;\n/\n")
            ),
            SelectAllColumnsCheck()
        )
    }

    private companion object {
        val SPECIFICATION = ProjectTestSource(
            "pkg.pks",
            """
            create package pkg is
              type rec is record (empno number, ename varchar2(10));
              type rows is table of emp%rowtype;
              type recs is table of rec;
              type nums is table of number;
              v_recs recs;
              v_nums nums;
              r_row emp%rowtype;
              v_rows rows;
              r_rec rec;
              procedure p;
            end;
            /
            """.trimIndent()
        )

        val BODY = """
            create package body pkg is
              procedure p is
              begin
                select * into r_row from emp;
                select * bulk collect into v_rows from emp;
                select * into r_rec from emp; -- finding
                select * bulk collect into v_recs from emp; -- finding
                select * bulk collect into v_nums from emp; -- finding
              end;
            end;
            /
            """.trimIndent()
    }

}
