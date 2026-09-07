package com.felipebz.zpa.project

import java.util.concurrent.ConcurrentHashMap

/** Collects immutable per-file facts and freezes them into a deterministic index. */
class ProjectSymbolIndexBuilder {
    private val declarationsByFile = ConcurrentHashMap<FileId, List<ProjectDeclaration>>()

    fun add(fileId: FileId, declarations: Collection<ProjectDeclaration>) {
        require(declarations.all { it.fileId == fileId }) {
            "All declarations added for a file must have the same FileId"
        }
        val copy = immutableList(declarations)
        require(declarationsByFile.putIfAbsent(fileId, copy) == null) {
            "Declarations for $fileId were added more than once"
        }
    }

    fun build(): ProjectSymbolIndex {
        val declarations = declarationsByFile.entries
            .sortedBy { it.key.value }
            .flatMap { it.value }
            .sortedWith(ProjectSymbolIndex.declarationComparator)
        return ProjectSymbolIndex(declarationsByFile.keys.toList().sortedBy { it.value }, declarations)
    }
}

/** Immutable project-wide declaration registry. It does not perform semantic resolution. */
class ProjectSymbolIndex internal constructor(
    fileIds: List<FileId>,
    declarations: List<ProjectDeclaration>
) {
    val fileIds: List<FileId> = immutableList(fileIds)
    val declarations: List<ProjectDeclaration> = immutableList(declarations)

    private val packagesByName = this.declarations
        .filterIsInstance<PackageDeclaration>()
        .groupBy { it.name }
        .mapValues { (_, values) -> immutableList(values.sortedWith(declarationComparator)) }

    private val typesByOwnerAndName = this.declarations
        .filter { it is PackageTypeDeclaration || it is PackageSubtypeDeclaration || it is StandaloneTypeDeclaration }
        .groupBy { declaration -> typeKey(declaration) }
        .mapValues { (_, values) -> immutableList(values.sortedWith(declarationComparator)) }

    private val subprogramsByOwnerAndName = this.declarations
        .filterIsInstance<PackageSubprogramDeclaration>()
        .groupBy { it.owner to it.name }
        .mapValues { (_, values) -> immutableList(values.sortedWith(declarationComparator)) }

    fun findPackages(name: QualifiedName): List<PackageDeclaration> = packagesByName[name].orEmpty()

    fun findTypes(owner: QualifiedName?, name: OracleIdentifier): List<ProjectDeclaration> =
        typesByOwnerAndName[owner to name].orEmpty()

    fun findSubprograms(owner: QualifiedName, name: OracleIdentifier): List<PackageSubprogramDeclaration> =
        subprogramsByOwnerAndName[owner to name].orEmpty().filterIsInstance<PackageSubprogramDeclaration>()

    fun findDeclarations(name: QualifiedName): List<ProjectDeclaration> = declarations.filter { declaration ->
        when (declaration) {
            is PackageDeclaration -> declaration.name == name
            is StandaloneTypeDeclaration -> declaration.name == name
            is PackageTypeDeclaration -> declaration.owner.append(declaration.name) == name
            is PackageSubtypeDeclaration -> declaration.owner.append(declaration.name) == name
            is PackageSubprogramDeclaration -> declaration.owner.append(declaration.name) == name
        }
    }

    companion object {
        internal val declarationComparator = compareBy<ProjectDeclaration>(
            { it.fileId.value },
            { it.sourceRange.startLine },
            { it.sourceRange.startColumn },
            { it.kind.ordinal },
            { declarationOrderingKey(it) }
        )

        internal fun empty() = ProjectSymbolIndex(emptyList(), emptyList())

        private fun typeKey(declaration: ProjectDeclaration): Pair<QualifiedName?, OracleIdentifier> = when (declaration) {
            is PackageTypeDeclaration -> declaration.owner to declaration.name
            is PackageSubtypeDeclaration -> declaration.owner to declaration.name
            is StandaloneTypeDeclaration -> declaration.name.segments.dropLast(1).takeIf { it.isNotEmpty() }?.let(::QualifiedName) to declaration.name.last
            else -> error("Not a type declaration: $declaration")
        }

        /** Semantic identity. It deliberately excludes comparison-only metadata. */
        private fun declarationIdentityKey(declaration: ProjectDeclaration): String = when (declaration) {
            is PackageDeclaration -> declaration.name.lookupKey()
            is StandaloneTypeDeclaration -> declaration.name.lookupKey()
            is PackageTypeDeclaration -> "${declaration.owner.lookupKey()}.${declaration.name.lookupName}"
            is PackageSubtypeDeclaration -> "${declaration.owner.lookupKey()}.${declaration.name.lookupName}:${declaration.baseType.structuralKey()}"
            is PackageSubprogramDeclaration -> declaration.overloadIdentity().toString()
        }

        /** Stable tie-breaker only; source spelling and rule metadata do not define identity. */
        private fun declarationOrderingKey(declaration: ProjectDeclaration): String =
            declarationIdentityKey(declaration) + ":" + declaration.role + ":" + when (declaration) {
                is PackageDeclaration -> ""
                is StandaloneTypeDeclaration -> declaration.shape.toString()
                is PackageTypeDeclaration -> declaration.shape.toString()
                is PackageSubtypeDeclaration -> declaration.baseType.structuralKey()
                is PackageProcedureDeclaration -> declaration.parameters.orderingKey()
                is PackageFunctionDeclaration -> declaration.parameters.orderingKey() + ":" + declaration.returnType.structuralKey()
            }

        private fun List<ProjectParameter>.orderingKey(): String = joinToString("|") {
            "${it.ordinal}:${it.name.originalSpelling}:${it.mode}:${it.nocopy}:${it.defaultPresent}:${it.typeRef.structuralKey()}"
        }
    }
}
