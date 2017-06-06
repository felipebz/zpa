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
import org.sonar.plsqlopen.annnotations.ActivatedByDefault;
import org.sonar.plsqlopen.annnotations.ConstantRemediation;
import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;

@Rule(
    key = DuplicateConditionIfElsifCheck.CHECK_KEY,
    priority = Priority.BLOCKER,
    tags = Tags.BUG
)
@ConstantRemediation("5min")
@ActivatedByDefault
public class DuplicateConditionIfElsifCheck extends AbstractBaseCheck {
    public static final String CHECK_KEY = "DuplicateConditionIfElsif";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.IF_STATEMENT);
    }

    @Override
    public void visitNode(AstNode node) {
        List<AstNode> conditions = collectConditionsFromBranches(node);
        findSameConditions(conditions);
    }
    
    private static List<AstNode> collectConditionsFromBranches(AstNode node) {
        List<AstNode> conditionsFromBranches = new ArrayList<>();
        
        conditionsFromBranches.add(getConditions(node));
        
        for (AstNode branch : node.getChildren(PlSqlGrammar.ELSIF_CLAUSE)) {
            conditionsFromBranches.add(getConditions(branch));
        }
        
        return conditionsFromBranches;
    }
    
    private static AstNode getConditions(AstNode node) {
        return node.getChildren().get(1);
    }
    
    private void findSameConditions(List<AstNode> branches) {
        for (int i = 1; i < branches.size(); i++) {
            checkCondition(branches, i);
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
