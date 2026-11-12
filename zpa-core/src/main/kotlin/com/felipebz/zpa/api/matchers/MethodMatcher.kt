//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.matchers

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.symbols.PlSqlType
import com.felipebz.zpa.api.syntax.MethodCall
import com.felipebz.zpa.symbols.EffectiveSemanticTypeQuery
import com.felipebz.zpa.symbols.EffectiveTypeCategory
import java.util.function.Predicate

class MethodMatcher private constructor()
{

    private var methodNameCriteria: NameCriteria? = null
    private var packageNameCriteria: NameCriteria? = null
    private var schemaNameCriteria: NameCriteria? = null
    private var shouldCheckParameters = true
    private var schemaIsOptional = false
    var methodName: String = ""
        private set
    private class ArgumentConstraint(
        @Suppress("DEPRECATION") val type: PlSqlType? = null,
        val predicate: Predicate<AstNode>? = null
    )

    private val expectedArguments = ArrayList<ArgumentConstraint>()

    fun name(methodNameCriteria: String) =
        name(NameCriteria.`is`(methodNameCriteria))

    fun name(methodNameCriteria: NameCriteria) = apply {
        check(this.methodNameCriteria == null)
        this.methodNameCriteria = methodNameCriteria
    }

    fun packageName(packageNameCriteria: String) =
        packageName(NameCriteria.`is`(packageNameCriteria))

    fun packageName(packageNameCriteria: NameCriteria) = apply {
        check(this.packageNameCriteria == null)
        this.packageNameCriteria = packageNameCriteria
    }

    fun schema(schemaNameCriteria: String) =
        schema(NameCriteria.`is`(schemaNameCriteria))

    fun schema(schemaNameCriteria: NameCriteria) = apply {
        check(this.schemaNameCriteria == null)
        this.schemaNameCriteria = schemaNameCriteria
    }

    fun withNoParameterConstraint() = apply {
        check(this.expectedArguments.isEmpty())
        this.shouldCheckParameters = false
    }

    fun schemaIsOptional() = apply {
        this.schemaIsOptional = true
    }

    fun addParameter() = apply {
        check(this.shouldCheckParameters)
        expectedArguments.add(ArgumentConstraint())
    }

    /**
     * Deprecated: part of the legacy symbol model, removed in the next major version. Use [addParameter] with a
     * predicate on the argument's expression node instead (for example [nullLiteral]).
     */
    @Deprecated(
        "Part of the legacy symbol model. It will be removed in the next major version. Use addParameter(Predicate) " +
        "with a predicate on the argument's expression node instead, for example MethodMatcher.nullLiteral()."
    )
    @Suppress("DEPRECATION")
    fun addParameter(type: PlSqlType) = apply {
        check(this.shouldCheckParameters)
        expectedArguments.add(ArgumentConstraint(type = type))
    }

    /**
     * Expects one more argument, accepted only when [predicate] accepts its expression node: the expression of a
     * positional actual, or the expression after `=>` of a named one. Constraints apply to the written arguments in
     * order, whatever notation the call uses.
     */
    @ZpaExperimentalApi
    fun addParameter(predicate: Predicate<AstNode>) = apply {
        check(this.shouldCheckParameters)
        expectedArguments.add(ArgumentConstraint(predicate = predicate))
    }

    fun addParameters(quantity: Int) = apply {
        for (i in 0 until quantity) {
            addParameter()
        }
    }

    /**
     * Deprecated: part of the legacy symbol model, removed in the next major version. Use [addParameters] with
     * predicates on the arguments' expression nodes instead (for example [nullLiteral]).
     */
    @Deprecated(
        "Part of the legacy symbol model. It will be removed in the next major version. Use addParameters(vararg " +
        "Predicate) with predicates on the arguments' expression nodes instead, for example MethodMatcher.nullLiteral()."
    )
    @Suppress("DEPRECATION")
    fun addParameters(vararg types: PlSqlType) = apply {
        check(this.shouldCheckParameters)
        for (type in types) {
            addParameter(type)
        }
    }

