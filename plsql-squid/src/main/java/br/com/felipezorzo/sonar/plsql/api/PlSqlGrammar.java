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
    ANCHORED_DATATYPE,
    CUSTOM_DATATYPE,
    REF_DATATYPE,
    
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
    EXISTS_EXPRESSION,
    UNARY_EXPRESSION, 
    MULTIPLICATIVE_EXPRESSION, 
    ADDITIVE_EXPRESSION, 
    CONCATENATION_EXPRESSION, 
    COMPARISION_EXPRESSION, 
    EXPONENTIATION_EXPRESSION, 
    ARGUMENT, 
    ARGUMENTS, 
    CALL_EXPRESSION,
    CASE_EXPRESSION,
    
    // DML
    SELECT_COLUMN,
    FROM_CLAUSE,
    WHERE_CLAUSE,
    INTO_CLAUSE,
    GROUP_BY_CLAUSE,
    ORDER_BY_CLAUSE,
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
    SELECT_STATEMENT,
    INSERT_STATEMENT,
    UPDATE_COLUMN,
    UPDATE_STATEMENT,
    DELETE_STATEMENT,
    CALL_STATEMENT,
    UNNAMED_ACTUAL_PAMETER,
    EXECUTE_IMMEDIATE_STATEMENT,
    OPEN_STATEMENT,
    OPEN_FOR_STATEMENT,
    FETCH_STATEMENT,
    CLOSE_STATEMENT,
    STATEMENT,
    
    // Declarations
    VARIABLE_DECLARATION,
    PARAMETER_DECLARATION,
    CURSOR_DECLARATION,
    RECORD_DECLARATION,
    TABLE_OF_DECLARATION,
    REF_CURSOR_DECLARATION,
    AUTONOMOUS_TRANSACTION_PRAGMA,
    EXCEPTION_INIT_PRAGMA,
    SERIALLY_REUSABLE_PRAGMA,
    PRAGMA_DECLARATION,
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
        b.rule(FILE_INPUT).is(b.oneOrMore(b.firstOf(
                ANONYMOUS_BLOCK,
                CREATE_PROCEDURE, 
                CREATE_FUNCTION, 
                CREATE_PACKAGE,
                CREATE_PACKAGE_BODY,
                EXECUTE_PLSQL_BUFFER)), EOF);

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
        
        b.rule(ANCHORED_DATATYPE).is(
                IDENTIFIER_NAME,
                b.optional(
                        DOT, IDENTIFIER_NAME, 
                        b.optional(DOT, IDENTIFIER_NAME)),
                MOD,
                b.firstOf(TYPE, ROWTYPE));

        b.rule(CUSTOM_DATATYPE).is(b.optional(IDENTIFIER_NAME, DOT), b.optional(IDENTIFIER_NAME, DOT), IDENTIFIER_NAME);
        
        b.rule(REF_DATATYPE).is(REF, CUSTOM_DATATYPE);
        
        b.rule(DATATYPE).is(b.firstOf(
                NUMERIC_DATATYPE,
                LOB_DATATYPE,
                CHARACTER_DATAYPE,
                BOOLEAN_DATATYPE,
                DATE_DATATYPE,
                ANCHORED_DATATYPE,
                CUSTOM_DATATYPE,
                REF_DATATYPE));
    }

    private static void createStatements(LexerfulGrammarBuilder b) {
        b.rule(HOST_AND_INDICATOR_VARIABLE).is(COLON, IDENTIFIER_NAME, b.optional(COLON, IDENTIFIER_NAME));
        
        b.rule(NULL_STATEMENT).is(NULL, SEMICOLON);
        
        b.rule(EXCEPTION_HANDLER).is(WHEN, b.firstOf(OTHERS, IDENTIFIER_NAME), THEN, b.oneOrMore(STATEMENT));
        
        b.rule(BLOCK_STATEMENT).is(
                b.optional(DECLARE, b.oneOrMore(DECLARE_SECTION)),
                BEGIN,
                b.oneOrMore(STATEMENT),
                b.optional(EXCEPTION, b.oneOrMore(EXCEPTION_HANDLER)), 
                END, b.optional(IDENTIFIER_NAME), SEMICOLON);
        
        b.rule(ASSIGNMENT_STATEMENT).is(OBJECT_REFERENCE, ASSIGNMENT, EXPRESSION, SEMICOLON);
        
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
                FOR, IDENTIFIER_NAME, IN, b.optional(REVERSE),
                b.firstOf(
                        b.sequence(OBJECT_REFERENCE, b.nextNot(RANGE)),
                        b.sequence(EXPRESSION, RANGE, EXPRESSION)),
                LOOP,
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
        
        b.rule(SELECT_STATEMENT).is(SELECT_EXPRESSION, SEMICOLON);
        
        b.rule(INSERT_STATEMENT).is(
                INSERT, INTO, IDENTIFIER_NAME,
                b.optional(LPARENTHESIS, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS),
                b.firstOf(
                        b.sequence(VALUES, LPARENTHESIS, EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION), RPARENTHESIS),
                        SELECT_EXPRESSION),
                SEMICOLON);
        
        b.rule(UPDATE_COLUMN).is(IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME), EQUALS, EXPRESSION);
        
        b.rule(UPDATE_STATEMENT).is(
                UPDATE, IDENTIFIER_NAME, b.optional(IDENTIFIER_NAME), SET, UPDATE_COLUMN, b.zeroOrMore(COMMA, UPDATE_COLUMN),
                b.optional(WHERE_CLAUSE),
                SEMICOLON);
        
        b.rule(DELETE_STATEMENT).is(DELETE, b.optional(FROM), IDENTIFIER_NAME, b.optional(WHERE_CLAUSE), SEMICOLON);
        
        b.rule(CALL_STATEMENT).is(OBJECT_REFERENCE, SEMICOLON);
        
        b.rule(UNNAMED_ACTUAL_PAMETER).is(
                b.optional(b.firstOf(b.sequence(IN, b.optional(OUT)), OUT)),
                EXPRESSION);
        
        b.rule(EXECUTE_IMMEDIATE_STATEMENT).is(
                EXECUTE, IMMEDIATE, CONCATENATION_EXPRESSION,
                b.optional(INTO, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME)),
                b.optional(USING, UNNAMED_ACTUAL_PAMETER, b.zeroOrMore(COMMA, UNNAMED_ACTUAL_PAMETER)),
                SEMICOLON);
        
        b.rule(OPEN_STATEMENT).is(
                OPEN, IDENTIFIER_NAME,
                b.optional(LPARENTHESIS, EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION), RPARENTHESIS),
                SEMICOLON);
        
        b.rule(OPEN_FOR_STATEMENT).is(
                OPEN, PRIMARY_EXPRESSION, FOR, EXPRESSION,
                b.optional(USING, UNNAMED_ACTUAL_PAMETER, b.zeroOrMore(COMMA, UNNAMED_ACTUAL_PAMETER)),
                SEMICOLON);
        
        b.rule(FETCH_STATEMENT).is(
                FETCH, PRIMARY_EXPRESSION,
                b.firstOf(
                        b.sequence(INTO, UNNAMED_ACTUAL_PAMETER, b.zeroOrMore(COMMA, UNNAMED_ACTUAL_PAMETER)),
                        b.sequence(
                                BULK, COLLECT, INTO, UNNAMED_ACTUAL_PAMETER, b.zeroOrMore(COMMA, UNNAMED_ACTUAL_PAMETER),
                                b.optional(LIMIT, EXPRESSION))),
                SEMICOLON);
        
        b.rule(CLOSE_STATEMENT).is(CLOSE, PRIMARY_EXPRESSION, SEMICOLON);
        
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
                                       CLOSE_STATEMENT));
    }
    
    private static void createDmlStatements(LexerfulGrammarBuilder b) {
        b.rule(SELECT_COLUMN).is(EXPRESSION, b.optional(b.optional(AS), IDENTIFIER_NAME));
        
        b.rule(FROM_CLAUSE).is(
                b.firstOf(
                        b.sequence(
                                IDENTIFIER_NAME,
                                b.optional(DOT, IDENTIFIER_NAME),
                                b.optional(REMOTE, IDENTIFIER_NAME)),
                        b.sequence(b.optional(THE), LPARENTHESIS, SELECT_EXPRESSION, RPARENTHESIS)),
                b.optional(IDENTIFIER_NAME));
        
        b.rule(WHERE_CLAUSE).is(WHERE, EXPRESSION);
        
        b.rule(INTO_CLAUSE).is(
                b.optional(BULK, COLLECT), INTO,
                OBJECT_REFERENCE, b.zeroOrMore(COMMA, OBJECT_REFERENCE));
        
        b.rule(GROUP_BY_CLAUSE).is(
                GROUP, BY, EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION),
                b.optional(HAVING, EXPRESSION));
        
        b.rule(ORDER_BY_CLAUSE).is(
                ORDER, BY, EXPRESSION, b.optional(b.firstOf(ASC, DESC)), b.zeroOrMore(COMMA, EXPRESSION), b.optional(b.firstOf(ASC, DESC)));
        
        b.rule(SELECT_EXPRESSION).is(
                b.firstOf(
                    b.sequence(
                            SELECT, b.optional(b.firstOf(ALL, DISTINCT, UNIQUE)), SELECT_COLUMN, b.zeroOrMore(COMMA, SELECT_COLUMN),
                            b.optional(INTO_CLAUSE),
                            FROM, FROM_CLAUSE, b.zeroOrMore(COMMA, FROM_CLAUSE),
                            b.optional(WHERE_CLAUSE),
                            b.optional(GROUP_BY_CLAUSE),
                            b.optional(ORDER_BY_CLAUSE)),
                    b.sequence(LPARENTHESIS, SELECT_EXPRESSION, RPARENTHESIS)),
                b.optional(b.firstOf(MINUS_KEYWORD, b.sequence(UNION, b.optional(ALL))), SELECT_EXPRESSION));
    }
    
    private static void createExpressions(LexerfulGrammarBuilder b) {
        // Reference: http://docs.oracle.com/cd/B28359_01/appdev.111/b28370/expression.htm
        
        // TODO: remove this rule and fix IDENTIFIER_NAME to accept all non-reserved keywords
        b.rule(BUILTIN_FUNCTIONS).is(b.firstOf(REPLACE, COUNT, OPEN, DELETE, CLOSE, EXISTS));
        
        b.rule(PRIMARY_EXPRESSION).is(
                b.firstOf(IDENTIFIER_NAME, HOST_AND_INDICATOR_VARIABLE, LITERAL, SQL, BUILTIN_FUNCTIONS, MULTIPLICATION));
        
        b.rule(BRACKED_EXPRESSION).is(b.firstOf(
                PRIMARY_EXPRESSION,
                b.sequence(LPARENTHESIS, EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION), RPARENTHESIS))).skipIfOneChild();
        
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
                                ISOPEN,
                                DELETE)
                        )).skipIfOneChild();
        
        b.rule(ARGUMENT).is(b.optional(IDENTIFIER_NAME, ASSOCIATION), EXPRESSION);
        
        b.rule(ARGUMENTS).is(LPARENTHESIS, b.optional(ARGUMENT, b.zeroOrMore(COMMA, ARGUMENT)), RPARENTHESIS);
        
        b.rule(CALL_EXPRESSION).is(MEMBER_EXPRESSION, ARGUMENTS);
        
        b.rule(OBJECT_REFERENCE).is(
                b.firstOf(CALL_EXPRESSION, MEMBER_EXPRESSION),
                b.zeroOrMore(DOT, b.firstOf(CALL_EXPRESSION, MEMBER_EXPRESSION)),
                b.optional(LPARENTHESIS, PLUS, RPARENTHESIS)).skipIfOneChild();
        
        b.rule(POSTFIX_EXPRESSION).is(OBJECT_REFERENCE, b.optional(IS, b.optional(NOT), NULL));
        
        b.rule(IN_EXPRESSION).is(POSTFIX_EXPRESSION, b.optional(b.sequence(b.optional(NOT), IN , LPARENTHESIS, EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION), RPARENTHESIS))).skipIfOneChild();
        
        b.rule(EXISTS_EXPRESSION).is(EXISTS , LPARENTHESIS, EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION), RPARENTHESIS).skipIfOneChild();
        
        b.rule(CASE_EXPRESSION).is(
                CASE, b.optional(IDENTIFIER_NAME),
                b.oneOrMore(WHEN, EXPRESSION, THEN, EXPRESSION),
                b.optional(ELSE, EXPRESSION),
                END);
        
        b.rule(UNARY_EXPRESSION).is(b.firstOf(
                        b.sequence(NOT, UNARY_EXPRESSION),
                        b.sequence(PLUS, UNARY_EXPRESSION),
                        b.sequence(MINUS, UNARY_EXPRESSION),
                        IN_EXPRESSION,
                        SELECT_EXPRESSION,
                        CASE_EXPRESSION,
                        EXISTS_EXPRESSION)).skipIfOneChild();
        
        b.rule(EXPONENTIATION_EXPRESSION).is(UNARY_EXPRESSION, b.zeroOrMore(EXPONENTIATION, UNARY_EXPRESSION)).skipIfOneChild();
        
        b.rule(MULTIPLICATIVE_EXPRESSION).is(EXPONENTIATION_EXPRESSION, b.zeroOrMore(b.firstOf(MULTIPLICATION, DIVISION), EXPONENTIATION_EXPRESSION)).skipIfOneChild();
        
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
                RETURN, DATATYPE, b.optional(DETERMINISTIC),
                b.firstOf(
                        SEMICOLON,
                        b.sequence(b.firstOf(IS, AS), b.zeroOrMore(DECLARE_SECTION), BLOCK_STATEMENT))
                );
        
        b.rule(VARIABLE_DECLARATION).is(IDENTIFIER_NAME,
                b.optional(CONSTANT),
                b.firstOf(
                        b.sequence(
                                DATATYPE,
                                b.optional(b.optional(NOT, NULL), b.firstOf(ASSIGNMENT, DEFAULT), EXPRESSION)),
                        EXCEPTION),                                           
                SEMICOLON);
        
        b.rule(CUSTOM_SUBTYPE).is(
                SUBTYPE, IDENTIFIER_NAME, IS, DATATYPE,
                b.optional(NOT, NULL),
                b.optional(RANGE_KEYWORD, NUMERIC_LITERAL, RANGE, NUMERIC_LITERAL),
                SEMICOLON);
        
        b.rule(CURSOR_DECLARATION).is(
                CURSOR, IDENTIFIER_NAME,
                b.optional(LPARENTHESIS, b.oneOrMore(PARAMETER_DECLARATION, b.optional(COMMA)), RPARENTHESIS),
                IS, SELECT_EXPRESSION, SEMICOLON);
        
        b.rule(RECORD_DECLARATION).is(
                TYPE, IDENTIFIER_NAME, IS, RECORD,
                LPARENTHESIS, b.oneOrMore(IDENTIFIER_NAME, DATATYPE, b.optional(COMMA)), RPARENTHESIS,
                SEMICOLON);
        
        b.rule(TABLE_OF_DECLARATION).is(
                TYPE, IDENTIFIER_NAME, IS, TABLE, OF, DATATYPE,
                b.optional(INDEX, BY, DATATYPE),
                SEMICOLON);
        
        b.rule(REF_CURSOR_DECLARATION).is(TYPE, IDENTIFIER_NAME, IS, REF, CURSOR, b.optional(RETURN, DATATYPE), SEMICOLON);
        
        b.rule(AUTONOMOUS_TRANSACTION_PRAGMA).is(PRAGMA, AUTONOMOUS_TRANSACTION, SEMICOLON);
        
        b.rule(EXCEPTION_INIT_PRAGMA).is(PRAGMA, EXCEPTION_INIT, LPARENTHESIS, EXPRESSION, COMMA, EXPRESSION, RPARENTHESIS, SEMICOLON);
        
        b.rule(SERIALLY_REUSABLE_PRAGMA).is(PRAGMA, SERIALLY_REUSABLE, SEMICOLON);
        
        b.rule(PRAGMA_DECLARATION).is(b.firstOf(
                EXCEPTION_INIT_PRAGMA,
                AUTONOMOUS_TRANSACTION_PRAGMA,
                SERIALLY_REUSABLE_PRAGMA));
        
        b.rule(DECLARE_SECTION).is(b.oneOrMore(b.firstOf(
                VARIABLE_DECLARATION,
                PROCEDURE_DECLARATION,
                FUNCTION_DECLARATION,
                CUSTOM_SUBTYPE,
                CURSOR_DECLARATION,
                RECORD_DECLARATION,
                TABLE_OF_DECLARATION,
                REF_CURSOR_DECLARATION,
                PRAGMA_DECLARATION)));
    }
    
    private static void createProgramUnits(LexerfulGrammarBuilder b) {
        b.rule(EXECUTE_PLSQL_BUFFER).is(DIVISION);
        
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
                RETURN, DATATYPE, b.optional(DETERMINISTIC),
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
        
        b.rule(ANONYMOUS_BLOCK).is(BLOCK_STATEMENT, EXECUTE_PLSQL_BUFFER);
    }
}
