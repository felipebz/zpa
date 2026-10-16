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
