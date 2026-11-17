//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.project.extraction

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.zpa.project.*

/**
 * `CREATE [GLOBAL TEMPORARY] TABLE` and `ALTER TABLE` statements, read with the value-based token operations. A
 * statement holding text the lexer discarded (conditional compilation, unreadable text) is left out.
 */
internal class TableExtractor(private val stream: TokenStream, private val bounds: UnitBounds, private val discardedText: DiscardedSourceText) {

    fun tables(): List<SourceTable> = statements("CREATE", ::tableAt)

    fun alteredTables(): List<AlteredTable> = statements("ALTER", ::alteredAt)

    private fun <T : Any> statements(keyword: String, read: (Int) -> T?): List<T> {
        val found = ArrayList<T>()
        var cursor = 0
        while (cursor < stream.tokens.size) {
            val start = stream.nextValue(cursor, keyword) ?: break
            cursor = start + 1
            if (stream.isKeyword(start, keyword)) read(start)?.let(found::add)
        }
        return immutableList(found)
    }

    private fun tableAt(create: Int): SourceTable? {
        var index = create + 1
        if (stream.valueAt(index) == "GLOBAL" && stream.valueAt(index + 1) == "TEMPORARY") index += 2
        if (stream.valueAt(index) != "TABLE") return null
        index++
        if (stream.valueAt(index) == "IF" && stream.valueAt(index + 1) == "NOT" && stream.valueAt(index + 2) == "EXISTS") index += 3
        val (name, afterName) = stream.qualifiedName(index) ?: return null
        if (name.segments.size > 2) return null
        val end = minOf(bounds.statementEnd(afterName), stream.tokens.size - 1)
        if (discardedText.scan(stream.tokens, create, end).kinds.isNotEmpty()) return null
        val owner = name.segments.takeIf { it.size == 2 }?.first()
        return when (stream.valueAt(afterName)) {
            // An object table: its columns are the attributes of its type.
            "OF" -> SourceTable(stream.fileId, owner, name.last, emptyList(), columnsComplete = false)
            // Created from a query: the columns are the query's.
            "AS" -> SourceTable(stream.fileId, owner, name.last, emptyList(), columnsComplete = false)
            "(" -> relational(owner, name.last, afterName, end)
            else -> null
        }
    }

    private fun relational(owner: OracleIdentifier?, name: OracleIdentifier, open: Int, end: Int): SourceTable? {
        val close = stream.matching(open)
        if (close > end) return null
        val after = close + 1
        // External tables are left out; a query after the list names its columns only.
        if (stream.firstAtTopLevel(after, setOf("EXTERNAL"), end + 1) != null) return null
        val query = isQuery(after, end)
        val items = items(open, close)
        if (query) return SourceTable(stream.fileId, owner, name, emptyList(), columnsComplete = false)
        val primaryKey = items.flatMap(::primaryKeyColumns).toSet()
        val columns = items.filter { !isConstraint(it.first) }.map { column(it) ?: return null }
        if (columns.map { it.name }.toSet().size != columns.size) return null
        return SourceTable(
            stream.fileId, owner, name,
            columns.map { if (it.name in primaryKey && !it.notNull) SourceColumn(it.name, it.datatype, it.invisible, true) else it },
            columnsComplete = true
        )
    }

    /** The depth-0 comma-separated items between [open] and [close]. */
    private fun items(open: Int, close: Int): List<IntRange> {
        val parts = ArrayList<IntRange>()
        var depth = 0
        var start = open + 1
        for (index in open + 1 until close) {
            when (stream.valueAt(index)) {
                "(" -> depth++
                ")" -> depth--
                "," -> if (depth == 0) {
                    if (index > start) parts += start until index
                    start = index + 1
                }
            }
        }
        if (close > start) parts += start until close
        return parts
    }

    private fun isConstraint(index: Int) = stream.valueAt(index) in OUT_OF_LINE

    /** The columns an enforced out-of-line `PRIMARY KEY (…)` lists. */
    private fun primaryKeyColumns(item: IntRange): List<OracleIdentifier> {
        if (!isConstraint(item.first)) return emptyList()
        val key = (item.first until item.last).firstOrNull { stream.valueAt(it) == "PRIMARY" && stream.valueAt(it + 1) == "KEY" } ?: return emptyList()
        if (item.any { stream.valueAt(it) in NOT_ENFORCED }) return emptyList()
        val open = key + 2
        if (stream.valueAt(open) != "(") return emptyList()
        val close = stream.matching(open)
        return items(open, close).mapNotNull { part -> stream.identifierAt(part.first)?.first?.takeIf { part.first == part.last } }
    }

    /** A column definition: its name, the datatype written up to its first clause, and its inline constraints. */
    private fun column(item: IntRange): SourceColumn? {
        val name = stream.identifierAt(item.first)?.first ?: return null
        val typeEnd = (item.first + 1..item.last).firstOrNull { stream.valueAt(it) in COLUMN_CLAUSES && depthAt(item.first + 1, it) == 0 } ?: (item.last + 1)
        val datatype = (item.first + 1 until typeEnd).takeIf { !it.isEmpty() }?.let(::datatypeText)
        val clauses = (typeEnd..item.last).filter { depthAt(typeEnd, it) == 0 }
        // An identity column has an implicit NOT NULL constraint.
        val identity = clauses.any { stream.valueAt(it) == "IDENTITY" }
        return SourceColumn(name, datatype, clauses.any { stream.valueAt(it) == "INVISIBLE" }, identity || enforced(clauses))
    }

