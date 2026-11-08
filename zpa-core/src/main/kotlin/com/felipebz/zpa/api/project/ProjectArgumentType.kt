//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.QualifiedName
import java.util.*

/**
 * Detached datatype evidence for one written actual argument of a call, used by [ProjectSnapshot.narrowCall]. It
 * holds only a classification: no expression, text, syntax tree, symbol or type description is retained, and nothing
 * is inferred from expression text.
 *
 * Typical sources, all derived by the caller:
 * - a numeric literal is `builtIn("NUMBER")` (`BINARY_DOUBLE`/`BINARY_FLOAT` for `d`/`f` suffixes);
 * - a character literal is `builtIn("CHAR")`, **including the empty string `''`**, which Oracle types as character
 *   for overload purposes even though its value is NULL;
 * - `TRUE`/`FALSE` is `builtIn("BOOLEAN")`; `DATE '…'`, `TIMESTAMP '…'` and `INTERVAL '…'` literals are those names;
 * - the `NULL` literal is [nullLiteral], never a character or other datatype;
 * - a variable is `builtIn` of its declared built-in type, [composite] for a record or collection, or [described]
 *   from [ProjectSnapshot.describeExternalType] for project types and anchors;
 * - anything else is [unknown].
 *
 * Built-in names are classified as Oracle's predefined types: resolving a name the caller's own scope may shadow is
 * the caller's lexical responsibility.
 */
