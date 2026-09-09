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

/** A resolved project RECORD member segment and the type fact for that field. */
internal data class ProjectRecordMemberPathSegment(
    val field: ProjectRecordField,
    val fieldTypeResolution: ProjectRecordFieldTypeResolution
)

/**
 * The bounded result of composing project RECORD members. It can contain at most two
 * resolved segments; a stopped result records why the next segment was not considered.
 */
internal sealed interface ProjectRecordMemberPathResolution {
    val segments: List<ProjectRecordMemberPathSegment>

    class Completed(
        segments: List<ProjectRecordMemberPathSegment>
    ) : ProjectRecordMemberPathResolution {
        override val segments: List<ProjectRecordMemberPathSegment> = immutableList(segments)

        init {
            require(segments.size == 2) { "A completed project member path requires two segments" }
        }

        override fun equals(other: Any?): Boolean = other is Completed && segments == other.segments

        override fun hashCode(): Int = segments.hashCode()

        override fun toString(): String = "Completed($segments)"
    }

    class Stopped(
        segments: List<ProjectRecordMemberPathSegment>,
        val nextMember: OracleIdentifier,
        val nextMemberOrdinal: Int,
        val reason: StopReason
    ) : ProjectRecordMemberPathResolution {
        override val segments: List<ProjectRecordMemberPathSegment> = immutableList(segments)

        init {
            require(segments.size <= 2) { "A project member path can retain at most two segments" }
            require(nextMemberOrdinal == segments.size) {
                "The next member ordinal must follow the resolved segments"
            }
        }

        override fun equals(other: Any?): Boolean = other is Stopped &&
            segments == other.segments && nextMember == other.nextMember &&
            nextMemberOrdinal == other.nextMemberOrdinal && reason == other.reason

        override fun hashCode(): Int = listOf(segments, nextMember, nextMemberOrdinal, reason).hashCode()

        override fun toString(): String = "Stopped($segments, $nextMember, $nextMemberOrdinal, $reason)"
    }

    enum class StopReason {
        MEMBER_NOT_FOUND,
        MEMBER_AMBIGUOUS,
        UNSUPPORTED_TYPE,
        FIELD_TYPE_UNRESOLVED,
        HOP_LIMIT
    }
}

/** Composes at most two project RECORD member lookups from an already known base type. */
internal class ProjectRecordMemberPathResolver(
    private val memberResolver: ProjectRecordMemberResolver,
    private val fieldTypeResolver: ProjectRecordFieldTypeResolver
) {
    fun resolve(
        baseDeclaration: ProjectTypeDeclaration,
        memberNames: List<OracleIdentifier>
    ): ProjectRecordMemberPathResolution {
        require(memberNames.size >= 2) { "A project member path requires two member names" }

        val first = memberResolver.resolve(baseDeclaration, memberNames[0])
        if (first !is ProjectRecordMemberResolution.Resolved) {
            return stopped(emptyList(), memberNames[0], reason(first))
        }

        val firstType = fieldTypeResolver.resolve(first)
        val firstSegment = ProjectRecordMemberPathSegment(first.field, firstType)
        val firstTypeDeclaration = when (firstType) {
            is ProjectRecordFieldTypeResolution.Named ->
                (firstType.resolution as? ProjectTypeResolution.Resolved)?.declaration
            is ProjectRecordFieldTypeResolution.Unsupported -> null
        }
        if (firstTypeDeclaration == null) {
            val reason = if (firstType is ProjectRecordFieldTypeResolution.Unsupported) {
                ProjectRecordMemberPathResolution.StopReason.UNSUPPORTED_TYPE
            } else {
                ProjectRecordMemberPathResolution.StopReason.FIELD_TYPE_UNRESOLVED
            }
            return stopped(listOf(firstSegment), memberNames[1], reason)
        }

        val nestedRecord = firstTypeDeclaration as? PackageTypeDeclaration
        if (nestedRecord == null || nestedRecord.shape != ProjectTypeShape.RECORD) {
            return stopped(
                listOf(firstSegment),
                memberNames[1],
                ProjectRecordMemberPathResolution.StopReason.UNSUPPORTED_TYPE
            )
        }

        val second = memberResolver.resolve(nestedRecord, memberNames[1])
        if (second !is ProjectRecordMemberResolution.Resolved) {
            return stopped(
                listOf(firstSegment),
                memberNames[1],
                reason(second)
            )
        }

        val segments = listOf(
            firstSegment,
            ProjectRecordMemberPathSegment(
                second.field,
                fieldTypeResolver.resolve(second)
            )
        )
        if (memberNames.size > 2) {
            return stopped(
                segments,
                memberNames[2],
                ProjectRecordMemberPathResolution.StopReason.HOP_LIMIT
            )
        }
        return ProjectRecordMemberPathResolution.Completed(segments)
    }

    private fun stopped(
        segments: List<ProjectRecordMemberPathSegment>,
        nextMember: OracleIdentifier,
        reason: ProjectRecordMemberPathResolution.StopReason
    ) = ProjectRecordMemberPathResolution.Stopped(segments, nextMember, segments.size, reason)

    private fun reason(resolution: ProjectRecordMemberResolution): ProjectRecordMemberPathResolution.StopReason =
        when (resolution) {
            is ProjectRecordMemberResolution.NotFound -> ProjectRecordMemberPathResolution.StopReason.MEMBER_NOT_FOUND
            is ProjectRecordMemberResolution.Ambiguous -> ProjectRecordMemberPathResolution.StopReason.MEMBER_AMBIGUOUS
            is ProjectRecordMemberResolution.UnsupportedType -> ProjectRecordMemberPathResolution.StopReason.UNSUPPORTED_TYPE
            is ProjectRecordMemberResolution.Resolved -> error("Resolved member was not expected here")
        }
}
