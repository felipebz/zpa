//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks.utplsql

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UtPlSqlOracleWordSyntaxTest {

    @Test
    fun acceptsUnicodeAlphanumericCharacters() {
        assertThat(UtPlSqlOracleWordSyntax.isWordOrDollarOrHash("emissão")).isTrue()
        assertThat(UtPlSqlOracleWordSyntax.isWordOrDollarOrHash("ação_1")).isTrue()
        assertThat(UtPlSqlOracleWordSyntax.isWordOrDollarOrHash("δοκιμή_1")).isTrue()
    }

    @Test
    fun preservesQualifiedComponentBoundaries() {
        assertThat(UtPlSqlOracleWordSyntax.isQualifiedWordOrDollarOrHash("brasil.emissão")).isTrue()
        assertThat(UtPlSqlOracleWordSyntax.isQualifiedWordOrDollarOrHash("foo..bar")).isFalse()
        assertThat(UtPlSqlOracleWordSyntax.isQualifiedWordOrDollarOrHash("foo bar")).isFalse()
    }
}
