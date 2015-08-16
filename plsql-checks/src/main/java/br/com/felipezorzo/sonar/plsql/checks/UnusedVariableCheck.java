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
package br.com.felipezorzo.sonar.plsql.checks;

import java.util.ArrayDeque;
import java.util.Deque;

import javax.annotation.Nullable;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.PlSqlKeyword;
import br.com.felipezorzo.sonar.plsql.checks.scope.Scope;
import br.com.felipezorzo.sonar.plsql.checks.scope.Variable;

@Rule(
    key = UnusedVariableCheck.CHECK_KEY,
    priority = Priority.MAJOR,
    tags = Tags.UNUSED
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("2min")
@ActivatedByDefault
public class UnusedVariableCheck extends AbstractBaseCheck {

    public static final String CHECK_KEY = "UnusedVariable";
    private static final AstNodeType[] scopeHolders = { 
            PlSqlGrammar.ANONYMOUS_BLOCK,
            PlSqlGrammar.CREATE_PROCEDURE,
            PlSqlGrammar.PROCEDURE_DECLARATION,
            PlSqlGrammar.CREATE_FUNCTION,
            PlSqlGrammar.FUNCTION_DECLARATION,
            PlSqlGrammar.CREATE_PACKAGE_BODY,
            PlSqlGrammar.BLOCK_STATEMENT,
            PlSqlGrammar.FOR_STATEMENT
            };

    private Deque<Scope> scopes = new ArrayDeque<>();

    @Override
    public void init() {
        subscribeTo(scopeHolders);
        subscribeTo(PlSqlGrammar.VARIABLE_DECLARATION,
                    PlSqlGrammar.PRIMARY_EXPRESSION);
    }

    @Override
    public void visitNode(AstNode node) {
        if (node.is(scopeHolders)) {
            
            scopes.push(new Scope(getCurrentScope()));
            
            if (node.is(PlSqlGrammar.FOR_STATEMENT)) {
                AstNode identifier = node.getFirstChild(PlSqlKeyword.FOR).getNextSibling();
                getCurrentScope().declareLocalVariable(identifier, 1);
            }
            
        } else if (!scopes.isEmpty()) {

            if (node.is(PlSqlGrammar.VARIABLE_DECLARATION)) {
                getCurrentScope().declareLocalVariable(node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME));
            } else if (node.is(PlSqlGrammar.PRIMARY_EXPRESSION)) {
                AstNode identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME);
                if (identifier != null) {
                    getCurrentScope().useVariable(identifier);
                }
            }
            
        }
    }
    
    @Override
    public void leaveNode(AstNode astNode) {
      if (astNode.is(scopeHolders)) {
        reportUnusedVariable();
        scopes.pop();
      }
    }
    
    @Override
    public void leaveFile(@Nullable AstNode astNode) {
        scopes.clear();
    }
    
    private void reportUnusedVariable() {
        for (Variable localVar : getCurrentScope().getLocalVariables().values()) {

            if (localVar.getUsage() == 0) {
                getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY),
                        localVar.getDeclaration(), localVar.getDeclaration().getTokenOriginalValue());
            }
        }
    }

    private Scope getCurrentScope() {
        return scopes.peek();
    }

}
