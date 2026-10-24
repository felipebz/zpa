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

import com.felipebz.zpa.squid.CcValue
import com.felipebz.zpa.squid.ConditionalCompilationSettings
import java.util.*

/** Result of evaluating a `$IF`/`$ELSIF` condition. [UNKNOWN] means it could not be evaluated. */
internal enum class ConditionResult { TRUE, FALSE, NULL, UNKNOWN }

/**
 * Evaluates the static boolean expression of a selection directive (the text between `$IF`/`$ELSIF` and `$THEN`)
 * against [ConditionalCompilationSettings], following *PL/SQL Language Reference*, "Static Expressions": BOOLEAN and
 * PLS_INTEGER values, three-valued logic, `$$name` inquiry directives and `DBMS_DB_VERSION` constants. Anything it does
 * not model (package constants, functions, strings, `/`, `MOD`, a parse failure, a type mismatch) is [UNKNOWN],
 * which propagates without ever being dropped: `FALSE AND unknown` is `FALSE` and `TRUE OR unknown` is `TRUE` only
 * because those hold whatever the unknown operand is.
 */
internal object ConditionalCompilationEvaluator {

    /** Predefined inquiry directives and compilation parameters: their values are not known here. */
    private val PREDEFINED = setOf(
        "PLSQL_LINE", "PLSQL_UNIT", "PLSQL_UNIT_OWNER", "PLSQL_UNIT_TYPE", "PLSCOPE_SETTINGS", "PLSQL_CCFLAGS",
        "PLSQL_CODE_TYPE", "PLSQL_OPTIMIZE_LEVEL", "PLSQL_WARNINGS", "NLS_LENGTH_SEMANTICS", "PERMIT_92_WRAP_FORMAT",
        "PLSQL_DEBUG", "PLSQL_COMPILER_FLAGS", "PLSQL_NATIVE_LIBRARY_DIR", "PLSQL_NATIVE_LIBRARY_SUBDIR_COUNT"
    )

    private val VER_LE = Regex("VER_LE_(\\d+)(?:_(\\d+))?")

    fun evaluate(condition: String, settings: ConditionalCompilationSettings): ConditionResult {
        val tokens = tokenize(condition) ?: return ConditionResult.UNKNOWN
        val parser = Parser(tokens, settings)
        val value = parser.parse() ?: return ConditionResult.UNKNOWN
        return when (value) {
            Value.Unknown -> ConditionResult.UNKNOWN
            is Value.Bool -> when (value.value) {
                true -> ConditionResult.TRUE
                false -> ConditionResult.FALSE
                null -> ConditionResult.NULL
            }
            Value.Null -> ConditionResult.NULL
            is Value.Int -> ConditionResult.UNKNOWN
        }
    }

    // ---- values ----

    private sealed class Value {
        /** Not evaluable. */
        data object Unknown : Value()

        /** The untyped `NULL` (a literal, or a `NULL` flag). */
        data object Null : Value()

        /** BOOLEAN; null is a BOOLEAN NULL. */
        data class Bool(val value: Boolean?) : Value()

        /** PLS_INTEGER; null is a PLS_INTEGER NULL. */
        data class Int(val value: Long?) : Value()
    }

    private fun asBool(v: Value): Value? = when (v) {
        is Value.Bool -> v
        Value.Null -> Value.Bool(null)
        else -> null
    }

    private fun asInt(v: Value): Value.Int? = when (v) {
        is Value.Int -> v
        Value.Null -> Value.Int(null)
        else -> null
    }

    private fun intValue(n: Long?): Value = when {
        n == null -> Value.Int(null)
        n < Int.MIN_VALUE || n > Int.MAX_VALUE -> Value.Unknown
        else -> Value.Int(n)
    }

    // ---- tokens ----

    private enum class T { WORD, INQUIRY, NUMBER, OP, LPAREN, RPAREN, DOT, COMMA, OTHER }

    private class Token(val type: T, val text: String)

