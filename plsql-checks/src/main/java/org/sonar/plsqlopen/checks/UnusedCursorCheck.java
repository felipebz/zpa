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
import java.util.Set;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword;
import org.sonar.plugins.plsqlopen.api.symbols.Scope;
import org.sonar.plugins.plsqlopen.api.symbols.Symbol;
import org.sonar.plsqlopen.annnotations.ActivatedByDefault;
import org.sonar.plsqlopen.annnotations.ConstantRemediation;
import com.sonar.sslr.api.AstNode;

@Rule(
    key = UnusedCursorCheck.CHECK_KEY,
    priority = Priority.MAJOR,
    tags = Tags.UNUSED
)
@ConstantRemediation("2min")
@ActivatedByDefault
public class UnusedCursorCheck extends AbstractBaseCheck {

    public static final String CHECK_KEY = "UnusedCursor";

    @Override
    public void leaveFile(AstNode astNode) {
        Set<Scope> scopes = getContext().getSymbolTable().getScopes();
        for (Scope scope : scopes) {
            if (scope.tree().is(PlSqlGrammar.CREATE_PACKAGE)) {
                continue;
            }
            checkScope(scope);
        }
    }
    
    private void checkScope(Scope scope) {
        List<Symbol> symbols = scope.getSymbols(Symbol.Kind.CURSOR);
        for (Symbol symbol : symbols) {
            if (symbol.usages().isEmpty() && !symbol.declaration().getParent().hasDirectChildren(PlSqlKeyword.RETURN)) {
                getContext().createViolation(this, getLocalizedMessage(CHECK_KEY),
                        symbol.declaration().getParent(), symbol.declaration().getTokenOriginalValue());
            }
        }
    }

}
