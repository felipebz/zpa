package consumer

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectSnapshots
import com.felipebz.zpa.api.project.ProjectSourceInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Runs the documented Kotlin call example against the packaged jar. */
@OptIn(ZpaExperimentalApi::class)
class ProjectCallExampleKotlinTest {
    private val spec = "CREATE PACKAGE billing AS PROCEDURE charge(account_id NUMBER, amount NUMBER, " +
        "currency VARCHAR2 DEFAULT 'EUR'); END billing;"
    private val standalone = "CREATE PROCEDURE billing.charge(account_id NUMBER, amount NUMBER) IS BEGIN NULL; END;"

    @Test
    fun resolvesAWrittenNameWithOneInterpretation() {
        val snapshot = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("billing.pks", spec)), false)
        assertThat(describeWrittenCall(snapshot)).containsExactly(
            "calls billing.charge",
            "SPECIFICATION billing.charge: APPLICABLE #1->account_id #2->amount"
        )
    }

    @Test
    fun keepsEvidenceButSelectsNothingWhenTheNamespaceCompetes() {
        val snapshot = ProjectSnapshots.prepare(listOf(
            ProjectSourceInput.ofText("billing.pks", spec),
            ProjectSourceInput.ofText("standalone.sql", standalone)
        ), false)
        assertThat(describeWrittenCall(snapshot)).containsExactly(
            "not settled: [NAMESPACE_COMPETITION] []",
            "SPECIFICATION billing.charge: APPLICABLE #1->account_id #2->amount",
            "STANDALONE billing.charge: APPLICABLE #1->account_id #2->amount"
        )
    }
}
