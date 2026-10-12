package consumer

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/** Structural call resolution through the supported API only, compiled against the packaged zpa-core jar. */
@OptIn(ZpaExperimentalApi::class)
class ProjectCallResolutionKotlinConsumerTest {
    private val snapshot = ProjectSnapshots.prepare(listOf(
        ProjectSourceInput.ofText("spec.sql", "CREATE PACKAGE p AS PROCEDURE copy(src IN NUMBER, dst OUT NOCOPY NUMBER); " +
            "PROCEDURE boot; PROCEDURE boot(x NUMBER DEFAULT 1); FUNCTION boot(y NUMBER) RETURN NUMBER; END p;")
    ), false)

    private fun member(name: String) = ProjectCallTarget.packageMember(ProjectQualifiedName.of("p"), ProjectIdentifier.fromSource(name))
    private fun named(label: String) = ProjectCallArgument.named(ProjectIdentifier.fromSource(label))

    // Deliberately exhaustive and without else branches.
    private fun label(status: ProjectCallResolution.Status): String = when (status) {
        ProjectCallResolution.Status.RESOLVED -> "resolved"
        ProjectCallResolution.Status.AMBIGUOUS -> "ambiguous"
        ProjectCallResolution.Status.NO_APPLICABLE_CANDIDATE -> "none applicable"
        ProjectCallResolution.Status.WRONG_KIND -> "wrong kind"
        ProjectCallResolution.Status.NOT_FOUND_IN_PROJECT -> "not found"
        ProjectCallResolution.Status.UNKNOWN_PACKAGE -> "unknown package"
        ProjectCallResolution.Status.AMBIGUOUS_TARGET -> "ambiguous target"
        ProjectCallResolution.Status.PARTIAL_DECLARATIONS -> "partial"
        ProjectCallResolution.Status.INCOMPLETE_INDEX -> "incomplete"
        ProjectCallResolution.Status.INVALID_NOTATION -> "invalid notation"
        ProjectCallResolution.Status.NOT_PREPARED -> "not prepared"
    }

    private fun label(applicability: ProjectCallCandidate.Applicability): String = when (applicability) {
        ProjectCallCandidate.Applicability.APPLICABLE -> "applicable"
        ProjectCallCandidate.Applicability.WRONG_KIND -> "wrong kind"
        ProjectCallCandidate.Applicability.TOO_MANY_ARGUMENTS -> "too many"
        ProjectCallCandidate.Applicability.UNKNOWN_LABEL -> "unknown label"
        ProjectCallCandidate.Applicability.DUPLICATE_BINDING -> "duplicate binding"
        ProjectCallCandidate.Applicability.MISSING_REQUIRED -> "missing"
        ProjectCallCandidate.Applicability.INVALID_SIGNATURE -> "invalid signature"
    }

    @Test
    fun reorderedNamedBindingUsesTheDeclarationsOwnParameters() {
        val arguments = ProjectCallArguments.of(listOf(named("dst"), named("src")))
        val resolution = snapshot.resolveCall(member("copy"), ProjectCallKind.PROCEDURE, arguments)
        assertThat(label(resolution.status)).isEqualTo("resolved")
        val selected = resolution.selected.get()
        assertThat(resolution.candidates.single()).isSameAs(selected)
        val declaration: ProjectSubprogramView = selected.declaration
        assertThat(selected.bindings.map { it.argumentOrdinal to it.parameter.ordinal }).containsExactly(1 to 2, 2 to 1)
        assertThat(selected.bindings.map { it.argument }).containsExactlyElementsOf(arguments.arguments)
        assertThat(selected.bindings[0].parameter).isSameAs(declaration.parameters[1])
        assertThat(selected.bindings[0].parameter.mode).isEqualTo(ProjectParameterMode.OUT)
        assertThat(selected.bindings[0].parameter.isNocopy).isTrue()
        assertThat(selected.rejectedArgumentOrdinal.isPresent).isFalse()
        assertThatThrownBy { (selected.bindings as MutableList<ProjectArgumentBinding>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun zeroActualOverloadsAreAmbiguousAndKindsAreSeparated() {
        val procedures = snapshot.resolveCall(member("boot"), ProjectCallKind.PROCEDURE, ProjectCallArguments.none())
        assertThat(label(procedures.status)).isEqualTo("ambiguous")
        assertThat(procedures.candidates.map { label(it.applicability) }).containsExactly("applicable", "applicable", "wrong kind")
        assertThat(procedures.selected.isPresent).isFalse()
        assertThat(procedures.candidates[1].omittedParameters.single().name).isEqualTo(ProjectIdentifier.fromSource("x"))

        val function = snapshot.resolveCall(member("boot"), ProjectCallKind.FUNCTION, ProjectCallArguments.none())
        assertThat(label(function.status)).isEqualTo("none applicable")
        assertThat(function.candidates[2].missingParameters.single().ordinal).isEqualTo(1)

        val duplicate = ProjectCallArguments.of(listOf(named("x"), named("X")))
        assertThat(duplicate.notation).isEqualTo(ProjectCallArguments.Notation.DUPLICATE_LABEL)
        assertThat(duplicate.invalidArgumentOrdinal.asInt).isEqualTo(2)
        assertThat(label(snapshot.resolveCall(member("boot"), ProjectCallKind.PROCEDURE, duplicate).status)).isEqualTo("invalid notation")
    }
}
