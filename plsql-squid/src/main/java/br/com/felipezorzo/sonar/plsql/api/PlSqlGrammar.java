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
    
    /* Literals */
    LITERAL,
    BOOLEAN_LITERAL,
    NULL_LITERAL,
    NUMERIC_LITERAL,
    
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
        return b;
    }
    
    private static void createLiterals(LexerfulGrammarBuilder b) {
        b.rule(NULL_LITERAL).is(NULL);
        b.rule(BOOLEAN_LITERAL).is(b.firstOf(TRUE, FALSE));
        b.rule(NUMERIC_LITERAL).is(b.firstOf(INTEGER_LITERAL, REAL_LITERAL, SCIENTIFIC_LITERAL));
        
        b.rule(LITERAL).is(b.firstOf(NULL_LITERAL, BOOLEAN_LITERAL));
    }
    
    private static void createDatatypes(LexerfulGrammarBuilder b) {
        b.rule(NUMERIC_DATATYPE).is(NUMBER, b.optional(LPARENTHESIS, INTEGER_LITERAL, b.optional(COMMA, INTEGER_LITERAL), RPARENTHESIS));
        b.rule(LOB_DATATYPE).is(b.firstOf(BFILE, BLOB, CLOB, NCLOB));
        
        b.rule(DATATYPE).is(b.firstOf(NUMERIC_DATATYPE, LOB_DATATYPE));
    }

    private static void createStatements(LexerfulGrammarBuilder b) {
        b.rule(VARIABLE_DECLARATION).is(IDENTIFIER_NAME,
                                          b.optional(CONSTANT),
                                          DATATYPE,
                                          b.optional(b.optional(NOT, NULL), b.firstOf(ASSIGNMENT, DEFAULT), NUMERIC_LITERAL),
                                          SEMICOLON);
        b.rule(NULL_STATEMENT).is(NULL, SEMICOLON);
        b.rule(EXCEPTION_HANDLER).is(WHEN, b.firstOf(OTHERS, IDENTIFIER_NAME), THEN, b.oneOrMore(STATEMENT));
        b.rule(BLOCK_STATEMENT).is(BEGIN, b.oneOrMore(STATEMENT), b.optional(EXCEPTION, b.oneOrMore(EXCEPTION_HANDLER)), END, SEMICOLON);
        b.rule(STATEMENT).is(b.firstOf(NULL_STATEMENT, BLOCK_STATEMENT));
    }
}
