package br.com.felipezorzo.sonar.plsql.api;

import static br.com.felipezorzo.sonar.plsql.api.PlSqlKeyword.*;
import static br.com.felipezorzo.sonar.plsql.api.PlSqlPunctuator.*;
import static br.com.felipezorzo.sonar.plsql.api.PlSqlTokenType.*;
import static com.sonar.sslr.api.GenericTokenType.EOF;
import static com.sonar.sslr.api.GenericTokenType.IDENTIFIER;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

public enum PlSqlGrammar implements GrammarRuleKey {
    
    // Data types
    DATATYPE,
    NUMERIC_DATATYPE,
    LOB_DATATYPE,
    CHARACTER_DATAYPE,
    BOOLEAN_DATATYPE,
    DATE_DATATYPE,
    CUSTOM_SUBTYPE,
    
    // Literals
    LITERAL,
    BOOLEAN_LITERAL,
    NULL_LITERAL,
    NUMERIC_LITERAL,
    CHARACTER_LITERAL,
    
    // Expressions
    EXPRESSION,
    BOOLEAN_EXPRESSION,
    PRIMARY_EXPRESSION, 
    BRACKED_EXPRESSION, 
    MEMBER_EXPRESSION, 
    OBJECT_REFERENCE, 
    POSTFIX_EXPRESSION, 
    IN_EXPRESSION, 
    UNARY_EXPRESSION, 
    MULTIPLICATIVE_EXPRESSION, 
    ADDITIVE_EXPRESSION, 
    CONCATENATION_EXPRESSION, 
    COMPARISION_EXPRESSION, 
    EXPONENTIATION_EXPRESSION, 
    ARGUMENT, 
    ARGUMENTS, 
    CALL_EXPRESSION,
    
    // DML
    SELECT_COLUMN,
    FROM_CLAUSE,
    WHERE_CLAUSE,
    SELECT_EXPRESSION,
    
    // Statements
    BLOCK_STATEMENT,
    NULL_STATEMENT,
    ASSIGNMENT_STATEMENT,
    IF_STATEMENT,
    LOOP_STATEMENT,
    EXIT_STATEMENT,
    CONTINUE_STATEMENT,
    FOR_STATEMENT,
    WHILE_STATEMENT,
    RETURN_STATEMENT,
    COMMIT_STATEMENT,
    ROLLBACK_STATEMENT,
    SAVEPOINT_STATEMENT,
    RAISE_STATEMENT,
    STATEMENT,
    
    // Declarations
    VARIABLE_DECLARATION,
    PARAMETER_DECLARATION,
    TYPE_ATTRIBUTE_DECLARATION,
    HOST_AND_INDICATOR_VARIABLE,
    
    DECLARE_SECTION,
    EXCEPTION_HANDLER,
    IDENTIFIER_NAME,
    EXECUTE_PLSQL_BUFFER,
    BUILTIN_FUNCTIONS,
    
    // Program units
    ANONYMOUS_BLOCK,
    PROCEDURE_DECLARATION,
    FUNCTION_DECLARATION,
    CREATE_PROCEDURE,
    CREATE_FUNCTION,
    CREATE_PACKAGE,
    CREATE_PACKAGE_BODY,
    
    // Top-level components
    FILE_INPUT;

    public static LexerfulGrammarBuilder create() {
        LexerfulGrammarBuilder b = LexerfulGrammarBuilder.create();

        b.rule(IDENTIFIER_NAME).is(IDENTIFIER);
        b.rule(BUILTIN_FUNCTIONS).is(REPLACE);
        b.rule(FILE_INPUT).is(b.oneOrMore(b.firstOf(ANONYMOUS_BLOCK, CREATE_PROCEDURE, CREATE_FUNCTION, CREATE_PACKAGE, EXECUTE_PLSQL_BUFFER)), EOF);

        createLiterals(b);
        createDatatypes(b);
        createStatements(b);
        createDmlStatements(b);
        createExpressions(b);
        createDeclarations(b);
        createProgramUnits(b);
        
        b.setRootRule(FILE_INPUT);
        b.buildWithMemoizationOfMatchesForAllRules();
        
        return b;
    }
    
