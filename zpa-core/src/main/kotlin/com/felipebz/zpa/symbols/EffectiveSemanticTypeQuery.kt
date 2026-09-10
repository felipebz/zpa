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
package com.felipebz.zpa.symbols

import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.symbols.PlSqlType
import com.felipebz.zpa.project.PackageTypeDeclaration
import com.felipebz.zpa.project.ProjectRecordFieldTypeResolution
import com.felipebz.zpa.project.ProjectRecordMemberPathResolution
import com.felipebz.zpa.project.ProjectTypeDeclaration
import com.felipebz.zpa.project.ProjectTypeResolution
import com.felipebz.zpa.project.ProjectTypeShape
import com.felipebz.zpa.project.TypeRefSemanticResolution

/** The source from which an effective broad type category was proven. */
internal enum class EffectiveTypeSource {
    LEGACY,
    BUILT_IN_TYPE_REF,
    PROJECT_DECLARATION
}

/** A deliberately small result for querying only a node's broad semantic type category. */
internal sealed interface EffectiveTypeCategory {
    data class Known(
        val type: PlSqlType,
        val source: EffectiveTypeSource
    ) : EffectiveTypeCategory

    data object Unknown : EffectiveTypeCategory
}

/** Interprets existing semantic decorations without performing another resolution pass. */
internal object EffectiveSemanticTypeQuery {
    fun typeCategory(node: SemanticAstNode): EffectiveTypeCategory {
        node.plSqlType.takeUnless { it == PlSqlType.UNKNOWN }?.let {
            return EffectiveTypeCategory.Known(it, EffectiveTypeSource.LEGACY)
        }

        node.projectRecordMemberPathResolution?.let { path ->
            return when (path) {
                is ProjectRecordMemberPathResolution.Completed ->
                    path.segments.lastOrNull()?.fieldTypeResolution?.let(::typeCategory)
                        ?: EffectiveTypeCategory.Unknown
                is ProjectRecordMemberPathResolution.Stopped ->
                    EffectiveTypeCategory.Unknown
            }
        }

        node.projectRecordFieldTypeResolution?.let {
            return typeCategory(it)
        }

        node.projectTypeResolution?.let {
            return typeCategory(it)
        }

        node.symbol?.projectTypeDeclaration?.let {
            return typeCategory(it)
        }

        return EffectiveTypeCategory.Unknown
    }

    private fun typeCategory(resolution: ProjectRecordFieldTypeResolution): EffectiveTypeCategory =
        typeCategory(resolution.resolution)

    private fun typeCategory(resolution: TypeRefSemanticResolution): EffectiveTypeCategory = when (resolution) {
        is TypeRefSemanticResolution.BuiltIn ->
            EffectiveTypeCategory.Known(resolution.type, EffectiveTypeSource.BUILT_IN_TYPE_REF)
        is TypeRefSemanticResolution.Project -> typeCategory(resolution.resolution)
        is TypeRefSemanticResolution.Unsupported -> EffectiveTypeCategory.Unknown
    }

    private fun typeCategory(resolution: ProjectTypeResolution): EffectiveTypeCategory = when (resolution) {
        is ProjectTypeResolution.Resolved -> typeCategory(resolution.declaration)
        is ProjectTypeResolution.Ambiguous,
        is ProjectTypeResolution.IncompleteIndex,
        is ProjectTypeResolution.NotFoundInProject,
        is ProjectTypeResolution.NotPrepared -> EffectiveTypeCategory.Unknown
    }

    private fun typeCategory(declaration: ProjectTypeDeclaration): EffectiveTypeCategory = when (declaration) {
        is PackageTypeDeclaration -> if (declaration.shape == ProjectTypeShape.RECORD) {
            EffectiveTypeCategory.Known(PlSqlType.RECORD, EffectiveTypeSource.PROJECT_DECLARATION)
        } else {
            EffectiveTypeCategory.Unknown
        }
        else -> EffectiveTypeCategory.Unknown
    }
}
