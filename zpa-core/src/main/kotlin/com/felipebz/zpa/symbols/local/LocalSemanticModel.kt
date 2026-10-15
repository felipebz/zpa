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

/*
 * Per-file local semantic model: the lexical regions of one parsed file and the declarations they own, as immutable,
 * AST-detached facts. Internal and not consumed yet. It records what was written; it does not resolve names, and
 * nothing in it proves that a name is absent from a region (see LocalCompleteness).
 */

/** A region's position in [LocalSemanticModel.regions]: document order of the region's start, the file region first. */
@JvmInline
internal value class LocalRegionId(val ordinal: Int) {
    override fun toString() = "R$ordinal"
}

/** A declaration's position in [LocalSemanticModel.declarations]: document order over the whole file. */
@JvmInline
internal value class LocalDeclarationId(val ordinal: Int) {
    override fun toString() = "D$ordinal"
}

internal enum class LocalRegionKind {
    /**
     * The whole file: the root. It holds the top-level units: standalone subprograms, packages, package bodies,
     * triggers, types and type bodies are its declarations, and so are the labels of top-level blocks. It is not an Oracle region (standalone units live in a schema, and a top-level
     * block's label is visible only inside the block), so a resolver must treat it specially.
     */
    FILE,

    /** A `[DECLARE …] BEGIN … END` block, top-level or nested in a statement list. */
    ANONYMOUS_BLOCK,

    /**
     * A procedure or function with a body: its parameters and its local declarations. Inside a member method of an
     * object type (see [LocalSubprogram.implicitSelf]) a resolver must also consider the implicit `SELF` and the
     * attributes and methods of the type's specification, reachable without the `SELF.` qualifier. That rule is
     * documented by Oracle (Object-Relational Developer's Guide, "Object Methods"), not probed.
     */
    SUBPROGRAM,

    /** A cursor with parameters: the parameters, visible in its query. */
    CURSOR,

    /** A FOR loop, from `FOR` to `END LOOP;`: its indices, visible in the iteration controls too. */
    FOR_LOOP,

    /** A FORALL statement: its index. */
    FORALL,

    /** An iterator choice of a qualified expression (`FOR i IN … => …`): its indices. */
    ITERATOR,

    /**
     * A SQL query whose `WITH` clause declares PL/SQL functions or procedures, from `WITH` to the end of the query.
     * It holds those subprograms as declarations, in source order; each one with a body forms a nested SUBPROGRAM
     * region. Its extraction state concerns these PL/SQL declarations only: the names of the factoring clauses
     * (`WITH x AS (…)`) are SQL names and are not recorded. There is no Oracle evidence yet on which names of the
     * enclosing PL/SQL are visible inside these functions, nor on how the `WITH` subprograms see each other; a
     * resolver must not assume either way. A `WITH` subprogram is never linked to a forward declaration.
     */
    SQL_WITH,

    /** A package specification: its declaration-section items. Its subprograms are headings without regions. */
    PACKAGE_SPEC,

    /**
     * A package body: its declaration-section items, and its initialization section (blocks, loops and labels there
     * have this region as their parent). The model records the body and its specification as two regions, linked
     * through [LocalUnit.counterpart]; it does not merge them. For a resolver:
     * - the specification's declarations are visible throughout the body, whatever their order;
     * - the body's private declarations follow the ordinary order rule of a region;
     * - specification and body declarations collide as if they were one region (a duplicate is diagnosed lazily, on
     *   reference, as PLS-00371).
     *
     * A body whose specification is not in the file has no link: the specification's declarations then come from the
     * project and are not represented here.
     */
    PACKAGE_BODY,

    /**
     * The PL/SQL body of a trigger, from `DECLARE`, `BEGIN` or `COMPOUND TRIGGER` to its end: the declarations of its
     * `DECLARE` section, or the section-level declarations of a compound trigger. The heading (events, `REFERENCING`,
     * `WHEN`) is SQL-level, cannot see these declarations, and is not part of the region.
     */
    TRIGGER,

    /** A timing-point section of a compound trigger: its own declarations, visible only inside that timing point. */
    TIMING_POINT,

    /**
     * An object type specification: its attributes and its method and constructor headings. Every rule about how
     * methods see attributes and `SELF` is documented by Oracle, not probed (see [SUBPROGRAM]).
     */
    OBJECT_TYPE,

    /**
     * An object type body: its member subprograms, each with a body forming a nested SUBPROGRAM region. The type's
     * attributes are not declarations of this region: they come from the linked specification ([LocalUnit.counterpart])
     * or, when the specification is not in the file, from elsewhere and are not represented here. Inside a member
     * method a resolver must also consider `SELF` and the attributes and methods of the specification; documented by
     * Oracle, not probed.
     */
    TYPE_BODY
}

internal enum class LocalDeclarationKind {
    VARIABLE,
    SUBTYPE,
    PROCEDURE,
    FUNCTION,
    PARAMETER,
    LOOP_INDEX,
    LABEL,
    EXCEPTION,
    CURSOR,
    CURSOR_PARAMETER,
    RECORD_TYPE,
    COLLECTION_TYPE,
    REF_CURSOR_TYPE,
    PACKAGE,
    PACKAGE_BODY,

    /**
     * A trigger. Inside the trigger its name is not a qualifier (`trigger_name.x` fails with PLS-00201) and a trigger
     * is not callable.
     */
    TRIGGER,

