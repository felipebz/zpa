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

        assertThat(snapshot.getPreparationState()).isEqualTo(ProjectPreparationState.PREPARED_EMPTY)
        assertThat(snapshot.getAttemptedFileCount()).isZero()
        assertThat(snapshot.getSuccessfulFileCount()).isZero()
        assertThat(snapshot.getFailures()).isEmpty()
        assertThat(snapshot.getFileIds()).isEmpty()
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

        assertThat(snapshot.getPreparationState()).isEqualTo(ProjectPreparationState.PREPARED_EMPTY)
        assertThat(snapshot.getAttemptedFileCount()).isEqualTo(2)
        assertThat(snapshot.getSuccessfulFileCount()).isEqualTo(2)
        assertThat(snapshot.getFailures()).isEmpty()
        assertThat(snapshot.getFileIds()).containsExactly("a_reader_block.sql", "z_text_block.sql")
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

        assertThat(snapshot.getPreparationState()).isEqualTo(ProjectPreparationState.PREPARED_WITH_FAILURES)
        assertThat(snapshot.getAttemptedFileCount()).isEqualTo(2)
        assertThat(snapshot.getSuccessfulFileCount()).isEqualTo(1)
        assertThat(snapshot.getFileIds()).containsExactly("broken.sql", "empty.sql")
        assertThat(snapshot.getFailures()).hasSize(1)
        val failure = snapshot.getFailures().single()
        assertThat(failure.getFileId()).isEqualTo("broken.sql")
        assertThat(failure.getExceptionType()).isEqualTo(IOException::class.java.name)
    }
}