    /** As [addParameter] with a predicate, once per predicate, in order. */
    @ZpaExperimentalApi
    @OptIn(ZpaExperimentalApi::class)
    fun addParameters(vararg predicates: Predicate<AstNode>) = apply {
        check(this.shouldCheckParameters)
        for (predicate in predicates) {
            addParameter(predicate)
        }
    }

    fun getArguments(node: AstNode): List<AstNode> {
        val arguments = node.getFirstChildOrNull(PlSqlGrammar.ARGUMENTS)
        return arguments?.getChildren(PlSqlGrammar.ARGUMENT) ?: ArrayList()

    }

    fun getArgumentsValues(node: AstNode) =
        getArguments(node).map { it.lastChild }.toList()

    fun matches(originalNode: AstNode): Boolean {
        val node = normalize(originalNode)

        var componentCount = 0
        var firstComponent: String? = null
        var secondComponent: String? = null
        var thirdComponent: String? = null
        val children = node.children
        var childIndex = 0
        while (childIndex < children.size) {
            val child = children[childIndex]
            if (componentCount < 3 &&
                (child.type === PlSqlGrammar.VARIABLE_NAME || child.type === PlSqlGrammar.IDENTIFIER_NAME)) {
                when (componentCount) {
                    0 -> firstComponent = child.tokenValue
                    1 -> secondComponent = child.tokenValue
                    else -> thirdComponent = child.tokenValue
                }
                componentCount++
            }
            childIndex++
        }

        if (componentCount == 0) {
            return false
        }

        return matchesNameComponents(
            componentCount,
            firstComponent,
            secondComponent,
            thirdComponent,
        ) && argumentsAcceptable(originalNode)
    }

    @ZpaExperimentalApi
    @OptIn(ZpaExperimentalApi::class)
    fun matches(call: MethodCall): Boolean = matches(call.astNode)

    private fun matchesNameComponents(
        componentCount: Int,
        firstComponent: String?,
        secondComponent: String?,
        thirdComponent: String?,
    ): Boolean {
        val methodMatches = matchesMethodComponent(componentCount, firstComponent, secondComponent, thirdComponent)
        val packageMatches = matchesPackageComponent(componentCount, firstComponent, secondComponent, thirdComponent)
        val schemaMatches = matchesSchemaComponent(componentCount, firstComponent, secondComponent, thirdComponent)

        return methodMatches and packageMatches and schemaMatches &&
            consumedComponentCount(componentCount) == componentCount
    }

    private fun matchesMethodComponent(
        componentCount: Int,
        firstComponent: String?,
        secondComponent: String?,
        thirdComponent: String?,
    ): Boolean {
        val criteria = methodNameCriteria ?: return true
        return nameAcceptable(componentAt(componentCount - 1, firstComponent, secondComponent, thirdComponent), criteria)
    }

    private fun matchesPackageComponent(
        componentCount: Int,
        firstComponent: String?,
        secondComponent: String?,
        thirdComponent: String?,
    ): Boolean {
        val criteria = packageNameCriteria ?: return true
        val componentIndex = componentCount - 1 - methodNameCriteria.configuredComponentCount()
        return componentIndex > -1 &&
            nameAcceptable(componentAt(componentIndex, firstComponent, secondComponent, thirdComponent), criteria)
    }

    private fun matchesSchemaComponent(
        componentCount: Int,
        firstComponent: String?,
        secondComponent: String?,
        thirdComponent: String?,
    ): Boolean {
        val criteria = schemaNameCriteria ?: return true
        val componentIndex = componentCount - 1 - methodNameCriteria.configuredComponentCount() -
            consumedPackageComponentCount(componentCount)

        return (schemaIsOptional && componentIndex == -1) ||
            (componentIndex > -1 &&
                nameAcceptable(componentAt(componentIndex, firstComponent, secondComponent, thirdComponent), criteria))
    }

