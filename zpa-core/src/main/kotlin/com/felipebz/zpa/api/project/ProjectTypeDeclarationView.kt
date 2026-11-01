//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.util.*

/** A type declaration occurrence (standalone type, package type or package subtype). */
@ZpaExperimentalApi
interface ProjectTypeDeclarationView : ProjectDeclarationView

/** Type declaration whose extractor-retained shape may be known. */
@ZpaExperimentalApi
interface ProjectShapedTypeView : ProjectTypeDeclarationView {
    /** Empty when the extractor retained no classification; this does not mean the type is invalid. */
    val shape: Optional<ProjectTypeShape>

    /** Direct, unresolved collection element type; empty when absent or unsupported. */
    val elementType: Optional<ProjectTypeReference>
}

/** Standalone (schema-level) `CREATE TYPE` declaration. */
@ZpaExperimentalApi
interface ProjectStandaloneTypeView : ProjectShapedTypeView {
    /**
     * Runtime-unmodifiable attributes this declaration writes, in source order; empty unless the shape is OBJECT. A
     * subtype lists its own attributes only: an inherited one is its supertype's. Each [ProjectRecordFieldView.sourceRange]
     * starts at the attribute's name.
     */
    val attributes: List<ProjectRecordFieldView>

    /**
     * False when an attribute could not be extracted, or when conditional-compilation text the configured settings do
     * not decide lies in the declaration: [attributes] may then not be those Oracle compiles. `ALTER TYPE` is not
     * reflected here: see [ProjectSnapshot.isAttributeListAltered].
     */
    val attributesComplete: Boolean
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
