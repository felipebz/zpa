//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.semantics.UsageEvidence

@Rule(priority = Priority.MAJOR, tags = [Tags.UNUSED])
@ConstantRemediation("2min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class UnusedVariableCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.VARIABLE_DECLARATION, PlSqlGrammar.EXCEPTION_DECLARATION)
    }

    override fun visitNode(node: AstNode) {
        // Package specification members are visible to other units.
        if (node.parent.parentOrNull?.type == PlSqlGrammar.CREATE_PACKAGE)
            return

        if (context.usageEvidence(node) != UsageEvidence.Unused)
            return

        val name = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        addIssue(name, getLocalizedMessage(), name.tokenValue)
    }

}
