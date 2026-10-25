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
