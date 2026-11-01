//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DisassociateStatisticsTest : RuleTest() {
    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.DISASSOCIATE_STATISTICS)
    }

    @Test
    fun matchesEveryTargetFamilyAndForceAfterList() {
        assertThat(p).matches("disassociate statistics from packages hr.emp_mgmt;")
        assertThat(p).matches("disassociate statistics from columns hr.emp_mgmt.col1 force;")
        assertThat(p).matches("disassociate statistics from columns tab.col, other.col force")
        for (family in listOf("functions", "packages", "types", "indexes", "indextypes")) {
            assertThat(p).matches("disassociate statistics from $family name")
            assertThat(p).matches("disassociate statistics from $family schema.name, other force")
            assertThat(p).matches("disassociate statistics from $family a.b.c force")
            assertThat(p).notMatches("disassociate statistics from $family a.b.c.d force")
            assertThat(p).notMatches("disassociate statistics from $family name,")
        }
        // FORCE after a comma is a second (nonreserved) object name, not the FORCE option (ORA-29816).
        assertThat(p).matches("disassociate statistics from packages pkg, force")
        assertThat(p).notMatches("disassociate statistics from columns col force")
        assertThat(p).notMatches("disassociate statistics from columns a.b.c.d force")
        assertThat(p).notMatches("disassociate statistics from packages")
        assertThat(p).notMatches("disassociate statistics from packages pkg force force")
        assertThat(p).notMatches("disassociate statistics from packages force pkg")
        assertThat(p).notMatches("disassociate statistics force from packages pkg")
        assertThat(p).notMatches("disassociate statistics from packages pkg using stats_type")
    }

    @Test
    fun routesThroughDdl() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("disassociate statistics from packages hr.emp_mgmt;")
        assertThat(p).matches("disassociate statistics from columns hr.emp_mgmt.col1 force;")
    }
}
