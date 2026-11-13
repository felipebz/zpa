//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.util.*

/**
 * The name of an object of an external schema that can take the first segment of an anchor or of a qualified name
 * (an `ALL_OBJECTS` row of one of the [Kind]s), identified by its exact dictionary schema and name. Only the name and
 * the kind are known: a table or view described by the metadata is a [ProjectExternalRelation], a synonym a
 * [ProjectExternalSynonym].
 */
@ZpaExperimentalApi
class ProjectExternalObject private constructor(val schema: ProjectIdentifier, val name: ProjectIdentifier, val kind: Kind) {
    enum class Kind { TABLE, VIEW, SYNONYM, PACKAGE, TYPE, SEQUENCE, PROCEDURE, FUNCTION }

    /** `[schema, name]`, each one segment. */
    val qualifiedName: ProjectQualifiedName = ProjectQualifiedName.ofIdentifiers(listOf(schema, name))

    override fun equals(other: Any?): Boolean =
        other is ProjectExternalObject && schema == other.schema && name == other.name && kind == other.kind

    override fun hashCode(): Int = Objects.hash(schema, name, kind)

    override fun toString(): String = "$kind $qualifiedName"

    companion object {
        /**
         * @param dictionarySchema `OWNER` exactly as in the data dictionary (`PUBLIC` for a public synonym)
         * @param dictionaryName `OBJECT_NAME` exactly as in the data dictionary
         * @throws IllegalArgumentException for an invalid name
         */
        @JvmStatic
        fun of(dictionarySchema: String, dictionaryName: String, kind: Kind): ProjectExternalObject =
            ProjectExternalObject(dictionaryIdentifier(dictionarySchema, "schema"), dictionaryIdentifier(dictionaryName, "object"), kind)

        @JvmSynthetic
        internal fun of(schema: ProjectIdentifier, name: ProjectIdentifier, kind: Kind) = ProjectExternalObject(schema, name, kind)
    }
}
