package consumer

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Source/external type composition of written and retained anchors through the packaged zpa-core jar only. */
@OptIn(ZpaExperimentalApi::class)
class ProjectExternalTypeDescriptionKotlinConsumerTest {
    private val hr = ProjectIdentifier.fromSource("HR")
    private val metadata = ProjectExternalMetadata.builder("fixture").completeSchema("HR").relation(
        ProjectExternalRelation.of(
            "HR", "EMPLOYEES", true, listOf(
                ProjectExternalColumn.of("NAME", 3, ProjectExternalDatatype.of("VARCHAR2", null, 30, ProjectExternalDatatype.LengthSemantics.BYTE, null, null)),
                ProjectExternalColumn.of("ID", 1, ProjectExternalDatatype.of("NUMBER", null, null, null, 8, 0)),
                ProjectExternalColumn.of("SALARY", 2, ProjectExternalDatatype.of("NUMBER", null, null, null, 10, 2))
            )
        )
    ).build()

    private fun label(status: ProjectExternalTypeDescription.Status) = when (status) {
        ProjectExternalTypeDescription.Status.RESOLVED -> "resolved"
        ProjectExternalTypeDescription.Status.NOT_EXTERNAL -> "not external"
        ProjectExternalTypeDescription.Status.CAPTURE_NOT_EXCLUDED -> "capture not excluded"
        ProjectExternalTypeDescription.Status.SOURCE_INCOMPLETE -> "source incomplete"
        ProjectExternalTypeDescription.Status.CONFLICT -> "conflict"
        ProjectExternalTypeDescription.Status.UNAVAILABLE -> "unavailable"
        ProjectExternalTypeDescription.Status.SCHEMA_CONTEXT_MISSING -> "schema context missing"
        ProjectExternalTypeDescription.Status.AMBIGUOUS -> "ambiguous"
        ProjectExternalTypeDescription.Status.INCOMPLETE -> "incomplete"
        ProjectExternalTypeDescription.Status.RELATION_NOT_FOUND -> "relation not found"
        ProjectExternalTypeDescription.Status.COLUMN_NOT_FOUND -> "column not found"
    }

    @Test
    fun composesWrittenAndRetainedAnchors() {
        val empty = ProjectSnapshots.prepare(emptyList(), false)
        val sal = empty.describeExternalType(ProjectWrittenAnchor.of(ProjectQualifiedName.of("employees", "salary"), ProjectTypeReference.Anchor.TYPE), null, metadata, hr)
        assertThat(label(sal.status)).isEqualTo("resolved")
        assertThat(sal.lookup.get().column.get().datatype.get().scale.asInt).isEqualTo(2)
        assertThat(label(empty.describeExternalType(ProjectWrittenAnchor.of(ProjectQualifiedName.of("employees"), ProjectTypeReference.Anchor.ROWTYPE), null, metadata, null).status))
            .isEqualTo("schema context missing")

        val snapshot = ProjectSnapshots.prepare(
            listOf(ProjectSourceInput.ofText("p_spec.sql", "CREATE PACKAGE p AS CURSOR employees RETURN r_t; TYPE rows_t IS TABLE OF employees%ROWTYPE; TYPE free_t IS TABLE OF jobs%ROWTYPE; END p;")),
            false
        )
        fun element(name: String) = (snapshot.findDeclarations(ProjectQualifiedName.of("p", name)).declarations.single() as ProjectShapedTypeView).elementType.get()
        assertThat(label(snapshot.describeExternalType(element("rows_t"), ProjectQualifiedName.of("p"), metadata, hr).status)).isEqualTo("not external")
        val free = snapshot.describeExternalType(element("free_t"), ProjectQualifiedName.of("p"), metadata, hr)
        assertThat(label(free.status)).isEqualTo("relation not found")
        assertThat(free.source.terminalReference.get().sourceRange.fileId).isEqualTo("p_spec.sql")
    }
}
