package br.com.felipezorzo.sonar.plsql.api;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.TokenType;

public enum PlSqlKeyword implements TokenType {
    BEGIN("begin"),
    NULL("null"),
    END("end"),
    EXCEPTION("exception"),
    NUMBER("number"),
    OTHERS("others"),
    THEN("then"),
    WHEN("when"),
    DEFAULT("default"),
    NOT("not"),
    CONSTANT("constant"),
    FALSE("false"),
    TRUE("true"),
    BFILE("bfile"),
    BLOB("blob"),
    CLOB("clob"),
    NCLOB("nclob");

    private final String value;

    private PlSqlKeyword(String value) {
        this.value = value;
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public boolean hasToBeSkippedFromAst(AstNode node) {
        return false;
    }

    public static String[] keywordValues() {
        PlSqlKeyword[] keywordsEnum = PlSqlKeyword.values();
        String[] keywords = new String[keywordsEnum.length];
        for (int i = 0; i < keywords.length; i++) {
            keywords[i] = keywordsEnum[i].getValue();
        }
        return keywords;
    }
}
