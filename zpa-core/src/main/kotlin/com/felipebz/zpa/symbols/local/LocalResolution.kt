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
package com.felipebz.zpa.symbols.local

import java.util.*

/**
 * The outcome of looking up one [LocalReference] in the regions of its file. Outcomes hold declaration ids, never
 * declaration objects or syntax. Only [ProvenAbsent] lets a caller continue past the local scope (to the enclosing
 * package's specification in the project, the Project System, STANDARD and external metadata); that next step is
 * outside the local resolver and nothing consumes these outcomes yet.
 *
 * Where several declarations form one entity (a forward declaration and its body, a specification heading and its
 * implementation, a forward cursor and its definition), the outcome holds one representative per entity: the member
 * with a body (or the cursor definition) when there is one, else the earliest member.
 */
internal sealed interface LocalResolution {

    /** One entity of an applicable kind, declared earlier, decides the name. */
    data class Bound(val declaration: LocalDeclarationId) : LocalResolution

    /**
     * Callables only decide the name: the subprogram entities of the deciding region, never merged with outer ones.
     * Choosing one is call binding, not lookup.
     */
    data class OverloadSet(val declarations: List<LocalDeclarationId>) : LocalResolution

    /**
     * The deciding declaration cannot be used in this context, and Oracle rejects the reference instead of looking
     * further out (PLS-00221, PLS-00222, PLS-00330, PLS-00382, PLS-00488, PLS-00206, PLS-00310, PLS-00485…).
     */
    data class WrongKind(val declaration: LocalDeclarationId) : LocalResolution

    /** The deciding region declares the name only after the reference: an order error, never an outer binding. */
    data class DeclaredLater(val declaration: LocalDeclarationId) : LocalResolution

    /**
     * Several entities decide the name and they are not all callables: two non-callables (lazy PLS-00371), or a
     * callable mixed with a non-callable (eager PLS-00305).
     */
    data class Duplicate(val declarations: List<LocalDeclarationId>) : LocalResolution

    /**
     * Every region the lookup crossed is complete and none declares the name. The only outcome after which the caller
     * may look past the local scope.
     */
    data object ProvenAbsent : LocalResolution

    /**
     * A region the outcome relies on may miss declarations, or the lookup depends on something outside the file.
     *
     * [provisional] extends the outcome model on purpose: it is the outcome the resolver would give if every region it
     * relied on were complete. It is never itself [Incomplete] or [Unsupported]: an unmodeled construct is reported
     * as such, unwrapped. It exists so that references in incomplete regions can still be compared with the legacy
     * symbol table. It is diagnostic evidence only: it is not a binding, it must never let a caller look past the local
     * scope, and nothing may consume it as a resolution.
     */
    class Incomplete(
        regionReasons: Set<LocalIncompleteness>,
        lookupReasons: Set<LocalLookupGap>,
        val provisional: LocalResolution
    ) : LocalResolution {
        /** The verdicts of the regions relied on, in [LocalIncompleteness] order. */
        val regionReasons: Set<LocalIncompleteness> = Collections.unmodifiableSet(enumSet(regionReasons))

        /** The resolver's own reasons, in [LocalLookupGap] order. */
        val lookupReasons: Set<LocalLookupGap> = Collections.unmodifiableSet(enumSet(lookupReasons))

        init {
            require(provisional !is Incomplete && provisional !is Unsupported) { "A provisional outcome is never incomplete or unsupported" }
            require(regionReasons.isNotEmpty() || lookupReasons.isNotEmpty()) { "An incomplete outcome has a reason" }
        }

        override fun equals(other: Any?) = other is Incomplete && regionReasons == other.regionReasons &&
            lookupReasons == other.lookupReasons && provisional == other.provisional

        override fun hashCode() = Objects.hash(regionReasons, lookupReasons, provisional)

        override fun toString() = "Incomplete(${(regionReasons + lookupReasons).joinToString()}, provisional=$provisional)"

        private companion object {
            inline fun <reified E : Enum<E>> enumSet(values: Set<E>): EnumSet<E> =
                if (values.isEmpty()) EnumSet.noneOf(E::class.java) else EnumSet.copyOf(values)
        }
    }

