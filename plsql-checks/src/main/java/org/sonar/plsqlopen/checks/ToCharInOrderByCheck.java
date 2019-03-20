/*
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
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
import org.sonar.plugins.plsqlopen.api.DmlGrammar;
import org.sonar.plugins.plsqlopen.api.annnotations.ActivatedByDefault;
import org.sonar.plugins.plsqlopen.api.annnotations.ConstantRemediation;
import org.sonar.plugins.plsqlopen.api.annnotations.RuleInfo;
import org.sonar.plugins.plsqlopen.api.matchers.MethodMatcher;
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType;

import com.sonar.sslr.api.AstNode;

@Rule(
    key = ToCharInOrderByCheck.CHECK_KEY,
    priority = Priority.MAJOR,
    tags = Tags.BUG
)
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
public class ToCharInOrderByCheck extends AbstractBaseCheck {
    public static final String CHECK_KEY = "ToCharInOrderBy";
    private static MethodMatcher toChar = MethodMatcher.create().name("to_char").withNoParameterConstraint();

    @Override
    public void init() {
        subscribeTo(DmlGrammar.ORDER_BY_ITEM);
    }

    @Override
    public void visitNode(AstNode node) {
        AstNode expression = node.getFirstChild();

        if (toChar.matches(expression)) {
            addIssue(node, getLocalizedMessage(CHECK_KEY));
        }

        if (semantic(expression).getPlSqlType() == PlSqlType.NUMERIC) {
            int index = Integer.valueOf(expression.getTokenOriginalValue());

            AstNode selectExpression = node.getFirstAncestor(DmlGrammar.SELECT_EXPRESSION);
            List<AstNode> columns = selectExpression.getChildren(DmlGrammar.SELECT_COLUMN);

            if (columns.size() >= index) {
                AstNode selectColumn = columns.get(index - 1).getFirstChild();

                if (toChar.matches(selectColumn)) {
                    addIssue(node, getLocalizedMessage(CHECK_KEY));
                }
            }
        }
    }
}
