//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier
import org.junit.jupiter.api.Test

class AddParenthesesInNestedExpressionCheckTest : BaseCheckTest() {

    @Test
    fun test() {
        PlSqlCheckVerifier.verify(getPath("add_parentheses_in_nested_expression.sql"), AddParenthesesInNestedExpressionCheck())
    }

}
