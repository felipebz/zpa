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
package com.felipebz.zpa.dynamicsql

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.api.Token
import com.felipebz.flr.api.Trivia
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.dynamicsql.DynamicSqlHole
import com.felipebz.zpa.api.dynamicsql.DynamicSqlText
import com.felipebz.zpa.lexer.PlSqlLexer
import com.felipebz.zpa.lexer.StringLiteralSyntax
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.squid.PlSqlAstWalker
import com.felipebz.zpa.squid.PlSqlConfiguration
import com.felipebz.zpa.squid.SemanticAnalysisPipeline
import com.felipebz.zpa.squid.SemanticScanCapabilities
import com.felipebz.zpa.squid.createNotPreparedProjectAnalysis
import java.nio.charset.StandardCharsets
import java.util.IdentityHashMap

/** A lexer and a parser for built texts, with the configuration of the scanned files; created on first use. */
internal class DynamicSqlTools(configuration: PlSqlConfiguration) {
    val lexer by lazy { PlSqlLexer.create(configuration) }
    val parser by lazy { PlSqlParser.create(configuration) }
}

/**
 * Builds and parses the SQL text of dynamic statements for one scanned file. Nothing is done until a text is asked
 * for, and each expression (or list of parts) is built at most once: results, including null, are cached.
 *
 * The text is lexed as a file of its own with the file's configuration; each token's line and column is then rewritten
 * from a map of the text's characters to their place in the scanned file, and the rewritten tokens are parsed. The
 * tree is decorated by a fresh run of the semantic pipeline (symbols, then the project resolution visitors) under a
 * file id of its own, so no declaration of the text can match a project declaration of the scanned file.
 */
