//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier
import com.felipebz.zpa.checks.verifier.ProjectPlSqlCheckVerifier
import com.felipebz.zpa.checks.verifier.ProjectTestSource
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class VariableHidingCheckTest : BaseCheckTest() {

    @Test
    fun test() {
        PlSqlCheckVerifier.verify(getPath("variable_hiding.sql"), VariableHidingCheck())
    }

    /** The message names the specification file by its name, whatever identity the runner gives the file. */
    @ParameterizedTest
    @ValueSource(strings = ["counter_pkg.pks", "src/db/counter_pkg.pks", "file:///home/runner/work/app/src/counter_pkg.pks"])
    fun aSpecificationInAnotherFile(specificationId: String) {
        ProjectPlSqlCheckVerifier.verify(
            listOf(
                ProjectTestSource(specificationId, "create package counter_pkg as\n  counter number;\n  procedure run;\nend;\n/\n"),
                ProjectTestSource(
                    "counter_pkg.pkb",
                    """
                    create package body counter_pkg as
                      procedure run is
                        counter number; -- Noncompliant {{This variable "COUNTER" hides the declaration on line 2 of counter_pkg.pks.}}
                      begin
                        null;
                      end;
                    end;
                    /
                    """.trimIndent()
                )
            ),
            VariableHidingCheck()
        )
    }

}
