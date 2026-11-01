//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.util.*

/**
 * An explicit cursor declared directly in a package specification; its [kind] is
 * [ProjectDeclarationKind.PACKAGE_CURSOR].
 *
 * [owner] is the explicit declaring package as written in the source; no owner or schema is inferred. [localName]
 * preserves the quoted/unquoted identifier semantics of [ProjectIdentifier]. [parameters] are the cursor's formal
 * parameters in declaration order (always `IN`), possibly empty. [returnType] is the written `RETURN` row type syntax,
 * retained unresolved, and is empty when the declaration has no `RETURN` clause. The query is not retained, so the
 * row fields of a cursor without `RETURN` are not described.
 */
@ZpaExperimentalApi
interface ProjectPackageCursorView : ProjectDeclarationView {
    val owner: ProjectQualifiedName
    val localName: ProjectIdentifier
    val parameters: List<ProjectParameterView>
    val returnType: Optional<ProjectTypeReference>
}
