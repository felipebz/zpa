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

/** A project RECORD member path result; a stopped result records why the next segment was not considered. */
internal sealed interface ProjectRecordMemberPathResolution {
    val segments: List<ProjectRecordMemberPathSegment>

    class Completed(
        segments: List<ProjectRecordMemberPathSegment>
    ) : ProjectRecordMemberPathResolution {
        override val segments: List<ProjectRecordMemberPathSegment> = immutableList(segments)

        init {
            require(segments.size >= 2) { "A completed project member path requires at least two segments" }
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
        FIELD_TYPE_UNRESOLVED
    }
}

/** Composes a finite project RECORD member path from an already known base type. */
internal class ProjectRecordMemberPathResolver(
    private val memberResolver: ProjectRecordMemberResolver,
    private val fieldTypeResolver: ProjectRecordFieldTypeResolver
) {
    fun resolve(
        baseDeclaration: ProjectTypeDeclaration,
        memberNames: List<OracleIdentifier>
    ): ProjectRecordMemberPathResolution {
        require(memberNames.size >= 2) { "A project member path requires two member names" }

        val segments = mutableListOf<ProjectRecordMemberPathSegment>()
        var currentDeclaration: ProjectTypeDeclaration = baseDeclaration

        for (index in memberNames.indices) {
            val member = memberResolver.resolve(currentDeclaration, memberNames[index])
            if (member !is ProjectRecordMemberResolution.Resolved) {
                return stopped(segments, memberNames[index], reason(member))
            }

            val fieldTypeResolution = fieldTypeResolver.resolve(member)
            segments += ProjectRecordMemberPathSegment(member.field, fieldTypeResolution)

            if (index == memberNames.lastIndex) {
                return ProjectRecordMemberPathResolution.Completed(segments)
            }

            val nextDeclaration = when (val semanticResolution = fieldTypeResolution.resolution) {
                is TypeRefSemanticResolution.Project ->
                    (semanticResolution.resolution as? ProjectTypeResolution.Resolved)?.declaration
                is TypeRefSemanticResolution.BuiltIn,
                is TypeRefSemanticResolution.Unsupported -> null
            }
            if (nextDeclaration == null) {
                return stopped(segments, memberNames[index + 1], fieldTypeReason(fieldTypeResolution))
            }

            val nestedRecord = nextDeclaration as? PackageTypeDeclaration
            if (nestedRecord == null || nestedRecord.shape != ProjectTypeShape.RECORD) {
                return stopped(
                    segments,
                    memberNames[index + 1],
                    ProjectRecordMemberPathResolution.StopReason.UNSUPPORTED_TYPE
                )
            }
            currentDeclaration = nestedRecord
        }

        error("A project member path must contain at least two member names")
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

    private fun fieldTypeReason(
        resolution: ProjectRecordFieldTypeResolution
    ): ProjectRecordMemberPathResolution.StopReason =
        when (resolution.resolution) {
            is TypeRefSemanticResolution.Project -> ProjectRecordMemberPathResolution.StopReason.FIELD_TYPE_UNRESOLVED
            is TypeRefSemanticResolution.BuiltIn,
            is TypeRefSemanticResolution.Unsupported -> ProjectRecordMemberPathResolution.StopReason.UNSUPPORTED_TYPE
        }
}
