//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:Suppress("DEPRECATION")

package com.felipebz.zpa.api.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.symbols.Symbol.Kind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class SymbolTest {

    @Test
    fun testSymbol() {
        val scope = mock(Scope::class.java)
        val symbol = createSymbol(scope, "foo", Kind.VARIABLE)

        assertThat(symbol.name).isEqualTo("foo")
        assertThat(symbol.kind).isEqualTo(Kind.VARIABLE)
        assertThat(symbol.scope).isEqualTo(scope)
        assertThat(symbol.toString()).startsWith("Symbol name=foo kind=VARIABLE datatype=Unknown")
    }

    @Test
    fun getModifiers() {
        val scope = mock(Scope::class.java)
        val symbol = createSymbol(scope, "foo", Kind.VARIABLE)

        val node = mock(AstNode::class.java)
        val modifiers = ArrayList<AstNode>()
        modifiers.add(node)
        symbol.addModifiers(modifiers)

        assertThat(symbol.modifiers).containsExactly(node)
    }

    @Test
    fun hasModifier() {
        val scope = mock(Scope::class.java)
        val symbol = createSymbol(scope, "foo", Kind.VARIABLE)

        val node = mock(AstNode::class.java)
        `when`(node.tokenValue).thenReturn("foo")
        val modifiers = ArrayList<AstNode>()
        modifiers.add(node)
        symbol.addModifiers(modifiers)

        assertThat(symbol.hasModifier("foo")).isTrue
        assertThat(symbol.hasModifier("FOO")).isTrue
        assertThat(symbol.hasModifier("bar")).isFalse
    }

    private fun createSymbol(scope: Scope, name: String, kind: Kind): Symbol {
        val node = mock(AstNode::class.java)
        `when`(node.tokenValue).thenReturn(name)
        return Symbol(node, kind, scope, null)
    }

}
