//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.oracle.metadata

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.squid.DatabaseVersion
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

/** Session context of the acquiring connection. */
internal class SessionInfo(val sessionUser: String, val currentSchema: String, val container: String, val database: String)

/**
 * One `ALL_ALL_TABLES` or `ALL_VIEWS` row ([view]); [typeName] is set for object tables and object views. [columnsAcquired]
 * states that the relation's user-generated columns were queried over the same scope in the same transaction, which is
 * what makes it column-complete (independently of how many columns were returned). [valid] is false for a view whose
 * status is not `VALID`.
 */
internal class RelationRow(
    val schema: String,
    val name: String,
    val typeOwner: String?,
    val typeName: String?,
    val columnsAcquired: Boolean,
    val view: Boolean = false,
    val valid: Boolean = true
)

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
    val scale: BigDecimal?,
    val nullable: String? = null
)

/** One `ALL_OBJECTS` row. */
internal class ObjectRow(val schema: String, val name: String, val type: String)

/** One `ALL_SYNONYMS` row, values as read (SQL NULL stays null). */
internal class SynonymRow(val owner: String, val name: String, val targetOwner: String?, val targetName: String?, val dbLink: String?)

/** One `ALL_TYPES` row. */
internal class TypeRow(val owner: String, val name: String, val typecode: String?, val supertypeOwner: String?, val supertypeName: String?)

/** One `ALL_TYPE_ATTRS` row of an attribute the type declares itself, values as read. */
internal class TypeAttributeRow(
    val owner: String,
    val type: String,
    val name: String,
    val typeMod: String?,
    val typeOwner: String?,
    val typeName: String?,
    val length: BigDecimal?,
    val precision: BigDecimal?,
    val scale: BigDecimal?,
    val charUsed: String?
)

/** One `ALL_TYPE_METHODS` row of a method the type declares itself; [results] is `RESULTS` (0 for a procedure). */
internal class TypeMethodRow(val owner: String, val type: String, val name: String, val number: BigDecimal, val results: BigDecimal?)

/** One `ALL_METHOD_PARAMS` row, `SELF` included. */
internal class MethodParameterRow(
    val owner: String,
    val type: String,
    val method: String,
    val number: BigDecimal,
    val name: String,
    val mode: String?,
    val typeMod: String?,
    val typeOwner: String?,
    val typeName: String?
)

/** One `ALL_METHOD_RESULTS` row. */
internal class MethodResultRow(
    val owner: String,
    val type: String,
    val method: String,
    val number: BigDecimal,
    val typeMod: String?,
    val typeOwner: String?,
    val typeName: String?
)

/** The rows of the object types read, each type once, with what it declares. */
internal class TypeRows(
    val types: List<TypeRow> = emptyList(),
    val attributes: List<TypeAttributeRow> = emptyList(),
    val methods: List<TypeMethodRow> = emptyList(),
    val parameters: List<MethodParameterRow> = emptyList(),
    val results: List<MethodResultRow> = emptyList()
)

/**
 * One `ALL_ARGUMENTS` row at `DATA_LEVEL` 0: a parameter, or the result of a function at position 0. A subprogram
 * without parameters has only its result row, a function's; a procedure without parameters has no row.
 */
