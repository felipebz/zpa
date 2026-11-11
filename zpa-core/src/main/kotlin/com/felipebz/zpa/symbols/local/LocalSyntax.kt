//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
    /** An item after the first child of an `OBJECT_REFERENCE`, or the callee of one: a member of an expression receiver. */
    fun isReceiverItem(node: AstNode): Boolean {
        var current = node
        while (current.parentOrNull?.type == PlSqlGrammar.METHOD_CALL && current.parent.firstChild === current) current = current.parent
        val parent = current.parentOrNull ?: return false
        return parent.type == PlSqlGrammar.OBJECT_REFERENCE && parent.firstChild !== current
    }

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
            val select = if (expression.type === DmlGrammar.SELECT_EXPRESSION) expression else expression.getFirstChild(DmlGrammar.SELECT_EXPRESSION)
            return LocalLoopControl(LocalLoopControl.Form.QUERY, null, null, queryRow(select))
        }
        val name = (if (expression.type === PlSqlGrammar.METHOD_CALL) expression.firstChild else expression)
            .takeIf { it.type === PlSqlGrammar.VARIABLE_NAME }?.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME) ?: return null
        return LocalLoopControl(LocalLoopControl.Form.NAME, identifier(name), range(fileId, name))
    }

    /**
     * The row of the query [select]: [LocalQueryRow.OwnColumns] when its first query block selects no `*`;
     * [LocalQueryRow.AllOf] for a single query block `SELECT *` or `SELECT x.*` from one plain relation name (`x` its
     * alias or name); null otherwise.
     */
    fun queryRow(select: AstNode): LocalQueryRow? {
        val block = select.getFirstChildOrNull(DmlGrammar.QUERY_BLOCK) ?: return null
        val columns = block.getChildren(DmlGrammar.SELECT_COLUMN)
        fun star(column: AstNode) = column.hasDirectChildren(PlSqlPunctuator.MULTIPLICATION) ||
            column.firstChild.let { it.type === PlSqlGrammar.OBJECT_REFERENCE && it.lastChild.type === PlSqlPunctuator.MULTIPLICATION }
        if (columns.isNotEmpty() && columns.none(::star)) return LocalQueryRow.OwnColumns
        val column = columns.singleOrNull()?.takeIf(::star) ?: return null
        if (select.getChildren(DmlGrammar.QUERY_BLOCK).size != 1 || select.children.any { it.type !== DmlGrammar.QUERY_BLOCK && it.type !== DmlGrammar.ORDER_BY_CLAUSE }) return null
        val sources =
            block.getFirstChildOrNull(DmlGrammar.FROM_CLAUSE)?.getChildren(DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE)
                .orEmpty()
        val source = sources.singleOrNull() ?: return null
        val table =
            source.firstChild.takeIf { it.type === DmlGrammar.TABLE_REFERENCE && source.children.all { c -> c === it || c.type === DmlGrammar.ALIAS } }
                ?: return null
        if (table.children.any { it.type !== PlSqlGrammar.IDENTIFIER_NAME && it.type !== PlSqlPunctuator.DOT }) return null
        val name = QualifiedName(table.getChildren(PlSqlGrammar.IDENTIFIER_NAME).map(::identifier))
        if (!column.hasDirectChildren(PlSqlPunctuator.MULTIPLICATION)) {
            val prefix = column.firstChild.firstChild.takeIf { it.type === PlSqlGrammar.VARIABLE_NAME }
                ?.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME) ?: return null
            val alias = source.getFirstChildOrNull(DmlGrammar.ALIAS)?.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)
            if (identifier(prefix) != (alias?.let(::identifier) ?: name.last)) return null
        }
        return LocalQueryRow.AllOf(name)
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
        var index = tokens.indexOfFirst { OracleIdentifier.fromToken(it.originalValue, it.value) == segments.first() }
        if (index < 0) return emptyList()
        val result = ArrayList<SourceRange>(segments.size)
        for ((position, segment) in segments.withIndex()) {
            if (position > 0) {
                if (index + 1 >= tokens.size || tokens[index].value != ".") return emptyList()
                index++
            }
            val token = tokens[index++]
            if (OracleIdentifier.fromToken(token.originalValue, token.value) != segment) return emptyList()
            result += SourceRange(fileId, token.line, token.column, token.endLine, token.endColumn)
        }
        return result
    }

    /**
     * The descendants of [root] (not [root] itself) whose type is one of [types], in document order: what
     * `AstNode.getDescendants` returns, without the arrays it copies at every node.
     */
    fun descendants(root: AstNode, vararg types: AstNodeType): List<AstNode> {
        val result = ArrayList<AstNode>()
        val children = root.children
        for (i in 0 until children.size) collect(children[i], types, result)
        return result
    }

    private fun collect(node: AstNode, types: Array<out AstNodeType>, result: MutableList<AstNode>) {
        for (type in types) if (node.type === type) result += node
        val children = node.children
        for (i in 0 until children.size) collect(children[i], types, result)
    }

    fun identifier(node: AstNode): OracleIdentifier = OracleIdentifier.fromToken(node.token.originalValue, node.token.value)

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
