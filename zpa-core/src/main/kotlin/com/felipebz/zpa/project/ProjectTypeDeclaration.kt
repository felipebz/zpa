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

enum class ProjectTypeShape {
    RECORD,
    OBJECT,
    COLLECTION,
    REF_CURSOR
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
    override val sourceRange: SourceRange
) : ProjectTypeDeclaration {
    override val kind = ProjectDeclarationKind.STANDALONE_TYPE
    override val role = DeclarationRole.STANDALONE
    override val qualifiedName: QualifiedName = name
}

class PackageTypeDeclaration(
    val owner: QualifiedName,
    val name: OracleIdentifier,
    val shape: ProjectTypeShape?,
    recordFields: List<ProjectRecordField>,
    override val fileId: FileId,
    override val sourceRange: SourceRange
) : ProjectTypeDeclaration {

    val recordFields: List<ProjectRecordField> = immutableList(recordFields)
    override val kind = ProjectDeclarationKind.PACKAGE_TYPE
    override val role = DeclarationRole.SPECIFICATION
    override val qualifiedName: QualifiedName = owner.append(name)

    init {
        require(shape == ProjectTypeShape.RECORD || recordFields.isEmpty()) {
            "Only RECORD declarations may contain record fields"
        }
    }

    override fun equals(other: Any?): Boolean = other is PackageTypeDeclaration &&
        owner == other.owner && name == other.name && shape == other.shape &&
        recordFields == other.recordFields && fileId == other.fileId && sourceRange == other.sourceRange

    override fun hashCode(): Int = listOf(owner, name, shape, recordFields, fileId, sourceRange).hashCode()

    override fun toString(): String =
        "PackageTypeDeclaration($qualifiedName, $shape, $recordFields, $fileId, $sourceRange)"
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
