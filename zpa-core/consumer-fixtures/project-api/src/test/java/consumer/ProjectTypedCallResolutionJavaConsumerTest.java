package consumer;

import com.felipebz.zpa.api.project.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Datatype narrowing of call resolutions through the packaged zpa-core jar only. */
class ProjectTypedCallResolutionJavaConsumerTest {
    private static final ProjectIdentifier HR = ProjectIdentifier.fromSource("HR");

    // Deliberately exhaustive and without a default branch.
    private static String label(ProjectTypedCallResolution.Status status) {
        return switch (status) {
            case RESOLVED -> "resolved";
            case AMBIGUOUS -> "ambiguous";
            case NO_COMPATIBLE_CANDIDATE -> "no compatible candidate";
            case NOT_NARROWED -> "not narrowed";
        };
    }

    private static String label(ProjectBindingCompatibility.Outcome outcome) {
        return switch (outcome) {
            case COMPATIBLE -> "compatible";
            case INCOMPATIBLE -> "incompatible";
            case UNKNOWN -> "unknown";
        };
    }

    private static String label(ProjectBindingCompatibility.UnknownReason reason) {
        return switch (reason) {
            case ACTUAL_UNKNOWN -> "actual unknown";
            case NULL_LITERAL -> "null literal";
            case FORMAL_INCONCLUSIVE -> "formal inconclusive";
            case ENVIRONMENT_DEPENDENT -> "environment dependent";
            case UNSUPPORTED_PAIR -> "unsupported pair";
            case UNCLASSIFIED_TYPE -> "unclassified type";
        };
    }

    private static String label(ProjectDatatypeFamily family) {
        return switch (family) {
            case NUMERIC, CHARACTER, CLOB, BLOB, RAW, DATETIME, INTERVAL, BOOLEAN, JSON, ROWID -> "scalar";
            case COMPOSITE -> "composite";
        };
    }

    private static String label(ProjectArgumentType.Kind kind) {
        return switch (kind) {
            case UNKNOWN, NULL_LITERAL, BUILT_IN, COMPOSITE, DESCRIBED -> kind.name();
        };
    }

    private static String label(ProjectCandidateCompatibility.Outcome outcome) {
        return switch (outcome) {
            case RETAINED -> "retained";
            case REJECTED -> "rejected";
        };
    }

    private static ProjectCallResolution pick(ProjectSnapshot snapshot) {
        return snapshot.resolveCall(ProjectCallTarget.packageMember(ProjectQualifiedName.of("p"), ProjectIdentifier.fromSource("pick")),
            ProjectCallKind.PROCEDURE, ProjectCallArguments.of(List.of(ProjectCallArgument.positional())));
    }

    @Test
    void characterEvidenceKeepsTheEqualArityOverloadsAmbiguous() {
        ProjectSnapshot snapshot = ProjectSnapshots.prepare(List.of(ProjectSourceInput.ofText("p.sql",
            "CREATE PACKAGE p AS PROCEDURE pick(n NUMBER); PROCEDURE pick(s VARCHAR2); END p;")), false);
        ProjectCallResolution structural = pick(snapshot);
        ProjectTypedCallResolution typed = snapshot.narrowCall(structural, List.of(ProjectArgumentType.builtIn("CHAR")),
            ProjectExternalMetadata.none(), HR);
        assertThat(label(typed.getStatus())).isEqualTo("ambiguous");
        assertThat(typed.getSelected()).isEmpty();
        assertThat(typed.getStructural()).isSameAs(structural);
        assertThat(typed.getEvaluations()).extracting(e -> label(e.getOutcome())).containsExactly("retained", "retained");
        assertThat(typed.getEvaluations()).extracting(e -> label(e.getBindings().get(0).getOutcome())).containsExactly("compatible", "compatible");
        assertThat(label(ProjectArgumentType.builtIn("CHAR").getKind())).isEqualTo("BUILT_IN");
        assertThat(label(ProjectArgumentType.builtIn("CHAR").getFamily().orElseThrow())).isEqualTo("scalar");
        ProjectTypedCallResolution nul = snapshot.narrowCall(structural, List.of(ProjectArgumentType.nullLiteral()), ProjectExternalMetadata.none(), HR);
        assertThat(label(nul.getEvaluations().get(0).getBindings().get(0).getUnknownReason().orElseThrow())).isEqualTo("null literal");
    }

