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
package com.felipebz.zpa.oracle.metadata

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.util.*

/**
 * A relation identified by exact Oracle dictionary names (as stored in `ALL_OBJECTS.OWNER`/`OBJECT_NAME`: unquoted
 * names are upper case, quoted names keep their case). Names are compared exactly; nothing is upper-cased or quoted.
 */
@ZpaExperimentalApi
class OracleRelationName private constructor(val schema: String, val name: String) {
    override fun equals(other: Any?): Boolean = other is OracleRelationName && schema == other.schema && name == other.name

    override fun hashCode(): Int = Objects.hash(schema, name)

    override fun toString(): String = "$schema.$name"

    companion object {
        /** @throws IllegalArgumentException if a name is blank or contains a double quote */
        @JvmStatic
        fun of(schema: String, name: String): OracleRelationName =
            OracleRelationName(dictionaryName(schema, "schema"), dictionaryName(name, "relation"))

        internal fun dictionaryName(value: String, what: String): String {
            require(value.isNotBlank()) { "The $what name must not be blank" }
            require(!value.contains('"')) { "The $what name must be an exact dictionary name, without quotes: $value" }
            return value
        }
    }
}
