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

import com.felipebz.flr.api.AstNode;
import com.felipebz.zpa.api.PlSqlFile;
import com.felipebz.zpa.api.PlSqlGrammar;
import com.felipebz.zpa.api.checks.PlSqlCheck;
import com.felipebz.zpa.api.project.ProjectAnalysis;
import com.felipebz.zpa.api.project.ProjectPreparationState;
import com.felipebz.zpa.api.project.ProjectSnapshot;
import com.felipebz.zpa.squid.AstScanner;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectAnalysisSnapshotJavaApiTest {
    static final class Probe extends PlSqlCheck {
        final List<ProjectSnapshot> snapshots = new ArrayList<>();

        Probe() {
            subscribeTo(PlSqlGrammar.PROCEDURE_DECLARATION);
        }

        @Override
        public void visitNode(AstNode node) {
            ProjectAnalysis analysis = projectAnalysis();
            snapshots.add(analysis.getSnapshot());
            snapshots.add(analysis.getSnapshot());
        }
    }

    @Test
    void javaCheckReadsTheStableScanSnapshotWithoutPreparingProjectSources() {
        String source = "CREATE PACKAGE BODY p AS PROCEDURE work IS BEGIN NULL; END work; END p;";
        Probe probe = new Probe();
        PlSqlFile file = new PlSqlFile() {
            public String contents() { return source; }
            public String fileName() { return "b.sql"; }
            public Path path() { return Path.of("b.sql"); }
            public Type type() { return Type.MAIN; }
        };
        new AstScanner(List.of(), null, true, StandardCharsets.UTF_8)
            .scanFile(file, List.of(probe), "b.sql");
        assertThat(probe.snapshots).hasSize(2);
        assertThat(probe.snapshots.get(0)).isSameAs(probe.snapshots.get(1));
        assertThat(probe.snapshots.get(0).getPreparationState()).isEqualTo(ProjectPreparationState.NOT_PREPARED);
        assertThat(probe.snapshots.get(0).getDeclarations()).isEmpty();
    }
}
