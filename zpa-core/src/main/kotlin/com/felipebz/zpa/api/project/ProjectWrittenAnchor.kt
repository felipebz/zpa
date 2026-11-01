//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/**
 * A `%TYPE` or `%ROWTYPE` anchor written by the caller outside the indexed sources (for example in an anonymous block,
 * a package body or a local declaration). It is a request value, not retained syntax: it has no source range and none
 * is invented. Equality follows [ProjectQualifiedName] (Oracle identifier identity) and the anchor kind.
 */
@ZpaExperimentalApi
class ProjectWrittenAnchor private constructor(
    val name: ProjectQualifiedName,
    val anchor: ProjectTypeReference.Anchor
) {
    override fun equals(other: Any?): Boolean = other is ProjectWrittenAnchor && name == other.name && anchor == other.anchor

    override fun hashCode(): Int = 31 * name.hashCode() + anchor.hashCode()

    override fun toString(): String = "$name%$anchor"

    companion object {
        /** @throws NullPointerException if an argument is null */
        @JvmStatic
        fun of(name: ProjectQualifiedName, anchor: ProjectTypeReference.Anchor): ProjectWrittenAnchor =
            ProjectWrittenAnchor(name, anchor)
    }
}
