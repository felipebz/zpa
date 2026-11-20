//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.project.ProjectExternalDatatype
import com.felipebz.zpa.api.project.ProjectExternalTypeDescription
import com.felipebz.zpa.api.semantics.*

@Rule(priority = Priority.MAJOR, tags = [Tags.BUG], category = RuleCategory.LOGIC_ERROR)
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class ColumnIntoSmallerVariableCheck : AbstractBaseCheck() {

    private enum class Unit(val key: String) { CHARACTERS("charactersMessage"), BYTES("bytesMessage"), DIGITS("digitsMessage") }

    /** A size: a length, or the digits before the point for a number. [raw] tells `RAW` apart from text. */
    private class Size(val value: Int, val unit: Unit, val raw: Boolean = false) {
        val numeric get() = unit == Unit.DIGITS
    }

    override fun init() {
        subscribeTo(DmlGrammar.INTO_CLAUSE)
    }

    /** The declarations of the file by the position of their name, built the first time a target or cursor needs one. */
    private var declarations: Map<Long, AstNode>? = null

    override fun visitFile(node: AstNode) {
        declarations = null
    }

    override fun visitNode(node: AstNode) {
        if (node.hasDirectChildren(PlSqlKeyword.BULK)) return
        val items = items(node) ?: return
        val targets = node.children.filter { it.type !is PlSqlKeyword && it.tokenValue != "," }
        if (items.size != targets.size) return
        for ((item, target) in items.zip(targets)) check(item, target)
    }

    /** The values the targets of [into] receive, in order; null when they cannot be paired by position. */
    private fun items(into: AstNode): List<AstNode?>? {
        val parent = into.parent
        return when (parent.type) {
            DmlGrammar.QUERY_BLOCK -> selectItems(parent)
            DmlGrammar.RETURNING_INTO_CLAUSE -> parent.children.filter { it.type !is PlSqlKeyword && it.type !== into.type && it.tokenValue != "," }
            PlSqlGrammar.FETCH_STATEMENT -> cursorQuery(parent.getFirstChild(PlSqlGrammar.MEMBER_EXPRESSION, PlSqlGrammar.VARIABLE_NAME))
            else -> null
        }
    }

    private fun selectItems(block: AstNode): List<AstNode?>? = block.getChildren(DmlGrammar.SELECT_COLUMN).map { column ->
        if (column.tokens.lastOrNull()?.value == "*") return null
        column.firstChild
    }

    /** The select items of the query of the explicit cursor [name] declares in this file, when it is one query block. */
    private fun cursorQuery(name: AstNode): List<AstNode?>? {
        val declaration = (context.nameBinding(name) as? NameBinding.Local)?.declaration ?: return null
        if (declaration.kind != NameDeclarationKind.CURSOR || declaration.fileId != null) return null
        val cursor = declarationNode(name, declaration)?.takeIf { it.type === PlSqlGrammar.CURSOR_DECLARATION } ?: return null
        val query = cursor.getFirstChildOrNull(DmlGrammar.SELECT_EXPRESSION) ?: return null
        val block = query.getChildren(DmlGrammar.QUERY_BLOCK).singleOrNull()?.takeIf { query.children.none { it.type === PlSqlKeyword.UNION } } ?: return null
        return selectItems(block)
    }

    private fun check(item: AstNode?, target: AstNode) {
        if (item == null || (item.type !== PlSqlGrammar.VARIABLE_NAME && item.type !== PlSqlGrammar.MEMBER_EXPRESSION)) return
        val binding = context.columnBinding(item) as? ColumnBinding.Column ?: return
        if (binding.exists != true) return
        val column = binding.column?.datatype?.orElse(null)?.let(::columnSize) ?: return
        val name = target.takeIf { it.type === PlSqlGrammar.VARIABLE_NAME } ?: return
        val own = targetSize(name) ?: return
        if (own.numeric != column.numeric || own.raw != column.raw || own.value >= column.value) return
        addIssue(target, getLocalizedMessage(column.unit.key), binding.name, column.value, name.tokenValue, own.value)
    }

    /** The declared size of the scalar variable, constant or parameter [name] reads. */
    private fun targetSize(name: AstNode): Size? {
        val declaration = (context.nameBinding(name) as? NameBinding.Local)?.declaration ?: return null
        if (declaration.fileId != null) return null
        if (declaration.kind != NameDeclarationKind.VARIABLE && declaration.kind != NameDeclarationKind.PARAMETER) return null
        val node = declarationNode(name, declaration)
            ?.takeIf { it.type === PlSqlGrammar.VARIABLE_DECLARATION || it.type === PlSqlGrammar.PARAMETER_DECLARATION } ?: return null
        val datatype = node.getFirstChildOrNull(PlSqlGrammar.DATATYPE) ?: return null
        return when (val type = context.declaredType(datatype)) {
            is DeclaredType.BuiltIn -> builtInSize(type.name, type.constraints)
            is DeclaredType.Anchored -> type.description.takeIf { it.status == ProjectExternalTypeDescription.Status.RESOLVED }
                ?.lookup?.flatMap { it.column }?.flatMap { it.datatype }?.orElse(null)?.let(::columnSize)
            else -> null
        }
    }

    /** The variable, parameter or cursor declaration whose name is where [declaration] places it. */
    private fun declarationNode(from: AstNode, declaration: NameDeclaration): AstNode? {
        val byPosition = declarations ?: HashMap<Long, AstNode>().also { map ->
            val root = generateSequence(from) { it.parentOrNull }.last()
            for (node in root.getDescendants(PlSqlGrammar.VARIABLE_DECLARATION, PlSqlGrammar.PARAMETER_DECLARATION, PlSqlGrammar.CURSOR_DECLARATION)) {
                val token = node.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)?.token ?: continue
                map.putIfAbsent(position(token.line, token.column), node)
            }
            declarations = map
        }
        return byPosition[position(declaration.line, declaration.column)]
    }

    private fun position(line: Int, column: Int) = line.toLong() shl 32 or column.toLong()

    private fun builtInSize(name: String, constraints: WrittenTypeConstraints): Size? {
        val length = (constraints.length as? WrittenTypeConstraint.Value)?.value
        return when (name) {
            "VARCHAR2", "VARCHAR" -> length?.let { Size(it, if (constraints.lengthSemantics == LengthSemantics.BYTE) Unit.BYTES else Unit.CHARACTERS) }
            "CHAR", "CHARACTER" -> if (constraints.length == null) Size(1, Unit.CHARACTERS) else length?.let { Size(it, Unit.CHARACTERS) }
            "NCHAR" -> if (constraints.length == null) Size(1, Unit.CHARACTERS) else length?.let { Size(it, Unit.CHARACTERS) }
            "NVARCHAR2" -> length?.let { Size(it, Unit.CHARACTERS) }
            "RAW" -> length?.let { Size(it, Unit.BYTES, raw = true) }
            "NUMBER", "NUMERIC", "DECIMAL", "DEC" -> numberSize(name, constraints)
            "INTEGER", "INT", "SMALLINT" -> Size(38, Unit.DIGITS)
            else -> null
        }
    }

    /** Digits before the point: precision minus scale; `*` is 38; NUMERIC, DECIMAL and DEC alone are 38. */
    private fun numberSize(name: String, constraints: WrittenTypeConstraints): Size? {
        val scale = when (val written = constraints.scale) {
            null -> 0
            is WrittenTypeConstraint.Value -> written.value
            else -> return null
        }
        val precision = when (val written = constraints.precision) {
            null -> if (name == "NUMBER") return null else 38
            is WrittenTypeConstraint.Value -> written.value
            WrittenTypeConstraint.Maximum -> 38
            WrittenTypeConstraint.Unknown -> return null
        }
        return Size((precision - scale).coerceAtLeast(0), Unit.DIGITS)
    }

    /** The size the dictionary gives a column of [datatype]; null without a length or precision. */
    private fun columnSize(datatype: ProjectExternalDatatype): Size? {
        if (datatype.typeOwner.isPresent) return null
        val length = datatype.length.takeIf { it.isPresent }?.asInt
        val chars = datatype.lengthSemantics.orElse(null) == com.felipebz.zpa.api.project.ProjectExternalDatatype.LengthSemantics.CHAR
        return when (datatype.name.uppercase()) {
            "VARCHAR2", "VARCHAR", "CHAR" -> length?.let { Size(it, if (chars) Unit.CHARACTERS else Unit.BYTES) }
            "NVARCHAR2", "NCHAR" -> length?.let { Size(it, Unit.CHARACTERS) }
            "RAW" -> length?.let { Size(it, Unit.BYTES, raw = true) }
            "NUMBER" -> datatype.precision.takeIf { it.isPresent }?.asInt?.let { precision ->
                Size((precision - datatype.scale.orElse(0)).coerceAtLeast(0), Unit.DIGITS)
            }
            else -> null
        }
    }
}
