//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.consumer

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectPreparationState
import com.felipebz.zpa.api.project.ProjectSnapshots
import com.felipebz.zpa.api.project.ProjectSourceInput
import com.felipebz.zpa.api.project.ProjectSourceReader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ZpaExperimentalApi::class)
class ProjectPreparationKotlinApiTest {
    @Test
    fun preparingNoSourcesProducesAPreparedEmptySnapshot() {
        val snapshot = ProjectSnapshots.prepare(emptyList<ProjectSourceInput>())

        assertThat(snapshot.preparationState).isEqualTo(ProjectPreparationState.PREPARED_EMPTY)
        assertThat(snapshot.attemptedFileCount).isZero()
        assertThat(snapshot.successfulFileCount).isZero()
        assertThat(snapshot.failures).isEmpty()
        assertThat(snapshot.fileIds).isEmpty()
    }

    @Test
    fun anonymousBlocksAreSuccessfulWithoutDurableProjectDeclarations() {
        val reads = AtomicInteger()
        val textBlock = ProjectSourceInput.ofText(
            "z_text_block.sql",
            "BEGIN NULL; END;"
        )
        val readerBlock = ProjectSourceInput.ofReader("a_reader_block.sql", ProjectSourceReader {
            reads.incrementAndGet()
            "DECLARE v NUMBER := 1; BEGIN v := v + 1; END;"
        })

        val snapshot = ProjectSnapshots.prepare(listOf(textBlock, readerBlock), false)

        assertThat(snapshot.preparationState).isEqualTo(ProjectPreparationState.PREPARED_EMPTY)
        assertThat(snapshot.attemptedFileCount).isEqualTo(2)
        assertThat(snapshot.successfulFileCount).isEqualTo(2)
        assertThat(snapshot.failures).isEmpty()
        assertThat(snapshot.fileIds).containsExactly("a_reader_block.sql", "z_text_block.sql")
        assertThat(reads.get()).isEqualTo(1)
    }

    @Test
    fun readerFailuresTakePrecedenceWithoutAnyKnownDeclarations() {
        val snapshot = ProjectSnapshots.prepare(
            listOf(
                ProjectSourceInput.ofText("empty.sql", "-- no declarations"),
                ProjectSourceInput.ofReader("broken.sql", ProjectSourceReader {
                    throw IOException("diagnostic contents are not metadata")
                })
            ),
            false
        )

        assertThat(snapshot.preparationState).isEqualTo(ProjectPreparationState.PREPARED_WITH_FAILURES)
        assertThat(snapshot.attemptedFileCount).isEqualTo(2)
        assertThat(snapshot.successfulFileCount).isEqualTo(1)
        assertThat(snapshot.fileIds).containsExactly("broken.sql", "empty.sql")
        assertThat(snapshot.failures).hasSize(1)
        val failure = snapshot.failures.single()
        assertThat(failure.fileId).isEqualTo("broken.sql")
        assertThat(failure.exceptionType).isEqualTo(IOException::class.java.name)
    }
}
