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

import javax.annotation.Nullable;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plsqlopen.annnotations.ActivatedByDefault;
import org.sonar.plsqlopen.annnotations.ConstantRemediation;
import com.sonar.sslr.api.AstNode;

@Rule(
    key = DeadCodeCheck.CHECK_KEY,
    priority = Priority.MAJOR,
    tags = Tags.UNUSED
)
@ConstantRemediation("5min")
@ActivatedByDefault
public class DeadCodeCheck extends AbstractBaseCheck {
    public static final String CHECK_KEY = "DeadCode";

    @Override
    public void init() {
        subscribeTo(CheckUtils.getTerminationStatements());
        subscribeTo(PlSqlGrammar.METHOD_CALL);
    }

    @Override
    public void visitNode(AstNode node) {
        if (CheckUtils.isTerminationStatement(node)) {
            AstNode parent = node.getParent();
            while (!checkNode(parent)) {
                parent = parent.getParent();
            }
        }
    }
    
    private static boolean shouldCheckNode(@Nullable AstNode node) {
        if (node == null || CheckUtils.isProgramUnit(node)) {
            return false;
        }
        if (node.is(PlSqlGrammar.STATEMENT, PlSqlGrammar.BLOCK_STATEMENT, PlSqlGrammar.CALL_STATEMENT)) {
            return true;
        }
        if (node.is(PlSqlGrammar.STATEMENTS_SECTION, PlSqlGrammar.STATEMENTS) && !node.hasDirectChildren(PlSqlGrammar.EXCEPTION_HANDLER)) {
            return true;
        }
        return false;
    }
    
    private boolean checkNode(AstNode node) {
        if (!shouldCheckNode(node)) {
            return true;
        }
        AstNode nextSibling = node.getNextSibling();
        if (nextSibling != null && nextSibling.is(PlSqlGrammar.STATEMENT)) {
            getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), nextSibling);
            return true;
        }
        return false;
    }

}
