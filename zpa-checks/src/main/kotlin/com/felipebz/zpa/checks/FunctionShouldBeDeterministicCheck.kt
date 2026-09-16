/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
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
package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.zpa.api.ConditionsGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.api.annotations.ConstantRemediation
import com.felipebz.zpa.api.annotations.Priority
import com.felipebz.zpa.api.annotations.Rule
import com.felipebz.zpa.api.annotations.RuleInfo
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.PackageSpecificationResolution
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.symbols.PlSqlType
import com.felipebz.zpa.api.symbols.Symbol

/**
 * Recommends DETERMINISTIC only for a deliberately small, locally verifiable subset of functions.
 * Unknown calls, references, statements and execution models fail closed.
 */
@Rule(priority = Priority.MINOR)
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@OptIn(ZpaExperimentalApi::class)
class FunctionShouldBeDeterministicCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.FUNCTION_DECLARATION, PlSqlGrammar.CREATE_FUNCTION)
    }

    override fun visitNode(node: AstNode) {
        if (context.formsMetadata != null) {
            return
        }

        if (isCandidate(node)) {
            addIssue(functionNameNode(node), getLocalizedMessage())
        }
    }

    private fun isCandidate(function: AstNode): Boolean {
        if (!isCandidateDeclaration(function) || function.hasDirectChildren(PlSqlKeyword.DETERMINISTIC)) {
            return false
        }
        if (function.getFirstChildOrNull(PlSqlGrammar.STATEMENTS_SECTION) == null) {
            return false
        }

        val parameters = function.getFirstChildOrNull(PlSqlGrammar.PARAMETER_DECLARATIONS)
        if (parameters != null && !isSafeParameters(parameters)) {
            return false
        }

        if (function.hasDirectChildren(*UNSUPPORTED_FUNCTION_MODIFIERS)) {
            return false
        }

        val declareSection = function.getFirstChildOrNull(PlSqlGrammar.DECLARE_SECTION)
        if (declareSection != null && !isSafeDeclareSection(declareSection)) {
            return false
        }

        return isSafeStatementsSection(function.getFirstChild(PlSqlGrammar.STATEMENTS_SECTION))
    }

    private fun isSafeParameters(parameters: AstNode): Boolean =
        parameters.getChildren(PlSqlGrammar.PARAMETER_DECLARATION).all { parameter ->
            if (parameter.hasDirectChildren(PlSqlKeyword.OUT)) return@all false
            val assignment = parameter.getFirstChildOrNull(PlSqlGrammar.DEFAULT_VALUE_ASSIGNMENT)
                ?: return@all true
            val initializer = assignment.lastChildOrNull ?: return@all false
            val symbol = (parameter.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME) as? SemanticAstNode)?.symbol
            isSafeExpression(initializer) && isTypeCompatible(initializer, symbol?.type)
        }

    private fun isCandidateDeclaration(function: AstNode): Boolean = when (function.type) {
        PlSqlGrammar.CREATE_FUNCTION -> true
        PlSqlGrammar.FUNCTION_DECLARATION -> isPublicPackageBodyFunction(function) &&
            projectAnalysis().resolvePackageSpecification(function).let { resolution ->
                resolution.status == PackageSpecificationResolution.Status.RESOLVED &&
                    resolution.specification.orElse(null)?.let { specification ->
                        !specification.isDeterministic &&
                            specification.parameters.none { it.isDefaultPresent }
                    } == true
            }
        else -> false
    }

    private fun isPublicPackageBodyFunction(function: AstNode): Boolean =
        function.parentOrNull?.type == PlSqlGrammar.DECLARE_SECTION &&
            function.parentOrNull?.parentOrNull?.type == PlSqlGrammar.CREATE_PACKAGE_BODY

    private fun isSafeDeclareSection(declareSection: AstNode): Boolean =
        declareSection.children.all { declaration ->
            when (declaration.type) {
                PlSqlGrammar.VARIABLE_DECLARATION -> isSafeVariableInitializer(declaration)
                // A nested routine is not executed merely by being declared. Calls to it are
                // rejected by expression analysis, so its implementation is intentionally not
                // inspected as part of the enclosing function.
                PlSqlGrammar.PROCEDURE_DECLARATION,
                PlSqlGrammar.FUNCTION_DECLARATION -> true
                else -> false
            }
        }

    private fun isSafeVariableInitializer(declaration: AstNode): Boolean {
        val assignment = declaration.getFirstChildOrNull(PlSqlGrammar.DEFAULT_VALUE_ASSIGNMENT) ?: return true
        val initializer = assignment.lastChildOrNull ?: return false
        val symbol = (declaration.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME) as? SemanticAstNode)?.symbol
        return isSafeExpression(initializer) && isTypeCompatible(initializer, symbol?.type)
    }

    private fun isSafeStatementsSection(section: AstNode): Boolean {
        if (section.hasDirectChildren(PlSqlGrammar.EXCEPTION_HANDLERS)) return false
        val statements = section.getFirstChildOrNull(PlSqlGrammar.STATEMENTS) ?: return false
        return isSafeStatements(statements) && guaranteesReturn(statements)
    }

    private fun isSafeStatements(statements: AstNode): Boolean {
        val statementNodes = statements.getChildren(PlSqlGrammar.STATEMENT)
        return statementNodes.isNotEmpty() && statementNodes.all(::isSafeStatement)
    }

    private fun isSafeStatement(wrapper: AstNode): Boolean {
        val statement = wrapper.children.singleOrNull() ?: return false
        return when (statement.type) {
            PlSqlGrammar.NULL_STATEMENT -> true
            PlSqlGrammar.RETURN_STATEMENT -> isSafeReturnStatement(statement)
            PlSqlGrammar.ASSIGNMENT_STATEMENT -> isSafeAssignmentStatement(statement)
            PlSqlGrammar.IF_STATEMENT -> isSafeIfStatement(statement)
            PlSqlGrammar.CASE_STATEMENT -> isSafeCaseStatement(statement)
            else -> false
        }
    }

    private fun isSafeReturnStatement(statement: AstNode): Boolean {
        val expression = expressionAfter(statement, PlSqlKeyword.RETURN)
        val function = statement.getFirstAncestorOrNull(
            PlSqlGrammar.CREATE_FUNCTION,
            PlSqlGrammar.FUNCTION_DECLARATION
        ) ?: return false
        val returnType = function.getFirstChildOrNull(PlSqlKeyword.RETURN)
            ?.nextSibling
            ?.let(::declaredType)
            ?: return false
        return expression != null && isSafeExpression(expression) && isTypeCompatible(expression, returnType)
    }

    private fun isSafeAssignmentStatement(statement: AstNode): Boolean {
        val assignmentIndex = statement.children.indexOfFirst { it.type === PlSqlPunctuator.ASSIGNMENT }
        if (assignmentIndex < 1) return false

        val target = statement.children.subList(0, assignmentIndex)
            .lastOrNull { it.type !== PlSqlGrammar.LABEL }
            ?: return false
        val expression = statement.children.subList(assignmentIndex + 1, statement.children.size)
            .filter { it.type !== PlSqlPunctuator.SEMICOLON }
            .singleOrNull()
            ?: return false

        val targetSymbol = writableSymbol(target) ?: return false
        return isSafeWritableReference(target) &&
            isSafeExpression(expression) &&
            isTypeCompatible(expression, targetSymbol.type)
    }

    private fun isSafeWritableReference(node: AstNode): Boolean {
        return when (node.type) {
            PlSqlGrammar.VARIABLE_NAME,
            PlSqlGrammar.MEMBER_EXPRESSION -> safeWritableSymbol((node as? SemanticAstNode)?.symbol)
            PlSqlGrammar.OBJECT_REFERENCE -> node.children.singleOrNull()?.let(::isSafeWritableReference) == true
            else -> false
        }
    }

    private fun writableSymbol(node: AstNode): Symbol? = when (node.type) {
        PlSqlGrammar.VARIABLE_NAME,
        PlSqlGrammar.MEMBER_EXPRESSION -> (node as? SemanticAstNode)?.symbol
        PlSqlGrammar.OBJECT_REFERENCE -> node.children.singleOrNull()?.let(::writableSymbol)
        else -> null
    }

    private fun safeWritableSymbol(symbol: Symbol?): Boolean =
        isSafeSymbol(symbol) && symbol?.hasModifier("constant") != true

    private fun isSafeIfStatement(statement: AstNode): Boolean {
        val thenIndex = statement.children.indexOfFirst { it.type === PlSqlKeyword.THEN }
        val condition = expressionBefore(statement, thenIndex, PlSqlKeyword.IF, PlSqlGrammar.LABEL)
        val initialStatements = statement.getFirstChildOrNull(PlSqlGrammar.STATEMENTS)
        if (thenIndex < 0 || condition == null || initialStatements == null ||
            !isSafeExpression(condition) || !isSafeStatements(initialStatements)) {
            return false
        }

        if (statement.getChildren(PlSqlGrammar.ELSIF_CLAUSE).any { elsif ->
                val elsifThenIndex = elsif.children.indexOfFirst { it.type === PlSqlKeyword.THEN }
                val elsifCondition = expressionBefore(elsif, elsifThenIndex, PlSqlKeyword.ELSIF)
                val elsifStatements = elsif.getFirstChildOrNull(PlSqlGrammar.STATEMENTS)
                elsifThenIndex < 0 || elsifCondition == null || elsifStatements == null ||
                    !isSafeExpression(elsifCondition) || !isSafeStatements(elsifStatements)
            }) {
            return false
        }

        return statement.getFirstChildOrNull(PlSqlGrammar.ELSE_CLAUSE)?.let {
            it.getFirstChildOrNull(PlSqlGrammar.STATEMENTS)?.let(::isSafeStatements) ?: false
        } ?: true
    }

    private fun guaranteesReturn(statements: AstNode): Boolean {
        val statementNodes = statements.getChildren(PlSqlGrammar.STATEMENT)
        if (statementNodes.isEmpty()) return false

        var canContinue = true
        for (statement in statementNodes) {
            if (canContinue) {
                canContinue = !statementGuaranteesReturn(statement)
            }
        }
        return !canContinue
    }

    private fun statementGuaranteesReturn(wrapper: AstNode): Boolean {
        val statement = wrapper.children.singleOrNull() ?: return false
        return when (statement.type) {
            PlSqlGrammar.RETURN_STATEMENT -> true
            PlSqlGrammar.IF_STATEMENT -> ifGuaranteesReturn(statement)
            PlSqlGrammar.CASE_STATEMENT -> caseGuaranteesReturn(statement)
            else -> false
        }
    }

    private fun ifGuaranteesReturn(statement: AstNode): Boolean {
        val initialStatements = statement.getFirstChildOrNull(PlSqlGrammar.STATEMENTS) ?: return false
        if (!guaranteesReturn(initialStatements)) return false

        if (statement.getChildren(PlSqlGrammar.ELSIF_CLAUSE).any { elsif ->
                elsif.getFirstChildOrNull(PlSqlGrammar.STATEMENTS)?.let(::guaranteesReturn) != true
            }) {
            return false
        }

        return statement.getFirstChildOrNull(PlSqlGrammar.ELSE_CLAUSE)
            ?.getFirstChildOrNull(PlSqlGrammar.STATEMENTS)
            ?.let(::guaranteesReturn) == true
    }

    private fun caseGuaranteesReturn(statement: AstNode): Boolean {
        val firstWhenIndex = statement.children.indexOfFirst { it.type === PlSqlKeyword.WHEN }
        if (firstWhenIndex < 0) return false

        var index = firstWhenIndex
        var branchCount = 0
        var hasElse = false
        while (index < statement.children.size) {
            when (statement.children[index].type) {
                PlSqlKeyword.WHEN -> {
                    val thenIndex = statement.indexOfFirstFrom(index + 1) { it.type === PlSqlKeyword.THEN }
                    val statementsIndex = statement.indexOfFirstFrom(thenIndex + 1) {
                        it.type === PlSqlGrammar.STATEMENTS
                    }
                    if (thenIndex < 0 || statementsIndex != thenIndex + 1 ||
                        !guaranteesReturn(statement.children[statementsIndex])) {
                        return false
                    }
                    branchCount++
                    index = statementsIndex + 1
                }
                PlSqlKeyword.ELSE -> {
                    val statements = statement.children.getOrNull(index + 1)
                        ?.takeIf { it.type === PlSqlGrammar.STATEMENTS }
                        ?: return false
                    if (!guaranteesReturn(statements)) return false
                    hasElse = true
                    index += 2
                }
                PlSqlKeyword.END -> return branchCount > 0 && hasElse
                else -> return false
            }
        }
        return false
    }

    private fun isSafeCaseStatement(statement: AstNode): Boolean {
        val firstWhenIndex = statement.children.indexOfFirst { it.type === PlSqlKeyword.WHEN }
        if (firstWhenIndex < 0) return false

        val selector = statement.children.subList(0, firstWhenIndex)
            .filter { it.type !== PlSqlKeyword.CASE && it.type !== PlSqlGrammar.LABEL }
            .singleOrNull()
        if (selector != null) return false

        var index = firstWhenIndex
        var branchCount = 0
        var hasElse = false
        while (index < statement.children.size) {
            when (statement.children[index].type) {
                PlSqlKeyword.WHEN -> {
                    val thenIndex = statement.indexOfFirstFrom(index + 1) { it.type === PlSqlKeyword.THEN }
                    val condition = expressionBetween(statement, index + 1, thenIndex)
                    val statementsIndex = statement.indexOfFirstFrom(thenIndex + 1) {
                        it.type === PlSqlGrammar.STATEMENTS
                    }
                    if (thenIndex < 0 || condition == null || statementsIndex != thenIndex + 1 ||
                        !isSafeExpression(condition) || !isSafeStatements(statement.children[statementsIndex])) {
                        return false
                    }
                    branchCount++
                    index = statementsIndex + 1
                }
                PlSqlKeyword.ELSE -> {
                    val statements = statement.children.getOrNull(index + 1)
                        ?.takeIf { it.type === PlSqlGrammar.STATEMENTS }
                        ?: return false
                    if (!isSafeStatements(statements)) return false
                    hasElse = true
                    index += 2
                }
                PlSqlKeyword.END -> return branchCount > 0 && hasElse
                else -> return false
            }
        }
        return false
    }

    private fun isSafeExpression(node: AstNode): Boolean =
        isSafeExpressionNode(node)

    private fun isSafeExpressionNode(node: AstNode): Boolean {
        when (node.type) {
            PlSqlGrammar.LITERAL -> return isSafeLiteral(node)
            PlSqlGrammar.VARIABLE_NAME -> return isSafeVariable(node)
            PlSqlGrammar.MEMBER_EXPRESSION -> return isSafeMember(node)
            PlSqlGrammar.UNARY_EXPRESSION -> return isSafeUnaryExpression(node)
            PlSqlGrammar.EXPONENTIATION_EXPRESSION -> return node.children.size == 1 &&
                isSafeExpressionNode(node.firstChild)
            PlSqlGrammar.ADDITIVE_EXPRESSION -> return isSafeArithmeticExpression(node)
            PlSqlGrammar.MULTIPLICATIVE_EXPRESSION -> return isSafeArithmeticExpression(node)
            PlSqlGrammar.CONCATENATION_EXPRESSION -> return isSafeConcatenationExpression(node)
            PlSqlGrammar.COMPARISON_EXPRESSION -> return isSafeComparisonExpression(node)
            PlSqlGrammar.NOT_EXPRESSION -> return isSafeNotExpression(node)
            PlSqlGrammar.AND_EXPRESSION,
            PlSqlGrammar.OR_EXPRESSION -> return isSafeBooleanExpression(node)
            PlSqlGrammar.BRACKED_EXPRESSION -> return isSafeParenthesizedExpression(node)
            PlSqlGrammar.EXPRESSION,
            PlSqlGrammar.BOOLEAN_EXPRESSION,
            PlSqlGrammar.PRIMARY_EXPRESSION -> return isSafeTransparentExpression(node)
        }
        return false
    }

    private fun isSafeLiteral(node: AstNode): Boolean =
        node.children.singleOrNull()?.type in SAFE_LITERAL_TYPES

    private fun isSafeParenthesizedExpression(node: AstNode): Boolean =
        node.children.size == 3 &&
            node.children[0].type === PlSqlPunctuator.LPARENTHESIS &&
            node.children[2].type === PlSqlPunctuator.RPARENTHESIS &&
            isSafeExpressionNode(node.children[1])

    private fun isSafeTransparentExpression(node: AstNode): Boolean =
        node.children.singleOrNull()?.let(::isSafeExpressionNode) == true

    private fun isSafeUnaryExpression(node: AstNode): Boolean {
        if (node.children.size == 1) return isSafeExpressionNode(node.firstChild)
        if (node.children.size != 2) return false
        if (node.children[0].type !== PlSqlPunctuator.PLUS && node.children[0].type !== PlSqlPunctuator.MINUS) {
            return false
        }
        val operand = node.children[1]
        return isSafeExpressionNode(operand) && expressionType(operand) == PlSqlType.NUMERIC
    }

    private fun isSafeArithmeticExpression(node: AstNode): Boolean {
        if (node.children.size == 1) return isSafeExpressionNode(node.firstChild)
        if (node.children.size < 3 || node.children.size % 2 == 0) return false

        val operatorTypes = if (node.type === PlSqlGrammar.ADDITIVE_EXPRESSION) {
            setOf(PlSqlPunctuator.PLUS, PlSqlPunctuator.MINUS)
        } else {
            setOf(PlSqlPunctuator.MULTIPLICATION)
        }
        if (node.children.indices.filter { it % 2 == 1 }.any { node.children[it].type !in operatorTypes }) {
            return false
        }

        return node.children.indices
            .filter { it % 2 == 0 }
            .map { node.children[it] }
            .all { isSafeExpressionNode(it) && expressionType(it) == PlSqlType.NUMERIC }
    }

    private fun isSafeConcatenationExpression(node: AstNode): Boolean {
        if (node.children.size == 1) return isSafeExpressionNode(node.firstChild)
        if (node.children.size < 3 || node.children.size % 2 == 0) return false
        if (node.children.indices.filter { it % 2 == 1 }
                .any { node.children[it].type !== PlSqlGrammar.CONCATENATION_OPERATOR }) {
            return false
        }

        return node.children.indices
            .filter { it % 2 == 0 }
            .map { node.children[it] }
            .all {
                isSafeExpressionNode(it) &&
                    expressionType(it) in setOf(PlSqlType.CHARACTER, PlSqlType.NULL)
            }
    }

    private fun isSafeComparisonExpression(node: AstNode): Boolean {
        if (node.children.size == 1) return isSafeExpressionNode(node.firstChild)
        if (node.children.size != 3 || node.children[1].type !== ConditionsGrammar.RELATIONAL_OPERATOR) return false

        val left = node.children[0]
        val right = node.children[2]
        return isSafeExpressionNode(left) && isSafeExpressionNode(right) && areComparable(left, right)
    }

    private fun isSafeNotExpression(node: AstNode): Boolean {
        if (node.children.size == 1) return isSafeExpressionNode(node.firstChild)
        if (node.children.size != 2 || node.children[0].type !== PlSqlKeyword.NOT) return false
        val operand = node.children[1]
        return isSafeExpressionNode(operand) && expressionType(operand) == PlSqlType.BOOLEAN
    }

    private fun isSafeBooleanExpression(node: AstNode): Boolean {
        if (node.children.size == 1) return isSafeExpressionNode(node.firstChild)
        if (node.children.size < 3 || node.children.size % 2 == 0) return false
        val operator = if (node.type === PlSqlGrammar.AND_EXPRESSION) PlSqlKeyword.AND else PlSqlKeyword.OR
        if (node.children.indices.filter { it % 2 == 1 }.any { node.children[it].type !== operator }) return false
        return node.children.indices
            .filter { it % 2 == 0 }
            .map { node.children[it] }
            .all { isSafeExpressionNode(it) && expressionType(it) == PlSqlType.BOOLEAN }
    }

    private fun areComparable(left: AstNode, right: AstNode): Boolean {
        val leftType = expressionType(left) ?: return false
        val rightType = expressionType(right) ?: return false
        if (leftType == PlSqlType.CHARACTER || rightType == PlSqlType.CHARACTER) return false
        return leftType == rightType || leftType == PlSqlType.NULL || rightType == PlSqlType.NULL
    }

    private fun isSafeVariable(node: AstNode): Boolean {
        val name = node.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)?.tokenOriginalValue ?: return false
        if (isKnownExternalName(name)) return false
        val symbol = (node as? SemanticAstNode)?.symbol
        return symbol?.let(::isSafeSymbol) == true
    }

    private fun isSafeMember(node: AstNode): Boolean {
        if (node.hasDescendant(PlSqlKeyword.NEXTVAL, PlSqlKeyword.CURRVAL)) return false

        val components = node.getChildren(PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.VARIABLE_NAME)
            .mapNotNull(::componentName)
        if (components.any(::isKnownExternalName)) return false

        val symbol = (node as? SemanticAstNode)?.symbol
        return symbol?.let(::isSafeSymbol) == true
    }

    private fun expressionType(node: AstNode): PlSqlType? {
        when (node.type) {
            PlSqlGrammar.LITERAL -> return (node as? SemanticAstNode)?.plSqlType?.takeUnless { it.isUnknown }
            PlSqlGrammar.VARIABLE_NAME,
            PlSqlGrammar.MEMBER_EXPRESSION -> return (node as? SemanticAstNode)?.symbol?.type
                ?.takeUnless { it.isUnknown }
            PlSqlGrammar.UNARY_EXPRESSION -> {
                if (node.children.size == 1) return expressionType(node.firstChild)
                if (node.children.size == 2 &&
                    (node.children[0].type === PlSqlPunctuator.PLUS || node.children[0].type === PlSqlPunctuator.MINUS)
                ) {
                    return expressionType(node.children[1])
                        ?.takeIf { it == PlSqlType.NUMERIC }
                }
            }
            PlSqlGrammar.ADDITIVE_EXPRESSION,
            PlSqlGrammar.MULTIPLICATIVE_EXPRESSION -> {
                if (node.children.size == 1) return expressionType(node.firstChild)
                val operatorTypes = if (node.type === PlSqlGrammar.ADDITIVE_EXPRESSION) {
                    setOf(PlSqlPunctuator.PLUS, PlSqlPunctuator.MINUS)
                } else {
                    setOf(PlSqlPunctuator.MULTIPLICATION)
                }
                if (node.children.size >= 3 && node.children.size % 2 == 1 &&
                    node.children.indices.filter { it % 2 == 1 }.all { node.children[it].type in operatorTypes } &&
                    node.children.indices.filter { it % 2 == 0 }
                        .map { node.children[it] }
                        .all { expressionType(it) == PlSqlType.NUMERIC }
                ) {
                    return PlSqlType.NUMERIC
                }
            }
            PlSqlGrammar.CONCATENATION_EXPRESSION -> {
                if (node.children.size == 1) return expressionType(node.firstChild)
                if (node.children.size >= 3 && node.children.size % 2 == 1 &&
                    node.children.indices.filter { it % 2 == 1 }
                        .all { node.children[it].type === PlSqlGrammar.CONCATENATION_OPERATOR } &&
                    node.children.indices.filter { it % 2 == 0 }
                        .map { node.children[it] }
                        .all { expressionType(it) in setOf(PlSqlType.CHARACTER, PlSqlType.NULL) }
                ) {
                    return PlSqlType.CHARACTER
                }
            }
            PlSqlGrammar.COMPARISON_EXPRESSION -> {
                if (node.children.size == 1) return expressionType(node.firstChild)
                if (node.children.size == 3 && node.children[1].type === ConditionsGrammar.RELATIONAL_OPERATOR &&
                    areComparable(node.children[0], node.children[2])
                ) {
                    return PlSqlType.BOOLEAN
                }
            }
            PlSqlGrammar.NOT_EXPRESSION -> {
                if (node.children.size == 1) return expressionType(node.firstChild)
                if (node.children.size == 2 && node.children[0].type === PlSqlKeyword.NOT) {
                    return expressionType(node.children[1])?.takeIf { it == PlSqlType.BOOLEAN }
                }
            }
            PlSqlGrammar.AND_EXPRESSION,
            PlSqlGrammar.OR_EXPRESSION -> {
                if (node.children.size == 1) return expressionType(node.firstChild)
                if (node.children.size >= 3 && node.children.size % 2 == 1 &&
                    node.children.indices.filter { it % 2 == 0 }
                        .map { node.children[it] }
                        .all { expressionType(it) == PlSqlType.BOOLEAN }
                ) {
                    return PlSqlType.BOOLEAN
                }
            }
            PlSqlGrammar.BRACKED_EXPRESSION -> {
                if (node.children.size == 3 &&
                    node.children[0].type === PlSqlPunctuator.LPARENTHESIS &&
                    node.children[2].type === PlSqlPunctuator.RPARENTHESIS
                ) {
                    return expressionType(node.children[1])
                }
            }
            PlSqlGrammar.EXPRESSION,
            PlSqlGrammar.BOOLEAN_EXPRESSION,
            PlSqlGrammar.PRIMARY_EXPRESSION -> {
                return node.children.singleOrNull()?.let(::expressionType)
            }
            else -> Unit
        }
        return null
    }

    private fun isTypeCompatible(expression: AstNode, targetType: PlSqlType?): Boolean {
        val sourceType = expressionType(expression) ?: return false
        return targetType != null && targetType != PlSqlType.UNKNOWN &&
            (sourceType == targetType || sourceType == PlSqlType.NULL)
    }

    private fun declaredType(datatype: AstNode): PlSqlType? {
        if (datatype.type !== PlSqlGrammar.DATATYPE) return null
        return when {
            datatype.hasDirectChildren(PlSqlGrammar.NUMERIC_DATATYPE) -> PlSqlType.NUMERIC
            datatype.hasDirectChildren(PlSqlGrammar.CHARACTER_DATAYPE) -> PlSqlType.CHARACTER
            datatype.hasDirectChildren(PlSqlGrammar.DATE_DATATYPE) -> PlSqlType.DATE
            datatype.hasDirectChildren(PlSqlGrammar.BOOLEAN_DATATYPE) -> PlSqlType.BOOLEAN
            else -> (datatype as? SemanticAstNode)?.plSqlType?.takeUnless { it.isUnknown }
        }
    }

    private fun isSafeSymbol(symbol: Symbol?): Boolean {
        if (symbol == null || symbol.scope.type !in FUNCTION_SCOPE_TYPES) return false
        return when (symbol.kind) {
            Symbol.Kind.VARIABLE,
            Symbol.Kind.PARAMETER -> !symbol.hasModifier("out")
            else -> false
        }
    }

    private fun expressionAfter(node: AstNode, keyword: AstNodeType): AstNode? {
        val keywordIndex = node.children.indexOfFirst { it.type === keyword }
        if (keywordIndex < 0) return null
        return node.children.subList(keywordIndex + 1, node.children.size)
            .filter { it.type !== PlSqlPunctuator.SEMICOLON }
            .singleOrNull()
    }

    private fun expressionBefore(node: AstNode, markerIndex: Int, vararg ignoredTypes: AstNodeType): AstNode? {
        if (markerIndex < 0) return null
        return node.children.subList(0, markerIndex)
            .asReversed()
            .firstOrNull { it.type !in ignoredTypes }
    }

    private fun expressionBetween(node: AstNode, startIndex: Int, endIndex: Int): AstNode? {
        if (endIndex < startIndex) return null
        return node.children.subList(startIndex, endIndex).singleOrNull()
    }

    private fun AstNode.indexOfFirstFrom(startIndex: Int, predicate: (AstNode) -> Boolean): Int {
        for (index in startIndex until children.size) {
            if (predicate(children[index])) return index
        }
        return -1
    }

    private fun functionNameNode(function: AstNode): AstNode {
        val unitName = function.getFirstChildOrNull(PlSqlGrammar.UNIT_NAME)
        if (unitName != null) return unitName.getLastChild(PlSqlGrammar.IDENTIFIER_NAME)
        return function.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
    }

    private fun componentName(node: AstNode): String? = when (node.type) {
        PlSqlGrammar.IDENTIFIER_NAME -> node.tokenOriginalValue
        PlSqlGrammar.VARIABLE_NAME -> node.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)?.tokenOriginalValue
        else -> null
    }

    private fun isKnownExternalName(name: String): Boolean =
        isUnquoted(name) && name.equalsAnyIgnoreCase(EXTERNAL_ENVIRONMENT_NAMES)

    private fun isUnquoted(value: String): Boolean =
        !value.startsWith("\"") && !value.endsWith("\"")

    private fun String.equalsAnyIgnoreCase(values: Set<String>): Boolean =
        values.any { equals(it, ignoreCase = true) }

    private companion object {
        val EXTERNAL_ENVIRONMENT_NAMES = setOf(
            "CURRENT_DATE",
            "CURRENT_TIME",
            "CURRENT_TIMESTAMP",
            "CURRENT_USER",
            "CURRENT_SCHEMA",
            "DBTIMEZONE",
            "LOCALTIME",
            "LOCALTIMESTAMP",
            "SESSIONTIMEZONE",
            "SESSION_USER",
            "SYSDATE",
            "SYSTIMESTAMP",
            "USER",
        )

        val SAFE_LITERAL_TYPES: Set<AstNodeType> = setOf(
            PlSqlGrammar.NULL_LITERAL,
            PlSqlGrammar.BOOLEAN_LITERAL,
            PlSqlGrammar.NUMERIC_LITERAL,
            PlSqlGrammar.FLOATING_POINT_LITERAL,
            PlSqlGrammar.CHARACTER_LITERAL,
            PlSqlTokenType.DATE_LITERAL,
            PlSqlTokenType.TIMESTAMP_LITERAL,
            PlSqlGrammar.INTERVAL_LITERAL,
        )

        val UNSUPPORTED_FUNCTION_MODIFIERS = arrayOf(
            PlSqlKeyword.AGGREGATE,
            PlSqlKeyword.PARALLEL_ENABLE,
            PlSqlKeyword.PIPELINED,
            PlSqlKeyword.RESULT_CACHE,
        )

        val FUNCTION_SCOPE_TYPES = setOf<AstNodeType>(
            PlSqlGrammar.CREATE_FUNCTION,
            PlSqlGrammar.FUNCTION_DECLARATION,
        )
    }
}
