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

public class SymbolTableImplTest {

    @Test
    public void verifyScopes() {
        Scope scope1 = mock(Scope.class);
        Scope scope2 = mock(Scope.class);
        
        SymbolTableImpl symbolTable = new SymbolTableImpl();
        symbolTable.addScope(scope1);
        symbolTable.addScope(scope2);
        
        assertThat(symbolTable.getScopes()).containsExactly(scope1, scope2);
    }
    
    @Test
    public void doNotReturnScopeIfNotIdentified() {
        SymbolTableImpl symbolTable = new SymbolTableImpl();    
        assertThat(symbolTable.getScopeFor(null)).isNull();
    }
    
    @Test
    public void returnScopeForSymbol() {
        AstNode node1 = mock(AstNode.class);
        Scope scope1 = new Scope(null, node1, false, false);
        
        AstNode node2 = mock(AstNode.class);
        Scope scope2 = new Scope(null, node2, false, false);
        
        SymbolTableImpl symbolTable = new SymbolTableImpl();
        symbolTable.addScope(scope1);
        symbolTable.addScope(scope2);
        
        assertThat(symbolTable.getScopeFor(node1)).isEqualTo(scope1);
        assertThat(symbolTable.getScopeFor(node2)).isEqualTo(scope2);
    }
    
    @Test
    public void doNotReturnSymbolIfNotIdentified() {
        SymbolTableImpl symbolTable = new SymbolTableImpl();
        assertThat(symbolTable.getSymbolFor(null)).isNull();
    }
    
    @Test
    public void returnSymbolForNode() {
        AstNode node = mock(AstNode.class);
        AstNode node2 = mock(AstNode.class);
        Scope scope = new Scope(null, null, false, false);
        
        SymbolTableImpl symbolTable = new SymbolTableImpl();
        Symbol symbol = symbolTable.declareSymbol(node, Kind.CURSOR, scope, null);
        
        assertThat(symbolTable.getSymbolFor(node)).isEqualTo(symbol);
        assertThat(symbolTable.getSymbolFor(node2)).isNull();
    }
    
    @Test
    public void doNotReturnScopeForSymbolIfNotIdentified() {
        SymbolTableImpl symbolTable = new SymbolTableImpl();
        assertThat(symbolTable.getScopeForSymbol(null)).isNull();
    }
    
    @Test
    public void returnScopeForSymbolForNode() {
        AstNode node = mock(AstNode.class);
        AstNode node2 = mock(AstNode.class);
        Scope scope = new Scope(null, null, false, false);
        
        SymbolTableImpl symbolTable = new SymbolTableImpl();
        symbolTable.declareSymbol(node, Kind.CURSOR, scope, null);
        
        assertThat(symbolTable.getScopeForSymbol(node)).isEqualTo(scope);
        assertThat(symbolTable.getScopeForSymbol(node2)).isNull();
    }
    
    @Test
    public void getSymbolsByKind() {
        Scope scope = new Scope(null, null, false, false);
        
        SymbolTableImpl symbolTable = new SymbolTableImpl();
        Symbol symbol1 = symbolTable.declareSymbol(mock(AstNode.class), Kind.CURSOR, scope, null);
        Symbol symbol2 = symbolTable.declareSymbol(mock(AstNode.class), Kind.VARIABLE, scope, null);
        
        assertThat(symbolTable.getSymbols(Kind.CURSOR)).containsExactly(symbol1);
        assertThat(symbolTable.getSymbols(Kind.VARIABLE)).containsExactly(symbol2);
        assertThat(symbolTable.getSymbols(Kind.PARAMETER)).isEmpty();
    }
    
    @Test
    public void getSymbolsByName() {
        Scope scope = new Scope(null, null, false, false);
        
        AstNode node1 = mock(AstNode.class);
        when(node1.getTokenOriginalValue()).thenReturn("foo");
        
        AstNode node2 = mock(AstNode.class);
        when(node2.getTokenOriginalValue()).thenReturn("FOO");
        
        SymbolTableImpl symbolTable = new SymbolTableImpl();
        Symbol symbol1 = symbolTable.declareSymbol(node1, Kind.CURSOR, scope, null);
        Symbol symbol2 = symbolTable.declareSymbol(node2, Kind.VARIABLE, scope, null);
        
        assertThat(symbolTable.getSymbols("foo")).containsExactly(symbol1, symbol2);
        assertThat(symbolTable.getSymbols("bar")).isEmpty();
    }
    
}
