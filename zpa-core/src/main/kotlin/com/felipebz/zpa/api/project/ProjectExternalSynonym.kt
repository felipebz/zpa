//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.util.*

/**
 * An `ALL_SYNONYMS` row: the synonym [owner].[name] (owner `PUBLIC` for a public synonym) stands for
 * [targetOwner].[targetName], at [dbLink] when the target is in another database. Names are exact dictionary names.
 */
@ZpaExperimentalApi
class ProjectExternalSynonym private constructor(
    val owner: ProjectIdentifier,
    val name: ProjectIdentifier,
    targetOwner: ProjectIdentifier?,
    val targetName: ProjectIdentifier,
    dbLink: String?
) {
    /** `TABLE_OWNER`; the dictionary may leave it empty for a synonym over a database link. */
    val targetOwner: Optional<ProjectIdentifier> = Optional.ofNullable(targetOwner)

    /** `DB_LINK`, as the dictionary writes it. */
    val dbLink: Optional<String> = Optional.ofNullable(dbLink)

    val isPublic: Boolean get() = owner == PUBLIC

    override fun equals(other: Any?): Boolean =
        other is ProjectExternalSynonym && owner == other.owner && name == other.name && targetOwner == other.targetOwner &&
            targetName == other.targetName && dbLink == other.dbLink

    override fun hashCode(): Int = Objects.hash(owner, name, targetOwner, targetName, dbLink)

    override fun toString(): String =
        "$owner.$name -> ${targetOwner.map { "$it." }.orElse("")}$targetName${dbLink.map { "@$it" }.orElse("")}"

    companion object {
        internal val PUBLIC: ProjectIdentifier = dictionaryIdentifier("PUBLIC", "schema")

        /**
         * @param owner `OWNER` (`PUBLIC` for a public synonym)
         * @param name `SYNONYM_NAME`
         * @param targetOwner `TABLE_OWNER`, null only for a synonym over a database link
         * @param targetName `TABLE_NAME`
         * @param dbLink `DB_LINK`, or null for a local target
         * @throws IllegalArgumentException for an invalid name, or a missing target owner of a local target
         */
        @JvmStatic
        fun of(owner: String, name: String, targetOwner: String?, targetName: String, dbLink: String?): ProjectExternalSynonym {
            require(targetOwner != null || dbLink != null) { "A local synonym target needs its owner: $owner.$name" }
            require(dbLink == null || dbLink.isNotBlank()) { "The database link must not be blank" }
            return ProjectExternalSynonym(
                dictionaryIdentifier(owner, "schema"), dictionaryIdentifier(name, "synonym"),
                targetOwner?.let { dictionaryIdentifier(it, "schema") }, dictionaryIdentifier(targetName, "object"), dbLink
            )
        }
    }
}
