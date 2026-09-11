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

class NamedTypeRef private constructor(
    override val name: QualifiedName,
    override val sourceRange: SourceRange,
    internal val headerTypeIdentity: String?,
    @Suppress("UNUSED_PARAMETER") marker: Unit
) : TypeRef {
    constructor(name: QualifiedName, sourceRange: SourceRange) : this(name, sourceRange, null, Unit)

    companion object {
        internal fun withHeaderTypeIdentity(
            name: QualifiedName,
            sourceRange: SourceRange,
            headerTypeIdentity: String
        ): NamedTypeRef = NamedTypeRef(name, sourceRange, headerTypeIdentity, Unit)
    }

    override fun structuralKey() = "named:${name.lookupKey()}"

    // Heading-only syntax is not part of the TypeRef value projection. Keeping it out of
    // equality also preserves the existing AST/project declaration value semantics.
    override fun equals(other: Any?): Boolean =
        other is NamedTypeRef && name == other.name && sourceRange == other.sourceRange

    override fun hashCode(): Int = 31 * name.hashCode() + sourceRange.hashCode()

    override fun toString(): String = "NamedTypeRef(name=$name, sourceRange=$sourceRange)"

    operator fun component1(): QualifiedName = name

    operator fun component2(): SourceRange = sourceRange

    fun copy(
        name: QualifiedName = this.name,
        sourceRange: SourceRange = this.sourceRange
    ): NamedTypeRef = NamedTypeRef(
        name,
        sourceRange,
        if (name == this.name) headerTypeIdentity else null,
        Unit
    )
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

/**
 * Identity used when correlating subprogram headings. This separate key retains the complete
 * normalized datatype token sequence without changing project type lookup semantics.
 */
internal fun TypeRef.headerIdentityKey(): String = when (this) {
    is NamedTypeRef -> headerTypeIdentity ?: structuralKey()
    else -> structuralKey()
}
