/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2023 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api

import com.felipebz.flr.grammar.GrammarRuleKey
import org.sonar.plsqlopen.sslr.PlSqlGrammarBuilder
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar.EXPRESSION
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword.*
import org.sonar.plugins.plsqlopen.api.PlSqlPunctuator.*

enum class AggregateSqlFunctionsGrammar : GrammarRuleKey {

    LISTAGG_EXPRESSION,
    AGGREGATE_SQL_FUNCTION;

    companion object {
        fun buildOn(b: PlSqlGrammarBuilder) {
            b.rule(LISTAGG_EXPRESSION).define(
                    LISTAGG,
                    LPARENTHESIS, b.optional(b.firstOf(ALL, DISTINCT)), EXPRESSION, b.optional(COMMA, EXPRESSION),
                    b.optional(ON, OVERFLOW, b.firstOf(
                            ERROR,
                            b.sequence(TRUNCATE, b.optional(EXPRESSION), b.optional(b.firstOf(WITH, WITHOUT), COUNT)))),
                    RPARENTHESIS,
                    WITHIN, GROUP, LPARENTHESIS, DmlGrammar.ORDER_BY_CLAUSE, RPARENTHESIS)

            b.rule(AGGREGATE_SQL_FUNCTION).define(LISTAGG_EXPRESSION)
        }
    }

}
