package com.felipebz.zpa.project

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProjectIndexPreparationTest {
    @Test
    fun preparationUsesTheSameCorePathForSerialAndConcurrentSources() {
        val sources = listOf(
            ProjectSource(FileId("b.sql"), ProjectSourceReader { "CREATE PACKAGE b AS END b;" }),
            ProjectSource(FileId("a.sql"), ProjectSourceReader { "CREATE PACKAGE a AS END a;" })
        )

        val serial = ProjectIndexPreparation().prepare(sources, concurrent = false)
        val concurrent = ProjectIndexPreparation().prepare(sources, concurrent = true)

        assertThat(serial.index.fileIds).containsExactly(FileId("a.sql"), FileId("b.sql"))
        assertThat(concurrent.index.fileIds).containsExactlyElementsOf(serial.index.fileIds)
        assertThat(concurrent.index.declarations).containsExactlyElementsOf(serial.index.declarations)
        assertThat(serial.failures).isEmpty()
        assertThat(concurrent.failures).isEmpty()
    }

    @Test
    fun concurrentPreparationDoesNotLoseDeclarations() {
        val sources = (1..128).map { number ->
            val fileId = FileId("$number.sql")
            ProjectSource(fileId, ProjectSourceReader {
                "CREATE PACKAGE p$number AS PROCEDURE work(value IN NUMBER); END p$number;"
            })
        }

        val serial = ProjectIndexPreparation().prepare(sources, concurrent = false)
        val concurrent = ProjectIndexPreparation().prepare(sources, concurrent = true)

        assertThat(concurrent.index.declarations).containsExactlyElementsOf(serial.index.declarations)
    }

    @Test
    fun preparationReportsFailuresWithoutRetainingExceptions() {
        val failingFile = FileId("broken.sql")
        val preparation = ProjectIndexPreparation(ProjectDeclarationSourceExtractor { fileId, _ ->
            if (fileId == failingFile) error("diagnostic details must not be retained")
            emptyList()
        })

        val result = preparation.prepare(
            listOf(
                ProjectSource(failingFile, ProjectSourceReader { "not used" }),
                ProjectSource(FileId("empty.sql"), ProjectSourceReader { "-- no declarations" })
            ),
            concurrent = true
        )

        assertThat(result.attemptedFileCount).isEqualTo(2)
        assertThat(result.successfulFileCount).isEqualTo(1)
        assertThat(result.index.declarations).isEmpty()
        assertThat(result.failures).containsExactly(
            ProjectIndexPreparationFailure(failingFile, IllegalStateException::class.java.name)
        )
    }

    @Test
    fun contextDistinguishesPreparationStates() {
        val emptyResult = ProjectIndexPreparation().prepare(emptyList(), concurrent = false)
        val declarationResult = ProjectIndexPreparation().prepare(
            listOf(ProjectSource(FileId("p.sql"), ProjectSourceReader { "CREATE PACKAGE p AS END p;" })),
            concurrent = false
        )
        val failedResult = ProjectIndexPreparation(ProjectDeclarationSourceExtractor { _, _ -> error("failure") })
            .prepare(listOf(ProjectSource(FileId("broken.sql"), ProjectSourceReader { "" })), concurrent = false)

        assertThat(ProjectAnalysisContext.NOT_PREPARED.state.kind)
            .isEqualTo(ProjectAnalysisContext.State.Kind.NOT_PREPARED)
        assertThat(ProjectAnalysisContext.prepared(emptyResult).state.kind)
            .isEqualTo(ProjectAnalysisContext.State.Kind.PREPARED_EMPTY)
        assertThat(ProjectAnalysisContext.prepared(declarationResult).state.kind)
            .isEqualTo(ProjectAnalysisContext.State.Kind.PREPARED_WITH_DECLARATIONS)
        assertThat(ProjectAnalysisContext.prepared(failedResult).state.kind)
            .isEqualTo(ProjectAnalysisContext.State.Kind.PREPARED_WITH_FAILURES)
    }
}
