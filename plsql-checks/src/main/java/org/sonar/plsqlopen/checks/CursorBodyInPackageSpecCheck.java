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

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.plsqlopen.api.DmlGrammar;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.symbols.Symbol;
import org.sonar.plugins.plsqlopen.api.symbols.Symbol.Kind;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;

import com.sonar.sslr.api.AstNode;

@Rule(
    key = CursorBodyInPackageSpecCheck.CHECK_KEY,
    priority = Priority.MAJOR
)
@SqaleConstantRemediation("10min")
public class CursorBodyInPackageSpecCheck extends AbstractBaseCheck {

    public static final String CHECK_KEY = "CursorBodyInPackageSpec";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.CREATE_PACKAGE);
    }
    
    @Override
    public void visitNode(AstNode astNode) {
        for (Symbol cursor : getPlSqlContext().getCurrentScope().getSymbols(Kind.CURSOR)) {
            AstNode cursorDeclaration = cursor.declaration().getParent(); 
            if (cursorDeclaration.hasDirectChildren(DmlGrammar.SELECT_EXPRESSION)) {
                getPlSqlContext().createViolation(this, getLocalizedMessage(CHECK_KEY), cursorDeclaration, cursor.name());
            }
        }
    }

}
