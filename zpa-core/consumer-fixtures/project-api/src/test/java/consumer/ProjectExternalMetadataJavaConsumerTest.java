package consumer;

import com.felipebz.zpa.api.project.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** External relation metadata and exact lookups through the packaged zpa-core jar only. */
class ProjectExternalMetadataJavaConsumerTest {
    private static ProjectIdentifier id(String spelling) {
        return ProjectIdentifier.fromSource(spelling);
    }

    private static ProjectExternalMetadata employees() {
        List<ProjectExternalColumn> columns = new ArrayList<>(List.of(
            ProjectExternalColumn.of("NAME", 3, ProjectExternalDatatype.of("VARCHAR2", null, 30, ProjectExternalDatatype.LengthSemantics.BYTE, null, null)),
            ProjectExternalColumn.of("ID", 1, ProjectExternalDatatype.of("NUMBER", null, null, null, 8, 0)),
            ProjectExternalColumn.of("SALARY", 2, ProjectExternalDatatype.of("NUMBER", null, null, null, 10, 2))));
        ProjectExternalRelation relation = ProjectExternalRelation.of("HR", "EMPLOYEES", true, columns);
        columns.clear();
        return ProjectExternalMetadata.builder("fixture").completeSchema("HR").relation(relation).build();
    }

    // Deliberately exhaustive and without a default branch.
    private static String label(ProjectExternalLookup.Status status) {
        return switch (status) {
            case FOUND -> "found";
            case RELATION_NOT_FOUND -> "no relation";
            case COLUMN_NOT_FOUND -> "no column";
            case NOT_COVERED -> "not covered";
            case UNAVAILABLE -> "unavailable";
        };
    }

    private static String label(ProjectExternalDatatype.LengthSemantics semantics) {
        return switch (semantics) {
            case BYTE -> "byte";
            case CHAR -> "char";
        };
    }

    @Test
    void buildsTheMetadataAndReadsOrderedRowFields() {
        ProjectExternalMetadata metadata = employees();
        assertThat(metadata.getOrigin()).contains("fixture");
        ProjectExternalLookup relation = metadata.findRelation(id("hr"), id("employees"));
        assertThat(label(relation.getStatus())).isEqualTo("found");
        ProjectExternalRelation employees = relation.getRelation().orElseThrow();
        assertThat(employees.isColumnsComplete()).isTrue();
        assertThat(employees.getRowObjectType()).isEmpty();
        assertThat(employees.getRowFields()).extracting(ProjectExternalColumn::getName).containsExactly(id("id"), id("salary"), id("name"));
        assertThat(employees.getRowFields()).extracting(c -> c.getPosition().getAsInt()).containsExactly(1, 2, 3);
        assertThatThrownBy(() -> employees.getRowFields().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> employees.getColumns().add(null)).isInstanceOf(UnsupportedOperationException.class);

        ProjectExternalLookup salary = metadata.findColumn(id("HR"), id("EMPLOYEES"), id("SALARY"));
        ProjectExternalDatatype type = salary.getColumn().orElseThrow().getDatatype().orElseThrow();
        assertThat(type.getName()).isEqualTo("NUMBER");
        assertThat(type.getPrecision()).isEqualTo(OptionalInt.of(10));
        assertThat(type.getScale().getAsInt()).isEqualTo(2);
        assertThat(type.getLength()).isEqualTo(OptionalInt.empty());
        assertThat(type.getCategory()).contains(ProjectBuiltInTypeCategory.NUMERIC);
        assertThat(type.getTypeOwner()).isEmpty();

        ProjectExternalDatatype name = metadata.findColumn(id("hr"), id("employees"), id("name")).getColumn().orElseThrow().getDatatype().orElseThrow();
        assertThat(label(name.getLengthSemantics().orElseThrow())).isEqualTo("byte");
        assertThat(name.getLength().getAsInt()).isEqualTo(30);
    }

