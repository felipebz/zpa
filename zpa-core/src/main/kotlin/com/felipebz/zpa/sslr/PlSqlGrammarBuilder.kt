//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.sslr

import com.felipebz.flr.api.Grammar
import com.felipebz.flr.api.TokenType
import com.felipebz.flr.grammar.ContextKey
import com.felipebz.flr.grammar.GrammarRuleBuilder
import com.felipebz.flr.grammar.GrammarRuleKey
import com.felipebz.flr.grammar.LexerfulGrammarBuilder
import com.felipebz.flr.internal.vm.lexerful.TokenTypeDispatchExpression

class PlSqlGrammarBuilder(private val builder: LexerfulGrammarBuilder) {

    fun build(): Grammar = builder.build()

    fun rule(ruleKey: GrammarRuleKey): GrammarRuleBuilder = builder.rule(ruleKey)

    fun setRootRule(ruleKey: GrammarRuleKey) {
        builder.setRootRule(ruleKey)
    }

    fun sequence(e1: Any, e2: Any): Any = builder.sequence(e1, e2)

    fun sequence(e1: Any, e2: Any, vararg rest: Any): Any = builder.sequence(e1, e2, *rest)

    fun firstOf(e1: Any, e2: Any): Any = builder.firstOf(e1, e2)

    fun firstOf(e1: Any, e2: Any, vararg rest: Any): Any = builder.firstOf(e1, e2, *rest)

    fun firstOf(rules: List<GrammarRuleKey>): Any {
        require(rules.size >= 2) { "firstOf requires at least 2 rules" }
        val rest = Array<Any>(rules.size - 2) { i -> rules[i + 2] }
        return builder.firstOf(rules[0], rules[1], *rest)
    }

    fun <T> withContext(key: ContextKey<T>, value: T, e: Any): Any = builder.withContext(key, value, e)

    fun <T> withContext(key: ContextKey<T>, value: T, e1: Any, vararg rest: Any): Any =
        builder.withContext(key, value, e1, *rest)

    fun <T> withoutContext(key: ContextKey<T>, e: Any): Any = builder.withoutContext(key, e)

    fun <T> withoutContext(key: ContextKey<T>, e1: Any, vararg rest: Any): Any =
        builder.withoutContext(key, e1, *rest)

    fun <T> requireContext(key: ContextKey<T>, value: T): Any = builder.requireContext(key, value)

    fun <T> requireContext(key: ContextKey<T>): Any = builder.requireContext(key)

    fun optional(e: Any): Any = builder.optional(e)

    fun optional(e1: Any, vararg rest: Any): Any = builder.optional(e1, *rest)

    fun oneOrMore(e: Any): Any = builder.oneOrMore(e)

    fun oneOrMore(e1: Any, vararg rest: Any): Any = builder.oneOrMore(e1, *rest)

    fun zeroOrMore(e: Any): Any = builder.zeroOrMore(e)

    fun zeroOrMore(e1: Any, vararg rest: Any): Any = builder.zeroOrMore(e1, *rest)

    fun next(e: Any): Any = builder.next(e)

    fun next(e1: Any, vararg rest: Any): Any = builder.next(e1, *rest)

    fun nextNot(e: Any): Any = builder.nextNot(e)

    fun nextNot(e1: Any, vararg rest: Any): Any = builder.nextNot(e1, *rest)

    /**
     * Matches each expression at most once, in any order. Every branch consumes the expression it starts
     * with, so the ordered choice always takes whichever expression comes next in the input.
     *
     * The expression tree grows factorially (n! paths), so this is only for small, closed sets; larger or
     * open-ended sets must use a repetition with an explicit alternative list instead.
     */
    fun anyOrder(e1: Any, vararg rest: Any): Any {
        require(rest.size <= 2) { "anyOrder is limited to three expressions; got ${rest.size + 1}" }
        return anyOrder(listOf(e1, *rest))
    }

    private fun anyOrder(expressions: List<Any>): Any {
        if (expressions.size == 1) return builder.optional(expressions.first())
        val branches = expressions.map { e -> builder.sequence(e, anyOrder(expressions - e)) }
        return builder.optional(builder.firstOf(branches[0], branches[1], *branches.drop(2).toTypedArray()))
    }

    fun nothing(): Any = builder.nothing()

    fun buildWithMemoizationOfMatchesForAllRules(): Grammar = builder.buildWithMemoizationOfMatchesForAllRules()

    fun adjacent(e: Any): Any = builder.adjacent(e)

    fun anyTokenButNot(e: Any): Any = builder.anyTokenButNot(e)

    fun isOneOfThem(t1: TokenType, vararg rest: TokenType): Any = builder.isOneOfThem(t1, *rest)

    fun isOneOfThem(tokens: Array<TokenType>): Any {
        require(tokens.isNotEmpty())
        return builder.isOneOfThem(tokens[0], *tokens.copyOfRange(1, tokens.size))
    }

    fun tokenTypeDispatch(vararg branches: Pair<TokenType, Any>): Any {
        return TokenTypeDispatchExpression(
            branches.associate { (tokenType, ruleKey) ->
                tokenType to builder.convertToExpression(ruleKey)
            }
        )
    }

    fun bridge(from: TokenType, to: TokenType): Any = builder.bridge(from, to)

    fun anyToken(): Any = builder.anyToken()

    fun tillNewLine(): Any = builder.tillNewLine()

    fun till(e: Any): Any = builder.till(e)

    fun exclusiveTill(e: Any): Any = builder.exclusiveTill(e)

    fun exclusiveTill(e1: Any, vararg rest: Any): Any = builder.exclusiveTill(e1, *rest)

}
