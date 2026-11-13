//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.project


/**
 * A variable or constant declared directly in a package specification. Only the declared type
 * syntax is retained: no initializer, `NOT NULL`, tokens or source text. [owner] is the package
 * name as written; no schema is inferred.
 */
class PackageValueDeclaration(
    val owner: QualifiedName,
    val name: OracleIdentifier,
    val constant: Boolean,
    val typeRef: TypeRef,
    override val fileId: FileId,
    override val sourceRange: SourceRange,
    /** For a constant initialized with a signed integer literal, that integer. */
    val staticDefault: Long? = null
) : ProjectDeclaration {
    override val kind = if (constant) ProjectDeclarationKind.PACKAGE_CONSTANT else ProjectDeclarationKind.PACKAGE_VARIABLE
    override val role = DeclarationRole.SPECIFICATION

    override fun equals(other: Any?): Boolean = other is PackageValueDeclaration &&
        owner == other.owner && name == other.name && constant == other.constant &&
        typeRef == other.typeRef && fileId == other.fileId && sourceRange == other.sourceRange && staticDefault == other.staticDefault

    override fun hashCode(): Int = listOf(owner, name, constant, typeRef, fileId, sourceRange, staticDefault).hashCode()

    override fun toString(): String = "PackageValueDeclaration($owner.$name, constant=$constant, $typeRef, $fileId, $sourceRange)"
}

/** An exception declared directly in a package specification (`name EXCEPTION;`). [owner] is the package name as written. */
class PackageExceptionDeclaration(
    val owner: QualifiedName,
    val name: OracleIdentifier,
    override val fileId: FileId,
    override val sourceRange: SourceRange
) : ProjectDeclaration {
    override val kind = ProjectDeclarationKind.PACKAGE_EXCEPTION
    override val role = DeclarationRole.SPECIFICATION

    override fun equals(other: Any?): Boolean = other is PackageExceptionDeclaration &&
        owner == other.owner && name == other.name && fileId == other.fileId && sourceRange == other.sourceRange

    override fun hashCode(): Int = listOf(owner, name, fileId, sourceRange).hashCode()

    override fun toString(): String = "PackageExceptionDeclaration($owner.$name, $fileId, $sourceRange)"
}

/**
 * An explicit cursor declared directly in a package specification: its `IN` parameters, its `RETURN` row type, null
 * when the declaration has none, and the [selectList] of its query, null only when the declaration writes no query (a
 * `RETURN` clause alone); a query whose select list cannot be read gives one item without a name covering the query.
 * The query itself is not retained, only its select list. [owner] is the package name as written.
 */
class PackageCursorDeclaration(
    val owner: QualifiedName,
    val name: OracleIdentifier,
    parameters: List<ProjectParameter>,
    val returnType: TypeRef?,
    val selectList: SelectList?,
    override val fileId: FileId,
    override val sourceRange: SourceRange
) : ProjectDeclaration {
    val parameters: List<ProjectParameter> = immutableList(parameters)

    override val kind = ProjectDeclarationKind.PACKAGE_CURSOR
    override val role = DeclarationRole.SPECIFICATION

    override fun equals(other: Any?): Boolean = other is PackageCursorDeclaration &&
        owner == other.owner && name == other.name && parameters == other.parameters && returnType == other.returnType &&
        selectList == other.selectList && fileId == other.fileId && sourceRange == other.sourceRange

    override fun hashCode(): Int = listOf(owner, name, parameters, returnType, selectList, fileId, sourceRange).hashCode()

    override fun toString(): String =
        "PackageCursorDeclaration($owner.$name, $parameters, $returnType, $selectList, $fileId, $sourceRange)"
}
