//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier
import org.junit.jupiter.api.Test

class UnnecessaryAliasInQueryCheckTest : BaseCheckTest() {

    @Test
    fun test() {
        PlSqlCheckVerifier.verify(getPath("unnecessary_alias_in_query.sql"), UnnecessaryAliasInQueryCheck())
    }

    @Test
    fun test2() {
        val check = UnnecessaryAliasInQueryCheck()
        check.acceptedLength = 4

        PlSqlCheckVerifier.verify(getPath("unnecessary_alias_in_query_custom_length.sql"), check)
    }

}
