//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols.local

/**
 * [template] with the `/*A*/` mark (a statement position) removed and an undecided `$IF` written in the declarative part
 * of the block holding that statement, right before its `BEGIN`. Its dropped branch ends a block it does not open, so it
 * may change the block structure and declare any name the region holds, which a statement cannot. Other `/*S*/` marks
 * are removed.
 */
internal fun withConditionalDeclarations(template: String): String {
    val mark = template.indexOf("/*A*/")
    require(mark >= 0) { "no /*A*/ mark" }
    val begin = enclosingBegin(template.substring(0, mark)) ?: error("no open BEGIN before the /*A*/ mark")
    return (template.substring(0, begin) + "\$IF \$\$x \$THEN \$ELSE end; \$END " + template.substring(begin)).replace("/*A*/", "").replace("/*S*/", "")
}

/** The offset of the `BEGIN` of the innermost block still open at the end of [text]. */
private fun enclosingBegin(text: String): Int? {
    val words = Regex("(?i)\\b(begin|end)\\b(\\s+(if|loop|case)\\b)?").findAll(text).toList()
    var depth = 0
    for (word in words.asReversed()) {
        when {
            word.groupValues[1].equals("end", ignoreCase = true) -> if (word.groupValues[3].isEmpty()) depth++
            depth == 0 -> return word.range.first
            else -> depth--
        }
    }
    return null
}
