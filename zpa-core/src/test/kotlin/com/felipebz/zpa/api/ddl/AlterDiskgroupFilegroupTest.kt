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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class AlterDiskgroupFilegroupTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_DISKGROUP)
    }

    private fun matches(vararg tails: String) {
        for (tail in tails) assertThat(p).describedAs(tail).matches("alter diskgroup $tail")
    }

    private fun notMatches(vararg tails: String) {
        for (tail in tails) assertThat(p).describedAs(tail).notMatches("alter diskgroup $tail")
    }

    @Test
    fun matchesAddFilegroupClients() {
        matches(
            "dg add filegroup fg template", "dg add filegroup fg database db", "dg add filegroup fg database none",
            "dg add filegroup fg database 'db'", "dg add filegroup fg cluster c", "dg add filegroup fg cluster none",
            "dg add filegroup fg volume v", "dg add filegroup \"Fg\" template", "dg add filegroup fg template;"
        )
    }

    @Test
    fun matchesAddFilegroupTemplateAndProperty() {
        for (client in listOf("template", "database db", "database none", "cluster c", "volume v")) {
            matches(
                "dg add filegroup fg $client from template t",
                "dg add filegroup fg $client set 'a.b'='c'",
                "dg add filegroup fg $client from template t set 'a.b' = 'c'"
            )
        }
        matches(
            "hmdg add filegroup fgtem template set 'datafile.redundancy'='unprotected'",
            "hmdg add filegroup fgdb database none from template fgtem",
            "hmdg add filegroup fgtem2 template from template fgtem",
            "dg add filegroup fg template set 'p'='v'"
        )
    }

    @Test
    fun rejectsMalformedAddFilegroup() {
        notMatches(
            "dg add filegroup fg", "dg add filegroup fg from template t", "dg add filegroup fg set 'a.b'='c'",
            "dg add filegroup template", "dg add filegroup fg template template t", "dg add filegroup fg template from t",
            "dg add filegroup fg template from template", "dg add filegroup fg template set 'a.b'",
            "dg add filegroup fg template set 'a.b'=", "dg add filegroup fg template set a.b='c'",
            "dg add filegroup fg template set 'a'=c", "dg add filegroup fg template set 'a'='c' from template t",
            "dg add filegroup fg database db template", "dg add filegroup fg database a.b",
            "dg add filegroup fg database db cluster c", "dg add filegroup fg1, fg2 template",
            "dg add filegroup a.b template", "dg add filegroup fg template from template a.b",
            "dg add filegroup fg template, database db", "dg add filegroup fg database", "dg add filegroup fg volume",
            "dg add filegroup fg volume 'v'", "dg add filegroup fg template set 'a'='b',",
            "dg add filegroup fg template set 'a'='b' 'c'='d'", "dg add filegroup fg template set 'a'='b', 'c'",
            "dg add filegroup fg template x", "dg add filegroup fg cascade"
        )
    }

    @Test
    fun matchesModifyFilegroup() {
        matches(
            "dg modify filegroup fg set 'a.b'='c'", "dg modify filegroup fg set 'p'='v'",
            "dg modify filegroup \"Fg\" set 'a.b' = 'c';"
        )
        notMatches(
            "dg modify filegroup fg", "dg modify filegroup fg set", "dg modify filegroup fg template set 'a.b'='c'",
            "dg modify filegroup fg from template t", "dg modify filegroup set 'a.b'='c'",
            "dg modify filegroup fg set 'a.b'", "dg modify filegroup fg set a.b='c'",
            "dg modify filegroup fg set 'a.b'='c' 'd'='e'", "dg modify filegroup a.b set 'a.b'='c'",
            "dg modify filegroup fg set 'a.b'='c',"
        )
    }

    @Test
    fun matchesMoveToFilegroup() {
        matches(
            "dg move file '+dg/f' to filegroup fg", "dg move file '+dg/f' to filegroup \"Fg\";"
        )
        notMatches(
            "dg move file '+dg/f', '+dg/g' to filegroup fg", "dg move file '+dg/f' to filegroup",
            "dg move file '+dg/f' to fg", "dg move file '+dg/f' filegroup fg", "dg move file f to filegroup fg",
            "dg move files '+dg/f' to filegroup fg", "dg move '+dg/f' to filegroup fg",
            "dg move file '+dg/f' to filegroup fg cascade", "dg move file '+dg/f' to filegroup fg1, fg2",
            "dg move file '+dg/f' to filegroup fg power 5", "dg move file to filegroup fg"
        )
    }

    @Test
    fun matchesDropFilegroup() {
        matches(
            "dg drop filegroup fg", "dg drop filegroup fg cascade", "dg drop filegroup fg for database db",
            "dg drop filegroup fg cascade for database db",
            "dg drop filegroup fg for pluggable database \"pdb\" database db",
            "dg drop filegroup fg cascade for pluggable database pdb database db", "dg drop filegroup \"Fg\";",
            "dg drop filegroup fg for pluggable database \"PDB\" database db",
            "dg drop filegroup fg for pluggable database \"a.b\" database db",
            "dg drop filegroup fg for database \"db\""
        )
        notMatches(
            "dg drop filegroup", "dg drop filegroup fg for", "dg drop filegroup fg for database",
            "dg drop filegroup fg for database 'db'", "dg drop filegroup fg for pluggable database 'pdb' database db",
            "dg drop filegroup fg for pluggable database \"pdb\"", "dg drop filegroup fg for pluggable database",
            "dg drop filegroup fg for database db cascade", "dg drop filegroup fg cascade cascade",
            "dg drop filegroup fg for database db for database db2", "dg drop filegroup fg1, fg2",
            "dg drop filegroup fg force", "dg drop filegroup fg including contents",
            "dg drop filegroup fg for pluggable database pdb"
        )
    }

    @Test
    fun matchesCommaSeparatedPropertyLists() {
        matches(
            "dg add filegroup fg template set 'a.b'='c', 'd'='e'",
            "dg add filegroup fg database db from template t set 'a'='b', 'c'='d', 'e'='f'",
            "dg modify filegroup fg set 'a.b'='c', 'd'='e'"
        )
    }

    @Test
    fun matchesActionRunsWithoutCommas() {
        matches(
            "dg add filegroup f template add filegroup g database d",
            "dg modify filegroup f set 'a'='b' modify filegroup g set 'c'='d'",
            "dg move file '+a' to filegroup f move file '+b' to filegroup g",
            "dg move file '+a' to filegroup f add filegroup g template drop filegroup h cascade",
            "dg drop filegroup f drop filegroup g", "dg drop filegroup f cascade drop filegroup g for database d",
            "dg add filegroup f template add disk '/d1'", "dg add disk '/d1' add filegroup f template",
            "dg add disk '/d1' drop filegroup f", "dg drop disk d1 drop filegroup g",
            "dg drop disk d1, drop filegroup g", "dg drop filegroup g drop disk d1",
            "dg drop filegroup g add disk '/d1' drop disk d2 add filegroup f template",
            "dg add filegroup f template add disk '/d1' add filegroup g template add disk '/d2'",
            "dg add filegroup f template set 'a'='b', 'c'='d' add disk '/d1', '/d2'"
        )
    }

    @Test
    fun rejectsCommasBetweenActionsAndMalformedRuns() {
        notMatches(
            "dg add filegroup f template, add filegroup g database d",
            "dg add filegroup f template, add disk '/d1'", "dg drop filegroup f, drop filegroup g",
            "dg drop filegroup f cascade, drop filegroup g", "dg drop filegroup f for database d, drop filegroup g",
            "dg drop filegroup g, drop disk d1", "dg drop filegroup g, add disk '/d1'",
            "dg move file '+a' to filegroup f, move file '+b' to filegroup g",
            "dg modify filegroup f set 'a'='b', modify filegroup g set 'c'='d'",
            "dg add filegroup f template add", "dg add filegroup f template add filegroup",
            "dg add filegroup f template drop filegroup", "dg add filegroup f template x",
            "dg add filegroup f template add disk"
        )
    }

    @Test
    fun keepsOtherClausesSeparate() {
        matches("dg add disk '/d1'", "dg drop disk d1", "dg add template t attributes (mirror)", "dg drop template t",
            "dg add disk '/d1' rebalance power 2")
        notMatches(
            "dg add filegroup fg template rebalance", "dg add filegroup fg template resize all",
            "dg add filegroup fg template check all", "dg add filegroup fg template scrub",
            "dg add filegroup fg template mount", "dg add filegroup fg template undrop disks",
            "dg add filegroup fg template add template t attributes (mirror)",
            "dg add filegroup fg template add directory '+dg/d'", "dg add filegroup fg template add volume v size 1g",
            "dg add template t attributes (mirror) add filegroup fg template",
            "dg drop filegroup f rebalance", "dg move file 'f' to filegroup fg rebalance",
            "dg filegroup fg", "dg add filegroups fg template", "dg add volume v1 size 1g"
        )
    }

    @Test
    fun buildsOneAstNodePerFilegroupAction() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse(
            "alter diskgroup dg add filegroup fg database db from template t set 'a.b'='c';\n" +
                "alter diskgroup dg modify filegroup fg set 'a.b'='c';\n" +
                "alter diskgroup dg move file '+dg/f' to filegroup fg;\n" +
                "alter diskgroup dg drop filegroup fg cascade for database db;\n" +
                "alter diskgroup dg drop disk d1;\n")
        assertThatAst(tree.getDescendants(DdlGrammar.ALTER_DISKGROUP)).hasSize(5)
        assertThatAst(tree.getDescendants(DdlGrammar.DISKGROUP_FILEGROUP_CLAUSE)).hasSize(4)
        assertThatAst(tree.getDescendants(DdlGrammar.ADD_FILEGROUP_CLAUSE)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.MODIFY_FILEGROUP_CLAUSE)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.MOVE_TO_FILEGROUP_CLAUSE)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.DROP_FILEGROUP_CLAUSE)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.FILEGROUP_PROPERTY)).hasSize(2)
        assertThatAst(tree.getDescendants(DdlGrammar.DROP_DISK_CLAUSE)).hasSize(1)
    }
}
