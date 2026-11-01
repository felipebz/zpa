//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
