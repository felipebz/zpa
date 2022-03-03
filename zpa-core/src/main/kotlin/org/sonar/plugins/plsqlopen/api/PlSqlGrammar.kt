/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2022 Felipe Zorzo
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

import com.felipebz.flr.api.GenericTokenType.EOF
import com.felipebz.flr.api.GenericTokenType.IDENTIFIER
import com.felipebz.flr.grammar.GrammarRuleKey
import com.felipebz.flr.grammar.LexerfulGrammarBuilder
import org.sonar.plsqlopen.squid.PlSqlConfiguration
import org.sonar.plsqlopen.sslr.*
import org.sonar.plugins.plsqlopen.api.DclGrammar.DCL_COMMAND
import org.sonar.plugins.plsqlopen.api.DdlGrammar.DDL_COMMAND
import org.sonar.plugins.plsqlopen.api.DmlGrammar.*
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword.*
import org.sonar.plugins.plsqlopen.api.PlSqlPunctuator.*
import org.sonar.plugins.plsqlopen.api.PlSqlTokenType.*
import org.sonar.plugins.plsqlopen.api.SessionControlGrammar.SESSION_CONTROL_COMMAND
import org.sonar.plugins.plsqlopen.api.SqlPlusGrammar.SQLPLUS_COMMAND
import org.sonar.plugins.plsqlopen.api.TclGrammar.*

enum class PlSqlGrammar : GrammarRuleKey {

    // Data types
    DATATYPE,
    DATATYPE_LENGTH,
    CHARACTER_SET_CLAUSE,
    NUMERIC_DATATYPE_CONSTRAINT,
    NUMERIC_DATATYPE,
    LOB_DATATYPE,
    CHARACTER_DATATYPE_CONSTRAINT,
    CHARACTER_DATAYPE,
    BOOLEAN_DATATYPE,
    DATE_DATATYPE,
    CUSTOM_SUBTYPE,
    ANCHORED_DATATYPE,
    CUSTOM_DATATYPE,
    REF_DATATYPE,

    // Literals
    LITERAL,
    BOOLEAN_LITERAL,
    NULL_LITERAL,
    NUMERIC_LITERAL,
    CHARACTER_LITERAL,
    INTERVAL_YEAR_TO_MONTH_LITERAL,
    INTERVAL_DAY_TO_SECOND_LITERAL,
    INTERVAL_LITERAL,
    INQUIRY_DIRECTIVE,

    // Expressions
    EXPRESSION,
    AND_EXPRESSION,
    OR_EXPRESSION,
    NOT_EXPRESSION,
    BOOLEAN_EXPRESSION,
    PRIMARY_EXPRESSION,
    BRACKED_EXPRESSION,
    MULTIPLE_VALUE_EXPRESSION,
    MEMBER_EXPRESSION,
    OBJECT_REFERENCE,
    POSTFIX_EXPRESSION,
    IN_EXPRESSION,
    EXISTS_EXPRESSION,
    UNARY_EXPRESSION,
    MULTIPLICATIVE_EXPRESSION,
    ADDITIVE_EXPRESSION,
    CONCATENATION_EXPRESSION,
    COMPARISON_EXPRESSION,
    EXPONENTIATION_EXPRESSION,
    ARGUMENT,
    ARGUMENTS,
    TREAT_AS_EXPRESSION,
    SET_EXPRESSION,
    METHOD_CALL,
    CALL_EXPRESSION,
    CASE_EXPRESSION,
    AT_TIME_ZONE_EXPRESSION,
    VARIABLE_NAME,
    NEW_OBJECT_EXPRESSION,
    MULTISET_EXPRESSION,

    // Statements
    LABEL,
    STATEMENTS_SECTION,
    BLOCK_STATEMENT,
    NULL_STATEMENT,
    ASSIGNMENT_STATEMENT,
    ELSIF_CLAUSE,
    ELSE_CLAUSE,
    IF_STATEMENT,
    LOOP_STATEMENT,
    EXIT_STATEMENT,
    CONTINUE_STATEMENT,
    GOTO_STATEMENT,
    FOR_STATEMENT,
    WHILE_STATEMENT,
    RETURN_STATEMENT,
    COMMIT_STATEMENT,
    ROLLBACK_STATEMENT,
    SAVEPOINT_STATEMENT,
    RAISE_STATEMENT,
    SELECT_STATEMENT,
    INSERT_STATEMENT,
    UPDATE_STATEMENT,
    DELETE_STATEMENT,
    CALL_STATEMENT,
    UNNAMED_ACTUAL_PAMETER,
    EXECUTE_IMMEDIATE_STATEMENT,
    OPEN_STATEMENT,
    OPEN_FOR_STATEMENT,
    FETCH_STATEMENT,
    CLOSE_STATEMENT,
    PIPE_ROW_STATEMENT,
    CASE_STATEMENT,
    STATEMENT,
    STATEMENTS,
    FORALL_STATEMENT,
    SET_TRANSACTION_STATEMENT,
    MERGE_STATEMENT,
    INLINE_PRAGMA_STATEMENT,

    // Declarations
    DEFAULT_VALUE_ASSIGNMENT,
    VARIABLE_DECLARATION,
    PARAMETER_DECLARATION,
    CURSOR_PARAMETER_DECLARATION,
    CURSOR_DECLARATION,
    RECORD_FIELD_DECLARATION,
    RECORD_DECLARATION,
    NESTED_TABLE_DEFINITION,
    TABLE_OF_DECLARATION,
    VARRAY_TYPE_DEFINITION,
    VARRAY_DECLARATION,
    REF_CURSOR_DECLARATION,
    AUTONOMOUS_TRANSACTION_PRAGMA,
    EXCEPTION_INIT_PRAGMA,
    SERIALLY_REUSABLE_PRAGMA,
    INTERFACE_PRAGMA,
    RESTRICT_REFERENCES_PRAGMA,
    UDF_PRAGMA,
    DEPRECATE_PRAGMA,
    PRAGMA_DECLARATION,
    HOST_AND_INDICATOR_VARIABLE,
    JAVA_DECLARATION,
    C_DECLARATION,
    EXTERNAL_PARAMETER_PROPERTY,
    EXTERNAL_PARAMETER,
    CALL_SPECIFICATION,

    DECLARE_SECTION,
    EXCEPTION_HANDLER,
    IDENTIFIER_NAME,
    NON_RESERVED_KEYWORD,
    EXECUTE_PLSQL_BUFFER,

    // Triggers
    SIMPLE_DML_TRIGGER,
    INSTEAD_OF_DML_TRIGGER,
    COMPOUND_DML_TRIGGER,
    SYSTEM_TRIGGER,
    DML_EVENT_CLAUSE,
    REFERENCING_CLAUSE,
    TRIGGER_EDITION_CLAUSE,
    TRIGGER_ORDERING_CLAUSE,
    COMPOUND_TRIGGER_BLOCK,
    TIMING_POINT_SECTION,
    TIMING_POINT,
    TPS_BODY,
    DDL_EVENT,
    DATABASE_EVENT,

    // Program units
    COMPILATION_UNIT,
    UNIT_NAME,
    ANONYMOUS_BLOCK,
    PROCEDURE_DECLARATION,
    FUNCTION_DECLARATION,
    CREATE_PROCEDURE,
    CREATE_FUNCTION,
    CREATE_PACKAGE,
    CREATE_PACKAGE_BODY,
    VIEW_RESTRICTION_CLAUSE,
    CREATE_VIEW,
    TYPE_ATTRIBUTE,
    INHERITANCE_CLAUSE,
    TYPE_SUBPROGRAM,
    TYPE_CONSTRUCTOR,
    MAP_ORDER_FUNCTION,
    TYPE_ELEMENT_SPEC,
    OBJECT_TYPE_DEFINITION,
    CREATE_TRIGGER,
    CREATE_TYPE,
    CREATE_TYPE_BODY,

