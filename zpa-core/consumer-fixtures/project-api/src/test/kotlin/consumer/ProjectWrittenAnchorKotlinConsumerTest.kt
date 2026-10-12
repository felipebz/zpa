package consumer

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException

/** Written anchors through the packaged zpa-core jar only. */
@OptIn(ZpaExperimentalApi::class)
class ProjectWrittenAnchorKotlinConsumerTest {
    private fun sql(name: String) = checkNotNull(javaClass.getResourceAsStream("/sql/$name")).use { String(it.readAllBytes()) }

    private fun project(vararg extra: ProjectSourceInput) = ProjectSnapshots.prepare(
        listOf(
            ProjectSourceInput.ofText("db/anchors_p.sql", sql("anchors_p.sql")),
            ProjectSourceInput.ofText("db/anchors_q.sql", sql("anchors_q.sql"))
        ) + extra, false
    )

    private fun n(vararg s: String) = ProjectQualifiedName.of(*s)
    private fun type(vararg s: String) = ProjectWrittenAnchor.of(n(*s), ProjectTypeReference.Anchor.TYPE)

    // Exhaustive `when` without else: the written query adds no status.
    private fun label(status: ProjectTypeDescription.Status) = when (status) {
        ProjectTypeDescription.Status.DECLARED_TYPE -> "declared"
        ProjectTypeDescription.Status.BUILT_IN -> "built-in"
        ProjectTypeDescription.Status.UNRESOLVED_NAME -> "unresolved"
        ProjectTypeDescription.Status.ANCHORED_REFERENCE -> "anchored"
        ProjectTypeDescription.Status.REF_REFERENCE -> "ref"
        ProjectTypeDescription.Status.CYCLE -> "cycle"
        ProjectTypeDescription.Status.UNRESOLVED_VALUE -> "unresolved value"
    }

    @Test
    fun writtenAnchorsAreDescribedAndEchoed() {
        val snapshot = project()
        val request = type("p", "third_value")
        val chain = snapshot.describeType(request, null)
        assertThat(label(chain.status)).isEqualTo("built-in")
        assertThat(chain.writtenAnchor.get()).isSameAs(request)
        assertThat(chain.steps.map { it.qualifiedName }).containsExactly(
            n("p", "third_value"), n("p", "second_value"), n("p", "first_value"), n("p", "numeric_t")
        )
        assertThat(request.name).isEqualTo(n("p", "third_value"))
        assertThat(request.anchor).isEqualTo(ProjectTypeReference.Anchor.TYPE)
        assertThat(request).isEqualTo(type("P", "\"THIRD_VALUE\""))

        val missing = snapshot.describeType(type("p", "nope"), null)
        assertThat(label(missing.status)).isEqualTo("unresolved value")
        assertThat(missing.terminalReference.isPresent).isFalse()
        assertThat(missing.valueResolution.get().name).isEqualTo(n("p", "nope"))

        val row = snapshot.describeType(ProjectWrittenAnchor.of(n("employees"), ProjectTypeReference.Anchor.ROWTYPE), null)
        assertThat(row.anchorStop.get()).isEqualTo(ProjectTypeDescription.AnchorStop.ROWTYPE)
        assertThat(snapshot.describeType(type("g"), null).anchorStop.get()).isEqualTo(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE)
        assertThat(snapshot.describeType(type("rec_value", "id"), n("p")).anchorStop.get()).isEqualTo(ProjectTypeDescription.AnchorStop.FIELD_PATH)

        val retained = snapshot.findDeclarations(n("q", "a")).declarations.single() as ProjectPackageValueView
        val fromRetained = snapshot.describeType(retained.typeRef, retained.owner)
        assertThat(fromRetained.writtenAnchor.isPresent).isFalse()
        assertThat(snapshot.describeType(type("p", "g"), n("q")).steps.map { it.id }).isEqualTo(fromRetained.steps.map { it.id })
    }

    @Test
    fun incompletePreparationIsReported() {
        val failed = project(ProjectSourceInput.ofReader("db/bad.sql") { throw IOException("cannot read") })
        val d = failed.describeType(type("p", "g"), null)
        assertThat(d.valueResolution.get().status).isEqualTo(ProjectValueResolution.Status.INCOMPLETE_INDEX)
        assertThat(d.writtenAnchor.isPresent).isTrue()
    }
}
