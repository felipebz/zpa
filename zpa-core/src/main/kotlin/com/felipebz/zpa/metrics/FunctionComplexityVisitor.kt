//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.metrics

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck

class FunctionComplexityVisitor : PlSqlCheck() {
    var numberOfFunctions: Int = 0
        private set

    override fun init() {
        subscribeTo(PlSqlGrammar.CREATE_PROCEDURE,
            PlSqlGrammar.CREATE_FUNCTION,
            PlSqlGrammar.PROCEDURE_DECLARATION,
            PlSqlGrammar.FUNCTION_DECLARATION)
    }

    override fun visitNode(node: AstNode) {
        numberOfFunctions++
    }

}
