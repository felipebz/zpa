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
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.annotations.ActivatedByDefault
import com.felipebz.zpa.api.annotations.ConstantRemediation
import com.felipebz.zpa.api.annotations.Priority
import com.felipebz.zpa.api.annotations.Rule
import com.felipebz.zpa.api.annotations.RuleInfo
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.PackageSpecificationResolution
import com.felipebz.zpa.api.project.SequenceReferenceResolution
import com.felipebz.zpa.api.syntax.MethodCall
import com.felipebz.zpa.api.syntax.SyntaxViews
import com.felipebz.zpa.api.squid.SemanticAstNode

/**
 * Reports deterministic functions only when their own implementation contains strong syntactic or
 * semantic evidence of changing state or a side effect. User-defined calls and unresolved package
 * or global state are intentionally not treated as proof of impurity.
 */
@Rule(priority = Priority.MAJOR, tags = [Tags.BUG])
@ConstantRemediation("10min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class DeterministicFunctionCheck : AbstractBaseCheck() {

    private val reportedFunctions = java.util.Collections.newSetFromMap(
        java.util.IdentityHashMap<AstNode, Boolean>()
    )

    override fun startScan() {
        super.startScan()
        reportedFunctions.clear()
    }

    override fun init() {
        subscribeTo(PlSqlGrammar.FUNCTION_DECLARATION, PlSqlGrammar.CREATE_FUNCTION)
        subscribeTo(SyntaxViews.METHOD_CALL, ::visitMethodCall)
    }

    override fun visitNode(node: AstNode) {
        if (!isDeterministicFunction(node)) return

        val roots = implementationRoots(node)
        if (roots.isNotEmpty() && hasDirectViolation(node, roots)) {
            report(node)
        }
    }

    private fun visitMethodCall(call: MethodCall) {
        if (!isKnownNonDeterministicCall(call)) return

        val function = call.astNode.getFirstAncestorOrNull(
            PlSqlGrammar.FUNCTION_DECLARATION,
            PlSqlGrammar.CREATE_FUNCTION
        ) ?: return
        if (!isDeterministicFunction(function) || !isInImplementation(function, call.astNode)) {
            return
        }

        report(function)
    }

    private fun report(function: AstNode) {
        if (!reportedFunctions.add(function)) return
        val location = function.getFirstChildOrNull(PlSqlKeyword.DETERMINISTIC)
            ?: function.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)
            ?: function.getFirstChildOrNull(PlSqlKeyword.FUNCTION)
            ?: function
        addIssue(location, getLocalizedMessage())
    }

    private fun implementationRoots(function: AstNode): List<AstNode> = listOfNotNull(
        function.getFirstChildOrNull(PlSqlGrammar.DECLARE_SECTION),
        function.getFirstChildOrNull(PlSqlGrammar.STATEMENTS_SECTION)
    )

    private fun isDeterministicFunction(function: AstNode): Boolean {
        if (function.type === PlSqlGrammar.FUNCTION_DECLARATION && isPackageBodyFunction(function)) {
            val resolution = projectAnalysis().resolvePackageSpecification(function)
            return resolution.status == PackageSpecificationResolution.Status.RESOLVED &&
                resolution.specification.orElse(null)?.isDeterministic == true
        }
        return function.hasDirectChildren(PlSqlKeyword.DETERMINISTIC)
    }

    private fun isPackageBodyFunction(function: AstNode): Boolean =
        function.parentOrNull?.type == PlSqlGrammar.DECLARE_SECTION &&
            function.parentOrNull?.parentOrNull?.type == PlSqlGrammar.CREATE_PACKAGE_BODY

    private fun hasDirectViolation(function: AstNode, roots: List<AstNode>): Boolean {
        return hasExternalEnvironmentReference(function, roots) ||
            hasSequenceReference(function, roots) ||
            hasKnownNonDeterministicMember(function, roots) ||
            hasDatabaseRead(function, roots) ||
            hasSideEffectingStatement(function, roots) ||
            hasOutParameter(function)
    }

    private fun hasExternalEnvironmentReference(function: AstNode, roots: List<AstNode>): Boolean {
        return roots.asSequence()
            .flatMap { it.getDescendants(PlSqlGrammar.VARIABLE_NAME).asSequence() }
            .filter { belongsTo(function, it) }
            .any { variable ->
                val name = variable.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)
                    ?.tokenOriginalValue
                    ?: return@any false
                isUnquoted(name) && name.equalsAnyIgnoreCase(EXTERNAL_ENVIRONMENT_NAMES) &&
                    (variable as? SemanticAstNode)?.symbol == null
            }
    }

    private fun hasSequenceReference(function: AstNode, roots: List<AstNode>): Boolean {
        return roots.asSequence()
            .flatMap { it.getDescendants(PlSqlKeyword.NEXTVAL, PlSqlKeyword.CURRVAL).asSequence() }
            .filter { belongsTo(function, it) }
            .any { pseudocolumn ->
                val member = pseudocolumn.getFirstAncestorOrNull(PlSqlGrammar.MEMBER_EXPRESSION)
                    ?: return@any false
                projectAnalysis().resolveSequenceReference(member) == SequenceReferenceResolution.RESOLVED_SEQUENCE
            }
    }

    private fun hasKnownNonDeterministicMember(function: AstNode, roots: List<AstNode>): Boolean {
        return roots.asSequence()
            .flatMap { it.getDescendants(PlSqlGrammar.MEMBER_EXPRESSION).asSequence() }
            .filter { belongsTo(function, it) }
            .any { member ->
                val components = member.children
                    .filter { it.type === PlSqlGrammar.VARIABLE_NAME || it.type === PlSqlGrammar.IDENTIFIER_NAME }
                    .mapNotNull { component ->
                        if (component.type === PlSqlGrammar.VARIABLE_NAME) {
                            component.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)?.tokenOriginalValue
                        } else {
                            component.tokenOriginalValue
                        }
                    }
                components.size == 2 &&
                    isUnquoted(components[0]) &&
                    isUnquoted(components[1]) &&
                    components[0].equals("DBMS_RANDOM", ignoreCase = true) &&
                    components[1].equalsAnyIgnoreCase(DBMS_RANDOM_CALLS)
            }
    }

    private fun hasDatabaseRead(function: AstNode, roots: List<AstNode>): Boolean {
        return roots.asSequence()
            .flatMap { it.getDescendants(DmlGrammar.SELECT_EXPRESSION).asSequence() }
            .filter { belongsTo(function, it) }
            .any { select ->
                select.getDescendants(DmlGrammar.TABLE_REFERENCE)
                    .filter {
                        belongsTo(function, it) &&
                            it.getFirstAncestorOrNull(DmlGrammar.SELECT_EXPRESSION) === select
                    }
                    .any { !isDual(it) && !isCteReference(select, it) }
            }
    }

    private fun isCteReference(selectExpression: AstNode, tableReference: AstNode): Boolean {
        val tableComponents = tableReference.getChildren(PlSqlGrammar.IDENTIFIER_NAME)
        if (tableComponents.size != 1) return false
        val tableName = tableComponents.single().tokenOriginalValue
        return visibleCteNames(selectExpression).any { sameIdentifier(tableName, it) }
    }

    private fun visibleCteNames(selectExpression: AstNode): List<String> {
        return generateSequence(selectExpression) { it.parentOrNull }
            .filter { it.type === DmlGrammar.SELECT_EXPRESSION }
            .flatMap { directCteNames(it).asSequence() }
            .toList()
    }

    private fun directCteNames(selectExpression: AstNode): List<String> =
        selectExpression.getDescendants(DmlGrammar.SUBQUERY_FACTORING_CLAUSE)
            .filter { it.getFirstAncestorOrNull(DmlGrammar.SELECT_EXPRESSION) === selectExpression }
            .mapNotNull {
                it.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)?.tokenOriginalValue
            }

    private fun hasSideEffectingStatement(function: AstNode, roots: List<AstNode>): Boolean {
        return roots.asSequence()
            .flatMap { it.getDescendants(*SIDE_EFFECTING_STATEMENTS).asSequence() }
            .any { belongsTo(function, it) }
    }

    private fun hasOutParameter(function: AstNode): Boolean {
        val parameters = function.getFirstChildOrNull(PlSqlGrammar.PARAMETER_DECLARATIONS) ?: return false
        return parameters.getChildren(PlSqlGrammar.PARAMETER_DECLARATION)
            .any { it.hasDirectChildren(PlSqlKeyword.OUT) }
    }

    private fun isKnownNonDeterministicCall(call: MethodCall): Boolean {
        val name = call.name
        if (!isUnquoted(name)) return false

        return when {
            call.qualifier.isEmpty() && name.equalsAnyIgnoreCase(KNOWN_NON_DETERMINISTIC_CALLS) -> true
            call.qualifier.size == 1 &&
                isUnquoted(call.qualifier.single()) &&
                call.qualifier.single().equals("DBMS_RANDOM", ignoreCase = true) &&
                name.equalsAnyIgnoreCase(DBMS_RANDOM_CALLS) -> true
            else -> false
        }
    }

    private fun isInImplementation(function: AstNode, node: AstNode): Boolean {
        val implementation = node.getFirstAncestorOrNull(
            PlSqlGrammar.DECLARE_SECTION,
            PlSqlGrammar.STATEMENTS_SECTION
        ) ?: return false
        return nearestSubprogram(implementation) === function
    }

    private fun belongsTo(function: AstNode, node: AstNode): Boolean =
        nearestSubprogram(node) === function

    private fun nearestSubprogram(node: AstNode): AstNode? =
        node.getFirstAncestorOrNull(*SUBPROGRAM_TYPES)

    private fun isDual(tableReference: AstNode): Boolean {
        if (tableReference.getDescendants(PlSqlPunctuator.REMOTE).isNotEmpty()) return false
        val components = tableReference.getChildren(PlSqlGrammar.IDENTIFIER_NAME)
        val names = components.map { it.tokenOriginalValue }
        return when {
            names.size == 1 -> isUnquoted(names.single()) && names.single().equals("DUAL", ignoreCase = true)
            names.size == 2 -> isUnquoted(names[0]) && isUnquoted(names[1]) &&
                names[0].equals("SYS", ignoreCase = true) && names[1].equals("DUAL", ignoreCase = true)
            else -> false
        }
    }

    private fun sameIdentifier(first: String, second: String): Boolean =
        if (isUnquoted(first) && isUnquoted(second)) {
            first.equals(second, ignoreCase = true)
        } else {
            first == second
        }

    private fun isUnquoted(value: String): Boolean =
        !value.startsWith("\"") && !value.endsWith("\"")

    private fun String.equalsAnyIgnoreCase(values: Set<String>): Boolean =
        values.any { equals(it, ignoreCase = true) }

    private companion object {
        val EXTERNAL_ENVIRONMENT_NAMES = setOf(
            "CURRENT_DATE",
            "CURRENT_TIMESTAMP",
            "CURRENT_USER",
            "CURRENT_SCHEMA",
            "DBTIMEZONE",
            "LOCALTIMESTAMP",
            "SESSIONTIMEZONE",
            "SESSION_USER",
            "SYSDATE",
            "SYSTIMESTAMP",
            "USER",
        )

        val KNOWN_NON_DETERMINISTIC_CALLS = setOf("SYS_CONTEXT", "SYS_GUID")
        val DBMS_RANDOM_CALLS = setOf("RANDOM", "VALUE")

        val SIDE_EFFECTING_STATEMENTS = arrayOf(
            PlSqlGrammar.COMMIT_STATEMENT,
            PlSqlGrammar.DELETE_STATEMENT,
            PlSqlGrammar.INSERT_STATEMENT,
            PlSqlGrammar.MERGE_STATEMENT,
            PlSqlGrammar.ROLLBACK_STATEMENT,
            PlSqlGrammar.SAVEPOINT_STATEMENT,
            PlSqlGrammar.SET_TRANSACTION_STATEMENT,
            PlSqlGrammar.UPDATE_STATEMENT,
        )

        val SUBPROGRAM_TYPES = arrayOf(
            PlSqlGrammar.CREATE_FUNCTION,
            PlSqlGrammar.FUNCTION_DECLARATION,
            PlSqlGrammar.CREATE_PROCEDURE,
            PlSqlGrammar.PROCEDURE_DECLARATION,
        )
    }
}
