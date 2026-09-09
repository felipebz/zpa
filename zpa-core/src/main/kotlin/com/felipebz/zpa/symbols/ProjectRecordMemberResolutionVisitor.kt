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
package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.ProjectRecordMemberResolver

/** Connects existing base symbols to project RECORD member metadata without re-resolving types. */
internal class ProjectRecordMemberResolutionVisitor(
    private val resolver: ProjectRecordMemberResolver
) : PlSqlCheck() {

    init {
        subscribeTo(PlSqlGrammar.MEMBER_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        val memberExpression = node as? SemanticAstNode ?: return
        if (memberExpression.symbol != null) return

        val parts = node.getChildren(PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.VARIABLE_NAME)
        if (parts.size != 2 || parts.first().type != PlSqlGrammar.VARIABLE_NAME ||
            parts.last().type != PlSqlGrammar.IDENTIFIER_NAME) return

        val baseSymbol = (parts.first() as? SemanticAstNode)?.symbol ?: return
        val projectType = baseSymbol.projectTypeDeclaration ?: return
        val memberName = OracleIdentifier.fromSource(parts.last().tokenOriginalValue)
        memberExpression.projectRecordMemberResolution = resolver.resolve(projectType, memberName)
    }
}
