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
package com.felipebz.zpa.project.extraction

import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator

/** Maximum parenthesis/CASE nesting examined in a package value initializer; deeper is not admitted. */
private const val MAX_INITIALIZER_NESTING = 64

private val INITIALIZER_NEVER_STARTS = setOf(
    PlSqlPunctuator.COMMA, PlSqlPunctuator.RPARENTHESIS, PlSqlPunctuator.RBRACKET, PlSqlPunctuator.MULTIPLICATION,
    PlSqlPunctuator.DIVISION, PlSqlPunctuator.EQUALS, PlSqlPunctuator.GREATERTHAN, PlSqlPunctuator.SINGLE_PIPE,
    PlSqlPunctuator.DOT, PlSqlPunctuator.MOD, PlSqlPunctuator.ASSIGNMENT, PlSqlPunctuator.ASSOCIATION,
    PlSqlPunctuator.SEMICOLON, PlSqlPunctuator.EXPONENTIATION
)

private val INITIALIZER_NEVER_STARTING_WORDS = setOf(
    "AND", "OR", "IN", "IS", "LIKE", "BETWEEN", "THEN", "WHEN", "ELSE", "END", "FROM", "AS", "DEFAULT", "OF"
)

private val INITIALIZER_NEVER_ENDS = setOf(
    PlSqlPunctuator.COMMA, PlSqlPunctuator.PLUS, PlSqlPunctuator.MINUS, PlSqlPunctuator.SUBTRACTION,
    PlSqlPunctuator.MOD, PlSqlPunctuator.DOT, PlSqlPunctuator.DIVISION, PlSqlPunctuator.LPARENTHESIS,
    PlSqlPunctuator.COLON, PlSqlPunctuator.MULTIPLICATION, PlSqlPunctuator.EQUALS, PlSqlPunctuator.LESSTHAN,
    PlSqlPunctuator.GREATERTHAN, PlSqlPunctuator.SINGLE_PIPE, PlSqlPunctuator.EXPONENTIATION,
    PlSqlPunctuator.ASSIGNMENT, PlSqlPunctuator.ASSOCIATION, PlSqlPunctuator.LBRACKET, PlSqlPunctuator.REMOTE
)

private val INITIALIZER_NEVER_ENDING_WORDS = setOf(
    "AND", "OR", "NOT", "IN", "IS", "LIKE", "BETWEEN", "THEN", "WHEN", "ELSE", "FROM", "AS", "OF", "ESCAPE"
)

/** Non-reserved words that are infix operators: the token after them is at an operand position. */
private val OPERATOR_WORDS = setOf("MULTISET", "MEMBER", "SUBMULTISET", "ESCAPE", "AT", "LIKEC", "LIKE2", "LIKE4", "EXCEPT", "MINUS")

/** Conservative structural admission of package value initializers; not an expression parser. */
internal class InitializerShape(private val stream: TokenStream) {
    private class InitializerScan(val limit: Int) {
        /** `CASE` index to the index after its `END`, or -1 (also while being parsed). */
        val cases = HashMap<Int, Int>()
    }

    /**
     * Conservative admission rule for an initializer in `[start, endExclusive)`; the expression is not
     * parsed and its grammatical validity is not established. Admitted iff: it is nonempty; it neither
     * starts nor ends with a token that cannot start/end an operand (`,`, `*`, `AND`, `+`, ...); it has
     * balanced, correctly nested delimiters; no `;`, `:=`, top-level `,` or `=>`; no empty comma slot;
     * and every `WHEN`/`THEN`/`ELSE`/`END` belongs to a well-formed `CASE [selector] WHEN c THEN r
     * {WHEN c THEN r} [ELSE r] END`. A `CASE` token is a CASE expression only where an operand may start,
     * is not preceded by `.`, and a well-formed structure follows; otherwise it is an ordinary
     * identifier (`case`, `t.case`, `case(1)`; likewise `function(1)` is just a call). A `CASE` that looks
     * like an opener but has no well-formed structure leaves its `WHEN`/`END` stray: rejected.
     */
    fun initializerShapeOk(start: Int, endExclusive: Int): Boolean {
        if (start >= endExclusive || !canStartInitializer(start) || !canEndInitializer(endExclusive - 1)) return false
        return scanInitializerLevel(start, InitializerScan(endExclusive), allowComma = false, depth = 0) == endExclusive
    }

    private fun canStartInitializer(index: Int): Boolean = when (val type = stream.tokens[index].type) {
        is PlSqlPunctuator -> type !in INITIALIZER_NEVER_STARTS
        is PlSqlKeyword -> !(type.isReserved && stream.valueAt(index) in INITIALIZER_NEVER_STARTING_WORDS)
        else -> true
    }

