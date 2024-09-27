/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar.*
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword.*
import org.sonar.plugins.plsqlopen.api.PlSqlPunctuator.*
import org.sonar.plugins.plsqlopen.api.PlSqlTokenType.STRING_LITERAL

enum class SingleRowSqlFunctionsGrammar : GrammarRuleKey {

    // internals
    JSON_PASSING_CLAUSE,
    JSON_ON_NULL_CLAUSE,
    JSON_ON_ERROR_CLAUSE,
    JSON_RETURNING_CLAUSE,
    JSON_ARRAY_ENUMERATION_CONTENT,
    JSON_ARRAY_QUERY_CONTENT,
    JSON_ARRAY_ELEMENT,
    JSON_OBJECT_ENTRY,
    JSON_QUERY_RETURNING_CLAUSE,
    JSON_QUERY_WRAPPER_CLAUSE,
    JSON_QUERY_QUOTES_CLAUSE,
    JSON_QUERY_ON_ERROR_CLAUSE,
    JSON_QUERY_ON_EMPTY_CLAUSE,
    JSON_QUERY_ON_MISMATCH_CLAUSE,
    JSON_BASIC_PATH_EXPRESSION,
    JSON_PATH_EXPRESSION,
    JSON_RELATIVE_OBJECT_ACCESS,
    JSON_TABLE_ON_ERROR_CLAUSE,
    JSON_TABLE_ON_EMPTY_CLAUSE,
    JSON_TABLE_COLUMNS_CLAUSE,
    JSON_COLUMNS_CLAUSE,
    JSON_COLUMN_DEFINITION,
    JSON_EXISTS_COLUMN,
    JSON_QUERY_COLUMN,
    JSON_VALUE_COLUMN,
    JSON_NESTED_PATH,
    JSON_ORDINALITY_COLUMN,
    JSON_PATH,
    JSON_ARRAY_STEP,
    JSON_VALUE_RETURN_TYPE,
    JSON_VALUE_ON_ERROR_CLAUSE,
    JSON_VALUE_ON_EMPTY_CLAUSE,
    JSON_VALUE_ON_MISMATCH_CLAUSE,
    JSON_VALUE_RETURN_OBJECT_INSTANCE,
    JSON_TRANSFORM_OPERATION,
    JSON_TRANSFORM_RETURNING_CLAUSE,
    JSON_RHS_EXPRESSION,
    JSON_APPEND_OPERATION,
    JSON_CASE_OPERATION,
    JSON_COPY_OPERATION,
    JSON_INSERT_OPERATION,
    JSON_INTERSECT_OPERATION,
    JSON_KEEP_OPERATION,
    JSON_MERGE_OPERATION,
    JSON_MINUS_OPERATION,
    JSON_NESTED_PATH_OPERATION,
    JSON_PREPEND_OPERATION,
    JSON_REMOVE_OPERATION,
    JSON_RENAME_OPERATION,
    JSON_REPLACE_OPERATION,
    JSON_SET_OPERATION,
    JSON_SORT_OPERATION,
    JSON_UNION_OPERATION,
    JSON_VALUE_RETURNING_CLAUSE,
    XML_COLUMN,
    XML_NAMESPACE,
    XMLNAMESPACES_CLAUSE,
    XMLTABLE_OPTIONS,
    XML_PASSING_CLAUSE,
    XML_TABLE_COLUMN,

    // functions
    EXTRACT_DATETIME_EXPRESSION,
    JSON_CONSTRUCTOR,
    JSON_ARRAY_EXPRESSION,
    JSON_DATAGUIDE_EXPRESSION,
    JSON_MERGEPATCH_EXPRESSION,
    JSON_OBJECT_EXPRESSION,
    JSON_SCALAR_EXPRESSION,
    JSON_SERIALIZE_EXPRESSION,
    JSON_QUERY_EXPRESSION,
    JSON_TABLE_EXPRESSION,
    JSON_TRANSFORM_EXPRESSION,
    JSON_VALUE_EXPRESSION,
    XMLATTRIBUTES_EXPRESSION,
    XMLCAST_EXPRESSION,
    XMLCDATA_EXPRESSION,
    XMLCOLATTVAL_EXPRESSION,
    XMLCOMMENT_EXPRESSION,
    XMLCONCAT_EXPRESSION,
    XMLDIFF_EXPRESSION,
    XMLELEMENT_EXPRESSION,
    XMLEXISTS_EXPRESSION,
    XMLFOREST_EXPRESSION,
    XMLISVALID_EXPRESSION,
    XMLPARSE_EXPRESSION,
    XMLPATCH_EXPRESSION,
    XMLPI_EXPRESSION,
    XMLQUERY_EXPRESSION,
    XMLROOT_EXPRESSION,
    XMLSEQUENCE_EXPRESSION,
    XMLSERIALIZE_EXPRESSION,
    XMLTABLE_EXPRESSION,
    XMLTRANSFORM_EXPRESSION,

    DEFAULT_ON_ERROR_CLAUSE,
    TREAT_AS_EXPRESSION,
    SET_EXPRESSION,
    CAST_EXPRESSION,
    TO_BINARY_DOUBLE_EXPRESSION,
    TRIM_EXPRESSION,
    TABLE_EXPRESSION,
    THE_EXPRESSION,
    CURSOR_EXPRESSION,
    SINGLE_ROW_SQL_FUNCTION;

