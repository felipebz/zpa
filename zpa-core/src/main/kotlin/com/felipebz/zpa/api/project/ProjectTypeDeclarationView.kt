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
package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.util.Optional

/** A type declaration occurrence (standalone type, package type or package subtype). */
@ZpaExperimentalApi
interface ProjectTypeDeclarationView : ProjectDeclarationView

/** Type declaration whose extractor-retained shape may be known. */
@ZpaExperimentalApi
interface ProjectShapedTypeView : ProjectTypeDeclarationView {
    /** Empty when the extractor retained no classification; this does not mean the type is invalid. */
    val shape: Optional<ProjectTypeShape>
}

/** Package specification TYPE declaration. */
@ZpaExperimentalApi
interface ProjectPackageTypeView : ProjectShapedTypeView {
    val owner: ProjectQualifiedName
    val localName: ProjectIdentifier

    /** Runtime-unmodifiable fields in source order; empty unless the shape is RECORD. */
    val recordFields: List<ProjectRecordFieldView>
}

/** Package specification SUBTYPE declaration; the base remains unresolved syntax. */
@ZpaExperimentalApi
interface ProjectSubtypeView : ProjectTypeDeclarationView {
    val owner: ProjectQualifiedName
    val localName: ProjectIdentifier
    val baseType: ProjectTypeReference
}

/** One retained record field. [ordinal] is zero-based. */
@ZpaExperimentalApi
interface ProjectRecordFieldView {
    val name: ProjectIdentifier
    val ordinal: Int
    val typeRef: ProjectTypeReference
    val sourceRange: ProjectSourceRange
}
