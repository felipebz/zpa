//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.project.ProjectSymbolIndex
import com.felipebz.zpa.project.QualifiedName

@OptIn(ZpaExperimentalApi::class)
internal class ProjectSynonymScope(val metadata: ProjectExternalMetadata, val schema: ProjectIdentifier)

/**
 * A written name read through synonyms: [name] is the name to look up in the project (the target followed by the rest
 * of the written name), [target] the object the synonyms end at, null when they end at none (a database link, or a
 * synonym that resolves to itself).
 */
@OptIn(ZpaExperimentalApi::class)
internal class SynonymReading(val name: QualifiedName, val synonyms: List<ProjectExternalSynonym>, val target: ProjectQualifiedName?)

/**
 * Oracle reads the first segment of a name as an object of the compilation schema, then as a private synonym of that
 * schema, then as a public synonym; a synonym leads to its target, which may be another synonym.
 */
@OptIn(ZpaExperimentalApi::class)
internal object ProjectSynonymNames {
    /** The reading of [written] through the synonyms of [scope]; null when no synonym takes its first segment. */
    fun read(index: ProjectSymbolIndex, scope: ProjectSynonymScope, written: QualifiedName): SynonymReading? {
        val metadata = scope.metadata
        val schema = scope.schema
        val first = written.segments.first()
        if (index.hasUnitPossiblyIn(schema.identifier, first)) return null
        val firstId = ProjectIdentifier.wrap(first)
        val own = metadata.findObject(schema, firstId).orElse(null)
        var synonym = when {
            own != null && own.kind != ProjectExternalObject.Kind.SYNONYM -> return null
            own != null -> metadata.findSynonym(schema, firstId).orElse(null) ?: return null
            // An object of the compilation schema the metadata does not hold would come before a public synonym.
            metadata.isSchemaComplete(schema) -> metadata.findSynonym(ProjectExternalSynonym.PUBLIC, firstId).orElse(null) ?: return null
            else -> return null
        }
        val rest = written.segments.drop(1)
        val synonyms = ArrayList<ProjectExternalSynonym>()
        while (true) {
            // Oracle rejects a synonym that resolves to itself (ORA-01775).
            if (synonym in synonyms) return SynonymReading(written, synonyms, null)
            synonyms += synonym
            if (synonym.dbLink.isPresent) return SynonymReading(written, synonyms, null)
            val owner = synonym.targetOwner.get()
            val name = synonym.targetName
            val held = owner != ProjectExternalSynonym.PUBLIC && index.hasUnitPossiblyIn(owner.identifier, name.identifier)
            synonym = (if (held) null else metadata.findSynonym(owner, name).orElse(null))
                ?: return SynonymReading(QualifiedName(listOf(owner.identifier, name.identifier) + rest), synonyms, ProjectQualifiedName.ofIdentifiers(listOf(owner, name)))
        }
    }
}
