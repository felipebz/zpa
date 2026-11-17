//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.project.*
import java.util.*

/**
 * The tables created in the project's sources, by schema and name. A table written without an owner is in its file's
 * schema; a file without one contributes no such table. Of two tables with the same schema and name, the first in file
 * order is kept. A table the sources change with `ALTER TABLE` is not complete: it keeps the columns of its CREATE
 * TABLE unless a column changes, and their nullability is unknown once a constraint changes ([TableChange]).
 */
@OptIn(ZpaExperimentalApi::class)
internal class SourceRelations private constructor(val byName: Map<Pair<ProjectIdentifier, ProjectIdentifier>, ProjectExternalRelation>) {
    companion object {
        val NONE = SourceRelations(emptyMap())

        fun of(
            tablesByFile: Map<FileId, List<SourceTable>>,
            alteredByFile: Map<FileId, List<AlteredTable>>,
            schemasByFile: Map<FileId, OracleIdentifier>
        ): SourceRelations {
            if (tablesByFile.isEmpty()) return NONE
            val changes = HashMap<Pair<OracleIdentifier, OracleIdentifier>, MutableSet<TableChange>>()
            for ((fileId, altered) in alteredByFile) {
                for (table in altered) {
                    val schema = table.owner ?: schemasByFile[fileId] ?: continue
                    changes.getOrPut(schema to table.name) { EnumSet.noneOf(TableChange::class.java) } += table.changes
                }
            }
            val relations = LinkedHashMap<Pair<ProjectIdentifier, ProjectIdentifier>, ProjectExternalRelation>()
            for ((fileId, tables) in tablesByFile.entries.sortedBy { it.key.value }) {
                for (table in tables) {
                    val schema = table.owner ?: schemasByFile[fileId] ?: continue
                    val key = stored(schema, "schema") to stored(table.name, "relation")
                    if (key in relations) continue
                    relations[key] = relation(key, table, changes[schema to table.name].orEmpty())
                }
            }
            return SourceRelations(Collections.unmodifiableMap(relations))
        }

        private fun relation(key: Pair<ProjectIdentifier, ProjectIdentifier>, table: SourceTable, changes: Set<TableChange>): ProjectExternalRelation {
            if (TableChange.COLUMNS in changes) return ProjectExternalRelation.fromSource(key.first, key.second, false, emptyList())
            val nullabilityKnown = TableChange.CONSTRAINTS !in changes
            var position = 0
            val columns = table.columns.map { column ->
                ProjectExternalColumn.fromSource(
                    stored(column.name, "column"),
                    if (column.invisible) null else ++position,
                    column.datatype?.let { ProjectExternalDatatype.of(it, null, null, null, null, null) },
                    if (nullabilityKnown) !column.notNull else null
                )
            }
            return ProjectExternalRelation.fromSource(key.first, key.second, table.columnsComplete && changes.isEmpty(), columns)
        }

        /** [name] as Oracle stores it: an unquoted name in upper case, a quoted one as written. */
        private fun stored(name: OracleIdentifier, what: String) = dictionaryIdentifier(name.lookupName, what)
    }
}