@OptIn(ZpaExperimentalApi::class)
internal class DynamicSqlSupport(
    /** The file's lexer and parser; created lazily and shared by the scans of one scanner. */
    private val tools: DynamicSqlTools,
    private val pipeline: SemanticAnalysisPipeline,
    private val fileId: FileId,
    private val capabilities: SemanticScanCapabilities
) {
    private val lexer get() = tools.lexer
    private val parser get() = tools.parser
    private val byExpression = IdentityHashMap<AstNode, Cached>()
    private val byParts = HashMap<PartsKey, Cached>()

    private class Cached(val text: DynamicSqlText?)

    /** Identity of a list of part nodes. */
    private class PartsKey(val parts: List<AstNode>) {
        override fun equals(other: Any?) =
            other is PartsKey && other.parts.size == parts.size && parts.indices.all { parts[it] === other.parts[it] }

        override fun hashCode() = parts.fold(1) { hash, node -> 31 * hash + System.identityHashCode(node) }
    }

    fun fromExpression(expression: AstNode): DynamicSqlText? =
        byExpression.getOrPut(expression) { Cached(build(expression, operands(expression))) }.text

    fun fromParts(parts: List<AstNode>): DynamicSqlText? {
        val copy = parts.toList()
        return byParts.getOrPut(PartsKey(copy)) { Cached(build(null, copy)) }.text
    }

    // Parts.

    private sealed interface Piece {
        val node: AstNode
    }

    /** Known text; [positions] holds the file position of each character. */
    private class Known(override val node: AstNode, val value: String, val positions: LongArray) : Piece

    private class Unknown(override val node: AstNode) : Piece {
        val position = pack(node.token.line, node.token.column)
        lateinit var hole: DynamicSqlHole
    }

    /** The operands of a concatenation, with parenthesized expressions unwrapped and nested concatenations flattened. */
    private fun operands(node: AstNode): List<AstNode> = when {
        node.type === PlSqlGrammar.CONCATENATION_EXPRESSION ->
            node.children.filter { it.type !== PlSqlGrammar.CONCATENATION_OPERATOR }.flatMap(::operands)
        node.type === PlSqlGrammar.BRACKED_EXPRESSION && node.children.size == 3 -> operands(node.children[1])
        else -> listOf(node)
    }

    private fun piece(node: AstNode): Piece = literal(node) ?: chr(node) ?: Unknown(node)

    private fun literalToken(node: AstNode): Token? {
        val literal = when {
            node.type === PlSqlGrammar.CHARACTER_LITERAL -> node
            node.type === PlSqlGrammar.LITERAL && node.children.size == 1 && node.firstChild.type === PlSqlGrammar.CHARACTER_LITERAL -> node.firstChild
            else -> return null
        }
        return literal.token.takeIf { it.type === PlSqlTokenType.STRING_LITERAL }
    }

    private fun literal(node: AstNode): Known? {
        val token = literalToken(node) ?: return null
        val decoded = StringLiteralSyntax.decode(token.originalValue) ?: return null
        val inLiteral = positionsOf(token)
        return Known(node, decoded.value, LongArray(decoded.value.length) { inLiteral[decoded.offsets[it]] })
    }

    /** `CHR(n)` with an integer literal: that character, at the start of the call. */
    private fun chr(node: AstNode): Known? {
        if (node.type !== PlSqlGrammar.METHOD_CALL || node.children.size != 2) return null
        val (name, arguments) = node.children
        if (name.type !== PlSqlGrammar.VARIABLE_NAME || name.tokens.size != 1 || !name.tokenValue.equals("CHR", ignoreCase = true)) return null
        if (arguments.type !== PlSqlGrammar.ARGUMENTS) return null
        val argument = arguments.getChildren(PlSqlGrammar.ARGUMENT).singleOrNull() ?: return null
        val tokens = argument.tokens
        if (tokens.size != 1 || tokens[0].type !== PlSqlTokenType.INTEGER_LITERAL) return null
        val code = tokens[0].originalValue.toIntOrNull() ?: return null
        if (code !in 0..0xFFFF || code.toChar().isSurrogate()) return null
        return Known(node, code.toChar().toString(), longArrayOf(pack(node.token.line, node.token.column)))
    }

    /** The file position of every character of a token's original text (lines break like the lexer's). */
    private fun positionsOf(token: Token): LongArray {
        val text = token.originalValue
        val result = LongArray(text.length)
        var line = token.line
        var column = token.column
        for (index in text.indices) {
            result[index] = pack(line, column)
            val c = text[index]
            if (c == '\n' || c == '\r' && text.getOrNull(index + 1) != '\n') {
                line++
                column = 0
            } else {
                column++
            }
        }
        return result
    }

    // Text.

    private fun build(expression: AstNode?, parts: List<AstNode>): DynamicSqlText? {
        if (parts.isEmpty()) return null
        val pieces = parts.map(::piece)
        if (pieces.none { it is Known && literalToken(it.node) != null }) return null
        val known = pieces.filterIsInstance<Known>().joinToString("") { it.value }.uppercase()
        // The placeholder prefix never occurs in the known text, so no name written there can equal or contain it.
        var prefix = PLACEHOLDER_PREFIX
        while (known.contains(prefix)) prefix += "$"
        pieces.filterIsInstance<Unknown>().forEachIndexed { index, unknown -> unknown.hole = DynamicSqlHole("$prefix${index + 1}", unknown.node) }
        val holes = pieces.filterIsInstance<Unknown>().map { it.hole }
        val first = pieces.first().node.token
        val treeFileId = FileId("${fileId.value}#dynamic@${first.line}:${first.column}")
        val parsed = attempt(expression, parts, pieces, holes, dropHoles = false, treeFileId)
            ?: if (holes.isNotEmpty()) attempt(expression, parts, pieces, holes, dropHoles = true, treeFileId) else null
        return parsed
    }

    private class Built(val text: String, val positions: LongArray, val placeholders: List<Triple<Int, Int, DynamicSqlHole>>)

    private fun assemble(pieces: List<Piece>, dropHoles: Boolean): Built {
        val text = StringBuilder()
        val positions = ArrayList<Long>()
        val placeholders = ArrayList<Triple<Int, Int, DynamicSqlHole>>()
        var inString = false
        var afterPlaceholder: Long? = null
        fun append(c: Char, position: Long) {
            text.append(c)
            positions += position
            if (c == '\'') inString = !inString
        }
        for (piece in pieces) {
            when (piece) {
                is Known -> piece.value.forEachIndexed { index, c ->
                    val padding = afterPlaceholder
                    if (index == 0 && padding != null) {
                        if (!touches(c) && !c.isWhitespace()) append(' ', padding)
                        afterPlaceholder = null
                    }
                    append(c, piece.positions[index])
                }
                is Unknown -> {
                    if (dropHoles) continue
                    val last = text.lastOrNull()
                    // Pad unless the placeholder is inside a string of the built SQL or glued to a name or a dot.
                    if (!inString && last != null && (afterPlaceholder != null || !touches(last) && !last.isWhitespace())) append(' ', piece.position)
                    val start = text.length
                    piece.hole.placeholder.forEach { append(it, piece.position) }
                    placeholders += Triple(start, text.length, piece.hole)
                    afterPlaceholder = if (inString) null else piece.position
                }
            }
        }
        return Built(text.toString(), positions.toLongArray(), placeholders)
    }

    /** A placeholder glues to a name character or a dot; next to anything else but a blank it gets a blank. */
    private fun touches(c: Char) = c == '.' || c == '_' || c == '$' || c == '#' || c.isLetterOrDigit()

    // Parse.

    private fun attempt(
        expression: AstNode?,
        parts: List<AstNode>,
        pieces: List<Piece>,
        holes: List<DynamicSqlHole>,
        dropHoles: Boolean,
        treeFileId: FileId
    ): DynamicSqlText? {
        val built = assemble(pieces, dropHoles)
        if (built.text.isBlank()) return null
        val lexed = try {
            lexer.lex(built.text)
        } catch (e: Exception) {
            return null
        }
        val lineStarts = lineStarts(built.text)
        val holesByToken = IdentityHashMap<Token, List<DynamicSqlHole>>()
        val exactByToken = IdentityHashMap<Token, DynamicSqlHole>()
        val tokens = lexed.map { token ->
            val offset = offset(token, lineStarts)
            val mapped = relocate(token, lineStarts, built)
            val end = offset + token.originalValue.length
            val inside = built.placeholders.filter { (start, stop) -> start < end && offset < stop }
            if (inside.isNotEmpty()) holesByToken[mapped] = inside.map { it.third }
            inside.singleOrNull { (start, stop) -> start == offset && stop == end }?.let { exactByToken[mapped] = it.third }
            mapped
        }
        val root = try {
            parser.parse(tokens)
        } catch (e: Exception) {
            return null
        }
        if (root.children.count { it.type !== GenericTokenType.EOF } != 1 || root.hasDescendant(PlSqlGrammar.RECOVERY)) return null

        val context = PlSqlVisitorContext(root, null as PlSqlFile?, null)
        PlSqlAstWalker(pipeline.create(treeFileId).all).walk(context, capabilities)
        return DynamicSqlText(
            root, expression, parts, built.text, holes, dropHoles, context.symbolTable,
            holesByToken, exactByToken
        )
    }

    /** Offsets where each line of [text] starts, breaking lines like the lexer (LF, or CR not followed by LF). */
    private fun lineStarts(text: String): IntArray {
        val starts = arrayListOf(0)
        for (index in text.indices) {
            val c = text[index]
            if (c == '\n' || c == '\r' && text.getOrNull(index + 1) != '\n') starts += index + 1
        }
        return starts.toIntArray()
    }

    private fun offset(token: Token, lineStarts: IntArray): Int = lineStarts[token.line - 1] + token.column

    /** The token, with its trivia, at the file position of its first character. */
    private fun relocate(token: Token, lineStarts: IntArray, built: Built): Token {
        val position = built.positions.getOrElse(offset(token, lineStarts)) { built.positions.last() }
        val trivia = token.trivia.map { trivia ->
            val pieces = trivia.tokens.map { relocate(it, lineStarts, built) }
            if (trivia.isComment) Trivia.createComment(pieces.single()) else Trivia.createSkippedText(pieces)
        }
        return Token.builder(token).setLine(line(position)).setColumn(column(position)).setTrivia(trivia).build()
    }

    companion object {
        /** Placeholder spelling: `ZPA$` and the hole's number, extended with `$` while the known text contains it. */
        const val PLACEHOLDER_PREFIX = "ZPA$"

        private fun pack(line: Int, column: Int) = (line.toLong() shl 32) or column.toLong()
        private fun line(position: Long) = (position ushr 32).toInt()
        private fun column(position: Long) = position.toInt()

        /** Support for a context not created by the scanner: default configuration, no project. */
        fun standalone(file: PlSqlFile?): DynamicSqlSupport = DynamicSqlSupport(
            DynamicSqlTools(PlSqlConfiguration(StandardCharsets.UTF_8, true)),
            SemanticAnalysisPipeline(ProjectAnalysisContext.NOT_PREPARED),
            FileId(file?.path()?.toString() ?: "dynamic.sql"),
            SemanticScanCapabilities(createNotPreparedProjectAnalysis())
        )
    }
}
