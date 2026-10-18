package consumer;

import com.felipebz.zpa.api.project.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Source/external type composition of written and retained anchors through the packaged zpa-core jar only. */
class ProjectExternalTypeDescriptionJavaConsumerTest {
    private static final ProjectIdentifier HR = ProjectIdentifier.fromSource("HR");

    private static ProjectExternalMetadata fixture() {
        return ProjectExternalMetadata.builder("fixture").completeSchema("HR")
            .relation(ProjectExternalRelation.of("HR", "EMPLOYEES", true, List.of(
                ProjectExternalColumn.of("SALARY", 2, ProjectExternalDatatype.of("NUMBER", null, null, null, 10, 2)),
                ProjectExternalColumn.of("ID", 1, ProjectExternalDatatype.of("NUMBER", null, null, null, 8, 0)),
                ProjectExternalColumn.of("NAME", 3, ProjectExternalDatatype.of("VARCHAR2", null, 30, ProjectExternalDatatype.LengthSemantics.BYTE, null, null)))))
            .build();
    }

    // Deliberately exhaustive and without a default branch.
    private static String label(ProjectExternalTypeDescription.Status status) {
        return switch (status) {
            case RESOLVED -> "resolved";
            case NOT_EXTERNAL -> "not external";
            case CAPTURE_NOT_EXCLUDED -> "capture not excluded";
            case SOURCE_INCOMPLETE -> "source incomplete";
            case CONFLICT -> "conflict";
            case UNAVAILABLE -> "unavailable";
            case SCHEMA_CONTEXT_MISSING -> "schema context missing";
            case AMBIGUOUS -> "ambiguous";
            case INCOMPLETE -> "incomplete";
            case RELATION_NOT_FOUND -> "relation not found";
            case COLUMN_NOT_FOUND -> "column not found";
            case REMOTE -> "remote";
        };
    }

    @Test
    void writtenAnchorsOfAnAnonymousBlockResolveExternally() {
        ProjectSnapshot snapshot = ProjectSnapshots.prepare(List.of(), false);
        ProjectExternalTypeDescription sal = snapshot.describeExternalType(
            ProjectWrittenAnchor.of(ProjectQualifiedName.of("employees", "salary"), ProjectTypeReference.Anchor.TYPE), null, fixture(), HR);
        assertThat(label(sal.getStatus())).isEqualTo("resolved");
        assertThat(sal.getSource().getStatus()).isEqualTo(ProjectTypeDescription.Status.UNRESOLVED_VALUE);
        assertThat(sal.getSource().getWrittenAnchor()).isPresent();
        assertThat(sal.getRelationName()).contains(ProjectQualifiedName.of("HR", "EMPLOYEES"));
        ProjectExternalColumn column = sal.getLookup().orElseThrow().getColumn().orElseThrow();
        assertThat(column.getPosition()).isEqualTo(OptionalInt.of(2));
        assertThat(column.getDatatype().orElseThrow().getCategory()).contains(ProjectBuiltInTypeCategory.NUMERIC);
        assertThat(sal.getLookup().orElseThrow().getOrigin()).contains("fixture");

        ProjectExternalTypeDescription row = snapshot.describeExternalType(
            ProjectWrittenAnchor.of(ProjectQualifiedName.of("employees"), ProjectTypeReference.Anchor.ROWTYPE), null, fixture(), HR);
        assertThat(label(row.getStatus())).isEqualTo("resolved");
        assertThat(row.getLookup().orElseThrow().getRelation().orElseThrow().getRowFields())
            .extracting(ProjectExternalColumn::getName)
            .containsExactly(ProjectIdentifier.fromSource("id"), ProjectIdentifier.fromSource("salary"), ProjectIdentifier.fromSource("name"));
        assertThat(label(snapshot.describeExternalType(
            ProjectWrittenAnchor.of(ProjectQualifiedName.of("employees"), ProjectTypeReference.Anchor.ROWTYPE), null,
            ProjectExternalMetadata.none(), HR).getStatus())).isEqualTo("unavailable");
        assertThatThrownBy(() -> row.getSourceCandidates().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void retainedCollectionElementKeepsSourceAndExternalProvenance() {
        ProjectSnapshot snapshot = ProjectSnapshots.prepare(List.of(ProjectSourceInput.ofText("p_spec.sql",
            "CREATE PACKAGE p AS TYPE rows_t IS TABLE OF employees%ROWTYPE; END p;")), false);
        ProjectShapedTypeView view = (ProjectShapedTypeView) snapshot.describeType(ProjectQualifiedName.of("p", "rows_t"), null)
            .getDeclaration().orElseThrow();
        ProjectTypeReference element = view.getElementType().orElseThrow();
        ProjectExternalTypeDescription e = snapshot.describeExternalType(element, ProjectQualifiedName.of("p"), fixture(), HR);
        assertThat(label(e.getStatus())).isEqualTo("resolved");
        assertThat(e.getSource().getTerminalReference().orElseThrow().getSourceRange().getFileId()).isEqualTo("p_spec.sql");
        assertThat(e.getLookup().orElseThrow().getRelation().orElseThrow().getRowFields()).hasSize(3);
        assertThat(e.getExternalCandidates()).isEmpty();
        assertThat(label(snapshot.describeExternalType(element, ProjectQualifiedName.of("p"), ProjectExternalMetadata.none(), HR).getStatus()))
            .isEqualTo("unavailable");
        assertThat(snapshot.describeType(ProjectQualifiedName.of("p", "rows_t"), null).getDeclaration().orElseThrow().getId()).isEqualTo(view.getId());
    }
}
