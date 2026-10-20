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

import com.felipebz.zpa.project.*

/**
 * A written datatype and where a resolver must look it up: in [lookupRegion], at the start of [range]. The
 * declarations declared earlier than this type use are those of [lookupRegion] whose range ends before [range]
 * starts. Every other declaration of that region is still visible (a name declared anywhere in a region hides outer
 * names for the whole region) but is not declared earlier: a reference to it is an order case, not a binding. In
 * particular the declaration that owns the type never counts as earlier: in `SUBTYPE t IS t` or `x x%TYPE` the
 * declaration's range contains its type's range.
 *
 * The region is not always the one owning the declaration: parameter types are looked up in the region enclosing the
 * subprogram, a function's RETURN type in the function's own region. With the "ends before" rule:
 * - a function's parameters end before its RETURN type, so they are earlier;
 * - a subprogram's own declaration does not end before its parameter types, so a parameter typed with the
 *   subprogram's name is not an earlier binding;
 * - a record field typed with its record's name (`TYPE r IS RECORD (a NUMBER, b r%TYPE)`) is not earlier either.
 */
internal class LocalTypeUse(
    /** The written datatype as unresolved syntax, or null when it cannot be represented as a [TypeRef]. */
    val typeRef: TypeRef?,
    val range: SourceRange,
    val lookupRegion: LocalRegionId
) {
    override fun toString() = "${typeRef?.name ?: "?"} from $lookupRegion"
}

/** Facts specific to some declaration kinds. */
internal sealed interface LocalDeclarationDetails

internal enum class LocalSubprogramForm {
    /** A body (`IS … BEGIN … END`): the subprogram forms a [LocalRegionKind.SUBPROGRAM] region. */
    BODY,

    /** No body: a forward declaration in a block or subprogram; a standalone unit without a body (`AGGREGATE USING`). */
    HEADING_ONLY,

    /** `IS LANGUAGE …` / `IS EXTERNAL …`. */
    CALL_SPECIFICATION,

    /** Wrapped source: nothing of the heading is visible. */
    WRAPPED
}

internal class LocalParameter(
    val name: OracleIdentifier,
    val nameRange: SourceRange,
    /** One-based, as in [com.felipebz.zpa.project.ProjectParameter]. */
    val ordinal: Int,
    val mode: ParameterMode,
    val isNocopy: Boolean,
    val hasDefault: Boolean,
    /** Looked up in the region enclosing the subprogram. */
    val type: LocalTypeUse?
) : LocalDeclarationDetails {
    override fun toString() = "$ordinal $name $mode"
}

/** How an object type method is declared. */
internal enum class LocalMethodKind { MEMBER, STATIC, MAP, ORDER, CONSTRUCTOR }

internal data class LocalSubprogram(
    /** As written: `scott.p` for a standalone unit, the single name for a nested one. */
    val unitName: QualifiedName,
    val form: LocalSubprogramForm,
    /**
     * In ordinal order, for every form (empty when wrapped). An explicitly written `SELF` parameter is one of them,
     * including the `SELF IN OUT t` of a constructor.
     */
    val parameters: List<LocalParameter>,
    /** The RETURN type of a function, looked up in the function's own region when it has a body. */
    val returnType: LocalTypeUse?,
    /** `RETURN SELF AS RESULT` of a constructor: a return that is not a written type, so [returnType] is null. */
    val returnsSelf: Boolean,
    /** The region formed by a [LocalSubprogramForm.BODY]. */
    val body: LocalRegionId?,
    /** A forward declaration and its body in the same region, linked both ways when the match is unique. */
    val counterpart: LocalDeclarationId?,
    /**
     * A heading of a package or object type specification and its implementation in the linked body, linked both
     * ways when the match is unique. Independent of [counterpart]: a heading, a forward declaration in the body and
     * the body's implementation are one entity, and both links may exist at once.
     */
    val specificationCounterpart: LocalDeclarationId?,
    /** The method kind of an object type member; null outside object types. */
    val methodKind: LocalMethodKind?,
    /**
     * The mode of the implicit `SELF` parameter of a member method, which has no source text and is therefore not a
     * declaration: IN for member functions (MAP and ORDER included), IN OUT for member procedures (documented by
     * Oracle; observed on Oracle 26ai: assigning an attribute fails with PLS-00363 in a member function and succeeds
     * in a member procedure), and IN OUT for constructors, taken from the grammar's explicit form (`SELF IN OUT t`),
     * since the documentation does not state it; Oracle accepts attribute assignments in constructors, which does not
     * distinguish OUT from IN OUT. Null for static methods (no `SELF`: PLS-00201, and PLS-00588 for an unqualified
     * attribute), for non-members, and when `SELF` is written as a parameter.
     */
    val implicitSelf: ParameterMode?
) : LocalDeclarationDetails

internal enum class LocalMutability { MUTABLE, IMMUTABLE }

