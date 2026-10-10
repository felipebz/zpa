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

import com.felipebz.zpa.api.project.ProjectDeclarationId;
import com.felipebz.zpa.api.project.ProjectDeclarationKind;
import com.felipebz.zpa.api.project.ProjectDeclarationRole;
import com.felipebz.zpa.api.project.ProjectDeclarationView;
import com.felipebz.zpa.api.project.ProjectInventory;
import com.felipebz.zpa.api.project.ProjectPackageTypeView;
import com.felipebz.zpa.api.project.ProjectRecordFieldView;
import com.felipebz.zpa.api.project.ProjectShapedTypeView;
import com.felipebz.zpa.api.project.ProjectSubtypeView;
import com.felipebz.zpa.api.project.ProjectTypeReference;
import com.felipebz.zpa.api.project.ProjectTypeShape;
import com.felipebz.zpa.api.project.ProjectQualifiedName;
import com.felipebz.zpa.api.project.ProjectSnapshot;
import com.felipebz.zpa.api.project.ProjectSnapshots;
import com.felipebz.zpa.api.project.ProjectSourceInput;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectDeclarationInventoryJavaApiTest {
    private static ProjectSnapshot prepare() {
        return ProjectSnapshots.prepare(List.of(
            ProjectSourceInput.ofText("spec.sql", "CREATE PACKAGE shop AS PROCEDURE ship(a NUMBER); END shop;"),
            ProjectSourceInput.ofText("seq.sql", "CREATE SEQUENCE app.orders;")
        ));
    }

    @Test
    void enumeratesGenericDeclarationsUsingOnlySupportedImports() {
        ProjectSnapshot snapshot = prepare();

        List<ProjectDeclarationView> all = snapshot.getDeclarations();
        assertThat(all).hasSize(3);
        ProjectDeclarationView sequence = all.stream()
            .filter(d -> d.getKind() == ProjectDeclarationKind.SEQUENCE).findFirst().orElseThrow();
        assertThat(sequence.getRole()).isEqualTo(ProjectDeclarationRole.STANDALONE);
        assertThat(sequence.getFileId()).isEqualTo("seq.sql");
        assertThat(sequence.getQualifiedName()).isEqualTo(ProjectQualifiedName.of("APP", "ORDERS"));
        assertThat(sequence.getQualifiedName().getLast().getOriginalSpelling()).isEqualTo("orders");
        assertThat(sequence.getSourceRange().getStartLine()).isEqualTo(1);

        ProjectInventory<ProjectDeclarationView> byName =
            snapshot.findDeclarations(ProjectQualifiedName.of("shop", "ship"));
        assertThat(byName.getStatus()).isEqualTo(ProjectInventory.Status.COMPLETE);
        assertThat(byName.getDeclarations()).extracting(ProjectDeclarationView::getKind)
            .containsExactly(ProjectDeclarationKind.PACKAGE_PROCEDURE);
        assertThat(snapshot.declarationsFor("spec.sql").getDeclarations()).hasSize(2);
    }

    @Test
    void nullQueryArgumentsAreRejected() {
        ProjectSnapshot snapshot = prepare();
        assertThatThrownBy(() -> snapshot.declarationsFor(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> snapshot.findDeclarations(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void idsKeyMixedSnapshotsAndCollectionsAreUnmodifiable() {
        ProjectSnapshot first = prepare();
        ProjectSnapshot second = prepare();
        Map<ProjectDeclarationId, ProjectDeclarationView> map = new HashMap<>();
        first.getDeclarations().forEach(d -> map.put(d.getId(), d));
        second.getDeclarations().forEach(d -> map.put(d.getId(), d));

        assertThat(map).hasSize(6);
        assertThatThrownBy(() -> first.getDeclarations().clear())
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> first.declarationsFor("seq.sql").getDeclarations().clear())
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> first.declarationsFor("seq.sql").getFailures().add(null))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> ProjectQualifiedName.of("a", "b").getSegments().clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void inspectsSpecializedTypeViewsWithInstanceof() {
        ProjectSnapshot snapshot = ProjectSnapshots.prepare(List.of(
            ProjectSourceInput.ofText("t.sql",
                "CREATE PACKAGE t AS TYPE r IS RECORD (a NUMBER, b x.y%TYPE, c REF o); SUBTYPE s IS q.base; END t;"),
            ProjectSourceInput.ofText("o.sql", "CREATE TYPE o_t AS OBJECT (id NUMBER); CREATE TYPE bare;")
        ));
        int packageTypes = 0, subtypes = 0, shaped = 0;
        for (ProjectDeclarationView view : snapshot.getDeclarations()) {
            if (view instanceof ProjectPackageTypeView) {
                ProjectPackageTypeView type = (ProjectPackageTypeView) view;
                packageTypes++;
                assertThat(type.getShape()).contains(ProjectTypeShape.RECORD);
                List<ProjectRecordFieldView> fields = type.getRecordFields();
                assertThat(fields).hasSize(3);
                assertThat(fields.get(1).getTypeRef().getAnchor()).contains(ProjectTypeReference.Anchor.TYPE);
                assertThat(fields.get(2).getTypeRef().getKind()).isEqualTo(ProjectTypeReference.Kind.REF);
                assertThat(fields.get(0).getTypeRef().getAnchor()).isEmpty();
                assertThatThrownBy(() -> fields.remove(0)).isInstanceOf(UnsupportedOperationException.class);
            } else if (view instanceof ProjectSubtypeView) {
                subtypes++;
                assertThat(((ProjectSubtypeView) view).getBaseType().getKind())
                    .isEqualTo(ProjectTypeReference.Kind.NAMED);
            } else if (view instanceof ProjectShapedTypeView) {
                shaped++;
            }
        }
        assertThat(new int[] {packageTypes, subtypes, shaped}).containsExactly(1, 1, 2);
        ProjectShapedTypeView bare = (ProjectShapedTypeView) snapshot
            .findDeclarations(ProjectQualifiedName.of("bare")).getDeclarations().get(0);
        assertThat(bare.getShape()).isEmpty();
    }
}
