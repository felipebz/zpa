package consumer

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Datatype narrowing of call resolutions through the packaged zpa-core jar only. */
@OptIn(ZpaExperimentalApi::class)
class ProjectTypedCallResolutionKotlinConsumerTest {
    private fun label(status: ProjectTypedCallResolution.Status) = when (status) {
        ProjectTypedCallResolution.Status.RESOLVED -> "resolved"
        ProjectTypedCallResolution.Status.AMBIGUOUS -> "ambiguous"
        ProjectTypedCallResolution.Status.NO_COMPATIBLE_CANDIDATE -> "no compatible candidate"
        ProjectTypedCallResolution.Status.NOT_NARROWED -> "not narrowed"
    }

    private fun label(reason: ProjectBindingCompatibility.UnknownReason) = when (reason) {
        ProjectBindingCompatibility.UnknownReason.ACTUAL_UNKNOWN -> "actual unknown"
        ProjectBindingCompatibility.UnknownReason.NULL_LITERAL -> "null literal"
        ProjectBindingCompatibility.UnknownReason.FORMAL_INCONCLUSIVE -> "formal inconclusive"
        ProjectBindingCompatibility.UnknownReason.ENVIRONMENT_DEPENDENT -> "environment dependent"
        ProjectBindingCompatibility.UnknownReason.UNSUPPORTED_PAIR -> "unsupported pair"
        ProjectBindingCompatibility.UnknownReason.UNCLASSIFIED_TYPE -> "unclassified type"
    }

    @Test
    fun narrowsByRecordEvidenceAndKeepsUncertainDiscoveryUnnarrowed() {
        val source = "CREATE PACKAGE p AS TYPE rec_t IS RECORD (a NUMBER); PROCEDURE put(r rec_t); PROCEDURE put(v VARCHAR2); END p;"
        val snapshot = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("p.sql", source)), false)
        val structural = snapshot.resolveCall(
            ProjectCallTarget.packageMember(ProjectQualifiedName.of("p"), ProjectIdentifier.fromSource("put")),
            ProjectCallKind.PROCEDURE, ProjectCallArguments.of(listOf(ProjectCallArgument.positional()))
        )
        val typed = snapshot.narrowCall(structural, listOf(ProjectArgumentType.composite()), ProjectExternalMetadata.none(), null)
        assertThat(label(typed.status)).isEqualTo("resolved")
        assertThat(typed.selected.get()).isSameAs(structural.candidates[0])
        val unknown = snapshot.narrowCall(structural, listOf(ProjectArgumentType.unknown()), ProjectExternalMetadata.none(), null)
        assertThat(label(unknown.status)).isEqualTo("ambiguous")
        assertThat(label(unknown.evaluations[0].bindings[0].unknownReason.get())).isEqualTo("actual unknown")

        val incomplete = ProjectSnapshots.prepare(
            listOf(ProjectSourceInput.ofText("p.sql", source), ProjectSourceInput.ofReader("bad.sql") { throw IllegalStateException("x") }), false
        )
        val incompleteStructural = incomplete.resolveCall(
            ProjectCallTarget.packageMember(ProjectQualifiedName.of("p"), ProjectIdentifier.fromSource("put")),
            ProjectCallKind.PROCEDURE, ProjectCallArguments.of(listOf(ProjectCallArgument.positional()))
        )
        val notNarrowed = incomplete.narrowCall(incompleteStructural, listOf(ProjectArgumentType.composite()), ProjectExternalMetadata.none(), null)
        assertThat(label(notNarrowed.status)).isEqualTo("not narrowed")
        assertThat(notNarrowed.selected).isEmpty
    }

    @Test
    fun describedEvidenceTakesAnOptionalPackageOwner() {
        val snapshot = ProjectSnapshots.prepare(
            listOf(
                ProjectSourceInput.ofText("p.sql", "CREATE PACKAGE p AS SUBTYPE number IS DATE; g number; END p;"),
                ProjectSourceInput.ofText("q.sql", "CREATE PACKAGE q AS PROCEDURE pick(x NUMBER); PROCEDURE pick(x DATE); END q;")
            ), false
        )
        val structural = snapshot.resolveCall(
            ProjectCallTarget.packageMember(ProjectQualifiedName.of("q"), ProjectIdentifier.fromSource("pick")),
            ProjectCallKind.PROCEDURE, ProjectCallArguments.of(listOf(ProjectCallArgument.positional()))
        )
        val g = snapshot.findDeclarations(ProjectQualifiedName.of("p", "g")).declarations.single() as ProjectPackageValueView
        val description = snapshot.describeExternalType(g.typeRef, g.owner, ProjectExternalMetadata.none(), null)
        val typed = snapshot.narrowCall(structural, listOf(ProjectArgumentType.described(description, g.owner)), ProjectExternalMetadata.none(), null)
        assertThat(label(typed.status)).isEqualTo("ambiguous")
        assertThat(label(typed.evaluations[0].bindings[0].unknownReason.get())).isEqualTo("actual unknown")
    }
}
