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
package com.felipebz.zpa.consumer;

import com.felipebz.zpa.api.project.ProjectPreparationFailure;
import com.felipebz.zpa.api.project.ProjectPreparationState;
import com.felipebz.zpa.api.project.ProjectSnapshot;
import com.felipebz.zpa.api.project.ProjectSnapshots;
import com.felipebz.zpa.api.project.ProjectSourceInput;
import com.felipebz.zpa.api.project.ProjectSourceReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectPreparationJavaApiTest {

    @Test
    void preparesTextThroughTheDefaultFactoryAndExposesUnmodifiableLists() {
        ProjectSourceInput source = ProjectSourceInput.ofText("package.sql", "CREATE PACKAGE p AS END p;");

        ProjectSnapshot snapshot = ProjectSnapshots.prepare(List.of(source));

        assertThat(source.getFileId()).isEqualTo("package.sql");
        assertThat(snapshot.getPreparationState()).isEqualTo(ProjectPreparationState.PREPARED_WITH_DECLARATIONS);
        assertThat(snapshot.getAttemptedFileCount()).isEqualTo(1);
        assertThat(snapshot.getSuccessfulFileCount()).isEqualTo(1);
        assertThat(snapshot.getFileIds()).containsExactly("package.sql");
        assertThat(snapshot.getFailures()).isEmpty();
        assertThatThrownBy(() -> snapshot.getFileIds().add("other.sql"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.getFailures().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void serialAndConcurrentPreparationHaveTheSameSortedMetadataAndFailurePrecedence() {
        AtomicInteger declarationReads = new AtomicInteger();
        AtomicInteger firstFailureReads = new AtomicInteger();
        AtomicInteger secondFailureReads = new AtomicInteger();
        List<ProjectSourceInput> sources = List.of(
                ProjectSourceInput.ofReader("z.sql", () -> {
                    secondFailureReads.incrementAndGet();
                    throw new IOException("private diagnostic z");
                }),
                ProjectSourceInput.ofReader("m.sql", () -> {
                    declarationReads.incrementAndGet();
                    return "CREATE PACKAGE p AS END p;";
                }),
                ProjectSourceInput.ofReader("a.sql", () -> {
                    firstFailureReads.incrementAndGet();
                    throw new IOException("private diagnostic a");
                })
        );

        ProjectSnapshot serial = ProjectSnapshots.prepare(sources, false);
        assertThat(declarationReads.get()).isEqualTo(1);
        assertThat(firstFailureReads.get()).isEqualTo(1);
        assertThat(secondFailureReads.get()).isEqualTo(1);
        ProjectSnapshot concurrent = ProjectSnapshots.prepare(sources, true);
        assertThat(declarationReads.get()).isEqualTo(2);
        assertThat(firstFailureReads.get()).isEqualTo(2);
        assertThat(secondFailureReads.get()).isEqualTo(2);

        for (ProjectSnapshot snapshot : List.of(serial, concurrent)) {
            assertThat(snapshot.getPreparationState()).isEqualTo(ProjectPreparationState.PREPARED_WITH_FAILURES);
            assertThat(snapshot.getAttemptedFileCount()).isEqualTo(3);
            assertThat(snapshot.getSuccessfulFileCount()).isEqualTo(1);
            assertThat(snapshot.getFileIds()).containsExactly("a.sql", "m.sql", "z.sql");
            assertThat(snapshot.getFailures()).extracting(ProjectPreparationFailure::getFileId)
                    .containsExactly("a.sql", "z.sql");
            assertThat(snapshot.getFailures()).extracting(ProjectPreparationFailure::getExceptionType)
                    .containsExactly(IOException.class.getName(), IOException.class.getName());
        }
        assertThatThrownBy(() -> serial.getFailures().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> serial.getFileIds().set(0, "replacement.sql"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(declarationReads.get()).isEqualTo(2);
        assertThat(firstFailureReads.get()).isEqualTo(2);
        assertThat(secondFailureReads.get()).isEqualTo(2);
    }

    @Test
    void metadataAccessDoesNotReadAgainButASeparatePreparationDoes() {
        AtomicInteger reads = new AtomicInteger();
        ProjectSourceReader reader = () -> {
            reads.incrementAndGet();
            return "CREATE PACKAGE p AS END p;";
        };
        ProjectSourceInput source = ProjectSourceInput.ofReader("reader.sql", reader);
        assertThat(source.getFileId()).isEqualTo("reader.sql");
        assertThat(reads.get()).isZero();

        ProjectSnapshot first = ProjectSnapshots.prepare(List.of(source), false);
        for (int i = 0; i < 3; i++) {
            assertThat(first.getPreparationState()).isEqualTo(ProjectPreparationState.PREPARED_WITH_DECLARATIONS);
            assertThat(first.getAttemptedFileCount()).isEqualTo(1);
            assertThat(first.getSuccessfulFileCount()).isEqualTo(1);
            assertThat(first.getFailures()).isEmpty();
            assertThat(first.getFileIds()).containsExactly("reader.sql");
        }
        assertThat(reads.get()).isEqualTo(1);

        ProjectSnapshot second = ProjectSnapshots.prepare(List.of(source), false);
        assertThat(second.getPreparationState()).isEqualTo(first.getPreparationState());
        assertThat(second.getFileIds()).containsExactlyElementsOf(first.getFileIds());
        assertThat(reads.get()).isEqualTo(2);
    }

    @Test
    void duplicateIdentitiesAreRejectedBeforeReadingAnySource() {
        AtomicInteger reads = new AtomicInteger();
        ProjectSourceReader reader = () -> {
            reads.incrementAndGet();
            return "CREATE PACKAGE p AS END p;";
        };
        List<ProjectSourceInput> sources = List.of(
                ProjectSourceInput.ofReader("same.sql", reader),
                ProjectSourceInput.ofReader("same.sql", reader)
        );

        for (boolean concurrent : new boolean[] {false, true}) {
            assertThatThrownBy(() -> ProjectSnapshots.prepare(sources, concurrent))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(reads.get()).isZero();
        }
    }

    @Test
    void stabilizesTheInputCollectionBeforeReadersRunAndOwnsItsMetadataAfterward() {
        List<ProjectSourceInput> sources = new ArrayList<>();
        AtomicInteger firstReads = new AtomicInteger();
        AtomicInteger secondReads = new AtomicInteger();
        AtomicInteger replacementReads = new AtomicInteger();
        ProjectSourceInput replacement = ProjectSourceInput.ofReader("replacement.sql", () -> {
            replacementReads.incrementAndGet();
            return "CREATE PACKAGE replacement AS END replacement;";
        });
        sources.add(ProjectSourceInput.ofReader("b.sql", () -> {
            firstReads.incrementAndGet();
            sources.clear();
            sources.add(replacement);
            return "CREATE PACKAGE b AS END b;";
        }));
        sources.add(ProjectSourceInput.ofReader("a.sql", () -> {
            secondReads.incrementAndGet();
            return "CREATE PACKAGE a AS END a;";
        }));

        ProjectSnapshot snapshot = ProjectSnapshots.prepare(sources, false);
        sources.clear();
        sources.add(ProjectSourceInput.ofText("later.sql", "-- no declarations"));

        assertThat(snapshot.getPreparationState()).isEqualTo(ProjectPreparationState.PREPARED_WITH_DECLARATIONS);
        assertThat(snapshot.getAttemptedFileCount()).isEqualTo(2);
        assertThat(snapshot.getSuccessfulFileCount()).isEqualTo(2);
        assertThat(snapshot.getFileIds()).containsExactly("a.sql", "b.sql");
        assertThat(snapshot.getFailures()).isEmpty();
        assertThat(firstReads.get()).isEqualTo(1);
        assertThat(secondReads.get()).isEqualTo(1);
        assertThat(replacementReads.get()).isZero();
    }

    @Test
    void errorsEscapeSerialPreparationRatherThanBecomingFailureSummaries() {
        AssertionError error = new AssertionError("must escape");
        ProjectSourceInput source = ProjectSourceInput.ofReader("error.sql", () -> {
            throw error;
        });

        assertThatThrownBy(() -> ProjectSnapshots.prepare(List.of(source), false)).isSameAs(error);
    }
}
