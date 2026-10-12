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
