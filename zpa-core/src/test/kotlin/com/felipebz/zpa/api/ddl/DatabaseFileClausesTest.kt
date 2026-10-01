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
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class DatabaseFileClausesTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_DATABASE)
    }

    private fun matches(vararg tails: String) {
        for (tail in tails) {
            assertThat(p).describedAs(tail).matches("alter database $tail")
        }
    }

    private fun notMatches(vararg tails: String) {
        for (tail in tails) {
            assertThat(p).describedAs(tail).notMatches("alter database $tail")
        }
    }

    @Test
    fun matchesDatafileOperationsAndSelectors() {
        matches(
            "datafile 'a.dbf' online",
            "datafile 1 offline",
            "datafile 'a.dbf', 'b.dbf' offline for drop",
            "datafile 1, 2, 3 end backup",
            "datafile 'a.dbf' encrypt",
            "datafile 2 decrypt;",
            "datafile 'a.dbf', 2 online",
            "datafile 1, 'b.dbf' offline",
            "datafile 1, 'b.dbf', 3, 'd.dbf' offline for drop",
            "datafile 'a.dbf', 2 resize 10m",
            "datafile 1, 'b.dbf' autoextend on next 1m maxsize unlimited",
            "datafile 'a.dbf', 2 end backup",
            "datafile 1, 'b.dbf' encrypt",
            "datafile 'a.dbf', 2 decrypt"
        )
    }

    @Test
    fun matchesTempfileOperationsAndHomogeneousSelectors() {
        matches(
            "tempfile 'a.dbf' online",
            "tempfile 1 offline",
            "tempfile 'a.dbf', 'b.dbf' drop",
            "tempfile 1, 2 drop including datafiles;"
        )
    }

    @Test
    fun matchesSharedResizeSizeUnits() {
        for (kind in listOf("datafile", "tempfile")) {
            for (size in listOf("4096", "10k", "10 M", "10g", "10t", "10p", "10e", "10.0m", "1e1 M", "1.5m")) {
                matches("$kind 'a.dbf' resize $size")
            }
        }
    }

    @Test
    fun matchesNumericSelectors() {
        for (selector in listOf("17.0", "17e0", "17.1")) {
            matches(
                "datafile $selector online",
                "tempfile $selector offline",
                "move datafile $selector to 'b.dbf'"
            )
        }
    }

    @Test
    fun matchesAllAutoextendForms() {
        for (kind in listOf("datafile", "tempfile")) {
            matches(
                "$kind 1 autoextend off",
                "$kind 'a.dbf' autoextend on",
                "$kind 'a.dbf' autoextend on next 10k",
                "$kind 'a.dbf' autoextend on maxsize 100m",
                "$kind 'a.dbf' autoextend on maxsize unlimited",
                "$kind 'a.dbf' autoextend on next 10m maxsize 1g",
                "$kind 'a.dbf' autoextend on next 10m maxsize unlimited"
            )
        }
    }

    @Test
    fun rejectsMalformedDatafileAndTempfileSelectors() {
        for (kind in listOf("datafile", "tempfile")) {
            for (selector in listOf("", "\"a.dbf\"", "a", ":file", "1+1", "('a.dbf')", "()")) {
                notMatches("$kind $selector online")
            }
            notMatches(
                "$kind 'a.dbf', online",
                "$kind , 'a.dbf' online",
                "$kind 'a.dbf', , 'b.dbf' online",
                "$kind 1, online",
                "$kind 'a.dbf' 'b.dbf' online",
                "$kind 'a.dbf'",
                "$kind 1"
            )
        }
        notMatches(
            "datafile 1, , 'a.dbf' online",
            "datafile 'a.dbf', 2, online",
            "datafile , 1, 'a.dbf' online",
            "datafile 'a.dbf' 2 online"
        )
    }

    @Test
    fun rejectsMixedTempfileSelectors() {
        notMatches(
            "tempfile 'a.dbf', 2 online",
            "tempfile 1, 'a.dbf' offline",
            "tempfile 'a.dbf', 2, 'b.dbf' drop",
            "tempfile 1, 'a.dbf', 3 resize 10m"
        )
    }

    @Test
    fun rejectsIncompleteResizeAndAutoextendOperands() {
        for (kind in listOf("datafile", "tempfile")) {
            notMatches(
                "$kind 1 resize",
                "$kind 1 resize '10m'",
                "$kind 1 resize :size",
                "$kind 1 resize 1+1",
                "$kind 1 resize 10kb",
                "$kind 1 autoextend",
                "$kind 1 autoextend next 10m",
                "$kind 1 autoextend on next",
                "$kind 1 autoextend on maxsize",
                "$kind 1 autoextend on next unlimited",
                "$kind 1 autoextend on maxsize '10m'",
                "$kind 1 autoextend on maxsize :size"
            )
        }
    }

    @Test
    fun rejectsRepeatedReorderedAndTrailingOperations() {
        for (kind in listOf("datafile", "tempfile")) {
            notMatches(
                "$kind 1 online offline",
                "$kind 1 online online",
                "$kind 1 resize 10m resize 20m",
                "$kind 1 resize 10m autoextend on",
                "$kind 1 autoextend off next 10m",
                "$kind 1 autoextend off maxsize unlimited",
                "$kind 1 autoextend on maxsize 1g next 10m",
                "$kind 1 autoextend on next 10m next 20m",
                "$kind 1 autoextend on maxsize 1g maxsize unlimited",
                "$kind 1 autoextend on autoextend off",
                "$kind 1 online garbage"
            )
        }
    }

    @Test
    fun distinguishesDatafileAndTempfileOperationSets() {
        notMatches(
            "datafile 1 drop",
            "datafile 1 drop including datafiles",
            "datafile 1 offline for",
            "datafile 1 offline drop",
            "datafile 1 for drop offline",
            "datafile 1 online for drop",
            "datafile 1 end",
            "datafile 1 backup",
            "datafile 1 end backup encrypt",
            "datafile 1 encrypt decrypt",
            "tempfile 1 offline for drop",
            "tempfile 1 end backup",
            "tempfile 1 encrypt",
            "tempfile 1 decrypt",
            "tempfile 1 drop including",
            "tempfile 1 including datafiles drop",
            "tempfile 1 drop including datafiles including datafiles",
            "tempfile 1 drop online"
        )
    }

    @Test
    fun keepsLostWriteOutsideOrdinaryDatafileOperations() {
        for (operation in listOf("remove", "suspend", "enable")) {
            notMatches("tempfile 1 $operation lost write protection", "datafile 1 $operation lost write")
        }
        notMatches("datafile 1 online enable lost write protection", "datafile 1 resize 10m remove lost write protection")
    }

    @Test
    fun matchesMoveDatafileSelectorsAndOptions() {
        matches(
            "move datafile 'a.dbf'",
            "move datafile '+DATA/db/datafile/users.123.456'",
            "move datafile '+DATA.123.456'",
            "move datafile '+DATA'",
            "move datafile '+DATA/db/users.dbf'",
            "move datafile 1",
            "move datafile 'a.dbf' to 'b.dbf'",
            "move datafile 1 to '+DATA'",
            "move datafile '+DATA/db/datafile/users.123.456' to '+RECO/db/datafile/users.789.012'",
            "move datafile 1 reuse",
            "move datafile 1 keep",
            "move datafile 1 reuse keep",
            "move datafile 1 keep reuse",
            "move datafile 1 to 'b.dbf' reuse",
            "move datafile 1 to 'b.dbf' keep",
            "move datafile 1 to 'b.dbf' keep reuse",
            "move datafile 1 to 'b.dbf' reuse keep;"
        )
    }

    @Test
    fun rejectsMalformedMoveDatafileSelectorsAndDestinations() {
        for (selector in listOf("", "\"a.dbf\"", "a", ":file", "1+1", "('a.dbf')")) {
            notMatches("move datafile $selector to 'b.dbf'")
        }
        notMatches(
            "move tempfile 1 to 'b.dbf'",
            "move datafile 1, 2 to 'b.dbf'",
            "move datafile 'a.dbf', 'b.dbf' to 'c.dbf'",
            "move datafile +DATA to 'b.dbf'",
            "move datafile 1 to",
            "move datafile 1 to 2",
            "move datafile 1 to b",
            "move datafile 1 to \"b.dbf\"",
            "move datafile 1 to :file",
            "move datafile 1 to ('b.dbf')"
        )
    }

    @Test
    fun rejectsRepeatedAndReorderedMoveOptions() {
        notMatches(
            "move datafile 1 reuse reuse",
            "move datafile 1 keep keep",
            "move datafile 1 reuse to 'b.dbf'",
            "move datafile 1 keep to 'b.dbf'",
            "move datafile 1 to 'b.dbf' to 'c.dbf'",
            "move datafile 1 to 'b.dbf' reuse keep keep",
            "move datafile 1 online",
            "move datafile 1 to 'b.dbf' resize 10m"
        )
    }

    @Test
    fun preservesOperationSelectorAndSharedSizeBoundariesInAst() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("alter database datafile 'a.dbf', 'b.dbf' resize 10.0m; " +
            "alter database tempfile 3, 4 autoextend on next 20k maxsize 1g; " +
            "alter database move datafile 5 to '+DATA' reuse keep;")
        val clauses = tree.getDescendants(DdlGrammar.DATABASE_FILE_CLAUSES)
        assertThatAst(clauses.map { it.children.single().type }).containsExactly(
            DdlGrammar.ALTER_DATAFILE_CLAUSE,
            DdlGrammar.ALTER_TEMPFILE_CLAUSE,
            DdlGrammar.MOVE_DATAFILE_CLAUSE
        )
        val datafile = clauses[0].getDescendants(DdlGrammar.ALTER_DATAFILE_CLAUSE).single()
        assertThatAst(datafile.getChildren(PlSqlGrammar.CHARACTER_LITERAL).map { it.tokenValue })
            .containsExactly("'a.dbf'", "'b.dbf'")
        assertThatAst(datafile.getChildren(PlSqlTokenType.INTEGER_LITERAL)).isEmpty()
        assertThatAst(datafile.getDescendants(DdlGrammar.INDEX_SIZE_CLAUSE).map { it.tokenValue })
            .containsExactly("10.0")
        assertThatAst(datafile.getDescendants(DdlGrammar.AUTOEXTEND_CLAUSE)).isEmpty()
        val tempfile = clauses[1].getDescendants(DdlGrammar.ALTER_TEMPFILE_CLAUSE).single()
        assertThatAst(tempfile.getChildren(PlSqlTokenType.INTEGER_LITERAL).map { it.tokenValue })
            .containsExactly("3", "4")
        val autoextend = tempfile.getDescendants(DdlGrammar.AUTOEXTEND_CLAUSE).single()
        assertThatAst(autoextend.getDescendants(DdlGrammar.INDEX_SIZE_CLAUSE).map { it.tokenValue })
            .containsExactly("20", "1")
        val move = clauses[2].getDescendants(DdlGrammar.MOVE_DATAFILE_CLAUSE).single()
        assertThatAst(move.getChildren(PlSqlTokenType.INTEGER_LITERAL).map { it.tokenValue })
            .containsExactly("5")
        assertThatAst(move.getChildren(PlSqlGrammar.CHARACTER_LITERAL).map { it.tokenValue })
            .containsExactly("'+DATA'")
        assertThatAst(move.getDescendants(DdlGrammar.INDEX_SIZE_CLAUSE)).isEmpty()
        assertThatAst(move.getDescendants(DdlGrammar.AUTOEXTEND_CLAUSE)).isEmpty()
    }
}
