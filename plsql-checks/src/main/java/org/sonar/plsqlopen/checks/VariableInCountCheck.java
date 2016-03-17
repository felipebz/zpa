/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2016 Felipe Zorzo
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

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plsqlopen.matchers.MethodMatcher;
import org.sonar.plugins.plsqlopen.api.DmlGrammar;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.symbols.Scope;
import org.sonar.plugins.plsqlopen.api.symbols.Symbol;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import com.sonar.sslr.api.AstNode;

@Rule(
    key = VariableInCountCheck.CHECK_KEY,
    priority = Priority.BLOCKER,
    tags = Tags.BUG
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ERRORS)
@SqaleConstantRemediation("2min")
@ActivatedByDefault
public class VariableInCountCheck extends AbstractBaseCheck {
    public static final String CHECK_KEY = "VariableInCount";
    
    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.METHOD_CALL);
    }
    
    @Override
    public void visitNode(AstNode node) {
        MethodMatcher count = MethodMatcher.create().name("count").addParameter();
        
        if (!node.getParent().is(DmlGrammar.SELECT_COLUMN) || !count.matches(node)) {
            return;
        }
        
        checkArguments(node, count.getArguments(node));
    }
    
    protected void checkArguments(AstNode currentNode, List<AstNode> arguments) {
        if (arguments.size() != 1) {
            return;
        }
        
        String value = arguments.get(0).getTokenOriginalValue();
        Scope scope = getPlSqlContext().getCurrentScope();
        if (scope != null) {
            Symbol symbol = scope.getSymbol(value);
            if (symbol != null) {
                getPlSqlContext().createViolation(this, getLocalizedMessage(CHECK_KEY), currentNode, value);
            }
        }
    }

}
