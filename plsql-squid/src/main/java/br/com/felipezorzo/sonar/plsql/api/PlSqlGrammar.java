package br.com.felipezorzo.sonar.plsql.api;

import static br.com.felipezorzo.sonar.plsql.api.PlSqlKeyword.*;
import static br.com.felipezorzo.sonar.plsql.api.PlSqlPunctuator.*;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

public enum PlSqlGrammar implements GrammarRuleKey {
    BLOCK_STATEMENT,
    NULL_LITERAL,
    NULL_STATEMENT,
    STATEMENT;

    public static LexerfulGrammarBuilder create() {
        LexerfulGrammarBuilder b = LexerfulGrammarBuilder.create();

        b.rule(NULL_LITERAL).is(NULL);

        createStatements(b);
        return b;
    }

    private static void createStatements(LexerfulGrammarBuilder b) {
        b.rule(NULL_STATEMENT).is(NULL, SEMICOLON);
        b.rule(BLOCK_STATEMENT).is(BEGIN, b.oneOrMore(STATEMENT), END, SEMICOLON);
        b.rule(STATEMENT).is(b.firstOf(NULL_STATEMENT, BLOCK_STATEMENT));
    }
}
