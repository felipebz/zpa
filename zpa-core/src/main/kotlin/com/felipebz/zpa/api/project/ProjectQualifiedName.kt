//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.project.QualifiedName
import java.util.*

/** Ordered, non-empty identifier segments. Equality compares the segments in order. */
@ZpaExperimentalApi
class ProjectQualifiedName private constructor(
    @get:JvmSynthetic internal val name: QualifiedName,
    segments: List<ProjectIdentifier>
) {
    /** Runtime-unmodifiable segments in source order. */
    val segments: List<ProjectIdentifier> = Collections.unmodifiableList(segments)

    val last: ProjectIdentifier get() = segments.last()

    override fun equals(other: Any?): Boolean = other is ProjectQualifiedName && name == other.name

    override fun hashCode(): Int = name.hashCode()

    /** Original spellings joined by dots; for display only, not a lookup key. */
    override fun toString(): String = name.toString()

    companion object {
        /**
         * Each argument is exactly one identifier segment; dotted strings are never split, and a
         * quoted identifier containing a dot stays one segment.
         *
         * @throws IllegalArgumentException if no segment is given or a spelling is empty
         */
        @JvmStatic
        fun of(vararg identifierSpellings: String): ProjectQualifiedName =
            ofIdentifiers(identifierSpellings.map { ProjectIdentifier.fromSource(it) })

        /** @throws IllegalArgumentException if the list is empty */
        @JvmStatic
        fun ofIdentifiers(segments: List<ProjectIdentifier>): ProjectQualifiedName {
            val copy = segments.toList()
            return ProjectQualifiedName(QualifiedName(copy.map { it.identifier }), copy)
        }

        @JvmSynthetic
        internal fun wrap(name: QualifiedName): ProjectQualifiedName =
            ProjectQualifiedName(name, name.segments.map { ProjectIdentifier.wrap(it) })
    }
}
