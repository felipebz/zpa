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

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.sonar.sslr.ast.AstSelect;

import com.sonar.sslr.api.AstNode;

@Rule(
    key = SelectWithRownumAndOrderByCheck.CHECK_KEY,
    priority = Priority.BLOCKER,
    tags = Tags.BUG
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("20min")
@ActivatedByDefault
public class SelectWithRownumAndOrderByCheck extends AbstractBaseCheck {

    public static final String CHECK_KEY = "SelectWithRownumAndOrderBy";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.SELECT_EXPRESSION);
    }

    @Override
    public void visitNode(AstNode node) {
        if (!hasOrderByClause(node)) {
            return;
        }
        
        AstSelect whereClause = node.select().children(PlSqlGrammar.WHERE_CLAUSE);
        if (whereClause.isEmpty()) {
            return;
        }
        
        AstSelect whereComparisonConditions = whereClause.descendants(PlSqlGrammar.COMPARISON_EXPRESSION);
        if (whereComparisonConditions.isEmpty()) {
            return;
        }
        
        for (AstNode comparison : whereComparisonConditions) {
            AstSelect children = comparison.select().children(PlSqlGrammar.VARIABLE_NAME);
            for (AstNode child : children) {
                if ("rownum".equalsIgnoreCase(child.getTokenValue())) {
                    getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), child);
                }
            }
        }
    }

    private static boolean hasOrderByClause(AstNode node) {
        return node.hasDirectChildren(PlSqlGrammar.ORDER_BY_CLAUSE);
    }

}
