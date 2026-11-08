//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectSnapshots
import com.felipebz.zpa.api.project.ProjectSourceInput
import com.felipebz.zpa.api.project.ProjectTypedCallResolution
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * A schema-qualified call in a package body whose specification, in another file, has conditional text: the first
 * segment reaches the project unless that text, or the body's own, writes its name.
 */
@OptIn(ZpaExperimentalApi::class)
class LocalConditionalSpecificationTest {
    private val target = "create or replace package app_owner.pk_x as\n  procedure run(p_val number);\nend;\n/\n"

    private fun specification(conditional: String) =
        "create or replace package pk_caller as\n  procedure work;\n  $conditional\nend;\n/\n"

    private fun body(declarations: String = "") =
        "create or replace package body pk_caller as\n$declarations  procedure work is\n  begin\n    app_owner.pk_x.run(1);\n  end;\nend;\n/\n"

    private fun call(specification: String, body: String = body()): LocalCallAnalysis {
        val snapshot = ProjectSnapshots.prepare(
            listOf(ProjectSourceInput.ofText("pk_caller.pks", specification), ProjectSourceInput.ofText("pk_x.pks", target)), false
        )
        return LocalForwardCallTest().analyze(body, snapshot, "pk_caller.pkb").calls.single { it.target.toString().endsWith("run") }
    }

    @Test
    fun conditionalTextOfTheSpecificationThatWritesOtherNamesLetsTheCallReachItsPackage() {
        val analysis = call(specification("\$IF \$\$v2 \$THEN procedure later_work; \$ELSE procedure old_work; \$END"))
        assertThat(analysis.outcome).isEqualTo(LocalCallOutcome.PROJECT)
        assertThat(analysis.typed!!.status).isEqualTo(ProjectTypedCallResolution.Status.RESOLVED)
    }

    @Test
    fun conditionalTextOfTheSpecificationThatWritesTheFirstSegmentKeepsItIncomplete() {
        // The text writes the schema's name: another compilation may declare a member that captures it.
        val mentioned = call(specification("\$IF \$\$v2 \$THEN app_owner constant number := 1; \$END"))
        assertThat(mentioned.outcome).isEqualTo(LocalCallOutcome.INCOMPLETE)
        // Text that may change the block structure counts for every name.
        val wide = call(specification("\$IF \$\$v2 \$THEN procedure later_work; \$ELSE end; \$END"))
        assertThat(wide.outcome).isEqualTo(LocalCallOutcome.INCOMPLETE)
    }

    @Test
    fun conditionalTextOfTheCallingBodyCountsWhenItWritesTheFirstSegment() {
        val plain = specification("\$IF \$\$v2 \$THEN procedure later_work; \$END")
        // A dropped declaration of the body may capture the schema's name.
        val writes = body("  \$IF \$\$v2 \$THEN g_count number; \$ELSE app_owner number; \$END\n")
        assertThat(call(plain, writes).outcome).isEqualTo(LocalCallOutcome.INCOMPLETE)
        val unrelated = body("  \$IF \$\$v2 \$THEN g_count number; \$ELSE g_total number; \$END\n")
        assertThat(call(plain, unrelated).outcome).isEqualTo(LocalCallOutcome.PROJECT)
    }
}
