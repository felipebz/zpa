//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.project

import java.util.*
import java.util.concurrent.ConcurrentHashMap

/** Collects immutable per-file facts and freezes them into a deterministic index. */
class ProjectSymbolIndexBuilder {
    private val declarationsByFile = ConcurrentHashMap<FileId, List<ProjectDeclaration>>()
    private val omittedStandaloneCallablesByFile = ConcurrentHashMap<FileId, Set<QualifiedName>>()
    private val alteredTypesByFile = ConcurrentHashMap<FileId, AlteredTypes>()
    private val schemasByFile = ConcurrentHashMap<FileId, OracleIdentifier>()
    private val configuredSchemas = HashSet<OracleIdentifier>()

    /** The schema the units of [fileId] written without an owner compile in. */
    internal fun schema(fileId: FileId, schema: OracleIdentifier) {
        schemasByFile[fileId] = schema
    }

    /** Schemas the configuration names: known even when no declaration of the project is in them. */
    internal fun configuredSchemas(schemas: Collection<OracleIdentifier>) {
        configuredSchemas += schemas
    }

    fun add(fileId: FileId, declarations: Collection<ProjectDeclaration>) = add(fileId, declarations, emptySet())

    /** [omittedStandaloneCallables]: names of standalone callable headings of this file that produced no fact. */
    internal fun add(
        fileId: FileId,
        declarations: Collection<ProjectDeclaration>,
        omittedStandaloneCallables: Set<QualifiedName>,
        alteredTypes: AlteredTypes = AlteredTypes.NONE
    ) {
        require(declarations.all { it.fileId == fileId }) {
            "All declarations added for a file must have the same FileId"
        }
        val copy = immutableList(declarations)
        require(declarationsByFile.putIfAbsent(fileId, copy) == null) {
            "Declarations for $fileId were added more than once"
        }
        if (omittedStandaloneCallables.isNotEmpty()) omittedStandaloneCallablesByFile[fileId] = omittedStandaloneCallables
        if (!alteredTypes.isEmpty()) alteredTypesByFile[fileId] = alteredTypes
    }

    fun build(): ProjectSymbolIndex {
        val sortedFiles = declarationsByFile.entries.sortedBy { it.key.value }
        // Ordinals are positions in each file's frozen sequence; duplicate facts, including the
        // same reference inserted twice, remain distinct occurrences.
        val occurrencesByFile = sortedFiles.associate { (fileId, facts) ->
            fileId to immutableList(
                facts.sortedWith(ProjectSymbolIndex.declarationComparator)
                    .mapIndexed { ordinal, fact -> IndexedDeclaration(fact, ordinal) }
            )
        }
        val omittedStandaloneCallables = omittedStandaloneCallablesByFile.values.flatMapTo(HashSet()) { it }
        return ProjectSymbolIndex(
            occurrencesByFile.keys.toList(), occurrencesByFile, omittedStandaloneCallables, AlteredTypes.merge(alteredTypesByFile.values),
            HashMap(schemasByFile), HashSet(configuredSchemas)
        )
    }
}

/**
 * One canonical indexed declaration occurrence: the authoritative fact plus its zero-based
 * ordinal in its file's frozen declaration sequence. Identity is by reference; it deliberately
 * has no value equality, so equal facts remain separate occurrences.
 */
class IndexedDeclaration internal constructor(
    val declaration: ProjectDeclaration,
    val ordinal: Int
) {
    val fileId: FileId get() = declaration.fileId
}

/**
 * Immutable project-wide declaration registry. It does not perform semantic resolution.
 *
 * Unqualified standalone project declarations belong to one logical project namespace; explicitly qualified
 * declarations remain distinct. Each occurrence also has an effective schema (its unit's written owner, else the
 * schema configured for its file), through which a name qualified by a schema reaches it when nothing has that exact
 * name ([throughSchema]).
 */
