//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.semantics.WrittenTypeConstraint
import com.felipebz.zpa.api.semantics.WrittenTypeConstraints
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
    link: ProjectQualifiedName?,
    /** The normalized tokens of a named type written as more than a plain name (`TIMESTAMP(6) WITH TIME ZONE`). */
    @get:JvmSynthetic internal val writtenIdentity: String? = null,
    @get:JvmSynthetic internal val writtenSyntax: WrittenTypeSyntax? = null
) {
    /**
     * For a [Kind.NAMED] reference written as a predefined type, its written length, length semantics, precision and
     * scale (see [WrittenTypeConstraints]). The index keeps no constant's value, so a constraint written as a constant
     * or an expression is [WrittenTypeConstraint.Unknown]. Empty for an anchor, a `REF`, and a name that is not a
     * predefined type (a subtype or a type the source declares, whose constraints the declaration decides).
     */
    val constraints: Optional<WrittenTypeConstraints>
        get() = Optional.ofNullable(writtenSyntax?.constraints { WrittenTypeConstraint.Unknown })

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
                is NamedTypeRef -> ProjectTypeReference(Kind.NAMED, name, range, null, null, ref.headerTypeIdentity, ref.writtenSyntax)
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
