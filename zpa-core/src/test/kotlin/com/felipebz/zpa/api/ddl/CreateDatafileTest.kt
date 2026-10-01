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

class CreateDatafileTest : RuleTest() {

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
    fun matchesSourcesWithOmittedAsAndNew() {
        matches(
            "create datafile 'old.dbf'",
            "create datafile 7;",
            "create datafile 'old.dbf' as new",
            "create datafile 7 as new",
            "payable create datafile '+DATA/payable/datafile/users.257.123456789' as new"
        )
    }

    @Test
    fun matchesMixedSourceLists() {
        matches(
            "create datafile 'old.dbf', 8",
            "create datafile 7, 'old.dbf'",
            "create datafile 'a.dbf', 2, 'c.dbf', 4",
            "create datafile 1, 'b.dbf', 3, 'd.dbf'",
            "create datafile 'old.dbf', 8 as new",
            "create datafile 7, 'old.dbf' as new",
            "create datafile 'old.dbf', 8 as 'new.dbf' size 20m, '+DATA' reuse",
            "create datafile 7, 'old.dbf' as 'new.dbf', 'other.dbf' autoextend on"
        )
        notMatches(
            "create datafile 'old.dbf', , 8",
            "create datafile 7, 'old.dbf',",
            "create datafile , 7, 'old.dbf'",
            "create datafile 7 'old.dbf'"
        )
    }

    @Test
    fun matchesMultipleSourcesAndDestinationSpecifications() {
        matches(
            "create datafile 'old.dbf', 'other.dbf'",
            "create datafile 7, 8",
            "create datafile 'old.dbf', 'other.dbf' as new",
            "create datafile 7, 8 as new",
            "create datafile 'old.dbf', 'other.dbf' as 'new.dbf' size 20m, 'new2.dbf' reuse",
            "create datafile 7, 8 as '+DATA' size 20m reuse, '+RECO/payable/new2.dbf' size 30m",
            "create datafile 7, 8 as 'new.dbf', '+DATA/payable/new2.dbf' reuse"
        )
    }

    @Test
    fun matchesOrdinaryAndAsmDestinationsWithSizeAndReuse() {
        matches(
            "create datafile 'old.dbf' as 'new.dbf'",
            "create datafile 7 as '+DATA'",
            "create datafile 7 as '+DATA/payable/new.dbf' reuse",
            "create datafile 'old.dbf' as 'new.dbf' size 20m",
            "create datafile 7 as 'new.dbf' reuse",
            "create datafile 7 as 'new.dbf' size 1024 reuse",
            "create datafile 7 as 'new.dbf' size 50k reuse",
            "create datafile 7 as '+DATA/payable/datafile/users.257.123456789' size 2g"
        )
    }

    @Test
    fun matchesNumericSourceLiterals() {
        matches(
            "create datafile 7.0",
            "create datafile 7.",
            "create datafile 7e0",
            "create datafile 7e2",
            "create datafile .17e2",
            "create datafile 7.0, 8e0 as new",
            "create datafile 7.5",
            "create datafile 7e-1"
        )
    }

    @Test
    fun matchesOmittedDestinationNamesAndImplicitOmfSlots() {
        matches(
            "create datafile 7 as",
            "create datafile 7 as size 20m",
            "create datafile 7 as reuse",
            "create datafile 7 as size 20m reuse",
            "create datafile 7, 8 as 'new.dbf',",
            "create datafile 7, 8 as , 'new.dbf'",
            "create datafile 7, 8 as ,",
            "create datafile 7, 8, 9 as 'new.dbf',, '+DATA'",
            "create datafile 7 as 'new.dbf', '+DATA'",
            "create datafile 7, 8 as 'new.dbf'"
        )
    }

    @Test
    fun matchesAutoextensionInDestinationSpecifications() {
        matches(
            "create datafile 7 as autoextend off",
            "create datafile 7 as autoextend on",
            "create datafile 7 as 'new.dbf' autoextend off",
            "create datafile 7 as 'new.dbf' autoextend on",
            "create datafile 7 as 'new.dbf' autoextend on next 1m",
            "create datafile 7 as 'new.dbf' autoextend on maxsize unlimited",
            "create datafile 7 as 'new.dbf' size 20m reuse autoextend on next 1m maxsize 2g",
            "create datafile 7, 8 as '+DATA' autoextend off, size 30m autoextend on maxsize 1g"
        )
    }

