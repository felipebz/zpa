//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols.local

/**
 * The text of a conditional-compilation boundary as the lexer reports it: the directive (`$IF … $THEN`, `$ELSIF …
 * $THEN`, `$ELSE`, `$END`) and the branches it drops. Oracle may compile a dropped branch; one that ends more blocks
 * than it opens (`$ELSE null; end; function f return number is begin`) ends the block it is written in and declares
 * in the enclosing one.
 */
internal object DroppedBranchText {
    /**
     * Whether every block the dropped text ends is one it opens: `BEGIN` and `CASE` open, `END` closes, `END IF`,
     * `END LOOP` and `END CASE` close what the text itself writes. Strings, quoted identifiers, comments and directive
     * words are skipped.
     */
    fun isBlockBalanced(text: String): Boolean {
        var depth = 0
        var previous: String? = null
        for (word in words(text)) {
            // `END IF`, `END LOOP`: an IF or a loop the text writes; `END CASE` and a plain `END` close a block or CASE.
            if (previous == "END" && (word == "IF" || word == "LOOP")) depth++
            if (word == "BEGIN" || word == "CASE" && previous != "END") depth++
            if (word == "END" && --depth < 0) return false
            previous = word
        }
        return depth == 0
    }

    /** The unquoted words of [text], upper case; `$` words (directives, inquiry directives) are skipped. */
    private fun words(text: String): List<String> {
        val words = ArrayList<String>()
        var i = 0
        while (i < text.length) {
            val skipped = skip(text, i)
            if (skipped > i) {
                i = skipped
                continue
            }
            if (text[i].isLetter() || text[i] == '$' || text[i] == '_') {
                val start = i
                while (i < text.length && (text[i].isLetterOrDigit() || text[i] in WORD_CHARACTERS)) i++
                if (text[start] != '$') words += text.substring(start, i).uppercase()
            } else {
                i++
            }
        }
        return words
    }

    /** The end of a string, a quoted identifier or a comment starting at [i]; [i] itself when none does. */
    private fun skip(text: String, i: Int): Int = when {
        text[i] == '\'' || text[i] == '"' -> after(text, text.indexOf(text[i], i + 1), 1)
        text.startsWith("--", i) -> after(text, text.indexOf('\n', i), 1)
        text.startsWith("/*", i) -> after(text, text.indexOf("*/", i + 2), 2)
        else -> i
    }

    private fun after(text: String, found: Int, length: Int) = if (found < 0) text.length else found + length

    private val WORD_CHARACTERS = setOf('_', '$', '#')
}
