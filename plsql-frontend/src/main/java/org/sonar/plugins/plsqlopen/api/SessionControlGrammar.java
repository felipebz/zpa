package org.sonar.plugins.plsqlopen.api;

import static com.sonar.sslr.api.GenericTokenType.EOF;
import static org.sonar.plugins.plsqlopen.api.PlSqlGrammar.IDENTIFIER_NAME;
import static org.sonar.plugins.plsqlopen.api.PlSqlKeyword.*;
import static org.sonar.plugins.plsqlopen.api.PlSqlPunctuator.*;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

public enum SessionControlGrammar implements GrammarRuleKey {
    
    ALTER_SESSION,
    SET_ROLE,
    SESSION_CONTROL_COMMAND;
    
    public static void buildOn(LexerfulGrammarBuilder b) {
        
        b.rule(ALTER_SESSION).is(ALTER, SESSION, b.oneOrMore(b.anyTokenButNot(b.firstOf(SEMICOLON, DIVISION, EOF))));
        
        b.rule(SET_ROLE).is(SET, ROLE,
                b.firstOf(
                        NONE,
                        b.sequence(ALL, b.optional(EXCEPT, b.oneOrMore(IDENTIFIER_NAME, b.optional(COMMA)))),
                        b.oneOrMore(IDENTIFIER_NAME, b.optional(IDENTIFIED, BY, b.anyToken()), b.optional(COMMA))
                        ));
        
        b.rule(SESSION_CONTROL_COMMAND).is(b.firstOf(ALTER_SESSION, SET_ROLE), b.optional(SEMICOLON));
    }

}