    private static void createLiterals(LexerfulGrammarBuilder b) {
        b.rule(NULL_LITERAL).is(NULL);
        b.rule(BOOLEAN_LITERAL).is(b.firstOf(TRUE, FALSE));
        b.rule(NUMERIC_LITERAL).is(b.firstOf(INTEGER_LITERAL, REAL_LITERAL, SCIENTIFIC_LITERAL));
        b.rule(CHARACTER_LITERAL).is(STRING_LITERAL);
        
        b.rule(LITERAL).is(b.firstOf(NULL_LITERAL, BOOLEAN_LITERAL, NUMERIC_LITERAL, CHARACTER_LITERAL, DATE_LITERAL));
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
        
        b.rule(DATATYPE).is(b.firstOf(NUMERIC_DATATYPE, LOB_DATATYPE, CHARACTER_DATAYPE, BOOLEAN_DATATYPE, DATE_DATATYPE, TYPE_ATTRIBUTE_DECLARATION));
    }

    private static void createStatements(LexerfulGrammarBuilder b) {
        b.rule(HOST_AND_INDICATOR_VARIABLE).is(COLON, IDENTIFIER_NAME, b.optional(COLON, IDENTIFIER_NAME));
        
        b.rule(TYPE_ATTRIBUTE_DECLARATION).is(
                IDENTIFIER_NAME,
                b.optional(
                        DOT, IDENTIFIER_NAME, 
                        b.optional(DOT, IDENTIFIER_NAME)),
                MOD,
                TYPE);
        
        b.rule(VARIABLE_DECLARATION).is(IDENTIFIER_NAME,
                                        b.optional(CONSTANT),
                                        b.firstOf(
                                                b.sequence(
                                                        DATATYPE,
                                                        b.optional(b.optional(NOT, NULL), b.firstOf(ASSIGNMENT, DEFAULT), LITERAL)),
                                                EXCEPTION),                                           
                                        SEMICOLON);
        
        b.rule(CUSTOM_SUBTYPE).is(
                SUBTYPE, IDENTIFIER_NAME, IS, DATATYPE,
                b.optional(NOT, NULL),
                b.optional(RANGE_KEYWORD, NUMERIC_LITERAL, RANGE, NUMERIC_LITERAL),
                SEMICOLON);
        
        b.rule(NULL_STATEMENT).is(NULL, SEMICOLON);
        
        b.rule(EXCEPTION_HANDLER).is(WHEN, b.firstOf(OTHERS, IDENTIFIER_NAME), THEN, b.oneOrMore(STATEMENT));
        
        b.rule(BLOCK_STATEMENT).is(
                BEGIN,
                b.oneOrMore(STATEMENT),
                b.optional(EXCEPTION, b.oneOrMore(EXCEPTION_HANDLER)), 
                END, b.optional(IDENTIFIER_NAME), SEMICOLON);
        
        b.rule(ASSIGNMENT_STATEMENT).is(
                b.firstOf(b.sequence(IDENTIFIER_NAME,
                                     b.optional(b.firstOf(b.sequence(DOT, IDENTIFIER_NAME),
                                                          b.sequence(LPARENTHESIS, EXPRESSION, RPARENTHESIS)))),
                          HOST_AND_INDICATOR_VARIABLE),
                ASSIGNMENT,
                EXPRESSION,
                SEMICOLON);
        
        b.rule(IF_STATEMENT).is(
                IF, EXPRESSION, THEN,
                b.oneOrMore(STATEMENT),
                b.zeroOrMore(ELSIF, EXPRESSION, THEN, b.oneOrMore(STATEMENT)),
                b.optional(ELSE, b.oneOrMore(STATEMENT)),
                END, IF, SEMICOLON);
        
        b.rule(LOOP_STATEMENT).is(LOOP, b.oneOrMore(STATEMENT), END, LOOP, SEMICOLON);
        
        b.rule(EXIT_STATEMENT).is(EXIT, b.optional(WHEN, EXPRESSION), SEMICOLON);
        
        b.rule(CONTINUE_STATEMENT).is(CONTINUE, b.optional(WHEN, EXPRESSION), SEMICOLON);
        
        b.rule(FOR_STATEMENT).is(
                FOR, IDENTIFIER_NAME, IN, b.optional(REVERSE), EXPRESSION, RANGE, EXPRESSION, LOOP,
                b.oneOrMore(STATEMENT),
                END, LOOP, SEMICOLON);
        
        b.rule(WHILE_STATEMENT).is(
                WHILE, EXPRESSION, LOOP,
                b.oneOrMore(STATEMENT),
                END, LOOP, SEMICOLON);
        
        b.rule(RETURN_STATEMENT).is(RETURN, b.optional(EXPRESSION), SEMICOLON);
        
        b.rule(COMMIT_STATEMENT).is(
                COMMIT,
                b.optional(WORK),
                b.optional(b.firstOf(
                        b.sequence(FORCE, STRING_LITERAL, b.optional(COMMA, INTEGER_LITERAL)),
                        b.sequence(
                                b.optional(COMMENT, STRING_LITERAL),
                                b.optional(WRITE, b.optional(b.firstOf(IMMEDIATE, BATCH)), b.optional(b.firstOf(WAIT, NOWAIT)))))),
                SEMICOLON);
        
        b.rule(ROLLBACK_STATEMENT).is(
                ROLLBACK,
                b.optional(WORK),
                b.optional(b.firstOf(
                        b.sequence(FORCE, STRING_LITERAL),
                        b.sequence(TO, b.optional(SAVEPOINT), IDENTIFIER_NAME))),
                SEMICOLON);
        
        b.rule(SAVEPOINT_STATEMENT).is(SAVEPOINT, IDENTIFIER_NAME, SEMICOLON);
        
        b.rule(RAISE_STATEMENT).is(RAISE, b.optional(IDENTIFIER_NAME), SEMICOLON);
        
        b.rule(STATEMENT).is(b.firstOf(NULL_STATEMENT,
                                       BLOCK_STATEMENT,
                                       ASSIGNMENT_STATEMENT, 
                                       IF_STATEMENT, 
                                       LOOP_STATEMENT, 
                                       EXIT_STATEMENT, 
                                       CONTINUE_STATEMENT,
                                       FOR_STATEMENT,
                                       WHILE_STATEMENT,
                                       RETURN_STATEMENT,
                                       COMMIT_STATEMENT,
                                       ROLLBACK_STATEMENT,
                                       RAISE_STATEMENT));
    }
    
