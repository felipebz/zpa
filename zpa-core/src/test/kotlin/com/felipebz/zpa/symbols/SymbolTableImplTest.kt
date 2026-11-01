//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:Suppress("DEPRECATION")

package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.symbols.Scope
import com.felipebz.zpa.api.symbols.Symbol.Kind
import com.felipebz.zpa.api.symbols.datatype.UnknownDatatype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

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
    fun returnSymbolForNode() {
        val node = newAstNodeForTest("foo")
        val node2 = newAstNodeForTest("bar")
        val scope =
            ScopeImpl(null, newAstNodeForTest("scope"), isAutonomousTransaction = false, hasExceptionHandler = false)

        val symbolTable = SymbolTableImpl()
        val symbol = symbolTable.declareSymbol(node, Kind.CURSOR, scope, UnknownDatatype)

        assertThat(symbolTable.symbols.find { it.declaration == node }).isEqualTo(symbol)
        assertThat(symbolTable.symbols.find { it.declaration == node2 }).isNull()
    }

    @Test
    fun returnScopeForSymbolForNode() {
        val node = newAstNodeForTest("foo")
        val node2 = newAstNodeForTest("bar")
        val scope =
            ScopeImpl(null, newAstNodeForTest("scope"), isAutonomousTransaction = false, hasExceptionHandler = false)

        val symbolTable = SymbolTableImpl()
        symbolTable.declareSymbol(node, Kind.CURSOR, scope, UnknownDatatype)

        assertThat(symbolTable.symbols.find { it.declaration == node }?.scope).isEqualTo(scope)
        assertThat(symbolTable.symbols.find { it.declaration == node2 }?.scope).isNull()
    }

    @Test
    fun getSymbolsByKind() {
        val scope =
            ScopeImpl(null, newAstNodeForTest("scope"), isAutonomousTransaction = false, hasExceptionHandler = false)

        val node = newAstNodeForTest("foo")

        val symbolTable = SymbolTableImpl()
        val symbol1 = symbolTable.declareSymbol(node, Kind.CURSOR, scope, UnknownDatatype)
        val symbol2 = symbolTable.declareSymbol(node, Kind.VARIABLE, scope, UnknownDatatype)

        assertThat(symbolTable.symbols.filter { it.kind == Kind.CURSOR }).containsExactly(symbol1)
        assertThat(symbolTable.symbols.filter { it.kind == Kind.VARIABLE }).containsExactly(symbol2)
        assertThat(symbolTable.symbols.filter { it.kind == Kind.PARAMETER }).isEmpty()
    }

    @Test
    fun getSymbolsByName() {
        val scope =
            ScopeImpl(null, newAstNodeForTest("scope"), isAutonomousTransaction = false, hasExceptionHandler = false)

        val node1 = newAstNodeForTest("foo")
        val node2 = newAstNodeForTest("FOO")

        val symbolTable = SymbolTableImpl()
        val symbol1 = symbolTable.declareSymbol(node1, Kind.CURSOR, scope, UnknownDatatype)
        val symbol2 = symbolTable.declareSymbol(node2, Kind.VARIABLE, scope, UnknownDatatype)

        assertThat(symbolTable.symbols.filter { it.name.equals("foo", ignoreCase = true) }).containsExactly(symbol1, symbol2)
        assertThat(symbolTable.symbols.filter { it.name.equals("bar", ignoreCase = true) }).isEmpty()
    }

    private fun newAstNodeForTest(value: String): AstNode {
        val node = mock(AstNode::class.java)
        `when`(node.tokenValue).thenReturn(value)
        return node
    }

}