    /** Null on anything that cannot be tokenized as a static expression. */
    private fun tokenize(s: String): List<Token>? {
        val out = ArrayList<Token>()
        var i = 0
        fun word(start: Int): Int {
            var p = start
            while (p < s.length && (s[p].isLetterOrDigit() || s[p] == '_' || s[p] == '$' || s[p] == '#')) p++
            return p
        }
        while (i < s.length) {
            val c = s[i]
            val n = if (i + 1 < s.length) s[i + 1] else '\u0000'
            when {
                c.isWhitespace() -> i++
                c == '-' && n == '-' -> { while (i < s.length && s[i] != '\n' && s[i] != '\r') i++ }
                c == '/' && n == '*' -> {
                    val end = s.indexOf("*/", i + 2)
                    if (end < 0) return null
                    i = end + 2
                }
                c == '$' && n == '$' -> {
                    val end = word(i + 2)
                    if (end == i + 2) return null
                    out += Token(T.INQUIRY, s.substring(i + 2, end).uppercase(Locale.ROOT)); i = end
                }
                c.isDigit() -> {
                    var p = i
                    while (p < s.length && s[p].isDigit()) p++
                    // Decimals and exponents are not PLS_INTEGER literals.
                    if (p < s.length && (s[p] == '.' || s[p].isLetter())) out += Token(T.OTHER, s.substring(i, p))
                    else out += Token(T.NUMBER, s.substring(i, p))
                    i = p
                }
                c.isLetter() -> {
                    val end = word(i)
                    out += Token(T.WORD, s.substring(i, end).uppercase(Locale.ROOT)); i = end
                }
                c == '(' -> { out += Token(T.LPAREN, "("); i++ }
                c == ')' -> { out += Token(T.RPAREN, ")"); i++ }
                c == '.' -> { out += Token(T.DOT, "."); i++ }
                c == ',' -> { out += Token(T.COMMA, ","); i++ }
                c in "<>!~^" && n == '=' -> { out += Token(T.OP, "$c="); i += 2 }
                c == '<' && n == '>' -> { out += Token(T.OP, "<>"); i += 2 }
                c in "=<>+-*" -> {
                    // `**` is exponentiation, not modeled.
                    if (c == '*' && n == '*') { out += Token(T.OTHER, "**"); i += 2 } else { out += Token(T.OP, c.toString()); i++ }
                }
                // Strings, quoted identifiers and any other operator: a leaf or operator that is not modeled.
                c == '\'' -> {
                    var p = i + 1
                    while (p < s.length) { if (s[p] == '\'') { if (p + 1 < s.length && s[p + 1] == '\'') p += 2 else break } else p++ }
                    if (p >= s.length) return null
                    out += Token(T.OTHER, s.substring(i, p + 1)); i = p + 1
                }
                c == '"' -> {
                    val end = s.indexOf('"', i + 1)
                    if (end < 0) return null
                    out += Token(T.OTHER, s.substring(i, end + 1)); i = end + 1
                }
                else -> { out += Token(T.OTHER, c.toString()); i++ }
            }
        }
        return out
    }

    // ---- parser and evaluator (one pass) ----

    private class Parser(private val tokens: List<Token>, private val settings: ConditionalCompilationSettings) {
        private var pos = 0

        private fun peek(): Token? = tokens.getOrNull(pos)
        private fun word(w: String) = peek()?.let { it.type == T.WORD && it.text == w } == true
        private fun op(o: String) = peek()?.let { it.type == T.OP && it.text == o } == true

        /** The value of the whole condition, or null when it does not parse as a static expression. */
        fun parse(): Value? {
            val v = or() ?: return null
            return if (pos == tokens.size) v else null
        }

        private fun or(): Value? {
            var left = and() ?: return null
            while (word("OR")) {
                pos++
                val right = and() ?: return null
                left = logical(left, right, dominant = true)
            }
            return left
        }

