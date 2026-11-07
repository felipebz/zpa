//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(com.felipebz.zpa.api.annotations.ZpaExperimentalApi::class)
@file:Suppress("DEPRECATION")

package com.felipebz.zpa.symbols

import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.symbols.PlSqlType
import com.felipebz.zpa.api.symbols.datatype.RecordDatatype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EffectiveSemanticTypeQueryTest {
    @Test
    fun legacyRecordTypeIsReportedAsRecord() {
        val node = node()
        node.plSqlDatatype = RecordDatatype(null, null, emptyList())

        assertThat(EffectiveSemanticTypeQuery.typeCategory(node)).isEqualTo(
            EffectiveTypeCategory.Known(PlSqlType.RECORD, EffectiveTypeSource.LEGACY)
        )
    }

    private fun node() = SemanticAstNode(PlSqlGrammar.DATATYPE, "DATATYPE", null)

}
