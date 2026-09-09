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
import com.felipebz.zpa.api.PlSqlPunctuator.DOT
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.ProjectRecordMemberPathResolver

/** Resolves project RECORD member paths from a semantic base symbol. */
internal class ProjectRecordMemberPathResolutionVisitor(
    private val resolver: ProjectRecordMemberPathResolver
) : PlSqlCheck() {

    init {
        subscribeTo(PlSqlGrammar.MEMBER_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        val memberExpression = node as? SemanticAstNode ?: return
        if (memberExpression.symbol != null || node.parent.type == PlSqlGrammar.METHOD_CALL) return

        val children = node.children
        if (children.size < 5 || children.size % 2 == 0 ||
            children.first().type != PlSqlGrammar.VARIABLE_NAME) return
        for (index in 1 until children.size) {
            val expectedType = if (index % 2 == 1) DOT else PlSqlGrammar.IDENTIFIER_NAME
            if (children[index].type != expectedType) return
        }

        val baseSymbol = (children.first() as? SemanticAstNode)?.symbol ?: return
        if (baseSymbol.kind !in setOf(Symbol.Kind.VARIABLE, Symbol.Kind.PARAMETER, Symbol.Kind.CURSOR)) return
        val baseType = baseSymbol.projectTypeDeclaration ?: return
        val memberNames = (2 until children.size step 2)
            .map { OracleIdentifier.fromSource(children[it].tokenOriginalValue) }
        memberExpression.projectRecordMemberPathResolution = resolver.resolve(baseType, memberNames)
    }
}
