//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.project

/** A column of a [SourceTable], as written. */
internal class SourceColumn(
    val name: OracleIdentifier,
    /** The datatype as written, null for a virtual column written without one. */
    val datatype: String?,
    val invisible: Boolean,
    /** Written `NOT NULL`, or part of the primary key, by a constraint that is enforced. */
    val notNull: Boolean
)

/**
 * `CREATE [GLOBAL TEMPORARY] TABLE [owner.]name …` written in a project source. [columns] are those of its column
 * list; [columnsComplete] is false when the table's columns are not all written there: a table created from a query,
 * or an object table.
 */
internal class SourceTable(
    val fileId: FileId,
    val owner: OracleIdentifier?,
    val name: OracleIdentifier,
    val columns: List<SourceColumn>,
    val columnsComplete: Boolean
)

/** What an `ALTER TABLE` statement does to a table's columns, by how much of the CREATE TABLE it leaves true. */
internal enum class TableChange {
    /** Columns or constraints are added: the columns written in the CREATE TABLE are still there, unchanged. */
    ADDS,

    /** A constraint is dropped, disabled, modified or renamed: a `NOT NULL` or primary key may no longer hold. */
    CONSTRAINTS,

    /** A column is modified, dropped, renamed or set unused, or the table is renamed: the columns written may be gone. */
    COLUMNS
}

/** `ALTER TABLE [owner.]name` that changes the table's columns or constraints, written in a project source. */
internal class AlteredTable(val owner: OracleIdentifier?, val name: OracleIdentifier, val changes: Set<TableChange>)

/** The `CREATE TABLE` and column-changing `ALTER TABLE` statements of one source. */
internal class SourceTables(val created: List<SourceTable>, val altered: List<AlteredTable>) {
    companion object {
        val EMPTY = SourceTables(emptyList(), emptyList())
    }
}
