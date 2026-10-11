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
import com.felipebz.zpa.api.symbols.PlSqlType
import com.felipebz.zpa.project.BuiltInTypeRefClassifier

/**
 * One finite, iterative walk over subtype aliases. Named steps go through [ProjectSnapshot.resolveNamedType], so lookup
 * rules, candidate counting and failure precedence are exactly the existing ones; built-ins use the existing
 * [BuiltInTypeRefClassifier] before any project lookup. Nothing is cached or retained.
 */
@OptIn(ZpaExperimentalApi::class)
internal object ProjectTypeDescriber {
    @JvmSynthetic
    fun describe(
        snapshot: ProjectSnapshot,
        startName: ProjectQualifiedName,
        startReference: ProjectTypeReference?,
        startOwner: ProjectQualifiedName?
    ): ProjectTypeDescription {
        val followed = LinkedHashMap<ProjectDeclarationId, ProjectSubtypeView>()
        var name = startName
        var reference = startReference
        var owner = startOwner
        while (true) {
            when (reference?.kind) {
                ProjectTypeReference.Kind.ANCHORED ->
                    return ProjectTypeDescription.stopped(ProjectTypeDescription.Status.ANCHORED_REFERENCE, followed.values.toList(), reference)
                ProjectTypeReference.Kind.REF ->
                    return ProjectTypeDescription.stopped(ProjectTypeDescription.Status.REF_REFERENCE, followed.values.toList(), reference)
                else -> Unit
            }
            category(name)?.let { return ProjectTypeDescription.builtIn(followed.values.toList(), it, reference) }
            val resolution = snapshot.resolveNamedType(name, owner)
            if (resolution.status != ProjectNamedTypeResolution.Status.RESOLVED) {
                return ProjectTypeDescription.unresolved(followed.values.toList(), resolution, reference)
            }
            val declaration = resolution.declaration.get()
            if (declaration !is ProjectSubtypeView) {
                return ProjectTypeDescription.declared(followed.values.toList(), declaration, reference)
            }
            if (followed.containsKey(declaration.id)) {
                return ProjectTypeDescription.cycle(followed.values.toList(), reference)
            }
            followed[declaration.id] = declaration
            reference = declaration.baseType
            name = declaration.baseType.name
            owner = declaration.owner
        }
    }

    private fun category(name: ProjectQualifiedName): ProjectBuiltInTypeCategory? = when (BuiltInTypeRefClassifier.classify(name.name)) {
        PlSqlType.NUMERIC -> ProjectBuiltInTypeCategory.NUMERIC
        PlSqlType.CHARACTER -> ProjectBuiltInTypeCategory.CHARACTER
        PlSqlType.LOB -> ProjectBuiltInTypeCategory.LOB
        PlSqlType.BOOLEAN -> ProjectBuiltInTypeCategory.BOOLEAN
        PlSqlType.DATE -> ProjectBuiltInTypeCategory.DATE
        PlSqlType.JSON -> ProjectBuiltInTypeCategory.JSON
        else -> null
    }
}
