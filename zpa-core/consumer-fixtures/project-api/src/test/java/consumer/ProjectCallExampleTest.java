package consumer;

import com.felipebz.zpa.api.project.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs the documented call-resolution example against the packaged jar. */
class ProjectCallExampleTest {
    static final String SPEC = """
        CREATE PACKAGE billing AS
          PROCEDURE charge(account_id NUMBER, amount NUMBER, currency VARCHAR2 DEFAULT 'EUR');
          PROCEDURE charge(invoice_id NUMBER);
          FUNCTION total(account_id NUMBER) RETURN NUMBER;
        END billing;
        """;
    static final String BODY = """
        CREATE PACKAGE BODY billing AS
          PROCEDURE charge(account_id NUMBER, amount NUMBER, currency VARCHAR2) IS BEGIN NULL; END;
          PROCEDURE charge(invoice_id NUMBER) IS BEGIN NULL; END;
          FUNCTION total(account_id NUMBER) RETURN NUMBER IS BEGIN RETURN 0; END;
        END billing;
        """;

    private static ProjectSnapshot billing(ProjectSourceInput... extra) {
        List<ProjectSourceInput> inputs = new java.util.ArrayList<>(List.of(
            ProjectSourceInput.ofText("billing.pks", SPEC), ProjectSourceInput.ofText("billing.pkb", BODY)));
        inputs.addAll(List.of(extra));
        return ProjectSnapshots.prepare(inputs, false);
    }

    private static final ProjectCallTarget CHARGE =
        ProjectCallTarget.packageMember(ProjectQualifiedName.of("billing"), ProjectIdentifier.fromSource("charge"));

    @Test
    void listsSpecificationCandidatesOnly() {
        assertThat(ProjectCallExample.listCandidates(billing())).containsExactly(
            "status FOUND",
            "SPECIFICATION charge(account_id, amount, currency)",
            "SPECIFICATION charge(invoice_id)");
    }

    @Test
    void explainsMixedPositionalAndNamedCall() {
        assertThat(ProjectCallExample.explain(billing(), CHARGE, ProjectCallExample.mixedArguments())).containsExactly(
            "calls charge(account_id, amount, currency)",
            "charge(account_id, amount, currency): APPLICABLE #1->account_id #2->amount default currency",
            "charge(invoice_id): UNKNOWN_LABEL rejects #2");
    }

    @Test
    void explainsInvalidNotationAndUncertainty() {
        ProjectCallArguments afterNamed = ProjectCallArguments.of(List.of(
            ProjectCallArgument.named(ProjectIdentifier.fromSource("amount")), ProjectCallArgument.positional()));
        assertThat(ProjectCallExample.explain(billing(), CHARGE, afterNamed))
            .containsExactly("invalid notation at argument 2");

        ProjectSnapshot failed = billing(ProjectSourceInput.ofReader("broken.sql", () -> {
            throw new IOException("unreadable");
        }));
        assertThat(ProjectCallExample.explain(failed, CHARGE, ProjectCallExample.mixedArguments())).containsExactly(
            "unknown: sources failed [broken.sql]",
            "charge(account_id, amount, currency): APPLICABLE #1->account_id #2->amount default currency",
            "charge(invoice_id): UNKNOWN_LABEL rejects #2");
    }

    @Test
    void documentedWalkthroughRunsAndPrintsTheSelection() {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        java.io.PrintStream original = System.out;
        System.setOut(new java.io.PrintStream(out, true, java.nio.charset.StandardCharsets.UTF_8));
        try {
            ProjectCallExample.documentedWalkthrough(SPEC, BODY, ProjectCallExample.mixedArguments());
        } finally {
            System.setOut(original);
        }
        assertThat(out.toString(java.nio.charset.StandardCharsets.UTF_8).lines()).containsExactly(
            "SPECIFICATION charge",
            "SPECIFICATION charge",
            "calls billing.pks#1",
            "billing.pks#1: APPLICABLE",
            "  #1 -> account_id",
            "  #2 -> amount",
            "  default currency",
            "billing.pks#2: UNKNOWN_LABEL",
            "  rejects #2");
    }
}
