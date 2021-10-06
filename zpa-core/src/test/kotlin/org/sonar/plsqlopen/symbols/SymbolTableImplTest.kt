/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
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
package org.sonar.plsqlopen.symbols

import com.felipebz.flr.api.AstNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType
import org.sonar.plugins.plsqlopen.api.symbols.Scope
import org.sonar.plugins.plsqlopen.api.symbols.Symbol.Kind

class SymbolTableImplTest {

    @Test
    fun verifyScopes() {
        val scope1 = mock(Scope::class.java)
        val scope2 = mock(Scope::class.java)

        val symbolTable = SymbolTableImpl()
        symbolTable.addScope(scope1)
        symbolTable.addScope(scope2)

        assertThat(symbolTable.scopes).containsExactly(scope1, scope2)
    }

    @Test
    fun doNotReturnScopeIfNotIdentified() {
        val symbolTable = SymbolTableImpl()
        assertThat(symbolTable.getScopeFor(newAstNodeForTest("foo"))).isNull()
    }

    @Test
    fun returnScopeForSymbol() {
        val node1 = newAstNodeForTest("foo")
        val scope1 = ScopeImpl(null, node1, isAutonomousTransaction = false, hasExceptionHandler = false)

        val node2 = newAstNodeForTest("bar")
        val scope2 = ScopeImpl(null, node2, isAutonomousTransaction = false, hasExceptionHandler = false)

        val symbolTable = SymbolTableImpl()
        symbolTable.addScope(scope1)
        symbolTable.addScope(scope2)

        assertThat(symbolTable.getScopeFor(node1)).isEqualTo(scope1)
        assertThat(symbolTable.getScopeFor(node2)).isEqualTo(scope2)
    }

    @Test
    fun doNotReturnSymbolIfNotIdentified() {
        val symbolTable = SymbolTableImpl()
        assertThat(symbolTable.getSymbolFor(newAstNodeForTest("foo"))).isNull()
    }

    @Test
    fun returnSymbolForNode() {
        val node = newAstNodeForTest("foo")
        val node2 = newAstNodeForTest("bar")
        val scope = ScopeImpl(null, newAstNodeForTest("scope"), isAutonomousTransaction = false, hasExceptionHandler = false)

        val symbolTable = SymbolTableImpl()
        val symbol = symbolTable.declareSymbol(node, Kind.CURSOR, scope, PlSqlType.UNKNOWN)

        assertThat(symbolTable.getSymbolFor(node)).isEqualTo(symbol)
        assertThat(symbolTable.getSymbolFor(node2)).isNull()
    }

    @Test
    fun doNotReturnScopeForSymbolIfNotIdentified() {
        val symbolTable = SymbolTableImpl()
        assertThat(symbolTable.getScopeForSymbol(newAstNodeForTest("foo"))).isNull()
    }

    @Test
    fun returnScopeForSymbolForNode() {
        val node = newAstNodeForTest("foo")
        val node2 = newAstNodeForTest("bar")
        val scope = ScopeImpl(null, newAstNodeForTest("scope"), isAutonomousTransaction = false, hasExceptionHandler = false)

        val symbolTable = SymbolTableImpl()
        symbolTable.declareSymbol(node, Kind.CURSOR, scope, PlSqlType.UNKNOWN)

        assertThat(symbolTable.getScopeForSymbol(node)).isEqualTo(scope)
        assertThat(symbolTable.getScopeForSymbol(node2)).isNull()
    }

    @Test
    fun getSymbolsByKind() {
        val scope = ScopeImpl(null, newAstNodeForTest("scope"), isAutonomousTransaction = false, hasExceptionHandler = false)

        val node = newAstNodeForTest("foo")

        val symbolTable = SymbolTableImpl()
        val symbol1 = symbolTable.declareSymbol(node, Kind.CURSOR, scope, PlSqlType.UNKNOWN)
        val symbol2 = symbolTable.declareSymbol(node, Kind.VARIABLE, scope, PlSqlType.UNKNOWN)

        assertThat(symbolTable.getSymbols(Kind.CURSOR)).containsExactly(symbol1)
        assertThat(symbolTable.getSymbols(Kind.VARIABLE)).containsExactly(symbol2)
        assertThat(symbolTable.getSymbols(Kind.PARAMETER)).isEmpty()
    }

    @Test
    fun getSymbolsByName() {
        val scope = ScopeImpl(null, newAstNodeForTest("scope"), isAutonomousTransaction = false, hasExceptionHandler = false)

        val node1 = newAstNodeForTest("foo")
        val node2 = newAstNodeForTest("FOO")

        val symbolTable = SymbolTableImpl()
        val symbol1 = symbolTable.declareSymbol(node1, Kind.CURSOR, scope, PlSqlType.UNKNOWN)
        val symbol2 = symbolTable.declareSymbol(node2, Kind.VARIABLE, scope, PlSqlType.UNKNOWN)

        assertThat(symbolTable.getSymbols("foo")).containsExactly(symbol1, symbol2)
        assertThat(symbolTable.getSymbols("bar")).isEmpty()
    }

    private fun newAstNodeForTest(value: String): AstNode {
        val node = mock(AstNode::class.java)
        `when`(node.tokenOriginalValue).thenReturn(value)
        return node
    }

}
