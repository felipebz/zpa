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
import org.sonar.check.RuleProperty;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plsqlopen.annnotations.ActivatedByDefault;
import org.sonar.plsqlopen.annnotations.ConstantRemediation;
import com.sonar.sslr.api.AstNode;

@Rule(
    key = QueryWithoutExceptionHandlingCheck.CHECK_KEY,
    priority = Priority.CRITICAL
)
@ConstantRemediation("20min")
@ActivatedByDefault
public class QueryWithoutExceptionHandlingCheck extends AbstractBaseCheck {
    public static final String CHECK_KEY = "QueryWithoutExceptionHandling";
    
    @RuleProperty (
        key = "strict",
        defaultValue = "true"
    )
    public boolean strictMode = true;

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.SELECT_STATEMENT);
    }
    
    @Override
    public void visitNode(AstNode node) {
        if (strictMode) {
            AstNode parentBlock = node.getFirstAncestor(PlSqlGrammar.STATEMENTS_SECTION);
            
            if (!hasExceptionHandling(parentBlock)) {
                getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), node);
            }
        } else {
            if (!getContext().getCurrentScope().hasExceptionHandler()) {
                getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), node);
            }
        }
    }

    private static boolean hasExceptionHandling(AstNode node) {
        return node != null && node.hasDirectChildren(PlSqlGrammar.EXCEPTION_HANDLER);
    }

}
