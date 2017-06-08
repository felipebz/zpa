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
import org.sonar.plugins.plsqlopen.api.PlSqlPunctuator;
import org.sonar.plsqlopen.annnotations.ActivatedByDefault;
import org.sonar.plsqlopen.annnotations.ConstantRemediation;
import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;

@Rule(
    key = DuplicatedValueInInCheck.CHECK_KEY,
    priority = Priority.BLOCKER,
    tags = Tags.BUG
)
@ConstantRemediation("5min")
@ActivatedByDefault
public class DuplicatedValueInInCheck extends AbstractBaseCheck {
    public static final String CHECK_KEY = "DuplicatedValueInIn";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.IN_EXPRESSION);
    }

    @Override
    public void visitNode(AstNode node) {
        List<AstNode> values = getInValue(node);
        findSameValues(values);
    }
    
    public List<AstNode> getInValue(AstNode inExpression) {
        List<AstNode> values = new ArrayList<>();
        AstNode current = inExpression.getFirstChild(PlSqlPunctuator.LPARENTHESIS);
        while (current != null) {
            current = current.getNextSibling();
            
            if (current.is(PlSqlPunctuator.RPARENTHESIS)) {
                current = null;
            } else if (!current.is(PlSqlPunctuator.COMMA)) {
                values.add(current);
            }
        }
        return values;
    }
    
    private void findSameValues(List<AstNode> values) {
        for (int i = 1; i < values.size(); i++) {
            checkValue(values, i);
        }
    }

    private void checkValue(List<AstNode> values, int index) {
        AstNode current  = values.get(index);
        
        for (int j = 0; j < index; j++) {
            AstNode other = values.get(j);
            
            if (CheckUtils.equalNodes(current, other)) {
                getContext().createViolation(this, getLocalizedMessage(CHECK_KEY), 
                        current,
                        ImmutableList.of(newLocation("Original", other)),
                        current.getTokenOriginalValue());
                return;
            }
        }
    }

}
