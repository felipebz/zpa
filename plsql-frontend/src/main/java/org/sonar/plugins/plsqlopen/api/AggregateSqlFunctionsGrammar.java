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
package org.sonar.plugins.plsqlopen.api;

import static org.sonar.plugins.plsqlopen.api.PlSqlKeyword.*;
import static org.sonar.plugins.plsqlopen.api.PlSqlGrammar.*;
import static org.sonar.plugins.plsqlopen.api.PlSqlPunctuator.*;
import static org.sonar.plugins.plsqlopen.api.PlSqlTokenType.*;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

public enum AggregateSqlFunctionsGrammar implements GrammarRuleKey {

    LISTAGG_EXPRESSION,
    AGGREGATE_SQL_FUNCTION;

    public static void buildOn(LexerfulGrammarBuilder b) {
        b.rule(LISTAGG_EXPRESSION).is(
                LISTAGG,
                LPARENTHESIS, EXPRESSION, b.optional(COMMA, STRING_LITERAL), RPARENTHESIS,
                WITHIN, GROUP, LPARENTHESIS, DmlGrammar.ORDER_BY_CLAUSE, RPARENTHESIS);
        
        b.rule(AGGREGATE_SQL_FUNCTION).is(LISTAGG_EXPRESSION);
    }

}