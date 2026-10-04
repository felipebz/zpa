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
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FlashbackDatabaseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.FLASHBACK_DATABASE)
    }

    @Test
    fun matchesTargetForms() {
        listOf(
            "flashback database to scn 100", "flashback database to before scn 100", "flashback database to timestamp sysdate - 1;",
            "flashback database to before timestamp sysdate - 1", "flashback database to restore point rp1",
            "flashback database to before resetlogs", "flashback database to before restore point rp1",
            "flashback database to resetlogs", "flashback database to scn 100 + 1", "flashback database to scn :n",
            "flashback database to timestamp to_timestamp('2020-01-01', 'YYYY-MM-DD')", "flashback database to scn (select 1 from dual)",
            "flashback database to restore point \"rp 1\"",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun matchesStandbyPluggableAndNamedForms() {
        listOf(
            "flashback standby database to scn 100", "flashback pluggable database to scn 100",
            "flashback standby pluggable database to scn 100", "flashback database mydb to scn 100",
            "flashback standby pluggable database mydb to timestamp sysdate", "flashback database \"My Db\" to scn 1",
            "flashback pluggable database to restore point rp1",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun rejectsMalformedForms() {
        listOf(
            "flashback database", "flashback standby database", "flashback database to", "flashback database to scn",
            "flashback database to before", "flashback database to restore point", "flashback database to before restore point",
            "flashback database scn 100", "flashback database to scn 100 scn 100", "flashback database to before resetlogs scn 1",
            "flashback pluggable standby database to scn 100", "flashback database db1.db2 to scn 1",
            "flashback database to restore point s.rp", "flashback database to before scn",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun acceptsAlterDatabaseFlashbackSwitch() {
        setRootRule(DdlGrammar.ALTER_DATABASE)
        listOf("alter database flashback on", "alter database flashback off;", "alter database mydb flashback on")
            .forEach { assertThat(p).describedAs(it).matches(it) }
        listOf("alter database flashback", "alter database flashback on off", "alter database flashback database")
            .forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun buildsFlashbackDatabaseNode() {
        val node = p.parse("flashback standby pluggable database db1 to before timestamp sysdate - 1;")
        assertThatAst(node.tokens.map { it.originalValue }.take(7)).containsExactly(
            "flashback", "standby", "pluggable", "database", "db1", "to", "before")
        assertThatAst(node.getFirstChild(com.felipebz.zpa.api.PlSqlGrammar.ADDITIVE_EXPRESSION).tokens.map { it.originalValue })
            .containsExactly("sysdate", "-", "1")
    }
}
