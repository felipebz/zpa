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
 * Members resolved for the field query alone, on a `MEMBER_EXPRESSION` whose names from [firstMember] on (0 for the
 * first name) are members of a value's type: [member] for one, [path] for several. They are the members after a
 * package variable the local model cannot type (one of another file, or of the specification of the package around
 * the code when it is in another file), and those of the name of a call (`v.f.g(1)`), which the member path visitor
 * leaves undecorated.
 */
internal class ProjectFieldMembers private constructor(
    val firstMember: Int,
    val member: ProjectRecordMemberResolution?,
    val path: ProjectRecordMemberPathResolution?
) {
    override fun toString() = "ProjectFieldMembers(from $firstMember, ${member ?: path})"

    companion object {
        fun of(firstMember: Int, member: ProjectRecordMemberResolution) = ProjectFieldMembers(firstMember, member, null)

        fun of(firstMember: Int, path: ProjectRecordMemberPathResolution) = ProjectFieldMembers(firstMember, null, path)
    }
}
