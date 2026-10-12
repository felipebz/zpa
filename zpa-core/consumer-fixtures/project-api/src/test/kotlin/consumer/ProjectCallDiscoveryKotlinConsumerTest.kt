package consumer

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.io.IOException

/** Call candidate discovery through the supported API only, compiled against the packaged zpa-core jar. */
@OptIn(ZpaExperimentalApi::class)
class ProjectCallDiscoveryKotlinConsumerTest {
    private fun n(vararg segments: String) = ProjectQualifiedName.of(*segments)
    private fun id(spelling: String) = ProjectIdentifier.fromSource(spelling)

    // Deliberately exhaustive and without else branches.
    private fun label(status: ProjectCallCandidates.Status): String = when (status) {
        ProjectCallCandidates.Status.FOUND -> "found"
        ProjectCallCandidates.Status.NOT_FOUND_IN_PROJECT -> "not found"
        ProjectCallCandidates.Status.UNKNOWN_PACKAGE -> "unknown package"
        ProjectCallCandidates.Status.AMBIGUOUS_TARGET -> "ambiguous target"
        ProjectCallCandidates.Status.PARTIAL_DECLARATIONS -> "partial"
        ProjectCallCandidates.Status.INCOMPLETE_INDEX -> "incomplete"
        ProjectCallCandidates.Status.NOT_PREPARED -> "not prepared"
    }

    private fun label(uncertainty: ProjectCallUncertainty): String = when (uncertainty) {
        ProjectCallUncertainty.NAMESPACE_COMPETITION -> "namespace"
        ProjectCallUncertainty.DUPLICATE_SPECIFICATION -> "duplicate"
        ProjectCallUncertainty.PARTIAL_DECLARATIONS -> "partial"
    }

    private fun label(form: ProjectCallTarget.Form): String = when (form) {
        ProjectCallTarget.Form.PACKAGE_MEMBER -> "member"
        ProjectCallTarget.Form.STANDALONE -> "standalone"
        ProjectCallTarget.Form.WRITTEN -> "written"
    }

    @Test
    fun discoversTypedCandidatesForEveryTargetForm() {
        val snapshot = ProjectSnapshots.prepare(listOf(
            ProjectSourceInput.ofText("spec.sql", "CREATE PACKAGE p AS PROCEDURE run(a NUMBER); FUNCTION run RETURN NUMBER; END p;"),
            ProjectSourceInput.ofText("app.sql", "CREATE PROCEDURE app.run IS BEGIN NULL; END;")
        ), false)
        val member = ProjectCallTarget.packageMember(n("p"), id("run"))
        assertThat(label(member.form)).isEqualTo("member")
        assertThat(member.packageName.get()).isEqualTo(n("p"))
        assertThat(ProjectCallTarget.standalone(n("app", "run")).packageName.isPresent).isFalse()

        val found = snapshot.findCallCandidates(member)
        assertThat(label(found.status)).isEqualTo("found")
        assertThat(found.target).isEqualTo(member)
        val procedure: ProjectSubprogramView = found.candidates[0]
        assertThat(procedure.localName).isEqualTo(id("run"))
        assertThat(procedure.parameters.map { it.name.originalSpelling }).containsExactly("a")
        assertThat((found.candidates[1] as ProjectFunctionView).returnType.name).isEqualTo(n("NUMBER"))
        assertThat(found.candidates.map { it.id }).doesNotHaveDuplicates()
            .allMatch { candidateId -> snapshot.declarations.any { it.id == candidateId } }

        val app = snapshot.findCallCandidates(ProjectCallTarget.standalone(n("app", "run")))
        assertThat(app.candidates.single()).isNotInstanceOf(ProjectPackageSubprogramView::class.java)
        assertThat(label(snapshot.findCallCandidates(ProjectCallTarget.written(n("p", "run"))).status)).isEqualTo("found")

        @Suppress("UNCHECKED_CAST")
        assertThatThrownBy { (found.candidates as MutableList<ProjectSubprogramView>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { (found.uncertainties as MutableSet<ProjectCallUncertainty>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun keepsEveryUncertaintyUnderIncompleteIndex() {
        val snapshot = ProjectSnapshots.prepare(listOf(
            ProjectSourceInput.ofText("a.sql", "CREATE PACKAGE p AS PROCEDURE run; END p;"),
            ProjectSourceInput.ofText("b.sql", "CREATE PACKAGE p AS PROCEDURE other; END p;"),
            ProjectSourceInput.ofReader("broken.sql", ProjectSourceReader { throw IOException("read failed") })
        ), false)
        val result = snapshot.findCallCandidates(ProjectCallTarget.packageMember(n("p"), id("run")))
        assertThat(label(result.status)).isEqualTo("incomplete")
        assertThat(result.uncertainties.map(::label)).containsExactly("duplicate")
        assertThat(result.candidates).hasSize(1)
        assertThat(result.packageSpecifications.map { it.fileId }).containsExactly("a.sql", "b.sql")
        assertThat(result.failures.single().fileId).isEqualTo("broken.sql")
    }
}
