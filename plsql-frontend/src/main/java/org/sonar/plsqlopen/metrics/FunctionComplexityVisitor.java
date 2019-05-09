/*
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
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

import com.sonar.sslr.api.AstNode;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;

public class FunctionComplexityVisitor extends ComplexityVisitor {

    private int numberOfFunctions;

    private static final PlSqlGrammar[] METHOD_DECLARATION = { 
          PlSqlGrammar.CREATE_PROCEDURE,
          PlSqlGrammar.CREATE_FUNCTION, 
          PlSqlGrammar.PROCEDURE_DECLARATION,
          PlSqlGrammar.FUNCTION_DECLARATION };

    @Override
    public void visitNode(AstNode node) {
        if (node.is(METHOD_DECLARATION)) {
            numberOfFunctions++;
        }
    }
    
    public int getNumberOfFunctions() {
        return numberOfFunctions;
    }
    
}
