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

import java.util.Deque;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword;
import org.sonar.plugins.plsqlopen.api.symbols.Scope;
import org.sonar.plugins.plsqlopen.api.symbols.Symbol;
import org.sonar.plsqlopen.annnotations.ConstantRemediation;
import com.sonar.sslr.api.AstNode;

@Rule(
    key = UnhandledUserDefinedExceptionCheck.CHECK_KEY,
    priority = Priority.CRITICAL
)
@ConstantRemediation("5min")
public class UnhandledUserDefinedExceptionCheck extends AbstractBaseCheck {
    public static final String CHECK_KEY = "UnhandledUserDefinedException";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.RAISE_STATEMENT);
    }

    @Override
    public void visitNode(AstNode node) {
        AstNode identifier = node.getFirstChild(PlSqlGrammar.VARIABLE_NAME);
        
        if (identifier == null) {
            return;
        }
        
        String identifierName = identifier.getTokenOriginalValue();
        Deque<Symbol> symbols = getContext().getCurrentScope().getSymbolsAcessibleInScope(identifierName);
        
        if (!symbols.isEmpty()) {
            boolean checkException = exceptionShouldBeChecked(symbols.getFirst());
            
            if (checkException && !isHandled(identifierName)) {
                getContext().createViolation(this, getLocalizedMessage(CHECK_KEY), node, identifierName);
            }
        }
    }
    
    private boolean exceptionShouldBeChecked(Symbol exceptionDeclaration) {
        AstNode scopeOfDeclaration = exceptionDeclaration.scope().tree();
        if (scopeOfDeclaration.is(PlSqlGrammar.CREATE_PACKAGE, PlSqlGrammar.CREATE_PACKAGE_BODY)) {
            return false;
        }
        
        return true;
    }
    
    private boolean isHandled(String identifierName) {
        Scope scope = getContext().getCurrentScope();
        
        Scope outerScope = scope;
        do {
            AstNode scopeNode = outerScope.tree();
            
            AstNode statements = scopeNode.getFirstChild(PlSqlGrammar.STATEMENTS_SECTION);
            if (statements != null) {
                for (AstNode handler : statements.getChildren(PlSqlGrammar.EXCEPTION_HANDLER)) {
                    if (handler.hasDirectChildren(PlSqlKeyword.OTHERS) && !handler.hasDescendant(PlSqlKeyword.SQLERRM)) {
                        return true;
                    }
                    
                    for (AstNode exceptionName : handler.getChildren(PlSqlGrammar.VARIABLE_NAME)) {
                        if (exceptionName.getTokenOriginalValue().equalsIgnoreCase(identifierName)) {
                            return true;
                        }
                    }
                }
            }
            
            outerScope = outerScope.outer();
        } while (outerScope != null);
        
        return false;
    }

}
