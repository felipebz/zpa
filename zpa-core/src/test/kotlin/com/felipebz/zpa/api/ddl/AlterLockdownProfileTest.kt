/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AlterLockdownProfileTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_LOCKDOWN_PROFILE)
    }

    private val setCpuCount = "alter lockdown profile p disable statement = ('ALTER SYSTEM') clause = ('SET') " +
        "option = ('CPU_COUNT')"

    @Test
    fun matchesFeatures() {
        assertThat(p).matches("alter lockdown profile p disable feature = ('NETWORK_ACCESS');")
        assertThat(p).matches("alter lockdown profile p disable feature = ('LOB_FILE_ACCESS', 'TRACE_VIEW_ACCESS')")
        assertThat(p).matches("alter lockdown profile p enable feature all")
        assertThat(p).matches("alter lockdown profile p disable feature all except = ('A', 'B')")
    }

    @Test
    fun matchesOptions() {
        assertThat(p).matches("alter lockdown profile p disable option = ('DATABASE QUEUING')")
        assertThat(p).matches("alter lockdown profile p enable option all")
        assertThat(p).matches("alter lockdown profile p enable option all except = ('PARTITIONING')")
    }

    @Test
    fun matchesStatements() {
        assertThat(p).matches("alter lockdown profile p disable statement = ('ALTER DATABASE')")
        assertThat(p).matches("alter lockdown profile p disable statement = ('ALTER DATABASE', 'ALTER SYSTEM')")
        assertThat(p).matches("alter lockdown profile p enable statement all except = ('ALTER DATABASE')")
        assertThat(p).matches("alter lockdown profile p disable statement = ('ALTER SYSTEM') clause = ('SUSPEND', 'RESUME')")
        assertThat(p).matches(
            "alter lockdown profile p enable statement = ('ALTER PLUGGABLE DATABASE') " +
                "clause all except = ('DEFAULT TABLESPACE', 'DEFAULT TEMPORARY TABLESPACE')")
        assertThat(p).matches(
            "alter lockdown profile p enable statement = ('ALTER SESSION') clause = ('SET') " +
                "option = ('COMMIT_WAIT', 'CURSOR_SHARING')")
        assertThat(p).matches(
            "alter lockdown profile p enable statement = ('ALTER SESSION') clause = ('SET') option = ('COMMIT_WAIT')")
        assertThat(p).matches(
            "alter lockdown profile p disable statement = ('ALTER SYSTEM') clause = ('SET') " +
                "option all except = ('CPU_COUNT', 'QUERY_REWRITE_%')")
    }

    @Test
    fun matchesOptionValuesInAnyOrderOnce() {
        assertThat(p).matches("$setCpuCount minvalue = '8'")
        assertThat(p).matches("$setCpuCount maxvalue = '2'")
        assertThat(p).matches("$setCpuCount minvalue = '2' maxvalue = '6'")
        assertThat(p).matches("$setCpuCount maxvalue = '6' minvalue = '2'")
        assertThat(p).matches("$setCpuCount maxvalue = '6' value = ('4') minvalue = '2';")
        assertThat(p).matches("$setCpuCount value = ('4') maxvalue = '8'")
        assertThat(p).matches(
            "alter lockdown profile p disable statement = ('ALTER SYSTEM') clause = ('SET') " +
                "option = ('PDB_FILE_NAME_CONVERT') value = ('cdb1_pdb0', 'cdb1_pdb1')")
    }

    @Test
    fun rejectsMalformedRootAndLists() {
        assertThat(p).notMatches("alter lockdown profile")
        assertThat(p).notMatches("alter lockdown profile p")
        assertThat(p).notMatches("alter lockdown p disable feature all")
        assertThat(p).notMatches("alter lockdown profile p disable")
        assertThat(p).notMatches("alter lockdown profile sys.p disable feature all")
        assertThat(p).notMatches("alter lockdown profile p disable feature = ()")
        assertThat(p).notMatches("alter lockdown profile p disable feature = ('A',)")
        assertThat(p).notMatches("alter lockdown profile p disable feature = (NETWORK_ACCESS)")
        assertThat(p).notMatches("alter lockdown profile p disable feature ('A')")
        assertThat(p).notMatches("alter lockdown profile p disable feature all except")
        assertThat(p).notMatches("alter lockdown profile p disable feature all except = ()")
        assertThat(p).notMatches("alter lockdown profile p disable feature all except ('A')")
        assertThat(p).notMatches("alter lockdown profile p disable feature all = ('A')")
    }

    @Test
    fun rejectsMalformedRefinements() {
        val statement = "alter lockdown profile p disable statement = ('ALTER SYSTEM')"
        assertThat(p).notMatches("$statement clause")
        assertThat(p).notMatches("$statement clause = ()")
        assertThat(p).notMatches("$statement clause = ('SET') option = ()")
        assertThat(p).notMatches("$statement clause = ('SET') option = ('CPU_COUNT') value = '4'")
        assertThat(p).notMatches("$statement clause = ('SET') option = ('CPU_COUNT') minvalue = ('2')")
    }

    @Test
    fun rejectsRefinementsOracleRejects() {
        // Each of these fails with ORA-00922 in Oracle 26 at the refinement keyword.
        assertThat(p).notMatches(
            "alter lockdown profile p disable statement = ('ALTER SYSTEM', 'ALTER SESSION') clause = ('SET')")
        assertThat(p).notMatches("alter lockdown profile p disable statement all clause = ('SET')")
        assertThat(p).notMatches(
            "alter lockdown profile p disable statement = ('ALTER SYSTEM') clause = ('SET', 'RESET') " +
                "option = ('CPU_COUNT')")
        assertThat(p).notMatches(
            "alter lockdown profile p disable statement = ('ALTER SYSTEM') clause all option = ('CPU_COUNT')")
        assertThat(p).notMatches(
            "alter lockdown profile p disable statement = ('ALTER SYSTEM') clause = ('SET') " +
                "option = ('CPU_COUNT', 'SGA_TARGET') minvalue = '2'")
        assertThat(p).notMatches("$setCpuCount minvalue = '2' minvalue = '3'")
        assertThat(p).notMatches("$setCpuCount value = ('1') value = ('2')")
        assertThat(p).notMatches("$setCpuCount minvalue = '2' value = ('4') minvalue = '3'")
        assertThat(p).notMatches(
            "alter lockdown profile p enable statement = ('ALTER SYSTEM') clause = ('SET') " +
                "option = ('CPU_COUNT') minvalue = '2'")
    }

    @Test
    fun matchesUsersSuffix() {
        assertThat(p).matches("alter lockdown profile p disable feature = ('NETWORK_ACCESS') users = local")
        assertThat(p).matches("alter lockdown profile p disable feature = ('A', 'B') users = all")
        assertThat(p).matches("alter lockdown profile p enable option all users = common;")
        assertThat(p).matches("alter lockdown profile p disable statement = ('ALTER SYSTEM') users = all")
        assertThat(p).matches("alter lockdown profile p enable statement all users = local")
        assertThat(p).matches(
            "alter lockdown profile p disable statement = ('ALTER SYSTEM') clause = ('SUSPEND', 'RESUME') users = local")
        assertThat(p).matches("alter lockdown profile p disable statement = ('ALTER SYSTEM') clause all users = local")
        assertThat(p).matches(
            "alter lockdown profile p enable statement = ('ALTER SESSION') clause = ('SET') option = ('COMMIT_WAIT') " +
                "users = common")
        assertThat(p).matches("$setCpuCount users = local")
        assertThat(p).matches(
            "alter lockdown profile p disable statement = ('ALTER SYSTEM') clause = ('SET') option all users = local")
        assertThat(p).matches(
            "alter lockdown profile p disable statement = ('ALTER SYSTEM') clause = ('SET') " +
                "option = ('PDB_FILE_NAME_CONVERT') value = ('a') users = local")
    }

    @Test
    fun rejectsUsersWhereOracleRejectsIt() {
        // Oracle 26 reports ORA-00922 at USERS after any ALL EXCEPT list and after MINVALUE/MAXVALUE,
        // despite the root-level position in the syntax diagram.
        assertThat(p).notMatches("alter lockdown profile p disable feature all except = ('A') users = local")
        assertThat(p).notMatches("alter lockdown profile p enable option all except = ('A') users = local")
        assertThat(p).notMatches("alter lockdown profile p enable statement all except = ('ALTER DATABASE') users = all")
        assertThat(p).notMatches(
            "alter lockdown profile p disable statement = ('ALTER PLUGGABLE DATABASE') " +
                "clause all except = ('DEFAULT TABLESPACE') users = local")
        assertThat(p).notMatches(
            "alter lockdown profile p disable statement = ('ALTER SYSTEM') clause = ('SET') " +
                "option all except = ('A') users = local")
        assertThat(p).notMatches("$setCpuCount minvalue = '2' users = local")
        assertThat(p).notMatches("$setCpuCount maxvalue = '6' users = local")
        assertThat(p).notMatches("$setCpuCount minvalue = '2' maxvalue = '6' users = local")
        assertThat(p).notMatches("$setCpuCount minvalue = '2' value = ('4') users = local")
        assertThat(p).notMatches("$setCpuCount value = ('4') minvalue = '2' users = local")
    }

    @Test
    fun rejectsMalformedUsersSuffix() {
        assertThat(p).notMatches("alter lockdown profile p users = local disable feature all")
        assertThat(p).notMatches("alter lockdown profile p disable statement = ('ALTER SYSTEM') users = local clause = ('SET')")
        assertThat(p).notMatches(
            "alter lockdown profile p disable statement = ('ALTER SYSTEM') clause = ('SET') users = local " +
                "option = ('CPU_COUNT')")
        assertThat(p).notMatches("alter lockdown profile p disable feature all users")
        assertThat(p).notMatches("alter lockdown profile p disable feature all users =")
        assertThat(p).notMatches("alter lockdown profile p disable feature all users = 'LOCAL'")
        assertThat(p).notMatches("alter lockdown profile p disable feature all users = (local)")
        assertThat(p).notMatches("alter lockdown profile p disable feature all users = public")
        // ORA-00922 at the second USERS.
        assertThat(p).notMatches("alter lockdown profile p disable feature all users = local users = common")
    }
}