    // Top-level components
    VALID_INPUT,
    RECOVERY,
    FILE_INPUT;

    companion object {
        fun create(conf: PlSqlConfiguration): PlSqlGrammarBuilder {
            val b = PlSqlGrammarBuilder(LexerfulGrammarBuilder.create())

            val keywords = PlSqlKeyword.nonReservedKeywords
            val rest = keywords.subList(2, keywords.size).toTypedArray()
            b.rule(NON_RESERVED_KEYWORD).define(b.firstOf(keywords[0], keywords[1], *rest))

            b.rule(IDENTIFIER_NAME).define(b.firstOf(IDENTIFIER, NON_RESERVED_KEYWORD))

            b.rule(VALID_INPUT).define(b.firstOf(
                    COMPILATION_UNIT,
                    DCL_COMMAND,
                    DDL_COMMAND,
                    DML_COMMAND,
                    TCL_COMMAND,
                    SQLPLUS_COMMAND,
                    SESSION_CONTROL_COMMAND,
                    EXECUTE_PLSQL_BUFFER)).skip()

            if (conf.isErrorRecoveryEnabled) {
                b.rule(RECOVERY).define(b.oneOrMore(
                        b.nextNot(b.firstOf(VALID_INPUT, EOF)),
                        b.anyToken()))

                b.rule(FILE_INPUT).define(b.zeroOrMore(b.firstOf(
                        VALID_INPUT,
                        RECOVERY)), EOF)
            } else {
                b.rule(RECOVERY).define(b.nothing())
                b.rule(FILE_INPUT).define(b.zeroOrMore(VALID_INPUT), EOF)
            }

            createLiterals(b)
            createDatatypes(b)
            createStatements(b)
            createExpressions(b)
            createDeclarations(b)
            createTrigger(b)
            createProgramUnits(b)
            DdlGrammar.buildOn(b)
            DmlGrammar.buildOn(b)
            DclGrammar.buildOn(b)
            TclGrammar.buildOn(b)
            SqlPlusGrammar.buildOn(b)
            SessionControlGrammar.buildOn(b)
            SingleRowSqlFunctionsGrammar.buildOn(b)
            AggregateSqlFunctionsGrammar.buildOn(b)
            ConditionsGrammar.buildOn(b)

            b.setRootRule(FILE_INPUT)
            b.buildWithMemoizationOfMatchesForAllRules()

            return b
        }

        private fun createLiterals(b: PlSqlGrammarBuilder) {
            b.rule(INTERVAL_YEAR_TO_MONTH_LITERAL).define(
                    INTERVAL, CHARACTER_LITERAL,
                    b.firstOf(YEAR, MONTH), b.optional(LPARENTHESIS, INTEGER_LITERAL, RPARENTHESIS),
                    b.optional(TO, b.firstOf(YEAR, MONTH)))

            b.rule(INTERVAL_DAY_TO_SECOND_LITERAL).define(
                    INTERVAL, CHARACTER_LITERAL,
                    b.firstOf(
                            b.sequence(b.firstOf(DAY, HOUR, MINUTE), b.optional(LPARENTHESIS, INTEGER_LITERAL, RPARENTHESIS)),
                            b.sequence(SECOND, b.optional(LPARENTHESIS, INTEGER_LITERAL, b.optional(COMMA, INTEGER_LITERAL), RPARENTHESIS))
                    ),
                    b.optional(TO,
                            b.firstOf(
                                    DAY,
                                    HOUR,
                                    MINUTE,
                                    b.sequence(SECOND, b.optional(LPARENTHESIS, INTEGER_LITERAL, RPARENTHESIS))
                            )))

            b.rule(NULL_LITERAL).define(NULL)
            b.rule(BOOLEAN_LITERAL).define(b.firstOf(TRUE, FALSE))
            b.rule(NUMERIC_LITERAL).define(b.firstOf(INTEGER_LITERAL, NUMBER_LITERAL))
            b.rule(CHARACTER_LITERAL).define(STRING_LITERAL)
            b.rule(INTERVAL_LITERAL).define(b.firstOf(INTERVAL_YEAR_TO_MONTH_LITERAL, INTERVAL_DAY_TO_SECOND_LITERAL))
            b.rule(INQUIRY_DIRECTIVE).define(DOUBLEDOLLAR, IDENTIFIER_NAME)

            b.rule(LITERAL).define(b.firstOf(NULL_LITERAL, BOOLEAN_LITERAL, NUMERIC_LITERAL, CHARACTER_LITERAL, DATE_LITERAL, INTERVAL_LITERAL, INQUIRY_DIRECTIVE))
        }

        private fun createDatatypes(b: PlSqlGrammarBuilder) {
            b.rule(DATATYPE_LENGTH).define(b.firstOf(INTEGER_LITERAL, INQUIRY_DIRECTIVE)).skip()

            b.rule(NUMERIC_DATATYPE_CONSTRAINT).define(
                LPARENTHESIS, b.firstOf(DATATYPE_LENGTH, MULTIPLICATION), b.optional(COMMA, b.optional(MINUS), DATATYPE_LENGTH), RPARENTHESIS
            ).skip()

            b.rule(NUMERIC_DATATYPE).define(
                    b.firstOf(
                            BINARY_DOUBLE,
                            BINARY_FLOAT,
                            BINARY_INTEGER,
                            DEC,
                            DECIMAL,
                            b.sequence(DOUBLE, PRECISION),
                            FLOAT,
                            INT,
                            INTEGER,
                            NATURAL,
                            NATURALN,
                            NUMBER,
                            NUMERIC,
                            PLS_INTEGER,
                            POSITIVE,
                            POSITIVEN,
                            REAL,
                            SIGNTYPE,
                            SMALLINT),
                    b.optional(NUMERIC_DATATYPE_CONSTRAINT))

            b.rule(CHARACTER_SET_CLAUSE).define(CHARACTER, SET, b.firstOf(ANY_CS, b.sequence(IDENTIFIER_NAME, MOD, CHARSET)))

            b.rule(LOB_DATATYPE).define(b.firstOf(BFILE, BLOB, CLOB, NCLOB), b.optional(CHARACTER_SET_CLAUSE))


            b.rule(CHARACTER_DATATYPE_CONSTRAINT).define(
                b.optional(LPARENTHESIS, DATATYPE_LENGTH, b.optional(b.firstOf(BYTE, CHAR)), RPARENTHESIS),
                b.optional(CHARACTER_SET_CLAUSE)
            ).skip()

            b.rule(CHARACTER_DATAYPE).define(
                    b.firstOf(
                            CHAR,
                            CHARACTER,
                            b.sequence(LONG, b.optional(RAW)),
                            NCHAR,
                            NVARCHAR2,
                            RAW,
                            ROWID,
                            STRING,
                            UROWID,
                            VARCHAR,
                            VARCHAR2),
                CHARACTER_DATATYPE_CONSTRAINT)

            b.rule(BOOLEAN_DATATYPE).define(BOOLEAN)

            b.rule(DATE_DATATYPE).define(b.firstOf(
                    DATE,
                    b.sequence(TIMESTAMP, b.optional(LPARENTHESIS, DATATYPE_LENGTH, RPARENTHESIS), b.optional(WITH, b.optional(LOCAL), TIME, ZONE)),
                    b.sequence(INTERVAL, YEAR, b.optional(LPARENTHESIS, DATATYPE_LENGTH, RPARENTHESIS), TO, MONTH),
                    b.sequence(INTERVAL, DAY, b.optional(LPARENTHESIS, DATATYPE_LENGTH, RPARENTHESIS),
                            TO, SECOND, b.optional(LPARENTHESIS, DATATYPE_LENGTH, RPARENTHESIS))))

            b.rule(ANCHORED_DATATYPE).define(CUSTOM_DATATYPE, MOD, b.firstOf(TYPE, ROWTYPE))

            b.rule(CUSTOM_DATATYPE).define(
                MEMBER_EXPRESSION,
                b.optional(b.firstOf(NUMERIC_DATATYPE_CONSTRAINT, CHARACTER_DATATYPE_CONSTRAINT)))

            b.rule(REF_DATATYPE).define(REF, CUSTOM_DATATYPE)

            b.rule(DATATYPE).define(b.firstOf(
                    NUMERIC_DATATYPE,
                    LOB_DATATYPE,
                    CHARACTER_DATAYPE,
                    BOOLEAN_DATATYPE,
                    DATE_DATATYPE,
                    ANCHORED_DATATYPE,
                    REF_DATATYPE,
                    CUSTOM_DATATYPE),
                    b.optional(b.firstOf(
                            b.sequence(NOT, NULL),
                            NULL)))
        }

        private fun createStatements(b: PlSqlGrammarBuilder) {
            b.rule(HOST_AND_INDICATOR_VARIABLE).define(COLON, IDENTIFIER_NAME, b.optional(COLON, IDENTIFIER_NAME))

            b.rule(NULL_STATEMENT, NullStatement::class).define(NULL, SEMICOLON)

            b.rule(EXCEPTION_HANDLER).define(
                    WHEN,
                    b.firstOf(OTHERS, OBJECT_REFERENCE),
                    b.zeroOrMore(OR, b.firstOf(OTHERS, OBJECT_REFERENCE)),
                    THEN, STATEMENTS)

            b.rule(LABEL).define(LLABEL, IDENTIFIER_NAME, RLABEL)

            b.rule(STATEMENTS_SECTION).define(
                    BEGIN,
                    STATEMENTS,
                    b.optional(EXCEPTION, b.oneOrMore(EXCEPTION_HANDLER)),
                    END, b.optional(IDENTIFIER_NAME), SEMICOLON)

            b.rule(BLOCK_STATEMENT).define(
                    b.optional(LABEL),
                    b.optional(DECLARE, b.optional(DECLARE_SECTION)),
                    STATEMENTS_SECTION)

            b.rule(ASSIGNMENT_STATEMENT).define(b.optional(LABEL), OBJECT_REFERENCE, ASSIGNMENT, EXPRESSION, SEMICOLON)

            b.rule(ELSIF_CLAUSE, ElsifClause::class).define(ELSIF, EXPRESSION, THEN, STATEMENTS)

            b.rule(ELSE_CLAUSE, ElseClause::class).define(ELSE, STATEMENTS)

            b.rule(IF_STATEMENT, IfStatement::class).define(
                    b.optional(LABEL),
                    IF, EXPRESSION, THEN,
                    STATEMENTS,
                    b.zeroOrMore(ELSIF_CLAUSE),
                    b.optional(ELSE_CLAUSE),
                    END, IF, b.optional(IDENTIFIER_NAME), SEMICOLON)

            b.rule(LOOP_STATEMENT).define(b.optional(LABEL), LOOP, STATEMENTS, END, LOOP, b.optional(IDENTIFIER_NAME), SEMICOLON)

            b.rule(EXIT_STATEMENT).define(b.optional(LABEL), EXIT, b.optional(IDENTIFIER_NAME), b.optional(WHEN, EXPRESSION), SEMICOLON)

            b.rule(CONTINUE_STATEMENT).define(b.optional(LABEL), CONTINUE, b.optional(IDENTIFIER_NAME), b.optional(WHEN, EXPRESSION), SEMICOLON)

            b.rule(GOTO_STATEMENT).define(b.optional(LABEL), GOTO, b.optional(IDENTIFIER_NAME), SEMICOLON)

            b.rule(FOR_STATEMENT).define(
                    b.optional(LABEL),
                    FOR, IDENTIFIER_NAME, IN, b.optional(REVERSE),
                    b.firstOf(
                            b.sequence(EXPRESSION, RANGE, EXPRESSION),
                            OBJECT_REFERENCE),
                    LOOP,
                    STATEMENTS,
                    END, LOOP, b.optional(IDENTIFIER_NAME), SEMICOLON)

            b.rule(WHILE_STATEMENT).define(
                    b.optional(LABEL),
                    WHILE, EXPRESSION, LOOP,
                    STATEMENTS,
                    END, LOOP, b.optional(IDENTIFIER_NAME), SEMICOLON)

            b.rule(FORALL_STATEMENT).define(
                b.optional(LABEL),
                FORALL, IDENTIFIER_NAME, IN,
                b.firstOf(b.sequence(EXPRESSION, RANGE, EXPRESSION),
                    b.sequence(VALUES, OF, IDENTIFIER_NAME),
                    b.sequence(INDICES, OF, IDENTIFIER_NAME, b.optional(BETWEEN, AND_EXPRESSION))),
                b.optional(SAVE, EXCEPTIONS),
                b.firstOf(
                    INSERT_STATEMENT,
                    UPDATE_STATEMENT,
                    DELETE_STATEMENT,
                    MERGE_STATEMENT,
                    EXECUTE_IMMEDIATE_STATEMENT))

            b.rule(RETURN_STATEMENT).define(b.optional(LABEL), RETURN, b.optional(EXPRESSION), SEMICOLON)

            b.rule(COMMIT_STATEMENT).define(b.optional(LABEL), COMMIT_EXPRESSION, SEMICOLON)

            b.rule(ROLLBACK_STATEMENT).define(b.optional(LABEL), ROLLBACK_EXPRESSION, SEMICOLON)

            b.rule(SAVEPOINT_STATEMENT).define(b.optional(LABEL), SAVEPOINT_EXPRESSION, SEMICOLON)

            b.rule(RAISE_STATEMENT, RaiseStatement::class).define(b.optional(LABEL), RAISE, b.optional(MEMBER_EXPRESSION), SEMICOLON)

            b.rule(SELECT_STATEMENT).define(b.optional(LABEL), SELECT_EXPRESSION, SEMICOLON)

            b.rule(INSERT_STATEMENT).define(b.optional(LABEL), INSERT_EXPRESSION, SEMICOLON)

            b.rule(UPDATE_STATEMENT).define(b.optional(LABEL), UPDATE_EXPRESSION, SEMICOLON)

            b.rule(DELETE_STATEMENT).define(b.optional(LABEL), DELETE_EXPRESSION, SEMICOLON)

            b.rule(MERGE_STATEMENT).define(b.optional(LABEL), MERGE_EXPRESSION, SEMICOLON)

            b.rule(CALL_STATEMENT).define(b.optional(LABEL), OBJECT_REFERENCE, SEMICOLON)

            b.rule(UNNAMED_ACTUAL_PAMETER).define(
                    b.optional(b.firstOf(b.sequence(IN, b.optional(OUT)), OUT)),
                    EXPRESSION)

            b.rule(EXECUTE_IMMEDIATE_STATEMENT).define(
                    b.optional(LABEL),
                    b.optional(FORALL_STATEMENT),
                    EXECUTE, IMMEDIATE, CONCATENATION_EXPRESSION,
                    b.optional(INTO_CLAUSE),
                    b.optional(USING, UNNAMED_ACTUAL_PAMETER, b.zeroOrMore(COMMA, UNNAMED_ACTUAL_PAMETER)),
                    b.optional(RETURNING_INTO_CLAUSE),
                    SEMICOLON)

            b.rule(OPEN_STATEMENT).define(
                    b.optional(LABEL),
                    OPEN, MEMBER_EXPRESSION,
                    b.optional(ARGUMENTS),
                    SEMICOLON)

            b.rule(OPEN_FOR_STATEMENT).define(
                    b.optional(LABEL),
                    OPEN, MEMBER_EXPRESSION, FOR, EXPRESSION,
                    b.optional(USING, UNNAMED_ACTUAL_PAMETER, b.zeroOrMore(COMMA, UNNAMED_ACTUAL_PAMETER)),
                    SEMICOLON)

            b.rule(FETCH_STATEMENT).define(
                    b.optional(LABEL),
                    FETCH, MEMBER_EXPRESSION,
                    INTO_CLAUSE, b.optional(LIMIT, EXPRESSION),
                    SEMICOLON)

            b.rule(CLOSE_STATEMENT).define(b.optional(LABEL), CLOSE, MEMBER_EXPRESSION, SEMICOLON)

            b.rule(PIPE_ROW_STATEMENT).define(b.optional(LABEL), PIPE, ROW, LPARENTHESIS, EXPRESSION, RPARENTHESIS, SEMICOLON)

            b.rule(CASE_STATEMENT).define(
                    b.optional(LABEL),
                    CASE, b.optional(EXPRESSION),
                    b.oneOrMore(WHEN, EXPRESSION, THEN, STATEMENTS),
                    b.optional(ELSE, STATEMENTS),
                    END, CASE, b.optional(IDENTIFIER_NAME), SEMICOLON)

            //https://docs.oracle.com/cd/E11882_01/server.112/e41084/statements_10005.htm#SQLRF01705
            b.rule(SET_TRANSACTION_STATEMENT).define(b.optional(LABEL), SET_TRANSACTION_EXPRESSION, SEMICOLON)

            b.rule(INLINE_PRAGMA_STATEMENT).define(PRAGMA, INLINE,
                    LPARENTHESIS, MEMBER_EXPRESSION, COMMA, STRING_LITERAL, RPARENTHESIS, SEMICOLON)

            b.rule(STATEMENT).define(b.firstOf(NULL_STATEMENT,
                    BLOCK_STATEMENT,
                    ASSIGNMENT_STATEMENT,
                    IF_STATEMENT,
                    LOOP_STATEMENT,
                    EXIT_STATEMENT,
                    CONTINUE_STATEMENT,
                    GOTO_STATEMENT,
                    FOR_STATEMENT,
                    WHILE_STATEMENT,
                    RETURN_STATEMENT,
                    COMMIT_STATEMENT,
                    ROLLBACK_STATEMENT,
                    SAVEPOINT_STATEMENT,
                    RAISE_STATEMENT,
                    FORALL_STATEMENT,
                    SELECT_STATEMENT,
                    INSERT_STATEMENT,
                    UPDATE_STATEMENT,
                    DELETE_STATEMENT,
                    CALL_STATEMENT,
                    EXECUTE_IMMEDIATE_STATEMENT,
                    OPEN_STATEMENT,
                    OPEN_FOR_STATEMENT,
                    FETCH_STATEMENT,
                    CLOSE_STATEMENT,
                    PIPE_ROW_STATEMENT,
                    CASE_STATEMENT,
                    SET_TRANSACTION_STATEMENT,
                    MERGE_STATEMENT,
                    INLINE_PRAGMA_STATEMENT))

            b.rule(STATEMENTS, Statements::class).define(b.oneOrMore(STATEMENT))
        }

        private fun createExpressions(b: PlSqlGrammarBuilder) {
            // Reference: http://docs.oracle.com/cd/B28359_01/appdev.111/b28370/expression.htm

            b.rule(VARIABLE_NAME).define(b.firstOf(IDENTIFIER_NAME, HOST_AND_INDICATOR_VARIABLE))

            b.rule(PRIMARY_EXPRESSION).define(
                    b.firstOf(LITERAL, VARIABLE_NAME, SQL, MULTIPLICATION)).skip()

            b.rule(BRACKED_EXPRESSION).define(b.firstOf(
                    PRIMARY_EXPRESSION,
                    b.sequence(LPARENTHESIS, EXPRESSION, RPARENTHESIS))).skipIfOneChild()

            b.rule(MULTIPLE_VALUE_EXPRESSION).define(b.firstOf(
                    BRACKED_EXPRESSION,
                    b.sequence(LPARENTHESIS, EXPRESSION, b.oneOrMore(COMMA, EXPRESSION), RPARENTHESIS))).skipIfOneChild()

            b.rule(MEMBER_EXPRESSION).define(
                    MULTIPLE_VALUE_EXPRESSION,
                    b.zeroOrMore(
                            b.firstOf(DOT,
                                    REMOTE,
                                    b.sequence(MOD, b.nextNot(ROWTYPE), b.nextNot(TYPE))),
                            b.firstOf(
                                    IDENTIFIER_NAME,
                                    COUNT,
                                    ROWCOUNT,
                                    BULK_ROWCOUNT,
                                    FIRST,
                                    LAST,
                                    LIMIT,
                                    NEXT,
                                    PRIOR,
                                    EXISTS,
                                    FOUND,
                                    NOTFOUND,
                                    ISOPEN,
                                    DELETE,
                                    TRIM,
                                    EXTEND,
                                    NEXTVAL,
                                    CURRVAL)
                    )).skipIfOneChild()

            b.rule(ARGUMENT).define(b.optional(IDENTIFIER_NAME, ASSOCIATION), b.optional(DISTINCT), EXPRESSION)

            b.rule(ARGUMENTS).define(LPARENTHESIS, b.optional(ARGUMENT, b.zeroOrMore(COMMA, ARGUMENT)), RPARENTHESIS)

            b.rule(TREAT_AS_EXPRESSION).define(b.optional(TREAT), LPARENTHESIS, EXPRESSION, AS, b.optional(REF), OBJECT_REFERENCE, RPARENTHESIS)

            b.rule(SET_EXPRESSION).define(SET, LPARENTHESIS, EXPRESSION, RPARENTHESIS)

            b.rule(METHOD_CALL).define(MEMBER_EXPRESSION, b.oneOrMore(ARGUMENTS))

            b.rule(CALL_EXPRESSION).define(b.firstOf(
                    TREAT_AS_EXPRESSION,
                    SET_EXPRESSION,
                    SingleRowSqlFunctionsGrammar.SINGLE_ROW_SQL_FUNCTION,
                    AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION,
                    METHOD_CALL)).skipIfOneChild()

            b.rule(OBJECT_REFERENCE).define(
                    b.firstOf(CALL_EXPRESSION, MEMBER_EXPRESSION),
                    b.zeroOrMore(DOT, b.firstOf(CALL_EXPRESSION, MEMBER_EXPRESSION)),
                    b.optional(LPARENTHESIS, PLUS, RPARENTHESIS)).skipIfOneChild()

            b.rule(POSTFIX_EXPRESSION).define(OBJECT_REFERENCE,
                    b.optional(b.firstOf(
                            b.sequence(IS, b.optional(NOT), NULL),
                            ANALYTIC_CLAUSE,
                            b.sequence(KEEP_CLAUSE, b.optional(ANALYTIC_CLAUSE))))).skipIfOneChild()

            b.rule(IN_EXPRESSION).define(POSTFIX_EXPRESSION,
                    b.optional(b.sequence(
                            b.optional(NOT), IN,
                            b.firstOf(
                                    b.sequence(LPARENTHESIS, EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION), RPARENTHESIS),
                                    EXPRESSION)))).skipIfOneChild()

            b.rule(EXISTS_EXPRESSION).define(EXISTS, LPARENTHESIS, EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION), RPARENTHESIS).skipIfOneChild()

