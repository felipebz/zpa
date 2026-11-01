//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.project.ProjectRecordFieldTypeResolver
import com.felipebz.zpa.project.ProjectRecordMemberResolution

/** Resolves only the type reference of a field whose RECORD member identity is known. */
internal class ProjectRecordFieldTypeResolutionVisitor(
    private val resolver: ProjectRecordFieldTypeResolver
) : PlSqlCheck() {

    init {
        subscribeTo(PlSqlGrammar.MEMBER_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        val memberExpression = node as? SemanticAstNode ?: return
        val member = memberExpression.projectRecordMemberResolution
            as? ProjectRecordMemberResolution.Resolved ?: return
        memberExpression.projectRecordFieldTypeResolution = resolver.resolve(member)
    }
}
