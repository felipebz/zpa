//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:Suppress("DEPRECATION")

package com.felipebz.zpa.symbols

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectBuiltInTypeCategory
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.symbols.PlSqlType
import com.felipebz.zpa.project.*

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
@OptIn(ZpaExperimentalApi::class)
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
            EffectiveTypeCategory.Known(legacyType(resolution.type), EffectiveTypeSource.BUILT_IN_TYPE_REF)
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

    /** The legacy type category the typed parameters of [com.felipebz.zpa.api.matchers.MethodMatcher] compare with. */
    private fun legacyType(category: ProjectBuiltInTypeCategory): PlSqlType = when (category) {
        ProjectBuiltInTypeCategory.NUMERIC -> PlSqlType.NUMERIC
        ProjectBuiltInTypeCategory.CHARACTER -> PlSqlType.CHARACTER
        ProjectBuiltInTypeCategory.LOB -> PlSqlType.LOB
        ProjectBuiltInTypeCategory.BOOLEAN -> PlSqlType.BOOLEAN
        ProjectBuiltInTypeCategory.DATE -> PlSqlType.DATE
        ProjectBuiltInTypeCategory.JSON -> PlSqlType.JSON
    }
}