            b.rule(CASE_EXPRESSION).define(
                    CASE, b.optional(EXPRESSION),
                    b.oneOrMore(WHEN, EXPRESSION, THEN, EXPRESSION),
                    b.optional(ELSE, EXPRESSION),
                    END)

            b.rule(AT_TIME_ZONE_EXPRESSION).define(AT, b.firstOf(LOCAL, b.sequence(TIME, ZONE, EXPRESSION)))

            b.rule(NEW_OBJECT_EXPRESSION).define(NEW, OBJECT_REFERENCE, b.optional(ARGUMENTS))

            //https://docs.oracle.com/cd/E11882_01/server.112/e41084/operators006.htm#SQLRF0032
            b.rule(MULTISET_EXPRESSION).define(
                    OBJECT_REFERENCE,
                    b.oneOrMore(
                            MULTISET,
                            b.firstOf(EXCEPT, INTERSECT, UNION),
                            b.optional(b.firstOf(ALL, DISTINCT)),
                            OBJECT_REFERENCE))

            b.rule(UNARY_EXPRESSION).define(b.firstOf(
                    b.sequence(PLUS, UNARY_EXPRESSION),
                    b.sequence(MINUS, UNARY_EXPRESSION),
                    b.sequence(PRIOR, UNARY_EXPRESSION),
                    b.sequence(CONNECT_BY_ROOT, UNARY_EXPRESSION),
                    EXISTS_EXPRESSION,
                    MULTISET_EXPRESSION,
                    NEW_OBJECT_EXPRESSION,
                    CASE_EXPRESSION,
                    IN_EXPRESSION,
                    SELECT_EXPRESSION),
                    b.optional(AT_TIME_ZONE_EXPRESSION)).skipIfOneChild()

