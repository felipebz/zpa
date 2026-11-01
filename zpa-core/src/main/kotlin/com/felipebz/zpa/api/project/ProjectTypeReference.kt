//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.project.*
import java.util.*

/**
 * Immutable view of retained, unresolved type syntax; not a semantic datatype. [Kind.REF] is
 * Oracle's `REF` datatype syntax, not `REF CURSOR`.
 */
@ZpaExperimentalApi
class ProjectTypeReference private constructor(
    val kind: Kind,
    val name: ProjectQualifiedName,
    val sourceRange: ProjectSourceRange,
    anchor: Anchor?,
    link: ProjectQualifiedName?
) {
    /** Present only for [Kind.ANCHORED] references. */
    val anchor: Optional<Anchor> = Optional.ofNullable(anchor)

    /**
     * The database link of a remote anchor (`t.c@link%TYPE`); present only for a [Kind.ANCHORED] reference written with
     * a link. [name] is then the anchored object on the remote database.
     */
    val link: Optional<ProjectQualifiedName> = Optional.ofNullable(link)

    enum class Kind {
        NAMED,
        ANCHORED,
        REF
    }

    enum class Anchor {
        TYPE,
        ROWTYPE
    }

    override fun toString(): String = "$kind $name" + link.map { "@$it" }.orElse("")

    internal companion object {
        @JvmSynthetic
        fun from(ref: TypeRef): ProjectTypeReference {
            val range = ref.sourceRange.toPublic()
            val name = ProjectQualifiedName.wrap(ref.name)
            return when (ref) {
                is NamedTypeRef -> ProjectTypeReference(Kind.NAMED, name, range, null, null)
                is RefTypeRef -> ProjectTypeReference(Kind.REF, name, range, null, null)
                is AnchoredTypeRef -> ProjectTypeReference(
                    Kind.ANCHORED, name, range,
                    when (ref.anchor) {
                        TypeAnchor.TYPE -> Anchor.TYPE
                        TypeAnchor.ROWTYPE -> Anchor.ROWTYPE
                    },
                    ref.link?.let(ProjectQualifiedName::wrap)
                )
            }
        }
    }
}
