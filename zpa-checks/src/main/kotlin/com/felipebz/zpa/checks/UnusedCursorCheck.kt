//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.semantics.UsageEvidence

@Rule(priority = Priority.MAJOR, tags = [Tags.UNUSED])
@ConstantRemediation("2min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class UnusedCursorCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.CURSOR_DECLARATION)
    }

    override fun visitNode(node: AstNode) {
        // Package specification members are visible to other units; a cursor with RETURN may be defined elsewhere.
        if (node.parent.parentOrNull?.type == PlSqlGrammar.CREATE_PACKAGE || node.hasDirectChildren(PlSqlKeyword.RETURN))
            return

        if (context.usageEvidence(node) != UsageEvidence.Unused)
            return

        addIssue(node, getLocalizedMessage(), node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenValue)
    }

}
