//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ConstantRemediation
import com.felipebz.zpa.api.annotations.Priority
import com.felipebz.zpa.api.annotations.Rule
import com.felipebz.zpa.api.annotations.RuleInfo

@Rule(priority = Priority.MAJOR)
@ConstantRemediation("10min")
@RuleInfo(scope = RuleInfo.Scope.MAIN)
class CursorBodyInPackageSpecCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.CREATE_PACKAGE)
    }

    override fun visitNode(node: AstNode) {
        val cursors = node.getFirstChildOrNull(PlSqlGrammar.DECLARE_SECTION)?.getChildren(PlSqlGrammar.CURSOR_DECLARATION).orEmpty()
        for (cursor in cursors) {
            if (cursor.hasDirectChildren(DmlGrammar.SELECT_EXPRESSION)) {
                addIssue(cursor, getLocalizedMessage(), cursor.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenValue)
            }
        }
    }

}
