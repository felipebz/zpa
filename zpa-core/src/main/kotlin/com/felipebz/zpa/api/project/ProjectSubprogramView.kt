//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** Reusable callable signature facet; carries no package membership. */
@ZpaExperimentalApi
interface ProjectSubprogramView : ProjectDeclarationView {
    val localName: ProjectIdentifier

    /** Runtime-unmodifiable formals in declaration order. */
    val parameters: List<ProjectParameterView>
}

/** Function facet: return type syntax and retained DETERMINISTIC metadata. */
@ZpaExperimentalApi
interface ProjectFunctionView : ProjectSubprogramView {
    val returnType: ProjectTypeReference

    /** Retained metadata only; body declarations are not populated with it by the extractor. */
    val isDeterministic: Boolean
}

/** Package membership facet; [packageOwner] is the declaring package as retained by the fact. */
@ZpaExperimentalApi
interface ProjectPackageSubprogramView : ProjectSubprogramView {
    val packageOwner: ProjectQualifiedName
}

/** One retained formal parameter. [ordinal] is one-based; no default expression is exposed. */
@ZpaExperimentalApi
interface ProjectParameterView {
    val name: ProjectIdentifier
    val ordinal: Int
    val mode: ProjectParameterMode
    val isNocopy: Boolean
    val typeRef: ProjectTypeReference
    val isDefaultPresent: Boolean
    val sourceRange: ProjectSourceRange
}

@ZpaExperimentalApi
enum class ProjectParameterMode {
    IN,
    OUT,
    IN_OUT
}
