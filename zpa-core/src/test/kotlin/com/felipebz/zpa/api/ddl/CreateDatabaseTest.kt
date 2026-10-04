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

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateDatabaseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_DATABASE)
    }

    private fun AstNode.texts() = tokens.map { it.originalValue }

    @Test
    fun matchesTheDocumentedStatements() {
        listOf(
            """create database sample controlfile reuse logfile
                 group 1 ('diskx:log1.log', 'disky:log1.log') size 50k, group 2 ('diskx:log2.log', 'disky:log2.log') size 50k
               maxlogfiles 5 maxloghistory 100 maxdatafiles 10 maxinstances 2 archivelog character set al32utf8
               national character set al16utf16 datafile 'disk1:df1.dbf' autoextend on,
                 'disk2:df2.dbf' autoextend on next 10m maxsize unlimited
               default temporary tablespace temp_ts undo tablespace undo_ts set time_zone = '+02:00';""",
            """create database newcdb user sys identified by sys_password user system identified by system_password
               logfile group 1 ('/u01/a.log','/u02/b.log') size 100m blocksize 512, group 2 ('/u01/c.log','/u02/d.log') size 100m blocksize 512
               maxloghistory 1 maxlogfiles 16 maxlogmembers 3 maxdatafiles 1024 character set al32utf8
               national character set al16utf16 extent management local
               datafile '/u01/system01.dbf' size 700m reuse autoextend on next 10240k maxsize unlimited
               sysaux datafile '/u01/sysaux01.dbf' size 550m reuse autoextend on next 10240k maxsize unlimited
               default tablespace deftbs datafile '/u01/deftbs01.dbf' size 500m reuse autoextend on maxsize unlimited
               default temporary tablespace tempts1 tempfile '/u01/temp01.dbf' size 20m reuse autoextend on next 640k maxsize unlimited
               undo tablespace undotbs1 datafile '/u01/undotbs01.dbf' size 200m reuse autoextend on next 5120k maxsize unlimited
               enable pluggable database seed file_name_convert = ('/u01/newcdb/', '/u01/pdbseed/')
               system datafiles size 125m autoextend on next 10m maxsize unlimited sysaux datafiles size 100m
               user_data tablespace usertbs datafile '/u01/usertbs01.dbf' size 200m reuse autoextend on maxsize unlimited;""",
            """create database payable logfile group 1 ('diska:log1.log', 'diskb:log1.log') size 50k,
                 group 2 ('diska:log2.log', 'diskb:log2.log') size 50k datafile 'diskc:dbone.dbf' size 30m;""",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun matchesClausesInAnyOrderAndOptionalName() {
        listOf(
            "create database db maxdatafiles 5", "create database user sys identified by x",
            "create database db user system identified by x user sys identified by y", "create database db controlfile reuse",
            "create database db set default bigfile tablespace", "create database db set default smallfile tablespace",
            "create database db character set utf8 national character set utf8", "create database db noarchivelog",
            "create database db force logging", "create database db set standby nologging for data availability",
            "create database db set standby nologging for load performance", "create database db extent management local",
            "create database db logfile 'a.log' size 1m", "create database db logfile ('a', 'b')", "create database db logfile group 1 size 1m",
            "create database db logfile 'a' reuse, 'b' reuse", "create database db datafile 'a', 'b' size 1m",
            "create database db sysaux datafile 'a' size 1m", "create database db default tablespace t",
            "create database db default tablespace t datafile 'a' size 1m extent management local autoallocate",
            "create database db default tablespace t extent management local uniform size 1m",
            "create database db default temporary tablespace t", "create database db bigfile default temporary tablespace t tempfile 'a', 'b'",
            "create database db smallfile default local temporary tablespace for all t", "create database db default local temporary tablespace for leaf t",
            "create database db undo tablespace u", "create database db bigfile undo tablespace u datafile 'a'",
            "create database db set time_zone = 'UTC'", "create database db user_data tablespace t datafile 'a'",
            "create database db bigfile user_data tablespace t datafile 'a' size 1m",
            "create database db enable pluggable database", "create database db enable pluggable database seed",
            "create database db enable pluggable database seed file_name_convert = none",
            "create database db enable pluggable database seed system datafiles autoextend on",
            "create database db enable pluggable database local undo on", "create database db enable pluggable database local undo off",
            "create database db maxdatafiles 5 maxdatafiles 6", "create database \"DB\" maxinstances 1;",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun rejectsMalformedClauses() {
        listOf(
            "create database", "create database db", "create database db bogus", "create database db maxdatafiles", "create database db maxdatafiles x", "create database db user sys",
            "create database db user other identified by x", "create database db character utf8", "create database db character set",
            "create database db national set utf8", "create database db controlfile", "create database db set default tablespace",
            "create database db force", "create database db set standby nologging for data", "create database db extent management",
            "create database db extent management dictionary", "create database db logfile", "create database db logfile group size 1m",
            "create database db logfile 'a',", "create database db logfile 'a', maxlogfiles 5", "create database db datafile",
            "create database db datafile 'a',", "create database db sysaux 'a'", "create database db default",
            "create database db default tablespace", "create database db default temporary t", "create database db undo t",
            "create database db set time_zone 'UTC'", "create database db set time_zone = UTC", "create database db user_data tablespace t",
            "create database db user_data tablespace t datafile", "create database db enable database",
            "create database db enable pluggable database seed system size 1m", "create database db enable pluggable database local undo",
            "create database db local temporary tablespace t", "create database db datafile 'a' maxlogfiles",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun buildsStructureAndReusesExistingFileSpecifications() {
        val node = p.parse(
            "create database db logfile group 1 ('a', 'b') size 1m blocksize 512, 'c' reuse " +
                "datafile 'd' size 2m autoextend on next 1m, 'e' enable pluggable database seed file_name_convert = ('x', 'y');")
        assertThatAst(node.getChildren(DdlGrammar.REDO_LOG_FILE_SPEC).map { it.texts() }).containsExactly(
            listOf("(", "'a'", ",", "'b'", ")", "size", "1", "m", "blocksize", "512"), listOf("'c'", "reuse"))
        assertThatAst(node.getChildren(DdlGrammar.DATAFILE_TEMPFILE_SPEC).map { it.texts() }).containsExactly(
            listOf("'d'", "size", "2", "m", "autoextend", "on", "next", "1", "m"), listOf("'e'"))
        assertThatAst(node.getDescendants(DdlGrammar.AUTOEXTEND_CLAUSE)).hasSize(1)
        assertThatAst(node.getFirstDescendant(DdlGrammar.PDB_FILE_NAME_CONVERT).texts())
            .containsExactly("file_name_convert", "=", "(", "'x'", ",", "'y'", ")")
        assertThatAst(node.texts().take(3)).containsExactly("create", "database", "db")
        assertThatAst(p.parse("create database user sys identified by x").texts().take(3)).containsExactly("create", "database", "user")
    }

    @Test
    fun keepsOtherDatabaseStatementsAndDispatchUnchanged() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val create = p.parse("create database db maxdatafiles 5;")
        assertThatAst(create.getDescendants(DdlGrammar.CREATE_DATABASE)).hasSize(1)
        listOf(
            "create database link l connect to u identified by p using 'x';", "create public database link l using 'x';",
            "create pluggable database p from seed;", "create pluggable database p admin user a identified by b;",
            "alter database add logfile group 3 ('a') size 1m;", "alter database datafile 'a' autoextend on;",
            "alter database create datafile 'a' as new;", "create diskgroup dg external redundancy disk '/a';",
        ).forEach {
            val tree = p.parse(it)
            assertThatAst(tree.getDescendants(DdlGrammar.CREATE_DATABASE)).describedAs(it).isEmpty()
        }
        assertThatAst(p.parse("create database link l connect to u identified by p using 'x';")
            .getDescendants(DdlGrammar.CREATE_DATABASE_LINK)).hasSize(1)
        assertThatAst(p.parse("create pluggable database p from seed;").getDescendants(DdlGrammar.CREATE_PLUGGABLE_DATABASE)).hasSize(1)
        assertThatAst(p.parse("alter database add logfile group 3 ('a') size 1m;").getDescendants(DdlGrammar.ALTER_DATABASE)).hasSize(1)
    }
}
