//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/**
 * Read-only view of one indexed declaration occurrence. Output contract only: views are not
 * cached, so use [id] (not object identity) to compare occurrences.
 */
@ZpaExperimentalApi
interface ProjectDeclarationView {
    val id: ProjectDeclarationId
    val kind: ProjectDeclarationKind
    val role: ProjectDeclarationRole

    /** Full qualified name as indexed (package members include their owner). */
    val qualifiedName: ProjectQualifiedName
    val fileId: String
    val sourceRange: ProjectSourceRange
}