    @Test
    fun rejectsNonliteralFilenamesAndMalformedNumericSources() {
        notMatches(
            "create datafile old.dbf",
            "create datafile \"old.dbf\"",
            "create datafile :file_number",
            "create datafile 7 + 1",
            "create datafile (7)",
            "create datafile -7",
            "create datafile +7",
            "create datafile 0x7",
            "create datafile 7foo",
            "create datafile 'old.dbf' as new.dbf",
            "create datafile 7 as \"new.dbf\"",
            "create datafile 7 as +DATA",
            "create datafile 7 as :filename",
            "create datafile 7 as 8",
            "create datafile 7 as 'new' || '.dbf'"
        )
    }

    @Test
    fun rejectsMissingSourcesAndParenthesizedLists() {
        notMatches(
            "create",
            "create datafile",
            "create datafile as new",
            "create datafile , 7",
            "create datafile 7,",
            "create datafile 7, as new",
            "create datafile 7,, 8",
            "create datafile ('old.dbf', 'other.dbf')",
            "create datafile 7 as ('new.dbf')"
        )
    }

    @Test
    fun rejectsMalformedNewAlternatives() {
        notMatches(
            "create datafile 7 new",
            "create datafile 7 as new 'new.dbf'",
            "create datafile 7 as new, 'new.dbf'",
            "create datafile 7 as 'new.dbf', new",
            "create datafile 7 as new size 20m",
            "create datafile 7 as new reuse",
            "create datafile 7 as new autoextend on",
            "create datafile 7 as new as new",
            "create datafile 7 as 'new.dbf' as new"
        )
    }

    @Test
    fun rejectsMisorderedRepeatedAndIncompleteDestinationOptions() {
        notMatches(
            "create datafile 7 size 20m",
            "create datafile 7 reuse",
            "create datafile 7 as 'new.dbf' reuse size 20m",
            "create datafile 7 as size 20m 'new.dbf'",
            "create datafile 7 as 'new.dbf' size 20m size 30m",
            "create datafile 7 as 'new.dbf' reuse reuse",
            "create datafile 7 as 'new.dbf' size",
            "create datafile 7 as 'new.dbf' size -1m",
            "create datafile 7 as 'new.dbf' size :file_size",
            "create datafile 7 as 'new.dbf' keep",
            "create datafile 7 as 'new.dbf' online",
            "create datafile 7 as 'new.dbf' unexpected",
            "create datafile 7 as 'new.dbf'; unexpected"
        )
    }

    @Test
    fun rejectsMalformedAndMisorderedAutoextension() {
        notMatches(
            "create datafile 7 autoextend on",
            "create datafile 7 as autoextend",
            "create datafile 7 as autoextend off next 1m",
            "create datafile 7 as autoextend off maxsize 1g",
            "create datafile 7 as autoextend on next",
            "create datafile 7 as autoextend on maxsize",
            "create datafile 7 as autoextend on maxsize 1g next 1m",
            "create datafile 7 as autoextend on next 1m next 2m",
            "create datafile 7 as autoextend on maxsize 1g maxsize unlimited",
            "create datafile 7 as autoextend on autoextend off",
            "create datafile 7 as autoextend on size 20m",
            "create datafile 7 as autoextend on reuse"
        )
    }

    @Test
    fun separatesStatementCreateClauseAndDestinationSpecifications() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("alter database payable create datafile 'old.dbf', 'other.dbf' as " +
            "'new.dbf' size 20m reuse, '+DATA/payable/new2.dbf' size 30m;")
        val statement = tree.getDescendants(DdlGrammar.ALTER_DATABASE).single()
        val files = statement.getChildren(DdlGrammar.DATABASE_FILE_CLAUSES).single()
        val create = files.getChildren(DdlGrammar.CREATE_DATAFILE_CLAUSE).single()
        val destinations = create.getChildren(DdlGrammar.DATAFILE_TEMPFILE_SPEC)
        assertThatAst(destinations).hasSize(2)
        assertThatAst(destinations.map {
            it.getChildren(PlSqlGrammar.CHARACTER_LITERAL).single().tokenValue
        }).containsExactly("'new.dbf'", "'+DATA/payable/new2.dbf'")
        assertThatAst(destinations.map {
            it.getChildren(DdlGrammar.INDEX_SIZE_CLAUSE).single().tokenValue
        }).containsExactly("20", "30")
        assertThatAst(create.getChildren(PlSqlGrammar.CHARACTER_LITERAL).map { it.tokenValue })
            .containsExactly("'old.dbf'", "'other.dbf'")
    }
}