    private fun canEndInitializer(index: Int): Boolean = when (val type = stream.tokens[index].type) {
        is PlSqlPunctuator -> type !in INITIALIZER_NEVER_ENDS
        is PlSqlKeyword -> !(stream.valueAt(index) in INITIALIZER_NEVER_ENDING_WORDS)
        else -> true
    }

    private fun endsOperand(index: Int): Boolean = when (val type = stream.tokens[index].type) {
        is PlSqlPunctuator -> type === PlSqlPunctuator.RPARENTHESIS || type === PlSqlPunctuator.RBRACKET
        is PlSqlKeyword -> stream.valueAt(index) == "NULL" || (!type.isReserved && stream.valueAt(index) !in OPERATOR_WORDS)
        else -> true
    }

    /**
     * Scans one nesting level from [start] and returns the index where it stopped (a `WHEN`, `THEN`,
     * `ELSE`, `END` or closer that belongs to an enclosing structure, or the limit), or null when the
     * level contains something that cannot be part of an expression.
     */
    private fun scanInitializerLevel(start: Int, scan: InitializerScan, allowComma: Boolean, depth: Int): Int? {
        if (depth > MAX_INITIALIZER_NESTING) return null
        var index = start
        while (index < scan.limit) {
            when {
                stream.isKeyword(index, "WHEN") || stream.isKeyword(index, "THEN") || stream.isKeyword(index, "ELSE") ||
                    stream.isKeyword(index, "END") || stream.isCloser(index) -> return index
                stream.isOpener(index) -> {
                    val close = scanInitializerLevel(index + 1, scan, allowComma = true, depth = depth + 1) ?: return null
                    if (close >= scan.limit || stream.tokens[close].type !== stream.closerFor(index)) return null
                    index = close + 1
                    continue
                }
                stream.isKeyword(index, "CASE") && (index == start || (!stream.isPunctuator(index - 1, PlSqlPunctuator.DOT) && !endsOperand(index - 1))) -> {
                    val end = caseEnd(index, scan, depth)
                    if (end != null) {
                        index = end
                        continue
                    }
                }
                stream.isPunctuator(index, PlSqlPunctuator.SEMICOLON) || stream.isPunctuator(index, PlSqlPunctuator.ASSIGNMENT) -> return null
                stream.isPunctuator(index, PlSqlPunctuator.ASSOCIATION) && !allowComma -> return null
                stream.isPunctuator(index, PlSqlPunctuator.COMMA) -> {
                    if (!allowComma || index == start || stream.isPunctuator(index - 1, PlSqlPunctuator.COMMA) ||
                        stream.isPunctuator(index + 1, PlSqlPunctuator.COMMA) || stream.isCloser(index + 1)
                    ) return null
                }
            }
            index++
        }
        return scan.limit
    }

    /** Index after the `END` of the `CASE` expression opened at [open], or null when it is not well formed. */
    private fun caseEnd(open: Int, scan: InitializerScan, depth: Int): Int? {
        scan.cases[open]?.let { return it.takeIf { end -> end >= 0 } }
        scan.cases[open] = -1
        val end = parseCase(open, scan, depth)
        if (end != null) scan.cases[open] = end
        return end
    }

    private fun parseCase(open: Int, scan: InitializerScan, depth: Int): Int? {
        var index = open + 1
        if (!stream.isKeyword(index, "WHEN")) {
            val selectorEnd = scanInitializerLevel(index, scan, allowComma = false, depth = depth + 1) ?: return null
            if (selectorEnd == index || selectorEnd >= scan.limit || !stream.isKeyword(selectorEnd, "WHEN")) return null
            index = selectorEnd
        }
        while (true) {
            val thenAt = scanInitializerLevel(index + 1, scan, allowComma = false, depth = depth + 1) ?: return null
            if (thenAt == index + 1 || thenAt >= scan.limit || !stream.isKeyword(thenAt, "THEN")) return null
            val after = scanInitializerLevel(thenAt + 1, scan, allowComma = false, depth = depth + 1) ?: return null
            if (after == thenAt + 1 || after >= scan.limit) return null
            when {
                stream.isKeyword(after, "WHEN") -> index = after
                stream.isKeyword(after, "END") -> return after + 1
                stream.isKeyword(after, "ELSE") -> {
                    val endAt = scanInitializerLevel(after + 1, scan, allowComma = false, depth = depth + 1) ?: return null
                    return if (endAt > after + 1 && endAt < scan.limit && stream.isKeyword(endAt, "END")) endAt + 1 else null
                }
                else -> return null
            }
        }
    }
}
