package br.com.felipezorzo.sonar.plsql.api;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.TokenType;

public enum PlSqlPunctuator implements TokenType {
    // Based on http://docs.oracle.com/cd/B19306_01/appdev.102/b14261/fundamentals.htm#sthref297
    COMMA(","),
    PLUS("+"),
    MOD("%"),
    DOT("."),
    DIVISION("/"),
    LPARENTHESIS("("),
    RPARENTHESIS(")"),
    COLON(":"),
    SEMICOLON(";"),
    MULTIPLICATION("*"),
    EQUALS("="),
    LESSTHAN("<"),
    GREATERTHAN(">"),
    REMOTE("@"),
    SUBTRACTION("-"),
    ASSIGNMENT(":="),
    ASSOCIATION("=>"),
    CONCATENATION("||"),
    EXPONENTIATION("**"),
    LLABEL("<<"),
    RLABEL(">>"),
    RANGE(".."),
    NOTEQUALS("<>"),
    NOTEQUALS2("!="),
    NOTEQUALS3("~="),
    NOTEQUALS4("^="),
    LESSTHANOREQUAL("<="),
    GREATERTHANOREQUAL(">=");

    private final String value;

    private PlSqlPunctuator(String word) {
        this.value = word;
    }

    public String getName() {
        return name();
    }

    public String getValue() {
        return value;
    }

    public boolean hasToBeSkippedFromAst(AstNode node) {
        return false;
    }
}
