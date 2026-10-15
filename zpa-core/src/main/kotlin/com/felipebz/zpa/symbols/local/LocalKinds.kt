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
     * attributes and methods of the type's specification (and of its supertypes), reachable without the `SELF.`
     * qualifier. Documented by Oracle (Object-Relational Developer's Guide, "Object Methods") and observed on Oracle
     * 26ai: the method's parameters and locals hide same-named attributes, and an attribute hides a same-named schema
     * object.
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
     * methods see attributes and `SELF` is documented by Oracle and observed on Oracle 26ai (see [SUBPROGRAM]).
     */
    OBJECT_TYPE,

    /**
     * An object type body: its member subprograms, each with a body forming a nested SUBPROGRAM region. The type's
     * attributes are not declarations of this region: they come from the linked specification ([LocalUnit.counterpart])
     * or, when the specification is not in the file, from elsewhere and are not represented here. Inside a member
     * method a resolver must also consider `SELF` and the attributes and methods of the specification; documented by
     * Oracle and observed on Oracle 26ai (see [SUBPROGRAM]).
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
