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
package com.felipebz.zpa.project

/**
 * The members written after a package variable of another project file: [member] for one (`pkg.v.f`), [path] for
 * several (`pkg.v.f.g`), resolved on the variable's type by the project member resolvers.
 */
internal class ProjectPackageValueMembers private constructor(
    val member: ProjectRecordMemberResolution?,
    val path: ProjectRecordMemberPathResolution?
) {
    override fun toString() = "ProjectPackageValueMembers(${member ?: path})"

    companion object {
        fun of(member: ProjectRecordMemberResolution) = ProjectPackageValueMembers(member, null)

        fun of(path: ProjectRecordMemberPathResolution) = ProjectPackageValueMembers(null, path)
    }
}
