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
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.Test

class AuditTest : RuleTest() {

    @Test
    fun matchesUnifiedAudit() {
        setRootRule(DdlGrammar.AUDIT_STATEMENT)
        assertThat(p).matches("audit policy table_pol;")
        assertThat(p).matches("audit policy dml_pol by hr, sh")
        assertThat(p).matches("audit policy read_dir_pol except hr, sh whenever not successful")
        assertThat(p).matches("audit policy security_pol by hr whenever not successful;")
        assertThat(p).matches("audit policy p by users with granted roles dba, connect whenever successful")
        assertThat(p).matches("audit policy p by public")
        assertThat(p).matches("audit context namespace userenv attributes current_user, db_name by hr;")
        assertThat(p).matches("audit context namespace userenv attributes current_user, " +
            "context namespace c2 attributes a, b by hr, sh")
    }

    @Test
    fun matchesUnifiedNoaudit() {
        setRootRule(DdlGrammar.NOAUDIT_STATEMENT)
        assertThat(p).matches("noaudit policy table_pol;")
        assertThat(p).matches("noaudit policy dml_pol by hr whenever not successful")
        assertThat(p).matches("noaudit policy p except hr")
        assertThat(p).matches("noaudit policy p by users with granted roles dba")
        assertThat(p).matches("noaudit context namespace userenv attributes current_user, db_name by hr;")
    }

    @Test
    fun matchesTraditionalNoaudit() {
        setRootRule(DdlGrammar.NOAUDIT_STATEMENT)
        assertThat(p).matches("noaudit role;")
        assertThat(p).matches("noaudit select table, update table by hr, oe whenever successful")
        assertThat(p).matches("noaudit delete any table, create session")
        assertThat(p).matches("noaudit all statements")
        assertThat(p).matches("noaudit all privileges by hr")
        assertThat(p).matches("noaudit direct_path load by hr")
        assertThat(p).matches("noaudit network")
        assertThat(p).matches("noaudit role whenever successful container = all")
        assertThat(p).matches("noaudit select on hr.employees whenever successful;")
        assertThat(p).matches("noaudit alter, grant, insert, update, delete on default")
        assertThat(p).matches("noaudit read on directory bfile_dir")
        assertThat(p).matches("noaudit select on mining model hr.m")
        assertThat(p).matches("noaudit use on sql translation profile hr.p")
    }

    @Test
    fun rejectsFormsOracleRejects() {
        setRootRule(DdlGrammar.AUDIT_STATEMENT)
        // ORA-03048: fixed order, a single unqualified policy, no WHENEVER or roles for contexts.
        assertThat(p).notMatches("audit policy s.p")
        assertThat(p).notMatches("audit policy p, p2")
        assertThat(p).notMatches("audit policy p whenever successful by hr")
        assertThat(p).notMatches("audit policy p by hr except sh")
        assertThat(p).notMatches("audit context namespace userenv attributes current_user whenever successful")
        assertThat(p).notMatches("audit context namespace userenv attributes a by users with granted roles dba")
        assertThat(p).notMatches("audit context namespace userenv")

        setRootRule(DdlGrammar.NOAUDIT_STATEMENT)
        // ORA-00956 without an option; ORA-01708/ORA-01718 for BY after ON object.
        assertThat(p).notMatches("noaudit")
        assertThat(p).notMatches("noaudit select on hr.employees by hr")
    }

    private val fixtureStatements = listOf(
        "AUDIT ROLE;", "AUDIT ROLE WHENEVER SUCCESSFUL;", "AUDIT ROLE WHENEVER NOT SUCCESSFUL;", "AUDIT SELECT TABLE, UPDATE TABLE;",
        "AUDIT SELECT TABLE, UPDATE TABLE BY hr, oe;", "AUDIT DELETE ANY TABLE;", "AUDIT CREATE ANY DIRECTORY;", "AUDIT DIRECTORY;",
        "AUDIT READ ON DIRECTORY bfile_dir;", "AUDIT SELECT ON hr.employees;", "AUDIT SELECT ON hr.employees WHENEVER SUCCESSFUL;",
        "AUDIT SELECT ON hr.employees WHENEVER NOT SUCCESSFUL;", "AUDIT INSERT, UPDATE ON oe.customers;", "AUDIT ALL ON hr.employees_seq;",
        "AUDIT ALTER, GRANT, INSERT, UPDATE, DELETE ON DEFAULT;",
    )

