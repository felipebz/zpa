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
import com.felipebz.flr.api.Token
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.QualifiedName
import com.felipebz.zpa.project.SourceRange
import com.felipebz.zpa.project.extraction.TokenStream
import com.felipebz.zpa.project.extraction.TypeReferences

/** Source facts shared by the local extraction code: identifiers and ranges of AST nodes, never the nodes. */
internal object LocalSyntax {
    /** The iteration control of a FOR loop [iterator] with one implicitly typed index and one control, if recognized. */
    fun loopControl(iterator: AstNode, fileId: FileId): LocalLoopControl? {
        val controls = iterator.getChildren(PlSqlGrammar.QUAL_ITERATION_CTL)
        if (iterator.getChildren(PlSqlGrammar.ITERAND_DECLARATION).size != 1 || controls.size != 1) return null
        val control = controls.single()
        control.getFirstChildOrNull(PlSqlGrammar.STEPPED_CONTROL)?.let {
            return if (it.hasDirectChildren(PlSqlKeyword.BY)) null else LocalLoopControl(LocalLoopControl.Form.STEPPED, null, null)
        }
        val expression = control.getFirstChildOrNull(PlSqlGrammar.SINGLE_EXPRESSION_CONTROL)?.lastChild ?: return null
        val query = expression.type === DmlGrammar.SELECT_EXPRESSION || expression.hasDirectChildren(DmlGrammar.SELECT_EXPRESSION) &&
            expression.children.all { it.type === DmlGrammar.SELECT_EXPRESSION || it.type === PlSqlPunctuator.LPARENTHESIS || it.type === PlSqlPunctuator.RPARENTHESIS }
        if (query) {
            return LocalLoopControl(LocalLoopControl.Form.QUERY, null, null)
        }
        val name = (if (expression.type === PlSqlGrammar.METHOD_CALL) expression.firstChild else expression)
            .takeIf { it.type === PlSqlGrammar.VARIABLE_NAME }?.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME) ?: return null
        return LocalLoopControl(LocalLoopControl.Form.NAME, identifier(name), range(fileId, name))
    }

    /** The written datatype [datatype], looked up from [lookupRegion]. */
    fun typeUse(fileId: FileId, datatype: AstNode, lookupRegion: LocalRegionId): LocalTypeUse {
        val tokens = datatype.tokens
        val typeRef = TypeReferences(TokenStream(fileId, tokens)).declaredType(0, tokens.size)
        // The written type's range is the TypeRef's own range: share the instance instead of holding an equal copy.
        val range = range(fileId, datatype).let { if (typeRef?.sourceRange == it) typeRef.sourceRange else it }
        return LocalTypeUse(typeRef, range, lookupRegion, typeRef?.let { segmentRanges(fileId, tokens, it.name.segments) }.orEmpty())
    }

    /**
     * The token of each segment of a written type's name: its qualified name `name(.name)*`, starting at the first token
     * that spells the first segment (after `REF`, for example). Empty when the tokens do not spell it so.
     */
    private fun segmentRanges(fileId: FileId, tokens: List<Token>, segments: List<OracleIdentifier>): List<SourceRange> {
        var index = tokens.indexOfFirst { OracleIdentifier.fromSource(it.originalValue) == segments.first() }
        if (index < 0) return emptyList()
        val result = ArrayList<SourceRange>(segments.size)
        for ((position, segment) in segments.withIndex()) {
            if (position > 0) {
                if (index + 1 >= tokens.size || tokens[index].value != ".") return emptyList()
                index++
            }
            val token = tokens[index++]
            if (OracleIdentifier.fromSource(token.originalValue) != segment) return emptyList()
            result += SourceRange(fileId, token.line, token.column, token.endLine, token.endColumn)
        }
        return result
    }

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
