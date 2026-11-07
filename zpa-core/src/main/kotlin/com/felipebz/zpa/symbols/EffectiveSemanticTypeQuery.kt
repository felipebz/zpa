//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:Suppress("DEPRECATION")

package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.semantics.RecordOrigin
import com.felipebz.zpa.api.semantics.ScalarFamily
import com.felipebz.zpa.api.semantics.ValueCategory
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.symbols.PlSqlType
import com.felipebz.zpa.project.PackageTypeDeclaration
import com.felipebz.zpa.project.ProjectTypeDeclaration
import com.felipebz.zpa.project.ProjectTypeShape

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

/** Interprets the legacy decorations and the local value categories without performing another resolution pass. */
@OptIn(ZpaExperimentalApi::class)
internal object EffectiveSemanticTypeQuery {
    fun typeCategory(node: SemanticAstNode): EffectiveTypeCategory {
        node.plSqlType.takeUnless { it == PlSqlType.UNKNOWN }?.let {
            return EffectiveTypeCategory.Known(it, EffectiveTypeSource.LEGACY)
        }

        projectCategory(node)?.let { return it }

        node.symbol?.projectTypeDeclaration?.let {
            return typeCategory(it)
        }

        return EffectiveTypeCategory.Unknown
    }

    /**
     * The category of the value [node] gives when it is a field or attribute of a value whose type the project declares
     * (as the local type walk reaches them): a predefined type's family, or a package record type. Null otherwise, and for
     * any other category (an object, a collection), which the legacy fallback answers.
     */
    private fun projectCategory(node: SemanticAstNode): EffectiveTypeCategory? {
        var root: AstNode = node
        while (true) root = root.parentOrNull ?: break
        val categories = (root as? SemanticAstNode)?.localAnalysis?.categories ?: return null
        return when (val category = categories.projectMemberCategory(node) ?: return null) {
            is ValueCategory.Scalar -> category.family?.let { EffectiveTypeCategory.Known(legacyType(it), EffectiveTypeSource.BUILT_IN_TYPE_REF) }
            is ValueCategory.Record ->
                EffectiveTypeCategory.Known(PlSqlType.RECORD, EffectiveTypeSource.PROJECT_DECLARATION).takeIf { category.origin == RecordOrigin.RECORD_TYPE }
            else -> null
        }
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
    private fun legacyType(family: ScalarFamily): PlSqlType = when (family) {
        ScalarFamily.NUMERIC -> PlSqlType.NUMERIC
        ScalarFamily.CHARACTER, ScalarFamily.RAW, ScalarFamily.ROWID -> PlSqlType.CHARACTER
        ScalarFamily.CLOB, ScalarFamily.BLOB -> PlSqlType.LOB
        ScalarFamily.BOOLEAN -> PlSqlType.BOOLEAN
        ScalarFamily.DATETIME, ScalarFamily.INTERVAL -> PlSqlType.DATE
        ScalarFamily.JSON -> PlSqlType.JSON
    }
}
