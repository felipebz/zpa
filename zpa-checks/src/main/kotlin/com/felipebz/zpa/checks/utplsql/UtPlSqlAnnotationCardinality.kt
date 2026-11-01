//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