    private static void createDmlStatements(LexerfulGrammarBuilder b) {
        b.rule(SELECT_COLUMN).is(EXPRESSION, b.optional(b.optional(AS), IDENTIFIER_NAME));
        
        b.rule(FROM_CLAUSE).is(
                IDENTIFIER_NAME,
                b.optional(DOT, IDENTIFIER_NAME),
                b.optional(REMOTE, IDENTIFIER_NAME),
                b.optional(IDENTIFIER_NAME));
        
        b.rule(WHERE_CLAUSE).is(WHERE, EXPRESSION);
        
        b.rule(SELECT_EXPRESSION).is(
                SELECT, SELECT_COLUMN, b.zeroOrMore(COMMA, SELECT_COLUMN),
                FROM, FROM_CLAUSE, b.zeroOrMore(COMMA, FROM_CLAUSE),
                b.optional(WHERE_CLAUSE));
    }
    
    private static void createExpressions(LexerfulGrammarBuilder b) {
        // Reference: http://docs.oracle.com/cd/B28359_01/appdev.111/b28370/expression.htm
        
        b.rule(PRIMARY_EXPRESSION).is(
                b.firstOf(IDENTIFIER_NAME, HOST_AND_INDICATOR_VARIABLE, LITERAL, SQL, BUILTIN_FUNCTIONS, MULTIPLICATION),
                b.nextNot(ASSIGNMENT_STATEMENT));
        
        b.rule(BRACKED_EXPRESSION).is(b.firstOf(PRIMARY_EXPRESSION, b.sequence(LPARENTHESIS, EXPRESSION, RPARENTHESIS))).skipIfOneChild();
        
        b.rule(MEMBER_EXPRESSION).is(
                BRACKED_EXPRESSION,
                b.zeroOrMore(
                        b.firstOf(DOT, MOD), 
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
                                ISOPEN)
                        )).skipIfOneChild();
        
        b.rule(ARGUMENT).is(b.optional(IDENTIFIER_NAME, ASSOCIATION), EXPRESSION);
        
