/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2016 Felipe Zorzo
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
package org.sonar.plsqlopen.checks;

import java.util.List;

import javax.annotation.Nullable;

import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;

public class CheckUtils {

    private static final AstNodeType[] TERMINATION_STATEMENTS = {
            PlSqlGrammar.RETURN_STATEMENT,
            PlSqlGrammar.EXIT_STATEMENT,
            PlSqlGrammar.CONTINUE_STATEMENT,
            PlSqlGrammar.RAISE_STATEMENT};
    
    private static final AstNodeType[] PROGRAM_UNITS = { 
            PlSqlGrammar.ANONYMOUS_BLOCK,
            PlSqlGrammar.CREATE_PROCEDURE,
            PlSqlGrammar.PROCEDURE_DECLARATION,
            PlSqlGrammar.CREATE_FUNCTION,
            PlSqlGrammar.FUNCTION_DECLARATION,
            PlSqlGrammar.CREATE_PACKAGE_BODY};
    
    private CheckUtils() {
    }
    
    public static AstNodeType[] getTerminationStatements() {
        return TERMINATION_STATEMENTS.clone();
    }
    
    public static boolean isNullLiteralOrEmptyString(AstNode node) {
        if (node != null) {
            if (node.hasDirectChildren(PlSqlGrammar.NULL_LITERAL)) {
                return true;
            }

            if (isEmptyString(node)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isEmptyString(AstNode node) {
        AstNode characterLiteral = node.getFirstChild(PlSqlGrammar.CHARACTER_LITERAL);
        if (characterLiteral != null && "''".equals(characterLiteral.getTokenValue())) {
            return true;
        }

        return false;
    }

    public static boolean equalNodes(AstNode node1, AstNode node2) {
        AstNode first = skipParenthesis(node1);
        AstNode second = skipParenthesis(node2);
        
        if (!first.getType().equals(second.getType()) || first.getNumberOfChildren() != second.getNumberOfChildren()) {
            return false;
        }

        if (first.getNumberOfChildren() == 0) {
            return first.getToken().getValue().equals(second.getToken().getValue());
        }

        List<AstNode> children1 = first.getChildren();
        List<AstNode> children2 = second.getChildren();
        for (int i = 0; i < children1.size(); i++) {
            if (!equalNodes(children1.get(i), children2.get(i))) {
                return false;
            }
        }
        return true;
    }
    
    public static AstNode skipParenthesis(AstNode node) {
        if (node.is(PlSqlGrammar.BRACKED_EXPRESSION)) {
            return node.getChildren().get(1);
        }
        return node;
    }
    
    public static boolean isTerminationStatement(AstNode node) {
        return node.is(TERMINATION_STATEMENTS) && !node.hasDirectChildren(PlSqlKeyword.WHEN);
    }
    
    public static boolean isProgramUnit(@Nullable AstNode node) {
        return node != null && node.is(PROGRAM_UNITS);
    }
}
