//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.Token
import com.felipebz.zpa.project.OracleIdentifier

/** A word of conditional-compilation text: an unquoted word as written, or a quoted identifier's contents. */
internal class ConditionalWord(val text: String, val quoted: Boolean, val line: Int, val column: Int, val label: Boolean = false) {
    /** The name zpa reads the word as, compared as identifiers are. */
    val lookupName: String get() = OracleIdentifier.fromSource(if (quoted) "\"$text\"" else text).lookupName

    /** The unquoted word in upper case, for the block keywords; null for a quoted identifier. */
    val keyword: String? get() = if (quoted) null else text.uppercase()
}

/**
 * Undecided conditional-compilation text, read word by word: the text of a boundary as the lexer reports it (the
 * directive `$IF … $THEN`, `$ELSIF … $THEN`, `$ELSE` or `$END` and the branches it drops), or the tokens of a kept
 * branch, which Oracle may not compile. Strings (`'…'`, `n'…'`, `q'…'`, `nq'…'` with any delimiter), comments, inquiry
 * directives (`$$name`) and directive words are skipped; quoted identifiers are words.
 */
internal object ConditionalText {
    /** The words of [text], whose first character is at [line], [column]. */
    fun words(text: String, line: Int, column: Int): List<ConditionalWord> {
        val words = ArrayList<ConditionalWord>()
        val position = Positions(text, line, column)
        // A word right after `<<` is a label.
        var labelNext = false
        fun add(word: String, quoted: Boolean, at: Int) {
            words += ConditionalWord(word, quoted, position.line(at), position.column(at), labelNext)
            labelNext = false
        }
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                text.startsWith("<<", i) -> {
                    labelNext = true
                    i += 2
                }
                c == '\'' -> i = stringEnd(text, i)
                c == '"' -> {
                    val end = text.indexOf('"', i + 1).let { if (it < 0) text.length else it }
                    if (end > i + 1) add(text.substring(i + 1, end), true, i)
                    i = end + 1
                }
                text.startsWith("--", i) -> i = after(text, text.indexOf('\n', i), 1)
                text.startsWith("/*", i) -> i = after(text, text.indexOf("*/", i + 2), 2)
                c.isLetter() || c == '_' || c == '$' -> {
                    val literal = prefixedLiteralEnd(text, i)
                    if (literal != null) {
                        i = literal
                    } else {
                        val start = i
                        while (i < text.length && (text[i].isLetterOrDigit() || text[i] in WORD_CHARACTERS)) i++
                        // `$IF`, `$END`, `$$name`: directives and inquiry directives are not names.
                        if (c != '$') add(text.substring(start, i), false, start)
                    }
                }
                c.isDigit() -> i = numberEnd(text, i)
                c.isWhitespace() -> i++
                else -> {
                    labelNext = false
                    i++
                }
            }
        }
        return words
    }

    /** The words of the tokens of a kept branch, each at its token's position. */
    fun words(tokens: List<Token>): List<ConditionalWord> {
        val words = ArrayList<ConditionalWord>()
        var labelNext = false
        for (token in tokens) {
            if (token.originalValue == "<<") {
                labelNext = true
                continue
            }
            val own = words(token.originalValue, token.line, token.column)
            words += if (labelNext) own.mapIndexed { i, w -> if (i == 0) ConditionalWord(w.text, w.quoted, w.line, w.column, true) else w } else own
            labelNext = false
        }
        return words
    }

    /**
     * Whether every block the text ends is one it opens: `BEGIN` and `CASE` open, `END` closes; `IF` and `LOOP` open
     * and `END IF`, `END LOOP` close them, `END CASE` closes a `CASE`. Text that ends more than it opens, or opens more
     * than it ends, may change the block structure around it.
     */
    fun isBlockBalanced(words: List<ConditionalWord>): Boolean {
        val keywords = words.mapNotNull { it.keyword }
        var blocks = 0
        var ifs = 0
        var loops = 0
        var i = 0
        while (i < keywords.size) {
            when (keywords[i]) {
                "BEGIN", "CASE" -> blocks++
                "IF" -> ifs++
                "LOOP" -> loops++
                "END" -> when (keywords.getOrNull(i + 1)) {
                    "IF" -> { ifs--; i++ }
                    "LOOP" -> { loops--; i++ }
                    "CASE" -> { blocks--; i++ }
                    else -> blocks--
                }
            }
            if (blocks < 0 || ifs < 0 || loops < 0) return false
            i++
        }
        return blocks == 0 && ifs == 0 && loops == 0
    }

    /** The undecided piece the words [words] form: wide when they may change the block structure. */
    fun piece(words: List<ConditionalWord>, wide: Boolean = !isBlockBalanced(words), kept: LocalConditionalPiece.Span? = null): LocalConditionalPiece {
        val mentions = HashMap<String, MutableList<Long>>()
        for (word in words) mentions.getOrPut(word.lookupName) { ArrayList(1) } += LocalConditionalPiece.position(word.line, word.column)
        val labels = words.filter { it.label }.mapTo(HashSet()) { it.lookupName }
        return LocalConditionalPiece(wide, mentions.mapValues { (_, positions) -> positions.toLongArray() }, labels, kept)
    }

    /**
     * The end of the number starting with the digit at [i], as Oracle writes one: digits with at most one `.` not
     * followed by another (`1..n` is a range), an optional exponent, an optional `f`/`d` suffix. What follows is read
     * again: `1..l_cnt` writes `l_cnt`.
     */
    private fun numberEnd(text: String, i: Int): Int {
        var j = i
        while (j < text.length && text[j].isDigit()) j++
        if (j < text.length && text[j] == '.' && text.getOrNull(j + 1) != '.') {
            j++
            while (j < text.length && text[j].isDigit()) j++
        }
        if (j < text.length && (text[j] == 'e' || text[j] == 'E')) {
            var k = j + 1
            if (k < text.length && (text[k] == '+' || text[k] == '-')) k++
            if (k < text.length && text[k].isDigit()) {
                j = k
                while (j < text.length && text[j].isDigit()) j++
            }
        }
        if (j < text.length && text[j] in "fFdD" && !(text.getOrNull(j + 1)?.let { it.isLetterOrDigit() || it in WORD_CHARACTERS } ?: false)) j++
        return j
    }

    /** The end of the string starting with the quote at [i] (`''` inside it is a quote). */
    private fun stringEnd(text: String, i: Int): Int {
        var j = i + 1
        while (j < text.length) {
            if (text[j] == '\'') {
                if (j + 1 < text.length && text[j + 1] == '\'') j += 2 else return j + 1
            } else {
                j++
            }
        }
        return text.length
    }

    /** The end of an `n'…'`, `q'…'` or `nq'…'` literal starting at the word start [i]; null when there is none. */
    private fun prefixedLiteralEnd(text: String, i: Int): Int? {
        var j = i
        if (text[j] == 'n' || text[j] == 'N') j++
        val quoted = j < text.length && (text[j] == 'q' || text[j] == 'Q')
        if (quoted) j++
        if (j == i || j >= text.length || text[j] != '\'') return null
        if (!quoted) return stringEnd(text, j)
        val open = text.getOrNull(j + 1) ?: return text.length
        val close = CLOSING[open] ?: open
        val end = text.indexOf("$close'", j + 2)
        return if (end < 0) text.length else end + 2
    }

    private fun after(text: String, found: Int, length: Int) = if (found < 0) text.length else found + length

    /** Line and column of an offset of a text, with the lexer's line rules (LF, or CR not followed by LF). */
    private class Positions(private val text: String, private val line: Int, private val column: Int) {
        private val lineStarts: IntArray by lazy(LazyThreadSafetyMode.NONE) {
            val starts = ArrayList<Int>()
            text.forEachIndexed { index, c ->
                if (c == '\n' || c == '\r' && text.getOrNull(index + 1) != '\n') starts += index + 1
            }
            starts.toIntArray()
        }

        /** How many line starts are at or before [offset]. */
        private fun linesBefore(offset: Int): Int {
            val found = java.util.Arrays.binarySearch(lineStarts, offset)
            return if (found >= 0) found + 1 else -found - 1
        }

        fun line(offset: Int): Int = line + linesBefore(offset)

        fun column(offset: Int): Int {
            val lines = linesBefore(offset)
            return if (lines == 0) column + offset else offset - lineStarts[lines - 1]
        }
    }

    private val WORD_CHARACTERS = setOf('_', '$', '#')
    private val CLOSING = mapOf('[' to ']', '{' to '}', '(' to ')', '<' to '>')
}

