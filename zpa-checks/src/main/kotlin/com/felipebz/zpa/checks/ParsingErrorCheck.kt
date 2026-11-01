//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.Priority
import com.felipebz.zpa.api.annotations.Rule
import com.felipebz.zpa.api.annotations.RuleInfo

@Rule(priority = Priority.INFO)
@RuleInfo(scope = RuleInfo.Scope.ALL)
class ParsingErrorCheck : AbstractBaseCheck() {

    override fun init() {
        val parsingException = context.parsingException()
        if (parsingException != null) {
            parsingException.message?.let {
                addLineIssue(it, parsingException.line)
            }
        }

        subscribeTo(PlSqlGrammar.RECOVERY)
    }

    override fun visitNode(node: AstNode) {
        addIssue(node, getLocalizedMessage())
    }

}
