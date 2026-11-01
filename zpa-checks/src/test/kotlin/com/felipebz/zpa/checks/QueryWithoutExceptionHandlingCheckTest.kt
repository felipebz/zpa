//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier
import org.junit.jupiter.api.Test

class QueryWithoutExceptionHandlingCheckTest : BaseCheckTest() {

    @Test
    fun test() {
        PlSqlCheckVerifier.verify(getPath("query_without_exception_handling.sql"), QueryWithoutExceptionHandlingCheck())
    }

    @Test
    fun testNoStrict() {
        val check = QueryWithoutExceptionHandlingCheck()
        check.strictMode = false
        PlSqlCheckVerifier.verify(getPath("query_without_exception_handling_no_strict.sql"), check)
    }

}