    @Test
    void dateEvidenceSelectsTheOnlyCompatibleStructuralCandidate() {
        ProjectSnapshot snapshot = ProjectSnapshots.prepare(List.of(ProjectSourceInput.ofText("p.sql",
            "CREATE PACKAGE p AS PROCEDURE pick(x NUMBER); PROCEDURE pick(x DATE); END p;")), false);
        ProjectCallResolution structural = pick(snapshot);
        List<ProjectArgumentType> types = new ArrayList<>(List.of(ProjectArgumentType.builtIn("DATE")));
        ProjectTypedCallResolution typed = snapshot.narrowCall(structural, types, ProjectExternalMetadata.none(), HR);
        types.clear();
        assertThat(label(typed.getStatus())).isEqualTo("resolved");
        assertThat(typed.getSelected().orElseThrow()).isSameAs(structural.getCandidates().get(1));
        ProjectBindingCompatibility rejected = typed.getEvaluations().get(0).getBindings().get(0);
        assertThat(label(rejected.getOutcome())).isEqualTo("incompatible");
        assertThat(rejected.getBinding()).isSameAs(structural.getCandidates().get(0).getBindings().get(0));
        assertThat(rejected.getActualFamily()).contains(ProjectDatatypeFamily.DATETIME);
        assertThat(rejected.getFormalFamily()).contains(ProjectDatatypeFamily.NUMERIC);
        assertThatThrownBy(() -> typed.getEvaluations().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.narrowCall(structural, List.of(), ProjectExternalMetadata.none(), HR))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void describedPackageValuesCarryTheirPackageScope() {
        ProjectSnapshot snapshot = ProjectSnapshots.prepare(List.of(
            ProjectSourceInput.ofText("p.sql", "CREATE PACKAGE p AS SUBTYPE number IS DATE; g number; END p;"),
            ProjectSourceInput.ofText("u.sql", "CREATE PACKAGE u AS n NUMBER; END u;"),
            ProjectSourceInput.ofText("q.sql", "CREATE PACKAGE q AS PROCEDURE pick(x NUMBER); PROCEDURE pick(x DATE); END q;")), false);
        ProjectCallResolution structural = snapshot.resolveCall(
            ProjectCallTarget.packageMember(ProjectQualifiedName.of("q"), ProjectIdentifier.fromSource("pick")),
            ProjectCallKind.PROCEDURE, ProjectCallArguments.of(List.of(ProjectCallArgument.positional())));
        ProjectPackageValueView g = (ProjectPackageValueView) snapshot.findDeclarations(ProjectQualifiedName.of("p", "g")).getDeclarations().get(0);
        ProjectPackageValueView n = (ProjectPackageValueView) snapshot.findDeclarations(ProjectQualifiedName.of("u", "n")).getDeclarations().get(0);
        ProjectExternalMetadata none = ProjectExternalMetadata.none();
        ProjectArgumentType shadowed = ProjectArgumentType.described(snapshot.describeExternalType(g.getTypeRef(), g.getOwner(), none, HR), g.getOwner());
        ProjectArgumentType plain = ProjectArgumentType.described(snapshot.describeExternalType(n.getTypeRef(), n.getOwner(), none, HR), n.getOwner());
        assertThat(label(snapshot.narrowCall(structural, List.of(shadowed), none, HR).getStatus())).isEqualTo("ambiguous");
        assertThat(label(snapshot.narrowCall(structural, List.of(plain), none, HR).getStatus())).isEqualTo("resolved");
        assertThat(ProjectArgumentType.described(snapshot.describeExternalType(n.getTypeRef(), n.getOwner(), none, HR)).getFamily()).isEmpty();
    }
}
