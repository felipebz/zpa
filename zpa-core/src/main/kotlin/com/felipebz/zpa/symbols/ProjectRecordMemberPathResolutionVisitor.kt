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
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.ProjectRecordMemberPathResolution
import com.felipebz.zpa.project.ProjectRecordMemberPathResolver
import com.felipebz.zpa.symbols.local.LocalDeclarationKind

/** Resolves project RECORD member paths from the declared project type of a local base. */
internal class ProjectRecordMemberPathResolutionVisitor(
    private val resolver: ProjectRecordMemberPathResolver,
    private val selfBase: ProjectSelfMemberBase? = null
) : PlSqlCheck() {

    init {
        subscribeTo(PlSqlGrammar.MEMBER_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        if (node.parent.type == PlSqlGrammar.METHOD_CALL) return
        pathOf(node)?.let { (node as SemanticAstNode).projectRecordMemberPathResolution = it }
    }

    /** The path a `base.member.member…` expression names from its base, without decorating it; null for another shape. */
    fun pathOf(node: AstNode): ProjectRecordMemberPathResolution? {
        val facts = context.localSemanticFacts ?: return null
        if (node.hasDirectChildren(PlSqlPunctuator.MOD) || ProjectReceiverMemberVisitor.isReceiverItem(node) ||
            facts.qualifiedDeclaration(node.token.line, node.token.column) != null) return null

        val children = node.children
        if (!ProjectReceiverMemberVisitor.isNameChain(children, 5)) return null

        val memberNames = (2 until children.size step 2)
            .map { OracleIdentifier.fromSource(children[it].tokenOriginalValue) }
        val base = children.first()
        val baseDeclaration = facts.nameDeclaration(base.token.line, base.token.column)
        val baseType = if (baseDeclaration != null) {
            if (baseDeclaration.kind !in VALUE_KINDS) return null
            facts.declaredType(baseDeclaration) ?: return null
        } else {
            selfBase?.of(context, children.first(), memberNames.first()) ?: return null
        }
        return resolver.resolve(baseType, memberNames)
    }

    private companion object {
        val VALUE_KINDS = setOf(
            LocalDeclarationKind.VARIABLE, LocalDeclarationKind.PARAMETER, LocalDeclarationKind.CURSOR,
            LocalDeclarationKind.CURSOR_PARAMETER, LocalDeclarationKind.LOOP_INDEX
        )
    }
}