@ZpaExperimentalApi
class ProjectArgumentType private constructor(
    val kind: Kind,
    family: ProjectDatatypeFamily?,
    /** True when the evidence was conclusive but names a type outside the supported families. */
    @get:JvmSynthetic internal val unclassified: Boolean,
    /** Built-in name whose predefined meaning a package member could shadow, with the package that resolved it. */
    @get:JvmSynthetic internal val shadowName: OracleIdentifier?,
    @get:JvmSynthetic internal val shadowScope: QualifiedName?,
    /** The exact type, as Oracle's choice among compatible overloads tells types apart; null when not known exactly. */
    @get:JvmSynthetic internal val rank: OverloadRank? = null,
    /** For a composite, what it is where Oracle's compatibility between composites depends on it; null when not known. */
    @get:JvmSynthetic internal val compositeKind: CompositeKind? = null
) {
    enum class Kind {
        /** No usable evidence. */
        UNKNOWN,

        /** The `NULL` literal: accepted by every formal type, so it never eliminates a candidate. */
        NULL_LITERAL,

        /** A built-in type name supplied by the caller. */
        BUILT_IN,

        /** A record, collection or object value. */
        COMPOSITE,

        /** Classified from a type description. */
        DESCRIBED
    }

    /** The datatype family, when the evidence identifies a supported one. */
    val family: Optional<ProjectDatatypeFamily> = Optional.ofNullable(family)

    override fun toString(): String = "$kind${family.map { " $it" }.orElse("")}"

    companion object {
        private val UNKNOWN = ProjectArgumentType(Kind.UNKNOWN, null, false, null, null)
        private val NULL = ProjectArgumentType(Kind.NULL_LITERAL, null, false, null, null)
        private val COMPOSITE = ProjectArgumentType(Kind.COMPOSITE, ProjectDatatypeFamily.COMPOSITE, false, null, null)

        /** No usable evidence: the actual is compared as unknown and never eliminates a candidate. */
        @JvmStatic
        fun unknown(): ProjectArgumentType = UNKNOWN

        /** The `NULL` literal. */
        @JvmStatic
        fun nullLiteral(): ProjectArgumentType = NULL

        /** A record or collection (PL/SQL or SQL), object instance or `%ROWTYPE` row value. */
        @JvmStatic
        fun composite(): ProjectArgumentType = COMPOSITE

        /**
         * An actual of the Oracle built-in type [typeName], spelled as in a declaration (`NUMBER`, `VARCHAR2(30)`,
         * `TIMESTAMP WITH TIME ZONE`, `BINARY_DOUBLE`). Unsupported, quoted or qualified names are kept as unclassified
         * evidence.
         *
         * @throws IllegalArgumentException if [typeName] is blank
         */
        @JvmStatic
        fun builtIn(typeName: String): ProjectArgumentType {
            require(typeName.isNotBlank()) { "The type name must not be blank" }
            val family = DatatypeFamilies.ofTypeName(typeName)
            return ProjectArgumentType(Kind.BUILT_IN, family, family == null, null, null, family?.let { DatatypeFamilies.rankOfTypeName(typeName) })
        }

        /** A composite value of a known [kind]: a ref cursor variable, or a value of a schema object type. */
        @JvmSynthetic
        internal fun composite(kind: CompositeKind): ProjectArgumentType = ProjectArgumentType(Kind.COMPOSITE, ProjectDatatypeFamily.COMPOSITE, false, null, null, null, kind)

        /** As [builtIn], for a value whose exact type within the family is not known (a timestamp literal, which may have a zone). */
        @JvmSynthetic
        internal fun builtInInexact(typeName: String): ProjectArgumentType {
            val family = DatatypeFamilies.ofTypeName(typeName)
            return ProjectArgumentType(Kind.BUILT_IN, family, family == null, null, null)
        }

        /** As [builtIn], with [rank] the exact type the caller read from the written type (a TIMESTAMP variant, a precision). */
        @JvmSynthetic
        internal fun builtIn(typeName: String, rank: OverloadRank?): ProjectArgumentType {
            val family = DatatypeFamilies.ofTypeName(typeName)
            return ProjectArgumentType(Kind.BUILT_IN, family, family == null, null, null, rank?.takeIf { family != null })
        }

        /**
         * An actual whose type was described by [ProjectSnapshot.describeExternalType] without telling which package
         * scope the request was described in. Only conclusive descriptions classify (a source record/collection/object
         * declaration, a resolved external column or row, or a source built-in reached through retained value or
         * subtype steps); a `%ROWTYPE` anchor is a row whatever its resolution. A built-in name reached without any
         * step could be shadowed by a member of the unknown package, so it gives unknown evidence: use the overload
         * with the package owner. The description itself is not retained.
         */
        @JvmStatic
        fun described(description: ProjectExternalTypeDescription): ProjectArgumentType = classify(description, false, null)

        /**
         * As [described], with [packageOwner] the package owner passed to [ProjectSnapshot.describeExternalType] for
         * the same description (null when the reference is in no package, such as an anonymous block or a standalone
         * subprogram). A built-in name is then checked against that package's members when narrowing, because a
         * package member named like a predefined type shadows it inside the package.
         */
        @JvmStatic
        fun described(description: ProjectExternalTypeDescription, packageOwner: ProjectQualifiedName?): ProjectArgumentType =
            classify(description, true, packageOwner?.name)

        private fun classify(description: ProjectExternalTypeDescription, ownerKnown: Boolean, owner: QualifiedName?): ProjectArgumentType {
            if (description.anchor.orElse(null) == ProjectTypeReference.Anchor.ROWTYPE) return COMPOSITE
            val family = DatatypeFamilies.ofDescription(description, null, null)
            val conclusive = DatatypeFamilies.isConclusive(description)
            val builtIn = DatatypeFamilies.builtInName(description, null)?.takeIf { family != null }
                ?: return ProjectArgumentType(Kind.DESCRIBED, family, family == null && conclusive, null, null, null, DatatypeFamilies.compositeKind(description))
            // The package that resolved the built-in name: the last retained step's owner, else the request's owner.
            val stepScope = DatatypeFamilies.builtInScope(description, null)
            if (stepScope == null && description.source.steps.isEmpty() && !ownerKnown) {
                return ProjectArgumentType(Kind.DESCRIBED, null, false, null, null)
            }
            val scope = stepScope ?: owner
            val rank = OverloadRank.of(builtIn.name, description.source.terminalReference.map { it.writtenIdentity }.orElse(null))
            return ProjectArgumentType(Kind.DESCRIBED, family, false, scope?.let { builtIn.name.segments.single() }, scope, rank)
        }
    }
}
