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
