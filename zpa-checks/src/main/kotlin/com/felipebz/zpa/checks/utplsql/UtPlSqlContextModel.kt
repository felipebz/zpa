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
package com.felipebz.zpa.checks.utplsql

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import java.util.Collections
import java.util.IdentityHashMap

internal class UtPlSqlContext(
    val openingAnnotation: UtPlSqlAnnotation,
    val closingAnnotation: UtPlSqlAnnotation?,
    val parentIndex: Int?,
    childIndices: List<Int>,
    selectedNameAnnotation: UtPlSqlAnnotation?,
    duplicateNameAnnotations: List<UtPlSqlAnnotation>,
    lateNameAnnotations: List<UtPlSqlAnnotation>
) {
    val childIndices: List<Int> = Collections.unmodifiableList(childIndices.toList())
    val selectedNameAnnotation: UtPlSqlAnnotation? = selectedNameAnnotation
    val duplicateNameAnnotations: List<UtPlSqlAnnotation> =
        Collections.unmodifiableList(duplicateNameAnnotations.toList())
    val lateNameAnnotations: List<UtPlSqlAnnotation> =
        Collections.unmodifiableList(lateNameAnnotations.toList())
}

internal class UtPlSqlPackageContextModel internal constructor(
    val packageNode: AstNode,
    annotations: List<UtPlSqlAnnotation>,
    contexts: List<UtPlSqlContext>,
    unmatchedEndContextAnnotations: List<UtPlSqlAnnotation>,
    nameAnnotationsOutsideContexts: List<UtPlSqlAnnotation>,
    duplicateContextAnnotations: List<UtPlSqlAnnotation>
) {
    val annotations: List<UtPlSqlAnnotation> = Collections.unmodifiableList(annotations.toList())
    val contexts: List<UtPlSqlContext> = Collections.unmodifiableList(contexts.toList())
    val unmatchedEndContextAnnotations: List<UtPlSqlAnnotation> =
        Collections.unmodifiableList(unmatchedEndContextAnnotations.toList())
    val nameAnnotationsOutsideContexts: List<UtPlSqlAnnotation> =
        Collections.unmodifiableList(nameAnnotationsOutsideContexts.toList())
    val duplicateContextAnnotations: List<UtPlSqlAnnotation> =
        Collections.unmodifiableList(duplicateContextAnnotations.toList())
}

internal class UtPlSqlContextModel internal constructor(
    packages: List<UtPlSqlPackageContextModel>
) {
    val packages: List<UtPlSqlPackageContextModel> = Collections.unmodifiableList(packages.toList())
}

internal object UtPlSqlContextCollector {

    fun collect(root: AstNode): UtPlSqlContextModel {
        val annotationModel = UtPlSqlAnnotationCollector.collect(root)
        val groupsByPackage = IdentityHashMap<AstNode, MutableList<UtPlSqlAnnotationGroup>>()
        val packageOrder = mutableListOf<AstNode>()

        annotationModel.groups
            .filter { it.packageNode.type == PlSqlGrammar.CREATE_PACKAGE }
            .forEach { group ->
                val groups = groupsByPackage[group.packageNode]
                if (groups == null) {
                    groupsByPackage[group.packageNode] = mutableListOf(group)
                    packageOrder += group.packageNode
                } else {
                    groups += group
                }
            }

        return UtPlSqlContextModel(
            packageOrder.map { packageNode ->
                buildPackage(packageNode, groupsByPackage.getValue(packageNode))
            }
        )
    }

    private fun buildPackage(
        packageNode: AstNode,
        groups: List<UtPlSqlAnnotationGroup>
    ): UtPlSqlPackageContextModel {
        val annotations = groups
            .filter { it.declarationNode == null }
            .flatMap { it.annotations }
        val builders = mutableListOf<ContextBuilder>()
        val openContexts = ArrayDeque<ContextBuilder>()
        val unmatchedEndContexts = mutableListOf<UtPlSqlAnnotation>()
        val namesOutsideContexts = mutableListOf<UtPlSqlAnnotation>()
        val lastEndContextPosition = annotations.indexOfLast {
            it.kind == UtPlSqlAnnotationKind.ENDCONTEXT
        }

        annotations.forEachIndexed { position, annotation ->
            when (annotation.kind) {
                UtPlSqlAnnotationKind.CONTEXT -> {
                    openContexts.lastOrNull()?.nameWindowClosed = true
                    val context = ContextBuilder(annotation, position, openContexts.lastOrNull())
                    openContexts.lastOrNull()?.children?.add(context)
                    builders += context
                    openContexts.addLast(context)
                }

                UtPlSqlAnnotationKind.ENDCONTEXT -> {
                    val context = openContexts.removeLastOrNull()
                    if (context == null) {
                        unmatchedEndContexts += annotation
                    } else {
                        context.closingAnnotation = annotation
                    }
                }

                UtPlSqlAnnotationKind.NAME -> {
                    val context = openContexts.lastOrNull()
                    if (context == null) {
                        namesOutsideContexts += annotation
                    } else if (context.nameWindowClosed) {
                        context.lateNameAnnotations += annotation
                    } else {
                        context.nameAnnotations += annotation
                    }
                }

                else -> Unit
            }
        }

        val recoveryContextIndex = builders.indexOfFirst { it.position > lastEndContextPosition }
        val effectiveBuilders = if (recoveryContextIndex >= 0) {
            builders.take(recoveryContextIndex + 1)
        } else {
            builders
        }
        val duplicateContextAnnotations = if (recoveryContextIndex >= 0) {
            builders.drop(recoveryContextIndex + 1).map { it.openingAnnotation }
        } else {
            emptyList()
        }

        val indexByBuilder = IdentityHashMap<ContextBuilder, Int>()
        effectiveBuilders.forEachIndexed { index, builder -> indexByBuilder[builder] = index }
        val contexts = effectiveBuilders.map { builder ->
            UtPlSqlContext(
                openingAnnotation = builder.openingAnnotation,
                closingAnnotation = builder.closingAnnotation,
                parentIndex = builder.parent?.let(indexByBuilder::getValue),
                childIndices = builder.children.mapNotNull { indexByBuilder[it] },
                selectedNameAnnotation = builder.nameAnnotations.firstOrNull(),
                duplicateNameAnnotations = builder.nameAnnotations.drop(1),
                lateNameAnnotations = builder.lateNameAnnotations
            )
        }

        return UtPlSqlPackageContextModel(
            packageNode = packageNode,
            annotations = annotations,
            contexts = contexts,
            unmatchedEndContextAnnotations = unmatchedEndContexts,
            nameAnnotationsOutsideContexts = namesOutsideContexts,
            duplicateContextAnnotations = duplicateContextAnnotations
        )
    }

    private class ContextBuilder(
        val openingAnnotation: UtPlSqlAnnotation,
        val position: Int,
        val parent: ContextBuilder?
    ) {
        var closingAnnotation: UtPlSqlAnnotation? = null
        val children = mutableListOf<ContextBuilder>()
        val nameAnnotations = mutableListOf<UtPlSqlAnnotation>()
        val lateNameAnnotations = mutableListOf<UtPlSqlAnnotation>()
        var nameWindowClosed = false
    }
}
