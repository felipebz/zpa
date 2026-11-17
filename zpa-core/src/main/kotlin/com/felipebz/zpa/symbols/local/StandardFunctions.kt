//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.OverloadRank
import com.felipebz.zpa.api.project.ProjectArgumentType
import com.felipebz.zpa.api.project.ProjectDatatypeFamily

/**
 * The result types of the functions and operators STANDARD declares, from the types of their arguments, as the
 * overload of STANDARD those arguments bind returns them. Each answer is a type name, or null when this does not
 * decide it: an argument of no type, or one no modeled overload takes. A `NULL` argument is taken by any overload.
 */
internal object StandardFunctions {
    private val NUMERIC = ProjectDatatypeFamily.NUMERIC
    private val CHARACTER = ProjectDatatypeFamily.CHARACTER
    private val CLOB = ProjectDatatypeFamily.CLOB
    private val DATETIME = ProjectDatatypeFamily.DATETIME

    /** Character, numeric and datetime values convert to VARCHAR2 for a VARCHAR2 parameter. */
    private val TO_CHARACTER = setOf(CHARACTER, NUMERIC, DATETIME)

    private val FIXED = mapOf(
        "TO_NUMBER" to "NUMBER", "TO_DATE" to "DATE", "TO_TIMESTAMP" to "TIMESTAMP", "TO_TIMESTAMP_TZ" to "TIMESTAMP WITH TIME ZONE",
        "TO_CHAR" to "VARCHAR2", "TO_CLOB" to "CLOB", "HEXTORAW" to "RAW", "RAWTOHEX" to "VARCHAR2", "CHR" to "VARCHAR2",
        "INITCAP" to "VARCHAR2", "TRANSLATE" to "VARCHAR2", "ASCII" to "PLS_INTEGER", "SIGN" to "PLS_INTEGER",
        "MONTHS_BETWEEN" to "NUMBER", "ADD_MONTHS" to "DATE", "LAST_DAY" to "DATE", "NEXT_DAY" to "DATE", "SYS_GUID" to "RAW"
    )

    /** VARCHAR2 for a value that converts to it, CLOB for a CLOB. */
    private val CHARACTER_OR_CLOB = setOf("UPPER", "LOWER", "REPLACE", "TRIM", "LTRIM", "RTRIM", "LPAD", "RPAD", "SUBSTR")

    /** A position or a length: BINARY_INTEGER for a VARCHAR2, INTEGER for a CLOB. */
    private val POSITION = setOf("INSTR", "LENGTH")

    /** Numeric functions with a PLS_INTEGER overload, and ROUND and TRUNC of a DATE. */
    private val NUMERIC_WITH_INTEGER = setOf("ABS", "ROUND", "TRUNC")
    private val NUMERIC_ONLY = setOf("CEIL", "FLOOR", "SQRT")

    /** How many arguments each function takes; Oracle rejects another count (PLS-00306). */
    private val ARITY = mapOf(
        "TO_NUMBER" to 1..3, "TO_DATE" to 1..3, "TO_TIMESTAMP" to 1..3, "TO_TIMESTAMP_TZ" to 1..3, "TO_CHAR" to 1..3, "TO_CLOB" to 1..1,
        "HEXTORAW" to 1..1, "RAWTOHEX" to 1..1, "CHR" to 1..1, "INITCAP" to 1..1, "TRANSLATE" to 3..3, "ASCII" to 1..1, "SIGN" to 1..1,
        "MONTHS_BETWEEN" to 2..2, "ADD_MONTHS" to 2..2, "LAST_DAY" to 1..1, "NEXT_DAY" to 2..2, "SYS_GUID" to 0..0,
        "UPPER" to 1..1, "LOWER" to 1..1, "REPLACE" to 2..3, "TRIM" to 1..1, "LTRIM" to 1..2, "RTRIM" to 1..2, "LPAD" to 2..3,
        "RPAD" to 2..3, "SUBSTR" to 2..3, "INSTR" to 2..4, "LENGTH" to 1..1, "ABS" to 1..1, "ROUND" to 1..2, "TRUNC" to 1..2,
        "CEIL" to 1..1, "FLOOR" to 1..1, "SQRT" to 1..1, "MOD" to 2..2, "POWER" to 2..2, "CONCAT" to 2..2, "NVL" to 2..2,
        "GREATEST" to 1..Int.MAX_VALUE, "LEAST" to 1..Int.MAX_VALUE, "COALESCE" to 2..Int.MAX_VALUE
    )

    /** Whether [name] is a function this types. */
    fun covers(name: String): Boolean = name in ARITY

    /** The result type of a call of the STANDARD function [name] with [arguments], written positionally, in SQL when [sql]. */
    fun result(name: String, arguments: List<ProjectArgumentType>, sql: Boolean = false): String? {
        if (arguments.size !in (ARITY[name] ?: return null)) return null
        if (arguments.any { !it.isNull && it.familyOrNull == null }) return null
        val first = arguments.firstOrNull()
        return when (name) {
            in FIXED -> FIXED[name]
            in CHARACTER_OR_CLOB -> when (first?.familyOrNull) {
                CLOB -> "CLOB"
                in TO_CHARACTER -> "VARCHAR2"
                else -> null
            }
            in POSITION -> when (first?.familyOrNull) {
                CLOB, ProjectDatatypeFamily.BLOB -> "INTEGER"
                in TO_CHARACTER -> "PLS_INTEGER"
                else -> null
            }
            in NUMERIC_WITH_INTEGER -> when {
                first?.familyOrNull == DATETIME && name != "ABS" && first.rank == OverloadRank.DATE -> "DATE"
                else -> numeric(arguments, integer = true)
            }
            in NUMERIC_ONLY, "MOD", "POWER" -> numeric(arguments, integer = false)
            "CONCAT" -> if (arguments.any { it.familyOrNull == CLOB }) "CLOB" else "VARCHAR2".takeIf { arguments.all { it.isNull || it.familyOrNull in TO_CHARACTER } }
            // The overload is chosen by the first argument.
            "NVL", "GREATEST", "LEAST" -> byFirst(arguments)
            "COALESCE" -> common(arguments, sql)
            else -> null
        }
    }