            b.rule(EXPONENTIATION_EXPRESSION).define(UNARY_EXPRESSION, b.zeroOrMore(EXPONENTIATION, UNARY_EXPRESSION)).skipIfOneChild()

            b.rule(MULTIPLICATIVE_EXPRESSION).define(EXPONENTIATION_EXPRESSION, b.zeroOrMore(b.firstOf(MULTIPLICATION, DIVISION, MOD_KEYWORD), EXPONENTIATION_EXPRESSION)).skipIfOneChild()

            b.rule(ADDITIVE_EXPRESSION).define(MULTIPLICATIVE_EXPRESSION, b.zeroOrMore(b.firstOf(PLUS, MINUS), MULTIPLICATIVE_EXPRESSION)).skipIfOneChild()

            b.rule(CONCATENATION_EXPRESSION).define(ADDITIVE_EXPRESSION, b.zeroOrMore(CONCATENATION, ADDITIVE_EXPRESSION)).skipIfOneChild()

            b.rule(COMPARISON_EXPRESSION).define(b.firstOf(
                    ConditionsGrammar.CONDITION,
                    CONCATENATION_EXPRESSION)).skipIfOneChild()

            b.rule(NOT_EXPRESSION).define(b.optional(NOT), COMPARISON_EXPRESSION).skipIfOneChild()
            b.rule(AND_EXPRESSION).define(NOT_EXPRESSION, b.zeroOrMore(AND, NOT_EXPRESSION)).skipIfOneChild()
            b.rule(OR_EXPRESSION).define(AND_EXPRESSION, b.zeroOrMore(OR, AND_EXPRESSION)).skipIfOneChild()

