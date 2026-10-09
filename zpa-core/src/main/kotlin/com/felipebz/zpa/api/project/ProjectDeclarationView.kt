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
