//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier
import org.junit.jupiter.api.Test

class VariableNameCheckTest : BaseCheckTest() {

    @Test
    fun test() {
        PlSqlCheckVerifier.verify(getPath("variable_name.sql"), VariableNameCheck())
    }

    @Test
    fun testIgnoreMethodsByName() {
        val check = VariableNameCheck()
        check.regexp = "\\w{3,}"
        PlSqlCheckVerifier.verify(getPath("variable_name_alternative.sql"), check)
    }

}
