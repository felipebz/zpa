package consumer;

import com.felipebz.zpa.api.project.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Call candidate discovery through the supported API only, compiled against the packaged zpa-core jar. */
class ProjectCallDiscoveryJavaConsumerTest {
    private static ProjectQualifiedName name(String... segments) {
        return ProjectQualifiedName.of(segments);
    }

    // Deliberately exhaustive and without default branches: these stop compiling when an enum grows.
    private static String label(ProjectCallCandidates.Status status) {
        return switch (status) {
            case FOUND -> "found";
            case NOT_FOUND_IN_PROJECT -> "not in the supplied sources";
            case UNKNOWN_PACKAGE -> "package specification not supplied";
            case AMBIGUOUS_TARGET -> "target not settled";
            case PARTIAL_DECLARATIONS -> "callable headers may be missing";
            case INCOMPLETE_INDEX -> "some sources failed";
            case NOT_PREPARED -> "no project";
        };
    }

    private static String label(ProjectCallUncertainty uncertainty) {
        return switch (uncertainty) {
            case NAMESPACE_COMPETITION -> "namespace";
            case DUPLICATE_SPECIFICATION -> "duplicate specification";
            case PARTIAL_DECLARATIONS -> "partial declarations";
        };
    }

    private static String label(ProjectCallTarget.Form form) {
        return switch (form) {
            case PACKAGE_MEMBER -> "member";
            case STANDALONE -> "standalone";
            case WRITTEN -> "written";
        };
    }

    @Test
    void factoriesExposeFormNameAndOptionalPackage() {
        ProjectCallTarget member = ProjectCallTarget.packageMember(name("app", "p"), ProjectIdentifier.fromSource("run"));
        ProjectCallTarget standalone = ProjectCallTarget.standalone(name("run"));
        ProjectCallTarget written = ProjectCallTarget.written(name("p", "run"));
        assertThat(label(member.getForm())).isEqualTo("member");
        assertThat(label(standalone.getForm())).isEqualTo("standalone");
        assertThat(label(written.getForm())).isEqualTo("written");
        assertThat(member.getName()).isEqualTo(name("app", "p", "run"));
        assertThat(member.getPackageName()).contains(name("app", "p"));
        assertThat(standalone.getPackageName()).isEmpty();
        assertThat(written.getPackageName()).isEmpty();
        assertThat(ProjectCallTarget.packageMember(name("\"P\""), ProjectIdentifier.fromSource("\"RUN\"")))
            .isEqualTo(ProjectCallTarget.packageMember(name("p"), ProjectIdentifier.fromSource("run")));
    }

