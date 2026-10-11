package consumer

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectSourceInput
import com.felipebz.zpa.api.project.ProjectSourceReader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ZpaExperimentalApi::class)
class ProjectApiExampleKotlinTest {
    private fun resource(id: String, name: String) = ProjectSourceInput.ofReader(id, ProjectSourceReader {
        checkNotNull(javaClass.getResourceAsStream("/sql/$name")).use { String(it.readAllBytes()) }
    })

    @Test
    fun kotlinExampleHandlesEachOutcome() {
        val spec = resource("shop_spec.sql", "shop_spec.sql")
        val body = resource("shop_body.sql", "shop_body.sql")
        assertThat(describeShop(listOf(spec, body))).containsExactly(
            "field 0: id NUMBER", "field 1: customer VARCHAR2", "field 2: total NUMBER", "field 3: note orders.note",
            "BODY total(o) returns money_t", "SPECIFICATION total(o) returns money_t", "body matches shop_spec.sql"
        )
        assertThat(describeShop(listOf(body)))
            .contains("order_t is not in the supplied sources (not proof it is missing in Oracle)", "no matching specification was supplied")
        val broken = ProjectSourceInput.ofReader("broken.sql", ProjectSourceReader { throw IOException("boom") })
        assertThat(describeShop(listOf(spec, body, broken)))
            .contains("unknown: [broken.sql] could not be read", "unknown: some sources failed")
        assertThat(describeShop(listOf(spec, body, resource("dup.sql", "shop_duplicate_type.sql"))))
            .contains("order_t is declared 2 times in the supplied sources")
    }
}
