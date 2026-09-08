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
package com.felipebz.zpa.project

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
class ProjectIndexPreparation(
    private val extractor: ProjectDeclarationSourceExtractor = ProjectDeclarationExtractor()
) {
    fun prepare(sources: Collection<ProjectSource>, concurrent: Boolean = true): ProjectIndexPreparationResult {
        require(sources.map { it.fileId }.distinct().size == sources.size) {
            "Project sources must have unique file identities"
        }

        val builder = ProjectSymbolIndexBuilder()
        val failures = ConcurrentLinkedQueue<ProjectIndexPreparationFailure>()
        val stream: Stream<ProjectSource> = if (concurrent) sources.parallelStream() else sources.stream()
        stream.forEach { source ->
            try {
                builder.add(source.fileId, extractor.extract(source.fileId, source.contents()))
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