        b.rule(ARGUMENTS).is(LPARENTHESIS, b.optional(ARGUMENT, b.zeroOrMore(COMMA, ARGUMENT)), RPARENTHESIS);
        
        b.rule(CALL_EXPRESSION).is(MEMBER_EXPRESSION, ARGUMENTS);
        
        b.rule(OBJECT_REFERENCE).is(
                b.firstOf(CALL_EXPRESSION, MEMBER_EXPRESSION),
                b.zeroOrMore(DOT, b.firstOf(CALL_EXPRESSION, MEMBER_EXPRESSION))).skipIfOneChild();
        
        b.rule(POSTFIX_EXPRESSION).is(OBJECT_REFERENCE, b.optional(IS, b.optional(NOT), NULL));
        
        b.rule(IN_EXPRESSION).is(POSTFIX_EXPRESSION, b.optional(b.sequence(b.optional(NOT), IN , LPARENTHESIS, EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION), RPARENTHESIS))).skipIfOneChild();
        
        b.rule(UNARY_EXPRESSION).is(b.firstOf(
                        b.sequence(NOT, UNARY_EXPRESSION),
                        IN_EXPRESSION)).skipIfOneChild();
        
        b.rule(EXPONENTIATION_EXPRESSION).is(UNARY_EXPRESSION, b.zeroOrMore(EXPONENTIATION, UNARY_EXPRESSION)).skipIfOneChild();
        
        b.rule(MULTIPLICATIVE_EXPRESSION).is(EXPONENTIATION_EXPRESSION, b.zeroOrMore(b.firstOf(MULTIPLICATION, DIVISION), UNARY_EXPRESSION)).skipIfOneChild();
        
        b.rule(ADDITIVE_EXPRESSION).is(MULTIPLICATIVE_EXPRESSION, b.zeroOrMore(b.firstOf(PLUS, MINUS), MULTIPLICATIVE_EXPRESSION)).skipIfOneChild();
        
        b.rule(CONCATENATION_EXPRESSION).is(ADDITIVE_EXPRESSION, b.zeroOrMore(CONCATENATION, ADDITIVE_EXPRESSION)).skipIfOneChild();
       
        b.rule(COMPARISION_EXPRESSION).is(CONCATENATION_EXPRESSION, 
                b.zeroOrMore(b.firstOf(
                        b.sequence(
                                b.firstOf(
                                    EQUALS,
                                    NOTEQUALS, 
                                    NOTEQUALS2, 
                                    NOTEQUALS3, 
                                    NOTEQUALS4, 
                                    LESSTHAN, 
                                    GREATERTHAN, 
                                    LESSTHANOREQUAL, 
                                    GREATERTHANOREQUAL, 
                                    b.sequence(b.optional(NOT), LIKE)),
                                CONCATENATION_EXPRESSION),
                        b.sequence(
                                b.optional(NOT),
                                BETWEEN, 
                                CONCATENATION_EXPRESSION, 
                                AND, 
                                CONCATENATION_EXPRESSION)))).skipIfOneChild();   
        b.rule(BOOLEAN_EXPRESSION).is(COMPARISION_EXPRESSION, b.zeroOrMore(b.firstOf(AND, OR), COMPARISION_EXPRESSION)).skipIfOneChild();
        
        b.rule(EXPRESSION).is(BOOLEAN_EXPRESSION);
    }
    
    private static void createDeclarations(LexerfulGrammarBuilder b) {
        b.rule(PARAMETER_DECLARATION).is(
                IDENTIFIER_NAME,
                b.optional(IN),
                b.firstOf(
                        b.sequence(DATATYPE, b.optional(b.firstOf(ASSIGNMENT, DEFAULT), EXPRESSION)),
                        b.sequence(OUT, b.optional(NOCOPY), DATATYPE))
                );
    }
    
    private static void createProgramUnits(LexerfulGrammarBuilder b) {
        b.rule(EXECUTE_PLSQL_BUFFER).is(DIVISION);
        
        b.rule(DECLARE_SECTION).is(b.oneOrMore(b.firstOf(VARIABLE_DECLARATION, PROCEDURE_DECLARATION, FUNCTION_DECLARATION, CUSTOM_SUBTYPE)));
        
        // http://docs.oracle.com/cd/B28359_01/appdev.111/b28370/procedure.htm
        b.rule(PROCEDURE_DECLARATION).is(
                PROCEDURE, IDENTIFIER_NAME,
                b.optional(LPARENTHESIS, b.oneOrMore(PARAMETER_DECLARATION, b.optional(COMMA)), RPARENTHESIS),
                b.firstOf(
                        SEMICOLON,
                        b.sequence(b.firstOf(IS, AS), b.zeroOrMore(DECLARE_SECTION), BLOCK_STATEMENT))
                );
        
        // http://docs.oracle.com/cd/B28359_01/appdev.111/b28370/function.htm
        b.rule(FUNCTION_DECLARATION).is(
                FUNCTION, IDENTIFIER_NAME,
                b.optional(LPARENTHESIS, b.oneOrMore(PARAMETER_DECLARATION, b.optional(COMMA)), RPARENTHESIS),
                RETURN, DATATYPE,
                b.firstOf(
                        SEMICOLON,
                        b.sequence(b.firstOf(IS, AS), b.zeroOrMore(DECLARE_SECTION), BLOCK_STATEMENT))
                );
        
        // http://docs.oracle.com/cd/B28359_01/appdev.111/b28370/create_procedure.htm
        b.rule(CREATE_PROCEDURE).is(
                CREATE, b.optional(OR, REPLACE),
                PROCEDURE, b.optional(IDENTIFIER_NAME, DOT), IDENTIFIER_NAME,
                b.optional(LPARENTHESIS, b.oneOrMore(PARAMETER_DECLARATION, b.optional(COMMA)), RPARENTHESIS),
                b.optional(AUTHID, b.firstOf(CURRENT_USER, DEFINER)),
                b.firstOf(IS, AS),
                b.firstOf(
                        b.sequence(b.zeroOrMore(DECLARE_SECTION), BLOCK_STATEMENT),
                        b.sequence(LANGUAGE, JAVA, STRING_LITERAL, SEMICOLON),
                        b.sequence(EXTERNAL, SEMICOLON))
                );
        
        // http://docs.oracle.com/cd/B28359_01/appdev.111/b28370/create_function.htm
        b.rule(CREATE_FUNCTION).is(
                CREATE, b.optional(OR, REPLACE),
                FUNCTION, b.optional(IDENTIFIER_NAME, DOT), IDENTIFIER_NAME,
                b.optional(LPARENTHESIS, b.oneOrMore(PARAMETER_DECLARATION, b.optional(COMMA)), RPARENTHESIS),
                RETURN, DATATYPE,
                b.optional(AUTHID, b.firstOf(CURRENT_USER, DEFINER)),
                b.firstOf(IS, AS),
                b.firstOf(
                        b.sequence(b.zeroOrMore(DECLARE_SECTION), BLOCK_STATEMENT),
                        b.sequence(LANGUAGE, JAVA, STRING_LITERAL, SEMICOLON))
                );
        
        // http://docs.oracle.com/cd/B28359_01/appdev.111/b28370/create_package.htm
        b.rule(CREATE_PACKAGE).is(
                CREATE, b.optional(OR, REPLACE),
                PACKAGE, b.optional(IDENTIFIER_NAME, DOT), IDENTIFIER_NAME,
                b.optional(AUTHID, b.firstOf(CURRENT_USER, DEFINER)),
                b.firstOf(IS, AS),
                b.zeroOrMore(DECLARE_SECTION),
                END, b.optional(IDENTIFIER_NAME), SEMICOLON);
        
        // http://docs.oracle.com/cd/B28359_01/appdev.111/b28370/create_package_body.htm
        b.rule(CREATE_PACKAGE_BODY).is(
                CREATE, b.optional(OR, REPLACE),
                PACKAGE, BODY, b.optional(IDENTIFIER_NAME, DOT), IDENTIFIER_NAME,
                b.firstOf(IS, AS),
                b.zeroOrMore(DECLARE_SECTION),
                b.firstOf(
                        BLOCK_STATEMENT,
                        b.sequence(END, b.optional(IDENTIFIER_NAME), SEMICOLON)));
        
        b.rule(ANONYMOUS_BLOCK).is(
                b.optional(DECLARE, DECLARE_SECTION),
                BLOCK_STATEMENT,
                EXECUTE_PLSQL_BUFFER
                );
    }
}
