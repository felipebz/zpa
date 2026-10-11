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
package com.felipebz.zpa.project.extraction

import com.felipebz.zpa.api.PlSqlPunctuator

private val UNIT_KINDS = listOf("FUNCTION", "PROCEDURE", "PACKAGE", "TRIGGER", "TYPE")

internal class PropertyScan(val next: Int, val deterministic: Boolean, val terminal: Boolean)

/** Strict scanning of subprogram properties (`DETERMINISTIC`, `PIPELINED`, `RESULT_CACHE`, `ACCESSIBLE BY`, ...). */
internal class CallableProperties(private val stream: TokenStream) {
    /**
     * Skips subprogram properties (grammar `subprogramProperty`, any order). Each clause's
     * mandatory structure is verified; anything unknown or malformed yields null. For a
     * [function], `PIPELINED ... USING ref ;` and `AGGREGATE USING ref ;` are terminal forms
     * (`next` is then the real `;`); a procedure cannot end this way, so they are rejected.
     */
    fun skipPropertiesStrict(start: Int, limit: Int, function: Boolean): PropertyScan? {
        var index = start
        var deterministic = false
        while (index < limit) {
            index = when {
                stream.isKeyword(index, "DETERMINISTIC") -> {
                    deterministic = true
                    index + 1
                }
                stream.isKeyword(index, "SHARD_ENABLE") -> index + 1
                stream.isKeyword(index, "PIPELINED") -> {
                    var next = index + 1
                    if ((stream.isKeyword(next, "ROW") || stream.isKeyword(next, "TABLE")) && stream.isKeyword(next + 1, "POLYMORPHIC")) next += 2
                    if (stream.isKeyword(next, "USING")) {
                        if (!function) return null
                        return terminalUsing(next, limit, deterministic)
                    }
                    next
                }
                stream.isKeyword(index, "AGGREGATE") -> {
                    if (!function || !stream.isKeyword(index + 1, "USING")) return null
                    return terminalUsing(index + 1, limit, deterministic)
                }
                stream.isKeyword(index, "SQL_MACRO") -> sqlMacroEnd(index + 1, limit) ?: return null
                stream.isKeyword(index, "PARALLEL_ENABLE") -> parallelEnableEnd(index + 1, limit) ?: return null
                stream.isKeyword(index, "ORDER") || stream.isKeyword(index, "CLUSTER") -> streamingEnd(index, limit) ?: return null
                stream.isKeyword(index, "RESULT_CACHE") ->
                    if (stream.isKeyword(index + 1, "RELIES_ON")) {
                        stream.delimitedListEnd(index + 2, limit) { stream.strictQualifiedNameAnyLength(it, limit) } ?: return null
                    } else {
                        index + 1
                    }
                stream.isKeyword(index, "AUTHID") ->
                    if (stream.isKeyword(index + 1, "CURRENT_USER") || stream.isKeyword(index + 1, "DEFINER")) index + 2 else return null
                stream.isKeyword(index, "DEFAULT") ->
                    if (stream.isKeyword(index + 1, "COLLATION") && stream.isKeyword(index + 2, "USING_NLS_COMP")) index + 3 else return null
                stream.isKeyword(index, "ACCESSIBLE") ->
                    if (stream.isKeyword(index + 1, "BY")) stream.delimitedListEnd(index + 2, limit, ::accessorEnd) ?: return null else return null
                else -> return PropertyScan(index, deterministic, false)
            }
        }
        return null
    }

    /** `USING object_reference ;` at [using]; the reference is a conservative name path. */
    private fun terminalUsing(using: Int, limit: Int, deterministic: Boolean): PropertyScan? {
        val end = stream.strictQualifiedNameAnyLength(using + 1, limit) ?: return null
        return if (end < limit && stream.isPunctuator(end, PlSqlPunctuator.SEMICOLON)) PropertyScan(end, deterministic, true) else null
    }

    /** Accessor of ACCESSIBLE BY: `[FUNCTION|PROCEDURE|PACKAGE|TRIGGER|TYPE] unit_name`. */
    private fun accessorEnd(start: Int): Int? {
        val limit = stream.tokens.size
        val index = if (UNIT_KINDS.any { stream.isKeyword(start, it) }) start + 1 else start
        return stream.strictQualifiedName(index, limit)?.second
    }

    /** `[( [TYPE =>] (TABLE|SCALAR) )]`. */
    private fun sqlMacroEnd(start: Int, limit: Int): Int? {
        if (!stream.isPunctuator(start, PlSqlPunctuator.LPARENTHESIS)) return start
        var index = start + 1
        if (stream.isKeyword(index, "TYPE")) {
            if (!stream.isPunctuator(index + 1, PlSqlPunctuator.ASSOCIATION)) return null
            index += 2
        }
        if (!stream.isKeyword(index, "TABLE") && !stream.isKeyword(index, "SCALAR")) return null
        return if (index + 1 < limit && stream.isPunctuator(index + 1, PlSqlPunctuator.RPARENTHESIS)) index + 2 else null
    }

    /**
     * `[( PARTITION name BY (ANY | (HASH|RANGE|VALUE) (name {, name})) [streaming] )]`.
     */
    private fun parallelEnableEnd(start: Int, limit: Int): Int? {
        if (!stream.isPunctuator(start, PlSqlPunctuator.LPARENTHESIS)) return start
        if (!stream.isKeyword(start + 1, "PARTITION") || stream.nameEnd(start + 2, limit) == null || !stream.isKeyword(start + 3, "BY")) return null
        var index = start + 4
        index = when {
            stream.isKeyword(index, "ANY") -> index + 1
            stream.isKeyword(index, "HASH") || stream.isKeyword(index, "RANGE") || stream.isKeyword(index, "VALUE") ->
                stream.delimitedListEnd(index + 1, limit) { stream.nameEnd(it, limit) } ?: return null
            else -> return null
        }
        if (stream.isKeyword(index, "ORDER") || stream.isKeyword(index, "CLUSTER")) index = streamingEnd(index, limit) ?: return null
        return if (index < limit && stream.isPunctuator(index, PlSqlPunctuator.RPARENTHESIS)) index + 1 else null
    }

    /** `(ORDER|CLUSTER) name BY ( name {, name} )`. */
    private fun streamingEnd(start: Int, limit: Int): Int? {
        if (stream.nameEnd(start + 1, limit) == null || !stream.isKeyword(start + 2, "BY")) return null
        return stream.delimitedListEnd(start + 3, limit) { stream.nameEnd(it, limit) }
    }
}
