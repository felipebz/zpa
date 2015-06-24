package br.com.felipezorzo.sonar.plsql.api;

import static br.com.felipezorzo.sonar.plsql.api.PlSqlKeyword.*;
import static br.com.felipezorzo.sonar.plsql.api.PlSqlPunctuator.*;
import static br.com.felipezorzo.sonar.plsql.api.PlSqlTokenType.*;
import static com.sonar.sslr.api.GenericTokenType.IDENTIFIER;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

public enum PlSqlGrammar implements GrammarRuleKey {
    
    /* Data types */
    DATATYPE,
    NUMERIC_DATATYPE,
    LOB_DATATYPE,
    CHARACTER_DATAYPE,
    BOOLEAN_DATATYPE,
    DATE_DATATYPE,
    
    /* Literals */
    LITERAL,
    BOOLEAN_LITERAL,
    NULL_LITERAL,
    NUMERIC_LITERAL,
    CHARACTER_LITERAL,
    
    /* Expressions */
    CHARACTER_EXPRESSION,
    BOOLEAN_EXPRESSION,
    
    BLOCK_STATEMENT,
    EXCEPTION_HANDLER,
    IDENTIFIER_NAME,
    NULL_STATEMENT,
    STATEMENT,
    VARIABLE_DECLARATION;

    public static LexerfulGrammarBuilder create() {
        LexerfulGrammarBuilder b = LexerfulGrammarBuilder.create();

        b.rule(IDENTIFIER_NAME).is(IDENTIFIER);

        createLiterals(b);
        createDatatypes(b);
        createStatements(b);
        createExpressions(b);
        return b;
    }
    
    private static void createLiterals(LexerfulGrammarBuilder b) {
        b.rule(NULL_LITERAL).is(NULL);
        b.rule(BOOLEAN_LITERAL).is(b.firstOf(TRUE, FALSE));
        b.rule(NUMERIC_LITERAL).is(b.firstOf(INTEGER_LITERAL, REAL_LITERAL, SCIENTIFIC_LITERAL));
        b.rule(CHARACTER_LITERAL).is(STRING_LITERAL);
        
        b.rule(LITERAL).is(b.firstOf(NULL_LITERAL, BOOLEAN_LITERAL, NUMERIC_LITERAL, CHARACTER_LITERAL));
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
        
        b.rule(LOB_DATATYPE).is(b.firstOf(BFILE, BLOB, CLOB, NCLOB));
        
        b.rule(CHARACTER_DATAYPE).is(
                b.firstOf(
                        CHAR,
                        CHARACTER,
                        LONG,
                        b.sequence(LONG, RAW),
                        NCHAR,
                        NVARCHAR2,
                        RAW,
                        ROWID,
                        STRING,
                        UROWID,
                        VARCHAR,
                        VARCHAR2), 
                b.optional(LPARENTHESIS, INTEGER_LITERAL, RPARENTHESIS));
        
        b.rule(BOOLEAN_DATATYPE).is(BOOLEAN);
        
        b.rule(DATE_DATATYPE).is(DATE);
        
        b.rule(DATATYPE).is(b.firstOf(NUMERIC_DATATYPE, LOB_DATATYPE, CHARACTER_DATAYPE, BOOLEAN_DATATYPE, DATE_DATATYPE));
    }

    private static void createStatements(LexerfulGrammarBuilder b) {
        b.rule(VARIABLE_DECLARATION).is(IDENTIFIER_NAME,
                                          b.optional(CONSTANT),
                                          DATATYPE,
                                          b.optional(b.optional(NOT, NULL), b.firstOf(ASSIGNMENT, DEFAULT), LITERAL),
                                          SEMICOLON);
        b.rule(NULL_STATEMENT).is(NULL, SEMICOLON);
        b.rule(EXCEPTION_HANDLER).is(WHEN, b.firstOf(OTHERS, IDENTIFIER_NAME), THEN, b.oneOrMore(STATEMENT));
        b.rule(BLOCK_STATEMENT).is(BEGIN, b.oneOrMore(STATEMENT), b.optional(EXCEPTION, b.oneOrMore(EXCEPTION_HANDLER)), END, SEMICOLON);
        b.rule(STATEMENT).is(b.firstOf(NULL_STATEMENT, BLOCK_STATEMENT));
    }
    
    private static void createExpressions(LexerfulGrammarBuilder b) {
        // Reference: http://docs.oracle.com/cd/B28359_01/appdev.111/b28370/expression.htm
        b.rule(CHARACTER_EXPRESSION).is(b.firstOf(STRING_LITERAL, IDENTIFIER_NAME), b.optional(CONCATENATION, CHARACTER_EXPRESSION));
        
        b.rule(BOOLEAN_EXPRESSION).is(b.optional(NOT), BOOLEAN_LITERAL, b.optional(b.firstOf(AND, OR), BOOLEAN_EXPRESSION));
    }
}
