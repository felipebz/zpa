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

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.api.Token
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.project.*

/** Reserved words retain their syntactic distinction from quoted identifiers in datatype headers. */
private val RESERVED_KEYWORD_VALUES: Set<String> = PlSqlKeyword.entries
    .filter { it.isReserved }.mapTo(HashSet()) { it.value.uppercase(java.util.Locale.ROOT) }

/** Words that may follow a datatype name in the strict standalone datatype check. */
private val DATATYPE_CONTINUATIONS = setOf(
    "CHAR", "BYTE", "VARYING", "PRECISION", "RAW", "WITH", "LOCAL", "TIME", "ZONE", "TO",
    "YEAR", "MONTH", "DAY", "HOUR", "MINUTE", "SECOND", "CHARACTER", "NATIONAL"
)

/** Strict validation of datatype token slices. */
internal class DatatypeShape(private val stream: TokenStream) {
    /**
     * Conservative datatype shape: `name[.name…] [@link[.link…]] %TYPE|%ROWTYPE` (the link only on an anchor),
     * `name[.name…]`, `REF name[.name]`, or a
     * name followed only by balanced groups and words from [DATATYPE_CONTINUATIONS]. Anything
     * else (an extra free identifier, punctuation, unknown words) is rejected: no fact.
     */
    fun strictDatatype(start: Int, endExclusive: Int): Boolean {
        if (start >= endExclusive) return false
        var index = start
        val ref = stream.isKeyword(index, "REF")
        if (ref) index++
        if (!stream.isNameBefore(index, endExclusive)) return false
        index++
        while (stream.isPunctuator(index, PlSqlPunctuator.DOT) && stream.isNameBefore(index + 1, endExclusive)) index += 2
        if (index == endExclusive) return true
        if (ref) return false
        if (stream.isPunctuator(index, PlSqlPunctuator.REMOTE)) {
            index = linkEnd(index, endExclusive) ?: return false
            if (!stream.isPunctuator(index, PlSqlPunctuator.MOD)) return false
        }
        if (stream.isPunctuator(index, PlSqlPunctuator.MOD)) {
            return index + 2 == endExclusive && (stream.isKeyword(index + 1, "TYPE") || stream.isKeyword(index + 1, "ROWTYPE"))
        }
        while (index < endExclusive) {
            index = when {
                stream.isPunctuator(index, PlSqlPunctuator.LPARENTHESIS) -> {
                    val close = stream.strictClose(index, endExclusive) ?: return false
                    close + 1
                }
                stream.tokens[index].type is PlSqlKeyword && stream.valueAt(index) in DATATYPE_CONTINUATIONS -> index + 1
                else -> return false
            }
        }
        return true
    }

    /** The index after the database link `@name[.name…]` whose `@` is at [at], or null when no name follows it. */
    fun linkEnd(at: Int, endExclusive: Int): Int? {
        var index = at + 1
        if (!stream.isNameBefore(index, endExclusive)) return null
        index++
        while (stream.isPunctuator(index, PlSqlPunctuator.DOT) && stream.isNameBefore(index + 1, endExclusive)) index += 2
        return index
    }

    /**
     * End (exclusive) of the datatype that starts at [start]: the first real token accepted by [isStop] at typed
     * delimiter depth 0, otherwise [limit]. Null when delimiters are crossed or unbalanced.
     */
    fun datatypeEnd(start: Int, limit: Int, isStop: (Int) -> Boolean): Int? {
        val expected = kotlin.collections.ArrayDeque<PlSqlPunctuator>()
        for (index in start until limit) {
            when {
                stream.isOpener(index) -> expected.addLast(stream.closerFor(index))
                stream.isCloser(index) -> if (expected.removeLastOrNull() !== stream.tokens[index].type) return null
                expected.isNotEmpty() -> Unit
                isStop(index) -> return index
            }
        }
        return if (expected.isEmpty()) limit else null
    }

    /**
     * [strictDatatype] accepts any balanced group after a type name. For values, every `( ... )` group
     * must additionally be a list of type arguments: each comma-separated part is `[+|-] (integer | *
     * | name) [CHAR | BYTE]`, with no nested delimiters. Anything else (`f(a NUMBER)`) is rejected.
     */
    fun strictTypeArguments(start: Int, endExclusive: Int): Boolean {
        var index = start
        while (index < endExclusive) {
            if (!stream.isPunctuator(index, PlSqlPunctuator.LPARENTHESIS)) {
                index++
                continue
            }
            val close = stream.strictClose(index, endExclusive) ?: return false
            val parts = stream.splitStrict(index, close) ?: return false
            if (!parts.all(::isTypeArgument)) return false
            index = close + 1
        }
        return true
    }

