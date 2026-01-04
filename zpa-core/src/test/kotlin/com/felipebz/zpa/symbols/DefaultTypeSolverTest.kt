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
package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.symbols.PlSqlType
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.api.symbols.datatype.PlSqlDatatype
import com.felipebz.zpa.api.symbols.datatype.RowtypeDatatype
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import java.nio.charset.StandardCharsets

class DefaultTypeSolverTest {

    private lateinit var scope: ScopeImpl
    private val p = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8))
    private val typeSolver = DefaultTypeSolver()

    @BeforeEach
    fun setup() {
        scope = ScopeImpl()
    }

    @Test
    fun identifyNumericType() {
        val type = solveTypeFromDatatype("number")
        assertThat(type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(type.isNumeric).isTrue
    }

    @Test
    fun identifyTypeNotNull() {
        val type = solveTypeFromDatatype("number not null")
        assertThat(type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(type.isNumeric).isTrue
    }

    @Test
    fun identifyCharacterType() {
        val type = solveTypeFromDatatype("varchar2(100)")
        assertThat(type).isEqualTo(PlSqlType.CHARACTER)
        assertThat(type.isCharacter).isTrue
    }

    @Test
    fun identifyDateType() {
        val type = solveTypeFromDatatype("date")
        assertThat(type).isEqualTo(PlSqlType.DATE)
    }

    @Test
    fun identifyLobType() {
        val type = solveTypeFromDatatype("clob")
        assertThat(type).isEqualTo(PlSqlType.LOB)
    }

    @Test
    fun identifyBooleanType() {
        val type = solveTypeFromDatatype("boolean")
        assertThat(type).isEqualTo(PlSqlType.BOOLEAN)
    }

    @Test
    fun identifyRowtype() {
        val type = solveTypeFromDatatype("tab%rowtype")
        assertThat(type).isEqualTo(PlSqlType.ROWTYPE)
    }

    @Test
    fun identifyRowtypeNotNull() {
        val type = solveTypeFromDatatype("tab%rowtype not null")
        assertThat(type).isEqualTo(PlSqlType.ROWTYPE)
    }

    @Test
    fun identifyCustomType() {
        val symbol = createSymbol("my_type", Symbol.Kind.TYPE, RowtypeDatatype())
        scope.addSymbol(symbol)

        val type = solveTypeFromDatatype("my_type")
        assertThat(type).isEqualTo(PlSqlType.ROWTYPE)
    }

    @Test
    fun unknownType() {
        val type = solveTypeFromDatatype("tab.col%type")
        assertThat(type).isEqualTo(PlSqlType.UNKNOWN)
        assertThat(type.isUnknown).isTrue
    }

    @Test
    fun unknownTypeNotNull() {
        val type = solveTypeFromDatatype("tab.col%type not null")
        assertThat(type).isEqualTo(PlSqlType.UNKNOWN)
        assertThat(type.isUnknown).isTrue
    }

    @Test
    fun identifyNumericLiteral() {
        val type = solveTypeFromLiteral("1")
        assertThat(type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(type.isNumeric).isTrue
    }

    @Test
    fun identifyCharacterLiteral() {
        val type = solveTypeFromLiteral("'foo'")
        val type2 = solveTypeFromLiteral("q'!foo!'")
        val type3 = solveTypeFromLiteral("n'!foo!'")
        val type4 = solveTypeFromLiteral("nq'!foo!'")

        assertThat(arrayOf(type, type2, type3, type4))
            .allMatch { it == PlSqlType.CHARACTER && it.isCharacter }
    }

    @Test
    fun identifyDateLiteral() {
        val type = solveTypeFromLiteral("date '2000-01-01'")
        assertThat(type).isEqualTo(PlSqlType.DATE)
    }

    @Test
    fun identifyBooleanLiteral() {
        val type = solveTypeFromLiteral("true")
        assertThat(type).isEqualTo(PlSqlType.BOOLEAN)
    }

    @Test
    fun emptyStringShouldNotBeTypedAsCharacter() {
        val type = solveTypeFromLiteral("''")
        val type2 = solveTypeFromLiteral("q'!!'")
        val type3 = solveTypeFromLiteral("n'!!'")
        val type4 = solveTypeFromLiteral("nq'!!'")

        assertThat(arrayOf(type, type2, type3, type4)).allMatch { it == PlSqlType.NULL }
    }

    private fun solveTypeFromDatatype(code: String): PlSqlType {
        p.setRootRule(p.grammar.rule(PlSqlGrammar.DATATYPE))
        return typeSolver.solve(p.parse(code), scope).type
    }

    private fun solveTypeFromLiteral(code: String): PlSqlType {
        p.setRootRule(p.grammar.rule(PlSqlGrammar.LITERAL))
        return typeSolver.solve(p.parse(code), scope).type
    }

    private fun mockAstNode() = mock(AstNode::class.java)

    private fun createSymbol(name: String, kind: Symbol.Kind, type: PlSqlDatatype): Symbol {
        val node = mockAstNode()
        whenever(node.tokenValue).thenReturn(name)
        return Symbol(node, kind, scope, type)
    }

}
