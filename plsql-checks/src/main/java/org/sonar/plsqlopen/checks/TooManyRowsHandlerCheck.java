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

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plsqlopen.annnotations.ActivatedByDefault;
import org.sonar.plsqlopen.annnotations.ConstantRemediation;
import com.sonar.sslr.api.AstNode;

@Rule(
    key = TooManyRowsHandlerCheck.CHECK_KEY,
    priority = Priority.CRITICAL
)
@ConstantRemediation("5min")
@ActivatedByDefault
public class TooManyRowsHandlerCheck extends AbstractBaseCheck {
    public static final String CHECK_KEY = "TooManyRowsHandler";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.EXCEPTION_HANDLER);
    }

    @Override
    public void visitNode(AstNode node) {
        // is a TOO_MANY_ROWS handler
        List<AstNode> exceptions = node.getChildren(PlSqlGrammar.VARIABLE_NAME);
        
        for (AstNode exception : exceptions) {
            AstNode child = exception.getFirstChild();
            
            if (child.is(PlSqlGrammar.IDENTIFIER_NAME) && 
                    "TOO_MANY_ROWS".equalsIgnoreCase(child.getTokenValue())) {
                // and have only one NULL_STATEMENT
                List<AstNode> children = node.getFirstChild(PlSqlGrammar.STATEMENTS).getChildren();
                if (children.size() == 1 && children.get(0).getFirstChild().is(PlSqlGrammar.NULL_STATEMENT)) {
                    getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), node);
                }
            }
        }
    }
}

