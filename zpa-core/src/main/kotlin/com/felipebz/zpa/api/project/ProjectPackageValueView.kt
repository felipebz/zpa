//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/**
 * A variable or constant declared directly in a package specification.
 *
 * [owner] is the explicit declaring package as written in the source; no owner or schema is inferred.
 * [localName] preserves the quoted/unquoted identifier semantics of [ProjectIdentifier]. [isConstant] reports the
 * declared `CONSTANT` modifier (the [kind] is then [ProjectDeclarationKind.PACKAGE_CONSTANT]). [typeRef] is the
 * declared type syntax, retained unresolved; it is not a resolved or evaluated type.
 *
 * No initializer value, initializer presence, nullability or Oracle compile-time validity is promised.
 */
@ZpaExperimentalApi
interface ProjectPackageValueView : ProjectDeclarationView {
    val owner: ProjectQualifiedName
    val localName: ProjectIdentifier
    val isConstant: Boolean
    val typeRef: ProjectTypeReference
}
