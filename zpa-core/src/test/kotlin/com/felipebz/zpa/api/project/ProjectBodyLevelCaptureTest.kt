//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectExternalTypeDescription.Status
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Column anchors in the subprograms of a package body, against the declarations of the body's own level. */
@OptIn(ZpaExperimentalApi::class)
class ProjectBodyLevelCaptureTest {
    private val hr = ProjectIdentifier.fromSource("HR")
    private val metadata = ProjectExternalMetadata.builder("fixture").completeSchema("HR").relation(
        ProjectExternalRelation.of(
            "HR", "CUSTOMER", true, listOf(
                ProjectExternalColumn.of("CUSTOMER_ID", 1, ProjectExternalDatatype.of("NUMBER", null, null, null, 7, 0)),
                ProjectExternalColumn.of("NAME", 2, ProjectExternalDatatype.of("VARCHAR2", null, 60, ProjectExternalDatatype.LengthSemantics.BYTE, null, null))
            )
        )
    ).build()

    private val spec = "CREATE PACKAGE pk_cust AS\n  PROCEDURE touch;\nEND;\n/\n"
    private val nameOf = "  FUNCTION name_of(p_id customer.customer_id%TYPE) RETURN customer.name%TYPE IS BEGIN RETURN NULL; END;\n"
    private val touch = "  PROCEDURE touch IS BEGIN NULL; END;\n"

    private fun body(vararg levels: String) = "CREATE PACKAGE BODY pk_cust AS\n" + levels.joinToString("") + "END;\n/\n"

    /** The statuses of the formal and the return type of `name_of`, described from the body. */
    private fun describe(body: String): List<Status> {
        val s = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("s.sql", spec), ProjectSourceInput.ofText("b.sql", body)), false)
        val f = s.findPackageSubprograms(ProjectQualifiedName.of("pk_cust"), ProjectIdentifier.fromSource("name_of")).declarations.single() as ProjectFunctionView
        val owner = ProjectQualifiedName.of("pk_cust")
        return listOf(f.parameters.single().typeRef, f.returnType).map { s.describeExternalType(it, owner, metadata, hr).status }
    }

    @Test
    fun noBodyDeclarationOfTheNameLeavesTheColumn() {
        assertThat(describe(body("  g_count PLS_INTEGER;\n", nameOf, touch))).containsOnly(Status.RESOLVED)
        // Declarations inside another subprogram of the body are not visible to the formal.
        assertThat(describe(body("  PROCEDURE inner IS customer PLS_INTEGER; BEGIN NULL; END;\n", nameOf, touch))).containsOnly(Status.RESOLVED)
    }

    @Test
    fun aBodyDeclarationBeforeTheSubprogramCapturesTheName() {
        // A variable: Oracle reports PLS-00487 on the anchor.
        assertThat(describe(body("  customer PLS_INTEGER;\n", nameOf, touch))).containsOnly(Status.NOT_EXTERNAL)
        // A record variable: Oracle reads the anchor as its field.
        assertThat(describe(body("  TYPE t_c IS RECORD (customer_id VARCHAR2(5), name DATE);\n  customer t_c;\n", nameOf, touch)))
            .containsOnly(Status.NOT_EXTERNAL)
        // A type: Oracle reports PLS-00206.
        assertThat(describe(body("  TYPE customer IS RECORD (customer_id NUMBER, name VARCHAR2(5));\n", nameOf, touch))).containsOnly(Status.NOT_EXTERNAL)
    }

    @Test
    fun aBodySubprogramDeclaredLaterStillTakesTheName() {
        // Only subprograms may follow a subprogram body; a later one of the name makes Oracle reject the anchor (PLS-00225).
        assertThat(describe(body(nameOf, "  FUNCTION customer RETURN NUMBER IS BEGIN RETURN 1; END;\n", touch))).containsOnly(Status.NOT_EXTERNAL)
        assertThat(describe(body(nameOf, "  PROCEDURE customer IS BEGIN NULL; END;\n", touch))).containsOnly(Status.NOT_EXTERNAL)
    }

    @Test
    fun undecidedConditionalCompilationKeepsTheCaptureOpen() {
        val conditional = "  \$IF \$\$with_cache \$THEN customer PLS_INTEGER; \$END\n"
        assertThat(describe(body(conditional, nameOf, touch))).containsOnly(Status.CAPTURE_NOT_EXCLUDED)
        // Conditional text that declares nothing of the name leaves it decided.
        val unrelated = "  \$IF \$\$with_cache \$THEN g_cache PLS_INTEGER; \$END\n"
        assertThat(describe(body(unrelated, nameOf, touch))).containsOnly(Status.RESOLVED)
    }
}