    /** An object type created by `CREATE TYPE … AS OBJECT` or `UNDER`, or an incomplete `CREATE TYPE t;`. */
    OBJECT_TYPE,
    TYPE_BODY,

    /** An attribute of an object type specification. */
    ATTRIBUTE
}

/** How much of a region's own declarations the builder extracted. Never a statement that the list is complete. */
internal enum class LocalExtraction {
    /** Every declaration item the AST shows for the region was extracted (the AST itself may be incomplete). */
    ALL_ITEMS_EXTRACTED,

    /** Some declaration items, recorded as [LocalCoverageGap]s, were not extracted. */
    PARTIAL
}

internal enum class LocalGapKind {
    /** A declaration item of a form the model does not extract. */
    UNSUPPORTED_DECLARATION,

    /** Source the parser skipped while recovering from a syntax error; it may have held declarations. */
    PARSE_RECOVERY
}

/** Evidence that something written in a region was not extracted. [syntax] names the grammar rule. */
internal class LocalCoverageGap(val kind: LocalGapKind, val syntax: String, val range: SourceRange) {
    override fun toString() = "$kind $syntax@${range.startLine}:${range.startColumn}"
}

/**
 * Whether the absence of a declaration from the model could ever be trusted. In this version it never can: the
 * evidence needed (conditional compilation, recovery attribution) is not established yet.
 */
internal enum class LocalCompleteness {
    /** No completeness evidence exists for the file; nothing observed contradicts the extracted facts either. */
    NOT_ESTABLISHED,

    /** The parser recovered from syntax errors: any region may be missing declarations, whatever its AST shows. */
    PARSE_RECOVERY_OBSERVED
}

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
     * declaration: IN for member functions (MAP and ORDER included), IN OUT for member procedures — documented by
     * Oracle, not probed — and IN OUT for constructors, taken from the grammar's explicit form (`SELF IN OUT t`), since
     * the documentation does not state it. Null for static methods, for non-members, and when `SELF` is written as a
     * parameter.
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

internal class LocalLabel(
    /** The region formed by the labeled statement or block, if it forms one. */
    val labeled: LocalRegionId?
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
     * (`scott.pk` and `pk` differ: the schema is unknown). Order never decides.
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

internal class LocalRegion(
    val id: LocalRegionId,
    /** Null only for the [LocalRegionKind.FILE] root. */
    val parent: LocalRegionId?,
    val kind: LocalRegionKind,
    /** The grammar rule that forms the region (`BLOCK_STATEMENT`, `FOR_STATEMENT`…; the root's is `FILE`). */
    val syntax: String,
    /** From the region's first token (after any label, which belongs to the enclosing region) to its last. */
    val range: SourceRange,
    /** The region's own declarations, in source order. */
    val declarations: List<LocalDeclarationId>,
    /** Directly nested regions, in source order. */
    val children: List<LocalRegionId>,
    val extraction: LocalExtraction,
    val gaps: List<LocalCoverageGap>
) {
    override fun toString() = "$kind $id${parent?.let { " in $it" } ?: ""} ${declarations.size} declarations, $extraction"
}

internal class LocalDeclaration(
    val id: LocalDeclarationId,
    val region: LocalRegionId,
    /** Index in the owning region's [LocalRegion.declarations]. */
    val position: Int,
    val kind: LocalDeclarationKind,
    val name: OracleIdentifier,
    /** The declaration item, from its first token to its last. */
    val range: SourceRange,
    val nameRange: SourceRange,
    /**
     * The declared datatype of a variable, subtype, parameter, cursor parameter or loop index, or the element type
     * of a collection type. A name such as `NUMBER` is only a written name here: a local declaration may shadow it.
     * Kind-specific types (RETURN, INDEX BY, fields) are in [details].
     */
    val type: LocalTypeUse?,
    /** `CONSTANT` (variables only). */
    val isConstant: Boolean,
    /** `NOT NULL` on the declared type (or on the element type of a collection). */
    val isNotNull: Boolean,
    /** A default value (`:=` or `DEFAULT`). */
    val hasDefault: Boolean,
    val details: LocalDeclarationDetails?
) {
    val typeRef: TypeRef? get() = type?.typeRef

    override fun toString() = "$kind $name $id in $region"
}

internal class LocalSemanticModel(
    val fileId: FileId,
    /** Indexed by [LocalRegionId.ordinal]; the root is first. */
    val regions: List<LocalRegion>,
    /** Indexed by [LocalDeclarationId.ordinal]. */
    val declarations: List<LocalDeclaration>,
    val completeness: LocalCompleteness
) {
    val root: LocalRegion get() = regions[0]

    fun region(id: LocalRegionId): LocalRegion = regions[id.ordinal]

    fun declaration(id: LocalDeclarationId): LocalDeclaration = declarations[id.ordinal]

    fun children(region: LocalRegionId): List<LocalRegion> = region(region).children.map(::region)

    fun declarationsOf(region: LocalRegionId): List<LocalDeclaration> = region(region).declarations.map(::declaration)

    fun parent(region: LocalRegionId): LocalRegion? = region(region).parent?.let(::region)

    override fun toString() = "LocalSemanticModel($fileId, ${regions.size} regions, ${declarations.size} declarations, $completeness)"
}