        private fun and(): Value? {
            var left = not() ?: return null
            while (word("AND")) {
                pos++
                val right = not() ?: return null
                left = logical(left, right, dominant = false)
            }
            return left
        }

        /** Kleene AND (`dominant` false) or OR (`dominant` true); the dominant value wins even over unknown. */
        private fun logical(a: Value, b: Value, dominant: Boolean): Value {
            val x = if (a == Value.Unknown) Value.Unknown else asBool(a) ?: Value.Unknown
            val y = if (b == Value.Unknown) Value.Unknown else asBool(b) ?: Value.Unknown
            if ((x as? Value.Bool)?.value == dominant || (y as? Value.Bool)?.value == dominant) return Value.Bool(dominant)
            if (x == Value.Unknown || y == Value.Unknown) return Value.Unknown
            if ((x as Value.Bool).value == null || (y as Value.Bool).value == null) return Value.Bool(null)
            return Value.Bool(!dominant)
        }

        private fun not(): Value? {
            if (word("NOT")) {
                pos++
                val v = not() ?: return null
                if (v == Value.Unknown) return Value.Unknown
                val b = asBool(v) as? Value.Bool ?: return Value.Unknown
                return Value.Bool(b.value?.not())
            }
            return comparison()
        }

        private fun comparison(): Value? {
            val left = additive() ?: return null
            if (word("IS")) {
                pos++
                val negated = word("NOT").also { if (it) pos++ }
                if (!word("NULL")) return null
                pos++
                val isNull = when (left) {
                    Value.Unknown -> return Value.Unknown
                    Value.Null -> true
                    is Value.Bool -> left.value == null
                    is Value.Int -> left.value == null
                }
                return Value.Bool(isNull != negated)
            }
            val operator = peek()?.takeIf { it.type == T.OP && it.text in COMPARISONS }?.text ?: return left
            pos++
            val right = additive() ?: return null
            return compare(left, operator, right)
        }

        private fun compare(a: Value, operator: String, b: Value): Value {
            if (a == Value.Unknown || b == Value.Unknown) return Value.Unknown
            val ordering: Int? = when {
                a is Value.Int || b is Value.Int -> {
                    val x = asInt(a) ?: return Value.Unknown
                    val y = asInt(b) ?: return Value.Unknown
                    if (x.value == null || y.value == null) null else x.value.compareTo(y.value)
                }
                a is Value.Bool || b is Value.Bool -> {
                    val x = asBool(a) as? Value.Bool ?: return Value.Unknown
                    val y = asBool(b) as? Value.Bool ?: return Value.Unknown
                    // FALSE < TRUE for BOOLEAN comparisons.
                    if (x.value == null || y.value == null) null else x.value.compareTo(y.value)
                }
                else -> null // NULL compared with NULL
            }
            if (ordering == null) return Value.Bool(null)
            return Value.Bool(
                when (operator) {
                    "=" -> ordering == 0
                    "<" -> ordering < 0
                    "<=" -> ordering <= 0
                    ">" -> ordering > 0
                    ">=" -> ordering >= 0
                    else -> ordering != 0
                }
            )
        }

        private fun additive(): Value? {
            var left = multiplicative() ?: return null
            while (op("+") || op("-")) {
                val minus = op("-")
                pos++
                val right = multiplicative() ?: return null
                left = arithmetic(left, right) { x, y -> if (minus) x - y else x + y }
            }
            return left
        }

        private fun multiplicative(): Value? {
            var left = unary() ?: return null
            while (op("*")) {
                pos++
                val right = unary() ?: return null
                left = arithmetic(left, right) { x, y -> x * y }
            }
            return left
        }

        private fun arithmetic(a: Value, b: Value, f: (Long, Long) -> Long): Value {
            if (a == Value.Unknown || b == Value.Unknown) return Value.Unknown
            val x = asInt(a) ?: return Value.Unknown
            val y = asInt(b) ?: return Value.Unknown
            if (x.value == null || y.value == null) return Value.Int(null)
            return intValue(f(x.value, y.value))
        }

