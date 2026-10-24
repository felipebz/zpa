package consumer;

import com.felipebz.zpa.api.project.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Structural call resolution through the supported API only, compiled against the packaged zpa-core jar. */
class ProjectCallResolutionJavaConsumerTest {
    private static final String SPEC = "CREATE PACKAGE p AS "
        + "PROCEDURE run(a IN NUMBER, b IN NUMBER DEFAULT 1, c OUT NOCOPY VARCHAR2); "
        + "PROCEDURE lab(a NUMBER); PROCEDURE lab(b NUMBER); "
        + "FUNCTION f(x NUMBER DEFAULT 1) RETURN NUMBER; END p;";

    private static ProjectSnapshot snapshot() {
        return ProjectSnapshots.prepare(List.of(
            ProjectSourceInput.ofText("spec.sql", SPEC),
            ProjectSourceInput.ofText("body.sql",
                "CREATE PACKAGE BODY p AS PROCEDURE run(a IN NUMBER, b IN NUMBER, c OUT NOCOPY VARCHAR2) "
                    + "IS BEGIN NULL; END; END p;")), false);
    }

    private static ProjectCallTarget member(String name) {
        return ProjectCallTarget.packageMember(ProjectQualifiedName.of("p"), ProjectIdentifier.fromSource(name));
    }

    private static ProjectCallArgument named(String label) {
        return ProjectCallArgument.named(ProjectIdentifier.fromSource(label));
    }

    // Deliberately exhaustive and without default branches: these stop compiling when an enum grows.
    private static String label(ProjectCallResolution.Status status) {
        return switch (status) {
            case RESOLVED -> "resolved";
            case AMBIGUOUS -> "ambiguous";
            case NO_APPLICABLE_CANDIDATE -> "no applicable candidate";
            case WRONG_KIND -> "wrong kind";
            case NOT_FOUND_IN_PROJECT -> "not found";
            case UNKNOWN_PACKAGE -> "unknown package";
            case AMBIGUOUS_TARGET -> "ambiguous target";
            case PARTIAL_DECLARATIONS -> "partial";
            case INCOMPLETE_INDEX -> "incomplete";
            case INVALID_NOTATION -> "invalid notation";
            case NOT_PREPARED -> "not prepared";
        };
    }

    private static String label(ProjectCallCandidate.Applicability applicability) {
        return switch (applicability) {
            case APPLICABLE -> "applicable";
            case WRONG_KIND -> "wrong kind";
            case TOO_MANY_ARGUMENTS -> "too many";
            case UNKNOWN_LABEL -> "unknown label";
            case DUPLICATE_BINDING -> "duplicate binding";
            case MISSING_REQUIRED -> "missing required";
            case INVALID_SIGNATURE -> "invalid signature";
        };
    }

    private static String label(ProjectCallArguments.Notation notation) {
        return switch (notation) {
            case VALID -> "valid";
            case POSITIONAL_AFTER_NAMED -> "positional after named";
            case DUPLICATE_LABEL -> "duplicate label";
        };
    }

    private static String label(ProjectCallKind kind) {
        return switch (kind) {
            case PROCEDURE -> "procedure";
            case FUNCTION -> "function";
        };
    }

