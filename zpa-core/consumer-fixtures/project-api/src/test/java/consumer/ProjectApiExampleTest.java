package consumer;

import com.felipebz.zpa.api.project.ProjectSourceInput;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectApiExampleTest {
    private static ProjectSourceInput resource(String id, String name) {
        return ProjectSourceInput.ofReader(id, () -> {
            try (var in = ProjectApiExampleTest.class.getResourceAsStream("/sql/" + name)) {
                return new String(in.readAllBytes());
            }
        });
    }

    @Test
    void javaExampleHandlesEachOutcome() {
        ProjectSourceInput spec = resource("shop_spec.sql", "shop_spec.sql");
        ProjectSourceInput body = resource("shop_body.sql", "shop_body.sql");
        assertThat(ProjectApiExample.describeShop(List.of(spec, body))).containsExactly(
            "field 0: id NUMBER", "field 1: customer VARCHAR2", "field 2: total NUMBER", "field 3: note orders.note",
            "BODY total(o) returns money_t", "SPECIFICATION total(o) returns money_t", "body matches shop_spec.sql");
        assertThat(ProjectApiExample.describeShop(List.of(body))).contains("order_t is not in the supplied sources (not proof it is missing in Oracle)",
            "no matching specification was supplied");
        ProjectSourceInput broken = ProjectSourceInput.ofReader("broken.sql", () -> {
            throw new IOException("boom");
        });
        assertThat(ProjectApiExample.describeShop(List.of(spec, body, broken)))
            .contains("unknown: 1 source(s) could not be read", "unknown: some sources failed");
        assertThat(ProjectApiExample.describeShop(List.of(spec, body, resource("dup.sql", "shop_duplicate_type.sql"))))
            .contains("order_t is declared 2 times in the supplied sources");
    }
}
