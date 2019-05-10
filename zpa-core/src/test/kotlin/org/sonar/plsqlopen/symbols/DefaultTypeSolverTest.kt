/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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
package org.sonar.plsqlopen.symbols

import org.assertj.core.api.Assertions
import org.junit.Test
import org.sonar.plsqlopen.parser.PlSqlParser
import org.sonar.plsqlopen.squid.PlSqlConfiguration
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType
import java.nio.charset.StandardCharsets

class DefaultTypeSolverTest {

    private val p = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8))
    private val typeSolver = DefaultTypeSolver()

    @Test
    fun identifyNumericType() {
        val type = solveTypeFromDatatype("number")
        Assertions.assertThat(type).isEqualTo(PlSqlType.NUMERIC)
        Assertions.assertThat(type.isNumeric).isTrue()
    }

    @Test
    fun identifyTypeNotNull() {
        val type = solveTypeFromDatatype("number not null")
        Assertions.assertThat(type).isEqualTo(PlSqlType.NUMERIC)
        Assertions.assertThat(type.isNumeric).isTrue()
    }

    @Test
    fun identifyCharacterType() {
        val type = solveTypeFromDatatype("varchar2(100)")
        Assertions.assertThat(type).isEqualTo(PlSqlType.CHARACTER)
        Assertions.assertThat(type.isCharacter).isTrue()
    }

    @Test
    fun identifyDateType() {
        val type = solveTypeFromDatatype("date")
        Assertions.assertThat(type).isEqualTo(PlSqlType.DATE)
    }

    @Test
    fun identifyLobType() {
        val type = solveTypeFromDatatype("clob")
        Assertions.assertThat(type).isEqualTo(PlSqlType.LOB)
    }

    @Test
    fun identifyBooleanType() {
        val type = solveTypeFromDatatype("boolean")
        Assertions.assertThat(type).isEqualTo(PlSqlType.BOOLEAN)
    }

    @Test
    fun identifyRowtype() {
        val type = solveTypeFromDatatype("tab%rowtype")
        Assertions.assertThat(type).isEqualTo(PlSqlType.ROWTYPE)
    }

    @Test
    fun identifyRowtypeNotNull() {
        val type = solveTypeFromDatatype("tab%rowtype not null")
        Assertions.assertThat(type).isEqualTo(PlSqlType.ROWTYPE)
    }

    @Test
    fun unknownType() {
        val type = solveTypeFromDatatype("tab.col%type")
        Assertions.assertThat(type).isEqualTo(PlSqlType.UNKNOWN)
        Assertions.assertThat(type.isUnknown).isTrue()
    }

    @Test
    fun unknownTypeNotNull() {
        val type = solveTypeFromDatatype("tab.col%type not null")
        Assertions.assertThat(type).isEqualTo(PlSqlType.UNKNOWN)
        Assertions.assertThat(type.isUnknown).isTrue()
    }

    @Test
    fun ifNodeIsNullReturnsUnknownType() {
        val type = typeSolver.solve(null)
        Assertions.assertThat(type).isEqualTo(PlSqlType.UNKNOWN)
        Assertions.assertThat(type.isUnknown).isTrue()
    }

    @Test
    fun identifyNumericLiteral() {
        val type = solveTypeFromLiteral("1")
        Assertions.assertThat(type).isEqualTo(PlSqlType.NUMERIC)
        Assertions.assertThat(type.isNumeric).isTrue()
    }

    @Test
    fun identifyCharacterLiteral() {
        val type = solveTypeFromLiteral("'foo'")
        Assertions.assertThat(type).isEqualTo(PlSqlType.CHARACTER)
        Assertions.assertThat(type.isCharacter).isTrue()
    }

    @Test
    fun identifyDateLiteral() {
        val type = solveTypeFromLiteral("date '2000-01-01'")
        Assertions.assertThat(type).isEqualTo(PlSqlType.DATE)
    }

    @Test
    fun identifyBooleanLiteral() {
        val type = solveTypeFromLiteral("true")
        Assertions.assertThat(type).isEqualTo(PlSqlType.BOOLEAN)
    }

    @Test
    fun emptyStringShouldNotBeTypedAsCharacter() {
        val type = solveTypeFromLiteral("''")
        /* TODO: handle these cases
        PlSqlType type = solveTypeFromLiteral("q'!!'");
        PlSqlType type = solveTypeFromLiteral("n'!!'");
        PlSqlType type = solveTypeFromLiteral("nq'!!'");
        */
        Assertions.assertThat(type).isEqualTo(PlSqlType.NULL)
    }

    private fun solveTypeFromDatatype(code: String): PlSqlType {
        p.setRootRule(p.grammar.rule(PlSqlGrammar.DATATYPE))
        return typeSolver.solve(p.parse(code))
    }

    private fun solveTypeFromLiteral(code: String): PlSqlType {
        p.setRootRule(p.grammar.rule(PlSqlGrammar.LITERAL))
        return typeSolver.solve(p.parse(code))
    }

}