    companion object {
        fun buildOn(b: PlSqlGrammarBuilder) {
            createCharacterFunctions(b)
            createConversionFunctions(b)
            createDateFunctions(b)
            createXmlFunctions(b)
            createJsonFunctions(b)

            b.rule(SINGLE_ROW_SQL_FUNCTION).define(
                b.firstOf(
                    EXTRACT_DATETIME_EXPRESSION,
                    JSON_CONSTRUCTOR,
                    JSON_ARRAY_EXPRESSION,
                    JSON_DATAGUIDE_EXPRESSION,
                    JSON_MERGEPATCH_EXPRESSION,
                    JSON_OBJECT_EXPRESSION,
                    JSON_SCALAR_EXPRESSION,
                    JSON_SERIALIZE_EXPRESSION,
                    JSON_QUERY_EXPRESSION,
                    JSON_TABLE_EXPRESSION,
                    JSON_TRANSFORM_EXPRESSION,
                    JSON_VALUE_EXPRESSION,
                    XMLATTRIBUTES_EXPRESSION,
                    XMLCAST_EXPRESSION,
                    XMLCDATA_EXPRESSION,
                    XMLCOLATTVAL_EXPRESSION,
                    XMLCOMMENT_EXPRESSION,
                    XMLCONCAT_EXPRESSION,
                    XMLDIFF_EXPRESSION,
                    XMLELEMENT_EXPRESSION,
                    XMLEXISTS_EXPRESSION,
                    XMLFOREST_EXPRESSION,
                    XMLISVALID_EXPRESSION,
                    XMLPARSE_EXPRESSION,
                    XMLPATCH_EXPRESSION,
                    XMLPI_EXPRESSION,
                    XMLQUERY_EXPRESSION,
                    XMLROOT_EXPRESSION,
                    XMLSEQUENCE_EXPRESSION,
                    XMLSERIALIZE_EXPRESSION,
                    XMLTABLE_EXPRESSION,
                    XMLTRANSFORM_EXPRESSION,
                    TREAT_AS_EXPRESSION,
                    SET_EXPRESSION,
                    CAST_EXPRESSION,
                    TO_BINARY_DOUBLE_EXPRESSION,
                    TRIM_EXPRESSION,
                    TABLE_EXPRESSION,
                    THE_EXPRESSION,
                    CURSOR_EXPRESSION
                )
            ).skip()
        }

        private fun createCharacterFunctions(b: PlSqlGrammarBuilder) {
            b.rule(TRIM_EXPRESSION).define(
                    TRIM, LPARENTHESIS,
                    b.optional(b.optional(b.firstOf(LEADING, TRAILING, BOTH)), EXPRESSION, FROM),
                    EXPRESSION, RPARENTHESIS)
        }

        private fun createConversionFunctions(b: PlSqlGrammarBuilder) {
            b.rule(DEFAULT_ON_ERROR_CLAUSE).define(DEFAULT, EXPRESSION, ON, CONVERSION, ERROR)

            b.rule(TREAT_AS_EXPRESSION).define(
                b.optional(TREAT),
                LPARENTHESIS,
                EXPRESSION,
                AS,
                b.optional(REF),
                OBJECT_REFERENCE,
                RPARENTHESIS
            )

            b.rule(SET_EXPRESSION).define(SET, LPARENTHESIS, EXPRESSION, RPARENTHESIS)

            b.rule(CAST_EXPRESSION).define(
                CAST, LPARENTHESIS,
                b.firstOf(b.sequence(MULTISET, EXPRESSION), EXPRESSION),
                AS, b.optional(DOMAIN), DATATYPE, b.optional(b.firstOf(VALIDATE, NOVALIDATE)),
                b.optional(DEFAULT_ON_ERROR_CLAUSE),
                b.optional(COMMA, EXPRESSION, b.optional(COMMA, EXPRESSION)),
                RPARENTHESIS
            )

            b.rule(TO_BINARY_DOUBLE_EXPRESSION).define(
                TO_BINARY_DOUBLE, LPARENTHESIS,
                EXPRESSION,
                b.optional(DEFAULT_ON_ERROR_CLAUSE),
                b.optional(COMMA, EXPRESSION, b.optional(COMMA, EXPRESSION)),
                RPARENTHESIS
            )

            b.rule(TABLE_EXPRESSION).define(
                TABLE, LPARENTHESIS,
                b.firstOf(DmlGrammar.SELECT_EXPRESSION, EXPRESSION),
                RPARENTHESIS
            )

            b.rule(THE_EXPRESSION).define(
                THE, LPARENTHESIS,
                b.firstOf(DmlGrammar.SELECT_EXPRESSION, EXPRESSION),
                RPARENTHESIS
            )

            b.rule(CURSOR_EXPRESSION).define(CURSOR, LPARENTHESIS, DmlGrammar.SELECT_EXPRESSION, RPARENTHESIS)
        }

        private fun createDateFunctions(b: PlSqlGrammarBuilder) {
            b.rule(EXTRACT_DATETIME_EXPRESSION).define(EXTRACT, LPARENTHESIS, IDENTIFIER_NAME, FROM, EXPRESSION, RPARENTHESIS)
        }

        private fun createXmlFunctions(b: PlSqlGrammarBuilder) {
            b.rule(XMLCDATA_EXPRESSION).define(XMLCDATA, LPARENTHESIS, EXPRESSION, RPARENTHESIS)

            b.rule(XMLCOMMENT_EXPRESSION).define(XMLCOMMENT, LPARENTHESIS, EXPRESSION, RPARENTHESIS)

            b.rule(XMLCONCAT_EXPRESSION).define(XMLCONCAT, LPARENTHESIS, EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION), RPARENTHESIS)

            b.rule(XMLDIFF_EXPRESSION).define(
                XMLDIFF,
                LPARENTHESIS,
                EXPRESSION, COMMA, EXPRESSION,
                b.optional(COMMA, EXPRESSION, COMMA, EXPRESSION),
                RPARENTHESIS)

            b.rule(XMLISVALID_EXPRESSION).define(
                XMLISVALID,
                LPARENTHESIS,
                EXPRESSION,
                b.optional(COMMA, EXPRESSION, b.optional(COMMA, EXPRESSION)),
                RPARENTHESIS)

            b.rule(XMLPATCH_EXPRESSION).define(
                XMLPATCH,
                LPARENTHESIS,
                EXPRESSION,
                COMMA,
                EXPRESSION,
                RPARENTHESIS
            )

            b.rule(XMLSEQUENCE_EXPRESSION).define(
                XMLSEQUENCE,
                LPARENTHESIS,
                EXPRESSION,
                b.zeroOrMore(COMMA, EXPRESSION),
                RPARENTHESIS
            )

            b.rule(XMLTRANSFORM_EXPRESSION).define(
                XMLTRANSFORM,
                LPARENTHESIS,
                EXPRESSION,
                COMMA,
                EXPRESSION,
                RPARENTHESIS
            )

            b.rule(XMLSERIALIZE_EXPRESSION).define(
                    XMLSERIALIZE, LPARENTHESIS,
                    b.firstOf(DOCUMENT, CONTENT), EXPRESSION,
                    b.optional(AS, DATATYPE),
                    b.optional(ENCODING, EXPRESSION),
                    b.optional(VERSION, STRING_LITERAL),
                    b.optional(b.firstOf(b.sequence(NO, INDENT), b.sequence(INDENT, b.optional(SIZE, EQUALS, EXPRESSION)))),
                    b.optional(b.firstOf(HIDE, SHOW), DEFAULTS),
                    RPARENTHESIS)

            b.rule(XML_COLUMN).define(
                    EXPRESSION, b.optional(b.optional(AS), b.firstOf(b.sequence(EVALNAME, EXPRESSION), IDENTIFIER_NAME)))

            b.rule(XMLATTRIBUTES_EXPRESSION).define(
                    XMLATTRIBUTES, LPARENTHESIS,
                    b.optional(b.firstOf(ENTITYESCAPING, NOENTITYESCAPING)),
                    b.optional(b.firstOf(SCHEMACHECK, NOSCHEMACHECK)),
                    XML_COLUMN, b.zeroOrMore(COMMA, XML_COLUMN),
                    RPARENTHESIS)

            b.rule(XMLELEMENT_EXPRESSION).define(
                    XMLELEMENT, LPARENTHESIS,
                    b.optional(b.firstOf(ENTITYESCAPING, NOENTITYESCAPING)),
                    b.firstOf(
                            b.sequence(EVALNAME, EXPRESSION),
                            b.sequence(b.optional(NAME), IDENTIFIER_NAME)
                    ),
                    b.optional(COMMA, XMLATTRIBUTES_EXPRESSION),
                    b.zeroOrMore(COMMA, EXPRESSION, b.optional(b.optional(AS), IDENTIFIER_NAME)),
                    RPARENTHESIS)

            b.rule(XMLFOREST_EXPRESSION).define(
                    XMLFOREST, LPARENTHESIS,
                    XML_COLUMN, b.zeroOrMore(COMMA, XML_COLUMN),
                    RPARENTHESIS)

            b.rule(XML_PASSING_CLAUSE).define(
                    PASSING, b.optional(BY, VALUE),
                    EXPRESSION, b.optional(AS, IDENTIFIER_NAME),
                    b.zeroOrMore(COMMA, IDENTIFIER_NAME, b.optional(AS, IDENTIFIER_NAME)))

            b.rule(XMLEXISTS_EXPRESSION).define(
                    XMLEXISTS, LPARENTHESIS, STRING_LITERAL,
                    b.optional(XML_PASSING_CLAUSE),
                    RPARENTHESIS)

            b.rule(XMLQUERY_EXPRESSION).define(
                    XMLQUERY, LPARENTHESIS, STRING_LITERAL,
                    b.optional(XML_PASSING_CLAUSE),
                    RETURNING, CONTENT,
                    b.optional(NULL, ON, EMPTY),
                    RPARENTHESIS)

            b.rule(XMLROOT_EXPRESSION).define(
                    XMLROOT, LPARENTHESIS,
                    EXPRESSION, COMMA,
                    VERSION, b.firstOf(b.sequence(NO, VALUE), EXPRESSION),
                    b.optional(COMMA, STANDALONE, b.firstOf(YES, b.sequence(NO, b.optional(VALUE)))),
                    RPARENTHESIS)

            b.rule(XMLCAST_EXPRESSION).define(
                    XMLCAST, LPARENTHESIS,
                    EXPRESSION, AS, DATATYPE,
                    RPARENTHESIS)

            b.rule(XMLCOLATTVAL_EXPRESSION).define(
                    XMLCOLATTVAL, LPARENTHESIS,
                    XML_COLUMN, b.zeroOrMore(COMMA, XML_COLUMN),
                    RPARENTHESIS)

            b.rule(XMLPARSE_EXPRESSION).define(
                    XMLPARSE, LPARENTHESIS,
                    b.firstOf(DOCUMENT, CONTENT), EXPRESSION, b.optional(WELLFORMED),
                    RPARENTHESIS)

            b.rule(XMLPI_EXPRESSION).define(
                    XMLPI, LPARENTHESIS,
                    b.firstOf(
                            b.sequence(EVALNAME, EXPRESSION),
                            b.sequence(b.optional(NAME), IDENTIFIER_NAME)),
                    b.optional(COMMA, EXPRESSION),
                    RPARENTHESIS)

            b.rule(XML_NAMESPACE).define(
                    b.firstOf(
                            b.sequence(DEFAULT, STRING_LITERAL),
                            b.sequence(STRING_LITERAL, AS, IDENTIFIER_NAME)))

            b.rule(XMLNAMESPACES_CLAUSE).define(
                    XMLNAMESPACES, LPARENTHESIS,
                    XML_NAMESPACE, b.zeroOrMore(COMMA, XML_NAMESPACE),
                    RPARENTHESIS)

            b.rule(XML_TABLE_COLUMN).define(
                    IDENTIFIER_NAME,
                    b.firstOf(
                            b.sequence(FOR, ORDINALITY),
                            b.sequence(
                                    b.firstOf(
                                            DATATYPE,
                                            b.sequence(XMLTYPE, b.optional(LPARENTHESIS, SEQUENCE, RPARENTHESIS, BY, REF))),
                                    b.optional(PATH, STRING_LITERAL),
                                    b.optional(DEFAULT, STRING_LITERAL))))

            b.rule(XMLTABLE_OPTIONS).define(
                    b.optional(XML_PASSING_CLAUSE),
                    b.optional(RETURNING, SEQUENCE, BY, REF),
                    b.optional(COLUMNS, XML_TABLE_COLUMN, b.zeroOrMore(COMMA, XML_TABLE_COLUMN))).skip()

            b.rule(XMLTABLE_EXPRESSION).define(
                    XMLTABLE, LPARENTHESIS,
                    b.optional(XMLNAMESPACES_CLAUSE, COMMA), STRING_LITERAL, XMLTABLE_OPTIONS,
                    RPARENTHESIS)
        }

