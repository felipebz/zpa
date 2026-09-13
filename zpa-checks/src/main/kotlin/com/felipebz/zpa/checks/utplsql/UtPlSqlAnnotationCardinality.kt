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

internal object UtPlSqlAnnotationCardinality {

    val PACKAGE_GLOBAL_SINGLETONS = setOf(
        UtPlSqlAnnotationKind.SUITE,
        UtPlSqlAnnotationKind.SUITEPATH
    )

    val SCOPE_SINGLETONS = setOf(
        UtPlSqlAnnotationKind.DISPLAYNAME,
        UtPlSqlAnnotationKind.ROLLBACK
    )

    val PROCEDURE_SINGLETONS = setOf(
        UtPlSqlAnnotationKind.TEST,
        UtPlSqlAnnotationKind.DISPLAYNAME,
        UtPlSqlAnnotationKind.ROLLBACK,
        UtPlSqlAnnotationKind.BEFOREALL,
        UtPlSqlAnnotationKind.AFTERALL,
        UtPlSqlAnnotationKind.BEFOREEACH,
        UtPlSqlAnnotationKind.AFTEREACH
    )

    fun isEffectiveForArgumentValidation(
        packageModel: UtPlSqlPackageContextModel,
        group: UtPlSqlAnnotationGroup,
        annotation: UtPlSqlAnnotation
    ): Boolean {
        if (group.declarationNode != null) {
            return annotation.kind !in PROCEDURE_SINGLETONS ||
                isFirstOfKind(group.annotations, annotation)
        }

        return when (annotation.kind) {
            in PACKAGE_GLOBAL_SINGLETONS -> isFirstOfKind(packageModel.annotations, annotation)
            in SCOPE_SINGLETONS -> packageModel.effectiveScopeAnnotations(annotation)
                ?.let { isFirstOfKind(it, annotation) }
                ?: true
            else -> true
        }
    }

    private fun isFirstOfKind(
        annotations: List<UtPlSqlAnnotation>,
        annotation: UtPlSqlAnnotation
    ): Boolean = annotations.firstOrNull { it.kind == annotation.kind }?.token === annotation.token
}

internal fun UtPlSqlPackageContextModel.effectiveScopeAnnotations(
    annotation: UtPlSqlAnnotation
): List<UtPlSqlAnnotation>? {
    if (rootAnnotations.any { it.token === annotation.token }) return rootAnnotations
    return contexts.firstOrNull { context ->
        context.annotations.any { it.token === annotation.token }
    }?.annotations
}
