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
