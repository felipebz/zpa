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

import static com.sonar.sslr.api.GenericTokenType.EOF;
import static com.sonar.sslr.api.GenericTokenType.IDENTIFIER;
import static org.sonar.plugins.plsqlopen.api.DclGrammar.*;
import static org.sonar.plugins.plsqlopen.api.DdlGrammar.*;
import static org.sonar.plugins.plsqlopen.api.DmlGrammar.*;
import static org.sonar.plugins.plsqlopen.api.TclGrammar.*;
import static org.sonar.plugins.plsqlopen.api.PlSqlKeyword.*;
import static org.sonar.plugins.plsqlopen.api.PlSqlPunctuator.*;
import static org.sonar.plugins.plsqlopen.api.PlSqlTokenType.*;
import static org.sonar.plugins.plsqlopen.api.SqlPlusGrammar.*;

import java.util.List;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

public enum PlSqlGrammar implements GrammarRuleKey {
    
    // Data types
    DATATYPE,
    CHARACTER_SET_CLAUSE,
    NUMERIC_DATATYPE,
    LOB_DATATYPE,
    CHARACTER_DATAYPE,
    BOOLEAN_DATATYPE,
    WITH_TIME_ZONE,
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
    METHOD_CALL,
    CALL_EXPRESSION,
    CASE_EXPRESSION,
    AT_TIME_ZONE_EXPRESSION,
    VARIABLE_NAME,
    
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
    
    // Declarations
    DEFAULT_VALUE_ASSIGNMENT,
    VARIABLE_DECLARATION,
    PARAMETER_DECLARATION,
    CURSOR_PARAMETER_DECLARATION,
    CURSOR_DECLARATION,
    RECORD_FIELD_DECLARATION,
    RECORD_DECLARATION,
    TABLE_OF_DECLARATION,
    VARRAY_DECLARATION,
    REF_CURSOR_DECLARATION,
    AUTONOMOUS_TRANSACTION_PRAGMA,
    EXCEPTION_INIT_PRAGMA,
    SERIALLY_REUSABLE_PRAGMA,
    INTERFACE_PRAGMA,
    PRAGMA_DECLARATION,
    HOST_AND_INDICATOR_VARIABLE,
    
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
    CREATE_TRIGGER,
    
    // Top-level components
    FILE_INPUT;

    public static LexerfulGrammarBuilder create() {
        LexerfulGrammarBuilder b = LexerfulGrammarBuilder.create();

        List<PlSqlKeyword> keywords = PlSqlKeyword.getNonReservedKeywords();
        PlSqlKeyword[] rest = keywords.subList(2, keywords.size()).toArray(new PlSqlKeyword[keywords.size() - 2]);
        b.rule(NON_RESERVED_KEYWORD).is(b.firstOf(keywords.get(0), keywords.get(1), (Object[]) rest));
        
        b.rule(IDENTIFIER_NAME).is(b.firstOf(IDENTIFIER, NON_RESERVED_KEYWORD));
        b.rule(FILE_INPUT).is(b.oneOrMore(b.firstOf(
                COMPILATION_UNIT,
                SQLPLUS_COMMAND,
                DCL_COMMAND,
                DDL_COMMAND,
                DML_COMMAND,
                TCL_COMMAND,
                EXECUTE_PLSQL_BUFFER)), EOF);

        createLiterals(b);
        createDatatypes(b);
        createStatements(b);
        createExpressions(b);
        createDeclarations(b);
        createTrigger(b);
        createProgramUnits(b);
        DdlGrammar.buildOn(b);
        DmlGrammar.buildOn(b);
        DclGrammar.buildOn(b);
        TclGrammar.buildOn(b);
        SqlPlusGrammar.buildOn(b);
        SingleRowSqlFunctionsGrammar.buildOn(b);
        AggregateSqlFunctionsGrammar.buildOn(b);
        ConditionsGrammar.buildOn(b);
        
        b.setRootRule(FILE_INPUT);
        b.buildWithMemoizationOfMatchesForAllRules();
        
        return b;
    }

