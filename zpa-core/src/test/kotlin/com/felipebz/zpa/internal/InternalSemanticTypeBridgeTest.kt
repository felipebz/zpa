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
package com.felipebz.zpa.internal

import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.symbols.PlSqlType
import com.felipebz.zpa.api.symbols.datatype.CharacterDatatype
import com.felipebz.zpa.api.symbols.datatype.NumericDatatype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@OptIn(ZpaInternalApi::class)
class InternalSemanticTypeBridgeTest {
    @Test
    fun returnsLegacyNumericCategory() {
        val node = node()
        node.plSqlDatatype = NumericDatatype()

        assertThat(InternalSemanticTypeBridge.effectivePlSqlType(node)).isEqualTo(PlSqlType.NUMERIC)
    }

    @Test
    fun returnsLegacyCharacterCategory() {
        val node = node()
        node.plSqlDatatype = CharacterDatatype()

        assertThat(InternalSemanticTypeBridge.effectivePlSqlType(node)).isEqualTo(PlSqlType.CHARACTER)
    }

    @Test
    fun mapsUnknownEffectiveCategoryToUnknownPlSqlType() {
        assertThat(InternalSemanticTypeBridge.effectivePlSqlType(node())).isEqualTo(PlSqlType.UNKNOWN)
    }

    private fun node() = SemanticAstNode(PlSqlGrammar.DATATYPE, "DATATYPE", null)
}
