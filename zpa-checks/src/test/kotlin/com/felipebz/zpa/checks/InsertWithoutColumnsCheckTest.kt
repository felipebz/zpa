//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier
import com.felipebz.zpa.checks.verifier.ProjectPlSqlCheckVerifier
import com.felipebz.zpa.checks.verifier.ProjectTestSource
import org.junit.jupiter.api.Test

class InsertWithoutColumnsCheckTest : BaseCheckTest() {

    @Test
    fun test() {
        PlSqlCheckVerifier.verify(getPath("insert_without_columns.sql"), InsertWithoutColumnsCheck())
    }

    @Test
    fun specificationInAnotherFile() {
        ProjectPlSqlCheckVerifier.verify(
            listOf(
                ProjectTestSource(
                    "pkg.pks",
                    """
                    create package pkg is
                      r_row tab%rowtype;
                      n number;
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
                      begin
                        insert into tab values r_row;
                        insert into tab values n; -- Noncompliant
                      end;
                    end;
                    /
                    """.trimIndent()
                )
            ),
            InsertWithoutColumnsCheck()
        )
    }

}
