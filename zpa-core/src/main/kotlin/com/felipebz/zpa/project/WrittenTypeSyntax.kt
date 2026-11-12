//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.semantics.LengthSemantics
import com.felipebz.zpa.api.semantics.WrittenTypeConstraint
import com.felipebz.zpa.api.semantics.WrittenTypeConstraints
import java.math.BigInteger

/** `TIMESTAMP (3) WITH TIME ZONE`: the words `TIMESTAMP WITH TIME ZONE`, and a group of one argument after the first. */
internal class WrittenTypeSyntax(
    /** Upper-cased; a qualified name is a single word. */
    val words: List<String>,
    val groups: List<ConstraintGroup>
) {
    val name: String get() = words.joinToString(" ")

    /**
     * Null for a name that is not a predefined type; [expression] values an argument that is neither a literal nor `*`.
     * [use] is a subtype's name written with constraints that apply to this predefined type instead of its own.
     */
    fun constraints(use: WrittenTypeSyntax? = null, expression: (ConstraintArgument) -> WrittenTypeConstraint): WrittenTypeConstraints? {
        if (use != null && use !== this && use.groups.isNotEmpty()) return foreign(use.groups, expression)
        fun value(argument: ConstraintArgument?): WrittenTypeConstraint? = when {
            argument == null -> null
            argument.star -> WrittenTypeConstraint.Maximum
            argument.value != null -> argument.value.toIntOrNull()?.let(WrittenTypeConstraint::Value) ?: WrittenTypeConstraint.Unknown
            else -> expression(argument)
        }
        fun after(word: String) = words.indexOf(word).takeIf { it >= 0 }?.let { index -> groups.firstOrNull { it.after == index } }
        val first = words.firstOrNull() ?: return null
        return when (first) {
            in LENGTH_TYPES -> {
                val length = groups.firstOrNull()?.arguments?.firstOrNull()
                WrittenTypeConstraints(value(length), length?.semantics, null, null)
            }
            in PRECISION_TYPES -> {
                val arguments = groups.firstOrNull()?.arguments.orEmpty()
                WrittenTypeConstraints(null, null, value(arguments.getOrNull(0)), value(arguments.getOrNull(1)))
            }
            "TIMESTAMP" -> WrittenTypeConstraints(null, null, null, value(after("TIMESTAMP")?.arguments?.firstOrNull()))
            "INTERVAL" -> WrittenTypeConstraints(
                null, null,
                value((after("DAY") ?: after("YEAR"))?.arguments?.firstOrNull()),
                value(after("SECOND")?.arguments?.firstOrNull())
            )
            in UNCONSTRAINED_TYPES -> WrittenTypeConstraints.NONE
            else -> null
        }
    }

    /**
     * Groups written after a subtype's name: their place among this type's words is not written, so only a type with a
     * single group (a length, a precision and scale, a TIMESTAMP's fractional seconds) reads them; any other gives
     * Unknown for each constraint the type may take.
     */
    private fun foreign(groups: List<ConstraintGroup>, expression: (ConstraintArgument) -> WrittenTypeConstraint): WrittenTypeConstraints? {
        val first = words.firstOrNull() ?: return null
        if (first in LENGTH_TYPES || first in PRECISION_TYPES || first == "TIMESTAMP") {
            return WrittenTypeSyntax(words, groups.map { ConstraintGroup(0, it.arguments) }).constraints(null, expression)
        }
        val length = WrittenTypeConstraint.Unknown.takeIf { first != "INTERVAL" }
        return WrittenTypeConstraints(length, null, WrittenTypeConstraint.Unknown, WrittenTypeConstraint.Unknown)
    }

    companion object {
        fun of(name: QualifiedName) =
            WrittenTypeSyntax(listOf(if (name.segments.size == 1) name.last.lookupName else name.lookupKey()), emptyList())

        private val LENGTH_TYPES = setOf("CHAR", "CHARACTER", "VARCHAR", "VARCHAR2", "NCHAR", "NVARCHAR2", "STRING", "RAW", "UROWID", "NATIONAL")
        private val PRECISION_TYPES = setOf("NUMBER", "NUMERIC", "DEC", "DECIMAL", "FLOAT")
        private val UNCONSTRAINED_TYPES = setOf(
            "BINARY_DOUBLE", "BINARY_FLOAT", "BINARY_INTEGER", "PLS_INTEGER", "SIMPLE_INTEGER", "SIMPLE_FLOAT", "SIMPLE_DOUBLE",
            "INT", "INTEGER", "SMALLINT", "REAL", "DOUBLE", "NATURAL", "NATURALN", "POSITIVE", "POSITIVEN", "SIGNTYPE",
            "DATE", "BOOLEAN", "BOOL", "CLOB", "NCLOB", "BLOB", "BFILE", "LONG", "ROWID", "XMLTYPE", "JSON", "SYS_REFCURSOR"
        )
    }
}

internal class ConstraintGroup(val after: Int, val arguments: List<ConstraintArgument>)

/** [value] for a signed integer literal, [star] for `*`; otherwise an expression at [range] (without `BYTE`/`CHAR`). */
internal class ConstraintArgument(val value: Long?, val star: Boolean, val range: SourceRange, val semantics: LengthSemantics?)

internal fun BigInteger.toIntOrNull(): Int? = if (bitLength() < 32) toInt() else null

internal fun Long.toIntOrNull(): Int? = if (this in Int.MIN_VALUE..Int.MAX_VALUE) toInt() else null
