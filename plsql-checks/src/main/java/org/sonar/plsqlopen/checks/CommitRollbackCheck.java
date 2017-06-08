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

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.symbols.Scope;
import org.sonar.plsqlopen.annnotations.ActivatedByDefault;
import org.sonar.plsqlopen.annnotations.ConstantRemediation;
import com.sonar.sslr.api.AstNode;

@Rule(
    key = CommitRollbackCheck.CHECK_KEY,
    priority = Priority.MAJOR
)
@ConstantRemediation("30min")
@ActivatedByDefault
public class CommitRollbackCheck extends AbstractBaseCheck {

    public static final String CHECK_KEY = "CommitRollback";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.COMMIT_STATEMENT, PlSqlGrammar.ROLLBACK_STATEMENT);
    }

    @Override
    public void visitNode(AstNode node) {
        Scope scope = getContext().getCurrentScope();
        
        Scope outerScope = scope;
        while (outerScope.outer() != null) {
            outerScope = outerScope.outer();
        }
        
        if (!scope.isAutonomousTransaction() && !outerScope.tree().is(PlSqlGrammar.BLOCK_STATEMENT)) {
            getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), node, node.getTokenValue());
        }
    }

}
