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

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.plsqlopen.api.DmlGrammar;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.symbols.Scope;
import org.sonar.plugins.plsqlopen.api.symbols.Symbol;
import org.sonar.plugins.plsqlopen.api.symbols.Symbol.Kind;
import org.sonar.plsqlopen.annnotations.ActivatedByDefault;
import org.sonar.plsqlopen.annnotations.ConstantRemediation;
import com.sonar.sslr.api.AstNode;

@Rule(
    key = ColumnsShouldHaveTableNameCheck.CHECK_KEY,
    priority = Priority.MAJOR
)
@ConstantRemediation("2min")
@ActivatedByDefault
public class ColumnsShouldHaveTableNameCheck extends AbstractBaseCheck {

    public static final String CHECK_KEY = "ColumnsShouldHaveTableName";

    @Override
    public void init() {
        subscribeTo(DmlGrammar.SELECT_COLUMN);
    }

    @Override
    public void visitNode(AstNode node) {
        AstNode candidate = node.getFirstChild();
        if (candidate.getFirstChild() != null) {
            candidate = candidate.getFirstChild();
        }
        
        AstNode selectExpression = node.getParent();
        if (selectExpression.getFirstChild(DmlGrammar.FROM_CLAUSE).getChildren(DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE).size() > 1 &&
                candidate.is(PlSqlGrammar.IDENTIFIER_NAME) && 
                !candidate.hasDirectChildren(PlSqlGrammar.NON_RESERVED_KEYWORD)) {
            
            Scope scope = getContext().getCurrentScope();
            Symbol symbol = null;
            if (scope != null) {
                symbol = scope.getSymbol(candidate.getTokenOriginalValue(), Kind.VARIABLE, Kind.PARAMETER);
            }
            
            if (symbol == null) {
                getContext().createViolation(this, getLocalizedMessage(CHECK_KEY), candidate, candidate.getTokenOriginalValue());
            }
        }
    }

}
