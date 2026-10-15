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
package com.felipebz.zpa.squid

import java.util.Collections
import java.util.Locale

/** A `PLSQL_CCFLAGS` value: `TRUE`, `FALSE`, `NULL` or a `PLS_INTEGER`. */
sealed class CcValue {
    data object True : CcValue()
    data object False : CcValue()
    data object Null : CcValue()
    data class Integer(val value: Int) : CcValue()
}

/** The target database version, as `DBMS_DB_VERSION.VERSION` and `DBMS_DB_VERSION.RELEASE`. */
data class DatabaseVersion(val version: Int, val release: Int)

/**
 * How the analyzed code is compiled: the `PLSQL_CCFLAGS` values and the target database version. With neither
 * ([isActive] false) conditional compilation conditions are not evaluated at all. Immutable.
 */
class ConditionalCompilationSettings(flags: Map<String, CcValue> = emptyMap(), val databaseVersion: DatabaseVersion? = null) {

    /** Flag values by upper-cased name. */
    val flags: Map<String, CcValue> =
        Collections.unmodifiableMap(LinkedHashMap(flags.mapKeys { it.key.uppercase(Locale.ROOT) }))

    val isActive: Boolean get() = flags.isNotEmpty() || databaseVersion != null

    override fun equals(other: Any?) =
        other is ConditionalCompilationSettings && flags == other.flags && databaseVersion == other.databaseVersion

    override fun hashCode() = 31 * flags.hashCode() + databaseVersion.hashCode()

    override fun toString() = "ConditionalCompilationSettings(flags=$flags, databaseVersion=$databaseVersion)"

    companion object {
        @JvmField
        val NONE = ConditionalCompilationSettings()

        private val FLAG_NAME = Regex("[A-Za-z][A-Za-z0-9_$#]*")
        private val INTEGER = Regex("-?\\d+")
        private val VERSION = Regex("(\\d+)(?:\\.(\\d+))?")

        /**
         * Parses the `PLSQL_CCFLAGS` syntax `name1:value1, name2:value2`. Values are `TRUE`, `FALSE`, `NULL` (any case)
         * or a `PLS_INTEGER`; a later duplicate overrides an earlier one. Blank text gives no flags.
         *
         * @throws IllegalArgumentException for anything else
         */
        @JvmStatic
        fun parseFlags(text: String): Map<String, CcValue> {
            val result = LinkedHashMap<String, CcValue>()
            if (text.isBlank()) return result
            for (entry in text.split(',')) {
                val parts = entry.split(':')
                require(parts.size == 2) { "Invalid conditional compilation flag '${entry.trim()}': expected name:value" }
                val name = parts[0].trim()
                val value = parts[1].trim()
                require(FLAG_NAME.matches(name)) { "Invalid conditional compilation flag name '$name'" }
                result[name.uppercase(Locale.ROOT)] = when {
                    value.equals("TRUE", ignoreCase = true) -> CcValue.True
                    value.equals("FALSE", ignoreCase = true) -> CcValue.False
                    value.equals("NULL", ignoreCase = true) -> CcValue.Null
                    INTEGER.matches(value) -> CcValue.Integer(
                        value.toIntOrNull() ?: throw IllegalArgumentException("Conditional compilation flag '$name' is out of the PLS_INTEGER range: $value")
                    )
                    else -> throw IllegalArgumentException("Invalid value '$value' for conditional compilation flag '$name': expected TRUE, FALSE, NULL or an integer")
                }
            }
            return result
        }

        /**
         * Parses a database version such as `19`, `19.0` or `12.2`; the release defaults to 0.
         *
         * @throws IllegalArgumentException for anything else
         */
        @JvmStatic
        fun parseVersion(text: String): DatabaseVersion {
            val match = VERSION.matchEntire(text.trim())
                ?: throw IllegalArgumentException("Invalid database version '$text': expected version or version.release, e.g. 19 or 12.2")
            val version = match.groupValues[1].toIntOrNull()
            val release = match.groupValues[2].ifEmpty { "0" }.toIntOrNull()
            require(version != null && release != null) { "Invalid database version '$text'" }
            return DatabaseVersion(version, release)
        }

        /** Settings from the textual forms used by frontends; blank or null text means not configured. */
        @JvmStatic
        fun of(flagsText: String?, versionText: String?): ConditionalCompilationSettings =
            ConditionalCompilationSettings(
                if (flagsText.isNullOrBlank()) emptyMap() else parseFlags(flagsText),
                if (versionText.isNullOrBlank()) null else parseVersion(versionText)
            )
    }
}
