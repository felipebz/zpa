package br.com.felipezorzo.sonar.plsql.checks.common;

import java.util.List;

import com.sonar.sslr.api.AstNode;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.checks.BaseCheck;

public abstract class BaseMethodCallChecker extends BaseCheck {
    
    private AstNode currentNode;
    
    public AstNode getCurrentNode() {
        return currentNode;
    }
    
    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.METHOD_CALL);
    }
    
    @Override
    public void visitNode(AstNode node) {
        currentNode = node;
        AstNode identifier = node.getFirstChild();
        if (!isMethod(identifier)) return;
        
        AstNode arguments = node.getFirstChild(PlSqlGrammar.ARGUMENTS);
        if (arguments == null) return;
        
        List<AstNode> allArguments = arguments.getChildren(PlSqlGrammar.ARGUMENT);
        checkArguments(allArguments);
    }
    
    protected abstract boolean isMethod(AstNode identifier);
    
    protected abstract void checkArguments(List<AstNode> arguments);

}
