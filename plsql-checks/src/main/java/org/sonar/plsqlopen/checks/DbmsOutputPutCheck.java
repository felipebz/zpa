/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015 Felipe Zorzo
 * felipe.b.zorzo@gmail.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plsqlopen.checks;

import java.util.List;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plsqlopen.checks.common.BaseMethodCallChecker;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.AstNode;

@Rule(
    key = DbmsOutputPutCheck.CHECK_KEY,
    priority = Priority.MINOR
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class DbmsOutputPutCheck extends BaseMethodCallChecker {
    public static final String CHECK_KEY = "DbmsOutputPut";
    private static final String DBMS_OUTPUT = "DBMS_OUTPUT";
    private static final List<String> PROCEDURES = ImmutableList.of("PUT", "PUT_LINE");

    @Override
    protected boolean isMethod(AstNode currentNode, AstNode identifier) {
        if (identifier.is(PlSqlGrammar.MEMBER_EXPRESSION)) {
            List<AstNode> members = identifier.getChildren(PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.VARIABLE_NAME);
            if (members.size() >= 2) {
                members = Lists.reverse(members);
                String methodName = members.get(0).getTokenOriginalValue().toUpperCase();
                String packageName = members.get(1).getTokenOriginalValue();
                if (DBMS_OUTPUT.equalsIgnoreCase(packageName) && PROCEDURES.contains(methodName)) {
                    getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), currentNode);
                }
            }
        }
        return false;
    }

    @Override
    protected void checkArguments(AstNode currentNode, List<AstNode> arguments) {
        // not used
    }

}
