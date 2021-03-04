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
package org.sonar.plugins.plsqlopen.api

import org.sonar.plsqlopen.sslr.PlSqlGrammarBuilder
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar.CONCATENATION_EXPRESSION
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar.OBJECT_REFERENCE
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword.*
import org.sonar.plugins.plsqlopen.api.PlSqlPunctuator.*
import org.sonar.sslr.grammar.GrammarRuleKey

enum class ConditionsGrammar : GrammarRuleKey {

    // internal
    RELATIONAL_OPERATOR,

    // conditions
    RELATIONAL_CONDITION,
    LIKE_CONDITION,
    BETWEEN_CONDITION,
    MULTISET_CONDITION,
    IS_A_SET_CONDITION,
    IS_EMPTY_CONDITION,
    MEMBER_CONDITION,
    SUBMULTISET_CONDITION,
    IS_OF_CONDITION,
    CONDITION;

    companion object {
        fun buildOn(b: PlSqlGrammarBuilder) {
            b.rule(RELATIONAL_OPERATOR).define(b.firstOf(
                    EQUALS,
                    NOTEQUALS,
                    NOTEQUALS2,
                    NOTEQUALS3,
                    NOTEQUALS4,
                    LESSTHAN,
                    GREATERTHAN,
                    LESSTHANOREQUAL,
                    GREATERTHANOREQUAL),
                    b.optional(b.firstOf(ANY, SOME, ALL)))

            b.rule(RELATIONAL_CONDITION).define(
                    CONCATENATION_EXPRESSION, RELATIONAL_OPERATOR, CONCATENATION_EXPRESSION).skip()

            b.rule(LIKE_CONDITION).define(
                    CONCATENATION_EXPRESSION,
                    b.optional(NOT), LIKE,
                    CONCATENATION_EXPRESSION,
                    b.optional(ESCAPE, CONCATENATION_EXPRESSION))

            b.rule(BETWEEN_CONDITION).define(
                    CONCATENATION_EXPRESSION,
                    b.optional(NOT), BETWEEN,
                    CONCATENATION_EXPRESSION, AND, CONCATENATION_EXPRESSION).skip()

            b.rule(IS_A_SET_CONDITION).define(CONCATENATION_EXPRESSION, IS, b.optional(NOT), A, SET)

            b.rule(IS_EMPTY_CONDITION).define(CONCATENATION_EXPRESSION, IS, b.optional(NOT), EMPTY)

            b.rule(MEMBER_CONDITION).define(CONCATENATION_EXPRESSION, b.optional(NOT), MEMBER, b.optional(OF), CONCATENATION_EXPRESSION).skip()

            b.rule(SUBMULTISET_CONDITION).define(CONCATENATION_EXPRESSION, b.optional(NOT), SUBMULTISET, b.optional(OF), CONCATENATION_EXPRESSION)

            //https://docs.oracle.com/cloud/latest/db112/SQLRF/conditions006.htm#SQLRF52128
            b.rule(MULTISET_CONDITION).define(b.firstOf(
                    IS_A_SET_CONDITION,
                    IS_EMPTY_CONDITION,
                    MEMBER_CONDITION,
                    SUBMULTISET_CONDITION))

            b.rule(IS_OF_CONDITION).define(CONCATENATION_EXPRESSION, IS, b.optional(NOT), OF, b.optional(TYPE),
                    LPARENTHESIS,
                    b.optional(ONLY), OBJECT_REFERENCE, b.zeroOrMore(COMMA, b.optional(ONLY), OBJECT_REFERENCE),
                    RPARENTHESIS)

            b.rule(CONDITION).define(b.firstOf(
                    RELATIONAL_CONDITION,
                    LIKE_CONDITION,
                    BETWEEN_CONDITION,
                    MULTISET_CONDITION,
                    IS_OF_CONDITION)).skip()
        }
    }

}
