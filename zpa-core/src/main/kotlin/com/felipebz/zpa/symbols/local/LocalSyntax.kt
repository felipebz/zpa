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
package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.flr.api.GenericTokenType
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.QualifiedName
import com.felipebz.zpa.project.SourceRange

/** Source facts shared by the local extraction code: identifiers and ranges of AST nodes, never the nodes. */
internal object LocalSyntax {
    fun identifier(node: AstNode): OracleIdentifier = OracleIdentifier.fromSource(node.token.originalValue)

    fun range(fileId: FileId, node: AstNode): SourceRange =
        SourceRange(fileId, node.token.line, node.token.column, node.lastToken.endLine, node.lastToken.endColumn)

    private val DEFAULT_CORRELATION_NAMES = LocalCorrelationNames(
        OracleIdentifier.fromSource("NEW"), OracleIdentifier.fromSource("OLD"), OracleIdentifier.fromSource("PARENT")
    )

    fun unitName(unitNameNode: AstNode) =
        QualifiedName(unitNameNode.getChildren(PlSqlGrammar.IDENTIFIER_NAME).map(LocalSyntax::identifier))

    fun correlationNames(referencing: AstNode?): LocalCorrelationNames {
        if (referencing == null) return DEFAULT_CORRELATION_NAMES
        var new = DEFAULT_CORRELATION_NAMES.new
        var old = DEFAULT_CORRELATION_NAMES.old
        var parent = DEFAULT_CORRELATION_NAMES.parent
        var pending: AstNodeType? = null
        for (child in referencing.children) {
            when {
                child.type === PlSqlKeyword.NEW || child.type === PlSqlKeyword.OLD || child.type === PlSqlKeyword.PARENT -> pending = child.type
                child.type === PlSqlGrammar.IDENTIFIER_NAME -> {
                    val alias = identifier(child)
                    when (pending) {
                        PlSqlKeyword.NEW -> new = alias
                        PlSqlKeyword.OLD -> old = alias
                        PlSqlKeyword.PARENT -> parent = alias
                        else -> Unit
                    }
                    pending = null
                }
            }
        }
        return LocalCorrelationNames(new, old, parent)
    }

    /** The method kind written around an object type member: its wrapper rule and keywords. */
    fun methodKind(node: AstNode): LocalMethodKind? {
        if (node.type === PlSqlGrammar.TYPE_CONSTRUCTOR) return LocalMethodKind.CONSTRUCTOR
        val wrapper = node.parent
        return when (wrapper.type) {
            PlSqlGrammar.MAP_ORDER_FUNCTION -> if (wrapper.hasDirectChildren(PlSqlKeyword.MAP)) LocalMethodKind.MAP else LocalMethodKind.ORDER
            PlSqlGrammar.TYPE_SUBPROGRAM -> if (wrapper.hasDirectChildren(PlSqlKeyword.STATIC)) LocalMethodKind.STATIC else LocalMethodKind.MEMBER
            else -> null
        }
    }

    fun notNull(node: AstNode?) =
        node?.getFirstChildOrNull(PlSqlGrammar.DATATYPE_NULL_CONSTRAINT)?.hasDirectChildren(PlSqlKeyword.NOT) == true

    fun fileRange(fileId: FileId, root: AstNode): SourceRange {
        val content = root.children.filter { it.type !== GenericTokenType.EOF && it.hasToken() }
        if (content.isEmpty()) return SourceRange(fileId, 1, 0, 1, 0)
        val first = content.first().token
        val last = content.last().lastToken
        return SourceRange(fileId, first.line, first.column, last.endLine, last.endColumn)
    }
}
