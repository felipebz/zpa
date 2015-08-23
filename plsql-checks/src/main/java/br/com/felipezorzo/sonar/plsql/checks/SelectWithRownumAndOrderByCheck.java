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

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.sonar.sslr.ast.AstSelect;

import com.sonar.sslr.api.AstNode;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;

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
            AstSelect children = comparison.select().children(PlSqlGrammar.PRIMARY_EXPRESSION);
            for (AstNode child : children) {
                if ("rownum".equalsIgnoreCase(child.getTokenValue())) {
                    getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), child);
                }
            }
        }
    }

    private boolean hasOrderByClause(AstNode node) {
        return node.hasDirectChildren(PlSqlGrammar.ORDER_BY_CLAUSE);
    }

}
