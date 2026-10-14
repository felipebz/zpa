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
import com.felipebz.zpa.api.project.ProjectExternalColumn
import com.felipebz.zpa.api.project.ProjectExternalDatatype
import com.felipebz.zpa.api.project.ProjectExternalMetadata
import com.felipebz.zpa.api.project.ProjectExternalRelation
import java.math.BigDecimal
import java.time.Instant

/** Session context of the acquiring connection. */
internal class SessionInfo(val sessionUser: String, val currentSchema: String, val container: String, val database: String)

/**
 * One `ALL_ALL_TABLES` or `ALL_VIEWS` row; [typeName] is set for object tables. [columnsAcquired] states that the
 * relation's user-generated columns were queried over the same scope in the same transaction, which is what makes it
 * column-complete (independently of how many columns were returned).
 */
internal class RelationRow(val schema: String, val name: String, val typeOwner: String?, val typeName: String?, val columnsAcquired: Boolean)

/** One user-generated `ALL_TAB_COLS` row, values as read (SQL NULL stays null). */
internal class ColumnRow(
    val schema: String,
    val relation: String,
    val name: String,
    val columnId: BigDecimal?,
    val dataType: String?,
    val dataTypeOwner: String?,
    val dataLength: BigDecimal?,
    val charLength: BigDecimal?,
    val charUsed: String?,
    val precision: BigDecimal?,
    val scale: BigDecimal?
)

/** Pure mapping from dictionary rows to S8 descriptors; no JDBC. */
@OptIn(ZpaExperimentalApi::class)
internal object OracleDictionaryMapping {
    private val CHARACTER_TYPES = setOf("CHAR", "VARCHAR2", "NCHAR", "NVARCHAR2")

    /**
     * The declared datatype of a column. `DATA_TYPE` and `DATA_TYPE_OWNER` are kept verbatim. A length is supplied
     * only where it is a declared length: character types in their `CHAR_USED` semantics and `RAW` in bytes; storage
     * lengths (NUMBER 22, DATE 7, LOB locators) are never used. SQL NULL precision or scale stays absent.
     */
    fun datatype(row: ColumnRow): ProjectExternalDatatype? {
        val type = row.dataType ?: return null
        var length: Int? = null
        var semantics: ProjectExternalDatatype.LengthSemantics? = null
        if (row.dataTypeOwner == null && type in CHARACTER_TYPES) {
            when (row.charUsed) {
                "C" -> row.charLength?.let { length = it.intValueExact(); semantics = ProjectExternalDatatype.LengthSemantics.CHAR }
                "B" -> row.dataLength?.let { length = it.intValueExact(); semantics = ProjectExternalDatatype.LengthSemantics.BYTE }
            }
        } else if (row.dataTypeOwner == null && type == "RAW") {
            row.dataLength?.let { length = it.intValueExact(); semantics = ProjectExternalDatatype.LengthSemantics.BYTE }
        }
        return ProjectExternalDatatype.of(type, row.dataTypeOwner, length, semantics, row.precision?.intValueExact(), row.scale?.intValueExact())
    }

    /** The column descriptor; a NULL `COLUMN_ID` (invisible column) stays without position. */
    fun column(row: ColumnRow): ProjectExternalColumn = ProjectExternalColumn.of(row.name, row.columnId?.intValueExact(), datatype(row))

    /**
     * Builds the snapshot. A relation is column-complete exactly when its columns were acquired with it
     * ([RelationRow.columnsAcquired]); a requested schema is declared complete only when it is the session user's own
     * schema (whose `ALL_*` rows are not filtered by privileges). Columns of relations not returned by the relation
     * queries are ignored.
     *
     * @throws OracleMetadataException if a relation appears twice, or the rows violate a descriptor invariant
     * (duplicate column names or positions)
     */
    fun assemble(
        request: OracleMetadataRequest,
        session: SessionInfo,
        relations: List<RelationRow>,
        columns: List<ColumnRow>,
        acquiredAt: Instant
    ): OracleMetadataResult {
        val columnsByRelation = columns.groupBy { it.schema to it.relation }
        val origin = "oracle ${session.database}/${session.container} as ${session.sessionUser} at $acquiredAt"
        val builder = ProjectExternalMetadata.builder(origin)
        val complete = request.schemas.filter { it == session.sessionUser }
        complete.forEach { builder.completeSchema(it) }
        val seen = HashSet<Pair<String, String>>()
        var columnCount = 0
        for (relation in relations) {
            val key = relation.schema to relation.name
            if (!seen.add(key)) throw OracleMetadataException("The dictionary returned relation ${relation.schema}.${relation.name} more than once", null)
            val rows = columnsByRelation[key].orEmpty()
            try {
                val descriptors = rows.map { column(it) }
                val descriptor = if (relation.typeName != null) {
                    val rowType = ProjectExternalDatatype.of(relation.typeName, relation.typeOwner, null, null, null, null)
                    ProjectExternalRelation.objectTable(relation.schema, relation.name, rowType, relation.columnsAcquired, descriptors)
                } else {
                    ProjectExternalRelation.of(relation.schema, relation.name, relation.columnsAcquired, descriptors)
                }
                builder.relation(descriptor)
            } catch (e: IllegalArgumentException) {
                throw OracleMetadataException("Inconsistent dictionary metadata for ${relation.schema}.${relation.name}: ${e.message}", e)
            } catch (e: ArithmeticException) {
                throw OracleMetadataException("Out-of-range dictionary value for ${relation.schema}.${relation.name}", e)
            }
            columnCount += rows.size
        }
        val notVisible = request.relations.filter { it.schema !in request.schemas && (it.schema to it.name) !in seen }
        return OracleMetadataResult(
            builder.build(), session.sessionUser, session.currentSchema, session.database, session.container, acquiredAt,
            complete, notVisible, seen.size, columnCount
        )
    }
}
