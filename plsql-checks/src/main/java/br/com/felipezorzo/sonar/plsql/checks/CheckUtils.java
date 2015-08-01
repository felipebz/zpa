package br.com.felipezorzo.sonar.plsql.checks;

import com.sonar.sslr.api.AstNode;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;

public class CheckUtils {

    private CheckUtils() {
    }
    
    public static boolean isNullLiteralOrEmptyString(AstNode node) {
        if (node.hasDirectChildren(PlSqlGrammar.NULL_LITERAL)) {
            return true;
        }
        
        AstNode characterLiteral = node.getFirstChild(PlSqlGrammar.CHARACTER_LITERAL);
        if (characterLiteral != null && "''".equals(characterLiteral.getTokenValue())) {
            return true;
        }
        
        return false;
    }
}
