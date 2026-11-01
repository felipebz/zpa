//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.semantics.HidingEvidence

@Rule(priority = Priority.MAJOR)
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class VariableHidingCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.VARIABLE_DECLARATION, PlSqlGrammar.EXCEPTION_DECLARATION)
    }

    override fun visitNode(node: AstNode) {
        val hidden = (context.hidingEvidence(node) as? HidingEvidence.Hides)?.hidden ?: return
        val identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        val name = identifier.tokenValue
        val hiddenNode = hidden.node
        if (hiddenNode != null) {
            addIssue(identifier, getLocalizedMessage(), name, hidden.line).secondary(hiddenNode, "Original")
        } else {
            // A package specification in another file: no secondary location there. The message names the file by its name
            // alone, which does not depend on the machine or on how the runner identifies files.
            addIssue(identifier, getLocalizedMessage("otherFileMessage"), name, hidden.line, hidden.fileName ?: "")
        }
    }

}
