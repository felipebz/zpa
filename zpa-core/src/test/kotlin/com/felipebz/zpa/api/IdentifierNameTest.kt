//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatValue

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
    fun matchesCreateJavaContextualKeywords() {
        listOf("class", "named", "noforce", "resolve", "resolver", "source").forEach { keyword ->
            assertThat(p).matches(keyword)
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
    fun preservesOutAsNonReservedKeywordAst() {
        val identifier = p.parse("OuT")
        val nonReservedKeyword = identifier.getFirstDescendant(PlSqlGrammar.NON_RESERVED_KEYWORD)

        assertThatValue(identifier.type).isEqualTo(PlSqlGrammar.IDENTIFIER_NAME)
        assertThatValue(nonReservedKeyword.tokens.map { it.type }).containsExactly(PlSqlKeyword.OUT)
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
