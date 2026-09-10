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
package com.felipebz.zpa.project

import com.felipebz.zpa.api.symbols.PlSqlType

/**
 * AST-free semantic facts for a retained project TypeRef.
 *
 * A project resolution is retained even when the project does not contain the type. That
 * distinction is important: NotFoundInProject does not mean that the reference is invalid or
 * that it is not a built-in/external database type.
 */
internal sealed interface TypeRefSemanticResolution {
    val reference: TypeRef

    data class BuiltIn(
        override val reference: NamedTypeRef,
        val type: PlSqlType
    ) : TypeRefSemanticResolution

    data class Project(
        override val reference: NamedTypeRef,
        val resolution: ProjectTypeResolution
    ) : TypeRefSemanticResolution

    data class Unsupported(
        override val reference: TypeRef
    ) : TypeRefSemanticResolution
}

/** Classifies TypeRef values without constructing legacy PlSqlDatatype objects. */
internal class TypeRefSemanticResolver(
    private val projectTypeResolver: ProjectTypeResolver
) {
    fun resolve(
        reference: TypeRef,
        lookupContext: ProjectTypeLookupContext = ProjectTypeLookupContext()
    ): TypeRefSemanticResolution = when (reference) {
        is NamedTypeRef -> BuiltInTypeRefClassifier.classify(reference.name)?.let {
            TypeRefSemanticResolution.BuiltIn(reference, it)
        } ?: TypeRefSemanticResolution.Project(
            reference,
            projectTypeResolver.resolve(reference, lookupContext)
        )
        else -> TypeRefSemanticResolution.Unsupported(reference)
    }
}

/**
 * Built-in classification that can be applied to the information retained in a NamedTypeRef.
 *
 * The parser grammar remains the authoritative source for the accepted spellings and for
 * datatype constraints. This classifier only provides the broad PlSqlType category that is
 * still recoverable after TypeRef extraction. Qualified and quoted names are deliberately
 * excluded because their meaning is namespace- or identifier-dependent.
 */
internal object BuiltInTypeRefClassifier {
    fun classify(name: QualifiedName): PlSqlType? {
        if (name.segments.size != 1) return null
        val identifier = name.last
        if (identifier.quoted) return null

        return when (identifier.lookupName) {
            "BINARY_DOUBLE", "BINARY_FLOAT", "BINARY_INTEGER", "DEC", "DECIMAL",
            "DOUBLE", "FLOAT", "INT", "INTEGER", "NATURAL", "NATURALN", "NUMBER",
            "NUMERIC", "PLS_INTEGER", "POSITIVE", "POSITIVEN", "REAL", "SIGNTYPE",
            "SMALLINT" -> PlSqlType.NUMERIC

            "CHAR", "CHARACTER", "LONG", "NCHAR", "NVARCHAR2", "RAW", "ROWID",
            "STRING", "UROWID", "VARCHAR", "VARCHAR2" -> PlSqlType.CHARACTER

            "BFILE", "BLOB", "CLOB", "NCLOB" -> PlSqlType.LOB
            "BOOLEAN" -> PlSqlType.BOOLEAN
            "DATE", "INTERVAL", "TIMESTAMP" -> PlSqlType.DATE
            "JSON" -> PlSqlType.JSON
            else -> null
        }
    }
}
