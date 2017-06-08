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

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plsqlopen.annnotations.ActivatedByDefault;
import org.sonar.plsqlopen.annnotations.ConstantRemediation;
import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;

@Rule(
    key = RaiseStandardExceptionCheck.CHECK_KEY,
    priority = Priority.MAJOR
)
@ConstantRemediation("20min")
@ActivatedByDefault
public class RaiseStandardExceptionCheck extends AbstractBaseCheck {
    public static final String CHECK_KEY = "RaiseStandardException";
    
    private final List<String> standardExceptions = ImmutableList.of(
            "ACCESS_INTO_NULL",
            "CASE_NOT_FOUND",
            "COLLECTION_IS_NULL",
            "CURSOR_ALREADY_OPEN",
            "DUP_VAL_ON_INDEX",
            "INVALID_CURSOR",
            "INVALID_NUMBER",
            "LOGIN_DENIED",
            "NO_DATA_FOUND",
            "NOT_LOGGED_ON",
            "PROGRAM_ERROR",
            "ROWTYPE_MISMATCH",
            "SELF_IS_NULL",
            "STORAGE_ERROR",
            "SUBSCRIPT_BEYOND_COUNT",
            "SUBSCRIPT_OUTSIDE_LIMIT",
            "SYS_INVALID_ROWID",
            "TIMEOUT_ON_RESOURCE",
            "TOO_MANY_ROWS",
            "VALUE_ERROR",
            "ZERO_DIVIDE");

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.RAISE_STATEMENT);
    }
    
    @Override
    public void visitNode(AstNode node) {
        AstNode exceptionIdentifier = node.getFirstChild(PlSqlGrammar.VARIABLE_NAME);
        if (exceptionIdentifier != null) {
            String exceptionName = exceptionIdentifier.getTokenOriginalValue();
            
            if (standardExceptions.contains(exceptionName.toUpperCase())) {
                getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), exceptionIdentifier, exceptionName);
            }
        }
    }

}
