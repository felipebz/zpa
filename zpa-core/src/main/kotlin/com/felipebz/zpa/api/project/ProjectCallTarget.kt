//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.util.*
import com.felipebz.zpa.project.QualifiedName
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

    /** The internal target, for [translated] instead of the written name when a synonym leads elsewhere. */
    @JvmSynthetic
    internal fun toInternal(translated: QualifiedName? = null): InternalCallTarget {
        val written = translated ?: name.name
        return when (form) {
            Form.PACKAGE_MEMBER -> InternalCallTarget.packageMember(QualifiedName(written.segments.dropLast(1)), written.last)
            Form.STANDALONE -> InternalCallTarget.standalone(written)
            Form.WRITTEN -> InternalCallTarget.written(written)
        }
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
