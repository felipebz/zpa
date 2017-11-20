/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2017 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.symbols;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.symbols.Symbol.Kind;

import com.sonar.sslr.api.AstNode;

public class ScopeTest {

    @Test
    public void testScope() {
        AstNode node = mock(AstNode.class);
        Scope scope = new Scope(null, node, false, false);
        assertThat(scope.outer()).isNull();
        assertThat(scope.tree()).isEqualTo(node);
        assertThat(scope.isAutonomousTransaction()).isFalse();
    }
    
    @Test
    public void getSymbolsInScope() {
        Scope scope = new Scope(null, null, false, false);
        
        Symbol symbol1 = createSymbol(scope, "foo", Kind.VARIABLE);
        scope.addSymbol(symbol1);
        
        Symbol symbol2 = createSymbol(scope, "bar", Kind.VARIABLE);
        scope.addSymbol(symbol2);
        
        assertThat(scope.getSymbols()).containsExactly(symbol1, symbol2);
    }
    
    @Test
    public void getSymbolsByKind() {
        Scope scope = new Scope(null, null, false, false);
        
        Symbol symbol1 = createSymbol(scope, "foo", Kind.VARIABLE);
        scope.addSymbol(symbol1);
        
        Symbol symbol2 = createSymbol(scope, "bar", Kind.CURSOR);
        scope.addSymbol(symbol2);
        
        assertThat(scope.getSymbols(Kind.VARIABLE)).containsExactly(symbol1);
        assertThat(scope.getSymbols(Kind.CURSOR)).containsExactly(symbol2);
    }
    
    @Test
    public void getSymbolsAcessibleInScope() {
        Scope scope = new Scope(null, null, false, false);
        
        Symbol symbol1 = createSymbol(scope, "foo", Kind.VARIABLE);
        scope.addSymbol(symbol1);
        
        Symbol symbol2 = createSymbol(scope, "bar", Kind.VARIABLE);
        scope.addSymbol(symbol2);
        
        assertThat(scope.getSymbolsAcessibleInScope("foo")).containsExactly(symbol1);
        assertThat(scope.getSymbolsAcessibleInScope("foo", Kind.VARIABLE)).containsExactly(symbol1);
        assertThat(scope.getSymbolsAcessibleInScope("foo", Kind.CURSOR)).isEmpty();
    }
    
    @Test
    public void getSymbolsAcessibleInScopeConsideringOuterScope() {
        Scope outerScope = new Scope(null, null, false, false);
        Symbol symbol1 = createSymbol(outerScope, "foo", Kind.VARIABLE);
        outerScope.addSymbol(symbol1);
        
        Scope innerScope = new Scope(outerScope, null, false, false);
        Symbol symbol2 = createSymbol(innerScope, "bar", Kind.VARIABLE);
        innerScope.addSymbol(symbol2);
        
        assertThat(innerScope.getSymbolsAcessibleInScope("foo")).containsExactly(symbol1);
        assertThat(innerScope.getSymbolsAcessibleInScope("foo", Kind.VARIABLE)).containsExactly(symbol1);
        assertThat(innerScope.getSymbolsAcessibleInScope("foo", Kind.CURSOR)).isEmpty();
    }
    
    @Test
    public void getSymbol() {
        Scope scope = new Scope(null, null, false, false);
        
        Symbol symbol1 = createSymbol(scope, "foo", Kind.VARIABLE);
        scope.addSymbol(symbol1);
        
        Symbol symbol2 = createSymbol(scope, "bar", Kind.VARIABLE);
        scope.addSymbol(symbol2);
        
        assertThat(scope.getSymbol("foo")).isEqualTo(symbol1);
        assertThat(scope.getSymbol("foo", Kind.VARIABLE)).isEqualTo(symbol1);
        assertThat(scope.getSymbol("foo", Kind.CURSOR)).isNull();
        assertThat(scope.getSymbol("baz")).isNull();
    }
    
    @Test
    public void getSymbolConsideringOuterScope() {
        Scope outerScope = new Scope(null, null, false, false);
        Symbol symbol1 = createSymbol(outerScope, "foo", Kind.VARIABLE);
        outerScope.addSymbol(symbol1);
        
        Scope innerScope = new Scope(outerScope, null, false, false);
        Symbol symbol2 = createSymbol(innerScope, "bar", Kind.VARIABLE);
        innerScope.addSymbol(symbol2);
        
        assertThat(innerScope.getSymbol("foo")).isEqualTo(symbol1);
        assertThat(innerScope.getSymbol("foo", Kind.VARIABLE)).isEqualTo(symbol1);
        assertThat(innerScope.getSymbol("foo", Kind.CURSOR)).isNull();
        assertThat(innerScope.getSymbol("baz")).isNull();
    }
    
    private Symbol createSymbol(Scope scope, String name, Kind kind) {
        AstNode node = mock(AstNode.class);
        when(node.getTokenOriginalValue()).thenReturn(name);
        return new Symbol(node, kind, scope, null);
    }
    
}