    /**
     * The reference is in a context the resolver does not model; [kind] and [context] name the pair when the construct
     * is a kind used in a context without evidence.
     */
    data class Unsupported(
        val construct: LocalUnsupportedConstruct,
        val kind: LocalDeclarationKind? = null,
        val context: LocalReferenceContext? = null
    ) : LocalResolution

    /**
     * No region declares the name and it cannot be a schema object: a label target nothing declares (PLS-00201), or a
     * member missing from a region, record or collection whose members the file shows in full (PLS-00302).
     */
    data object Undeclared : LocalResolution

    /**
     * The name binds [declaration], which this reference cannot reach from where it is written: a label or subprogram
     * name used as a qualifier outside its region (PLS-00219, PLS-00225), an `EXIT` outside the labeled loop
     * (PLS-00374), or a `GOTO` into a statement it cannot branch into (PLS-00375; PLS-00201 for a loop body).
     */
    data class OutOfScope(val declaration: LocalDeclarationId) : LocalResolution

    /** A member step: the field at [ordinal] (zero-based) of the local record type [recordType]. */
    data class Field(val recordType: LocalDeclarationId, val ordinal: Int) : LocalResolution

    /** A member step: a collection method (`COUNT`, `EXISTS`…) of a variable of the local collection type [collectionType]. */
    data class CollectionMethod(val collectionType: LocalDeclarationId) : LocalResolution

    /**
     * An unqualified name in SQL that the local regions also declare: a same-named column wins in Oracle, so without
     * relation metadata it is either.
     */
    data class SqlColumnOrLocal(val declaration: LocalDeclarationId) : LocalResolution
}

/** Why a lookup cannot be decided from the file, beyond the regions' own verdicts. */
internal enum class LocalLookupGap {
    /** A package body the lookup relies on has no specification in the file: its specification may declare the name. */
    SPECIFICATION_NOT_IN_FILE,

    /** A plain name in SQL that no local region declares may still be a column, which the resolver cannot see. */
    SQL_COLUMN_UNKNOWN
}

/** What [LocalResolution.Unsupported] does not model. */
internal enum class LocalUnsupportedConstruct {
    /** A reference inside an object type or type body, where attributes and `SELF` are visible (later increment). */
    OBJECT_TYPE_MEMBER,

    /** A reference inside a subprogram declared by a SQL `WITH` clause: no evidence on what it sees. */
    SQL_WITH_SUBPROGRAM,

    /**
     * A parameter default expression: resolved in the subprogram's own region with the order check, but the parameters
     * are not denotable there (PLS-00227) and the subprogram's own name is not visible (PLS-00313).
     */
    PARAMETER_DEFAULT,

    /** A declaration kind in a context where Oracle's behavior has not been observed, or depends on its datatype. */
    KIND_IN_CONTEXT,

    /** Some overloads of the deciding region are declared before the reference and some after. */
    OVERLOADS_PARTLY_LATER,

    /** A SQL name whose local outcome is not a single declaration: a column could still win over it. */
    SQL_NAME_WITHOUT_SINGLE_DECLARATION,

    /**
     * A member of a name whose members depend on its datatype, which is not a local record or collection type: a
     * subtype, `%TYPE`/`%ROWTYPE`, a type of another unit or STANDARD, a loop index, a qualified or unrepresentable type.
     */
    MEMBER_NEEDS_TYPING,

    /** A member of a label that labels a statement forming no region (a plain loop or an ordinary statement). */
    MEMBER_OF_STATEMENT_LABEL,

    /** A member of a parameterless cursor written inside the cursor's own declaration: not observed. */
    CURSOR_MEMBER_INSIDE_CURSOR
}
