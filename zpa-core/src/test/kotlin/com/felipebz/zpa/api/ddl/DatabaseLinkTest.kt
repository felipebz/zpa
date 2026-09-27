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

import com.felipebz.flr.api.RecognitionException
import com.felipebz.flr.grammar.GrammarRuleKey
import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class DatabaseLinkTest : RuleTest() {

    @Test
    fun matchesCreateDatabaseLink() {
        setRootRule(DdlGrammar.CREATE_DATABASE_LINK)
        assertThat(p).matches("create public database link remote using 'remote';")
        assertThat(p).matches("create database link local connect to hr identified by password using 'local';")
        assertThat(p).matches("create database link remote.us.example.com connect to current_user using 'remote';")
        assertThat(p).matches("create database link \"LINK\" connect to \"DBLINK\" identified by values ':1' using 'local';")
        assertThat(p).matches("create database link if not exists l.x@conn connect with hr.cred")
        assertThat(p).matches("create database link l")
        assertThat(p).matches("create shared public database link l connect to u identified by p " +
            "authenticated by a identified by b using 'x'")
        assertThat(p).matches("create shared database link l connect to current_user authenticated with credential")
    }

    @Test
    fun rejectsCreateFormsOracleRejects() {
        setRootRule(DdlGrammar.CREATE_DATABASE_LINK)
        // ORA-00922 / ORA-00905: AUTHENTICATED is required for SHARED links and rejected otherwise.
        assertThat(p).notMatches("create shared database link l using 'x'")
        assertThat(p).notMatches("create database link l authenticated by a identified by b using 'x'")
        // ORA-00901 / ORA-00922: header order, no OR REPLACE.
        assertThat(p).notMatches("create public shared database link l using 'x'")
        assertThat(p).notMatches("create or replace database link l using 'x'")
        // ORA-00954 / ORA-00988 / ORA-02010: IDENTIFIED BY needs an identifier, USING a literal.
        assertThat(p).notMatches("create database link l connect to u using 'x'")
        assertThat(p).notMatches("create database link l connect to u identified by 'p'")
        assertThat(p).notMatches("create database link l using x")
        // ORA-03048: one CONNECT, before AUTHENTICATED, and USING last.
        assertThat(p).notMatches("create database link l connect to u identified by p connect to v identified by q")
        assertThat(p).notMatches("create shared database link l authenticated by a identified by b connect to u identified by p")
        assertThat(p).notMatches("create database link l using 'x' connect to current_user")
    }

    @Test
    fun matchesAlterDatabaseLink() {
        setRootRule(DdlGrammar.ALTER_DATABASE_LINK)
        assertThat(p).matches("alter database link private_link connect to hr identified by hr_new_password;")
        assertThat(p).matches("alter public database link public_link connect to scott identified by scott_new_password;")
        assertThat(p).matches("alter shared public database link shared_pub_link connect to scott identified by s " +
            "authenticated by hr identified by h;")
        assertThat(p).matches("alter shared database link l connect to scott identified by s;")
        assertThat(p).matches("alter shared database link l authenticated with credential")
        assertThat(p).matches("alter database link if exists l.us.example.com@c connect with cred using 'x'")
        assertThat(p).matches("alter database link l using 'x'")
        // ORA-03048 / ORA-00987 / ORA-03049.
        assertThat(p).notMatches("alter database link l")
        assertThat(p).notMatches("alter database link l connect to current_user")
        assertThat(p).notMatches("alter database link l authenticated by a identified by b")
        assertThat(p).notMatches("alter shared database link l authenticated by a identified by b connect to u identified by p")
        assertThat(p).notMatches("alter public shared database link l connect to u identified by p")
    }

    @Test
    fun doesNotConsumeDatabaseStatementPrefixes() {
        // Unrelated CREATE/ALTER DATABASE statements must fail at their first token, not after `DATABASE`.
        assertFailsAtStart(DdlGrammar.CREATE_DATABASE_LINK, "create database testdb\n  datafile 'a.dbf' size 1m")
        assertFailsAtStart(DdlGrammar.CREATE_DATABASE_LINK, "create public database\n  x")
        assertFailsAtStart(DdlGrammar.ALTER_DATABASE_LINK, "alter database\n  recover tablespace tbs_03 parallel")
        assertFailsAtStart(DdlGrammar.ALTER_DATABASE_LINK, "alter shared database\n  x")
    }

    private fun assertFailsAtStart(rule: GrammarRuleKey, source: String) {
        setRootRule(rule)
        assertThatThrownBy { p.parse(source) }
            .isInstanceOfSatisfying(RecognitionException::class.java) { e ->
                org.assertj.core.api.Assertions.assertThat(e.line to e.column).isEqualTo(1 to 0)
            }
    }
}
