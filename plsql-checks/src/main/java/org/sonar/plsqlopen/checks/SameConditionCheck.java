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

import java.util.ArrayList;
import java.util.List;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword;
import org.sonar.plsqlopen.annnotations.ActivatedByDefault;
import org.sonar.plsqlopen.annnotations.ConstantRemediation;
import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;

@Rule(
    key = SameConditionCheck.CHECK_KEY,
    priority = Priority.BLOCKER,
    tags = Tags.BUG
)
@ConstantRemediation("5min")
@ActivatedByDefault
public class SameConditionCheck extends AbstractBaseCheck {
    public static final String CHECK_KEY = "SameCondition";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.AND_EXPRESSION);
        subscribeTo(PlSqlGrammar.OR_EXPRESSION);
    }
    
    @Override
    public void visitNode(AstNode node) {
        List<AstNode> expressions = getExpressions(node);
        findSameConditions(expressions);
    }

    private static List<AstNode> getExpressions(AstNode node) {
        List<AstNode> expressions = new ArrayList<>();
        for (AstNode subnode : node.getChildren()) {
            if (!subnode.is(PlSqlKeyword.AND, PlSqlKeyword.OR)) {
                expressions.add(subnode);
            }
        }
        return expressions;
    }
    
    private void findSameConditions(List<AstNode> conditions) {
        for (int i = 1; i < conditions.size(); i++) {
            checkCondition(conditions, i);
        }
    }

    private void checkCondition(List<AstNode> conditions, int index) {
        AstNode condition = conditions.get(index);
        for (int j = 0; j < index; j++) {
            AstNode otherCondition = conditions.get(j);
            if (CheckUtils.equalNodes(otherCondition, condition)) {
                getContext().createViolation(this, getLocalizedMessage(CHECK_KEY), 
                        condition,
                        ImmutableList.of(newLocation("Original", otherCondition)),
                        otherCondition.getToken().getLine());
                return;
            }
        }
    }
    
}
