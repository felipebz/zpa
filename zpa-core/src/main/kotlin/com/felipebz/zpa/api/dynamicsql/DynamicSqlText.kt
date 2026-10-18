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
package com.felipebz.zpa.api.dynamicsql

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.Token
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.symbols.SymbolTable
import java.util.Collections
import java.util.IdentityHashMap

/** A part of a dynamic statement whose text is not known statically, written into the built text as [placeholder]. */
@ZpaExperimentalApi
class DynamicSqlHole internal constructor(
    /** The identifier that stands for the hole in [DynamicSqlText.text], such as `ZPA$1`. */
    val placeholder: String,
    /** The expression of the scanned file that gives the hole its value. */
    val expression: AstNode
) {
    override fun toString() = "$placeholder <- ${expression.type}"
}

/**
 * The SQL text a dynamic statement builds (`EXECUTE IMMEDIATE`, `OPEN … FOR`, `DBMS_SQL.PARSE`), parsed as one statement.
 *
 * - Every token and trivia of [root] carries the line and column of its first character in the scanned file: a
 *   character of a literal is at its place inside that literal, a placeholder at the start of its hole's expression, and
 *   a character produced by `CHR(n)` at the start of that call.
 * - Token values are the built text. A literal nested in the built SQL, written `'',.''` in the file, has the value
 *   `',.'`; an end position computed from a value (such as `Token.endColumn`) therefore falls short by the number of
 *   escapes the file needed, and spans lines differently when the value does.
 * - The tree has its own [symbolTable]: Oracle compiles dynamic SQL on its own, so names in it never bind to the
 *   locals, parameters or private members of the code that runs it. Placeholders bind to nothing. Declarations inside
 *   the text never become project declarations.
 */
@ZpaExperimentalApi
class DynamicSqlText internal constructor(
    /** The `FILE_INPUT` root of the parsed text; it holds exactly one statement. */
    val root: AstNode,
    /** The expression the text was built from, when it was given as one expression. */
    val expression: AstNode?,
    /** The parts the text was built from, in order: the operands of the expression, or the parts given. */
    parts: List<AstNode>,
    /** The text that was parsed: literal values in order, holes as placeholders (or left out, see [holesDropped]). */
    val text: String,
    /** The holes, in order. When [holesDropped], their placeholders do not occur in [text]. */
    holes: List<DynamicSqlHole>,
    /**
     * True when the text did not parse with placeholders and was parsed with every hole left out. The dropped holes
     * may have held more of the statement (a condition, a list of columns…), so the tree may be partial.
     */
    val holesDropped: Boolean,
    /** The symbol table built for [root]. */
    val symbolTable: SymbolTable,
    private val holesByToken: Map<Token, List<DynamicSqlHole>>,
    private val exactByToken: Map<Token, DynamicSqlHole>
) {
    val parts: List<AstNode> = Collections.unmodifiableList(ArrayList(parts))
    val holes: List<DynamicSqlHole> = Collections.unmodifiableList(ArrayList(holes))

    /**
     * The hole [node] stands for when the node is a placeholder: its tokens are exactly one token whose text is a whole
     * placeholder (an `IDENTIFIER` token, or a name node wrapping it). Null otherwise.
     */
    fun holeOf(node: AstNode): DynamicSqlHole? {
        val token = node.tokenOrNull ?: return null
        if (node.hasChildren() && node.lastTokenOrNull !== token) return null
        return exactByToken[token]
    }

    fun isPlaceholder(node: AstNode): Boolean = holeOf(node) != null

    /**
     * Every hole whose placeholder is written in a token of [node], in order, including placeholders glued to other
     * characters (`tab_ZPA$1`, where the name is only partly known).
     */
    fun holesIn(node: AstNode): List<DynamicSqlHole> {
        val found = Collections.newSetFromMap(IdentityHashMap<DynamicSqlHole, Boolean>())
        node.tokens.forEach { token -> holesByToken[token]?.let(found::addAll) }
        return holes.filter { it in found }
    }

    override fun toString() = "DynamicSqlText(${if (holesDropped) "holes dropped, " else ""}$text)"
}
