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

import javax.annotation.Nullable;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plsqlopen.annnotations.ActivatedByDefault;
import org.sonar.plsqlopen.annnotations.ConstantRemediation;
import com.sonar.sslr.api.AstNode;

@Rule(
    key = ReturnOfBooleanExpressionCheck.CHECK_KEY,
    priority = Priority.MINOR,
    tags = Tags.CLUMSY
)
@ConstantRemediation("2min")
@ActivatedByDefault
public class ReturnOfBooleanExpressionCheck extends AbstractBaseCheck {
    public static final String CHECK_KEY = "ReturnOfBooleanExpression";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.IF_STATEMENT);
    }

    @Override
    public void visitNode(AstNode node) {
        if (!hasElsif(node) && hasElse(node)) {
            AstNode firstBoolean = getBooleanValue(node);
            AstNode secondBoolean = getBooleanValue(node.getFirstChild(PlSqlGrammar.ELSE_CLAUSE));

            if (firstBoolean != null && secondBoolean != null
                    && !firstBoolean.getTokenValue().equals(secondBoolean.getTokenValue())) {
                getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), node);
            }
        }
    }

    public boolean hasElsif(AstNode node) {
        return node.hasDirectChildren(PlSqlGrammar.ELSIF_CLAUSE);
    }

    public boolean hasElse(AstNode node) {
        return node.hasDirectChildren(PlSqlGrammar.ELSE_CLAUSE);
    }

    @Nullable
    public AstNode getBooleanValue(AstNode node) {
        return extractBooleanValueFromReturn(getStatementFrom(node));
    }

    @Nullable
    public AstNode getStatementFrom(AstNode node) {
        List<AstNode> statements = node.getFirstChild(PlSqlGrammar.STATEMENTS).getChildren();
        if (statements.size() == 1) {
            return statements.get(0);
        }
        return null;
    }

    @Nullable
    public AstNode extractBooleanValueFromReturn(@Nullable AstNode node) {
        if (node != null) {
            AstNode child = node.getFirstChild();
            if (child.is(PlSqlGrammar.RETURN_STATEMENT)) {
                AstNode expression = child.getFirstChild(PlSqlGrammar.LITERAL);

                return getBooleanLiteral(expression);
            }
        }
        return null;
    }

    @Nullable
    public AstNode getBooleanLiteral(@Nullable AstNode expression) {
        if (expression != null) {
            return expression.getFirstChild(PlSqlGrammar.BOOLEAN_LITERAL);
        }
        return null;
    }

}
