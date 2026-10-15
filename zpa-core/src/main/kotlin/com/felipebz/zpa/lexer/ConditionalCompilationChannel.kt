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
package com.felipebz.zpa.lexer

import com.felipebz.flr.api.Token
import com.felipebz.flr.api.Trivia
import com.felipebz.flr.channel.Channel
import com.felipebz.flr.channel.CodeReader
import com.felipebz.flr.impl.LexerOutput
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.squid.ConditionalCompilationSettings

/**
 * Removes PL/SQL selection and error directives (`$IF … $THEN … $ELSIF … $ELSE … $END`, `$ERROR … $END`) from the
 * token stream, keeping one branch as ordinary tokens. Each run of removed text (directives and the branches not
 * kept) becomes one skipped-text trivia of the next token, typed [PlSqlTokenType.CONDITIONAL_BRANCH_BOUNDARY] when it
 * opens or closes the kept branch, or [PlSqlTokenType.CONDITIONAL_DIRECTIVE] when it is a complete directive that
 * keeps no text.
 *
 * Branch policy (conditions are not evaluated): the first branch whose text is not just an `$ERROR … $END` directive
 * (ignoring whitespace and comments) is kept; an empty branch is kept like any other. When no branch qualifies, the
 * whole `$IF … $END` is removed.
 *
 * The channel is stateless (the lexer is reused): it looks ahead with [CodeReader.intAt] and then consumes the
 * decided number of characters, so line and column positions of later tokens stay correct. While scanning it skips
 * comments, string literals (including q-quoted ones), quoted identifiers, identifiers containing `$`/`#` and
 * inquiry directives (`$$name`), so directive-looking text inside them never counts; `$IF` and `$ERROR` nest and
 * `$END` closes one level. On malformed input (no `$THEN`, no matching `$END`) it consumes nothing and the `$` falls
 * through to the following channels, so the parser reports the error where it is.
 */
