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
    DECLARE("declare"),
    IF("if"),
    ELSIF("elsif"),
    ELSE("else"),
    LOOP("loop"),
    EXIT("exit"),
    CONTINUE("continue"),
    FOR("for"),
    IN("in"),
    OUT("out"),
    REVERSE("reverse"),
    WHILE("while"),
    RETURN("return"),
    NOCOPY("nocopy"),
    PROCEDURE("procedure"),
    CREATE("create"),
    REPLACE("replace"),
    IS("is"),
    AS("as"),
    EXTERNAL("external"),
    AUTHID("authid"),
    CURRENT_USER("current_user"),
    DEFINER("definer"),
    LANGUAGE("language"),
    JAVA("java"),
    FUNCTION("function"),
    EXISTS("exists"),
    FOUND("found"),
    NOTFOUND("notfound"),
    ISOPEN("isopen"),
    LIKE("like"),
    BETWEEN("between"),
    COMMIT("commit"),
    WORK("work"),
    FORCE("force"),
    COMMENT("comment"),
    WRITE("write"),
    IMMEDIATE("immediate"),
    BATCH("batch"),
    WAIT("wait"),
    NOWAIT("nowait"),
    ROLLBACK("rollback"),
    TO("to"),
    SAVEPOINT("savepoint"),
    RAISE("raise"),
    TYPE("type"),
    PACKAGE("package"),
    BODY("body"),
    SUBTYPE("subtype"),
    RANGE_KEYWORD("range"),
    WHERE("where"),
    SELECT("select"),
    FROM("from"),
    BULK("bulk"),
    COLLECT("collect"),
    INTO("into"),
    ALL("all"),
    DISTINCT("distinct"),
    UNIQUE("unique"),
    CURSOR("cursor"),
    RECORD("record"),
    TABLE("table"),
    OF("of"),
    INDEX("index"),
    BY("by"),
    REF("ref"),
    ROWTYPE("rowtype"),
    GROUP("group"),
    ORDER("order"),
    ASC("asc"),
    DESC("desc"),
    THE("the");

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
