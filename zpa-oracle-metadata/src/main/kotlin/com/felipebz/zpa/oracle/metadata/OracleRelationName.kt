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

        /**
         * The dictionary name of [written], one identifier as SQL writes it: unquoted, upper-cased as Oracle does
         * (`hr` is `HR`); in double quotes, exactly as written inside them (`"hr"` is `hr`).
         *
         * @throws IllegalArgumentException if it is blank, quoted other than around the whole name, or unquoted with a space
         */
        @JvmStatic
        fun dictionaryNameOf(written: String): String {
            val name = written.trim()
            require(name.isNotEmpty()) { "The name must not be blank" }
            if (name.length > 2 && name.startsWith('"') && name.endsWith('"')) {
                val inner = name.substring(1, name.length - 1)
                require(!inner.contains('"')) { "Invalid quoted name: $written" }
                return inner
            }
            require(!name.contains('"') && name.none { it.isWhitespace() }) { "Invalid name: $written" }
            return name.uppercase(Locale.ROOT)
        }

        /**
         * The relation `schema.relation` as SQL writes it, each part read as [dictionaryNameOf] reads it; a dot inside
         * double quotes is part of the name.
         *
         * @throws IllegalArgumentException if it is not two names separated by a dot
         */
        @JvmStatic
        fun parse(written: String): OracleRelationName {
            val parts = ArrayList<String>()
            val current = StringBuilder()
            var quoted = false
            for (char in written) {
                if (char == '.' && !quoted) {
                    parts += current.toString()
                    current.clear()
                } else {
                    if (char == '"') quoted = !quoted
                    current.append(char)
                }
            }
            parts += current.toString()
            require(!quoted && parts.size == 2) { "Expected SCHEMA.RELATION: $written" }
            return OracleRelationName(dictionaryNameOf(parts[0]), dictionaryNameOf(parts[1]))
        }

        internal fun dictionaryName(value: String, what: String): String {
            require(value.isNotBlank()) { "The $what name must not be blank" }
            require(!value.contains('"')) { "The $what name must be an exact dictionary name, without quotes: $value" }
            return value
        }
    }
}
