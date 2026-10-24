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
