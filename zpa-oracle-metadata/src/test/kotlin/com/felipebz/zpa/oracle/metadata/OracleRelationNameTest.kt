//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.oracle.metadata

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class OracleRelationNameTest {
    @Test
    fun aWrittenNameIsReadAsOracleReadsAnIdentifier() {
        assertThat(OracleRelationName.dictionaryNameOf("ledger")).isEqualTo("LEDGER")
        assertThat(OracleRelationName.dictionaryNameOf(" Ledger ")).isEqualTo("LEDGER")
        assertThat(OracleRelationName.dictionaryNameOf("\"ledger\"")).isEqualTo("ledger")
        assertThat(OracleRelationName.dictionaryNameOf("\"Two Words\"")).isEqualTo("Two Words")
        for (invalid in listOf("", "\"\"", "le\"dger", "\"le\"dger\"", "two words", "\"open")) {
            assertThatThrownBy { OracleRelationName.dictionaryNameOf(invalid) }.describedAs(invalid).isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun aWrittenRelationSplitsOnTheDotOutsideQuotes() {
        assertThat(OracleRelationName.parse("ref.countries")).isEqualTo(OracleRelationName.of("REF", "COUNTRIES"))
        assertThat(OracleRelationName.parse("\"ref\".\"Odd.Name\"")).isEqualTo(OracleRelationName.of("ref", "Odd.Name"))
        for (invalid in listOf("countries", "a.b.c", "\"ref.countries", "ref.")) {
            assertThatThrownBy { OracleRelationName.parse(invalid) }.describedAs(invalid).isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
