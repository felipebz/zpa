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
import org.assertj.core.api.Assertions.assertThat as assertThatValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IdentifierNameTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.IDENTIFIER_NAME)
    }

    @Test
    fun matchesSimpleIdentifier() {
        assertThat(p).matches("x")
    }

    @Test
    fun matchesIdentifierWithNumber() {
        assertThat(p).matches("t2")
    }

    @Test
    fun matchesIdentifierWithNumberSign() {
        assertThat(p).matches("phone#")
        assertThat(p).matches("SN##")
    }

    @Test
    fun matchesIdentifierWithUnderscore() {
        assertThat(p).matches("credit_limit")
        assertThat(p).matches("try_again_")
    }

    @Test
    fun matchesIdentifierWithDollarSign() {
        assertThat(p).matches("oracle\$number")
        assertThat(p).matches("money$$\$tree")
    }

    @Test
    fun matchesQuotedIdentifier() {
        assertThat(p).matches("\"X+Y\"")
        assertThat(p).matches("\"last name\"")
        assertThat(p).matches("\"on/off switch\"")
        assertThat(p).matches("\"employee(s)\"")
        assertThat(p).matches("\"*** header info ***\"")
    }

    @Test
    fun matchesNonReservedKeywords() {
        PlSqlKeyword.nonReservedKeywords.forEach { keyword ->
            assertThat(p).matches(keyword.value)
        }
    }

    @Test
    fun preservesNonReservedKeywordAst() {
        val identifier = p.parse("CuRsOr")
        val nonReservedKeyword = identifier.getFirstDescendant(PlSqlGrammar.NON_RESERVED_KEYWORD)

        assertThatValue(identifier.type).isEqualTo(PlSqlGrammar.IDENTIFIER_NAME)
        assertThatValue(nonReservedKeyword.type).isEqualTo(PlSqlGrammar.NON_RESERVED_KEYWORD)
        assertThatValue(nonReservedKeyword.parent).isSameAs(identifier)
        assertThatValue(nonReservedKeyword.fromIndex).isEqualTo(identifier.fromIndex)
        assertThatValue(nonReservedKeyword.toIndex).isEqualTo(identifier.toIndex)
        assertThatValue(nonReservedKeyword.tokens.map { it.originalValue }).containsExactly("CuRsOr")
    }

    @Test
    fun matchesIdentifierWithSpecialCharacters() {
        assertThat(p).matches("variável")
    }

    @Test
    fun notMatchesIdentifierStartingWithNumber() {
        assertThat(p).notMatches("2foo")
    }

    @Test
    fun notMatchesIdentifierWithAmpersand() {
        assertThat(p).notMatches("mine&yours")
    }

    @Test
    fun notMatchesIdentifierWithHyphen() {
        assertThat(p).notMatches("debit-amount")
    }

    @Test
    fun notMatchesIdentifierWithSlash() {
        assertThat(p).notMatches("on/off")
    }

    @Test
    fun notMatchesIdentifierWithSpace() {
        assertThat(p).notMatches("user id")
    }

    @Test
    fun notMatchesQuotedIdentifierCornerCases() {
        assertThat(p).notMatches("\"\"")
        assertThat(p).notMatches("\"\"\"\"")
    }

    @Test
    fun rejectsTokensThatAreNotIdentifierNames() {
        listOf("select", "begin", ",", "42", "'text'", "").forEach { source ->
            assertThat(p).notMatches(source)
        }
    }

}
