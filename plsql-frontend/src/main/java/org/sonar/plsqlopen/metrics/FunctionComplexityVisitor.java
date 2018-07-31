/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2018 Felipe Zorzo
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
package org.sonar.plsqlopen.metrics;

import java.util.ArrayList;
import java.util.List;

import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;

import com.sonar.sslr.api.AstNode;

public class FunctionComplexityVisitor extends ComplexityVisitor {
    
    private int numberOfFunctions;
    private List<Integer> functionComplexities = new ArrayList<>();

    private static final PlSqlGrammar[] METHOD_DECLARATION = { 
          PlSqlGrammar.CREATE_PROCEDURE,
          PlSqlGrammar.CREATE_FUNCTION, 
          PlSqlGrammar.PROCEDURE_DECLARATION,
          PlSqlGrammar.FUNCTION_DECLARATION };
            
    private int functionNestingLevel = 0;

    @Override
    public void visitNode(AstNode node) {
        if (node.is(METHOD_DECLARATION)) {
            functionNestingLevel++;
            numberOfFunctions++;
        }
        if (functionNestingLevel == 1) {
            super.visitNode(node);
        }
    }

    @Override
    public void leaveNode(AstNode node) {
        if (node.is(METHOD_DECLARATION)) {
            functionNestingLevel--;
            functionComplexities.add(getComplexity());
        }
    }
    
    public int getNumberOfFunctions() {
        return numberOfFunctions;
    }
    
    public List<Integer> getFunctionComplexities() {
        return functionComplexities;
    }
    
}
