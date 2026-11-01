//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier
import org.junit.jupiter.api.Test

class UtPlSqlTestShouldHaveDescriptionCheckTest : BaseCheckTest() {

    @Test
    fun reportsEffectiveTestsWithoutDescriptions() {
        PlSqlCheckVerifier.verify(
            getPath("utplsql_test_should_have_description.sql"),
            UtPlSqlTestShouldHaveDescriptionCheck()
        )
    }
}
