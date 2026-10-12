package consumer

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/** External relation metadata and exact lookups through the packaged zpa-core jar only. */
@OptIn(ZpaExperimentalApi::class)
class ProjectExternalMetadataKotlinConsumerTest {
    private fun id(s: String) = ProjectIdentifier.fromSource(s)

    private fun label(status: ProjectExternalLookup.Status) = when (status) {
        ProjectExternalLookup.Status.FOUND -> "found"
        ProjectExternalLookup.Status.RELATION_NOT_FOUND -> "no relation"
        ProjectExternalLookup.Status.COLUMN_NOT_FOUND -> "no column"
        ProjectExternalLookup.Status.NOT_COVERED -> "not covered"
        ProjectExternalLookup.Status.UNAVAILABLE -> "unavailable"
    }

    @Test
    fun buildsLooksUpAndOrdersRowFields() {
        val relation = ProjectExternalRelation.of(
            "HR", "EMPLOYEES", true, listOf(
                ProjectExternalColumn.of("SALARY", 2, ProjectExternalDatatype.of("NUMBER", null, null, null, 10, 2)),
                ProjectExternalColumn.of("NAME", 3, ProjectExternalDatatype.of("VARCHAR2", null, 30, ProjectExternalDatatype.LengthSemantics.BYTE, null, null)),
                ProjectExternalColumn.of("ID", 1, ProjectExternalDatatype.of("NUMBER", null, null, null, 8, 0))
            )
        )
        val metadata = ProjectExternalMetadata.builder("fixture").completeSchema("HR").relation(relation).build()
        assertThat(metadata.findRelation(id("hr"), id("employees")).relation.get().rowFields.map { it.name })
            .containsExactly(id("id"), id("salary"), id("name"))
        val id = metadata.findColumn(id("hr"), id("employees"), id("id"))
        assertThat(label(id.status)).isEqualTo("found")
        val type = id.column.get().datatype.get()
        assertThat(type.precision.asInt).isEqualTo(8)
        assertThat(type.scale.asInt).isEqualTo(0)
        assertThat(type.category.get()).isEqualTo(ProjectBuiltInTypeCategory.NUMERIC)
        assertThat(label(metadata.findColumn(id("hr"), id("employees"), id("bonus")).status)).isEqualTo("no column")
        assertThat(label(metadata.findRelation(id("app"), id("x")).status)).isEqualTo("not covered")
        assertThat(label(metadata.findRelation(id("hr"), id("x")).status)).isEqualTo("no relation")
        assertThat(label(ProjectExternalMetadata.none().findRelation(id("hr"), id("x")).status)).isEqualTo("unavailable")
        assertThat(ProjectExternalColumn.of("Emp", null, null).name).isEqualTo(id("\"Emp\"")).isNotEqualTo(id("emp"))
        assertThatThrownBy { (relation.rowFields as MutableList<ProjectExternalColumn>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)
    }
}
