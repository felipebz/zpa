//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
