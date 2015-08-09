package br.com.felipezorzo.sonar.plsql.checks;

import java.util.List;

import com.sonar.sslr.api.AstNode;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;

public class CheckUtils {

    private CheckUtils() {
    }
    
    public static boolean isNullLiteralOrEmptyString(AstNode node) {
        if (node.hasDirectChildren(PlSqlGrammar.NULL_LITERAL)) {
            return true;
        }
        
        if (isEmptyString(node)) {
            return true;
        }
        
        return false;
    }
    
    public static boolean isEmptyString(AstNode node) {
        AstNode characterLiteral = node.getFirstChild(PlSqlGrammar.CHARACTER_LITERAL);
        if (characterLiteral != null && "''".equals(characterLiteral.getTokenValue())) {
            return true;
        }
        
        return false;
    }
    
    public static boolean equalNodes(AstNode node1, AstNode node2){
    	if (!node1.getType().equals(node2.getType()) || node1.getNumberOfChildren() != node2.getNumberOfChildren()){
    		return false;
    	}

    	if (node1.getNumberOfChildren() == 0) {
    		return node1.getToken().getValue().equals(node2.getToken().getValue());
    	}

    	List<AstNode> children1 = node1.getChildren();
    	List<AstNode> children2 = node2.getChildren();
    	for (int i = 0; i < children1.size(); i++){
    		if (!equalNodes(children1.get(i), children2.get(i))){
    			return false;
    		}
    	}
    	return true;
    }
}
