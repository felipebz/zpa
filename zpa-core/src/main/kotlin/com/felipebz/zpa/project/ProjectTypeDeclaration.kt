//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.project

enum class ProjectTypeShape {
    RECORD,
    OBJECT,
    COLLECTION,
    REF_CURSOR
}

enum class ProjectCollectionKind {
    ASSOCIATIVE_ARRAY,
    NESTED_TABLE,
    VARRAY
}

data class ProjectRecordField(
    val name: OracleIdentifier,
    val ordinal: Int,
    val typeRef: TypeRef,
    val sourceRange: SourceRange
) {
    init {
        require(ordinal >= 0) { "Record field ordinals are zero-based" }
    }
}

/** The declaration kinds returned by the first project type resolver. */
sealed interface ProjectTypeDeclaration : ProjectDeclaration {
    val qualifiedName: QualifiedName
}

data class StandaloneTypeDeclaration(
    val name: QualifiedName,
    val shape: ProjectTypeShape?,
    override val fileId: FileId,
    override val sourceRange: SourceRange,
    /** Direct element type syntax of a COLLECTION, when the extractor admitted it. Unresolved. */
    val elementType: TypeRef? = null,
    val attributes: List<ProjectRecordField> = emptyList(),
    val methods: List<ProjectTypeMethod> = emptyList(),
    val supertype: TypeRef? = null,
    /** False when an attribute could not be extracted. `ALTER TYPE` changes are tracked by the index. */
    val attributesComplete: Boolean = true,
    /** False when a method could not be extracted. `ALTER TYPE` changes are tracked by the index. */
    val methodsComplete: Boolean = true,
    /**
     * True when conditional-compilation text the configured settings do not decide lies in the declaration: its
     * attributes and methods may not be those Oracle compiles.
     */
    val conditional: Boolean = false,
    val collectionKind: ProjectCollectionKind? = null
) : ProjectTypeDeclaration {
    override val kind = ProjectDeclarationKind.STANDALONE_TYPE
    override val role = DeclarationRole.STANDALONE
    override val qualifiedName: QualifiedName = name

    init {
        require((elementType == null && collectionKind == null) || shape == ProjectTypeShape.COLLECTION) {
            "Only COLLECTION declarations may contain an element type or a collection kind"
        }
        require(shape == ProjectTypeShape.OBJECT || (attributes.isEmpty() && methods.isEmpty() && supertype == null)) {
            "Only OBJECT declarations may contain attributes, methods or a supertype"
        }
    }
}

class PackageTypeDeclaration(
    val owner: QualifiedName,
    val name: OracleIdentifier,
    val shape: ProjectTypeShape?,
    recordFields: List<ProjectRecordField>,
    override val fileId: FileId,
    override val sourceRange: SourceRange,
    /** Direct element type syntax of a COLLECTION, when the extractor admitted it. Unresolved. */
    val elementType: TypeRef? = null,
    val collectionKind: ProjectCollectionKind? = null
) : ProjectTypeDeclaration {

    val recordFields: List<ProjectRecordField> = immutableList(recordFields)
    override val kind = ProjectDeclarationKind.PACKAGE_TYPE
    override val role = DeclarationRole.SPECIFICATION
    override val qualifiedName: QualifiedName = owner.append(name)

    init {
        require(shape == ProjectTypeShape.RECORD || recordFields.isEmpty()) {
            "Only RECORD declarations may contain record fields"
        }
        require((elementType == null && collectionKind == null) || shape == ProjectTypeShape.COLLECTION) {
            "Only COLLECTION declarations may contain an element type or a collection kind"
        }
    }

    override fun equals(other: Any?): Boolean = other is PackageTypeDeclaration &&
        owner == other.owner && name == other.name && shape == other.shape &&
        recordFields == other.recordFields && elementType == other.elementType && collectionKind == other.collectionKind &&
        fileId == other.fileId && sourceRange == other.sourceRange

    override fun hashCode(): Int = listOf(owner, name, shape, recordFields, elementType, collectionKind, fileId, sourceRange).hashCode()

    override fun toString(): String =
        "PackageTypeDeclaration($qualifiedName, $shape, $recordFields, $elementType, $collectionKind, $fileId, $sourceRange)"
}

data class PackageSubtypeDeclaration(
    val owner: QualifiedName,
    val name: OracleIdentifier,
    val baseType: TypeRef,
    override val fileId: FileId,
    override val sourceRange: SourceRange
) : ProjectTypeDeclaration {
    override val kind = ProjectDeclarationKind.PACKAGE_SUBTYPE
    override val role = DeclarationRole.SPECIFICATION
    override val qualifiedName: QualifiedName = owner.append(name)
}
