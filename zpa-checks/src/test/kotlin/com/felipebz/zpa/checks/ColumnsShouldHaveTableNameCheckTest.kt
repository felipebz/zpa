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

class ColumnsShouldHaveTableNameCheckTest : BaseCheckTest() {

    @Test
    fun test() {
        PlSqlCheckVerifier.verify(getPath("columns_should_have_table_name.sql"), ColumnsShouldHaveTableNameCheck())
    }

    @Test
    fun conditionalCompilation() {
        PlSqlCheckVerifier.verify(getPath("columns_should_have_table_name_conditional.sql"), ColumnsShouldHaveTableNameCheck())
    }

    @Test
    fun packageBodyWithSpecificationInAnotherFile() {
        ProjectPlSqlCheckVerifier.verify(
            listOf(
                ProjectTestSource(
                    "pkg.pks",
                    """
                    create package pkg is
                      other_col number;
                      procedure p;
                    end;
                    /
                    """.trimIndent()
                ),
                ProjectTestSource(
                    "pkg.pkb",
                    """
                    create package body pkg is
                      procedure p is
                        n number;
                      begin
                        select col into n from tab, tab2; -- Noncompliant
                        -- the specification's variable may be what the query reads
                        select other_col into n from tab, tab2;
                      end;
                    end;
                    /
                    """.trimIndent()
                )
            ),
            ColumnsShouldHaveTableNameCheck()
        )
    }

}
