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
    AND("and"),
    OR("or"),
    CONSTANT("constant"),
    FALSE("false"),
    TRUE("true"),
    BFILE("bfile"),
    BLOB("blob"),
    CLOB("clob"),
    NCLOB("nclob"),
    BINARY_DOUBLE("binary_double"),
    BINARY_FLOAT("binary_float"),
    BINARY_INTEGER("binary_integer"),
    DEC("dec"),
    DECIMAL("DECIMAL"),
    DOUBLE("double"),
    PRECISION("precision"),
    FLOAT("float"),
    INT("int"),
    INTEGER("integer"),
    NATURAL("natural"),
    NATURALN("naturaln"),
    NUMERIC("numeric"),
    PLS_INTEGER("pls_integer"),
    POSITIVE("positive"),
    POSITIVEN("positiven"),
    REAL("real"),
    SIGNTYPE("signtype"),
    SMALLINT("smallint"),
    CHAR("char"),
    CHARACTER("character"),
    LONG("long"),
    RAW("raw"),
    NCHAR("nchar"),
    NVARCHAR2("nvarchar2"),
    ROWID("rowid"),
    STRING("string"),
    UROWID("urowid"),
    VARCHAR("varchar"),
    VARCHAR2("varchar2"),
    BOOLEAN("boolean"),
    DATE("date"),
    ROWCOUNT("rowcount"),
    SQL("sql"),
    BULK_ROWCOUNT("bulk_rowcount"),
    COUNT("count"),
    FIRST("first"),
    LAST("last"),
    LIMIT("limit"),
    NEXT("next"),
    PRIOR("prior"),
    DECLARE("declare");

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