        private fun createJsonFunctions(b: PlSqlGrammarBuilder) {
            b.rule(JSON_CONSTRUCTOR).define(JSON, LPARENTHESIS, EXPRESSION, RPARENTHESIS)

            b.rule(JSON_ARRAY_EXPRESSION).define(
                JSON_ARRAY,
                LPARENTHESIS,
                b.firstOf(JSON_ARRAY_ENUMERATION_CONTENT, JSON_ARRAY_QUERY_CONTENT),
                RPARENTHESIS
            )

            b.rule(JSON_ARRAY_ENUMERATION_CONTENT).define(
                JSON_ARRAY_ELEMENT,
                b.zeroOrMore(COMMA, JSON_ARRAY_ELEMENT),
                b.optional(JSON_ON_NULL_CLAUSE),
                b.optional(JSON_RETURNING_CLAUSE),
                b.optional(STRICT)
            )

            b.rule(JSON_ARRAY_ELEMENT).define(
                EXPRESSION,
                b.optional(FORMAT, JSON)
            )

            b.rule(JSON_ON_NULL_CLAUSE).define(b.firstOf(NULL, ABSENT), ON, NULL)

            b.rule(JSON_ON_ERROR_CLAUSE).define(b.firstOf(NULL, ERROR), ON, ERROR)

            b.rule(JSON_RETURNING_CLAUSE).define(
                RETURNING,
                b.firstOf(
                    b.sequence(
                        VARCHAR2,
                        b.optional(LPARENTHESIS, PlSqlTokenType.INTEGER_LITERAL, b.optional(b.firstOf(CHAR, BYTE)), RPARENTHESIS),
                        b.optional(WITH, TYPENAME)
                    ),
                    b.sequence(
                        b.firstOf(CLOB, BLOB),
                        b.optional(b.firstOf(REFERENCE, VALUE))
                    ),
                    JSON
                )
            )

            b.rule(JSON_DATAGUIDE_EXPRESSION).define(
                JSON_DATAGUIDE,
                LPARENTHESIS,
                EXPRESSION,
                b.optional(COMMA, EXPRESSION, b.optional(COMMA, EXPRESSION)),
                RPARENTHESIS
            )

            b.rule(JSON_MERGEPATCH_EXPRESSION).define(
                JSON_MERGEPATCH,
                LPARENTHESIS,
                EXPRESSION,
                COMMA,
                EXPRESSION,
                b.optional(JSON_RETURNING_CLAUSE),
                b.optional(PRETTY),
                b.optional(ASCII),
                b.optional(TRUNCATE),
                b.optional(JSON_ON_ERROR_CLAUSE),
                RPARENTHESIS
            )

            b.rule(JSON_ARRAY_QUERY_CONTENT).define(
                DmlGrammar.SELECT_EXPRESSION,
                b.optional(JSON_ON_NULL_CLAUSE),
                b.optional(JSON_RETURNING_CLAUSE),
                b.optional(STRICT)
            )

            b.rule(JSON_OBJECT_EXPRESSION).define(
                JSON_OBJECT,
                LPARENTHESIS,
                JSON_OBJECT_ENTRY, b.zeroOrMore(COMMA, JSON_OBJECT_ENTRY),
                b.optional(JSON_ON_NULL_CLAUSE),
                b.optional(JSON_RETURNING_CLAUSE),
                b.optional(STRICT),
                b.optional(WITH, UNIQUE, KEYS),
                RPARENTHESIS
            )

            b.rule(JSON_OBJECT_ENTRY).define(
                b.firstOf(
                    b.sequence(b.optional(KEY), EXPRESSION, VALUE, EXPRESSION),
                    b.sequence(STRING_LITERAL, COLON, EXPRESSION),
                    EXPRESSION
                ),
                b.optional(FORMAT, JSON)
            )

            b.rule(JSON_SCALAR_EXPRESSION).define(
                JSON_SCALAR,
                LPARENTHESIS,
                EXPRESSION,
                b.optional(b.firstOf(
                    b.sequence(b.firstOf(JSON, SQL), NULL),
                    b.sequence(EMPTY, STRING)
                ), ON, NULL),
                b.optional(b.firstOf(NULL, ERROR), ON, ERROR),
                RPARENTHESIS
            )

            b.rule(JSON_SERIALIZE_EXPRESSION).define(
                JSON_SERIALIZE,
                LPARENTHESIS,
                EXPRESSION,
                b.optional(JSON_RETURNING_CLAUSE),
                b.optional(PRETTY),
                b.optional(ASCII),
                b.optional(ORDERED),
                b.optional(TRUNCATE),
                b.optional(
                    b.firstOf(
                        NULL,
                        ERROR,
                        b.sequence(EMPTY, b.optional(b.firstOf(ARRAY, OBJECT)))
                    ),
                    ON, ERROR
                ),
                RPARENTHESIS
            )

            b.rule(JSON_QUERY_EXPRESSION).define(
                JSON_QUERY,
                LPARENTHESIS,
                EXPRESSION,
                b.optional(FORMAT, JSON),
                COMMA,
                STRING_LITERAL,
                b.optional(JSON_PASSING_CLAUSE),
                b.optional(JSON_QUERY_RETURNING_CLAUSE),
                b.optional(JSON_QUERY_WRAPPER_CLAUSE),
                b.optional(JSON_QUERY_QUOTES_CLAUSE),
                b.optional(JSON_QUERY_ON_ERROR_CLAUSE),
                b.optional(JSON_QUERY_ON_EMPTY_CLAUSE),
                b.optional(JSON_QUERY_ON_MISMATCH_CLAUSE),
                b.optional(TYPE, b.firstOf(STRICT, LAX)),
                RPARENTHESIS
            )

            b.rule(JSON_PASSING_CLAUSE).define(
                PASSING, EXPRESSION, AS, IDENTIFIER_NAME, b.zeroOrMore(COMMA, EXPRESSION, AS, IDENTIFIER_NAME)
            )

            b.rule(JSON_QUERY_RETURNING_CLAUSE).define(
                b.optional(RETURNING, DATATYPE),
                b.optional(b.firstOf(ALLOW, DISALLOW), SCALARS),
                b.optional(PRETTY),
                b.optional(ASCII)
            ).skip()

            b.rule(JSON_QUERY_WRAPPER_CLAUSE).define(
                b.firstOf(
                    WITHOUT,
                    b.sequence(WITH, b.optional(b.firstOf(UNCONDITIONAL, CONDITIONAL)))
                ),
                b.optional(ARRAY), WRAPPER
            )

            b.rule(JSON_QUERY_QUOTES_CLAUSE).define(
                b.firstOf(KEEP, OMIT),
                QUOTES,
                b.optional(ON, SCALAR, STRING)
            )

            b.rule(JSON_QUERY_ON_ERROR_CLAUSE).define(
                b.firstOf(
                    ERROR,
                    NULL,
                    b.sequence(EMPTY, b.optional(b.firstOf(ARRAY, OBJECT)))
                ),
                ON, ERROR
            )

            b.rule(JSON_QUERY_ON_EMPTY_CLAUSE).define(
                b.firstOf(
                    ERROR,
                    NULL,
                    b.sequence(EMPTY, b.optional(b.firstOf(ARRAY, OBJECT)))
                ),
                ON, EMPTY
            )

            b.rule(JSON_QUERY_ON_MISMATCH_CLAUSE).define(
                b.firstOf(ERROR, NULL), ON, MISMATCH
            )

            b.rule(JSON_TABLE_EXPRESSION).define(
                JSON_TABLE,
                LPARENTHESIS,
                EXPRESSION,
                b.optional(FORMAT, JSON),
                b.optional(COMMA, JSON_BASIC_PATH_EXPRESSION),
                b.optional(JSON_TABLE_ON_ERROR_CLAUSE),
                b.optional(TYPE, b.firstOf(STRICT, LAX)),
                b.optional(JSON_TABLE_ON_EMPTY_CLAUSE),
                JSON_TABLE_COLUMNS_CLAUSE,
                RPARENTHESIS
            )

            b.rule(JSON_TABLE_ON_ERROR_CLAUSE).define(
                b.firstOf(ERROR, NULL),
                ON, ERROR
            )

            b.rule(JSON_TABLE_ON_EMPTY_CLAUSE).define(
                b.firstOf(ERROR, NULL),
                ON, EMPTY
            )

            b.rule(JSON_TABLE_COLUMNS_CLAUSE).define(
                COLUMNS,
                b.firstOf(
                    b.sequence(
                        LPARENTHESIS,
                        JSON_COLUMN_DEFINITION,
                        b.zeroOrMore(COMMA, JSON_COLUMN_DEFINITION),
                        RPARENTHESIS
                    ),
                    b.sequence(
                        JSON_COLUMN_DEFINITION,
                        b.zeroOrMore(COMMA, JSON_COLUMN_DEFINITION),
                    ),
                )
            )

            b.rule(JSON_COLUMN_DEFINITION).define(
                b.firstOf(
                    JSON_NESTED_PATH,
                    JSON_EXISTS_COLUMN,
                    JSON_ORDINALITY_COLUMN,
                    JSON_VALUE_COLUMN,
                    JSON_QUERY_COLUMN
                )
            )

            b.rule(JSON_EXISTS_COLUMN).define(
                IDENTIFIER_NAME,
                b.optional(JSON_VALUE_RETURN_TYPE),
                EXISTS,
                b.optional(PATH, JSON_PATH),
                b.optional(ConditionsGrammar.JSON_EXISTS_ON_ERROR_CLAUSE),
                b.optional(ConditionsGrammar.JSON_EXISTS_ON_EMPTY_CLAUSE)
            )

            b.rule(JSON_QUERY_COLUMN).define(
                IDENTIFIER_NAME,
                b.optional(JSON_VALUE_RETURN_TYPE),
                b.optional(FORMAT, JSON),
                b.optional(b.firstOf(ALLOW, DISALLOW), SCALARS),
                b.optional(JSON_QUERY_WRAPPER_CLAUSE),
                b.optional(PATH, JSON_PATH),
                b.optional(JSON_QUERY_ON_ERROR_CLAUSE)
            )

            b.rule(JSON_VALUE_COLUMN).define(
                IDENTIFIER_NAME,
                b.optional(JSON_VALUE_RETURN_TYPE),
                b.nextNot(FORMAT),
                b.optional(TRUNCATE),
                b.optional(PATH, JSON_PATH),
                b.optional(JSON_VALUE_ON_ERROR_CLAUSE),
                b.optional(JSON_VALUE_ON_EMPTY_CLAUSE),
                b.optional(JSON_VALUE_ON_MISMATCH_CLAUSE)
            )

            b.rule(JSON_NESTED_PATH).define(
                NESTED, b.optional(PATH),
                JSON_PATH,
                JSON_COLUMNS_CLAUSE
            )

            b.rule(JSON_COLUMNS_CLAUSE).define(
                COLUMNS,
                LPARENTHESIS,
                JSON_COLUMN_DEFINITION,
                b.zeroOrMore(COMMA, JSON_COLUMN_DEFINITION),
                RPARENTHESIS
            )

            b.rule(JSON_ORDINALITY_COLUMN).define(IDENTIFIER_NAME, FOR, ORDINALITY)

            b.rule(JSON_PATH).define(
                b.firstOf(
                    JSON_BASIC_PATH_EXPRESSION,
                    JSON_RELATIVE_OBJECT_ACCESS
                )
            )

            b.rule(JSON_BASIC_PATH_EXPRESSION).define(STRING_LITERAL).skip()

            b.rule(JSON_PATH_EXPRESSION).define(STRING_LITERAL).skip()

            b.rule(JSON_RELATIVE_OBJECT_ACCESS).define(
                IDENTIFIER_NAME, b.optional(JSON_ARRAY_STEP),
                b.zeroOrMore(DOT, IDENTIFIER_NAME, b.optional(JSON_ARRAY_STEP))
            )

            b.rule(JSON_ARRAY_STEP).define(
                LBRACKET,
                b.firstOf(
                    b.oneOrMore(PlSqlTokenType.INTEGER_LITERAL, b.optional(TO, PlSqlTokenType.INTEGER_LITERAL), b.optional(COMMA)),
                    MULTIPLICATION
                ),
                RBRACKET
            )

            b.rule(JSON_VALUE_RETURN_TYPE).define(
                b.firstOf(
                    b.sequence(
                        VARCHAR2,
                        b.optional(
                            LPARENTHESIS,
                            PlSqlTokenType.INTEGER_LITERAL,
                            b.optional(b.firstOf(CHAR, BYTE)),
                            RPARENTHESIS
                        ),
                        b.optional(TRUNCATE)
                    ),
                    CLOB,
                    b.sequence(
                        NUMBER,
                        b.optional(
                            LPARENTHESIS,
                            PlSqlTokenType.INTEGER_LITERAL,
                            b.optional(COMMA, PlSqlTokenType.INTEGER_LITERAL),
                            RPARENTHESIS
                        )
                    ),
                    b.sequence(b.firstOf(ALLOW, DISALLOW), b.optional(BOOLEAN), TO, NUMBER, b.optional(CONVERSION)),
                    b.sequence(DATE, b.optional(b.firstOf(TRUNCATE, PRESERVE), TIME)),
                    b.sequence(TIMESTAMP, b.optional(WITH, TIME, ZONE)),
                    BOOLEAN,
                    SDO_GEOMETRY,
                    JSON_VALUE_RETURN_OBJECT_INSTANCE,
                    VECTOR
                )
            )

            b.rule(JSON_VALUE_RETURN_OBJECT_INSTANCE).define(
                b.nextNot(NON_RESERVED_KEYWORD),
                CUSTOM_DATATYPE, b.optional(USING, CASE_SENSITIVE, MAPPING))

            b.rule(JSON_VALUE_ON_ERROR_CLAUSE).define(
                b.firstOf(
                    ERROR,
                    NULL,
                    b.sequence(DEFAULT, LITERAL)
                ), ON, ERROR
            )

            b.rule(JSON_VALUE_ON_EMPTY_CLAUSE).define(
                b.firstOf(
                    ERROR,
                    NULL,
                    b.sequence(DEFAULT, LITERAL)
                ), ON, EMPTY
            )

            b.rule(JSON_VALUE_ON_MISMATCH_CLAUSE).define(
                b.firstOf(IGNORE, ERROR, NULL),
                ON, MISMATCH,
                b.optional(
                    LPARENTHESIS,
                    b.oneOrMore(
                        b.firstOf(
                            b.sequence(MISSING, DATA),
                            b.sequence(EXTRA, DATA),
                            b.sequence(TYPE, ERROR)
                        ), b.optional(COMMA)
                    ),
                    RPARENTHESIS
                )
            )

            b.rule(JSON_VALUE_EXPRESSION).define(
                JSON_VALUE,
                LPARENTHESIS,
                EXPRESSION,
                b.optional(FORMAT, JSON),
                COMMA,
                STRING_LITERAL,
                b.optional(JSON_PASSING_CLAUSE),
                b.optional(JSON_VALUE_RETURNING_CLAUSE),
                b.optional(JSON_VALUE_ON_ERROR_CLAUSE),
                b.optional(JSON_VALUE_ON_EMPTY_CLAUSE),
                b.optional(JSON_VALUE_ON_MISMATCH_CLAUSE),
                b.optional(TYPE, b.firstOf(STRICT, LAX)),
                RPARENTHESIS
            )

            b.rule(JSON_VALUE_RETURNING_CLAUSE).define(RETURNING, JSON_VALUE_RETURN_TYPE, b.optional(ASCII))

            b.rule(JSON_TRANSFORM_EXPRESSION).define(
                JSON_TRANSFORM,
                LPARENTHESIS,
                EXPRESSION,
                COMMA,
                JSON_TRANSFORM_OPERATION,
                b.zeroOrMore(COMMA, JSON_TRANSFORM_OPERATION),
                b.optional(JSON_TRANSFORM_RETURNING_CLAUSE),
                b.optional(TYPE, b.firstOf(STRICT, LAX)),
                b.optional(JSON_PASSING_CLAUSE),
                RPARENTHESIS
            )

            b.rule(JSON_TRANSFORM_RETURNING_CLAUSE).define(
                RETURNING,
                b.firstOf(
                    b.sequence(
                        VARCHAR2,
                        b.optional(
                            LPARENTHESIS,
                            PlSqlTokenType.INTEGER_LITERAL,
                            b.optional(b.firstOf(CHAR, BYTE)),
                            RPARENTHESIS
                        )
                    ),
                    b.sequence(
                        b.firstOf(CLOB, BLOB),
                        b.optional(b.firstOf(REFERENCE, VALUE))
                    ),
                    JSON,
                    BOOLEAN,
                    VECTOR
                )
            )

            b.rule(JSON_RHS_EXPRESSION).define(
                b.firstOf(
                    b.sequence(PATH, JSON_BASIC_PATH_EXPRESSION),
                    b.sequence(EXPRESSION, b.optional(FORMAT, JSON))
                )
            )

            b.rule(JSON_TRANSFORM_OPERATION).define(
                b.firstOf(
                    JSON_APPEND_OPERATION,
                    JSON_CASE_OPERATION,
                    JSON_COPY_OPERATION,
                    JSON_INSERT_OPERATION,
                    JSON_INTERSECT_OPERATION,
                    JSON_KEEP_OPERATION,
                    JSON_MERGE_OPERATION,
                    JSON_MINUS_OPERATION,
                    JSON_NESTED_PATH_OPERATION,
                    JSON_PREPEND_OPERATION,
                    JSON_REMOVE_OPERATION,
                    JSON_RENAME_OPERATION,
                    JSON_REPLACE_OPERATION,
                    JSON_SET_OPERATION,
                    JSON_SORT_OPERATION,
                    JSON_UNION_OPERATION
                )
            )

            b.rule(JSON_APPEND_OPERATION).define(
                APPEND, JSON_PATH_EXPRESSION, EQUALS, JSON_RHS_EXPRESSION,
                b.optional(b.firstOf(IGNORE, ERROR, CREATE, NULL), ON, MISSING),
                b.optional(b.firstOf(IGNORE, ERROR, REPLACE, NULL), ON, MISMATCH),
                b.optional(b.firstOf(IGNORE, ERROR, NULL), ON, NULL),
                b.optional(b.firstOf(IGNORE, ERROR), ON, EMPTY),
            )

            b.rule(JSON_CASE_OPERATION).define(
                CASE,
                WHEN, JSON_PATH_EXPRESSION, THEN,
                LPARENTHESIS,
                JSON_TRANSFORM_OPERATION, b.zeroOrMore(COMMA, JSON_TRANSFORM_OPERATION),
                RPARENTHESIS,
                b.optional(
                    ELSE,
                    LPARENTHESIS,
                    JSON_TRANSFORM_OPERATION, b.zeroOrMore(COMMA, JSON_TRANSFORM_OPERATION),
                    RPARENTHESIS
                ),
                END
            )

            b.rule(JSON_COPY_OPERATION).define(
                COPY, JSON_PATH_EXPRESSION, EQUALS, JSON_RHS_EXPRESSION,
                b.optional(b.firstOf(IGNORE, ERROR, CREATE, NULL), ON, MISSING),
                b.optional(b.firstOf(IGNORE, ERROR, NULL), ON, NULL),
                b.optional(b.firstOf(IGNORE, ERROR), ON, EMPTY)
            )

            b.rule(JSON_INSERT_OPERATION).define(
                INSERT, JSON_PATH_EXPRESSION, EQUALS, JSON_RHS_EXPRESSION,
                b.optional(b.firstOf(IGNORE, ERROR, REPLACE, NULL), ON, EXISTING),
                b.optional(b.firstOf(IGNORE, ERROR, REMOVE, NULL), ON, NULL),
                b.optional(b.firstOf(IGNORE, ERROR, NULL), ON, EMPTY),
                b.optional(b.firstOf(IGNORE, ERROR), ON, ERROR)
            )

            b.rule(JSON_INTERSECT_OPERATION).define(
                INTERSECT, JSON_PATH_EXPRESSION, EQUALS, JSON_RHS_EXPRESSION,
                b.optional(b.firstOf(IGNORE, ERROR, CREATE, NULL), ON, MISSING),
                b.optional(b.firstOf(IGNORE, ERROR, NULL), ON, NULL)
            )

            b.rule(JSON_KEEP_OPERATION).define(
                KEEP, JSON_PATH_EXPRESSION, b.zeroOrMore(COMMA, JSON_PATH_EXPRESSION),
                b.optional(b.firstOf(IGNORE, ERROR), ON, MISSING)
            )

            b.rule(JSON_MERGE_OPERATION).define(
                MERGE, JSON_PATH_EXPRESSION, EQUALS, JSON_RHS_EXPRESSION,
                b.optional(b.firstOf(IGNORE, ERROR, CREATE, NULL), ON, MISSING),
                b.optional(b.firstOf(IGNORE, ERROR), ON, MISMATCH),
                b.optional(b.firstOf(IGNORE, ERROR, NULL), ON, NULL),
                b.optional(b.firstOf(IGNORE, ERROR), ON, EMPTY)
            )

            b.rule(JSON_MINUS_OPERATION).define(
                MINUS_KEYWORD, JSON_PATH_EXPRESSION, EQUALS, JSON_RHS_EXPRESSION,
                b.optional(b.firstOf(IGNORE, ERROR, CREATE, NULL), ON, MISSING),
                b.optional(b.firstOf(IGNORE, ERROR, NULL), ON, NULL)
            )

            b.rule(JSON_NESTED_PATH_OPERATION).define(
                NESTED, PATH, JSON_PATH_EXPRESSION, LPARENTHESIS,
                JSON_TRANSFORM_OPERATION, b.zeroOrMore(COMMA, JSON_TRANSFORM_OPERATION),
                RPARENTHESIS
            )

            b.rule(JSON_PREPEND_OPERATION).define(
                PREPEND, JSON_PATH_EXPRESSION, EQUALS, JSON_RHS_EXPRESSION,
                b.optional(b.firstOf(IGNORE, ERROR, CREATE, NULL), ON, MISSING),
                b.optional(b.firstOf(IGNORE, ERROR, REPLACE, CREATE), ON, MISMATCH),
                b.optional(b.firstOf(IGNORE, ERROR, NULL), ON, NULL),
                b.optional(b.firstOf(IGNORE, ERROR), ON, EMPTY)
            )

            b.rule(JSON_REMOVE_OPERATION).define(
                REMOVE, JSON_PATH_EXPRESSION,
                b.optional(b.firstOf(IGNORE, ERROR), ON, MISSING)
            )

            b.rule(JSON_RENAME_OPERATION).define(
                RENAME, JSON_PATH_EXPRESSION, WITH, STRING_LITERAL,
                b.optional(b.firstOf(IGNORE, ERROR), ON, MISSING)
            )

            b.rule(JSON_REPLACE_OPERATION).define(
                REPLACE, JSON_PATH_EXPRESSION, EQUALS, JSON_RHS_EXPRESSION,
                b.optional(b.firstOf(IGNORE, ERROR, CREATE, NULL), ON, MISSING),
                b.optional(b.firstOf(IGNORE, ERROR, REMOVE, NULL), ON, NULL),
                b.optional(b.firstOf(IGNORE, ERROR, NULL), ON, EMPTY),
                b.optional(b.firstOf(IGNORE, ERROR), ON, ERROR)
            )

            b.rule(JSON_SET_OPERATION).define(
                SET, JSON_PATH_EXPRESSION, EQUALS, JSON_RHS_EXPRESSION,
                b.optional(b.firstOf(IGNORE, ERROR, REPLACE), ON, EXISTING),
                b.optional(b.firstOf(IGNORE, ERROR, CREATE), ON, MISSING),
                b.optional(b.firstOf(IGNORE, ERROR, REMOVE, NULL), ON, NULL),
                b.optional(b.firstOf(IGNORE, ERROR, NULL), ON, EMPTY),
                b.optional(b.firstOf(IGNORE, ERROR), ON, ERROR)
            )

            b.rule(JSON_SORT_OPERATION).define(
                SORT, JSON_PATH_EXPRESSION,
                b.optional(
                    b.firstOf(
                        REVERSE,
                        b.sequence(
                            b.optional(REMOVE, NULLS),
                            ORDER, BY,
                            JSON_PATH_EXPRESSION, b.optional(b.firstOf(ASC, DESC)),
                            b.zeroOrMore(
                                COMMA,
                                JSON_PATH_EXPRESSION, b.optional(b.firstOf(ASC, DESC))
                            )
                        ),
                        b.sequence(
                            b.optional(b.firstOf(ASC, DESC)),
                            b.optional(UNIQUE),
                            b.optional(REMOVE, NULLS)
                        )
                    )
                ),
                b.optional(b.firstOf(IGNORE, ERROR, NULL), ON, MISSING),
                b.optional(b.firstOf(IGNORE, ERROR, NULL), ON, MISMATCH),
                b.optional(b.firstOf(IGNORE, ERROR), ON, EMPTY),
                b.optional(b.firstOf(IGNORE, ERROR), ON, ERROR)
            )

            b.rule(JSON_UNION_OPERATION).define(
                UNION, JSON_PATH_EXPRESSION, EQUALS, JSON_RHS_EXPRESSION,
                b.optional(b.firstOf(IGNORE, ERROR, CREATE, NULL), ON, MISSING),
                b.optional(b.firstOf(IGNORE, ERROR, NULL), ON, NULL)
            )
        }
    }

}
