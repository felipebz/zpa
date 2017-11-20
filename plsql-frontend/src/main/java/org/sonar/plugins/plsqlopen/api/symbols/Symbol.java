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

import java.util.LinkedList;
import java.util.List;

import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType.Type;

import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;

public class Symbol {
    public enum Kind {
        VARIABLE("variable"),
        PARAMETER("parameter"),
        CURSOR("cursor");

        private final String value;

        Kind(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private final String name;
    private final AstNode declaration;
    private Kind kind;
    private Scope scope;
    private List<AstNode> usages = new LinkedList<>();
    private List<AstNode> modifiers = new LinkedList<>();
    private PlSqlType type;

    public Symbol(AstNode declaration, Kind kind, Scope scope, PlSqlType type) {
        this.declaration = declaration;
        this.name = declaration.getTokenOriginalValue();
        this.kind = kind;
        this.scope = scope;
        if (type != null) {
            this.type = type;
        } else {
            this.type = new PlSqlType(Type.UNKNOWN);
        }
    }

    public ImmutableList<AstNode> modifiers() {
        return ImmutableList.copyOf(modifiers);
    }

    public boolean hasModifier(String modifier) {
        for (AstNode syntaxToken : modifiers) {
            if (syntaxToken.getTokenOriginalValue().equalsIgnoreCase(modifier)) {
                return true;
            }
        }
        return false;
    }

    public void addModifiers(List<AstNode> modifiers) {
        this.modifiers.addAll(modifiers);
    }

    public void addUsage(AstNode usage){
        usages.add(usage);
    }

    public List<AstNode> usages(){
        return usages;
    }

    public Scope scope() {
        return scope;
    }

    public String name() {
        return name;
    }

    public AstNode declaration() {
        return declaration;
    }

    public boolean is(Symbol.Kind kind) {
        return kind.equals(this.kind);
    }

    public boolean called(String name) {
        return name.equalsIgnoreCase(this.name);
    }

    public Kind kind() {
        return kind;
    }
    
    public PlSqlType type() {
        return type;
    }

    @Override
    public String toString() {
        return "Symbol{" +
                "name='" + name + '\'' +
                ", kind=" + kind +
                ", scope=" + scope +
                '}';
    }
}