    private static void createLiterals(LexerfulGrammarBuilder b) {
        b.rule(INTERVAL_YEAR_TO_MONTH_LITERAL).is(
                INTERVAL, CHARACTER_LITERAL, 
                b.firstOf(YEAR, MONTH), b.optional(LPARENTHESIS, INTEGER_LITERAL, RPARENTHESIS),
                b.optional(TO, b.firstOf(YEAR, MONTH)));
        
        b.rule(INTERVAL_DAY_TO_SECOND_LITERAL).is(
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
                    )));
        
        b.rule(NULL_LITERAL).is(NULL);
        b.rule(BOOLEAN_LITERAL).is(b.firstOf(TRUE, FALSE));
        b.rule(NUMERIC_LITERAL).is(b.firstOf(INTEGER_LITERAL, REAL_LITERAL, SCIENTIFIC_LITERAL));
        b.rule(CHARACTER_LITERAL).is(STRING_LITERAL);
        b.rule(INTERVAL_LITERAL).is(b.firstOf(INTERVAL_YEAR_TO_MONTH_LITERAL, INTERVAL_DAY_TO_SECOND_LITERAL));
        
        b.rule(LITERAL).is(b.firstOf(NULL_LITERAL, BOOLEAN_LITERAL, NUMERIC_LITERAL, CHARACTER_LITERAL, DATE_LITERAL, INTERVAL_LITERAL));
    }
    
    private static void createDatatypes(LexerfulGrammarBuilder b) {
        b.rule(NUMERIC_DATATYPE).is(
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
                b.optional(LPARENTHESIS, INTEGER_LITERAL, b.optional(COMMA, INTEGER_LITERAL), RPARENTHESIS));
        
        b.rule(CHARACTER_SET_CLAUSE).is(CHARACTER, SET, b.firstOf(ANY_CS, b.sequence(IDENTIFIER_NAME, MOD, CHARSET)));
        
        b.rule(LOB_DATATYPE).is(b.firstOf(BFILE, BLOB, CLOB, NCLOB), b.optional(CHARACTER_SET_CLAUSE));
        
        b.rule(CHARACTER_DATAYPE).is(
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
                b.optional(LPARENTHESIS, INTEGER_LITERAL, b.optional(b.firstOf(BYTE, CHAR)), RPARENTHESIS),
                b.optional(CHARACTER_SET_CLAUSE));
        
        b.rule(BOOLEAN_DATATYPE).is(BOOLEAN);
        
        b.rule(DATE_DATATYPE).is(b.firstOf(
                DATE,
                b.sequence(TIMESTAMP, b.optional(LPARENTHESIS, INTEGER_LITERAL, RPARENTHESIS), b.optional(WITH, b.optional(LOCAL), TIME, ZONE)),
                b.sequence(INTERVAL, YEAR, b.optional(LPARENTHESIS, INTEGER_LITERAL, RPARENTHESIS), TO, MONTH),
                b.sequence(INTERVAL, DAY, b.optional(LPARENTHESIS, INTEGER_LITERAL, RPARENTHESIS), 
                        TO, SECOND, b.optional(LPARENTHESIS, INTEGER_LITERAL, RPARENTHESIS))));
        
        b.rule(ANCHORED_DATATYPE).is(CUSTOM_DATATYPE, MOD, b.firstOf(TYPE, ROWTYPE));

        b.rule(CUSTOM_DATATYPE).is(MEMBER_EXPRESSION);
        
        b.rule(REF_DATATYPE).is(REF, CUSTOM_DATATYPE);
        
        b.rule(DATATYPE).is(b.firstOf(
                NUMERIC_DATATYPE,
                LOB_DATATYPE,
                CHARACTER_DATAYPE,
                BOOLEAN_DATATYPE,
                DATE_DATATYPE,
                ANCHORED_DATATYPE,
                CUSTOM_DATATYPE,
                REF_DATATYPE), b.optional(NOT, NULL));
    }

    private static void createStatements(LexerfulGrammarBuilder b) {
        b.rule(HOST_AND_INDICATOR_VARIABLE).is(COLON, IDENTIFIER_NAME, b.optional(COLON, IDENTIFIER_NAME));
        
        b.rule(NULL_STATEMENT).is(NULL, SEMICOLON);
        
        b.rule(EXCEPTION_HANDLER).is(
                WHEN,
                b.firstOf(OTHERS, OBJECT_REFERENCE), 
                b.zeroOrMore(OR, b.firstOf(OTHERS, OBJECT_REFERENCE)), 
                THEN, STATEMENTS);
        
        b.rule(LABEL).is(LLABEL, IDENTIFIER_NAME, RLABEL);
        
        b.rule(STATEMENTS_SECTION).is(
                BEGIN,
                STATEMENTS,
                b.optional(EXCEPTION, b.oneOrMore(EXCEPTION_HANDLER)), 
                END, b.optional(IDENTIFIER_NAME), SEMICOLON);
        
        b.rule(BLOCK_STATEMENT).is(
                b.optional(LABEL),
                b.optional(DECLARE, b.optional(DECLARE_SECTION)),
                STATEMENTS_SECTION);
        
        b.rule(ASSIGNMENT_STATEMENT).is(b.optional(LABEL), OBJECT_REFERENCE, ASSIGNMENT, EXPRESSION, SEMICOLON);
        
        b.rule(ELSIF_CLAUSE).is(ELSIF, EXPRESSION, THEN, STATEMENTS);
        
        b.rule(ELSE_CLAUSE).is(ELSE, STATEMENTS);
        
        b.rule(IF_STATEMENT).is(
                b.optional(LABEL),
                IF, EXPRESSION, THEN,
                STATEMENTS,
                b.zeroOrMore(ELSIF_CLAUSE),
                b.optional(ELSE_CLAUSE),
                END, IF, b.optional(IDENTIFIER_NAME), SEMICOLON);
        
        b.rule(LOOP_STATEMENT).is(b.optional(LABEL), LOOP, STATEMENTS, END, LOOP, b.optional(IDENTIFIER_NAME), SEMICOLON);
        
        b.rule(EXIT_STATEMENT).is(b.optional(LABEL), EXIT, b.optional(IDENTIFIER_NAME), b.optional(WHEN, EXPRESSION), SEMICOLON);
        
        b.rule(CONTINUE_STATEMENT).is(b.optional(LABEL), CONTINUE, b.optional(IDENTIFIER_NAME), b.optional(WHEN, EXPRESSION), SEMICOLON);
        
        b.rule(GOTO_STATEMENT).is(b.optional(LABEL), GOTO, b.optional(IDENTIFIER_NAME), SEMICOLON);
        
        b.rule(FOR_STATEMENT).is(
                b.optional(LABEL), 
                FOR, IDENTIFIER_NAME, IN, b.optional(REVERSE),
                b.firstOf(
                        b.sequence(EXPRESSION, RANGE, EXPRESSION),
                        OBJECT_REFERENCE),
                LOOP,
                STATEMENTS,
                END, LOOP, b.optional(IDENTIFIER_NAME), SEMICOLON);
        
        b.rule(WHILE_STATEMENT).is(
                b.optional(LABEL), 
                WHILE, EXPRESSION, LOOP,
                STATEMENTS,
                END, LOOP, b.optional(IDENTIFIER_NAME), SEMICOLON);
        
        b.rule(FORALL_STATEMENT).is(FORALL, IDENTIFIER_NAME, IN,
                b.firstOf(b.sequence(EXPRESSION, RANGE, EXPRESSION),
                          b.sequence(VALUES, OF, IDENTIFIER_NAME),
                          b.sequence(INDICES, OF, IDENTIFIER_NAME, b.optional(BETWEEN, AND_EXPRESSION))),
                b.optional(SAVE, EXCEPTIONS));
        
        b.rule(RETURN_STATEMENT).is(b.optional(LABEL), RETURN, b.optional(EXPRESSION), SEMICOLON);
        
        b.rule(COMMIT_STATEMENT).is(b.optional(LABEL), COMMIT_EXPRESSION, SEMICOLON);
        
        b.rule(ROLLBACK_STATEMENT).is(b.optional(LABEL), ROLLBACK_EXPRESSION, SEMICOLON);
        
        b.rule(SAVEPOINT_STATEMENT).is(b.optional(LABEL), SAVEPOINT_EXPRESSION, SEMICOLON);
        
        b.rule(RAISE_STATEMENT).is(b.optional(LABEL), RAISE, b.optional(MEMBER_EXPRESSION), SEMICOLON);
        
        b.rule(SELECT_STATEMENT).is(b.optional(LABEL), SELECT_EXPRESSION, SEMICOLON);
        
        b.rule(INSERT_STATEMENT).is(b.optional(LABEL), b.optional(FORALL_STATEMENT), INSERT_EXPRESSION, SEMICOLON);
        
        b.rule(UPDATE_STATEMENT).is(b.optional(LABEL), b.optional(FORALL_STATEMENT),UPDATE_EXPRESSION, SEMICOLON);
        
        b.rule(DELETE_STATEMENT).is(b.optional(LABEL), b.optional(FORALL_STATEMENT), DELETE_EXPRESSION, SEMICOLON);
        
        b.rule(CALL_STATEMENT).is(b.optional(LABEL), OBJECT_REFERENCE, SEMICOLON);
        
        b.rule(UNNAMED_ACTUAL_PAMETER).is(
                b.optional(b.firstOf(b.sequence(IN, b.optional(OUT)), OUT)),
                EXPRESSION);
        
        b.rule(EXECUTE_IMMEDIATE_STATEMENT).is(
                b.optional(LABEL), 
                b.optional(FORALL_STATEMENT),
                EXECUTE, IMMEDIATE, CONCATENATION_EXPRESSION,
                b.optional(b.optional(BULK, COLLECT), INTO, OBJECT_REFERENCE, b.zeroOrMore(COMMA, OBJECT_REFERENCE)),
                b.optional(USING, UNNAMED_ACTUAL_PAMETER, b.zeroOrMore(COMMA, UNNAMED_ACTUAL_PAMETER)),
                SEMICOLON);
        
        b.rule(OPEN_STATEMENT).is(
                b.optional(LABEL), 
                OPEN, MEMBER_EXPRESSION,
                b.optional(ARGUMENTS),
                SEMICOLON);
        
        b.rule(OPEN_FOR_STATEMENT).is(
                b.optional(LABEL), 
                OPEN, MEMBER_EXPRESSION, FOR, EXPRESSION,
                b.optional(USING, UNNAMED_ACTUAL_PAMETER, b.zeroOrMore(COMMA, UNNAMED_ACTUAL_PAMETER)),
                SEMICOLON);
        
        b.rule(FETCH_STATEMENT).is(
                b.optional(LABEL), 
                FETCH, MEMBER_EXPRESSION,
                b.firstOf(
                        b.sequence(INTO, UNNAMED_ACTUAL_PAMETER, b.zeroOrMore(COMMA, UNNAMED_ACTUAL_PAMETER)),
                        b.sequence(
                                BULK, COLLECT, INTO, UNNAMED_ACTUAL_PAMETER, b.zeroOrMore(COMMA, UNNAMED_ACTUAL_PAMETER),
                                b.optional(LIMIT, EXPRESSION))),
                SEMICOLON);
        
        b.rule(CLOSE_STATEMENT).is(b.optional(LABEL), CLOSE, MEMBER_EXPRESSION, SEMICOLON);
        
        b.rule(PIPE_ROW_STATEMENT).is(b.optional(LABEL), PIPE, ROW, LPARENTHESIS, EXPRESSION, RPARENTHESIS, SEMICOLON);
        
        b.rule(CASE_STATEMENT).is(
                b.optional(LABEL), 
                CASE, b.firstOf(b.sequence(EXPRESSION, b.oneOrMore(WHEN, EXPRESSION, THEN, STATEMENTS)),
                                b.oneOrMore(WHEN, BOOLEAN_EXPRESSION, THEN, STATEMENTS)),
                b.optional(ELSE, STATEMENTS),
                END, CASE, b.optional(IDENTIFIER_NAME), SEMICOLON);
        
        b.rule(STATEMENT).is(b.firstOf(NULL_STATEMENT,
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
                                       CASE_STATEMENT));
        
        b.rule(STATEMENTS).is(b.oneOrMore(STATEMENT));
    }
    
    private static void createExpressions(LexerfulGrammarBuilder b) {
        // Reference: http://docs.oracle.com/cd/B28359_01/appdev.111/b28370/expression.htm
        
        b.rule(VARIABLE_NAME).is(b.firstOf(IDENTIFIER_NAME, HOST_AND_INDICATOR_VARIABLE));
        
        b.rule(PRIMARY_EXPRESSION).is(
                b.firstOf(LITERAL, VARIABLE_NAME, SQL, MULTIPLICATION)).skip();
        
        b.rule(BRACKED_EXPRESSION).is(b.firstOf(
                PRIMARY_EXPRESSION,
                b.sequence(LPARENTHESIS, EXPRESSION, RPARENTHESIS))).skipIfOneChild();
        
        b.rule(MULTIPLE_VALUE_EXPRESSION).is(b.firstOf(
                BRACKED_EXPRESSION,
                b.sequence(LPARENTHESIS, EXPRESSION, b.oneOrMore(COMMA, EXPRESSION), RPARENTHESIS))).skipIfOneChild();
        
        b.rule(MEMBER_EXPRESSION).is(
                MULTIPLE_VALUE_EXPRESSION,
                b.zeroOrMore(
                        b.firstOf(DOT, MOD, REMOTE),
                        b.nextNot(ROWTYPE),
                        b.nextNot(TYPE),
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
                                DELETE)
                        )).skipIfOneChild();
        
        b.rule(ARGUMENT).is(b.optional(IDENTIFIER_NAME, ASSOCIATION), b.optional(DISTINCT), EXPRESSION);
        
        b.rule(ARGUMENTS).is(LPARENTHESIS, b.optional(ARGUMENT, b.zeroOrMore(COMMA, ARGUMENT)), RPARENTHESIS);
        
        b.rule(METHOD_CALL).is(MEMBER_EXPRESSION, b.oneOrMore(ARGUMENTS));
        
        b.rule(CALL_EXPRESSION).is(b.firstOf(
                SingleRowSqlFunctionsGrammar.SINGLE_ROW_SQL_FUNCTION,
                AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION,
                METHOD_CALL)).skipIfOneChild();
        
        b.rule(OBJECT_REFERENCE).is(
                b.firstOf(CALL_EXPRESSION, MEMBER_EXPRESSION),
                b.zeroOrMore(DOT, b.firstOf(CALL_EXPRESSION, MEMBER_EXPRESSION)),
                b.optional(LPARENTHESIS, PLUS, RPARENTHESIS)).skipIfOneChild();
        
        b.rule(POSTFIX_EXPRESSION).is(OBJECT_REFERENCE, 
                b.optional(b.firstOf(
                        b.sequence(IS, b.optional(NOT), NULL),
                        ANALYTIC_CLAUSE,
                        b.sequence(KEEP_CLAUSE, b.optional(ANALYTIC_CLAUSE))))).skipIfOneChild();
        
        b.rule(IN_EXPRESSION).is(POSTFIX_EXPRESSION, 
                b.optional(b.sequence(
                        b.optional(NOT), IN, 
                        b.firstOf(
                                b.sequence(LPARENTHESIS, EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION), RPARENTHESIS),
                                EXPRESSION)))).skipIfOneChild();
        
        b.rule(EXISTS_EXPRESSION).is(EXISTS , LPARENTHESIS, EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION), RPARENTHESIS).skipIfOneChild();
        
        b.rule(CASE_EXPRESSION).is(
                CASE, b.firstOf(b.sequence(EXPRESSION, b.oneOrMore(WHEN, EXPRESSION, THEN, EXPRESSION)),
                                b.oneOrMore(WHEN, BOOLEAN_EXPRESSION, THEN, EXPRESSION)),
                b.optional(ELSE, EXPRESSION),
                END);
        
        b.rule(AT_TIME_ZONE_EXPRESSION).is(AT, b.firstOf(LOCAL, b.sequence(TIME, ZONE, EXPRESSION)));
        
        b.rule(UNARY_EXPRESSION).is(b.firstOf(
                        b.sequence(PLUS, UNARY_EXPRESSION),
                        b.sequence(MINUS, UNARY_EXPRESSION),
                        b.sequence(PRIOR, UNARY_EXPRESSION),
                        b.sequence(CONNECT_BY_ROOT, UNARY_EXPRESSION),
                        IN_EXPRESSION,
                        SELECT_EXPRESSION,
                        CASE_EXPRESSION,
                        EXISTS_EXPRESSION),
                b.optional(AT_TIME_ZONE_EXPRESSION)).skipIfOneChild();
        
        b.rule(EXPONENTIATION_EXPRESSION).is(UNARY_EXPRESSION, b.zeroOrMore(EXPONENTIATION, UNARY_EXPRESSION)).skipIfOneChild();
        
        b.rule(MULTIPLICATIVE_EXPRESSION).is(EXPONENTIATION_EXPRESSION, b.zeroOrMore(b.firstOf(MULTIPLICATION, DIVISION, MOD_KEYWORD), EXPONENTIATION_EXPRESSION)).skipIfOneChild();
        
        b.rule(ADDITIVE_EXPRESSION).is(MULTIPLICATIVE_EXPRESSION, b.zeroOrMore(b.firstOf(PLUS, MINUS), MULTIPLICATIVE_EXPRESSION)).skipIfOneChild();
        
        b.rule(CONCATENATION_EXPRESSION).is(ADDITIVE_EXPRESSION, b.zeroOrMore(CONCATENATION, ADDITIVE_EXPRESSION)).skipIfOneChild();
       
        b.rule(COMPARISON_EXPRESSION).is(b.firstOf(
                ConditionsGrammar.CONDITION,
                CONCATENATION_EXPRESSION)).skipIfOneChild();   
        
        b.rule(NOT_EXPRESSION).is(b.optional(NOT), COMPARISON_EXPRESSION).skipIfOneChild();
        b.rule(AND_EXPRESSION).is(NOT_EXPRESSION, b.zeroOrMore(AND, NOT_EXPRESSION)).skipIfOneChild();
        b.rule(OR_EXPRESSION).is(AND_EXPRESSION, b.zeroOrMore(OR, AND_EXPRESSION)).skipIfOneChild();
        
        b.rule(BOOLEAN_EXPRESSION).is(OR_EXPRESSION).skip();
        
        b.rule(EXPRESSION).is(BOOLEAN_EXPRESSION).skipIfOneChild();
    }
    
    private static void createDeclarations(LexerfulGrammarBuilder b) {
        b.rule(DEFAULT_VALUE_ASSIGNMENT).is(b.firstOf(ASSIGNMENT, DEFAULT), EXPRESSION);
        
        b.rule(PARAMETER_DECLARATION).is(
                IDENTIFIER_NAME,
                b.optional(IN),
                b.firstOf(
                        b.sequence(DATATYPE, b.optional(DEFAULT_VALUE_ASSIGNMENT)),
                        b.sequence(OUT, b.optional(NOCOPY), DATATYPE))
                );
        
     // http://docs.oracle.com/cd/B28359_01/appdev.111/b28370/procedure.htm
        b.rule(PROCEDURE_DECLARATION).is(
                PROCEDURE, IDENTIFIER_NAME,
                b.optional(LPARENTHESIS, b.oneOrMore(PARAMETER_DECLARATION, b.optional(COMMA)), RPARENTHESIS),
                b.firstOf(
                        SEMICOLON,
                        b.sequence(b.firstOf(IS, AS), b.optional(DECLARE_SECTION), STATEMENTS_SECTION))
                );
        
        // http://docs.oracle.com/cd/B28359_01/appdev.111/b28370/function.htm
        b.rule(FUNCTION_DECLARATION).is(
                FUNCTION, IDENTIFIER_NAME,
                b.optional(LPARENTHESIS, b.oneOrMore(PARAMETER_DECLARATION, b.optional(COMMA)), RPARENTHESIS),
                RETURN, DATATYPE, b.zeroOrMore(b.firstOf(DETERMINISTIC, PIPELINED)),
                b.firstOf(
                        SEMICOLON,
                        b.sequence(b.firstOf(IS, AS), b.optional(DECLARE_SECTION), STATEMENTS_SECTION))
                );
        
        b.rule(VARIABLE_DECLARATION).is(IDENTIFIER_NAME,
                b.optional(CONSTANT),
                b.firstOf(
                        b.sequence(
                                DATATYPE,
                                b.optional(DEFAULT_VALUE_ASSIGNMENT)),
                        EXCEPTION),                                           
                SEMICOLON);
        
        b.rule(CUSTOM_SUBTYPE).is(
                SUBTYPE, IDENTIFIER_NAME, IS, DATATYPE,
                b.optional(RANGE_KEYWORD, NUMERIC_LITERAL, RANGE, NUMERIC_LITERAL),
                SEMICOLON);
        
        b.rule(CURSOR_PARAMETER_DECLARATION).is(
                IDENTIFIER_NAME,
                b.optional(IN),
                DATATYPE, b.optional(DEFAULT_VALUE_ASSIGNMENT));
        
        b.rule(CURSOR_DECLARATION).is(
                CURSOR, IDENTIFIER_NAME,
                b.optional(LPARENTHESIS, b.oneOrMore(CURSOR_PARAMETER_DECLARATION, b.optional(COMMA)), RPARENTHESIS),
                IS, SELECT_EXPRESSION, SEMICOLON);
        
        b.rule(RECORD_FIELD_DECLARATION).is(
                IDENTIFIER_NAME, DATATYPE,
                b.optional(DEFAULT_VALUE_ASSIGNMENT));
        
        b.rule(RECORD_DECLARATION).is(
                TYPE, IDENTIFIER_NAME, IS, RECORD,
                LPARENTHESIS, RECORD_FIELD_DECLARATION, b.zeroOrMore(COMMA, RECORD_FIELD_DECLARATION), RPARENTHESIS,
                SEMICOLON);
        
        b.rule(TABLE_OF_DECLARATION).is(
                TYPE, IDENTIFIER_NAME, IS, TABLE, OF, DATATYPE,
                b.optional(INDEX, BY, DATATYPE),
                SEMICOLON);
        
        b.rule(VARRAY_DECLARATION).is(
                TYPE, IDENTIFIER_NAME, IS, 
                b.firstOf(VARRAY, b.sequence(b.optional(VARYING), ARRAY)), 
                LPARENTHESIS, INTEGER_LITERAL, RPARENTHESIS, 
                OF, DATATYPE, SEMICOLON);
        
        b.rule(REF_CURSOR_DECLARATION).is(TYPE, IDENTIFIER_NAME, IS, REF, CURSOR, b.optional(RETURN, DATATYPE), SEMICOLON);
        
        b.rule(AUTONOMOUS_TRANSACTION_PRAGMA).is(PRAGMA, AUTONOMOUS_TRANSACTION, SEMICOLON);
        
        b.rule(EXCEPTION_INIT_PRAGMA).is(PRAGMA, EXCEPTION_INIT, LPARENTHESIS, EXPRESSION, COMMA, EXPRESSION, RPARENTHESIS, SEMICOLON);
        
        b.rule(SERIALLY_REUSABLE_PRAGMA).is(PRAGMA, SERIALLY_REUSABLE, SEMICOLON);
        
        b.rule(INTERFACE_PRAGMA).is(
                PRAGMA, INTERFACE, 
                LPARENTHESIS, IDENTIFIER_NAME, COMMA, IDENTIFIER_NAME, COMMA, INTEGER_LITERAL, RPARENTHESIS, SEMICOLON);
        
        b.rule(PRAGMA_DECLARATION).is(b.firstOf(
                EXCEPTION_INIT_PRAGMA,
                AUTONOMOUS_TRANSACTION_PRAGMA,
                SERIALLY_REUSABLE_PRAGMA,
                INTERFACE_PRAGMA));
        
        b.rule(DECLARE_SECTION).is(b.oneOrMore(b.firstOf(
                PRAGMA_DECLARATION,
                VARIABLE_DECLARATION,
                PROCEDURE_DECLARATION,
                FUNCTION_DECLARATION,
                CUSTOM_SUBTYPE,
                CURSOR_DECLARATION,
                RECORD_DECLARATION,
                TABLE_OF_DECLARATION,
                VARRAY_DECLARATION,
                REF_CURSOR_DECLARATION)));
    }
    
    private static void createTrigger(LexerfulGrammarBuilder b) {
        b.rule(CREATE_TRIGGER).is(
                CREATE, b.optional(OR, REPLACE),
                b.optional(b.firstOf(EDITIONABLE, NONEDITIONABLE)),
                TRIGGER, UNIT_NAME,
                //b.firstOf(SIMPLE_DML_TRIGGER, INSTEAD_OF_DML_TRIGGER, COMPOUND_DML_TRIGGER, SYSTEM_TRIGGER)
                SIMPLE_DML_TRIGGER);
        
        b.rule(SIMPLE_DML_TRIGGER).is(
                b.firstOf(BEFORE, AFTER),
                DML_EVENT_CLAUSE, b.zeroOrMore(OR, DML_EVENT_CLAUSE), ON, UNIT_NAME,
                b.optional(REFERENCING_CLAUSE),
                b.optional(FOR, EACH, ROW),
                b.optional(TRIGGER_EDITION_CLAUSE),
                b.optional(TRIGGER_ORDERING_CLAUSE),
                b.optional(b.firstOf(ENABLE, DISABLE)),
                b.optional(WHEN, LPARENTHESIS, EXPRESSION, RPARENTHESIS),
                b.optional(DECLARE, b.optional(DECLARE_SECTION)), STATEMENTS_SECTION
                );
        
        b.rule(DML_EVENT_CLAUSE).is(
                b.firstOf(
                        DELETE,
                        INSERT,
                        UPDATE), 
                b.optional(OF, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME))
                );
        
        b.rule(REFERENCING_CLAUSE).is(
                REFERENCING,
                b.oneOrMore(
                        b.firstOf(
                                b.sequence(OLD, b.optional(AS), IDENTIFIER_NAME),
                                b.sequence(NEW, b.optional(AS), IDENTIFIER_NAME),
                                b.sequence(PARENT, b.optional(AS), IDENTIFIER_NAME)
                        )));
        
        b.rule(TRIGGER_EDITION_CLAUSE).is(
                b.optional(b.firstOf(FORWARD, REVERSE)),
                CROSSEDITION);
        
        b.rule(TRIGGER_ORDERING_CLAUSE).is(
                b.firstOf(FOLLOWS, PRECEDES),
                UNIT_NAME, b.zeroOrMore(COMMA, UNIT_NAME));
    }
    
    private static void createProgramUnits(LexerfulGrammarBuilder b) {
        b.rule(EXECUTE_PLSQL_BUFFER).is(DIVISION);
        
        b.rule(UNIT_NAME).is(b.optional(IDENTIFIER_NAME, DOT), IDENTIFIER_NAME);
        
        // http://docs.oracle.com/cd/B28359_01/appdev.111/b28370/create_procedure.htm
        b.rule(CREATE_PROCEDURE).is(
                CREATE, b.optional(OR, REPLACE),
                PROCEDURE, UNIT_NAME, b.optional(TIMESTAMP, STRING_LITERAL),
                b.optional(LPARENTHESIS, b.oneOrMore(PARAMETER_DECLARATION, b.optional(COMMA)), RPARENTHESIS),
                b.optional(AUTHID, b.firstOf(CURRENT_USER, DEFINER)),
                b.firstOf(IS, AS),
                b.firstOf(
                        b.sequence(b.optional(DECLARE_SECTION), STATEMENTS_SECTION),
                        b.sequence(LANGUAGE, JAVA, STRING_LITERAL, SEMICOLON),
                        b.sequence(EXTERNAL, SEMICOLON))
                );
        
        // http://docs.oracle.com/cd/B28359_01/appdev.111/b28370/create_function.htm
        b.rule(CREATE_FUNCTION).is(
                CREATE, b.optional(OR, REPLACE),
                FUNCTION, UNIT_NAME, b.optional(TIMESTAMP, STRING_LITERAL),
                b.optional(LPARENTHESIS, b.oneOrMore(PARAMETER_DECLARATION, b.optional(COMMA)), RPARENTHESIS),
                RETURN, DATATYPE, b.zeroOrMore(b.firstOf(DETERMINISTIC, PIPELINED, PARALLEL_ENABLE, RESULT_CACHE)),
                b.optional(AUTHID, b.firstOf(CURRENT_USER, DEFINER)),
                b.firstOf(IS, AS),
                b.firstOf(
                        b.sequence(b.optional(DECLARE_SECTION), STATEMENTS_SECTION),
                        b.sequence(LANGUAGE, JAVA, STRING_LITERAL, SEMICOLON))
                );
        
        // http://docs.oracle.com/cd/B28359_01/appdev.111/b28370/create_package.htm
        b.rule(CREATE_PACKAGE).is(
                CREATE, b.optional(OR, REPLACE),
                PACKAGE, UNIT_NAME, b.optional(TIMESTAMP, STRING_LITERAL),
                b.optional(AUTHID, b.firstOf(CURRENT_USER, DEFINER)),
                b.firstOf(IS, AS),
                b.optional(DECLARE_SECTION),
                END, b.optional(IDENTIFIER_NAME), SEMICOLON);
        
        // http://docs.oracle.com/cd/B28359_01/appdev.111/b28370/create_package_body.htm
        b.rule(CREATE_PACKAGE_BODY).is(
                CREATE, b.optional(OR, REPLACE),
                PACKAGE, BODY, UNIT_NAME, b.optional(TIMESTAMP, STRING_LITERAL),
                b.firstOf(IS, AS),
                b.optional(DECLARE_SECTION),
                b.firstOf(
                        STATEMENTS_SECTION,
                        b.sequence(END, b.optional(IDENTIFIER_NAME), SEMICOLON)));
        
        b.rule(VIEW_RESTRICTION_CLAUSE).is(
                WITH, b.firstOf(
                        b.sequence(READ, ONLY),
                        b.sequence(CHECK, OPTION, b.optional(CONSTRAINT, IDENTIFIER_NAME))
                        )
                );
                
        // http://docs.oracle.com/cd/B28359_01/server.111/b28286/statements_8004.htm
        b.rule(CREATE_VIEW).is(
                CREATE, b.optional(OR, REPLACE), b.optional(b.optional(NO), FORCE),
                VIEW, UNIT_NAME,
                b.optional(LPARENTHESIS, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS),
                AS,
                SELECT_EXPRESSION,
                b.optional(VIEW_RESTRICTION_CLAUSE),
                b.optional(SEMICOLON));
        
        b.rule(ANONYMOUS_BLOCK).is(BLOCK_STATEMENT);
        
        b.rule(COMPILATION_UNIT).is(b.firstOf(
                ANONYMOUS_BLOCK,
                CREATE_PROCEDURE, 
                CREATE_FUNCTION, 
                CREATE_PACKAGE,
                CREATE_PACKAGE_BODY,
                CREATE_VIEW,
                CREATE_TRIGGER));
    }
}
