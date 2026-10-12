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
import java.util.*
import com.felipebz.zpa.project.ProjectCallTarget as InternalCallTarget

/**
 * Which namespace the caller asserts for a call target. Nothing is inferred from files, schemas, locations or the
 * catalog. Equality follows [ProjectQualifiedName] equality, so `p.run` and `"P"."RUN"` are the same target while
 * `p."run"` is not.
 */
@ZpaExperimentalApi
class ProjectCallTarget private constructor(
    val form: Form,
    /** The full requested name; for [Form.PACKAGE_MEMBER] it is the package name followed by the member. */
    val name: ProjectQualifiedName,
    private val packageOwner: ProjectQualifiedName?
) {
    enum class Form { PACKAGE_MEMBER, STANDALONE, WRITTEN }

    /** Present only for [Form.PACKAGE_MEMBER]. */
    val packageName: Optional<ProjectQualifiedName> get() = Optional.ofNullable(packageOwner)

    @JvmSynthetic
    internal fun toInternal(): InternalCallTarget = when (form) {
        Form.PACKAGE_MEMBER -> InternalCallTarget.packageMember(requireNotNull(packageOwner).name, name.name.last)
        Form.STANDALONE -> InternalCallTarget.standalone(name.name)
        Form.WRITTEN -> InternalCallTarget.written(name.name)
    }

    override fun equals(other: Any?): Boolean = other is ProjectCallTarget && form == other.form && name == other.name

    override fun hashCode(): Int = 31 * form.hashCode() + name.hashCode()

    override fun toString(): String = "$form $name"

    companion object {
        /** `packageName.memberName`; [packageName] may itself be qualified (`a.p`). */
        @JvmStatic
        fun packageMember(packageName: ProjectQualifiedName, memberName: ProjectIdentifier): ProjectCallTarget =
            ProjectCallTarget(
                Form.PACKAGE_MEMBER,
                ProjectQualifiedName.wrap(packageName.name.append(memberName.identifier)),
                packageName
            )

        /** A standalone procedure/function as written (one or two segments; longer names have no interpretation). */
        @JvmStatic
        fun standalone(qualifiedName: ProjectQualifiedName): ProjectCallTarget =
            ProjectCallTarget(Form.STANDALONE, qualifiedName, null)

        /** Every interpretation allowed by the segment count (one to three segments; longer names have none). */
        @JvmStatic
        fun written(qualifiedName: ProjectQualifiedName): ProjectCallTarget =
            ProjectCallTarget(Form.WRITTEN, qualifiedName, null)
    }
}