    private fun consumedComponentCount(componentCount: Int): Int {
        var consumed = methodNameCriteria.configuredComponentCount()
        consumed += consumedPackageComponentCount(componentCount)
        if (schemaNameCriteria != null && consumed < componentCount) consumed++
        return consumed
    }

    private fun consumedPackageComponentCount(componentCount: Int): Int {
        return if (packageNameCriteria != null && methodNameCriteria.configuredComponentCount() < componentCount) 1 else 0
    }

    private fun NameCriteria?.configuredComponentCount() = if (this == null) 0 else 1

    private fun componentAt(
        index: Int,
        firstComponent: String?,
        secondComponent: String?,
        thirdComponent: String?,
    ): String? = when (index) {
        2 -> thirdComponent
        1 -> secondComponent
        else -> firstComponent
    }

    private fun nameAcceptable(name: String?, criteria: NameCriteria): Boolean {
        methodName = name ?: ""
        return criteria.matches(methodName)
    }

    private fun argumentsAcceptable(node: AstNode): Boolean {
        val arguments = getArguments(node)
        return !shouldCheckParameters || arguments.size == expectedArguments.size && argumentsAreAccepted(arguments)
    }

    @Suppress("DEPRECATION")
    private fun argumentsAreAccepted(arguments: List<AstNode>): Boolean {
        var result = true
        for ((i, constraint) in expectedArguments.withIndex()) {
            constraint.predicate?.let { result = result and it.test(arguments[i].lastChild) }
            val type = constraint.type
            if (type == null || type === PlSqlType.UNKNOWN) continue
            val actualArgument = arguments[i].firstChild
            val actualType = when (val category = EffectiveSemanticTypeQuery.typeCategory(semantic(actualArgument))) {
                is EffectiveTypeCategory.Known -> category.type
                EffectiveTypeCategory.Unknown -> PlSqlType.UNKNOWN
            }
            result = result and (type === actualType)
        }
        return result
    }

    private fun normalize(node: AstNode): AstNode {
        if (node.type === PlSqlGrammar.METHOD_CALL || node.type === PlSqlGrammar.CALL_STATEMENT) {
            var child = normalize(node.firstChild)
            if (child.firstChild.type === PlSqlGrammar.HOST_AND_INDICATOR_VARIABLE) {
                child = child.firstChild
            }
            return child
        }
        return node
    }

    companion object {
        @JvmStatic
        fun create(): MethodMatcher {
            return MethodMatcher()
        }

        fun semantic(node: AstNode): SemanticAstNode {
            return node as SemanticAstNode
        }

        private val NULL_LITERAL = Predicate<AstNode> { isNullLiteral(it) }

        /**
         * Accepts the `NULL` literal and an empty character literal (`''`, `N''`, `q'[]'`): Oracle treats an empty
         * string as NULL. Any other expression, including a string of blanks, is rejected.
         */
        @ZpaExperimentalApi
        @JvmStatic
        fun nullLiteral(): Predicate<AstNode> = NULL_LITERAL

        private fun isNullLiteral(expression: AstNode): Boolean {
            if (expression.type !== PlSqlGrammar.LITERAL) return false
            if (expression.hasDirectChildren(PlSqlGrammar.NULL_LITERAL)) return true
            val value = expression.getFirstChildOrNull(PlSqlGrammar.CHARACTER_LITERAL)?.tokenValue ?: return false
            val opening = value.indexOf('\'')
            val closing = value.lastIndexOf('\'')
            if (opening < 0 || closing <= opening) return false
            val content = value.substring(opening + 1, closing)
            // An alternative-quoted literal keeps its opening and closing delimiters inside the quotes.
            return if (value.substring(0, opening).endsWith('q', ignoreCase = true)) content.length == 2 else content.isEmpty()
        }
    }

}