    private fun isTypeArgument(part: IntRange): Boolean {
        var index = part.first
        if (stream.isPunctuator(index, PlSqlPunctuator.PLUS) || stream.isPunctuator(index, PlSqlPunctuator.MINUS) ||
            stream.isPunctuator(index, PlSqlPunctuator.SUBTRACTION)
        ) index++
        index = when {
            stream.tokens.getOrNull(index)?.type === PlSqlTokenType.INTEGER_LITERAL || stream.isPunctuator(index, PlSqlPunctuator.MULTIPLICATION) -> index + 1
            index <= part.last && stream.isName(stream.tokens[index]) -> index + 1
            else -> return false
        }
        if (index <= part.last && (stream.isKeyword(index, "CHAR") || stream.isKeyword(index, "BYTE"))) index++
        return index == part.last + 1
    }
}

/** TypeRef construction, header identity normalization and type-shape metadata from token slices. */
internal class TypeReferences(private val stream: TokenStream) {
    fun typeShape(start: Int, end: Int): ProjectTypeShape? {
        val values = stream.tokens.subList(start, (end + 1).coerceAtMost(stream.tokens.size)).map { stream.value(it) }
        return when {
            "RECORD" in values -> ProjectTypeShape.RECORD
            "OBJECT" in values -> ProjectTypeShape.OBJECT
            "VARRAY" in values || "TABLE" in values -> ProjectTypeShape.COLLECTION
            "REF" in values && "CURSOR" in values -> ProjectTypeShape.REF_CURSOR
            else -> null
        }
    }

    fun typeReference(start: Int, endExclusive: Int): TypeRef? {
        if (start >= endExclusive || start >= stream.tokens.size) return null
        val end = endExclusive.coerceAtMost(stream.tokens.size)
        val selected = stream.tokens.subList(start, end)
        val percent = (start until end).firstOrNull { stream.isPunctuator(it, PlSqlPunctuator.MOD) }?.minus(start) ?: -1
        val ref = stream.isKeyword(start, "REF")
        val nameStart = if (ref) 1 else 0
        val at = (start until end).firstOrNull { stream.isPunctuator(it, PlSqlPunctuator.REMOTE) }?.minus(start) ?: -1
        val nameEnd = when {
            at >= 0 -> at
            percent >= 0 -> percent
            else -> selected.size
        }
        val datatypeTokens = selected.subList(nameStart, nameEnd)
        val parsedName = qualifiedSegments(datatypeTokens) ?: return null
        val anchor = anchorSuffix(start, percent, selected.size)
        if (percent >= 0 && (ref || parsedName.second != datatypeTokens.size || anchor == null)) return null
        // A database link is accepted only between the anchored name and its `%TYPE`/`%ROWTYPE`.
        val link = if (at < 0) null else {
            if (anchor == null || at > percent) return null
            val linkTokens = selected.subList(at + 1, percent)
            val parsedLink = qualifiedSegments(linkTokens) ?: return null
            if (parsedLink.second != linkTokens.size) return null
            parsedLink.first
        }
        val name = parsedName.first
        if (ref && !DatatypeShape(stream).strictDatatype(start, end)) return null
        val sourceRange = stream.range(start, end - 1)
        if (anchor != null) return AnchoredTypeRef(name, anchor, sourceRange, link)
        if (ref) return RefTypeRef(name, sourceRange)
        val headerTypeIdentity = normalizedHeaderTypeIdentity(selected)
        return if (headerTypeIdentity == null) {
            NamedTypeRef(name, sourceRange)
        } else {
            NamedTypeRef.withHeaderTypeIdentity(name, sourceRange, headerTypeIdentity)
        }
    }

    /** The `%TYPE`/`%ROWTYPE` anchor when the `%` at [percent] is followed by exactly one anchor keyword, else null. */
    private fun anchorSuffix(start: Int, percent: Int, size: Int): TypeAnchor? {
        if (percent < 0 || percent + 2 != size) return null
        val index = start + percent + 1
        return when {
            stream.isKeyword(index, "TYPE") -> TypeAnchor.TYPE
            stream.isKeyword(index, "ROWTYPE") -> TypeAnchor.ROWTYPE
            else -> null
        }
    }

