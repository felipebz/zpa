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
    key = SameBranchCheck.CHECK_KEY,
    priority = Priority.MAJOR
)
@ConstantRemediation("10min")
@ActivatedByDefault
public class SameBranchCheck extends AbstractBaseCheck {

    public static final String CHECK_KEY = "SameBranch";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.IF_STATEMENT);
    }

    @Override
    public void visitNode(AstNode node) {
        List<AstNode> branches = collectStatementsFromBranches(node);
        findSameBranches(branches);
    }
    
    private static List<AstNode> collectStatementsFromBranches(AstNode node) {
        List<AstNode> statementsFromBranches = new ArrayList<>();
        
        statementsFromBranches.add(getStatements(node));
        
        for (AstNode branch : node.getChildren(PlSqlGrammar.ELSIF_CLAUSE)) {
            statementsFromBranches.add(getStatements(branch));
        }
        
        AstNode elseBranch = node.getFirstChild(PlSqlGrammar.ELSE_CLAUSE);
        if (elseBranch != null) {
            statementsFromBranches.add(getStatements(elseBranch));
        }
        
        return statementsFromBranches;
    }
    
    private static AstNode getStatements(AstNode node) {
        return node.getFirstChild(PlSqlGrammar.STATEMENTS);
    }
    
    private void findSameBranches(List<AstNode> branches) {
        for (int i = 1; i < branches.size(); i++) {
            checkBranch(branches, i);
        }
    }
    
    private void checkBranch(List<AstNode> branches, int index) {
        AstNode branch = branches.get(index);
        final int previousBranchIndex = index - 1;
        AstNode otherBranch = branches.get(previousBranchIndex);
        if (CheckUtils.equalNodes(otherBranch, branch)) {
            getContext().createViolation(this, getLocalizedMessage(CHECK_KEY), 
                    branch,
                    ImmutableList.of(newLocation("Original", otherBranch)),
                    otherBranch.getToken().getLine());
            return;
        }
    }

}