    /** Whether the inline constraints of [clauses] hold an enforced `NOT NULL` or `PRIMARY KEY`. */
    private fun enforced(clauses: List<Int>): Boolean {
        val starts = clauses.filter { stream.valueAt(it) in INLINE_STARTS && !(stream.valueAt(it) == "NULL" && stream.valueAt(it - 1) == "NOT") }
        return starts.indices.any { i ->
            val start = starts[i]
            val strong = (stream.valueAt(start) == "NOT" && stream.valueAt(start + 1) == "NULL") ||
                (stream.valueAt(start) == "PRIMARY" && stream.valueAt(start + 1) == "KEY")
            val until = starts.getOrNull(i + 1) ?: (clauses.last() + 1)
            strong && clauses.none { it in start until until && stream.valueAt(it) in NOT_ENFORCED }
        }
    }

    private fun depthAt(from: Int, index: Int): Int {
        var depth = 0
        for (i in from until index) when (stream.valueAt(i)) {
            "(" -> depth++
            ")" -> depth--
        }
        return depth
    }

    /** The datatype's tokens as one text, a space only between two words. */
    private fun datatypeText(range: IntRange): String = buildString {
        for (index in range) {
            val value = stream.tokens[index].value.let { if (it.startsWith("\"")) it else it.uppercase() }
            if (isNotEmpty() && (last().isLetterOrDigit() || last() == ')' || last() == '"') && (value.first().isLetterOrDigit() || value.first() == '"')) append(' ')
            append(value)
        }
    }

    /**
     * Whether the clauses from [start] to [end] hold the query of `CREATE TABLE … AS`: `AS` at the top level followed by
     * `SELECT` or `WITH`, directly or inside parentheses. Another `AS (`, such as a LOB's `STORE AS (…)`, is no query.
     */
    private fun isQuery(start: Int, end: Int): Boolean {
        var depth = 0
        for (index in start..end) {
            when (stream.valueAt(index)) {
                "(" -> depth++
                ")" -> depth--
                "AS" -> if (depth == 0) {
                    val next = stream.valueAt(index + 1)
                    if (next in QUERY_STARTS || next == "(" && stream.valueAt(index + 2) in QUERY_STARTS) return true
                }
            }
        }
        return false
    }

    private fun alteredAt(alter: Int): AlteredTable? {
        if (stream.valueAt(alter + 1) != "TABLE") return null
        val (name, afterName) = stream.qualifiedName(alter + 2) ?: return null
        if (name.segments.size > 2) return null
        val end = minOf(bounds.statementEnd(afterName), stream.tokens.size - 1)
        val changes = (afterName..end).filter { depthAt(afterName, it) == 0 && stream.tokens[it].type !== GenericTokenType.EOF }
            .mapNotNullTo(HashSet(), ::change)
        if (changes.isEmpty()) return null
        return AlteredTable(name.segments.takeIf { it.size == 2 }?.first(), name.last, changes)
    }

    /** The change the action word at [index] of an `ALTER TABLE` makes, null for a word that starts none. */
    private fun change(index: Int): TableChange? {
        val next = stream.valueAt(index + 1)
        return when (stream.valueAt(index)) {
            "ADD" -> TableChange.ADDS
            "MODIFY", "DROP" -> when (next) {
                in CONSTRAINT_TARGETS -> TableChange.CONSTRAINTS
                in PARTITIONS -> null
                else -> TableChange.COLUMNS
            }
            "RENAME" -> when (next) {
                "CONSTRAINT" -> TableChange.CONSTRAINTS
                in PARTITIONS -> null
                // A column renamed, or the table: the columns written are no longer under this name.
                else -> TableChange.COLUMNS
            }
            "SET" -> TableChange.COLUMNS.takeIf { next == "UNUSED" }
            "DISABLE" -> TableChange.CONSTRAINTS
            "ENABLE" -> TableChange.CONSTRAINTS.takeIf { next == "NOVALIDATE" }
            else -> null
        }
    }

    private companion object {
        val OUT_OF_LINE = setOf("CONSTRAINT", "PRIMARY", "UNIQUE", "FOREIGN", "CHECK", "SUPPLEMENTAL", "SCOPE", "REF", "PERIOD")
        val COLUMN_CLAUSES = setOf(
            "DEFAULT", "NOT", "NULL", "CONSTRAINT", "PRIMARY", "UNIQUE", "REFERENCES", "CHECK", "GENERATED", "AS", "INVISIBLE",
            "VISIBLE", "ENCRYPT", "COLLATE", "SORT", "VIRTUAL", "DOMAIN", "ANNOTATIONS", "RESERVABLE"
        )
        val INLINE_STARTS = setOf("NOT", "NULL", "CONSTRAINT", "PRIMARY", "UNIQUE", "REFERENCES", "CHECK")
        val NOT_ENFORCED = setOf("DISABLE", "NOVALIDATE")
        val QUERY_STARTS = setOf("SELECT", "WITH")
        val CONSTRAINT_TARGETS = setOf("CONSTRAINT", "PRIMARY", "UNIQUE", "FOREIGN", "CHECK")
        val PARTITIONS = setOf("PARTITION", "SUBPARTITION")
    }
}