        private fun unary(): Value? {
            if (op("-") || op("+")) {
                val minus = op("-")
                pos++
                val v = unary() ?: return null
                if (v == Value.Unknown) return Value.Unknown
                val n = asInt(v) ?: return Value.Unknown
                return intValue(n.value?.let { if (minus) -it else it })
            }
            return primary()
        }

        private fun primary(): Value? {
            val t = peek() ?: return null
            return when (t.type) {
                T.LPAREN -> {
                    pos++
                    val v = or() ?: return null
                    if (peek()?.type != T.RPAREN) return null
                    pos++
                    v
                }
                T.NUMBER -> { pos++; intValue(t.text.toBigInteger().takeIf { it.bitLength() < 63 }?.toLong() ?: return Value.Unknown) }
                T.INQUIRY -> { pos++; inquiry(t.text) }
                // A string literal or quoted identifier is not modeled, but still parses as an operand.
                T.OTHER -> if (t.text.startsWith("'") || t.text.startsWith("\"")) { pos++; Value.Unknown } else null
                T.WORD -> name()
                else -> null
            }
        }

        private fun inquiry(name: String): Value = when (val flag = settings.flags[name]) {
            CcValue.True -> Value.Bool(true)
            CcValue.False -> Value.Bool(false)
            CcValue.Null -> Value.Null
            is CcValue.Integer -> Value.Int(flag.value.toLong())
            null -> if (name in PREDEFINED) Value.Unknown else Value.Null
        }

        /** A literal keyword, a `DBMS_DB_VERSION` constant, or another (dotted) name, optionally called. */
        private fun name(): Value? {
            val first = peek()!!.text
            if (first in RESERVED) {
                return when (first) {
                    "TRUE" -> { pos++; Value.Bool(true) }
                    "FALSE" -> { pos++; Value.Bool(false) }
                    "NULL" -> { pos++; Value.Null }
                    else -> null
                }
            }
            val segments = ArrayList<String>()
            while (true) {
                val t = peek()
                if (t == null || t.type != T.WORD || t.text in RESERVED) return null
                segments += t.text
                pos++
                if (peek()?.type != T.DOT) break
                pos++
            }
            if (peek()?.type == T.LPAREN) {
                // A function call: its arguments must still parse, but its value is not known.
                pos++
                if (peek()?.type != T.RPAREN) {
                    while (true) {
                        or() ?: return null
                        if (peek()?.type == T.COMMA) pos++ else break
                    }
                }
                if (peek()?.type != T.RPAREN) return null
                pos++
                return Value.Unknown
            }
            return dbVersion(segments) ?: Value.Unknown
        }

        private fun dbVersion(segments: List<String>): Value? {
            val parts = if (segments.size == 3 && segments[0] == "SYS") segments.drop(1) else segments
            if (parts.size != 2 || parts[0] != "DBMS_DB_VERSION") return null
            val version = settings.databaseVersion ?: return Value.Unknown
            val member = parts[1]
            return when (member) {
                "VERSION" -> Value.Int(version.version.toLong())
                "RELEASE" -> Value.Int(version.release.toLong())
                else -> {
                    val m = VER_LE.matchEntire(member) ?: return Value.Unknown
                    val v = m.groupValues[1].toIntOrNull() ?: return Value.Unknown
                    val r = m.groupValues[2].takeIf { it.isNotEmpty() }?.let { it.toIntOrNull() ?: return Value.Unknown }
                    Value.Bool(
                        if (r == null) version.version <= v
                        else version.version < v || (version.version == v && version.release <= r)
                    )
                }
            }
        }

        private companion object {
            val COMPARISONS = setOf("=", "<>", "!=", "~=", "^=", "<", "<=", ">", ">=")
            val RESERVED = setOf("TRUE", "FALSE", "NULL", "AND", "OR", "NOT", "IS", "MOD", "XOR", "LIKE", "IN", "BETWEEN")
        }
    }
}
