package org.sonar.plsqlopen.checks;

import java.util.HashSet;
import java.util.Set;

import org.sonar.plsqlopen.PlSqlVisitorContext;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;

public class PlSqlVisitor {

    private PlSqlVisitorContext context;

    private final Set<AstNodeType> astNodeTypesToVisit = new HashSet<>();
    
    public Set<AstNodeType> getAstNodeTypesToVisit() {
        return astNodeTypesToVisit;
    }
    
    public void init() {
        // default implementation does nothing
    }

    public void visitFile(AstNode node) {
        // default implementation does nothing
    }

    public void leaveFile(AstNode node) {
        // default implementation does nothing
    }

    public void visitNode(AstNode node) {
        // default implementation does nothing
    }

    public void visitToken(Token token) {
        // default implementation does nothing
    }

    public void leaveNode(AstNode node) {
        // default implementation does nothing
    }
    
    public void subscribeTo(AstNodeType... astNodeTypes) {
        for (AstNodeType type : astNodeTypes) {
            astNodeTypesToVisit.add(type);
        }
    }

    public PlSqlVisitorContext getContext() {
        return context;
    }
    
    public void setContext(PlSqlVisitorContext context) {
        this.context = context;
    }

}
