//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/**
 * An exception declared directly in a package specification (`name EXCEPTION;`); its [kind] is
 * [ProjectDeclarationKind.PACKAGE_EXCEPTION].
 *
 * [owner] is the explicit declaring package as written in the source; no owner or schema is inferred. [localName]
 * preserves the quoted/unquoted identifier semantics of [ProjectIdentifier]. A `PRAGMA EXCEPTION_INIT` associating an
 * error number is a separate declaration and is not reflected here.
 */
@ZpaExperimentalApi
interface ProjectPackageExceptionView : ProjectDeclarationView {
    val owner: ProjectQualifiedName
    val localName: ProjectIdentifier
}
