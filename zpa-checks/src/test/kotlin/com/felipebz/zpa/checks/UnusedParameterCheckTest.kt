//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier
import org.junit.jupiter.api.Test

class UnusedParameterCheckTest : BaseCheckTest() {

    @Test
    fun test() {
        PlSqlCheckVerifier.verify(getPath("unused_parameter.sql"), UnusedParameterCheck())
    }

    @Test
    fun testIgnoreMethodsByName() {
        val check = UnusedParameterCheck()
        check.ignoreMethods = "(on_.*|fullmatch)"
        PlSqlCheckVerifier.verify(getPath("unused_parameter_ignore_methods.sql"), check)
    }

}
