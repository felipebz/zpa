//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.squid.DatabaseVersion

internal enum class JsonTypeName {
    /** The predefined JSON type. */
    PREDEFINED,

    /** The schema's type or synonym `json`. */
    SCHEMA,

    /** Either one: the target database version is not known. */
    UNDECIDED;

    companion object {
        private const val FIRST_PREDEFINED_RELEASE = 21

        fun of(version: DatabaseVersion?): JsonTypeName = when {
            version == null -> UNDECIDED
            version.version >= FIRST_PREDEFINED_RELEASE -> PREDEFINED
            else -> SCHEMA
        }
    }
}