    @Test
    void lookupsReportCoverage() {
        ProjectExternalMetadata metadata = employees();
        assertThat(label(metadata.findColumn(id("hr"), id("employees"), id("bonus")).getStatus())).isEqualTo("no column");
        assertThat(label(metadata.findColumn(id("hr"), id("employees"), id("sys_nc00007$")).getStatus())).isEqualTo("not covered");
        assertThat(label(metadata.findRelation(id("hr"), id("jobs")).getStatus())).isEqualTo("no relation");
        assertThat(label(metadata.findRelation(id("app"), id("jobs")).getStatus())).isEqualTo("not covered");
        ProjectExternalLookup none = ProjectExternalMetadata.none().findColumn(id("hr"), id("employees"), id("id"));
        assertThat(label(none.getStatus())).isEqualTo("unavailable");
        assertThat(none.getOrigin()).isEmpty();
        assertThat(none.getColumnName()).contains(id("id"));
        ProjectExternalLookup partial = metadata.findColumn(id("hr"), id("employees"), id("bonus"));
        assertThat(partial.getRelation()).isPresent();
        assertThat(partial.getColumn()).isEmpty();
    }

    @Test
    void objectTablesAndUncategorizedTypes() {
        ProjectExternalDatatype rowType = ProjectExternalDatatype.of("EMP_OBJ_T", "HR", null, null, null, null);
        ProjectExternalRelation table = ProjectExternalRelation.objectTable("HR", "EMP_OT", rowType, true,
            List.of(ProjectExternalColumn.of("A", 1, null), ProjectExternalColumn.of("INV", null, null)));
        assertThat(table.getRowObjectType()).contains(rowType);
        assertThat(table.getRowFields()).hasSize(1);
        assertThat(table.getColumns()).hasSize(2);
        assertThat(table.isView()).isFalse();
        ProjectExternalRelation objectView = ProjectExternalRelation.objectView("HR", "EMP_OV", rowType, true,
            List.of(ProjectExternalColumn.of("A", 1, null)));
        assertThat(objectView.isView()).isTrue();
        assertThat(objectView.getRowObjectType()).contains(rowType);
        ProjectExternalRelation view = ProjectExternalRelation.view("HR", "EMP_V", true, List.of(ProjectExternalColumn.of("A", 1, null)));
        assertThat(view.isView()).isTrue();
        assertThat(view.getRowObjectType()).isEmpty();
        assertThat(rowType.getCategory()).isEmpty();
        assertThat(rowType.getTypeOwner()).contains(id("hr"));
        assertThat(ProjectExternalDatatype.of("XMLTYPE", "PUBLIC", null, null, null, null).getCategory()).isEmpty();
        assertThatThrownBy(() -> ProjectExternalRelation.of("HR", "T", true,
            List.of(ProjectExternalColumn.of("A", 1, null), ProjectExternalColumn.of("B", 1, null)))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void onlyTheDocumentedFactoriesArePublic() {
        assertThat(Modifier.isAbstract(ProjectExternalMetadata.Builder.class.getModifiers())).isTrue();
        // Abstract, and its only constructor is compiler-synthetic (not callable from Java source).
        assertThat(Arrays.stream(ProjectExternalMetadata.Builder.class.getConstructors()).filter(c -> !c.isSynthetic())).isEmpty();
        for (Class<?> type : List.of(ProjectExternalMetadata.class, ProjectExternalRelation.class, ProjectExternalColumn.class,
            ProjectExternalDatatype.class, ProjectExternalLookup.class)) {
            assertThat(Arrays.stream(type.getConstructors()).filter(c -> !c.isSynthetic())).as(type.getName()).isEmpty();
        }
        assertThat(Arrays.stream(ProjectExternalLookup.class.getMethods()).filter(m -> Modifier.isStatic(m.getModifiers())).map(m -> m.getName()))
            .doesNotContain("of");
        assertThat(Arrays.stream(ProjectExternalLookup.Status.values()).map(Enum::name))
            .containsExactly("FOUND", "RELATION_NOT_FOUND", "COLUMN_NOT_FOUND", "NOT_COVERED", "UNAVAILABLE");
    }

    @Test
    void nullColumnElementsAreRejectedWithIllegalArgumentException() {
        List<ProjectExternalColumn> columns = new ArrayList<>();
        columns.add(ProjectExternalColumn.of("ID", 1, null));
        columns.add(null);
        assertThatThrownBy(() -> ProjectExternalRelation.of("HR", "EMPLOYEES", true, columns))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("index 1");
        ProjectExternalDatatype rowType = ProjectExternalDatatype.of("EMP_T", "HR", null, null, null, null);
        assertThatThrownBy(() -> ProjectExternalRelation.objectTable("HR", "EMP_OT", rowType, true, columns))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
