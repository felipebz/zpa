/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
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