class ConditionalCompilationChannel @JvmOverloads constructor(
    private val settings: ConditionalCompilationSettings = ConditionalCompilationSettings.NONE
) : Channel<LexerOutput> {

    override fun consume(code: CodeReader, output: LexerOutput): Boolean {
        if (code.peek() != '$'.code) return false
        val scanner = Scanner(code, settings)
        val directive = scanner.directiveAt(0) ?: return false
        var type = PlSqlTokenType.CONDITIONAL_BRANCH_BOUNDARY
        val end = when (directive.kind) {
            Kind.IF -> scanner.selection(directive.end).also { if (it == scanner.lastSelectionEnd) type = PlSqlTokenType.CONDITIONAL_DIRECTIVE }
            // The kept branch just ended: skip the rest of this $IF.
            Kind.ELSIF, Kind.ELSE -> scanner.matchingEnd(directive.end)
            // The kept branch ends here.
            Kind.END -> directive.end
            Kind.ERROR -> scanner.matchingEnd(directive.end).also { type = PlSqlTokenType.CONDITIONAL_DIRECTIVE }
            Kind.THEN -> -1
        }
        if (end < 0) return false
        val line = code.getLinePosition()
        val column = code.getColumnPosition()
        val text = StringBuilder(end)
        repeat(end) { text.appendCodePoint(code.pop()) }
        output.addTrivia(
            Trivia.createSkippedText(
                Token.builder().setType(type).setValueAndOriginalValue(text.toString()).setLine(line).setColumn(column).build()
            )
        )
        return true
    }

    private enum class Kind { IF, THEN, ELSIF, ELSE, END, ERROR }

    private companion object {
        val BRANCH_ENDS = setOf(Kind.ELSIF, Kind.ELSE, Kind.END)
    }

    private class Directive(val kind: Kind, val end: Int)

    /** One branch of a selection: where its text starts, its condition text (null for `$ELSE`), and whether it is only `$ERROR`. */
    private class Branch(val start: Int, val condition: String?, val errorOnly: Boolean)

    private class Scanner(private val code: CodeReader, private val settings: ConditionalCompilationSettings) {
        /** The end of the `$IF … $END` scanned by the last [selection] call; equal to its result when nothing is kept. */
        var lastSelectionEnd = -1

        private fun char(offset: Int): Int = code.intAt(offset)

        private fun isLetter(c: Int) = c > 0 && Character.isLetter(c)

        private fun isWordPart(c: Int) = c > 0 && (Character.isLetterOrDigit(c) || c == '_'.code || c == '$'.code || c == '#'.code)

        /** The directive starting at [position] (`$` plus a directive word), or null. */
        fun directiveAt(position: Int): Directive? {
            if (char(position) != '$'.code || !isLetter(char(position + 1))) return null
            var end = position + 1
            while (isWordPart(char(end))) end++
            val word = StringBuilder()
            for (i in position + 1 until end) word.appendCodePoint(char(i))
            val kind = when (word.toString().uppercase()) {
                "IF" -> Kind.IF
                "THEN" -> Kind.THEN
                "ELSIF" -> Kind.ELSIF
                "ELSE" -> Kind.ELSE
                "END" -> Kind.END
                "ERROR" -> Kind.ERROR
                else -> return null
            }
            return Directive(kind, end)
        }

        /**
         * The end of the lexical element at [position] that may contain directive-looking text: a comment, a string or
         * q-quoted literal, a quoted identifier, an identifier, a number or an inquiry directive. -1 when there is none.
         * An unterminated element runs to the end of the input.
         */
        private fun elementEnd(position: Int): Int {
            val c = char(position)
            val next = char(position + 1)
            return when {
                c == '-'.code && next == '-'.code -> until(position + 2) { it == '\n'.code || it == '\r'.code }
                c == '/'.code && next == '*'.code -> {
                    var p = position + 2
                    while (char(p) >= 0 && !(char(p) == '*'.code && char(p + 1) == '/'.code)) p++
                    if (char(p) < 0) p else p + 2
                }
                c == '"'.code -> until(position + 1) { it == '"'.code }.let { if (char(it) < 0) it else it + 1 }
                c == '$'.code && next == '$'.code -> {
                    var p = position + 2
                    while (isWordPart(char(p))) p++
                    p
                }
                c == '\''.code -> stringEnd(position)
                // A number: no `$`/`#`, so a directive glued to it (`2$END`) is still seen.
                Character.isDigit(c) -> {
                    var p = position
                    while (char(p) > 0 && (Character.isLetterOrDigit(char(p)) || char(p) == '_'.code)) p++
                    p
                }
                isWordPart(c) && c != '$'.code -> {
                    quotedLiteralAt(position)?.let { return it }
                    var p = position
                    while (isWordPart(char(p))) p++
                    p
                }
                else -> -1
            }
        }

        private fun until(start: Int, stop: (Int) -> Boolean): Int {
            var p = start
            while (char(p) >= 0 && !stop(char(p))) p++
            return p
        }

        /** `'…'` with `''` escapes, starting at the quote. */
        private fun stringEnd(quote: Int): Int {
            var p = quote + 1
            while (char(p) >= 0) {
                if (char(p) == '\''.code) {
                    if (char(p + 1) == '\''.code) p += 2 else return p + 1
                } else {
                    p++
                }
            }
            return p
        }

        /** `n'…'`, `q'X…X'` or `nq'X…X'` (any case) starting at [position]; null when the word is not such a prefix. */
        private fun quotedLiteralAt(position: Int): Int? {
            var p = position
            if (char(p) == 'n'.code || char(p) == 'N'.code) p++
            val q = char(p) == 'q'.code || char(p) == 'Q'.code
            if (q) p++
            if (char(p) != '\''.code || p == position) return null
            if (!q) return stringEnd(p)
            val open = char(p + 1)
            if (open < 0) return p + 1
            val close = when (open) {
                '['.code -> ']'.code
                '('.code -> ')'.code
                '{'.code -> '}'.code
                '<'.code -> '>'.code
                else -> open
            }
            var e = p + 2
            while (char(e) >= 0 && !(char(e) == close && char(e + 1) == '\''.code)) e++
            return if (char(e) < 0) e else e + 2
        }

        /** One step of a scan: the next directive at [position], skipping whitespace and lexical elements. */
        private class Step(val directive: Directive?, val end: Int, val significant: Boolean)

        private fun step(position: Int): Step? {
            val c = char(position)
            if (c < 0) return null
            if (Character.isWhitespace(c)) return Step(null, position + 1, false)
            directiveAt(position)?.let { return Step(it, it.end, true) }
            val end = elementEnd(position)
            if (end >= 0) {
                val comment = (c == '-'.code && char(position + 1) == '-'.code) || (c == '/'.code && char(position + 1) == '*'.code)
                return Step(null, end, !comment)
            }
            return Step(null, position + 1, true)
        }

        /** The condition text starting at [position] and the position after its `$THEN`, or null. */
        private fun condition(position: Int): Pair<String, Int>? {
            var p = position
            while (true) {
                val step = step(p) ?: return null
                val directive = step.directive
                if (directive != null) {
                    if (directive.kind != Kind.THEN) return null
                    val text = StringBuilder()
                    for (i in position until p) text.appendCodePoint(char(i))
                    return text.toString() to directive.end
                }
                p = step.end
            }
        }

        /** The position after the `$END` closing the current level, ignoring sibling `$ELSIF`/`$ELSE`; or -1. */
        fun matchingEnd(position: Int): Int {
            var p = position
            var depth = 0
            while (true) {
                val step = step(p) ?: return -1
                when (step.directive?.kind) {
                    Kind.IF, Kind.ERROR -> depth++
                    Kind.END -> if (depth == 0) return step.end else depth--
                    else -> Unit
                }
                p = step.end
            }
        }

        /**
         * After `$IF` (at [position]): validates the whole selection up to its `$END` and returns how many characters
         * to remove: up to the start of the kept branch, or through `$END` when no branch is kept. -1 if malformed.
         */
        fun selection(position: Int): Int {
            val first = condition(position) ?: return -1
            var branchCondition: String? = first.first
            var branchStart = first.second
            val branches = ArrayList<Branch>()
            while (true) {
                var p = branchStart
                var depth = 0
                var items = 0
                var firstIsError = false
                val terminator: Directive
                while (true) {
                    val step = step(p) ?: return -1
                    val directive = step.directive
                    if (depth == 0 && directive != null && directive.kind in BRANCH_ENDS) {
                        terminator = directive
                        break
                    }
                    if (depth == 0 && step.significant) {
                        items++
                        if (items == 1) firstIsError = directive?.kind == Kind.ERROR
                    }
                    when (directive?.kind) {
                        Kind.IF, Kind.ERROR -> depth++
                        Kind.END -> depth--
                        else -> Unit
                    }
                    p = step.end
                }
                branches += Branch(branchStart, branchCondition, items == 1 && firstIsError)
                when (terminator.kind) {
                    Kind.END -> {
                        lastSelectionEnd = terminator.end
                        return select(branches)?.start ?: terminator.end
                    }
                    Kind.ELSIF -> {
                        val next = condition(terminator.end) ?: return -1
                        branchCondition = next.first
                        branchStart = next.second
                    }
                    else -> {
                        branchCondition = null
                        branchStart = terminator.end
                    }
                }
            }
        }

        /** The kept branch, or null when nothing is kept. */
        private fun select(branches: List<Branch>): Branch? {
            if (!settings.isActive) return firstNotErrorOnly(branches)
            for ((index, branch) in branches.withIndex()) {
                val condition = branch.condition ?: return branch.takeUnless { it.errorOnly }
                when (ConditionalCompilationEvaluator.evaluate(condition, settings)) {
                    ConditionResult.TRUE -> return branch.takeUnless { it.errorOnly }
                    ConditionResult.FALSE, ConditionResult.NULL -> Unit
                    // Earlier branches are proven not TRUE; the rest cannot be decided.
                    ConditionResult.UNKNOWN -> return firstNotErrorOnly(branches.subList(index, branches.size))
                }
            }
            return null
        }

        private fun firstNotErrorOnly(branches: List<Branch>) = branches.firstOrNull { !it.errorOnly }
    }
}
