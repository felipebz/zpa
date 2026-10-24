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
import java.util.*

/** Connects existing base symbols to project RECORD member metadata without re-resolving types. */
internal class ProjectRecordMemberResolutionVisitor(
    private val resolver: ProjectRecordMemberResolver,
    private val selfBase: ProjectSelfMemberBase? = null
) : PlSqlCheck() {

    init {
        subscribeTo(PlSqlGrammar.MEMBER_EXPRESSION)
    }

    private val decorated = Collections.newSetFromMap(IdentityHashMap<AstNode, Boolean>())

    override fun visitFile(node: AstNode) {
        decorated.clear()
    }

    override fun visitNode(node: AstNode) = decorate(node)

    /** Decorates [node] once; a call visitor may need the member before the walker reaches it. */
    fun decorate(node: AstNode) {
        if (!decorated.add(node)) return
        val memberExpression = node as? SemanticAstNode ?: return
        if (memberExpression.symbol != null) return

        val parts = node.getChildren(PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.VARIABLE_NAME)
        if (parts.size != 2 || parts.first().type != PlSqlGrammar.VARIABLE_NAME ||
            parts.last().type != PlSqlGrammar.IDENTIFIER_NAME) return

        val memberName = OracleIdentifier.fromSource(parts.last().tokenOriginalValue)
        val baseSymbol = (parts.first() as? SemanticAstNode)?.symbol
        val projectType = if (baseSymbol != null) baseSymbol.projectTypeDeclaration ?: return
        else selfBase?.of(context, parts.first(), memberName) ?: return
        memberExpression.projectRecordMemberResolution = resolver.resolve(projectType, memberName)
    }
}
