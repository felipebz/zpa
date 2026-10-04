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
 */package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateControlfileTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_CONTROLFILE)
    }

    @Test
    fun matchesCreateControlfile() {
        listOf(
            """create controlfile reuse database "demo" noresetlogs noarchivelog maxlogfiles 32 maxlogmembers 2 maxdatafiles 32
               maxinstances 1 maxloghistory 449 logfile group 1 '/p/t_log1.f' size 500k, group 2 '/p/t_log2.f' size 500k
               datafile '/p/t_db1.f', '/p/dbu19i.dbf', '/p/demo.dbf' character set we8dec;""",
            """create controlfile database "demo" logfile group 1 '/p/t_log1.f' size 500k, group 2 '/p/t_log2.f' size 500k
               resetlogs datafile '/p/file' force logging archivelog character set we8dec""",
            "create controlfile database d resetlogs", "create controlfile reuse database d noresetlogs", "create controlfile set database d resetlogs",
            "create controlfile reuse set database d resetlogs", "create controlfile database d logfile 'a' resetlogs datafile 'b'",
            "create controlfile database d resetlogs maxlogfiles 1 maxlogmembers 2 maxloghistory 3 maxdatafiles 4 maxinstances 5",
            "create controlfile database d noresetlogs noarchivelog force logging set standby nologging for load performance character set utf8;",
            "create controlfile database d resetlogs datafile 'a' size 1m reuse autoextend on, 'b'",
            "create controlfile database d logfile 'a' resetlogs datafile 'b' maxlogfiles 1 force logging character set utf8",
            "create controlfile database d noresetlogs archivelog maxdatafiles 2 logfile 'a' datafile 'b' character set utf8",
            "create controlfile database d resetlogs maxlogfiles 1 logfile 'a'", "create controlfile database d resetlogs character set utf8",
            "create controlfile database d resetlogs logfile 'a' datafile 'b'",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "create controlfile", "create controlfile database", "create controlfile database d", "create controlfile reuse d resetlogs",
            "create controlfile database d resetlogs noresetlogs", "create controlfile database d resetlogs resetlogs",
            "create controlfile database d noresetlogs resetlogs", "create controlfile database d datafile 'a' resetlogs",
            "create controlfile database d maxlogfiles 1 resetlogs", "create controlfile database d resetlogs maxlogfiles",
            "create controlfile database d resetlogs logfile", "create controlfile database d resetlogs datafile",
            "create controlfile database d resetlogs datafile 'a',", "create controlfile database d resetlogs character set",
            "create controlfile database d resetlogs bogus", "create controlfile reuse reuse database d resetlogs",
            "create controlfile database d resetlogs user sys identified by x",
            "create controlfile database d resetlogs datafile 'a' datafile 'b'",
            "create controlfile database d resetlogs logfile 'a' logfile 'b'",
            "create controlfile database d resetlogs character set utf8 character set utf8",
            "create controlfile database d resetlogs character set utf8 maxlogfiles 10",
            "create controlfile database d logfile 'a' resetlogs logfile 'b'",
            "create controlfile database d logfile 'a' resetlogs maxlogfiles 1 datafile 'b'",
            "create controlfile database d resetlogs maxlogfiles 1 datafile 'b' logfile 'a'",
            "create controlfile database d resetlogs datafile 'b' maxlogfiles 1 logfile 'a'",
            "create controlfile database d resetlogs datafile 'a' character set utf8 datafile 'b'",
            "create controlfile database d logfile 'a' resetlogs maxlogfiles 1 logfile 'b'",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun dispatchesToCreateControlfileOnly() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val controlfile = p.parse("create controlfile database d resetlogs;")
        assertThatAst(controlfile.getDescendants(DdlGrammar.CREATE_CONTROLFILE)).hasSize(1)
        assertThatAst(controlfile.getDescendants(DdlGrammar.CREATE_DATABASE)).isEmpty()
        val database = p.parse("create database db maxdatafiles 5;")
        assertThatAst(database.getDescendants(DdlGrammar.CREATE_DATABASE)).hasSize(1)
        assertThatAst(database.getDescendants(DdlGrammar.CREATE_CONTROLFILE)).isEmpty()
    }
}
