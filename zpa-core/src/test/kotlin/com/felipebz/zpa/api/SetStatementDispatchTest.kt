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
package com.felipebz.zpa.api

import com.felipebz.flr.tests.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SetStatementDispatchTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
    }

    @Test
    fun parsesSetConstraintsThroughTheSqlRule() {
        listOf(
            "SET CONSTRAINTS ALL IMMEDIATE;",
            "SET CONSTRAINT emp_job_nn DEFERRED;",
            "SET CONSTRAINTS emp_job_nn, emp_salary_min,\n  hr.jhist_dept_fk@remote DEFERRED;",
            "SET CONSTRAINTS emp_job_nn, emp_salary_min, hr.jhist_dept_fk@remote DEFERRED;",
            "set constraints zz@remote.example.com immediate;",
            "set constraints \"Zz\", hr.\"Yy\" deferred;",
        ).forEach { source ->
            val node = p.parse(source)
            assertThatAst(node.getDescendants(SqlPlusGrammar.SQLPLUS_COMMAND)).describedAs(source).isEmpty()
            assertThatAst(node.getDescendants(TclGrammar.SET_CONSTRAINTS_EXPRESSION)).describedAs(source).hasSize(1)
        }
    }

    @Test
    fun keepsRemoteConstraintNameInsideTheStatement() {
        val node = p.parse("SET CONSTRAINTS emp_job_nn,\n  hr.jhist_dept_fk@remote DEFERRED;")
            .getFirstDescendant(TclGrammar.SET_CONSTRAINTS_EXPRESSION)
        assertThatAst(node.tokens.map { it.originalValue }).containsExactly(
            "SET", "CONSTRAINTS", "emp_job_nn", ",", "hr", ".", "jhist_dept_fk", "@", "remote", "DEFERRED")
    }

    @Test
    fun parsesSetRoleThroughTheSessionControlRule() {
        listOf("SET ROLE NONE;", "SET ROLE ALL;", "SET ROLE role1;", "set role all except r1, r2;", "set role r1 identified by pw;")
            .forEach { source ->
                val node = p.parse(source)
                assertThatAst(node.getDescendants(SqlPlusGrammar.SQLPLUS_COMMAND)).describedAs(source).isEmpty()
                assertThatAst(node.getDescendants(SessionControlGrammar.SET_ROLE)).describedAs(source).hasSize(1)
            }
        assertThatAst(p.parse("SET ROLE\n  NONE;").getDescendants(SessionControlGrammar.SET_ROLE)).hasSize(1)
    }

    @Test
    fun keepsSqlPlusSetOptionsAsSqlPlusCommands() {
        listOf(
            "SET SERVEROUTPUT ON", "set define off", "SET PAGESIZE 100", "SET LINESIZE 200", "set feedback off",
            "set sqlblanklines on", "SET TIMING ON",
        ).forEach { source ->
            val node = p.parse("$source\n")
            assertThatAst(node.getDescendants(SqlPlusGrammar.SQLPLUS_COMMAND)).describedAs(source).hasSize(1)
            assertThatAst(node.getDescendants(TclGrammar.SET_CONSTRAINTS_EXPRESSION)).describedAs(source).isEmpty()
            assertThatAst(node.getDescendants(SessionControlGrammar.SET_ROLE)).describedAs(source).isEmpty()
        }
        assertThatAst(p.parse("SET TRANSACTION READ ONLY;").getDescendants(TclGrammar.SET_TRANSACTION_EXPRESSION)).hasSize(1)
    }

    @Test
    fun rejectsMalformedSetConstraintsAndSetRoleInsteadOfFallingBackToSqlPlus() {
        listOf(
            "SET CONSTRAINTS zz;", "SET CONSTRAINTS deferred;", "SET CONSTRAINTS all, zz deferred;", "SET CONSTRAINTS a.b.c deferred;",
            "SET CONSTRAINTS zz@ deferred;", "SET CONSTRAINTS all immediate deferred;", "SET ROLE;",
        ).forEach { source -> assertThat(p).describedAs(source).notMatches(source) }
    }
}
