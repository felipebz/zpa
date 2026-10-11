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
    override val sourceRange: SourceRange
) : ProjectDeclaration {
    override val kind = if (constant) ProjectDeclarationKind.PACKAGE_CONSTANT else ProjectDeclarationKind.PACKAGE_VARIABLE
    override val role = DeclarationRole.SPECIFICATION

    override fun equals(other: Any?): Boolean = other is PackageValueDeclaration &&
        owner == other.owner && name == other.name && constant == other.constant &&
        typeRef == other.typeRef && fileId == other.fileId && sourceRange == other.sourceRange

    override fun hashCode(): Int = listOf(owner, name, constant, typeRef, fileId, sourceRange).hashCode()

    override fun toString(): String = "PackageValueDeclaration($owner.$name, constant=$constant, $typeRef, $fileId, $sourceRange)"
}
