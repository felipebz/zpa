//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.metrics

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.checks.PlSqlCheck

open class ComplexityVisitor : PlSqlCheck() {

    var complexity: Int = 0
        private set

    override fun init() {
        subscribeTo(
                PlSqlGrammar.CREATE_PROCEDURE,
                PlSqlGrammar.CREATE_FUNCTION,
                PlSqlGrammar.ANONYMOUS_BLOCK,

                PlSqlGrammar.PROCEDURE_DECLARATION,
                PlSqlGrammar.FUNCTION_DECLARATION,

                PlSqlGrammar.LOOP_STATEMENT,
                PlSqlGrammar.CONTINUE_STATEMENT,
                PlSqlGrammar.FOR_STATEMENT,
                PlSqlGrammar.EXIT_STATEMENT,
                PlSqlGrammar.IF_STATEMENT,
                PlSqlGrammar.RAISE_STATEMENT,
                PlSqlGrammar.RETURN_STATEMENT,
                PlSqlGrammar.WHILE_STATEMENT,

                // this includes WHEN in exception handlers, exit/continue statements and CASE expressions
                PlSqlKeyword.WHEN,
                PlSqlKeyword.ELSIF)
    }

    override fun visitFile(node: AstNode) {
        complexity = 0
    }

    override fun visitNode(node: AstNode) {
        complexity++
    }

}
