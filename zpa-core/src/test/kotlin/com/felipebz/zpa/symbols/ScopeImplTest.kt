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
import com.felipebz.zpa.api.symbols.Scope
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.api.symbols.Symbol.Kind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ScopeImplTest {

    @Test
    fun testScope() {
        val node = mock(AstNode::class.java)
        val scope = ScopeImpl(null, node)
        assertThat(scope.outer).isNull()
        assertThat(scope.tree).isEqualTo(node)
        assertThat(scope.isAutonomousTransaction).isFalse
        assertThat(scope.isGlobal).isFalse
    }

    @Test
    fun globalContextMarkerPreservesGlobalScopeSemantics() {
        val fileScope = ScopeImpl(isGlobalContext = true)
        val packageScope = ScopeImpl(fileScope, type = PlSqlGrammar.CREATE_PACKAGE)
        val packageBodyScope = ScopeImpl(fileScope, type = PlSqlGrammar.CREATE_PACKAGE_BODY)
        val typeScope = ScopeImpl(fileScope, type = PlSqlGrammar.CREATE_TYPE)
        val typeBodyScope = ScopeImpl(fileScope, type = PlSqlGrammar.CREATE_TYPE_BODY)
        val standaloneScope = ScopeImpl(fileScope, type = PlSqlGrammar.CREATE_PROCEDURE)
        val nestedBlockScope = ScopeImpl(standaloneScope, type = PlSqlGrammar.BLOCK_STATEMENT)
        val packageMemberScope = ScopeImpl(packageScope, type = PlSqlGrammar.PROCEDURE_DECLARATION)
        val packageBodyMemberScope = ScopeImpl(packageBodyScope, type = PlSqlGrammar.PROCEDURE_DECLARATION)
        val typeMemberScope = ScopeImpl(typeScope, type = PlSqlGrammar.PROCEDURE_DECLARATION)
        val typeBodyMemberScope = ScopeImpl(typeBodyScope, type = PlSqlGrammar.PROCEDURE_DECLARATION)

        assertThat(fileScope.outer).isNull()
        assertThat(packageScope.outer).isSameAs(fileScope)
        assertThat(fileScope.isGlobal).isTrue
        assertThat(packageScope.isGlobal).isTrue
        assertThat(packageBodyScope.isGlobal).isTrue
        assertThat(typeScope.isGlobal).isTrue
        assertThat(typeBodyScope.isGlobal).isTrue
        assertThat(standaloneScope.isGlobal).isTrue
        assertThat(nestedBlockScope.isGlobal).isFalse
        assertThat(packageMemberScope.isGlobal).isTrue
        assertThat(packageBodyMemberScope.isGlobal).isFalse
        assertThat(typeMemberScope.isGlobal).isTrue
        assertThat(typeBodyMemberScope.isGlobal).isFalse

        assertThat(Symbol(null, Kind.VARIABLE, packageScope, null).isGlobal).isTrue
        assertThat(Symbol(null, Kind.TYPE, packageScope, null).isGlobal).isTrue
        assertThat(Symbol(null, Kind.CURSOR, packageScope, null).isGlobal).isTrue
        assertThat(Symbol(null, Kind.PROCEDURE, packageScope, null).isGlobal).isTrue
        assertThat(Symbol(null, Kind.FUNCTION, packageScope, null).isGlobal).isTrue
        assertThat(Symbol(null, Kind.PROCEDURE, fileScope, null).isGlobal).isTrue
        assertThat(Symbol(null, Kind.FUNCTION, fileScope, null).isGlobal).isTrue
        assertThat(Symbol(null, Kind.PROCEDURE, packageBodyScope, null).isGlobal).isTrue
        assertThat(Symbol(null, Kind.PROCEDURE, packageBodyMemberScope, null).isGlobal).isFalse
        assertThat(Symbol(null, Kind.VARIABLE, nestedBlockScope, null).isGlobal).isFalse
    }

    @Test
    fun getSymbolsInScope() {
        val scope = ScopeImpl()

        val symbol1 = createSymbol(scope, "foo", Kind.VARIABLE)
        scope.addSymbol(symbol1)

        val symbol2 = createSymbol(scope, "bar", Kind.VARIABLE)
        scope.addSymbol(symbol2)

        assertThat(scope.symbols).containsExactly(symbol1, symbol2)
    }

    @Test
    fun getSymbolsByKind() {
        val scope = ScopeImpl()

        val symbol1 = createSymbol(scope, "foo", Kind.VARIABLE)
        scope.addSymbol(symbol1)

        val symbol2 = createSymbol(scope, "bar", Kind.CURSOR)
        scope.addSymbol(symbol2)

        assertThat(scope.getSymbols(Kind.VARIABLE)).containsExactly(symbol1)
        assertThat(scope.getSymbols(Kind.CURSOR)).containsExactly(symbol2)
    }

    @Test
    fun getSymbolsAcessibleInScope() {
        val scope = ScopeImpl()

        val symbol1 = createSymbol(scope, "foo", Kind.VARIABLE)
        scope.addSymbol(symbol1)

        val symbol2 = createSymbol(scope, "bar", Kind.VARIABLE)
        scope.addSymbol(symbol2)

        assertThat(scope.getSymbolsAcessibleInScope("foo")).containsExactly(symbol1)
        assertThat(scope.getSymbolsAcessibleInScope("foo", Kind.VARIABLE)).containsExactly(symbol1)
        assertThat(scope.getSymbolsAcessibleInScope("foo", Kind.CURSOR)).isEmpty()
    }

    @Test
    fun getSymbolsAcessibleInScopeConsideringOuterScope() {
        val outerScope = ScopeImpl()
        val symbol1 = createSymbol(outerScope, "foo", Kind.VARIABLE)
        outerScope.addSymbol(symbol1)

        val innerScope = ScopeImpl(outerScope)
        val symbol2 = createSymbol(innerScope, "bar", Kind.VARIABLE)
        innerScope.addSymbol(symbol2)

        assertThat(innerScope.getSymbolsAcessibleInScope("foo")).containsExactly(symbol1)
        assertThat(innerScope.getSymbolsAcessibleInScope("foo", Kind.VARIABLE)).containsExactly(symbol1)
        assertThat(innerScope.getSymbolsAcessibleInScope("foo", Kind.CURSOR)).isEmpty()
    }

    @Test
    fun getSymbol() {
        val scope = ScopeImpl()

        val symbol1 = createSymbol(scope, "foo", Kind.VARIABLE)
        scope.addSymbol(symbol1)

        val symbol2 = createSymbol(scope, "bar", Kind.VARIABLE)
        scope.addSymbol(symbol2)

        assertThat(scope.getSymbol("foo")).isEqualTo(symbol1)
        assertThat(scope.getSymbol("foo", Kind.VARIABLE)).isEqualTo(symbol1)
        assertThat(scope.getSymbol("foo", Kind.CURSOR)).isNull()
        assertThat(scope.getSymbol("baz")).isNull()
    }

    @Test
    fun getSymbolConsideringOuterScope() {
        val outerScope = ScopeImpl()
        val symbol1 = createSymbol(outerScope, "foo", Kind.VARIABLE)
        outerScope.addSymbol(symbol1)

        val innerScope = ScopeImpl(outerScope)
        val symbol2 = createSymbol(innerScope, "bar", Kind.VARIABLE)
        innerScope.addSymbol(symbol2)

        assertThat(innerScope.getSymbol("foo")).isEqualTo(symbol1)
        assertThat(innerScope.getSymbol("foo", Kind.VARIABLE)).isEqualTo(symbol1)
        assertThat(innerScope.getSymbol("foo", Kind.CURSOR)).isNull()
        assertThat(innerScope.getSymbol("baz")).isNull()
    }

    @Test
    fun getSymbolPreservesInsertionOrderAndKindFiltering() {
        val scope = ScopeImpl()
        val first = createSymbol(scope, "foo", Kind.VARIABLE)
        val second = createSymbol(scope, "foo", Kind.TYPE)
        scope.addSymbol(first)
        scope.addSymbol(second)

        assertThat(scope.getSymbol("foo")).isEqualTo(first)
        assertThat(scope.getSymbol("foo", Kind.TYPE)).isEqualTo(second)
    }

    @Test
    fun getSymbolPreservesShadowingAndOuterFallback() {
        val outerScope = ScopeImpl()
        val outer = createSymbol(outerScope, "foo", Kind.VARIABLE)
        outerScope.addSymbol(outer)

        val innerScope = ScopeImpl(outerScope)
        val inner = createSymbol(innerScope, "foo", Kind.VARIABLE)
        innerScope.addSymbol(inner)

        assertThat(innerScope.getSymbol("foo")).isEqualTo(inner)
        assertThat(innerScope.getSymbol("bar")).isNull()
        assertThat(ScopeImpl(outerScope).getSymbol("foo")).isEqualTo(outer)
    }

    @Test
    fun getSymbolPreservesPathFiltering() {
        val outerScope = ScopeImpl(identifier = "outer")
        val outer = createSymbol(outerScope, "foo", Kind.VARIABLE)
        outerScope.addSymbol(outer)
        val innerScope = ScopeImpl(outerScope, identifier = "inner")
        val inner = createSymbol(innerScope, "foo", Kind.VARIABLE)
        innerScope.addSymbol(inner)

        assertThat(innerScope.getSymbol("foo", listOf("inner"))).isEqualTo(inner)
        assertThat(innerScope.getSymbol("foo", listOf("outer"))).isEqualTo(outer)
    }

    @Test
    fun getSymbolPreservesQuotedAndUnquotedNameSemantics() {
        val scope = ScopeImpl()
        val unquoted = createSymbol(scope, "Foo", Kind.VARIABLE)
        val quoted = createSymbol(scope, "\"Foo\"", Kind.VARIABLE)
        scope.addSymbol(unquoted)
        scope.addSymbol(quoted)

        assertThat(scope.getSymbol("foo")).isEqualTo(unquoted)
        assertThat(scope.getSymbol("FOO")).isEqualTo(unquoted)
        assertThat(scope.getSymbol("\"Foo\"")).isEqualTo(quoted)
        assertThat(scope.getSymbol("\"foo\"")).isNull()
    }

    @Test
    fun getSymbolPreservesEqualsIgnoreCaseUnicodeSemantics() {
        val scope = ScopeImpl()
        val kelvin = createSymbol(scope, "\u212A", Kind.VARIABLE)
        val longS = createSymbol(scope, "\u017F", Kind.TYPE)
        val sharpS = createSymbol(scope, "\u00DF", Kind.FUNCTION)
        val supplementary = createSymbol(scope, "\uD801\uDC00", Kind.PACKAGE)
        scope.addSymbol(kelvin)
        scope.addSymbol(longS)
        scope.addSymbol(sharpS)
        scope.addSymbol(supplementary)

        assertThat(scope.getSymbol("k")).isEqualTo(kelvin)
        assertThat(scope.getSymbol("s", Kind.TYPE)).isEqualTo(longS)
        assertThat(scope.getSymbol("SS")).isNull()
        assertThat(scope.getSymbol("\uD801\uDC28", Kind.PACKAGE)).isEqualTo(supplementary)
    }

    private fun mockAstNode() = mock(AstNode::class.java)

    private fun createSymbol(scope: Scope, name: String, kind: Kind): Symbol {
        val node = mockAstNode()
        `when`(node.tokenValue).thenReturn(name)
        return Symbol(node, kind, scope, null)
    }

}
