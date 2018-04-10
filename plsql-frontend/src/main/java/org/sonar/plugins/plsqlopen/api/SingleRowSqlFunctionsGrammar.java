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
import static org.sonar.plugins.plsqlopen.api.DmlGrammar.ORDER_BY_CLAUSE;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

public enum SingleRowSqlFunctionsGrammar implements GrammarRuleKey {

    // internals
    XML_COLUMN,
    XML_NAMESPACE,
    XMLNAMESPACES_CLAUSE,
    XMLTABLE_OPTIONS,
    XML_PASSING_CLAUSE,
    XML_TABLE_COLUMN,
    
    // functions
    EXTRACT_DATETIME_EXPRESSION,
    XMLATTRIBUTES_EXPRESSION,
    XMLELEMENT_EXPRESSION,
    XMLFOREST_EXPRESSION,
    XMLSERIALIZE_EXPRESSION,
    XMLAGG_EXPRESSION,
    XMLEXISTS_EXPRESSION,
    XMLQUERY_EXPRESSION,
    XMLROOT_EXPRESSION,
    XMLCAST_EXPRESSION,
    XMLCOLATTVAL_EXPRESSION,
    XMLPARSE_EXPRESSION,
    XMLPI_EXPRESSION,
    XMLTABLE_EXPRESSION,
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
                XMLAGG_EXPRESSION,
                XMLEXISTS_EXPRESSION,
                XMLQUERY_EXPRESSION,
                XMLROOT_EXPRESSION,
                XMLCAST_EXPRESSION,
                XMLCOLATTVAL_EXPRESSION,
                XMLPARSE_EXPRESSION,
                XMLPI_EXPRESSION,
                XMLTABLE_EXPRESSION,
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
        
        b.rule(XMLAGG_EXPRESSION).is(
                XMLAGG, LPARENTHESIS,
                EXPRESSION, b.optional(ORDER_BY_CLAUSE),
                RPARENTHESIS);
        
        b.rule(XMLFOREST_EXPRESSION).is(
                XMLFOREST, LPARENTHESIS,
                XML_COLUMN, b.zeroOrMore(COMMA, XML_COLUMN),
                RPARENTHESIS);
        
        b.rule(XML_PASSING_CLAUSE).is(
                PASSING, b.optional(BY, VALUE),
                IDENTIFIER_NAME, b.optional(AS, IDENTIFIER_NAME),
                b.zeroOrMore(COMMA, IDENTIFIER_NAME, b.optional(AS, IDENTIFIER_NAME)));
        
        b.rule(XMLEXISTS_EXPRESSION).is(
                XMLEXISTS, LPARENTHESIS, STRING_LITERAL,
                b.optional(XML_PASSING_CLAUSE),
                RPARENTHESIS);
        
        b.rule(XMLQUERY_EXPRESSION).is(
                XMLQUERY, LPARENTHESIS, STRING_LITERAL,
                b.optional(XML_PASSING_CLAUSE),
                RETURNING, CONTENT,
                b.optional(NULL, ON, EMPTY),
                RPARENTHESIS);
        
        b.rule(XMLROOT_EXPRESSION).is(
                XMLROOT, LPARENTHESIS,
                EXPRESSION, COMMA,
                VERSION, b.firstOf(b.sequence(NO, VALUE), EXPRESSION),
                b.optional(COMMA, STANDALONE, b.firstOf(YES, b.sequence(NO, b.optional(VALUE)))),
                RPARENTHESIS);
        
        b.rule(XMLCAST_EXPRESSION).is(
                XMLCAST, LPARENTHESIS,
                EXPRESSION, AS, DATATYPE,
                RPARENTHESIS);
        
        b.rule(XMLCOLATTVAL_EXPRESSION).is(
                XMLCOLATTVAL, LPARENTHESIS,
                XML_COLUMN, b.zeroOrMore(COMMA, XML_COLUMN),
                RPARENTHESIS);
        
        b.rule(XMLPARSE_EXPRESSION).is(
                XMLPARSE, LPARENTHESIS,
                b.firstOf(DOCUMENT, CONTENT), EXPRESSION, b.optional(WELLFORMED),
                RPARENTHESIS);
        
        b.rule(XMLPI_EXPRESSION).is(
                XMLPI, LPARENTHESIS,
                b.firstOf(
                        b.sequence(EVALNAME, EXPRESSION),
                        b.sequence(b.optional(NAME), IDENTIFIER_NAME)),
                b.optional(COMMA, EXPRESSION),
                RPARENTHESIS);
        
        b.rule(XML_NAMESPACE).is(
                b.firstOf(
                        b.sequence(DEFAULT, STRING_LITERAL),
                        b.sequence(STRING_LITERAL, AS, IDENTIFIER_NAME)));

        b.rule(XMLNAMESPACES_CLAUSE).is(
                XMLNAMESPACES, LPARENTHESIS,
                XML_NAMESPACE, b.zeroOrMore(COMMA, XML_NAMESPACE),
                RPARENTHESIS);
        
        b.rule(XML_TABLE_COLUMN).is(
                IDENTIFIER_NAME,
                b.firstOf(
                        b.sequence(FOR, ORDINALITY),
                        b.sequence(
                                b.firstOf(
                                        DATATYPE,
                                        b.sequence(XMLTYPE, b.optional(LPARENTHESIS, SEQUENCE, RPARENTHESIS, BY, REF))),
                                b.optional(PATH, STRING_LITERAL),
                                b.optional(DEFAULT, STRING_LITERAL))));
        
        b.rule(XMLTABLE_OPTIONS).is(
                b.optional(XML_PASSING_CLAUSE),
                b.optional(RETURNING, SEQUENCE, BY, REF),
                b.optional(COLUMNS, XML_TABLE_COLUMN, b.zeroOrMore(COMMA, XML_TABLE_COLUMN)));
        
        b.rule(XMLTABLE_EXPRESSION).is(
                XMLTABLE, LPARENTHESIS,                
                b.optional(XMLNAMESPACES_CLAUSE, COMMA), STRING_LITERAL, XMLTABLE_OPTIONS,
                RPARENTHESIS);
    }

}