    @Test
    void requestFactoriesAndNotation() {
        ProjectCallArgument positional = ProjectCallArgument.positional();
        ProjectCallArgument a = named("a");
        assertThat(positional.isNamed()).isFalse();
        assertThat(positional.getLabel()).isEmpty();
        assertThat(a.getLabel()).contains(ProjectIdentifier.fromSource("A"));

        List<ProjectCallArgument> written = new ArrayList<>(List.of(positional, a));
        ProjectCallArguments arguments = ProjectCallArguments.of(written);
        written.clear();
        assertThat(arguments.getArguments()).containsExactly(positional, a);
        assertThat(label(arguments.getNotation())).isEqualTo("valid");
        assertThat(arguments.getInvalidArgumentOrdinal()).isEmpty();
        assertThat(ProjectCallArguments.none().getArguments()).isEmpty();

        ProjectCallArguments afterNamed = ProjectCallArguments.of(List.of(a, positional));
        assertThat(label(afterNamed.getNotation())).isEqualTo("positional after named");
        OptionalInt offending = afterNamed.getInvalidArgumentOrdinal();
        assertThat(offending).hasValue(2);
        assertThat(ProjectCallArguments.of(List.of(a, named("\"A\""))).getNotation())
            .isEqualTo(ProjectCallArguments.Notation.DUPLICATE_LABEL);
        assertThat(ProjectCallArguments.of(List.of(a, named("\"a\""))).getNotation())
            .isEqualTo(ProjectCallArguments.Notation.VALID);
        var argumentsList = arguments.getArguments();
        assertThatThrownBy(argumentsList::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void resolvesWithBindingsOmissionsAndTheSelectedCandidateObject() {
        ProjectSnapshot snapshot = snapshot();
        ProjectCallArguments arguments = ProjectCallArguments.of(List.of(ProjectCallArgument.positional(), named("c")));
        ProjectCallResolution resolution = snapshot.resolveCall(member("run"), ProjectCallKind.PROCEDURE, arguments);
        assertThat(label(resolution.getStatus())).isEqualTo("resolved");
        assertThat(resolution.getTarget()).isEqualTo(member("run"));
        assertThat(label(resolution.getKind())).isEqualTo("procedure");
        assertThat(resolution.getArguments()).isSameAs(arguments);
        assertThat(resolution.getUncertainties()).isEmpty();
        assertThat(resolution.getFailures()).isEmpty();
        assertThat(resolution.getPackageSpecifications()).singleElement()
            .extracting(ProjectDeclarationView::getKind).isEqualTo(ProjectDeclarationKind.PACKAGE);

        Optional<ProjectCallCandidate> selected = resolution.getSelected();
        ProjectCallCandidate candidate = selected.orElseThrow();
        assertThat(resolution.getCandidates()).hasSize(1);
        assertThat(resolution.getCandidates().get(0)).isSameAs(candidate);
        assertThat(label(candidate.getApplicability())).isEqualTo("applicable");
        assertThat(candidate.getRejectedArgumentOrdinal()).isEmpty();
        assertThat(candidate.getMissingParameters()).isEmpty();

        ProjectSubprogramView declaration = candidate.getDeclaration();
        assertThat(declaration.getRole()).isEqualTo(ProjectDeclarationRole.SPECIFICATION);
        assertThat(declaration.getFileId()).isEqualTo("spec.sql");
        assertThat(declaration.getId()).isEqualTo(snapshot.findPackageSubprograms(ProjectQualifiedName.of("p"),
                ProjectIdentifier.fromSource("run")).getDeclarations().stream()
            .filter(d -> d.getRole() == ProjectDeclarationRole.SPECIFICATION).findFirst().orElseThrow().getId());

        List<ProjectArgumentBinding> bindings = candidate.getBindings();
        assertThat(bindings).extracting(ProjectArgumentBinding::getArgumentOrdinal).containsExactly(1, 2);
        assertThat(bindings.get(0).getArgument()).isSameAs(arguments.getArguments().get(0));
        assertThat(bindings.get(1).getArgument()).isSameAs(arguments.getArguments().get(1));
        ProjectParameterView c = bindings.get(1).getParameter();
        assertThat(c).isSameAs(declaration.getParameters().get(2))
            .extracting(ProjectParameterView::getOrdinal, ProjectParameterView::getMode, ProjectParameterView::isNocopy,
                p -> p.getTypeRef().getName(), p -> p.getSourceRange().getFileId())
            .containsExactly(3, ProjectParameterMode.OUT, true, ProjectQualifiedName.of("VARCHAR2"), "spec.sql");
        assertThat(candidate.getOmittedParameters()).singleElement().isSameAs(declaration.getParameters().get(1));
        assertThat(candidate.getOmittedParameters().get(0).isDefaultPresent()).isTrue();
    }

    @Test
    void resolutionListsAreImmutable() {
        ProjectSnapshot snapshot = snapshot();
        ProjectCallArguments arguments = ProjectCallArguments.of(List.of(ProjectCallArgument.positional(), named("c")));
        ProjectCallResolution resolution = snapshot.resolveCall(member("run"), ProjectCallKind.PROCEDURE, arguments);
        ProjectCallCandidate candidate = resolution.getSelected().orElseThrow();
        var resolutionCandidates = resolution.getCandidates();
        assertThatThrownBy(resolutionCandidates::clear).isInstanceOf(UnsupportedOperationException.class);
        var resolutionUncertainties = resolution.getUncertainties();
        assertThatThrownBy(resolutionUncertainties::clear).isInstanceOf(UnsupportedOperationException.class);
        var resolutionPackageSpecifications = resolution.getPackageSpecifications();
        assertThatThrownBy(resolutionPackageSpecifications::clear).isInstanceOf(UnsupportedOperationException.class);
        var resolutionFailures = resolution.getFailures();
        assertThatThrownBy(resolutionFailures::clear).isInstanceOf(UnsupportedOperationException.class);
        var candidateBindings = candidate.getBindings();
        assertThatThrownBy(candidateBindings::clear).isInstanceOf(UnsupportedOperationException.class);
        var candidateOmittedParameters = candidate.getOmittedParameters();
        assertThatThrownBy(candidateOmittedParameters::clear).isInstanceOf(UnsupportedOperationException.class);
        var candidateMissingParameters = candidate.getMissingParameters();
        assertThatThrownBy(candidateMissingParameters::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void reportsRejectionsAmbiguityKindAndUncertainty() {
        ProjectSnapshot snapshot = snapshot();
        ProjectCallResolution missing = snapshot.resolveCall(member("run"), ProjectCallKind.PROCEDURE, ProjectCallArguments.none());
        assertThat(label(missing.getStatus())).isEqualTo("no applicable candidate");
        ProjectCallCandidate rejected = missing.getCandidates().get(0);
        assertThat(label(rejected.getApplicability())).isEqualTo("missing required");
        assertThat(rejected.getMissingParameters()).extracting(ProjectParameterView::getOrdinal).containsExactly(1, 3);
        assertThat(missing.getSelected()).isEmpty();

        ProjectCallResolution unknown = snapshot.resolveCall(member("run"), ProjectCallKind.PROCEDURE,
            ProjectCallArguments.of(List.of(ProjectCallArgument.positional(), named("zz"))));
        assertThat(label(unknown.getCandidates().get(0).getApplicability())).isEqualTo("unknown label");
        assertThat(unknown.getCandidates().get(0).getRejectedArgumentOrdinal()).hasValue(2);

        ProjectCallResolution ambiguous = snapshot.resolveCall(member("lab"), ProjectCallKind.PROCEDURE,
            ProjectCallArguments.of(List.of(ProjectCallArgument.positional())));
        assertThat(label(ambiguous.getStatus())).isEqualTo("ambiguous");
        assertThat(ambiguous.getCandidates()).hasSize(2);
        assertThat(ambiguous.getSelected()).isEmpty();

        assertThat(label(snapshot.resolveCall(member("f"), ProjectCallKind.PROCEDURE, ProjectCallArguments.none()).getStatus()))
            .isEqualTo("wrong kind");
        ProjectCallResolution function = snapshot.resolveCall(member("f"), ProjectCallKind.FUNCTION, ProjectCallArguments.none());
        assertThat(function.getSelected().orElseThrow().getDeclaration()).isInstanceOf(ProjectFunctionView.class);

        ProjectCallResolution invalid = snapshot.resolveCall(member("run"), ProjectCallKind.PROCEDURE,
            ProjectCallArguments.of(List.of(named("a"), ProjectCallArgument.positional())));
        assertThat(label(invalid.getStatus())).isEqualTo("invalid notation");
        assertThat(invalid.getCandidates()).isEmpty();

        ProjectSnapshot failed = ProjectSnapshots.prepare(List.of(ProjectSourceInput.ofText("spec.sql", SPEC),
            ProjectSourceInput.ofReader("broken.sql", () -> { throw new IOException("unreadable"); })), false);
        ProjectCallResolution incomplete = failed.resolveCall(member("f"), ProjectCallKind.FUNCTION, ProjectCallArguments.none());
        assertThat(label(incomplete.getStatus())).isEqualTo("incomplete");
        assertThat(label(incomplete.getCandidates().get(0).getApplicability())).isEqualTo("applicable");
        assertThat(incomplete.getSelected()).isEmpty();
        assertThat(incomplete.getFailures()).singleElement()
            .extracting(ProjectPreparationFailure::getFileId).isEqualTo("broken.sql");
    }
}
