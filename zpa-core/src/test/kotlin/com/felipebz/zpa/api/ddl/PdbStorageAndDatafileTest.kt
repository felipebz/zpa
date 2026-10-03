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

class PdbStorageAndDatafileTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_PLUGGABLE_DATABASE)
    }

    private fun matches(vararg tails: String) {
        for (tail in tails) assertThat(p).describedAs(tail).matches("alter pluggable database $tail")
    }

    private fun notMatches(vararg tails: String) {
        for (tail in tails) assertThat(p).describedAs(tail).notMatches("alter pluggable database $tail")
    }

    @Test
    fun matchesStorage() {
        matches(
            "storage unlimited", "storage (maxsize 500m)", "storage (maxsize unlimited)",
            "storage (max_audit_size 1g max_diag_size 1g maxsize 2g)", "storage (maxsize 1g maxsize 2g)", "STORAGE UNLIMITED;",
            "pdb1 storage unlimited", "pdb1 storage (maxsize 500m);", "\"Pdb1\" storage unlimited", "storage storage unlimited"
        )
    }

    @Test
    fun rejectsMalformedStorage() {
        notMatches(
            "storage", "storage ()", "storage (unlimited)", "storage (maxsize)", "storage maxsize 2g",
            "storage (maxsize 2g, max_audit_size 1g)", "storage unlimited storage unlimited", "storage unlimited x",
            "pdb1, pdb2 storage unlimited", "all storage unlimited", "pdb1 pdb2 storage unlimited"
        )
    }

    @Test
    fun matchesDatafileTargets() {
        matches(
            "datafile 'f' online", "datafile 'f' offline", "datafile 'f', 'g' online", "datafile 3 online", "datafile 3, 4 offline",
            "datafile 'f', 3 online", "datafile 3, 'f' offline", "datafile all online", "datafile all offline",
            "DATAFILE 'f' ONLINE;"
        )
    }

    @Test
    fun matchesPdbTargets() {
        matches(
            "pdb1 datafile 'f' online", "pdb1 datafile all offline;", "\"Pdb1\" datafile 3 online", "datafile datafile 'f' online",
            "online datafile all online"
        )
    }

    @Test
    fun rejectsMalformedDatafile() {
        notMatches(
            "datafile online", "datafile 'f'", "datafile all", "datafile 'f' 'g' online", "datafile all, 'f' online",
            "datafile 'f', all online", "datafile all except 'f' online", "datafile all 'f' online", "datafile 'f' online offline",
            "datafile 'f' online online", "datafile online 'f'", "datafile f online", "datafile \"f\" online", "datafile -1 online",
            "datafile 'f', online", "datafile , 'f' online", "datafile 'f' online datafile 'g' offline",
            "pdb1, pdb2 datafile 'f' online", "all datafile 'f' online", "pdb1 pdb2 datafile 'f' online"
        )
    }

    @Test
    fun keepsAlterDatabaseOnlyDatafileOptionsOut() {
        notMatches(
            "datafile 'f' offline for drop", "datafile 'f' resize 1m", "datafile 'f' autoextend on", "datafile 'f' end backup",
            "datafile 'f' encrypt", "storage unlimited datafile 'f' online", "pdb1 open datafile 'f' online"
        )
    }

    @Test
    fun buildsDedicatedNodes() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse(
            "alter pluggable database pdb2 storage (maxsize 500m);\n" +
                "alter pluggable database pdb3 datafile all offline;\n" +
                "alter pluggable database datafile 'f', 3 online;\n" +
                "alter pluggable database pdb1 open;\n")
        assertThatAst(tree.getDescendants(DdlGrammar.ALTER_PLUGGABLE_DATABASE)).hasSize(4)
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_STORAGE_CLAUSE)).hasSize(1)
        val clauses = tree.getDescendants(DdlGrammar.PDB_DATAFILE_CLAUSE)
        assertThatAst(clauses).hasSize(2)
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_CHANGE_STATE)).hasSize(1)
        assertThatAst(clauses.map { c -> c.children.map { it.tokenOriginalValue.lowercase() } }).containsExactly(
            listOf("datafile", "all", "offline"),
            listOf("datafile", "'f'", ",", "3", "online"))
    }
}
