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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.sonar.sslr.api.AstNode;

public class SymbolTableImpl implements SymbolTable {

    private List<Symbol> symbols = new ArrayList<>();
    private Set<Scope> scopes = new LinkedHashSet<>();

    public void addScope(Scope scope){
        scopes.add(scope);
    }

    @Override
    public ImmutableSet<Scope> getScopes(){
        return ImmutableSet.copyOf(scopes);
    }

    @Nullable
    @Override
    public Scope getScopeFor(AstNode node) {
        for (Scope scope : scopes) {
            if (scope.tree().equals(node)) {
                return scope;
            }
        }
        return null;
    }
    
    @Nullable
    @Override
    public Scope getScopeForSymbol(AstNode node) {
        Symbol symbol = getSymbolFor(node);
        if (symbol != null) {
            return symbol.scope();
        }
        return null;
    }
    
    @Nullable
    @Override
    public Symbol getSymbolFor(AstNode node) {
        for (Symbol symbol : symbols) {
            if (symbol.declaration().equals(node)) {
                return symbol;
            }
        }
        return null;
    }

    public Symbol declareSymbol(AstNode name, Symbol.Kind kind, Scope scope, PlSqlType type) {
        Symbol symbol = new Symbol(name, kind, scope, type);
        symbols.add(symbol);
        scope.addSymbol(symbol);
        return symbol;
    }

    /**
     * Returns all symbols in script
     */
    @Override
    public List<Symbol> getSymbols() {
        return ImmutableList.copyOf(symbols);
    }

    /**
     *
     * @param kind kind of symbols to look for
     * @return list of symbols with the given kind
     */
    @Override
    public List<Symbol> getSymbols(Symbol.Kind kind) {
        List<Symbol> result = new ArrayList<>();
        for (Symbol symbol : getSymbols()){
            if (kind.equals(symbol.kind())){
                result.add(symbol);
            }
        }
        return result;
    }

    /**
     *
     * @param name name of symbols to look for
     * @return list of symbols with the given name
     */
    @Override
    public List<Symbol> getSymbols(String name) {
        List<Symbol> result = new ArrayList<>();
        for (Symbol symbol : getSymbols()){
            if (name.equalsIgnoreCase(symbol.name())){
                result.add(symbol);
            }
        }
        return result;
    }

}
