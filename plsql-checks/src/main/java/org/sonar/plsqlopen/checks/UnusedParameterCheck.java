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
package org.sonar.plsqlopen.checks;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.plsqlopen.api.annnotations.ActivatedByDefault;
import org.sonar.plugins.plsqlopen.api.annnotations.ConstantRemediation;
import org.sonar.plugins.plsqlopen.api.DmlGrammar;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword;
import org.sonar.plugins.plsqlopen.api.symbols.Scope;
import org.sonar.plugins.plsqlopen.api.symbols.Symbol;

import com.sonar.sslr.api.AstNode;

@Rule(
    key = UnusedParameterCheck.CHECK_KEY,
    priority = Priority.MAJOR,
    tags = Tags.UNUSED
)
@ConstantRemediation("30min")
@ActivatedByDefault
public class UnusedParameterCheck extends AbstractBaseCheck {

    public static final String CHECK_KEY = "UnusedParameter";

    @RuleProperty (
            key = "ignoreMethods",
            defaultValue = ""
        )
    public String ignoreMethods = "";
	private Pattern ignoreRegex;
    
    
    @Override
    public void init() {
    	if(!"".equals(ignoreMethods)) {
    		ignoreRegex = Pattern.compile(ignoreMethods);
    	}
    }
    
    @Override
    public void leaveFile(AstNode astNode) {
        Set<Scope> scopes = getContext().getSymbolTable().getScopes();
        for (Scope scope : scopes) {
            // ignore procedure/function specification and overriding members
            if (scope.tree().is(PlSqlGrammar.PROCEDURE_DECLARATION, PlSqlGrammar.FUNCTION_DECLARATION, PlSqlGrammar.TYPE_CONSTRUCTOR)) {
                // is specification?
                if (!scope.tree().hasDirectChildren(PlSqlGrammar.STATEMENTS_SECTION)) {
                    continue;
                }

                // is overriding something?
                AstNode inheritanceClause = scope.tree().getParent().getFirstChild(PlSqlGrammar.INHERITANCE_CLAUSE);
                if (inheritanceClause != null && inheritanceClause.getFirstChild().getType() != PlSqlKeyword.NOT &&
                    inheritanceClause.hasDirectChildren(PlSqlKeyword.OVERRIDING)) {
                    continue;
                }
            }
            
            // cursor declaration (without implementation)
            if (scope.tree().is(PlSqlGrammar.CURSOR_DECLARATION) &&
                !scope.tree().hasDirectChildren(DmlGrammar.SELECT_EXPRESSION)) {
                continue;
            }
            
            // ignore methods by name
            if(ignoreRegex != null && scope.tree().is(PlSqlGrammar.PROCEDURE_DECLARATION, PlSqlGrammar.FUNCTION_DECLARATION,
                    PlSqlGrammar.CREATE_PROCEDURE, PlSqlGrammar.CREATE_FUNCTION) && ignoreRegex.matcher(scope.identifier()).matches()) {
            	continue;
            }
            
            checkScope(scope);
        }
    }
    
    private void checkScope(Scope scope) {
        List<Symbol> symbols = scope.getSymbols(Symbol.Kind.PARAMETER);
        for (Symbol symbol : symbols) {
            if (symbol.usages().isEmpty()) {
                addIssue(symbol.declaration().getParent(), getLocalizedMessage(CHECK_KEY),
                        symbol.declaration().getTokenOriginalValue());
            }
        }
    }

}
