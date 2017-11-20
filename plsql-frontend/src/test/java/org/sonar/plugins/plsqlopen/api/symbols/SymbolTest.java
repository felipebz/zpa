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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.symbols.Symbol.Kind;

import com.sonar.sslr.api.AstNode;

public class SymbolTest {

    @Test
    public void testSymbol() {
        Scope scope = mock(Scope.class);
        Symbol symbol = createSymbol(scope, "foo", Kind.VARIABLE);
        
        assertThat(symbol.name()).isEqualTo("foo");
        assertThat(symbol.kind()).isEqualTo(Kind.VARIABLE);
        assertThat(symbol.scope()).isEqualTo(scope);
        assertThat(symbol.toString()).startsWith("Symbol{name='foo', kind=VARIABLE, scope=");
    }
    
    @Test
    public void getModifiers() {
        Scope scope = mock(Scope.class);
        Symbol symbol = createSymbol(scope, "foo", Kind.VARIABLE);
        
        AstNode node = mock(AstNode.class);
        List<AstNode> modifiers = new ArrayList<>();
        modifiers.add(node);
        symbol.addModifiers(modifiers);
        
        assertThat(symbol.modifiers()).containsExactly(node);
    }
    
    @Test
    public void hasModifier() {
        Scope scope = mock(Scope.class);
        Symbol symbol = createSymbol(scope, "foo", Kind.VARIABLE);
        
        AstNode node = mock(AstNode.class);
        when(node.getTokenOriginalValue()).thenReturn("foo");
        List<AstNode> modifiers = new ArrayList<>();
        modifiers.add(node);
        symbol.addModifiers(modifiers);
        
        assertThat(symbol.hasModifier("foo")).isTrue();
        assertThat(symbol.hasModifier("FOO")).isTrue();
        assertThat(symbol.hasModifier("bar")).isFalse();
    }
    
    private Symbol createSymbol(Scope scope, String name, Kind kind) {
        AstNode node = mock(AstNode.class);
        when(node.getTokenOriginalValue()).thenReturn(name);
        return new Symbol(node, kind, scope, null);
    }
    
}
