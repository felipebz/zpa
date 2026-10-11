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
        val typeEnd = datatypeShape.datatypeEnd(typeStart, semicolon, ::isValueTypeStop) ?: return null
        if (typeEnd == typeStart + 1 && stream.isKeyword(typeStart, "EXCEPTION")) return null
        if (!datatypeShape.strictDatatype(typeStart, typeEnd)) return null
        if (!datatypeShape.strictTypeArguments(typeStart, typeEnd)) return null
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

    /** The datatype of a value stops at its nullability constraint or initializer. */
    private fun isValueTypeStop(index: Int): Boolean =
        stream.isKeyword(index, "NOT") || stream.isKeyword(index, "NULL") || stream.isKeyword(index, "DEFAULT") ||
            stream.isPunctuator(index, PlSqlPunctuator.ASSIGNMENT)

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
