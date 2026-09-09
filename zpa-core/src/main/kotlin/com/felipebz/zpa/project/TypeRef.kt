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

enum class TypeAnchor {
    TYPE,
    ROWTYPE
}

/** Unresolved, AST-free type syntax retained for a later semantic resolution phase. */
sealed interface TypeRef {
    val sourceRange: SourceRange
    val name: QualifiedName

    fun structuralKey(): String
}

data class NamedTypeRef(
    override val name: QualifiedName,
    override val sourceRange: SourceRange
) : TypeRef {
    override fun structuralKey() = "named:${name.lookupKey()}"
}

data class AnchoredTypeRef(
    override val name: QualifiedName,
    val anchor: TypeAnchor,
    override val sourceRange: SourceRange
) : TypeRef {
    override fun structuralKey() = "anchored:${name.lookupKey()}:$anchor"
}

/** An Oracle REF datatype whose target object type remains unresolved. */
data class RefTypeRef(
    override val name: QualifiedName,
    override val sourceRange: SourceRange
) : TypeRef {
    override fun structuralKey() = "ref:${name.lookupKey()}"
}
