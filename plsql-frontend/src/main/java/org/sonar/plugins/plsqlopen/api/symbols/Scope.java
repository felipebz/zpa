/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015 Felipe Zorzo
 * felipe.b.zorzo@gmail.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.plsqlopen.api.symbols;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import com.sonar.sslr.api.AstNode;

public class Scope {

    private final Scope outer;
    private final AstNode node;
    protected List<Symbol> symbols = new ArrayList<>();

    public Scope(Scope outer, AstNode node) {
        this.outer = outer;
        this.node = node;
    }

    public AstNode tree() {
        return node;
    }

    public Scope outer() {
        return outer;
    }
    
    public List<Symbol> getSymbols() {
        return symbols;
    }

    /**
     * @param kind of the symbols to look for
     * @return the symbols corresponding to the given kind
     */
    public List<Symbol> getSymbols(Symbol.Kind kind) {
        List<Symbol> result = new LinkedList<>();
        for (Symbol symbol : symbols) {
            if (symbol.is(kind)) {
                result.add(symbol);
            }
        }
        return result;
    }
    
    public Deque<Symbol> getSymbolsAcessibleInScope(String name, Symbol.Kind ... kinds) {
        Deque<Symbol> result = new ArrayDeque<>();
        List<Symbol.Kind> kindList = Arrays.asList(kinds);
        Scope scope = this;
        while (scope != null) {
            for (Symbol s : scope.getSymbols()) {
                if (s.called(name) && (kindList.isEmpty() || kindList.contains(s.kind()))) {
                    result.add(s);
                }
            }
            scope = scope.outer();
        }

        return result;
    }

    public void addSymbol(Symbol symbol) {
        symbols.add(symbol);
    }

    @Nullable
    public Symbol getSymbol(String name, Symbol.Kind ... kinds) {
        List<Symbol.Kind> kindList = Arrays.asList(kinds);
        Scope scope = this;
        while (scope != null) {
            for (Symbol s : scope.getSymbols()) {
                if (s.called(name) && (kindList.isEmpty() || kindList.contains(s.kind()))) {
                    return s;
                }
            }
            scope = scope.outer();
        }

        return null;
    }
}
