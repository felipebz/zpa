/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
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
package org.sonar.plsqlopen.checks

object CheckList {

    const val SONAR_WAY_PROFILE = "Sonar way"

    val checks: List<Class<*>>
        get() = listOf(
                EmptyBlockCheck::class.java,
                ParsingErrorCheck::class.java,
                CollapsibleIfStatementsCheck::class.java,
                InequalityUsageCheck::class.java,
                ComparisonWithNullCheck::class.java,
                TooManyRowsHandlerCheck::class.java,
                InsertWithoutColumnsCheck::class.java,
                DeclareSectionWithoutDeclarationsCheck::class.java,
                NvlWithNullParameterCheck::class.java,
                ComparisonWithBooleanCheck::class.java,
                CharacterDatatypeUsageCheck::class.java,
                SelectAllColumnsCheck::class.java,
                ColumnsShouldHaveTableNameCheck::class.java,
                SelectWithRownumAndOrderByCheck::class.java,
                ToDateWithoutFormatCheck::class.java,
                ExplicitInParameterCheck::class.java,
                VariableInitializationWithNullCheck::class.java,
                UselessParenthesisCheck::class.java,
                IdenticalExpressionCheck::class.java,
                EmptyStringAssignmentCheck::class.java,
                DuplicatedValueInInCheck::class.java,
                VariableInitializationWithFunctionCallCheck::class.java,
                IfWithExitCheck::class.java,
                FunctionWithOutParameterCheck::class.java,
                SameConditionCheck::class.java,
                AddParenthesesInNestedExpressionCheck::class.java,
                RaiseStandardExceptionCheck::class.java,
                NotFoundCheck::class.java,
                QueryWithoutExceptionHandlingCheck::class.java,
                UnusedVariableCheck::class.java,
                VariableHidingCheck::class.java,
                DbmsOutputPutCheck::class.java,
                ReturnOfBooleanExpressionCheck::class.java,
                UnnecessaryElseCheck::class.java,
                DeadCodeCheck::class.java,
                ConcatenationWithNullCheck::class.java,
                SameBranchCheck::class.java,
                UnusedParameterCheck::class.java,
                CommitRollbackCheck::class.java,
                UnnecessaryNullStatementCheck::class.java,
                DuplicateConditionIfElsifCheck::class.java,
                UnnecessaryAliasInQueryCheck::class.java,
                VariableInCountCheck::class.java,
                UnhandledUserDefinedExceptionCheck::class.java,
                UnusedCursorCheck::class.java,
                NotASelectedExpressionCheck::class.java,
                InvalidReferenceToObjectCheck::class.java,
                CursorBodyInPackageSpecCheck::class.java,
                XPathCheck::class.java,
                VariableNameCheck::class.java,
                ToCharInOrderByCheck::class.java,
                DisabledTestCheck::class.java,
                RedundantExpectationCheck::class.java,
                UnnecessaryLikeCheck::class.java)

}
