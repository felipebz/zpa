//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectDeclarationView
import com.felipebz.zpa.api.project.ProjectNamedTypeResolution
import com.felipebz.zpa.api.project.ProjectTypeDeclarationView
import com.felipebz.zpa.api.semantics.UsageUncertainty

/**
 * What the type name a `DATATYPE` writes denotes, as the local type walk decides it: the first declaration the name
 * reaches (a subtype is not followed to its base), or why there is none.
 */
internal sealed interface LocalTypeName {
    /** A declaration of this file; [project] is the project's fact for that same declaration, when it holds one. */
    class Local(val declaration: LocalDeclarationId, val project: ProjectDeclarationView?) : LocalTypeName {
        override fun toString() = "Local($declaration${project?.let { ", project ${it.qualifiedName}" } ?: ""})"
    }

    /** A type the project declares (a package type or subtype, or a schema type). */
    class Project(val declaration: ProjectTypeDeclarationView) : LocalTypeName {
        override fun toString() = "Project(${declaration.kind} ${declaration.qualifiedName})"
    }

    /** A predefined type name. */
    class BuiltIn(val name: String) : LocalTypeName {
        override fun toString() = "BuiltIn($name)"
    }

    /** The project has no one declaration of the name: [resolution] says why (ambiguous, not found, incomplete index). */
    class Unresolved(val resolution: ProjectNamedTypeResolution) : LocalTypeName {
        override fun toString() = "Unresolved(${resolution.status})"
    }

    /** Not decided: text this file may hold differently, or a lookup that relies on another file, or an unmodeled form. */
    class Undecided(val reasons: Set<UsageUncertainty>) : LocalTypeName {
        override fun toString() = "Undecided${reasons.sorted()}"
    }

    /** Not a named type of a declaration: an anchored type, or a `DATATYPE` outside the file's declarations. */
    data object Unavailable : LocalTypeName
}
