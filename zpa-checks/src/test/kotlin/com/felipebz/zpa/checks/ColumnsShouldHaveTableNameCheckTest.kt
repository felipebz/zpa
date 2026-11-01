//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
