//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.project

/**
 * `CREATE [OR REPLACE] [PUBLIC] SYNONYM [owner.]name FOR [schema.]object[@link]` written in a project source, as
 * written: a missing owner or target schema is decided when the index knows the file's schema.
 */
internal class SourceSynonym(
    val fileId: FileId,
    val isPublic: Boolean,
    val owner: OracleIdentifier?,
    val name: OracleIdentifier,
    val targetOwner: OracleIdentifier?,
    val targetName: OracleIdentifier,
    val dbLink: String?
)
