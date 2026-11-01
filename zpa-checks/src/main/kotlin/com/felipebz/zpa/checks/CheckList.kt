//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

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
                DeterministicFunctionCheck::class.java,
                FunctionShouldBeDeterministicCheck::class.java,
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
                UnnecessaryStringLiteralConcatenationCheck::class.java,
                SequenceNextvalInConditionalResultCheck::class.java,
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
                PackageBodyParameterNocopyCheck::class.java,
                DisabledTestCheck::class.java,
                IneffectiveUtPlSqlAnnotationCheck::class.java,
                ConflictingUtPlSqlAnnotationsCheck::class.java,
                InvalidUtPlSqlAnnotationArgumentCheck::class.java,
                InvalidUtPlSqlContextStructureCheck::class.java,
                UnresolvedUtPlSqlSetupCleanupReferenceCheck::class.java,
                UtPlSqlTestShouldHaveDescriptionCheck::class.java,
                RedundantExpectationCheck::class.java,
                UnnecessaryLikeCheck::class.java)

}
