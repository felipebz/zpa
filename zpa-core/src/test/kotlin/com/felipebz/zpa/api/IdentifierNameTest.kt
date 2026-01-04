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
        assertThat(p).matches("cursor")
        assertThat(p).matches("rowid")
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

}
