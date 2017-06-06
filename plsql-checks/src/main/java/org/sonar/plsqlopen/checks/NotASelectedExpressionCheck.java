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
import org.sonar.plsqlopen.checks.CheckUtils;
import org.sonar.plugins.plsqlopen.api.DmlGrammar;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword;
import org.sonar.plsqlopen.annnotations.ConstantRemediation;
import com.sonar.sslr.api.AstNode;

@Rule(
    key = NotASelectedExpressionCheck.CHECK_KEY,
    priority = Priority.CRITICAL
)
@ConstantRemediation("5min")
public class NotASelectedExpressionCheck extends AbstractBaseCheck {

    public static final String CHECK_KEY = "NotASelectedExpression";
    
    @Override
    public void init() {
        subscribeTo(DmlGrammar.SELECT_EXPRESSION);
    }
    
    @Override
    public void visitNode(AstNode node) {
        if (!node.getChildren().get(1).is(PlSqlKeyword.DISTINCT) || !node.hasDirectChildren(DmlGrammar.ORDER_BY_CLAUSE)) {
            return;
        }
        
        List<AstNode> columns = node.getChildren(DmlGrammar.SELECT_COLUMN);
        List<AstNode> orderByItems = node.getFirstChild(DmlGrammar.ORDER_BY_CLAUSE).getChildren(DmlGrammar.ORDER_BY_ITEM);
        
        for (AstNode orderByItem : orderByItems) {
            checkOrderByItem(orderByItem, columns);
        }
    }

    private void checkOrderByItem(AstNode orderByItem, List<AstNode> columns) {
        AstNode orderByItemValue = skipVariableName(orderByItem.getFirstChild());
        
        if (orderByItemValue.is(PlSqlGrammar.LITERAL)) {
            return;
        }
        
        boolean found = false;
        for (AstNode column : columns) {
            List<AstNode> candidates = extractAcceptableValuesFromColumn(column);
            
            for (AstNode candidate : candidates) {
                if (CheckUtils.equalNodes(orderByItemValue, candidate)) {
                    found = true;
                }
            }
        }

        if (!found) {
            getContext().createViolation(this, getLocalizedMessage(CHECK_KEY), orderByItemValue);
        }
    }
    
    private static AstNode skipVariableName(AstNode node) {
        if (node.is(PlSqlGrammar.VARIABLE_NAME)) {
            return node.getFirstChild();
        }
        return node;
    }
    
    private static List<AstNode> extractAcceptableValuesFromColumn(AstNode column) {
        List<AstNode> values = new ArrayList<>();
        
        AstNode selectedExpression = skipVariableName(column.getFirstChild()); 
        values.add(selectedExpression);
        
        if (column.getNumberOfChildren() == 1) {
            // if the value is "table.column", "column" can be used in order by
            if (selectedExpression.is(PlSqlGrammar.MEMBER_EXPRESSION)) {
                values.add(selectedExpression.getLastChild());
            }
        } else {
            AstNode alias = skipVariableName(column.getLastChild());
            values.add(alias);
        }
        
        return values;
    }

}
