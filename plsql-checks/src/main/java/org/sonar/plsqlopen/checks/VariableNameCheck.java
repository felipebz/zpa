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

import java.util.regex.Pattern;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.annnotations.ActivatedByDefault;
import org.sonar.plugins.plsqlopen.api.annnotations.ConstantRemediation;
import org.sonar.plugins.plsqlopen.api.annnotations.RuleInfo;

import com.sonar.sslr.api.AstNode;

@Rule(
    key = VariableNameCheck.CHECK_KEY,
    priority = Priority.MINOR
)
@ConstantRemediation("10min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
public class VariableNameCheck extends AbstractBaseCheck {
    public static final String CHECK_KEY = "VariableName";

    private static final String DEFAULT_REGEXP = "[a-zA-Z]([a-zA-Z0-9_]*[a-zA-Z0-9])?";

    @RuleProperty(key = "regexp", defaultValue = DEFAULT_REGEXP)
    public String regexp = DEFAULT_REGEXP;

    private Pattern pattern;

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.VARIABLE_DECLARATION);
        pattern = Pattern.compile(regexp);
    }

    @Override
    public void visitNode(AstNode node) {
        AstNode identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME);
        String name = identifier.getTokenOriginalValue();

        if (!pattern.matcher(name).matches()) {
            addIssue(identifier, getLocalizedMessage(CHECK_KEY), name, regexp);
        }
    }
}