class ProjectSymbolIndex private constructor(
    /** Every derived map, built once by the root index and shared by its schema views. */
    private val tables: Tables,
    /** The schema of the unit looking names up, whose declarations an unqualified name prefers; null for none. */
    private val preferredSchema: OracleIdentifier?,
    /** The index this one is a view of; null for the root itself. */
    private val rootIndex: ProjectSymbolIndex?
) {
    internal constructor(
        fileIds: List<FileId>,
        fileOccurrences: Map<FileId, List<IndexedDeclaration>>,
        omittedStandaloneCallables: Set<QualifiedName> = emptySet(),
        alteredTypes: AlteredTypes = AlteredTypes.NONE,
        schemasByFile: Map<FileId, OracleIdentifier> = emptyMap(),
        configuredSchemas: Set<OracleIdentifier> = emptySet()
    ) : this(Tables(fileIds, fileOccurrences, omittedStandaloneCallables, alteredTypes, schemasByFile, configuredSchemas), null, null)

    /** The frozen declarations and every map derived from them; independent of the preferred schema. */
    private class Tables(
        fileIds: List<FileId>,
        fileOccurrences: Map<FileId, List<IndexedDeclaration>>,
        omittedStandaloneCallables: Set<QualifiedName>,
        alteredTypes: AlteredTypes,
        schemasByFile: Map<FileId, OracleIdentifier>,
        configuredSchemas: Set<OracleIdentifier>
    ) {
        val fileIds: List<FileId> = immutableList(fileIds)
        val occurrencesByFile: Map<FileId, List<IndexedDeclaration>> = fileOccurrences.toMap()

        // Name-only standalone coverage: identified standalone callable headings that produced no fact. Never candidates.
        val omittedStandaloneCallables: Set<QualifiedName> =
            Collections.unmodifiableSet(HashSet(omittedStandaloneCallables))

        val alteredTypes: AlteredTypes = alteredTypes

        /** All canonical occurrences in deterministic file, then per-file order. */
        val occurrences: List<IndexedDeclaration> = immutableList(this.fileIds.flatMap { occurrencesByFile.getValue(it) })
        val declarations: List<ProjectDeclaration> = immutableList(occurrences.map { it.declaration })

        /** Returns the frozen declarations belonging to exactly one source file. */
        internal fun declarationsFor(fileId: FileId): List<ProjectDeclaration> =
            immutableList(occurrencesFor(fileId).map { it.declaration })

        /** Returns the frozen occurrences belonging to exactly one source file. */
        fun occurrencesFor(fileId: FileId): List<IndexedDeclaration> = occurrencesByFile[fileId].orEmpty()

        // Built once from the frozen canonical list: every kind, duplicates and canonical order preserved.
        val occurrencesByQualifiedName: Map<QualifiedName, List<IndexedDeclaration>> = this.occurrences
            .groupBy { it.declaration.qualifiedName() }
            .mapValues { (_, values) -> immutableList(values) }

        val packagesByName = this.declarations
            .filterIsInstance<PackageDeclaration>()
            .groupBy { it.name }
            .mapValues { (_, values) -> immutableList(values.sortedWith(declarationComparator)) }

        val typeOccurrencesByOwnerAndName = this.occurrences
            .filter { it.declaration is ProjectTypeDeclaration }
            .groupBy { typeKey(it.declaration as ProjectTypeDeclaration) }
            .mapValues { (_, values) -> immutableList(values.sortedWith(compareBy(declarationComparator) { it.declaration })) }

        val subprogramOccurrencesByOwnerAndName = this.occurrences
            .filter { it.declaration is PackageSubprogramDeclaration }
            .groupBy { (it.declaration as PackageSubprogramDeclaration).let { fact -> fact.owner to fact.name } }
            .mapValues { (_, values) -> immutableList(values.sortedWith(compareBy(declarationComparator) { it.declaration })) }

        /** (effective schema, name without the written owner): the unit's written owner, else its file's schema. */
        fun schemaKey(occurrence: IndexedDeclaration): Pair<OracleIdentifier, QualifiedName>? {
            val declaration = occurrence.declaration
            val schema = schemaOf(declaration) ?: return null
            val name = declaration.qualifiedName()
            return schema to if (unitName(declaration)?.segments?.size == 2) QualifiedName(name.segments.drop(1)) else name
        }

        fun schemaOf(declaration: ProjectDeclaration): OracleIdentifier? {
            val unit = unitName(declaration) ?: return null
            return when (unit.segments.size) {
                2 -> unit.segments.first()
                1 -> schemasByFile[declaration.fileId]
                else -> null
            }
        }

        // Derived lookup evidence, not a second declaration registry. Body-only members count too.
        val packageOwners: Set<QualifiedName> = Collections.unmodifiableSet(
            this.declarations.mapNotNullTo(HashSet()) { declaration ->
                when (declaration) {
                    is PackageSubprogramDeclaration -> declaration.owner
                    is PackageValueDeclaration -> declaration.owner
                    is PackageTypeDeclaration -> declaration.owner
                    is PackageSubtypeDeclaration -> declaration.owner
                    is PackageExceptionDeclaration -> declaration.owner
                    is PackageCursorDeclaration -> declaration.owner
                    else -> null
                }
            }
        )

        val sequencesByName = this.declarations
            .filterIsInstance<SequenceDeclaration>()
            .groupBy { it.name }
            .mapValues { (_, values) -> immutableList(values.sortedWith(declarationComparator)) }

        val schemasByFile: Map<FileId, OracleIdentifier> = schemasByFile.toMap()

        /** Each occurrence by (effective schema, name without its written owner); only occurrences with a schema. */
        val occurrencesBySchema: Map<Pair<OracleIdentifier, QualifiedName>, List<IndexedDeclaration>> = this.occurrences
            .mapNotNull { occurrence -> schemaKey(occurrence)?.let { it to occurrence } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> immutableList(values) }

        /** Package owners of members, by (effective schema, owner without its written owner). */
        val ownersBySchema: Set<Pair<OracleIdentifier, QualifiedName>> = occurrencesBySchema.keys
            .filter { it.second.segments.size == 2 }.mapTo(HashSet()) { it.first to QualifiedName(it.second.segments.first()) }

        /** The schemas some occurrence is in, and those the configuration names. */
        val knownSchemas: Set<OracleIdentifier> = occurrencesBySchema.keys.mapTo(HashSet()) { it.first }.apply { addAll(configuredSchemas) }

        /** The names, without owner, of every unit (package, type, standalone subprogram, sequence) and package owner. */
        val unitNames: Set<OracleIdentifier> = HashSet<OracleIdentifier>().apply {
            occurrences.forEach { unitName(it.declaration)?.let { name -> add(name.last) } }
            packageOwners.forEach { add(it.last) }
        }
    }

    val fileIds: List<FileId> get() = tables.fileIds
    internal val omittedStandaloneCallables: Set<QualifiedName> get() = tables.omittedStandaloneCallables
    internal val alteredTypes: AlteredTypes get() = tables.alteredTypes

    /** All canonical occurrences in deterministic file, then per-file order. */
    val occurrences: List<IndexedDeclaration> get() = tables.occurrences
    val declarations: List<ProjectDeclaration> get() = tables.declarations

    /** Returns the frozen declarations belonging to exactly one source file. */
    internal fun declarationsFor(fileId: FileId): List<ProjectDeclaration> = tables.declarationsFor(fileId)

    /** Returns the frozen occurrences belonging to exactly one source file. */
    fun occurrencesFor(fileId: FileId): List<IndexedDeclaration> = tables.occurrencesFor(fileId)

    private val occurrencesByQualifiedName get() = tables.occurrencesByQualifiedName
    private val packagesByName get() = tables.packagesByName
    private val typeOccurrencesByOwnerAndName get() = tables.typeOccurrencesByOwnerAndName
    private val subprogramOccurrencesByOwnerAndName get() = tables.subprogramOccurrencesByOwnerAndName
    private val packageOwners get() = tables.packageOwners
    private val sequencesByName get() = tables.sequencesByName
    private val schemasByFile get() = tables.schemasByFile
    private val occurrencesBySchema get() = tables.occurrencesBySchema
    private val ownersBySchema get() = tables.ownersBySchema
    private val knownSchemas get() = tables.knownSchemas
    private val unitNames get() = tables.unitNames
    private fun schemaKey(occurrence: IndexedDeclaration) = tables.schemaKey(occurrence)

    /** Whether [other] reads the same maps as this index: true for a schema view and its root. */
    internal fun sharesTablesWith(other: ProjectSymbolIndex): Boolean = tables === other.tables

    /** Whether [schema] is the effective schema of some declaration, or one the configuration names. */
    internal fun isKnownSchema(schema: OracleIdentifier): Boolean = schema in knownSchemas

    /** The occurrences whose effective schema is [schema] and whose name, without its written owner, is [name]. */
    internal fun findInSchema(schema: OracleIdentifier, name: QualifiedName): List<IndexedDeclaration> =
        occurrencesBySchema[schema to name].orEmpty()

    /**
     * The schema and the rest of [name] when it is read through a schema: `x.y` (or `x.y.z`) whose first name is a known
     * schema that no project unit or package owner is named (a unit of that name, through a synonym Oracle cannot be
     * ruled out to read, would capture `x`). Null otherwise. Every exact lookup falls back to this when it finds nothing.
     */
    internal fun throughSchema(name: QualifiedName): Pair<OracleIdentifier, QualifiedName>? {
        if (name.segments.size < 2) return null
        val schema = name.segments.first()
        if (schema !in knownSchemas || schema in unitNames) return null
        return schema to QualifiedName(name.segments.drop(1))
    }

    /** The effective schema of [occurrence]: its unit's written owner, else its file's schema; null for none. */
    internal fun effectiveSchema(occurrence: IndexedDeclaration): OracleIdentifier? = schemaKey(occurrence)?.first

    /** The views of [preferring], by schema; read and written under its lock. */
    private val scoped = HashMap<OracleIdentifier, ProjectSymbolIndex>()

    /**
     * This index as seen from a unit of [schema]: among the matches of a lookup, those in [schema] are the only
     * candidates when there are some (Oracle reads an unqualified name in the unit's own schema first); otherwise every
     * match stays one, as a synonym may lead there. The same index for a null [schema]. Built once per schema.
     */
    internal fun preferring(schema: OracleIdentifier?): ProjectSymbolIndex {
        if (schema == preferredSchema) return this
        val root = root
        if (schema == null) return root
        // A view holds its schema and the root; every map is the root's.
        return synchronized(root.scoped) { root.scoped.getOrPut(schema) { ProjectSymbolIndex(tables, schema, root) } }
    }

    private val root: ProjectSymbolIndex get() = rootIndex ?: this

    /**
     * The occurrences named [name] (without owner) in the preferred schema that [accept] keeps, when there are some:
     * an unqualified name reads them first, whether written with that owner or compiled there by its file. Null
     * otherwise, or when no schema is preferred.
     */
    private fun inPreferredSchema(name: QualifiedName, accept: (ProjectDeclaration) -> Boolean): List<IndexedDeclaration>? {
        val schema = preferredSchema ?: return null
        return findInSchema(schema, name).filter { accept(it.declaration) }.ifEmpty { null }
    }

    /**
     * The occurrences that pair with the unit [unitName] written in [fileId] as its body or specification: the same
     * effective schema (its written owner, else the file's) and the same name without owner. With no schema, the same
     * written name, as without a schema model.
     */
    internal fun counterparts(unitName: QualifiedName, fileId: FileId): List<IndexedDeclaration> {
        val schema = if (unitName.segments.size == 2) unitName.segments.first() else schemasByFile[fileId]
        return if (schema == null) occurrencesByQualifiedName[unitName].orEmpty() else findInSchema(schema, QualifiedName(unitName.last))
    }

    /** The effective schema of the unit [unitName] written in [fileId]: its written owner, else the file's schema. */
    internal fun unitSchema(unitName: QualifiedName, fileId: FileId): OracleIdentifier? =
        if (unitName.segments.size == 2) unitName.segments.first() else schemasByFile[fileId]

    private fun schemaFallback(name: QualifiedName): List<IndexedDeclaration> =
        throughSchema(name)?.let { (schema, rest) -> findInSchema(schema, rest) }.orEmpty()

    fun findPackages(name: QualifiedName): List<PackageDeclaration> =
        inPreferredSchema(name) { it is PackageDeclaration }?.map { it.declaration as PackageDeclaration }
            ?: packagesByName[name] ?: schemaFallback(name).map { it.declaration }.filterIsInstance<PackageDeclaration>()

    fun findTypes(owner: QualifiedName?, name: OracleIdentifier): List<ProjectTypeDeclaration> =
        findTypeOccurrences(owner, name).map { it.declaration as ProjectTypeDeclaration }

    /** Canonical occurrences behind [findTypes], in the same order. */
    internal fun findTypeOccurrences(owner: QualifiedName?, name: OracleIdentifier): List<IndexedDeclaration> =
        inPreferredSchema(owner?.append(name) ?: QualifiedName(name)) { it is ProjectTypeDeclaration }
            ?: typeOccurrencesByOwnerAndName[owner to name]
            ?: owner?.let { schemaFallback(it.append(name)).filter { occurrence -> occurrence.declaration is ProjectTypeDeclaration } }.orEmpty()

    fun findSubprograms(owner: QualifiedName, name: OracleIdentifier): List<PackageSubprogramDeclaration> =
        findSubprogramOccurrences(owner, name).map { it.declaration as PackageSubprogramDeclaration }

    /** Canonical occurrences behind [findSubprograms], in the same order. */
    internal fun findSubprogramOccurrences(owner: QualifiedName, name: OracleIdentifier): List<IndexedDeclaration> =
        inPreferredSchema(owner.append(name)) { it is PackageSubprogramDeclaration }
            ?: subprogramOccurrencesByOwnerAndName[owner to name]
            ?: schemaFallback(owner.append(name)).filter { it.declaration is PackageSubprogramDeclaration }

    fun findSequences(name: QualifiedName): List<SequenceDeclaration> =
        inPreferredSchema(name) { it is SequenceDeclaration }?.map { it.declaration as SequenceDeclaration }
            ?: sequencesByName[name] ?: schemaFallback(name).map { it.declaration }.filterIsInstance<SequenceDeclaration>()

    fun findDeclarations(name: QualifiedName): List<ProjectDeclaration> =
        findOccurrences(name).map { it.declaration }

    /** Exact-name matches as canonical occurrences, in index order, without resolution. */
    fun findOccurrences(name: QualifiedName): List<IndexedDeclaration> =
        inPreferredSchema(name) { true } ?: occurrencesByQualifiedName[name] ?: schemaFallback(name)

    /** Positive evidence for the package interpretation, including non-package objects of that name. */
    internal fun hasPackageEvidence(name: QualifiedName): Boolean =
        name in packageOwners || occurrencesByQualifiedName.containsKey(name) || name in omittedStandaloneCallables ||
            throughSchema(name)?.let { it in ownersBySchema || occurrencesBySchema.containsKey(it) } == true

    /** A standalone procedure/function heading of exactly this name was identified but not retained as a fact. */
    internal fun hasOmittedStandaloneCallable(name: QualifiedName): Boolean = name in omittedStandaloneCallables

    companion object {
        internal val declarationComparator = compareBy<ProjectDeclaration>(
            { it.fileId.value },
            { it.sourceRange.startLine },
            { it.sourceRange.startColumn },
            { it.kind.ordinal },
            { declarationOrderingKey(it) }
        )

        internal fun empty() = ProjectSymbolIndex(emptyList(), emptyMap())

        /** The name of the unit a declaration is, or belongs to (the package of a member); null for none. */
        private fun unitName(declaration: ProjectDeclaration): QualifiedName? = when (declaration) {
            is PackageDeclaration -> declaration.name
            is StandaloneTypeDeclaration -> declaration.name
            is SequenceDeclaration -> declaration.name
            is StandaloneProcedureDeclaration -> declaration.name
            is StandaloneFunctionDeclaration -> declaration.name
            is PackageTypeDeclaration -> declaration.owner
            is PackageSubtypeDeclaration -> declaration.owner
            is PackageSubprogramDeclaration -> declaration.owner
            is PackageValueDeclaration -> declaration.owner
            is PackageExceptionDeclaration -> declaration.owner
            is PackageCursorDeclaration -> declaration.owner
        }

        private fun typeKey(declaration: ProjectTypeDeclaration): Pair<QualifiedName?, OracleIdentifier> =
            declaration.qualifiedName.segments.dropLast(1).takeIf { it.isNotEmpty() }?.let(::QualifiedName) to
                declaration.qualifiedName.last

        /** Semantic identity. It deliberately excludes comparison-only metadata. */
        private fun declarationIdentityKey(declaration: ProjectDeclaration): String = when (declaration) {
            is PackageDeclaration -> declaration.name.lookupKey()
            is StandaloneTypeDeclaration -> declaration.name.lookupKey()
            is PackageTypeDeclaration -> "${declaration.owner.lookupKey()}.${declaration.name.lookupName}"
            is PackageSubtypeDeclaration -> "${declaration.owner.lookupKey()}.${declaration.name.lookupName}:${declaration.baseType.structuralKey()}"
            is PackageSubprogramDeclaration -> declaration.overloadIdentity().toString()
            is SequenceDeclaration -> declaration.name.lookupKey()
            is StandaloneProcedureDeclaration -> declaration.name.lookupKey()
            is StandaloneFunctionDeclaration -> declaration.name.lookupKey()
            is PackageValueDeclaration -> "${declaration.owner.lookupKey()}.${declaration.name.lookupName}"
            is PackageExceptionDeclaration -> "${declaration.owner.lookupKey()}.${declaration.name.lookupName}"
            is PackageCursorDeclaration -> "${declaration.owner.lookupKey()}.${declaration.name.lookupName}"
        }

        /** Stable tie-breaker only; source spelling and rule metadata do not define identity. */
        private fun declarationOrderingKey(declaration: ProjectDeclaration): String =
            declarationIdentityKey(declaration) + ":" + declaration.role + ":" + when (declaration) {
                is PackageDeclaration -> ""
                is StandaloneTypeDeclaration -> declaration.shape.toString() +
                    (declaration.elementType?.let { ":element:" + it.structuralKey() } ?: "")
                is PackageTypeDeclaration -> declaration.shape.toString() + declaration.recordFields.joinToString("|") {
                    "${it.ordinal}:${it.name.originalSpelling}:${it.typeRef.structuralKey()}"
                }  +
                    (declaration.elementType?.let { ":element:" + it.structuralKey() } ?: "")
                is PackageSubtypeDeclaration -> declaration.baseType.structuralKey()
                is PackageProcedureDeclaration -> declaration.parameters.orderingKey()
                is PackageFunctionDeclaration -> declaration.parameters.orderingKey() + ":" + declaration.returnType.structuralKey()
                is SequenceDeclaration -> ""
                is PackageValueDeclaration -> "${declaration.constant}:${declaration.typeRef.structuralKey()}"
                is PackageExceptionDeclaration -> ""
                is PackageCursorDeclaration ->
                    declaration.parameters.orderingKey() + ":" + (declaration.returnType?.structuralKey() ?: "")
                is StandaloneProcedureDeclaration -> declaration.parameters.orderingKey()
                is StandaloneFunctionDeclaration ->
                    declaration.parameters.orderingKey() + ":" + declaration.returnType.structuralKey() + ":" + declaration.deterministic
            }

        private fun List<ProjectParameter>.orderingKey(): String = joinToString("|") {
            "${it.ordinal}:${it.name.originalSpelling}:${it.mode}:${it.nocopy}:${it.defaultPresent}:${it.typeRef.structuralKey()}"
        }
    }
}
