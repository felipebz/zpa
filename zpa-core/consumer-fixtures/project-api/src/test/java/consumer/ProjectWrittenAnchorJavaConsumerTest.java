package consumer;

import com.felipebz.zpa.api.project.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Written anchors through the packaged zpa-core jar only. */
class ProjectWrittenAnchorJavaConsumerTest {
    private static String sql(String name) {
        try (InputStream in = ProjectWrittenAnchorJavaConsumerTest.class.getResourceAsStream("/sql/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ProjectSnapshot project(ProjectSourceInput... extra) {
        var inputs = new java.util.ArrayList<>(List.of(
            ProjectSourceInput.ofText("db/anchors_p.sql", sql("anchors_p.sql")),
            ProjectSourceInput.ofText("db/anchors_q.sql", sql("anchors_q.sql"))));
        inputs.addAll(List.of(extra));
        return ProjectSnapshots.prepare(inputs, false);
    }

    private static ProjectWrittenAnchor type(String... segments) {
        return ProjectWrittenAnchor.of(ProjectQualifiedName.of(segments), ProjectTypeReference.Anchor.TYPE);
    }

    // Deliberately exhaustive and without a default branch: the written query adds no status.
    private static String label(ProjectTypeDescription.Status status) {
        return switch (status) {
            case DECLARED_TYPE -> "declared";
            case BUILT_IN -> "built-in";
            case UNRESOLVED_NAME -> "unresolved";
            case ANCHORED_REFERENCE -> "anchored";
            case REF_REFERENCE -> "ref";
            case CYCLE -> "cycle";
            case UNRESOLVED_VALUE -> "unresolved value";
        };
    }

    @Test
    void factoriesGettersAndIdentity() {
        ProjectWrittenAnchor anchor = type("p", "g");
        assertThat(anchor.getName()).isEqualTo(ProjectQualifiedName.of("p", "g"));
        assertThat(anchor.getAnchor()).isEqualTo(ProjectTypeReference.Anchor.TYPE);
        assertThat(anchor).isEqualTo(type("P", "\"G\"")).hasSameHashCodeAs(type("P", "\"G\""));
        assertThat(anchor).isNotEqualTo(type("p", "\"g\""));
        assertThat(anchor).isNotEqualTo(ProjectWrittenAnchor.of(ProjectQualifiedName.of("p", "g"), ProjectTypeReference.Anchor.ROWTYPE));
        assertThatThrownBy(() -> ProjectWrittenAnchor.of(null, ProjectTypeReference.Anchor.TYPE)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void writtenAnchorsAreDescribedAndEchoed() {
        ProjectSnapshot snapshot = project();
        ProjectWrittenAnchor value = type("p", "g");
        ProjectTypeDescription builtIn = snapshot.describeType(value, null);
        assertThat(label(builtIn.getStatus())).isEqualTo("built-in");
        assertThat(builtIn.getBuiltInCategory()).contains(ProjectBuiltInTypeCategory.NUMERIC);
        assertThat(builtIn.getWrittenAnchor()).containsSame(value);
        assertThat(builtIn.getValues()).extracting(ProjectDeclarationView::getQualifiedName).containsExactly(ProjectQualifiedName.of("p", "g"));
        assertThat(builtIn.getTerminalReference()).isPresent();

        ProjectTypeDescription record = snapshot.describeType(type("p", "rec_value"), null);
        assertThat(label(record.getStatus())).isEqualTo("declared");
        assertThat(record.getDeclaration().orElseThrow().getQualifiedName()).isEqualTo(ProjectQualifiedName.of("p", "rec_t"));

        ProjectWrittenAnchor column = type("employees", "salary");
        ProjectTypeDescription external = snapshot.describeType(column, null);
        assertThat(label(external.getStatus())).isEqualTo("unresolved value");
        assertThat(external.getTerminalReference()).isEmpty();
        assertThat(external.getWrittenAnchor()).containsSame(column);
        assertThat(external.getValueResolution().orElseThrow().getStatus()).isEqualTo(ProjectValueResolution.Status.NOT_FOUND_IN_PROJECT);

        ProjectWrittenAnchor rowAnchor = ProjectWrittenAnchor.of(ProjectQualifiedName.of("employees"), ProjectTypeReference.Anchor.ROWTYPE);
        ProjectTypeDescription row = snapshot.describeType(rowAnchor, null);
        assertThat(label(row.getStatus())).isEqualTo("anchored");
        assertThat(row.getAnchorStop()).contains(ProjectTypeDescription.AnchorStop.ROWTYPE);
        assertThat(row.getTerminalReference()).isEmpty();

        ProjectTypeDescription noOwner = snapshot.describeType(type("g"), null);
        assertThat(noOwner.getAnchorStop()).contains(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE);
        assertThat(snapshot.describeType(type("g"), ProjectQualifiedName.of("p")).getStatus()).isEqualTo(ProjectTypeDescription.Status.BUILT_IN);

        ProjectTypeDescription byName = snapshot.describeType(ProjectQualifiedName.of("numeric_t"), ProjectQualifiedName.of("p"));
        assertThat(byName.getWrittenAnchor()).isEmpty();
        assertThatThrownBy(() -> builtIn.getSteps().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void incompletePreparationIsReported() {
        ProjectSnapshot failed = project(ProjectSourceInput.ofReader("db/bad.sql", () -> {
            throw new IOException("cannot read");
        }));
        ProjectTypeDescription d = failed.describeType(type("p", "g"), null);
        assertThat(d.getStatus()).isEqualTo(ProjectTypeDescription.Status.UNRESOLVED_VALUE);
        assertThat(d.getValueResolution().orElseThrow().getStatus()).isEqualTo(ProjectValueResolution.Status.INCOMPLETE_INDEX);
        assertThat(d.getWrittenAnchor()).isPresent();
    }
}
