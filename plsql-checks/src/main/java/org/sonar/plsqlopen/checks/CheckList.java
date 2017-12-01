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

import com.google.common.collect.ImmutableList;

public class CheckList {

    public static final String REPOSITORY_KEY = "plsql";

    public static final String SONAR_WAY_PROFILE = "Sonar way";

    private CheckList() {
    }
    
    @SuppressWarnings("rawtypes")
    public static List<Class> getChecks() {
        return ImmutableList.<Class>of(
            EmptyBlockCheck.class,
            ParsingErrorCheck.class,
            CollapsibleIfStatementsCheck.class,
            InequalityUsageCheck.class,
            ComparisonWithNullCheck.class,
            TooManyRowsHandlerCheck.class,
            InsertWithoutColumnsCheck.class,
            DeclareSectionWithoutDeclarationsCheck.class,
            NvlWithNullParameterCheck.class,
            ComparisonWithBooleanCheck.class,
            CharacterDatatypeUsageCheck.class,
            SelectAllColumnsCheck.class,
            ColumnsShouldHaveTableNameCheck.class,
            SelectWithRownumAndOrderByCheck.class,
            ToDateWithoutFormatCheck.class,
            ExplicitInParameterCheck.class,
            VariableInitializationWithNullCheck.class,
            UselessParenthesisCheck.class,
            IdenticalExpressionCheck.class,
            EmptyStringAssignmentCheck.class,
            DuplicatedValueInInCheck.class,
            VariableInitializationWithFunctionCallCheck.class,
            IfWithExitCheck.class,
            FunctionWithOutParameterCheck.class,
            SameConditionCheck.class,
            AddParenthesesInNestedExpressionCheck.class,
            RaiseStandardExceptionCheck.class,
            NotFoundCheck.class,
            QueryWithoutExceptionHandlingCheck.class,
            UnusedVariableCheck.class,
            VariableHidingCheck.class,
            DbmsOutputPutCheck.class,
            ReturnOfBooleanExpressionCheck.class,
            UnnecessaryElseCheck.class,
            DeadCodeCheck.class,
            ConcatenationWithNullCheck.class,
            SameBranchCheck.class,
            UnusedParameterCheck.class,
            CommitRollbackCheck.class,
            UnnecessaryNullStatementCheck.class,
            DuplicateConditionIfElsifCheck.class,
            UnnecessaryAliasInQueryCheck.class,
            VariableInCountCheck.class,
            UnhandledUserDefinedExceptionCheck.class,
            UnusedCursorCheck.class,
            NotASelectedExpressionCheck.class,
            InvalidReferenceToObjectCheck.class,
            CursorBodyInPackageSpecCheck.class,
            XPathCheck.class);
    }
}
