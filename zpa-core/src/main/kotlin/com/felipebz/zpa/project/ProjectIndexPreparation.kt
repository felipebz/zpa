//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectExternalContext

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.stream.Stream

/**
 * A narrow source adapter shared by frontends. The reader is used only while the per-file
 * declaration facts are being built; neither it nor its source contents enter the index.
 */
class ProjectSource(
    val fileId: FileId,
    private val reader: ProjectSourceReader
) {
    fun contents(): String = reader.read()
}

fun interface ProjectSourceReader {
    fun read(): String
}

/** Builds the AST-free project declaration index before file-local analysis. */
@OptIn(ZpaExperimentalApi::class)
class ProjectIndexPreparation private constructor(
    private val extractor: ProjectDeclarationSourceExtractor,
    /**
     * The schemas: each file's from [ProjectExternalContext.fileSchema], given to the declarations written without an
     * owner. With none configured, no file has one.
     */
    private val schemas: ProjectExternalContext
) {
    constructor(extractor: ProjectDeclarationSourceExtractor = ProjectDeclarationExtractor()) : this(extractor, ProjectExternalContext.none())

    fun prepare(sources: Collection<ProjectSource>, concurrent: Boolean = true): ProjectIndexPreparationResult {
        require(sources.map { it.fileId }.distinct().size == sources.size) {
            "Project sources must have unique file identities"
        }

        val builder = ProjectSymbolIndexBuilder()
        builder.configuredSchemas(
            (schemas.schemaPaths.entries.map { it.schema } + listOfNotNull(schemas.defaultCompilationSchema.orElse(null))).map { it.identifier }
        )
        val failures = ConcurrentLinkedQueue<ProjectIndexPreparationFailure>()
        val stream: Stream<ProjectSource> = if (concurrent) sources.parallelStream() else sources.stream()
        stream.forEach { source ->
            schemas.fileSchema(source.fileId.value).ifPresent { builder.schema(source.fileId, it.identifier) }
            try {
                // A custom extractor supplies facts only, so it contributes no standalone coverage evidence.
                val extraction = (extractor as? ProjectDeclarationExtractor)?.extractFile(source.fileId, source.contents())
                    ?: ProjectFileExtraction(extractor.extract(source.fileId, source.contents()), emptySet())
                builder.add(source.fileId, extraction.declarations, extraction.omittedStandaloneCallables, extraction.alteredTypes)
            } catch (e: Exception) {
                // Pass 2 remains independent, while the result records that this file did
                // not contribute trustworthy project declarations.
                failures.add(ProjectIndexPreparationFailure(source.fileId, e::class.java.name))
                builder.add(source.fileId, emptyList())
            }
        }
        return ProjectIndexPreparationResult(
            index = builder.build(),
            attemptedFileCount = sources.size,
            failures = failures.toList().sortedBy { it.fileId.value }
        )
    }

    companion object {
        /** A preparation whose declarations get the schemas files compile in ([schemas] supplies the paths and default). */
        @ZpaExperimentalApi
        @JvmStatic
        fun withSchemas(extractor: ProjectDeclarationSourceExtractor, schemas: ProjectExternalContext): ProjectIndexPreparation =
            ProjectIndexPreparation(extractor, schemas)
    }
}


/** Failure summary retained without the exception or any parser state. */
data class ProjectIndexPreparationFailure(
    val fileId: FileId,
    val exceptionType: String
)

/** Immutable result of preparing the project declaration index. */
class ProjectIndexPreparationResult(
    val index: ProjectSymbolIndex,
    val attemptedFileCount: Int,
    failures: Collection<ProjectIndexPreparationFailure>
) {
    val failures: List<ProjectIndexPreparationFailure> = immutableList(failures)
    val successfulFileCount: Int = attemptedFileCount - this.failures.size

    init {
        require(attemptedFileCount >= 0) { "Attempted file count cannot be negative" }
        require(this.failures.map { it.fileId }.distinct().size == this.failures.size) {
            "Preparation failures must have unique file identities"
        }
        require(this.failures.size <= attemptedFileCount) {
            "Preparation failures cannot exceed attempted files"
        }
    }
}