internal class LocalLoopIndex(
    /** `MUTABLE` / `IMMUTABLE` when written. */
    val mutability: LocalMutability?,
    /**
     * True when no type is written: the type comes from the iteration control (numeric range, collection, cursor
     * row…), which the model does not infer.
     */
    val isTypeImplicit: Boolean
) : LocalDeclarationDetails

internal data class LocalLabel(
    /** The region formed by the labeled statement or block, if it forms one. */
    val labeled: LocalRegionId?,
    /** The labeled statement, label included. */
    val statement: SourceRange,
    /** The labeled statement is a loop (`LOOP`, `WHILE`, `FOR`): `EXIT` and `CONTINUE` may name the label. */
    val isLoop: Boolean,
    /**
     * The innermost branch of an `IF` or `CASE`, loop or exception handler of the label's region that holds the labeled
     * statement; null when it is directly in the region's statement list. A `GOTO` outside this range cannot reach it.
     */
    val compound: SourceRange?
) : LocalDeclarationDetails

internal class LocalCursor(
    /** The [LocalRegionKind.CURSOR] region holding the parameters, when there are parameters. */
    val parameters: LocalRegionId?,
    /** No `IS SELECT …`: a forward cursor declaration. It is not linked to its definition. */
    val isForward: Boolean,
    /** The RETURN type, looked up in the region holding the cursor. */
    val returnType: LocalTypeUse?
) : LocalDeclarationDetails

internal class LocalCursorParameter(
    /** One-based. */
    val ordinal: Int
) : LocalDeclarationDetails

internal class LocalRecordField(
    val name: OracleIdentifier,
    val nameRange: SourceRange,
    /** Looked up in the region holding the record type. */
    val type: LocalTypeUse?,
    val isNotNull: Boolean,
    val hasDefault: Boolean
) {
    override fun toString() = "$name ${type ?: ""}"
}

internal class LocalRecordType(
    /** In written order. Fields are facts of the type, not declarations of any region. */
    val fields: List<LocalRecordField>
) : LocalDeclarationDetails

internal enum class LocalCollectionForm { NESTED_TABLE, ASSOCIATIVE_ARRAY, VARRAY }

internal class LocalCollectionType(
    /** The element type is the declaration's [LocalDeclaration.type]. */
    val form: LocalCollectionForm,
    /** The `INDEX BY` type of an associative array, looked up in the region holding the type. */
    val indexType: LocalTypeUse?,
    /** The full written name of a schema-level `CREATE TYPE … AS TABLE OF` / `VARRAY`; null for a local type. */
    val unitName: QualifiedName? = null
) : LocalDeclarationDetails

internal enum class LocalUnitForm {
    /** Source is present: the unit forms its region. */
    SOURCE,

    /** Wrapped source: nothing of the unit is visible and it forms no region. */
    WRAPPED,

    /** `CREATE TYPE t;` without a definition: forms no region. */
    INCOMPLETE
}

/** A package, package body, object type or type body. */
internal class LocalUnit(
    val unitName: QualifiedName,
    val form: LocalUnitForm,
    /** The region the unit forms ([LocalRegionKind.PACKAGE_SPEC], `PACKAGE_BODY`, `OBJECT_TYPE` or `TYPE_BODY`). */
    val region: LocalRegionId?,
    /**
     * The region of the linked specification (on a body) or body (on a specification). Linked only when the file holds
     * exactly one specification and exactly one body with the same written unit name, compared segment by segment
     * (`scott.pk` and `pk` differ: the schema is unknown). An incomplete type (`CREATE TYPE t;`) is not counted as a
     * specification, since a complete `CREATE TYPE t AS OBJECT` in the same file completes the same object; it is
     * never linked itself. Wrapped units count but, having no region, are never linked. Order never decides.
     */
    val counterpart: LocalRegionId?,
    /** The written supertype of an `UNDER` object type. */
    val supertype: QualifiedName?
) : LocalDeclarationDetails

/**
 * The correlation names of a DML trigger: `NEW`, `OLD` and `PARENT`, renamed by `REFERENCING` when written. They are
 * facts of the trigger, not declarations of any region: they do not occupy the PL/SQL namespace (a local variable
 * named `new` compiles). In the body they are reached through bind syntax (`:new.a`, `:n.a`); without the colon only
 * in the `WHEN` condition.
 */
internal class LocalCorrelationNames(val new: OracleIdentifier, val old: OracleIdentifier, val parent: OracleIdentifier) {
    override fun toString() = "new=$new old=$old parent=$parent"
}

internal class LocalTrigger(
    val unitName: QualifiedName,
    /** The [LocalRegionKind.TRIGGER] region of a PL/SQL body; null for a `CALL` trigger. */
    val body: LocalRegionId?,
    /** Null for system (DDL and database event) triggers, which have none. */
    val correlationNames: LocalCorrelationNames?
) : LocalDeclarationDetails

internal class LocalRefCursorType(
    /** The RETURN type of a strong ref cursor; null for a weak one. */
    val returnType: LocalTypeUse?
) : LocalDeclarationDetails
