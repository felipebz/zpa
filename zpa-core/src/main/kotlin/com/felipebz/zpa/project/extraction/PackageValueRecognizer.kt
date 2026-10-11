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

import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.project.PackageValueDeclaration
import com.felipebz.zpa.project.QualifiedName

private val PRAGMAS_WITHOUT_ARGUMENTS = listOf("AUTONOMOUS_TRANSACTION", "SERIALLY_REUSABLE", "UDF")
private val PRAGMAS_WITH_ARGUMENTS = listOf(
    "EXCEPTION_INIT", "INTERFACE", "RESTRICT_REFERENCES", "DEPRECATE", "SUPPRESSES_WARNING_6009", "COVERAGE"
)

/** Recognizes `name [CONSTANT] datatype [NULL | NOT NULL] [(DEFAULT | :=) initializer] ;` package specification values. */
internal class PackageValueRecognizer(
    private val stream: TokenStream,
    private val datatypeShape: DatatypeShape,
    private val typeReferences: TypeReferences,
    private val initializerShape: InitializerShape
) {
    /**
     * `name [CONSTANT] datatype [NULL | NOT NULL] [(DEFAULT | :=) expression] ;` for the declaration
     * starting at [start] and ending at the real `;` at [semicolon]. The datatype slice stops at the
     * first depth-0 `NOT`, `NULL`, `DEFAULT` or `:=` and is fully validated by [strictDatatype] before
     * [typeReference] runs. The initializer is only shape-checked ([initializerShapeOk]) and never kept.
     * `PRAGMA` declarations, exceptions (`name EXCEPTION;`) and anything with an unsupported datatype
     * yield null.
     */
    fun parseSpecificationValue(start: Int, semicolon: Int, owner: QualifiedName): ParsedDeclaration? {
        if (!stream.isNameBefore(start, semicolon) || isPragmaDeclaration(start)) return null
        var typeStart = start + 1
        val constant = stream.isKeyword(typeStart, "CONSTANT")
        if (constant) typeStart++
        val typeEnd = datatypeEnd(typeStart, semicolon) ?: return null
        if (typeEnd == typeStart + 1 && stream.isKeyword(typeStart, "EXCEPTION")) return null
        if (!datatypeShape.strictDatatype(typeStart, typeEnd)) return null
        if (!strictTypeArguments(typeStart, typeEnd)) return null
        var cursor = typeEnd
        if (stream.isKeyword(cursor, "NOT")) {
            if (!stream.isKeyword(cursor + 1, "NULL")) return null
            cursor += 2
        } else if (stream.isKeyword(cursor, "NULL")) {
            cursor++
        }
        if (cursor < semicolon) {
            if (!stream.isKeyword(cursor, "DEFAULT") && !stream.isPunctuator(cursor, PlSqlPunctuator.ASSIGNMENT)) return null
            if (!initializerShape.initializerShapeOk(cursor + 1, semicolon)) return null
        }
        val type = typeReferences.typeReference(typeStart, typeEnd) ?: return null
        val declaration = PackageValueDeclaration(
            owner, stream.identifier(stream.tokens[start]), constant, type, stream.fileId, stream.range(start, semicolon)
        )
        return ParsedDeclaration(declaration, semicolon + 1)
    }

    /**
     * End (exclusive) of the datatype that starts at [start]: the first real `NOT`, `NULL`, `DEFAULT` or
     * `:=` at typed delimiter depth 0, otherwise [limit]. Null when delimiters are crossed or unbalanced.
     */
    private fun datatypeEnd(start: Int, limit: Int): Int? {
        val expected = kotlin.collections.ArrayDeque<PlSqlPunctuator>()
        for (index in start until limit) {
            when {
                stream.isOpener(index) -> expected.addLast(stream.closerFor(index))
                stream.isCloser(index) -> if (expected.removeLastOrNull() !== stream.tokens[index].type) return null
                expected.isNotEmpty() -> Unit
                stream.isKeyword(index, "NOT") || stream.isKeyword(index, "NULL") || stream.isKeyword(index, "DEFAULT") ||
                    stream.isPunctuator(index, PlSqlPunctuator.ASSIGNMENT) -> return index
            }
        }
        return if (expected.isEmpty()) limit else null
    }

    /**
     * [strictDatatype] accepts any balanced group after a type name. For values, every `( ... )` group
     * must additionally be a list of type arguments: each comma-separated part is `[+|-] (integer | *
     * | name) [CHAR | BYTE]`, with no nested delimiters. Anything else (`f(a NUMBER)`) is rejected.
     */
    private fun strictTypeArguments(start: Int, endExclusive: Int): Boolean {
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
            stream.tokenTypeAt(index) === PlSqlTokenType.INTEGER_LITERAL || stream.isPunctuator(index, PlSqlPunctuator.MULTIPLICATION) -> index + 1
            index <= part.last && stream.isName(stream.tokens[index]) -> index + 1
            else -> return false
        }
        if (index <= part.last && (stream.isKeyword(index, "CHAR") || stream.isKeyword(index, "BYTE"))) index++
        return index == part.last + 1
    }

    /**
     * True for the shapes of the grammar's `PRAGMA_DECLARATION`, which precedes `VARIABLE_DECLARATION`:
     * `PRAGMA <name> ;` for the argument-less pragmas and `PRAGMA <name> (` for the others. A variable
     * named `pragma` with any other type (`pragma NUMBER;`) is a value; `pragma interface(x);` is
     * excluded conservatively even though it is not a complete pragma.
     */
    private fun isPragmaDeclaration(start: Int): Boolean {
        if (!stream.isKeyword(start, "PRAGMA")) return false
        if (PRAGMAS_WITHOUT_ARGUMENTS.any { stream.isKeyword(start + 1, it) } && stream.isPunctuator(start + 2, PlSqlPunctuator.SEMICOLON)) return true
        return PRAGMAS_WITH_ARGUMENTS.any { stream.isKeyword(start + 1, it) } && stream.isPunctuator(start + 2, PlSqlPunctuator.LPARENTHESIS)
    }
}