/**
 * One piece of undecided conditional-compilation text: the text of one boundary (a directive and the branches it
 * drops), or the tokens of one kept branch. It can change the lookup of a name, hold a use of it or declare what it
 * hides only when it mentions the name, unless it is [wide]: it may change the block structure, or it could not be
 * delimited, and then it counts for every name.
 */
internal class LocalConditionalPiece(
    val wide: Boolean,
    private val mentions: Map<String, LongArray>,
    /** The names the piece writes as labels (`<<name>>`), which statements declare. */
    private val labels: Set<String> = emptySet(),
    /** For the tokens of a kept branch, where they are; null for dropped text. */
    val kept: Span? = null
) {
    class Span(val startLine: Int, val startColumn: Int, val endLine: Int, val endColumn: Int) {
        fun contains(line: Int, column: Int): Boolean =
            (startLine < line || startLine == line && startColumn <= column) && (line < endLine || line == endLine && column < endColumn)
    }

    /** Whether the piece counts for [name]: wide, or mentioning it at a position other than [except]. */
    fun affects(name: OracleIdentifier, except: Long = NO_POSITION): Boolean =
        wide || mentions[name.lookupName]?.any { it != except } == true

    /** The positions where the piece writes [name]; empty when it does not. */
    fun mentions(name: OracleIdentifier): LongArray = mentions[name.lookupName] ?: EMPTY

    /** Whether the piece writes [name] as a label. */
    fun labels(name: OracleIdentifier): Boolean = name.lookupName in labels

    companion object {
        const val NO_POSITION = -1L

        private val EMPTY = LongArray(0)

        /** The position of a word written at [line], [column], as [affects] compares it. */
        fun position(line: Int, column: Int): Long = (line.toLong() shl 32) or column.toLong()

        /** A piece that counts for every name. */
        val WIDE = LocalConditionalPiece(true, emptyMap())
    }
}
