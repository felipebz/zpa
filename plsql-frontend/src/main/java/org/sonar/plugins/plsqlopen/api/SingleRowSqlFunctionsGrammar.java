/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2016 Felipe Zorzo
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

public enum SingleRowSqlFunctionsGrammar implements GrammarRuleKey {

    // internals
    XML_COLUMN,
    
    // functions
    EXTRACT_DATETIME_EXPRESSION,
    XMLATTRIBUTES_EXPRESSION,
    XMLELEMENT_EXPRESSION,
    XMLFOREST_EXPRESSION,
    XMLSERIALIZE_EXPRESSION,
    CAST_EXPRESSION,
    TRIM_EXPRESSION,
    SINGLE_ROW_SQL_FUNCTION;
    
    public static void buildOn(LexerfulGrammarBuilder b) {
        createCharacterFunctions(b);
        createConversionFunctions(b);
        createDateFunctions(b);
        createXmlFunctions(b);
        
        b.rule(SINGLE_ROW_SQL_FUNCTION).is(b.firstOf(
                EXTRACT_DATETIME_EXPRESSION,
                XMLATTRIBUTES_EXPRESSION,
                XMLELEMENT_EXPRESSION,
                XMLFOREST_EXPRESSION,
                XMLSERIALIZE_EXPRESSION,
                CAST_EXPRESSION,
                TRIM_EXPRESSION)).skip();
    }

    private static void createCharacterFunctions(LexerfulGrammarBuilder b) {
        b.rule(TRIM_EXPRESSION).is(
                TRIM, LPARENTHESIS,
                b.optional(b.optional(b.firstOf(LEADING, TRAILING, BOTH)), EXPRESSION, FROM),
                EXPRESSION, RPARENTHESIS);
    }

    private static void createConversionFunctions(LexerfulGrammarBuilder b) {
        b.rule(CAST_EXPRESSION).is(
                CAST, LPARENTHESIS,
                b.firstOf(b.sequence(MULTISET, EXPRESSION), EXPRESSION),
                AS, DATATYPE,
                RPARENTHESIS);
    }

    private static void createDateFunctions(LexerfulGrammarBuilder b) {
        b.rule(EXTRACT_DATETIME_EXPRESSION).is(EXTRACT, LPARENTHESIS, IDENTIFIER_NAME, FROM, EXPRESSION, RPARENTHESIS);
    }

    private static void createXmlFunctions(LexerfulGrammarBuilder b) {
        b.rule(XMLSERIALIZE_EXPRESSION).is(
                XMLSERIALIZE, LPARENTHESIS,
                b.firstOf(DOCUMENT, CONTENT), EXPRESSION,
                b.optional(AS, DATATYPE),
                b.optional(ENCODING, EXPRESSION),
                b.optional(VERSION, STRING_LITERAL),
                b.optional(b.firstOf(b.sequence(NO, IDENT), b.sequence(IDENT, b.optional(SIZE, EQUALS, EXPRESSION)))),
                b.optional(b.firstOf(HIDE, SHOW), DEFAULTS),
                RPARENTHESIS);
        
        b.rule(XML_COLUMN).is(
                EXPRESSION, b.optional(AS, b.firstOf(b.sequence(EVALNAME, EXPRESSION), IDENTIFIER_NAME)));
        
        b.rule(XMLATTRIBUTES_EXPRESSION).is(
                XMLATTRIBUTES, LPARENTHESIS,
                b.optional(b.firstOf(ENTITYESCAPING, NOENTITYESCAPING)),
                b.optional(b.firstOf(SCHEMACHECK, NOSCHEMACHECK)),
                XML_COLUMN, b.zeroOrMore(COMMA, XML_COLUMN),
                RPARENTHESIS);
        
        b.rule(XMLELEMENT_EXPRESSION).is(
                XMLELEMENT, LPARENTHESIS,
                b.optional(b.firstOf(ENTITYESCAPING, NOENTITYESCAPING)),
                b.firstOf(
                        b.sequence(EVALNAME, EXPRESSION),
                        b.sequence(b.optional(NAME), IDENTIFIER_NAME)
                        ),
                b.optional(COMMA, XMLATTRIBUTES_EXPRESSION),
                b.zeroOrMore(COMMA, EXPRESSION, b.optional(AS, IDENTIFIER_NAME)),
                RPARENTHESIS);
        
        b.rule(XMLFOREST_EXPRESSION).is(
                XMLFOREST, LPARENTHESIS,
                XML_COLUMN, b.zeroOrMore(COMMA, XML_COLUMN),
                RPARENTHESIS);
    }

}
