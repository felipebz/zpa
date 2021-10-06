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
package org.sonar.plugins.plsqlopen.api.symbols

import com.felipebz.flr.api.AstNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.sonar.plugins.plsqlopen.api.symbols.Symbol.Kind

class SymbolTest {

    @Test
    fun testSymbol() {
        val scope = mock(Scope::class.java)
        val symbol = createSymbol(scope, "foo", Kind.VARIABLE)

        assertThat(symbol.name()).isEqualTo("foo")
        assertThat(symbol.kind()).isEqualTo(Kind.VARIABLE)
        assertThat(symbol.scope()).isEqualTo(scope)
        assertThat(symbol.toString()).startsWith("Symbol{name='foo', kind=VARIABLE, scope=")
    }

    @Test
    fun getModifiers() {
        val scope = mock(Scope::class.java)
        val symbol = createSymbol(scope, "foo", Kind.VARIABLE)

        val node = mock(AstNode::class.java)
        val modifiers = ArrayList<AstNode>()
        modifiers.add(node)
        symbol.addModifiers(modifiers)

        assertThat(symbol.modifiers()).containsExactly(node)
    }

    @Test
    fun hasModifier() {
        val scope = mock(Scope::class.java)
        val symbol = createSymbol(scope, "foo", Kind.VARIABLE)

        val node = mock(AstNode::class.java)
        `when`(node.tokenOriginalValue).thenReturn("foo")
        val modifiers = ArrayList<AstNode>()
        modifiers.add(node)
        symbol.addModifiers(modifiers)

        assertThat(symbol.hasModifier("foo")).isTrue
        assertThat(symbol.hasModifier("FOO")).isTrue
        assertThat(symbol.hasModifier("bar")).isFalse
    }

    private fun createSymbol(scope: Scope, name: String, kind: Kind): Symbol {
        val node = mock(AstNode::class.java)
        `when`(node.tokenOriginalValue).thenReturn(name)
        return Symbol(node, kind, scope, null)
    }

}