    /**
     * NUMBER for numeric arguments, PLS_INTEGER when every one is a PLS_INTEGER and [integer] (the function has that
     * overload); null for another argument (BINARY_FLOAT and BINARY_DOUBLE are not modeled).
     */
    private fun numeric(arguments: List<ProjectArgumentType>, integer: Boolean): String? {
        val ranks = arguments.filter { !it.isNull }.map { if (it.familyOrNull == NUMERIC) it.rank else null }
        if (ranks.any { it != OverloadRank.NUMBER && it != OverloadRank.PLS_INTEGER }) return null
        return if (integer && ranks.isNotEmpty() && ranks.all { it == OverloadRank.PLS_INTEGER }) "PLS_INTEGER" else "NUMBER"
    }

    private fun byFirst(arguments: List<ProjectArgumentType>): String? {
        val first = arguments.first()
        return when (first.familyOrNull) {
            CHARACTER -> "VARCHAR2"
            CLOB -> "CLOB"
            ProjectDatatypeFamily.BOOLEAN -> "BOOLEAN"
            DATETIME -> "DATE".takeIf { first.rank == OverloadRank.DATE }
            NUMERIC -> numericOf(arguments)
            else -> null
        }
    }

    /** The NUMBER or PLS_INTEGER overload of a numeric [arguments]' first one: PLS_INTEGER only when every numeric one is. */
    private fun numericOf(arguments: List<ProjectArgumentType>): String? {
        val ranks = arguments.filter { it.familyOrNull == NUMERIC }.map { it.rank }
        if (ranks.any { it != OverloadRank.NUMBER && it != OverloadRank.PLS_INTEGER }) return null
        return if (ranks.all { it == OverloadRank.PLS_INTEGER }) "PLS_INTEGER" else "NUMBER"
    }

    /**
     * The type of a `CASE` or `COALESCE` from its results: the type they share; in PL/SQL, a number or a date before a
     * character value (`'a'` with a number is a number, with a date a date). SQL rejects results of different families
     * (ORA-00932). Null for anything else.
     */
    fun common(results: List<ProjectArgumentType>, sql: Boolean = false): String? {
        if (results.any { !it.isNull && it.familyOrNull == null }) return null
        val typed = results.filter { !it.isNull }
        if (typed.isEmpty()) return null
        val families = typed.map { it.familyOrNull!! }.toSet()
        if (sql && families.size > 1) return null
        return when {
            families == setOf(CHARACTER) -> "VARCHAR2"
            families == setOf(CLOB) -> "CLOB"
            families == setOf(ProjectDatatypeFamily.BOOLEAN) -> "BOOLEAN"
            families.all { it == NUMERIC || it == CHARACTER } -> numericOf(typed)
            families.all { it == DATETIME || it == CHARACTER } && typed.all { it.familyOrNull != DATETIME || it.rank == OverloadRank.DATE } -> "DATE"
            else -> null
        }
    }

    /**
     * `a + b`, `a - b`, `a * b`, `a / b` and `a MOD b` ([operators] between the [operands], left to right): a number,
     * PLS_INTEGER when every operand is one and no `/` or `MOD` is applied; a date plus or minus a number is a date,
     * a date minus a date a number. Null for anything else.
     */
    fun arithmetic(operands: List<ProjectArgumentType>, operators: List<String>): String? {
        if (operands.any { it.familyOrNull == null }) return null
        var result = typeOf(operands.first()) ?: return null
        for (i in operators.indices) result = step(result, operands[i + 1], operators[i]) ?: return null
        return result
    }

    private fun typeOf(value: ProjectArgumentType): String? = when {
        value.familyOrNull == NUMERIC && value.rank == OverloadRank.PLS_INTEGER -> "PLS_INTEGER"
        value.familyOrNull == NUMERIC && value.rank == OverloadRank.NUMBER -> "NUMBER"
        value.familyOrNull == DATETIME && value.rank == OverloadRank.DATE -> "DATE"
        else -> null
    }

    private fun step(left: String, right: ProjectArgumentType, operator: String): String? {
        val other = typeOf(right) ?: return null
        val numbers = setOf("NUMBER", "PLS_INTEGER")
        return when {
            left in numbers && other in numbers -> if (left == "PLS_INTEGER" && other == "PLS_INTEGER" && operator in setOf("+", "-", "*")) "PLS_INTEGER" else "NUMBER"
            left == "DATE" && other in numbers && operator in setOf("+", "-") -> "DATE"
            left in numbers && other == "DATE" && operator == "+" -> "DATE"
            left == "DATE" && other == "DATE" && operator == "-" -> "NUMBER"
            else -> null
        }
    }

    private val ProjectArgumentType.isNull get() = kind == ProjectArgumentType.Kind.NULL_LITERAL
}
