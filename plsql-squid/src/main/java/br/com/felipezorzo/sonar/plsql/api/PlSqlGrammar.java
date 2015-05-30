package br.com.felipezorzo.sonar.plsql.api;

import static br.com.felipezorzo.sonar.plsql.api.PlSqlKeyword.*;
import static br.com.felipezorzo.sonar.plsql.api.PlSqlPunctuator.*;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

public enum PlSqlGrammar implements GrammarRuleKey {
    BLOCK_STATEMENT,
    NULL_LITERAL,
    NULL_STATEMENT;

    public static LexerfulGrammarBuilder create() {
        LexerfulGrammarBuilder b = LexerfulGrammarBuilder.create();

        b.rule(NULL_LITERAL).is(NULL);

        b.rule(NULL_STATEMENT).is(NULL, SEMICOLON);
        b.rule(BLOCK_STATEMENT).is(BEGIN, NULL_STATEMENT, END, SEMICOLON);

        return b;
    }
}
