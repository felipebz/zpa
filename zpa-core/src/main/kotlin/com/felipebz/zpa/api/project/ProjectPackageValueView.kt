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