    @Test
    fun matchesTraditionalAudit() {
        setRootRule(DdlGrammar.AUDIT_STATEMENT)
        fixtureStatements.forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "audit all statements", "audit all privileges by hr", "audit direct_path load by hr", "audit network",
            "audit role by hr, oe", "audit role container = current", "audit role whenever successful container = all",
            "audit select table by hr whenever successful", "audit select on mining model hr.m", "audit use on sql translation profile hr.p",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "audit", "audit select on hr.employees by hr", "audit role,",
            "audit select on", "audit select table by", "audit select table whenever",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun keepsMalformedUnifiedAuditFromFallingBackToTraditional() {
        setRootRule(DdlGrammar.AUDIT_STATEMENT)
        listOf(
            "audit policy", "audit policy p by", "audit policy p except", "audit policy s.p", "audit policy p, p2",
            "audit policy p whenever successful by hr", "audit context", "audit context namespace ns", "audit context namespace userenv attributes",
            "audit context namespace userenv attributes a whenever successful", "audit context by hr",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
        setRootRule(DdlGrammar.NOAUDIT_STATEMENT)
        listOf("noaudit policy p by", "noaudit policy p except").forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun distinguishesUnifiedAndTraditionalAuditNodes() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val unified = p.parse("audit policy p by hr; audit context namespace userenv attributes a;")
        assertThatAst(unified.getDescendants(DdlGrammar.AUDIT_POLICY_CLAUSE)).hasSize(1)
        assertThatAst(unified.getDescendants(DdlGrammar.AUDIT_CONTEXT_CLAUSE)).hasSize(1)
        assertThatAst(unified.getDescendants(DdlGrammar.AUDIT_STATEMENT)).hasSize(2)
        val traditional = p.parse("audit select table, update table by hr, oe; audit select on hr.employees;")
        assertThatAst(traditional.getDescendants(DdlGrammar.AUDIT_STATEMENT)).hasSize(2)
        assertThatAst(traditional.getDescendants(DdlGrammar.AUDIT_POLICY_CLAUSE)).isEmpty()
        assertThatAst(traditional.getDescendants(DdlGrammar.AUDIT_CONTEXT_CLAUSE)).isEmpty()
        val first = traditional.getFirstDescendant(DdlGrammar.AUDIT_STATEMENT)
        assertThatAst(first.tokens.map { it.originalValue }).containsExactly(
            "audit", "select", "table", ",", "update", "table", "by", "hr", ",", "oe", ";")
        val noaudit = p.parse("noaudit select table, update table by hr, oe;")
        assertThatAst(noaudit.getFirstDescendant(DdlGrammar.NOAUDIT_STATEMENT).children.map { it.tokenOriginalValue }).containsExactly(
            "noaudit", "select", "table", ",", "update", "table", "by", "hr", ",", "oe", ";")
        assertThatAst(noaudit.getDescendants(DdlGrammar.TRADITIONAL_AUDIT_CLAUSE)).isEmpty()
        assertThatAst(traditional.getDescendants(DdlGrammar.TRADITIONAL_AUDIT_CLAUSE)).isEmpty()
        assertThatAst(first.children.map { it.tokenOriginalValue }).contains("audit", "select", "table", "by")
        val whole = p.parse(fixtureStatements.joinToString("\n"))
        assertThatAst(whole.getDescendants(DdlGrammar.AUDIT_STATEMENT)).hasSize(15)
    }

    @Test
    fun keepsPolicyAndContextUsableAsOrdinaryNames() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).matches("create table policy (context number); select policy.context from policy;")
    }
}
