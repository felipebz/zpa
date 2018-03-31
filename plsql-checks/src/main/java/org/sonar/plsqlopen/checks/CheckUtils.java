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
package org.sonar.plsqlopen.checks;

import java.util.List;

import javax.annotation.Nullable;

import org.sonar.plsqlopen.matchers.MethodMatcher;
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
    
    private static final AstNodeType[] NULL_LITERAL = { PlSqlGrammar.NULL_LITERAL };
    
    private static final AstNodeType[] CHARACTER_LITERAL = { PlSqlGrammar.CHARACTER_LITERAL };
    
    private static final AstNodeType[] WHEN = { PlSqlKeyword.WHEN };
    
    private static final MethodMatcher NVL_MATCHER = MethodMatcher.create().name("nvl").addParameter().addParameter();

    private static final MethodMatcher RAISE_APPLICATION_ERROR_MATCHER = MethodMatcher.create().name("raise_application_error").withNoParameterConstraint();
    
    private CheckUtils() {
    }
    
    public static AstNodeType[] getTerminationStatements() {
        return TERMINATION_STATEMENTS.clone();
    }
    
    public static boolean isNullLiteralOrEmptyString(AstNode node) {
        if (node != null) {
            if (node.hasDirectChildren(NULL_LITERAL)) {
                return true;
            }

            if (isEmptyString(node)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isEmptyString(AstNode node) {
        AstNode characterLiteral = node.getFirstChild(CHARACTER_LITERAL);
        if (characterLiteral != null && "''".equals(characterLiteral.getTokenValue())) {
            return true;
        }

        return false;
    }

    public static boolean equalNodes(AstNode node1, AstNode node2) {
        AstNode first = skipExpressionsWithoutEffect(node1);
        AstNode second = skipExpressionsWithoutEffect(node2);
        
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
    
    public static AstNode skipExpressionsWithoutEffect(AstNode node) {
        AstNode newNode = skipParenthesis(node);
        newNode = skipNvlWithNull(newNode);
        return newNode;
    }
    
    public static AstNode skipParenthesis(AstNode node) {
        if (node.getType() == PlSqlGrammar.BRACKED_EXPRESSION) {
            return node.getChildren().get(1);
        }
        return node;
    }
    
    public static AstNode skipNvlWithNull(AstNode node) {
        if (NVL_MATCHER.matches(node)) {
            List<AstNode> arguments = NVL_MATCHER.getArgumentsValues(node);
            if (isNullLiteralOrEmptyString(arguments.get(1))) {
                return arguments.get(0);
            }
        }
        return node;
    }
    
    public static boolean isTerminationStatement(AstNode node) {
        return (node.is(TERMINATION_STATEMENTS) || RAISE_APPLICATION_ERROR_MATCHER.matches(node)) && !node.hasDirectChildren(WHEN);
    }
    
    public static boolean isProgramUnit(@Nullable AstNode node) {
        return node != null && node.is(PROGRAM_UNITS);
    }
}
