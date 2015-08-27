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
package br.com.felipezorzo.sonar.plsql.checks.scope;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.sonar.sslr.api.AstNode;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;

public class Scope {

    private Map<String, Variable> localVariables = new HashMap<>();
    private Scope outerScope;
    
    public Scope(Scope outerScope) {
        this.outerScope = outerScope;
    }
    
    public void declareLocalVariable(AstNode node) {
        declareLocalVariable(node, 0);
    }
    
    public void declareLocalVariable(AstNode node, int usage) {
        Preconditions.checkArgument(node.is(PlSqlGrammar.IDENTIFIER_NAME));
        localVariables.put(node.getTokenOriginalValue().toUpperCase(), new Variable(node, usage));
    }

    public Map<String, Variable> getLocalVariables() {
        return localVariables;
    }
    
    @Nullable
    public Variable getVariableDeclaration(AstNode node) {
        String variableName = node.getTokenOriginalValue().toUpperCase();
        
        Scope scope = this;
        while (scope != null) {
            Map<String, Variable> variables = scope.getLocalVariables();
            if (variables.containsKey(variableName)) {
                return variables.get(variableName);
            }
            scope = scope.outerScope;
        }
        return null;
    }

    public void useVariable(AstNode node) {
        Variable variable = getVariableDeclaration(node);
        if (variable != null) {
            variable.increaseUsage();
        }
    }

}