    /**
     * A complete declared datatype slice `[start, endExclusive)`: a strict datatype, optionally followed by
     * `CHARACTER SET name` or `CHARACTER SET name%CHARSET`. Used wherever Oracle allows that clause (formals, function
     * returns, record fields, subtypes). Anything else after the datatype rejects the slice.
     */
    fun declaredType(start: Int, endExclusive: Int): TypeRef? {
        // The TABLE pseudo-type of a polymorphic table function: a reserved word, never a datatype name.
        if (endExclusive == start + 1 && stream.isKeyword(start, "TABLE")) {
            return NamedTypeRef(QualifiedName(listOf(OracleIdentifier.fromSource("TABLE"))), stream.range(start, start))
        }
        val charset = stream.firstKeywordAtTopLevel(start, setOf("CHARACTER"), endExclusive)
        if (charset == null || !stream.isKeyword(charset + 1, "SET")) {
            if (!DatatypeShape(stream).strictDatatype(start, endExclusive)) return null
            return typeReference(start, endExclusive)
        }
        if (!DatatypeShape(stream).strictDatatype(start, charset)) return null
        val charsetName = charset + 2
        val nameEnd = stream.strictQualifiedNameAnyLength(charsetName, endExclusive)
        val charsetSuffixValid = nameEnd == endExclusive || run {
            val percent = (charsetName until endExclusive)
                .firstOrNull { stream.isPunctuator(it, PlSqlPunctuator.MOD) } ?: return@run false
            stream.strictQualifiedNameAnyLength(charsetName, percent) == percent &&
                percent + 2 == endExclusive && stream.isKeyword(percent + 1, "CHARSET")
        }
        if (!charsetSuffixValid) return null
        val base = typeReference(start, charset) as? NamedTypeRef ?: return null
        val fullIdentity = normalizedHeaderTypeIdentity(stream.tokens.subList(start, endExclusive)) ?: return null
        return NamedTypeRef.withHeaderTypeIdentity(
            base.name,
            stream.range(start, endExclusive - 1),
            fullIdentity
        )
    }

    /**
     * Keeps the complete datatype token sequence for callable-heading correlation.
     * Token boundaries are length encoded so punctuation and adjacent words cannot
     * collapse into the same identity. Lexer trivia is not part of this sequence.
     */
    private fun normalizedHeaderTypeIdentity(selected: List<Token>): String? {
        if (isPlainQualifiedName(selected)) return null
        return selected.joinToString(separator = "") { token ->
            val part = normalizedHeaderToken(token)
            "${part.length}:$part;"
        }
    }

    private fun isPlainQualifiedName(selected: List<Token>): Boolean {
        var index = 0
        if (!isNameAt(selected, index)) return false
        while (true) {
            index++
            if (index !in selected.indices || selected[index].type !== PlSqlPunctuator.DOT || !isNameAt(selected, index + 1)) {
                return index == selected.size
            }
            index += 2
        }
    }

    private fun normalizedHeaderToken(token: Token): String = when (val type = token.type) {
        GenericTokenType.IDENTIFIER -> {
            val identifier = stream.identifier(token)
            val prefix = if (identifier.quoted && RESERVED_KEYWORD_VALUES.contains(identifier.lookupName)) "quoted-keyword" else "identifier"
            "$prefix:${QualifiedName(identifier).lookupKey()}"
        }
        is PlSqlKeyword -> if (type.isReserved) {
            "keyword:${stream.value(token)}"
        } else {
            "identifier:${QualifiedName(stream.identifier(token)).lookupKey()}"
        }
        else -> "token:${token.originalValue}"
    }

    private fun qualifiedSegments(tokens: List<Token>): Pair<QualifiedName, Int>? {
        val segments = mutableListOf<OracleIdentifier>()
        var index = 0
        if (tokens.isEmpty() || !isNameAt(tokens, index)) return null
        segments += stream.identifier(tokens[index++])
        while (index in tokens.indices && tokens[index].type === PlSqlPunctuator.DOT && isNameAt(tokens, index + 1)) {
            segments += stream.identifier(tokens[index + 1])
            index += 2
        }
        return QualifiedName(segments) to index
    }

    private fun isNameAt(tokens: List<Token>, index: Int) = index in tokens.indices && stream.isName(tokens[index])
}
