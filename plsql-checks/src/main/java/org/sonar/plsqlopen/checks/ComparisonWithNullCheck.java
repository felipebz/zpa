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
import org.sonar.plugins.plsqlopen.api.ConditionsGrammar;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.PlSqlPunctuator;
import org.sonar.plsqlopen.annnotations.ActivatedByDefault;
import org.sonar.plsqlopen.annnotations.ConstantRemediation;
import com.sonar.sslr.api.AstNode;

@Rule(
    key = ComparisonWithNullCheck.CHECK_KEY,
    priority = Priority.BLOCKER,
    tags = Tags.BUG
)
@ConstantRemediation("5min")
@ActivatedByDefault
public class ComparisonWithNullCheck extends AbstractBaseCheck {
    public static final String CHECK_KEY = "ComparisonWithNull";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.COMPARISON_EXPRESSION);
    }

    @Override
    public void visitNode(AstNode node) {
        List<AstNode> children = node.getChildren(PlSqlGrammar.LITERAL);
        for (AstNode child : children) {
            if (CheckUtils.isNullLiteralOrEmptyString(child)) {
                String suggestion;
                AstNode operator = node.getFirstChild(ConditionsGrammar.RELATIONAL_OPERATOR);
                if (operator.getFirstChild().is(PlSqlPunctuator.EQUALS)) {
                    suggestion = "IS NULL";
                } else {
                    suggestion = "IS NOT NULL";  
                }
                
                getContext().createViolation(this, getLocalizedMessage(CHECK_KEY), node, suggestion);
                continue;
            }
        }
    }
}
