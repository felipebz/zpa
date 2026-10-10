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
import com.felipebz.zpa.project.AnchoredTypeRef
import com.felipebz.zpa.project.NamedTypeRef
import com.felipebz.zpa.project.RefTypeRef
import com.felipebz.zpa.project.TypeAnchor
import com.felipebz.zpa.project.TypeRef
import java.util.Optional

/**
 * Immutable view of retained, unresolved type syntax; not a semantic datatype. [Kind.REF] is
 * Oracle's `REF` datatype syntax, not `REF CURSOR`.
 */
@ZpaExperimentalApi
class ProjectTypeReference private constructor(
    val kind: Kind,
    val name: ProjectQualifiedName,
    val sourceRange: ProjectSourceRange,
    anchor: Anchor?
) {
    /** Present only for [Kind.ANCHORED] references. */
    val anchor: Optional<Anchor> = Optional.ofNullable(anchor)

    enum class Kind {
        NAMED,
        ANCHORED,
        REF
    }

    enum class Anchor {
        TYPE,
        ROWTYPE
    }

    override fun toString(): String = "$kind $name"

    internal companion object {
        @JvmSynthetic
        fun from(ref: TypeRef): ProjectTypeReference {
            val range = ref.sourceRange.toPublic()
            val name = ProjectQualifiedName.wrap(ref.name)
            return when (ref) {
                is NamedTypeRef -> ProjectTypeReference(Kind.NAMED, name, range, null)
                is RefTypeRef -> ProjectTypeReference(Kind.REF, name, range, null)
                is AnchoredTypeRef -> ProjectTypeReference(
                    Kind.ANCHORED, name, range,
                    when (ref.anchor) {
                        TypeAnchor.TYPE -> Anchor.TYPE
                        TypeAnchor.ROWTYPE -> Anchor.ROWTYPE
                    }
                )
            }
        }
    }
}
