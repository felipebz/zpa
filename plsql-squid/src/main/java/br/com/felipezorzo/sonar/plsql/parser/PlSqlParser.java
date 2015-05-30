package br.com.felipezorzo.sonar.plsql.parser;

import br.com.felipezorzo.sonar.plsql.PlSqlConfiguration;
import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.lexer.PlSqlLexer;

import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.impl.Parser;

public final class PlSqlParser {

    private PlSqlParser() {
    }

    public static Parser<Grammar> create(PlSqlConfiguration conf) {
        return Parser.builder(PlSqlGrammar.create().build())
                .withLexer(PlSqlLexer.create(conf)).build();
    }
}
