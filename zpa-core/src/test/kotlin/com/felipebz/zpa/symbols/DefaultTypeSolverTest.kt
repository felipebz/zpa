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
import com.felipebz.zpa.api.symbols.datatype.CharacterDatatype
import com.felipebz.zpa.api.symbols.datatype.NullDatatype
import com.felipebz.zpa.api.symbols.datatype.NumericDatatype
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
    fun evaluateStaticDatatypeConstraints() {
        val numeric = solveDatatype("number(2 + 3)") as NumericDatatype
        val numericWithScale = solveDatatype("number(5, 1 + 1)") as NumericDatatype
        val character = solveDatatype("varchar2(2 + 3)") as CharacterDatatype

        assertThat(numeric.length).isEqualTo(5)
        assertThat(numericWithScale.length).isEqualTo(5)
        assertThat(numericWithScale.scale).isEqualTo(2)
        assertThat(character.length).isEqualTo(5)
    }

    @Test
    fun evaluateParenthesizedAndUnaryDatatypeConstraints() {
        val parenthesized = solveDatatype("number((2 + 3))") as NumericDatatype
        val unary = solveDatatype("number(-(-5))") as NumericDatatype

        assertThat(parenthesized.length).isEqualTo(5)
        assertThat(unary.length).isEqualTo(5)
    }

    @Test
    fun preserveNumericScaleValuesFromLiteralsAndExpressions() {
        val positive = solveDatatype("number(5, 2)") as NumericDatatype
        val zero = solveDatatype("number(5, 0)") as NumericDatatype
        val negative = solveDatatype("number(5, -2)") as NumericDatatype
        val calculatedZero = solveDatatype("number(5, 1 - 1)") as NumericDatatype
        val calculatedNegative = solveDatatype("number(5, 1 - 3)") as NumericDatatype

        assertThat(positive.scale).isEqualTo(2)
        assertThat(zero.scale).isEqualTo(0)
        assertThat(negative.scale).isEqualTo(-2)
        assertThat(calculatedZero.scale).isEqualTo(0)
        assertThat(calculatedNegative.scale).isEqualTo(-2)
    }

    @Test
    fun unsupportedOrNonIntegralDatatypeConstraintsRemainUnknown() {
        val nonIntegral = solveDatatype("number(1 / 2)") as NumericDatatype
        val unresolved = solveDatatype("number(unknown_name)") as NumericDatatype
        val unsupported = solveDatatype("number(abs(-5))") as NumericDatatype
        val outOfRange = solveDatatype("number(2147483648)") as NumericDatatype

        assertThat(nonIntegral.length).isNull()
        assertThat(unresolved.length).isNull()
        assertThat(unsupported.length).isNull()
        assertThat(outOfRange.length).isNull()
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
        val datatype = solveLiteralDatatype("''")
        val datatype2 = solveLiteralDatatype("N''")
        val datatype3 = solveLiteralDatatype("n''")
        val datatype4 = solveLiteralDatatype("q'[]'")
        val datatype5 = solveLiteralDatatype("NQ'[]'")

        assertThat(arrayOf(datatype, datatype2, datatype3, datatype4, datatype5))
            .allMatch { it is NullDatatype }
    }

    @Test
    fun nonEmptyCharacterLiteralsShouldNotBeTypedAsNull() {
        val type = solveTypeFromLiteral("'a'")
        val type2 = solveTypeFromLiteral("N'a'")
        val type3 = solveTypeFromLiteral("n'a'")
        val type4 = solveTypeFromLiteral("n'!!'")
        val type5 = solveTypeFromLiteral("q'[a]'")
        val type6 = solveTypeFromLiteral("NQ'[a]'")

        assertThat(arrayOf(type, type2, type3, type4, type5, type6))
            .allMatch { it == PlSqlType.CHARACTER }
    }

    private fun solveTypeFromDatatype(code: String): PlSqlType {
        return solveDatatype(code).type
    }

    private fun solveDatatype(code: String): PlSqlDatatype {
        p.setRootRule(p.grammar.rule(PlSqlGrammar.DATATYPE))
        return typeSolver.solve(p.parse(code), scope)
    }

    private fun solveTypeFromLiteral(code: String): PlSqlType {
        return solveLiteralDatatype(code).type
    }

    private fun solveLiteralDatatype(code: String): PlSqlDatatype {
        p.setRootRule(p.grammar.rule(PlSqlGrammar.LITERAL))
        return typeSolver.solve(p.parse(code), scope)
    }

    private fun mockAstNode() = mock(AstNode::class.java)

    private fun createSymbol(name: String, kind: Symbol.Kind, type: PlSqlDatatype): Symbol {
        val node = mockAstNode()
        whenever(node.tokenValue).thenReturn(name)
        return Symbol(node, kind, scope, type)
    }

}