    @Test
    void discoversSpecificationOverloadsAsTypedViews() {
        ProjectSnapshot snapshot = ProjectSnapshots.prepare(List.of(
            ProjectSourceInput.ofText("spec.sql",
                "CREATE PACKAGE p AS PROCEDURE run(a NUMBER, b NUMBER DEFAULT 1); FUNCTION run RETURN NUMBER; END p;"),
            ProjectSourceInput.ofText("body.sql",
                "CREATE PACKAGE BODY p AS PROCEDURE run(a NUMBER, b NUMBER) IS BEGIN NULL; END; END p;")
        ), false);
        ProjectCallCandidates found = snapshot.findCallCandidates(
            ProjectCallTarget.packageMember(name("p"), ProjectIdentifier.fromSource("run")));
        assertThat(label(found.getStatus())).isEqualTo("found");
        assertThat(found.getUncertainties()).isEmpty();
        assertThat(found.getFailures()).isEmpty();
        List<ProjectSubprogramView> candidates = found.getCandidates();
        assertThat(candidates).hasSize(2).allMatch(c -> c.getRole() == ProjectDeclarationRole.SPECIFICATION);
        ProjectSubprogramView procedure = candidates.get(0);
        assertThat(procedure).isInstanceOf(ProjectPackageSubprogramView.class).isNotInstanceOf(ProjectFunctionView.class);
        assertThat(candidates.get(1)).isInstanceOf(ProjectFunctionView.class);
        assertThat(procedure.getParameters()).extracting(ProjectParameterView::isDefaultPresent).containsExactly(false, true);
        assertThat(procedure.getFileId()).isEqualTo("spec.sql");
        assertThat(procedure.getSourceRange().getFileId()).isEqualTo("spec.sql");
        assertThat(procedure.getId()).isEqualTo(snapshot.findPackageSubprograms(name("p"), ProjectIdentifier.fromSource("run"))
            .getDeclarations().stream().filter(d -> d.getRole() == ProjectDeclarationRole.SPECIFICATION)
            .findFirst().orElseThrow().getId());
        assertThat(found.getPackageSpecifications()).singleElement()
            .extracting(ProjectDeclarationView::getKind).isEqualTo(ProjectDeclarationKind.PACKAGE);

        var foundCandidates = found.getCandidates();
        assertThatThrownBy(() -> foundCandidates.clear()).isInstanceOf(UnsupportedOperationException.class);
        var foundPackageSpecifications = found.getPackageSpecifications();
        assertThatThrownBy(() -> foundPackageSpecifications.clear()).isInstanceOf(UnsupportedOperationException.class);
        var foundFailures = found.getFailures();
        assertThatThrownBy(() -> foundFailures.clear()).isInstanceOf(UnsupportedOperationException.class);
        var foundUncertainties = found.getUncertainties();
        assertThatThrownBy(() -> foundUncertainties.add(ProjectCallUncertainty.NAMESPACE_COMPETITION))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void reportsUncertaintyAndFailuresWithoutDroppingCandidates() {
        ProjectSnapshot snapshot = ProjectSnapshots.prepare(List.of(
            ProjectSourceInput.ofText("pkg.sql", "CREATE PACKAGE x AS PROCEDURE other; END x;"),
            ProjectSourceInput.ofText("y.sql", "CREATE PROCEDURE x.y IS BEGIN NULL; END;")
        ), false);
        ProjectCallCandidates competed = snapshot.findCallCandidates(ProjectCallTarget.written(name("x", "y")));
        assertThat(label(competed.getStatus())).isEqualTo("target not settled");
        assertThat(competed.getCandidates()).hasSize(1);
        assertThat(competed.getUncertainties()).containsExactly(ProjectCallUncertainty.NAMESPACE_COMPETITION);
        assertThat(label(competed.getUncertainties().iterator().next())).isEqualTo("namespace");
        assertThat(snapshot.findCallCandidates(ProjectCallTarget.standalone(name("x", "y"))).getStatus())
            .isEqualTo(ProjectCallCandidates.Status.FOUND);

        ProjectSnapshot failed = ProjectSnapshots.prepare(List.of(
            ProjectSourceInput.ofText("spec.sql", "CREATE PACKAGE p AS PROCEDURE run; END p;"),
            ProjectSourceInput.ofReader("broken.sql", () -> { throw new IOException("unreadable"); })
        ), false);
        ProjectCallCandidates incomplete = failed.findCallCandidates(
            ProjectCallTarget.packageMember(name("p"), ProjectIdentifier.fromSource("run")));
        assertThat(incomplete.getStatus()).isEqualTo(ProjectCallCandidates.Status.INCOMPLETE_INDEX);
        assertThat(incomplete.getCandidates()).hasSize(1);
        assertThat(incomplete.getFailures()).singleElement()
            .extracting(ProjectPreparationFailure::getFileId).isEqualTo("broken.sql");

        ProjectSnapshot empty = ProjectSnapshots.prepare(List.of(), false);
        assertThat(empty.getPreparationState()).isEqualTo(ProjectPreparationState.PREPARED_EMPTY);
        assertThat(empty.findCallCandidates(ProjectCallTarget.written(name("f"))).getStatus())
            .isEqualTo(ProjectCallCandidates.Status.NOT_FOUND_IN_PROJECT);
    }
}
