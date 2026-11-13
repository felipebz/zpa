//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.project

/**
 * The select list of a cursor's query, in order, as Oracle names the fields of its row: a column by its name
 * (`l.line_no` gives `LINE_NO`), an item with an alias by the alias (a quoted alias keeps its spelling). `*`, `t.*` and
 * an expression without an alias are items of no known name: Oracle names such an expression by its text, which this
 * does not reproduce, and a star brings columns this does not know. The list is [complete] only without such items.
 */
class SelectList(items: List<SelectItem>) {
    val items: List<SelectItem> = immutableList(items)

    val complete: Boolean get() = items.all { it.name != null }

    /** The zero-based positions of the items named [name]. */
    fun positionsOf(name: OracleIdentifier): List<Int> = items.indices.filter { items[it].name == name }

    override fun equals(other: Any?) = other is SelectList && items == other.items

    override fun hashCode() = items.hashCode()

    override fun toString() = items.joinToString(", ", "[", "]") { it.name?.originalSpelling ?: "?" }
}

/** One item of a [SelectList]: its field [name], null when not known, and the range of that name or of the whole item. */
class SelectItem(val name: OracleIdentifier?, val range: SourceRange) {
    override fun equals(other: Any?) = other is SelectItem && name == other.name && name?.originalSpelling == other.name?.originalSpelling && range == other.range

    override fun hashCode() = (name?.originalSpelling?.hashCode() ?: 0) * 31 + range.hashCode()

    override fun toString() = "${name?.originalSpelling ?: "?"}@$range"
}
