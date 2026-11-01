//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/**
 * Category of the retained Project System fact, not an enumeration of Oracle declaration
 * categories. `PACKAGE_BODY` is representable but the current extractor does not emit it;
 * `STANDALONE_PROCEDURE` and `STANDALONE_FUNCTION` are standalone schema-level callables
 * (`CREATE PROCEDURE` / `CREATE FUNCTION`), never members of a package.
 * `PACKAGE_VARIABLE` and `PACKAGE_CONSTANT` are variables and constants declared in a package specification;
 * `PACKAGE_EXCEPTION` and `PACKAGE_CURSOR` are exceptions and explicit cursors declared in a package specification.
 */
@ZpaExperimentalApi
enum class ProjectDeclarationKind {
    PACKAGE,
    PACKAGE_BODY,
    STANDALONE_TYPE,
    PACKAGE_TYPE,
    PACKAGE_SUBTYPE,
    PACKAGE_PROCEDURE,
    PACKAGE_FUNCTION,
    SEQUENCE,
    STANDALONE_PROCEDURE,
    STANDALONE_FUNCTION,
    PACKAGE_VARIABLE,
    PACKAGE_CONSTANT,
    PACKAGE_EXCEPTION,
    PACKAGE_CURSOR
}