            b.rule(BOOLEAN_EXPRESSION).define(OR_EXPRESSION).skip()

            b.rule(EXPRESSION).define(BOOLEAN_EXPRESSION).skipIfOneChild()
        }

        private fun createDeclarations(b: PlSqlGrammarBuilder) {
            b.rule(DEFAULT_VALUE_ASSIGNMENT).define(b.firstOf(ASSIGNMENT, DEFAULT), EXPRESSION)

            b.rule(PARAMETER_DECLARATION).define(
                    IDENTIFIER_NAME,
                    b.optional(IN),
                    b.firstOf(
                            b.sequence(DATATYPE, b.optional(DEFAULT_VALUE_ASSIGNMENT)),
                            b.sequence(OUT, b.optional(NOCOPY), DATATYPE))
            )

            b.rule(JAVA_DECLARATION).define(LANGUAGE, JAVA, NAME, STRING_LITERAL)

            b.rule(C_DECLARATION).define(
                    b.firstOf(b.sequence(LANGUAGE, "C"), EXTERNAL),
                    b.optional(NAME, IDENTIFIER_NAME), LIBRARY, IDENTIFIER_NAME, b.optional(NAME, IDENTIFIER_NAME),
                    b.optional(AGENT, IN, LPARENTHESIS, b.oneOrMore(IDENTIFIER_NAME, b.optional(COMMA)), RPARENTHESIS),
                    b.optional(WITH, CONTEXT),
                    b.optional(PARAMETERS, LPARENTHESIS, b.oneOrMore(EXTERNAL_PARAMETER, b.optional(COMMA)), RPARENTHESIS))

            b.rule(EXTERNAL_PARAMETER_PROPERTY).define(
                    b.sequence(INDICATOR, b.optional(b.firstOf(STRUCT, TDO))),
                    LENGTH,
                    DURATION,
                    MAXLEN,
                    CHARSETID,
                    CHARSETFORM)

            b.rule(EXTERNAL_PARAMETER).define(b.firstOf(
                    CONTEXT,
                    b.sequence(SELF, b.firstOf(TDO, EXTERNAL_PARAMETER_PROPERTY)),
                    b.sequence(
                            b.firstOf(RETURN, IDENTIFIER_NAME),
                            b.optional(EXTERNAL_PARAMETER_PROPERTY),
                            b.optional(BY, REFERENCE),
                            b.optional(DATATYPE))))

            b.rule(CALL_SPECIFICATION).define(b.firstOf(JAVA_DECLARATION, C_DECLARATION), SEMICOLON)

            // http://docs.oracle.com/cd/B28359_01/appdev.111/b28370/procedure.htm
            b.rule(PROCEDURE_DECLARATION).define(
                    PROCEDURE, IDENTIFIER_NAME,
                    b.optional(LPARENTHESIS, b.oneOrMore(PARAMETER_DECLARATION, b.optional(COMMA)), RPARENTHESIS),
                    b.optional(b.firstOf(
                            SEMICOLON,
                            b.sequence(b.firstOf(IS, AS),
                                    b.firstOf(
                                            b.sequence(b.optional(DECLARE_SECTION), STATEMENTS_SECTION),
                                            CALL_SPECIFICATION)))
                    ))

            // http://docs.oracle.com/cd/B28359_01/appdev.111/b28370/function.htm
            b.rule(FUNCTION_DECLARATION).define(
                    FUNCTION, IDENTIFIER_NAME,
                    b.optional(LPARENTHESIS, b.oneOrMore(PARAMETER_DECLARATION, b.optional(COMMA)), RPARENTHESIS),
                    RETURN, DATATYPE, b.zeroOrMore(b.firstOf(DETERMINISTIC, PIPELINED)),
                    b.optional(RESULT_CACHE, b.optional(RELIES_ON, LPARENTHESIS, b.oneOrMore(OBJECT_REFERENCE, b.optional(COMMA)), RPARENTHESIS)),
                    b.optional(b.firstOf(
                            SEMICOLON,
                            b.sequence(b.firstOf(IS, AS),
                                    b.firstOf(
                                            b.sequence(b.optional(DECLARE_SECTION), STATEMENTS_SECTION),
                                            CALL_SPECIFICATION)))
                    ))

            b.rule(VARIABLE_DECLARATION).define(IDENTIFIER_NAME,
                    b.optional(CONSTANT),
                    b.firstOf(
                            b.sequence(
                                    DATATYPE,
                                    b.optional(DEFAULT_VALUE_ASSIGNMENT)),
                            EXCEPTION),
                    SEMICOLON)

            b.rule(CUSTOM_SUBTYPE).define(
                    SUBTYPE, IDENTIFIER_NAME, IS, DATATYPE,
                    b.optional(RANGE_KEYWORD, NUMERIC_LITERAL, RANGE, NUMERIC_LITERAL),
                    SEMICOLON)

            b.rule(CURSOR_PARAMETER_DECLARATION).define(
                    IDENTIFIER_NAME,
                    b.optional(IN),
                    DATATYPE, b.optional(DEFAULT_VALUE_ASSIGNMENT))

            b.rule(CURSOR_DECLARATION).define(
                    CURSOR, IDENTIFIER_NAME,
                    b.optional(LPARENTHESIS, b.oneOrMore(CURSOR_PARAMETER_DECLARATION, b.optional(COMMA)), RPARENTHESIS),
                    b.optional(RETURN, DATATYPE),
                    b.optional(IS, SELECT_EXPRESSION), SEMICOLON)

            b.rule(RECORD_FIELD_DECLARATION).define(
                    IDENTIFIER_NAME, DATATYPE,
                    b.optional(DEFAULT_VALUE_ASSIGNMENT))

            b.rule(RECORD_DECLARATION).define(
                    TYPE, IDENTIFIER_NAME, IS, RECORD,
                    LPARENTHESIS, RECORD_FIELD_DECLARATION, b.zeroOrMore(COMMA, RECORD_FIELD_DECLARATION), RPARENTHESIS,
                    SEMICOLON)

            b.rule(NESTED_TABLE_DEFINITION).define(TABLE, OF, DATATYPE, b.optional(NOT, NULL))

            b.rule(TABLE_OF_DECLARATION).define(
                    TYPE, IDENTIFIER_NAME, IS, NESTED_TABLE_DEFINITION,
                    b.optional(INDEX, BY, DATATYPE),
                    SEMICOLON)

            b.rule(VARRAY_TYPE_DEFINITION).define(
                    b.firstOf(VARRAY, b.sequence(b.optional(VARYING), ARRAY)),
                    LPARENTHESIS, INTEGER_LITERAL, RPARENTHESIS,
                    OF, DATATYPE, b.optional(NOT, NULL))

            b.rule(VARRAY_DECLARATION).define(
                    TYPE, IDENTIFIER_NAME, IS, VARRAY_TYPE_DEFINITION, SEMICOLON)

            b.rule(REF_CURSOR_DECLARATION).define(TYPE, IDENTIFIER_NAME, IS, REF, CURSOR, b.optional(RETURN, DATATYPE), SEMICOLON)

            b.rule(AUTONOMOUS_TRANSACTION_PRAGMA).define(PRAGMA, AUTONOMOUS_TRANSACTION, SEMICOLON)

            b.rule(EXCEPTION_INIT_PRAGMA).define(PRAGMA, EXCEPTION_INIT, LPARENTHESIS, EXPRESSION, COMMA, EXPRESSION, RPARENTHESIS, SEMICOLON)

            b.rule(SERIALLY_REUSABLE_PRAGMA).define(PRAGMA, SERIALLY_REUSABLE, SEMICOLON)

            b.rule(INTERFACE_PRAGMA).define(
                    PRAGMA, INTERFACE,
                    LPARENTHESIS, IDENTIFIER_NAME, COMMA, IDENTIFIER_NAME, COMMA, INTEGER_LITERAL, RPARENTHESIS, SEMICOLON)

            b.rule(RESTRICT_REFERENCES_PRAGMA).define(
                    PRAGMA, RESTRICT_REFERENCES, LPARENTHESIS, b.oneOrMore(b.anyTokenButNot(RPARENTHESIS)), RPARENTHESIS, SEMICOLON)

            b.rule(UDF_PRAGMA).define(PRAGMA, UDF, SEMICOLON)

            b.rule(DEPRECATE_PRAGMA).define(PRAGMA, DEPRECATE, LPARENTHESIS, EXPRESSION, b.optional(COMMA, STRING_LITERAL), RPARENTHESIS)

            b.rule(PRAGMA_DECLARATION).define(b.firstOf(
                    EXCEPTION_INIT_PRAGMA,
                    AUTONOMOUS_TRANSACTION_PRAGMA,
                    SERIALLY_REUSABLE_PRAGMA,
                    INTERFACE_PRAGMA,
                    RESTRICT_REFERENCES_PRAGMA,
                    UDF_PRAGMA,
                    b.sequence(DEPRECATE_PRAGMA, SEMICOLON)))

            b.rule(DECLARE_SECTION).define(b.oneOrMore(b.firstOf(
                    PRAGMA_DECLARATION,
                    VARIABLE_DECLARATION,
                    PROCEDURE_DECLARATION,
                    FUNCTION_DECLARATION,
                    CUSTOM_SUBTYPE,
                    CURSOR_DECLARATION,
                    RECORD_DECLARATION,
                    TABLE_OF_DECLARATION,
                    VARRAY_DECLARATION,
                    REF_CURSOR_DECLARATION)))
        }

        private fun createTrigger(b: PlSqlGrammarBuilder) {
            b.rule(CREATE_TRIGGER).define(
                    CREATE, b.optional(OR, REPLACE),
                    b.optional(b.firstOf(EDITIONABLE, NONEDITIONABLE)),
                    TRIGGER, UNIT_NAME,
                    b.firstOf(SIMPLE_DML_TRIGGER, INSTEAD_OF_DML_TRIGGER, COMPOUND_DML_TRIGGER, SYSTEM_TRIGGER)
            )

            b.rule(SIMPLE_DML_TRIGGER).define(
                    b.firstOf(BEFORE, AFTER),
                    DML_EVENT_CLAUSE, b.zeroOrMore(OR, DML_EVENT_CLAUSE), ON, UNIT_NAME,
                    b.optional(REFERENCING_CLAUSE),
                    b.optional(FOR, EACH, ROW),
                    b.optional(TRIGGER_EDITION_CLAUSE),
                    b.optional(TRIGGER_ORDERING_CLAUSE),
                    b.optional(b.firstOf(ENABLE, DISABLE)),
                    b.optional(WHEN, LPARENTHESIS, EXPRESSION, RPARENTHESIS),
                    b.optional(DECLARE, b.optional(DECLARE_SECTION)), STATEMENTS_SECTION
            )

            b.rule(INSTEAD_OF_DML_TRIGGER).define(
                    INSTEAD, OF,
                    b.firstOf(
                            DELETE,
                            INSERT,
                            UPDATE),
                    b.zeroOrMore(OR, b.firstOf(
                            DELETE,
                            INSERT,
                            UPDATE)),
                    ON,
                    b.optional(NESTED, TABLE, IDENTIFIER_NAME, OF),
                    UNIT_NAME,
                    b.optional(REFERENCING_CLAUSE),
                    b.optional(FOR, EACH, ROW),
                    b.optional(TRIGGER_EDITION_CLAUSE),
                    b.optional(TRIGGER_ORDERING_CLAUSE),
                    b.optional(b.firstOf(ENABLE, DISABLE)),
                    b.optional(DECLARE, b.optional(DECLARE_SECTION)), STATEMENTS_SECTION
            )

            b.rule(COMPOUND_DML_TRIGGER).define(
                    FOR, DML_EVENT_CLAUSE, b.zeroOrMore(OR, DML_EVENT_CLAUSE), ON, UNIT_NAME,
                    b.optional(REFERENCING_CLAUSE),
                    b.optional(TRIGGER_EDITION_CLAUSE),
                    b.optional(TRIGGER_ORDERING_CLAUSE),
                    b.optional(b.firstOf(ENABLE, DISABLE)),
                    b.optional(WHEN, LPARENTHESIS, EXPRESSION, RPARENTHESIS),
                    COMPOUND_TRIGGER_BLOCK
            )

            b.rule(SYSTEM_TRIGGER).define(
                    b.firstOf(
                            b.sequence(b.firstOf(BEFORE, AFTER, b.sequence(INSTEAD, OF)),
                                    DDL_EVENT, b.zeroOrMore(OR, DDL_EVENT)),
                            b.sequence(DATABASE_EVENT, b.zeroOrMore(OR, DATABASE_EVENT))),
                    ON,
                    b.firstOf(
                            b.sequence(b.optional(IDENTIFIER_NAME, DOT), SCHEMA),
                            b.sequence(b.optional(PLUGGABLE), DATABASE)),
                    b.optional(TRIGGER_ORDERING_CLAUSE),
                    b.optional(b.firstOf(ENABLE, DISABLE)),
                    b.optional(DECLARE, b.optional(DECLARE_SECTION)), STATEMENTS_SECTION
            )

            b.rule(DML_EVENT_CLAUSE).define(
                    b.firstOf(
                            DELETE,
                            INSERT,
                            UPDATE),
                    b.optional(OF, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME))
            )

            b.rule(REFERENCING_CLAUSE).define(
                    REFERENCING,
                    b.zeroOrMore(
                            b.firstOf(
                                    b.sequence(OLD, b.optional(AS), IDENTIFIER_NAME),
                                    b.sequence(NEW, b.optional(AS), IDENTIFIER_NAME),
                                    b.sequence(PARENT, b.optional(AS), IDENTIFIER_NAME)
                            )))

            b.rule(TRIGGER_EDITION_CLAUSE).define(
                    b.optional(b.firstOf(FORWARD, REVERSE)),
                    CROSSEDITION)

            b.rule(TRIGGER_ORDERING_CLAUSE).define(
                    b.firstOf(FOLLOWS, PRECEDES),
                    UNIT_NAME, b.zeroOrMore(COMMA, UNIT_NAME))

            b.rule(COMPOUND_TRIGGER_BLOCK).define(
                    COMPOUND, TRIGGER,
                    b.optional(DECLARE_SECTION),
                    b.oneOrMore(TIMING_POINT_SECTION),
                    END, UNIT_NAME, SEMICOLON)

            b.rule(TIMING_POINT_SECTION).define(
                    TIMING_POINT, IS, BEGIN, TPS_BODY, END, TIMING_POINT, SEMICOLON)

            b.rule(TIMING_POINT).define(
                    b.firstOf(
                            b.sequence(BEFORE, STATEMENT_KEYWORD),
                            b.sequence(BEFORE, EACH, ROW),
                            b.sequence(AFTER, STATEMENT_KEYWORD),
                            b.sequence(AFTER, EACH, ROW),
                            b.sequence(INSTEAD, OF, EACH, ROW)
                    ))

            b.rule(TPS_BODY).define(
                    b.oneOrMore(STATEMENT),
                    b.optional(EXCEPTION, b.oneOrMore(EXCEPTION_HANDLER)))

            b.rule(DDL_EVENT).define(
                    b.firstOf(
                            ALTER,
                            ANALYZE,
                            b.sequence(ASSOCIATE, STATISTICS),
                            AUDIT,
                            COMMENT,
                            CREATE,
                            b.sequence(DISASSOCIATE, STATISTICS),
                            DROP,
                            GRANT,
                            NOAUDIT,
                            RENAME,
                            REVOKE,
                            TRUNCATE,
                            DDL)
            )

            b.rule(DATABASE_EVENT).define(
                    b.firstOf(
                            b.sequence(AFTER, STARTUP),
                            b.sequence(BEFORE, SHUTDOWN),
                            b.sequence(AFTER, DB_ROLE_CHANGE),
                            b.sequence(AFTER, SERVERERROR),
                            b.sequence(AFTER, LOGON),
                            b.sequence(BEFORE, LOGOFF),
                            b.sequence(AFTER, SUSPEND),
                            b.sequence(AFTER, CLONE),
                            b.sequence(BEFORE, UNPLUG),
                            b.sequence(b.firstOf(BEFORE, AFTER), SET, CONTAINER))
            )
        }

        private fun createProgramUnits(b: PlSqlGrammarBuilder) {
            b.rule(EXECUTE_PLSQL_BUFFER).define(DIVISION)

            b.rule(UNIT_NAME).define(b.optional(IDENTIFIER_NAME, DOT), IDENTIFIER_NAME)

            // https://docs.oracle.com/en/database/oracle/oracle-database/18/lnpls/CREATE-PROCEDURE-statement.html
            b.rule(CREATE_PROCEDURE).define(
                    CREATE, b.optional(OR, REPLACE), b.optional(b.firstOf(EDITIONABLE, NONEDITIONABLE)),
                    PROCEDURE, UNIT_NAME, b.optional(TIMESTAMP, STRING_LITERAL),
                    b.optional(LPARENTHESIS, b.oneOrMore(PARAMETER_DECLARATION, b.optional(COMMA)), RPARENTHESIS),
                    b.optional(SHARING, EQUALS, b.firstOf(METADATA, NONE)),
                    b.zeroOrMore(b.firstOf(
                            b.sequence(AUTHID, b.firstOf(CURRENT_USER, DEFINER)),
                            b.sequence(DEFAULT, COLLATION, USING_NLS_COMP),
                            b.sequence(ACCESSIBLE, BY, LPARENTHESIS,
                                    b.firstOf(FUNCTION, PROCEDURE, PACKAGE, TRIGGER, TYPE),
                                    UNIT_NAME,
                                    RPARENTHESIS))
                    ),
                    b.firstOf(IS, AS),
                    b.firstOf(
                            b.sequence(b.optional(DECLARE_SECTION), STATEMENTS_SECTION),
                            CALL_SPECIFICATION)
            )

            // https://docs.oracle.com/en/database/oracle/oracle-database/18/lnpls/CREATE-FUNCTION-statement.html
            b.rule(CREATE_FUNCTION).define(
                    CREATE, b.optional(OR, REPLACE), b.optional(b.firstOf(EDITIONABLE, NONEDITIONABLE)),
                    FUNCTION, UNIT_NAME, b.optional(TIMESTAMP, STRING_LITERAL),
                    b.optional(LPARENTHESIS, b.oneOrMore(PARAMETER_DECLARATION, b.optional(COMMA)), RPARENTHESIS),
                    RETURN, DATATYPE,
                    b.optional(SHARING, EQUALS, b.firstOf(METADATA, NONE)),
                    b.zeroOrMore(b.firstOf(
                            DETERMINISTIC,
                            PIPELINED,
                            PARALLEL_ENABLE,
                            b.sequence(
                                    RESULT_CACHE,
                                    b.optional(RELIES_ON, LPARENTHESIS, b.oneOrMore(OBJECT_REFERENCE, b.optional(COMMA)), RPARENTHESIS)),
                            b.sequence(AUTHID, b.firstOf(CURRENT_USER, DEFINER)),
                            b.sequence(DEFAULT, COLLATION, USING_NLS_COMP),
                            b.sequence(ACCESSIBLE, BY, LPARENTHESIS,
                                    b.firstOf(FUNCTION, PROCEDURE, PACKAGE, TRIGGER, TYPE),
                                    UNIT_NAME,
                                    RPARENTHESIS))),
                    b.firstOf(
                            b.sequence(
                                    b.firstOf(IS, AS),
                                    b.firstOf(
                                            b.sequence(b.optional(DECLARE_SECTION), STATEMENTS_SECTION),
                                            CALL_SPECIFICATION)),
                            b.sequence(AGGREGATE, USING, OBJECT_REFERENCE, SEMICOLON))
            )

            // https://docs.oracle.com/en/database/oracle/oracle-database/18/lnpls/CREATE-PACKAGE-statement.html
            b.rule(CREATE_PACKAGE).define(
                    b.optional(CREATE), b.optional(OR, REPLACE), b.optional(b.firstOf(EDITIONABLE, NONEDITIONABLE)),
                    PACKAGE, UNIT_NAME, b.optional(TIMESTAMP, STRING_LITERAL),
                    b.optional(SHARING, EQUALS, b.firstOf(METADATA, NONE)),
                    b.zeroOrMore(b.firstOf(
                            b.sequence(AUTHID, b.firstOf(CURRENT_USER, DEFINER)),
                            b.sequence(DEFAULT, COLLATION, USING_NLS_COMP),
                            b.sequence(ACCESSIBLE, BY, LPARENTHESIS,
                                    b.firstOf(FUNCTION, PROCEDURE, PACKAGE, TRIGGER, TYPE),
                                    UNIT_NAME,
                                    RPARENTHESIS))),
                    b.firstOf(IS, AS),
                    b.optional(DECLARE_SECTION),
                    END, b.optional(IDENTIFIER_NAME), SEMICOLON)

            // https://docs.oracle.com/en/database/oracle/oracle-database/18/lnpls/CREATE-PACKAGE-BODY-statement.html
            b.rule(CREATE_PACKAGE_BODY).define(
                    b.optional(CREATE), b.optional(OR, REPLACE), b.optional(b.firstOf(EDITIONABLE, NONEDITIONABLE)),
                    PACKAGE, BODY, UNIT_NAME, b.optional(TIMESTAMP, STRING_LITERAL),
                    b.firstOf(IS, AS),
                    b.optional(DECLARE_SECTION),
                    b.firstOf(
                            STATEMENTS_SECTION,
                            b.sequence(END, b.optional(IDENTIFIER_NAME), SEMICOLON)))

            b.rule(VIEW_RESTRICTION_CLAUSE).define(
                    WITH, b.firstOf(
                    b.sequence(READ, ONLY),
                    b.sequence(CHECK, OPTION, b.optional(CONSTRAINT, IDENTIFIER_NAME))
            )
            )

            // https://docs.oracle.com/en/database/oracle/oracle-database/18/sqlrf/CREATE-VIEW.html
            b.rule(CREATE_VIEW).define(
                    CREATE, b.optional(
                    b.firstOf(
                            b.sequence(MATERIALIZED, VIEW, UNIT_NAME,
                                    b.zeroOrMore(
                                            b.firstOf(
                                                    b.sequence(PCTFREE, INTEGER_LITERAL),
                                                    b.sequence(PCTUSED, INTEGER_LITERAL),
                                                    b.sequence(INITRANS, INTEGER_LITERAL),
                                                    b.sequence(TABLESPACE, IDENTIFIER_NAME)
                                            )),
                                    b.optional(b.sequence(b.optional(NO), REFRESH, COMPLETE, b.optional(START, WITH, EXPRESSION, NEXT, EXPRESSION)))),
                            b.sequence(b.optional(b.sequence(OR, REPLACE)), b.optional(b.firstOf(EDITIONABLE, NONEDITIONABLE)), b.optional(b.optional(NO), FORCE), VIEW, UNIT_NAME))),
                    b.optional(LPARENTHESIS, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS),
                    AS,
                    SELECT_EXPRESSION,
                    b.optional(VIEW_RESTRICTION_CLAUSE),
                    b.optional(ORDER_BY_CLAUSE),
                    b.optional(SEMICOLON))

            b.rule(TYPE_ATTRIBUTE).define(IDENTIFIER_NAME, DATATYPE)

            b.rule(INHERITANCE_CLAUSE).define(b.oneOrMore(b.optional(NOT), b.firstOf(OVERRIDING, FINAL, INSTANTIABLE)))

            b.rule(TYPE_SUBPROGRAM).define(
                    b.optional(INHERITANCE_CLAUSE),
                    b.firstOf(MEMBER, STATIC),
                    b.firstOf(PROCEDURE_DECLARATION, FUNCTION_DECLARATION))

            b.rule(TYPE_CONSTRUCTOR).define(
                    b.optional(FINAL),
                    b.optional(INSTANTIABLE),
                    CONSTRUCTOR, FUNCTION, IDENTIFIER_NAME,
                    b.optional(
                            LPARENTHESIS,
                            b.optional(SELF, IN, OUT, DATATYPE, COMMA),
                            b.oneOrMore(PARAMETER_DECLARATION, b.optional(COMMA)),
                            RPARENTHESIS),
                    RETURN, SELF, AS, RESULT,
                    b.optional(b.firstOf(IS, AS), b.optional(DECLARE_SECTION), STATEMENTS_SECTION))

            b.rule(MAP_ORDER_FUNCTION).define(
                    b.firstOf(MAP, ORDER),
                    MEMBER, FUNCTION_DECLARATION)

            b.rule(TYPE_ELEMENT_SPEC).define(
                    b.optional(INHERITANCE_CLAUSE),
                    b.firstOf(TYPE_SUBPROGRAM, TYPE_CONSTRUCTOR, MAP_ORDER_FUNCTION))

            b.rule(OBJECT_TYPE_DEFINITION).define(
                    b.firstOf(
                            b.sequence(b.firstOf(IS, AS), OBJECT),
                            b.sequence(UNDER, UNIT_NAME)),
                    LPARENTHESIS, b.optional(DEPRECATE_PRAGMA, COMMA), b.oneOrMore(b.firstOf(TYPE_ELEMENT_SPEC, TYPE_ATTRIBUTE), b.optional(COMMA), b.optional(DEPRECATE_PRAGMA, b.optional(COMMA))), RPARENTHESIS,
                    b.zeroOrMore(b.optional(NOT), b.firstOf(FINAL, INSTANTIABLE)))

            b.rule(CREATE_TYPE).define(
                    CREATE, b.optional(OR, REPLACE), b.optional(b.firstOf(EDITIONABLE, NONEDITIONABLE)),
                    TYPE, UNIT_NAME,
                    b.optional(FORCE),
                    b.optional(SHARING, EQUALS, b.firstOf(METADATA, NONE)),
                    b.zeroOrMore(b.firstOf(
                            b.sequence(AUTHID, b.firstOf(CURRENT_USER, DEFINER)),
                            b.sequence(DEFAULT, COLLATION, USING_NLS_COMP),
                            b.sequence(ACCESSIBLE, BY, LPARENTHESIS,
                                    b.firstOf(FUNCTION, PROCEDURE, PACKAGE, TRIGGER, TYPE),
                                    UNIT_NAME,
                                    RPARENTHESIS))),
                    b.optional(b.firstOf(
                            OBJECT_TYPE_DEFINITION,
                            b.sequence(
                                    b.firstOf(IS, AS),
                                    b.firstOf(
                                            VARRAY_TYPE_DEFINITION,
                                            NESTED_TABLE_DEFINITION)))),
                    b.optional(SEMICOLON))

            b.rule(CREATE_TYPE_BODY).define(
                    CREATE, b.optional(OR, REPLACE), b.optional(b.firstOf(EDITIONABLE, NONEDITIONABLE)),
                    TYPE, BODY, UNIT_NAME,
                    b.firstOf(IS, AS),
                    b.oneOrMore(b.firstOf(TYPE_SUBPROGRAM, TYPE_CONSTRUCTOR, MAP_ORDER_FUNCTION), b.optional(COMMA)),
                    END, b.optional(SEMICOLON))

            b.rule(ANONYMOUS_BLOCK).define(BLOCK_STATEMENT)

            b.rule(COMPILATION_UNIT).define(b.firstOf(
                    ANONYMOUS_BLOCK,
                    CREATE_PROCEDURE,
                    CREATE_FUNCTION,
                    CREATE_PACKAGE,
                    CREATE_PACKAGE_BODY,
                    CREATE_VIEW,
                    CREATE_TRIGGER,
                    CREATE_TYPE_BODY,
                    CREATE_TYPE,
                    PROCEDURE_DECLARATION,
                    FUNCTION_DECLARATION))
        }
    }
}