internal class ArgumentRow(
    val owner: String,
    val packageName: String?,
    val name: String,
    val subprogramId: BigDecimal,
    val overload: String?,
    val argumentName: String?,
    val position: BigDecimal,
    val mode: String?,
    val dataType: String?,
    val plsType: String?,
    val typeOwner: String?,
    val typeName: String?,
    val typeSubname: String?,
    val defaulted: String?
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

    /**
     * The column descriptor; a NULL `COLUMN_ID` (invisible column) stays without position. `NULLABLE` is `N` only for a
     * column under an enabled, validated, not deferrable `NOT NULL` or primary key, or for a view's column Oracle derives
     * from one; a disabled, `NOVALIDATE` or deferrable constraint shows `Y`. An invalid view's `NULLABLE` may be out of
     * date ([trusted] false): its `N` is not kept.
     */
    fun column(row: ColumnRow, trusted: Boolean = true): ProjectExternalColumn {
        val nullable = when {
            row.nullable == "Y" -> true
            row.nullable == "N" && trusted -> false
            else -> null
        }
        return ProjectExternalColumn.of(row.name, row.columnId?.intValueExact(), datatype(row), nullable)
    }

    /**
     * The kind of an `ALL_OBJECTS` `OBJECT_TYPE`, or null for a type that cannot start a qualified name (bodies,
     * indexes, triggers...). A materialized view is a table: its container table has the same name.
     */
    fun objectKind(objectType: String): ProjectExternalObject.Kind? = when (objectType) {
        "TABLE", "MATERIALIZED VIEW" -> ProjectExternalObject.Kind.TABLE
        "VIEW" -> ProjectExternalObject.Kind.VIEW
        "SYNONYM" -> ProjectExternalObject.Kind.SYNONYM
        "PACKAGE" -> ProjectExternalObject.Kind.PACKAGE
        "TYPE" -> ProjectExternalObject.Kind.TYPE
        "SEQUENCE" -> ProjectExternalObject.Kind.SEQUENCE
        "PROCEDURE" -> ProjectExternalObject.Kind.PROCEDURE
        "FUNCTION" -> ProjectExternalObject.Kind.FUNCTION
        else -> null
    }

    /** The kind of a `TYPECODE`: an object, a collection, or a type whose representation the dictionary does not show. */
    fun objectTypeKind(typecode: String?): ProjectExternalObjectType.Kind = when (typecode) {
        "OBJECT" -> ProjectExternalObjectType.Kind.OBJECT
        "COLLECTION" -> ProjectExternalObjectType.Kind.COLLECTION
        else -> ProjectExternalObjectType.Kind.OPAQUE
    }

    /**
     * The datatype of an attribute, a parameter or a result, as [datatype] reads a column's: none for a `REF` or another
     * modifier, whose target is not the value's own type.
     */
    private fun typeDatatype(
        name: String?,
        owner: String?,
        mod: String?,
        length: BigDecimal? = null,
        charUsed: String? = null,
        precision: BigDecimal? = null,
        scale: BigDecimal? = null
    ): ProjectExternalDatatype? {
        if (name == null || mod != null) return null
        return datatype(ColumnRow("", "", "", null, name, owner, length, length, charUsed, precision, scale))
    }

    /**
     * The object types of [rows], each with the attributes and methods it declares itself. A method whose first
     * parameter is `SELF` is a member method, any other a static one; one with results is a function.
     *
     * @throws IllegalArgumentException if the rows cannot form a type descriptor
     */
    fun objectTypes(rows: TypeRows): List<ProjectExternalObjectType> {
        val attributes = rows.attributes.groupBy { it.owner to it.type }
        val methods = rows.methods.groupBy { it.owner to it.type }
        val parameters = rows.parameters.groupBy { listOf(it.owner, it.type, it.method, it.number.toPlainString()) }
        val results = rows.results.associateBy { listOf(it.owner, it.type, it.method, it.number.toPlainString()) }
        return rows.types.map { type ->
            val key = type.owner to type.name
            val declared = attributes[key].orEmpty().map {
                ProjectExternalTypeAttribute.of(it.name, typeDatatype(it.typeName, it.typeOwner, it.typeMod, it.length, it.charUsed, it.precision, it.scale))
            }
            val declaredMethods = methods[key].orEmpty().map { method ->
                val methodKey = listOf(method.owner, method.type, method.name, method.number.toPlainString())
                val all = parameters[methodKey].orEmpty()
                val static = all.firstOrNull()?.name != "SELF"
                val formal = all.filter { it.name != "SELF" }.map {
                    ProjectExternalMethodParameter.of(it.name, parameterMode(it.mode), typeDatatype(it.typeName, it.typeOwner, it.typeMod))
                }
                val result = results[methodKey]
                if (result != null || (method.results?.signum() ?: 0) > 0) {
                    ProjectExternalTypeMethod.function(method.name, static, formal, result?.let { typeDatatype(it.typeName, it.typeOwner, it.typeMod) })
                } else {
                    ProjectExternalTypeMethod.procedure(method.name, static, formal)
                }
            }
            ProjectExternalObjectType.of(type.owner, type.name, objectTypeKind(type.typecode), type.supertypeOwner, type.supertypeName, declared, declaredMethods)
        }
    }

    private fun parameterMode(mode: String?): ProjectExternalMethodParameter.Mode = when (mode) {
        "OUT" -> ProjectExternalMethodParameter.Mode.OUT
        "IN/OUT", "IN OUT" -> ProjectExternalMethodParameter.Mode.IN_OUT
        else -> ProjectExternalMethodParameter.Mode.IN
    }

    /**
     * The datatype of an argument: its `PLS_TYPE` when set (`PLS_INTEGER`, `INTEGER`, `BOOLEAN`), else its `DATA_TYPE`;
     * a type a package declares is named by the package, with the type as the parameter's package type; an object type
     * or a collection by its owner and name. A `%ROWTYPE` has none.
     */
    private fun argumentDatatype(row: ArgumentRow): ProjectExternalDatatype? {
        if (row.plsType == "ROWTYPE") return null
        if (row.typeOwner != null && row.typeName != null) return ProjectExternalDatatype.of(row.typeName, row.typeOwner, null, null, null, null)
        val name = row.plsType ?: row.dataType ?: return null
        return ProjectExternalDatatype.of(name, null, null, null, null, null)
    }

    /** The subprograms of [rows], each overload apart; the row at position 0 is a function's result. */
    fun subprograms(rows: List<ArgumentRow>): List<ProjectExternalSubprogram> =
        rows.groupBy { listOf(it.owner, it.packageName, it.name, it.subprogramId.toPlainString()) }.values.map { overload ->
            val first = overload.first()
            val result = overload.firstOrNull { it.position.signum() == 0 }
            val parameters = overload.filter { it.position.signum() > 0 && it.argumentName != null }.map {
                ProjectExternalSubprogramParameter.of(
                    it.argumentName!!, parameterMode(it.mode), argumentDatatype(it), it.typeSubname,
                    when (it.defaulted) { "Y" -> true; "N" -> false; else -> null }
                )
            }
            val number = first.overload?.toIntOrNull()
            if (result != null) {
                ProjectExternalSubprogram.function(first.owner, first.packageName, first.name, number, parameters, argumentDatatype(result), result.typeSubname)
            } else {
                ProjectExternalSubprogram.procedure(first.owner, first.packageName, first.name, number, parameters)
            }
        }

    /** @throws IllegalArgumentException if the row cannot form a synonym descriptor */
    fun synonym(row: SynonymRow): ProjectExternalSynonym {
        val target = requireNotNull(row.targetName) { "The synonym has no target name" }
        return ProjectExternalSynonym.of(row.owner, row.name, row.targetOwner, target, row.dbLink)
    }

    /**
     * Builds the snapshot. A relation is column-complete exactly when its columns were acquired with it
     * ([RelationRow.columnsAcquired]); a requested schema is declared complete when it is the session user's own
     * schema (whose `ALL_*` rows are not filtered by privileges) or when the caller asserted it complete
     * ([OracleMetadataRequest.completeSchemas], not checked against the session's privileges). Columns of relations not
     * returned by the relation queries are ignored. Public synonyms are declared complete only when every public
     * synonym was requested ([OracleMetadataRequest.publicSynonyms] without targets).
     *
     * @throws OracleMetadataException if a relation or synonym appears twice, an object name appears with two kinds,
     * or the rows violate a descriptor invariant (duplicate column names or positions, a synonym without target)
     */
    fun assemble(
        request: OracleMetadataRequest,
        session: SessionInfo,
        relations: List<RelationRow>,
        columns: List<ColumnRow>,
        acquiredAt: Instant,
        version: Result<DatabaseVersion> = Result.failure(IllegalStateException("not read")),
        objects: List<ObjectRow> = emptyList(),
        synonyms: List<SynonymRow> = emptyList(),
        types: TypeRows = TypeRows(),
        typesReadIn: Duration = Duration.ZERO,
        arguments: List<ArgumentRow> = emptyList(),
        argumentsReadIn: Duration = Duration.ZERO
    ): OracleMetadataResult {
        val columnsByRelation = columns.groupBy { it.schema to it.relation }
        val origin = "oracle ${session.database}/${session.container} as ${session.sessionUser} at $acquiredAt"
        val builder = ProjectExternalMetadata.builder(origin)
        val complete = request.schemas.filter { it == session.sessionUser || it in request.completeSchemas }
        complete.forEach { builder.completeSchema(it) }
        val seen = HashSet<Pair<String, String>>()
        var columnCount = 0
        for (relation in relations) {
            val key = relation.schema to relation.name
            if (!seen.add(key)) throw OracleMetadataException("The dictionary returned relation ${relation.schema}.${relation.name} more than once", null)
            val rows = columnsByRelation[key].orEmpty()
            try {
                val descriptors = rows.map { column(it, relation.valid) }
                val rowType = relation.typeName?.let { ProjectExternalDatatype.of(it, relation.typeOwner, null, null, null, null) }
                val descriptor = when {
                    relation.view && rowType != null -> ProjectExternalRelation.objectView(relation.schema, relation.name, rowType, relation.columnsAcquired, descriptors)
                    relation.view -> ProjectExternalRelation.view(relation.schema, relation.name, relation.columnsAcquired, descriptors)
                    rowType != null -> ProjectExternalRelation.objectTable(relation.schema, relation.name, rowType, relation.columnsAcquired, descriptors)
                    else -> ProjectExternalRelation.of(relation.schema, relation.name, relation.columnsAcquired, descriptors)
                }
                builder.relation(descriptor)
            } catch (e: IllegalArgumentException) {
                throw OracleMetadataException("Inconsistent dictionary metadata for ${relation.schema}.${relation.name}: ${e.message}", e)
            } catch (e: ArithmeticException) {
                throw OracleMetadataException("Out-of-range dictionary value for ${relation.schema}.${relation.name}", e)
            }
            columnCount += rows.size
        }
        val objectNames = HashSet<Pair<String, String>>()
        for (row in objects) {
            val kind = objectKind(row.type) ?: continue
            try {
                builder.schemaObject(ProjectExternalObject.of(row.schema, row.name, kind))
            } catch (e: IllegalArgumentException) {
                throw OracleMetadataException("Inconsistent dictionary metadata for object ${row.schema}.${row.name}: ${e.message}", e)
            }
            objectNames += row.schema to row.name
        }
        for (row in synonyms) {
            try {
                builder.synonym(synonym(row))
            } catch (e: IllegalArgumentException) {
                throw OracleMetadataException("Inconsistent dictionary metadata for synonym ${row.owner}.${row.name}: ${e.message}", e)
            }
        }
        val objectTypes = try {
            objectTypes(types)
        } catch (e: IllegalArgumentException) {
            throw OracleMetadataException("Inconsistent dictionary metadata for an object type: ${e.message}", e)
        } catch (e: ArithmeticException) {
            throw OracleMetadataException("Out-of-range dictionary value for an object type", e)
        }
        for (type in objectTypes) {
            try {
                builder.objectType(type)
            } catch (e: IllegalArgumentException) {
                throw OracleMetadataException("The dictionary returned object type ${type.owner}.${type.name} more than once", e)
            }
        }
        val subprograms = try {
            subprograms(arguments)
        } catch (e: IllegalArgumentException) {
            throw OracleMetadataException("Inconsistent dictionary metadata for a subprogram: ${e.message}", e)
        }
        for (subprogram in subprograms) {
            try {
                builder.subprogram(subprogram)
            } catch (e: IllegalArgumentException) {
                throw OracleMetadataException("The dictionary returned subprogram $subprogram more than once", e)
            }
        }
        if (request.publicSynonyms && request.publicSynonymTargets.isEmpty()) builder.completePublicSynonyms()
        val notVisible = request.relations.filter { it.schema !in request.schemas && (it.schema to it.name) !in seen }
        return OracleMetadataResult(
            builder.build(), session.sessionUser, session.currentSchema, session.database, session.container, acquiredAt,
            complete, notVisible, seen.size, columnCount, objectNames.size, synonyms.size,
            version.getOrNull(), version.exceptionOrNull()?.let { it.message ?: it.javaClass.simpleName },
            objectTypes.size, objectTypes.sumOf { it.attributes.size }, objectTypes.sumOf { it.methods.size }, typesReadIn,
            subprograms.size, arguments.size, argumentsReadIn
        )
    }
}
