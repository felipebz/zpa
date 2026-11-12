//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.semantics

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectExternalTypeDescription
import com.felipebz.zpa.api.project.ProjectQualifiedName
import com.felipebz.zpa.api.project.ProjectTypeDeclarationView
import com.felipebz.zpa.api.project.ProjectTypeReference

/**
 * What a written datatype declares, following subtypes and `%TYPE` anchors to where they end, as the local name
 * resolution and, for declarations in other files, the prepared project decide it. [Unknown] is never a proof.
 */
@ZpaExperimentalApi
sealed interface DeclaredType {
    /**
     * A predefined type. [name] is its words upper-cased and without constraints (`VARCHAR2`, `TIMESTAMP WITH TIME ZONE`,
     * `INTERVAL DAY TO SECOND`). [constraints] are those that apply to the declaration: the ones written at the nearest
     * use along its subtypes and `%TYPE` anchors that writes any (`s_vc(20)` of a subtype `s_vc`, which replace a
     * constrained subtype's own), else the predefined type's. One that cannot be decided is
     * [WrittenTypeConstraint.Unknown]; a null one is written nowhere along the chain.
     */
    class BuiltIn(val name: String, val constraints: WrittenTypeConstraints) : DeclaredType {
        override fun equals(other: Any?) = other is BuiltIn && name == other.name && constraints == other.constraints

        override fun hashCode() = name.hashCode() * 31 + constraints.hashCode()

        override fun toString() = "BuiltIn($name$constraints)"
    }

    /** A record type of this file: its [declaration] and its [fields], as `recordFields` gives them. */
    class Record(val declaration: NameDeclaration, fields: List<DeclaredField>) : DeclaredType {
        val fields: List<DeclaredField> = fields.toList()

        override fun equals(other: Any?) = other is Record && declaration == other.declaration && fields == other.fields

        override fun hashCode() = declaration.hashCode() * 31 + fields.hashCode()

        override fun toString() = "Record($declaration)"
    }

    /**
     * A collection type of this file: its [declaration], [kind], and the `DATATYPE` node of its element, which this query
     * describes in turn. [element] is null when the element's node is not in the tree this was asked from.
     */
    class Collection(val declaration: NameDeclaration, val kind: CollectionKind?, val element: AstNode?) : DeclaredType {
        override fun equals(other: Any?) = other is Collection && declaration == other.declaration && kind == other.kind && element === other.element

        override fun hashCode() = declaration.hashCode() * 31 + (kind?.hashCode() ?: 0)

        override fun toString() = "Collection($declaration, ${kind ?: "?"})"
    }

    /** Another declaration of this file whose shape this query does not describe: a REF CURSOR type, or a cursor's row. */
    class OtherLocal(val declaration: NameDeclaration) : DeclaredType {
        override fun equals(other: Any?) = other is OtherLocal && declaration == other.declaration

        override fun hashCode() = declaration.hashCode()

        override fun toString() = "OtherLocal($declaration)"
    }

    /** A type declared in another file of the project, or at schema level: the [declaration] its [description] reaches. */
    class Project(val declaration: ProjectTypeDeclarationView, val description: ProjectExternalTypeDescription) : DeclaredType {
        override fun equals(other: Any?) = other is Project && declaration.id == other.declaration.id

        override fun hashCode() = declaration.id.hashCode()

        override fun toString() = "Project(${declaration.qualifiedName})"
    }

    /**
     * A `%ROWTYPE`, or a `%TYPE` of a column, of the relation [name] written before the anchor. [description] is the
     * project's reading of it, with the scan's external metadata when it has any; without metadata the relation's
     * columns are not known here.
     */
    class Anchored(val anchor: ProjectTypeReference.Anchor, val name: ProjectQualifiedName, val description: ProjectExternalTypeDescription) : DeclaredType {
        override fun equals(other: Any?) = other is Anchored && anchor == other.anchor && name == other.name

        override fun hashCode() = anchor.hashCode() * 31 + name.hashCode()

        override fun toString() = "Anchored($name%$anchor)"
    }

    /** Not decided; [reasons] says what the analysis could not see. Never a proof. */
    class Unknown(reasons: Set<UsageUncertainty>) : DeclaredType {
        val reasons: Set<UsageUncertainty> = reasons.toSet()

        override fun equals(other: Any?) = other is Unknown && reasons == other.reasons

        override fun hashCode() = reasons.hashCode()

        override fun toString() = "Unknown$reasons"
    }

    /** No evidence: the file was not analyzed, or the node is not a datatype this query covers. */
    class Unavailable(val reason: UsageUnavailability) : DeclaredType {
        override fun equals(other: Any?) = other is Unavailable && reason == other.reason

        override fun hashCode() = reason.hashCode()

        override fun toString() = "Unavailable($reason)"
    }
}
