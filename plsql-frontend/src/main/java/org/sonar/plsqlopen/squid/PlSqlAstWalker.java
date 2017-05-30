package org.sonar.plsqlopen.squid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sonar.plsqlopen.PlSqlVisitorContext;
import org.sonar.plsqlopen.checks.PlSqlCheck;
import org.sonar.plugins.plsqlopen.api.symbols.Scope;
import org.sonar.plugins.plsqlopen.api.symbols.SymbolTable;

import com.sonar.sslr.api.AstAndTokenVisitor;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.AstVisitor;
import com.sonar.sslr.api.Token;

public class PlSqlAstWalker {

    private final Map<AstNodeType, PlSqlCheck[]> visitorsByNodeType = new IdentityHashMap<AstNodeType, PlSqlCheck[]>();
    private Collection<PlSqlCheck> checks;
    private Token lastVisitedToken = null;

    public PlSqlAstWalker(Collection<PlSqlCheck> checks) {
        this.checks = checks;
    }

    public void walk(PlSqlVisitorContext context) {
        for (PlSqlCheck check : checks) {
            check.setContext(context);
            check.init();

            for (AstNodeType type : check.getAstNodeTypesToVisit()) {
                List<PlSqlCheck> visitorsByType = getAstVisitors(type);
                visitorsByType.add(check);
                putAstVisitors(type, visitorsByType);
            }
        }
        
        AstNode tree = context.rootTree();
        if (tree != null) {
            for (PlSqlCheck check : checks) {
                check.visitFile(tree);
            }
            visit(tree);
            for (PlSqlCheck check : checks) {
                check.leaveFile(tree);
            }
        }
    }
    
    private void visit(AstNode ast) {
        PlSqlCheck[] nodeVisitors = getNodeVisitors(ast);
        visitNode(ast, nodeVisitors);
        visitToken(ast);
        visitChildren(ast);
        leaveNode(ast, nodeVisitors);
    }

    private static void leaveNode(AstNode ast, PlSqlCheck[] nodeVisitors) {
        for (int i = nodeVisitors.length - 1; i >= 0; i--) {
            nodeVisitors[i].leaveNode(ast);
        }
    }

    private void visitChildren(AstNode ast) {
        for (AstNode child : ast.getChildren()) {
            visit(child);
        }
    }

    private void visitToken(AstNode ast) {
        if (ast.getToken() != null && lastVisitedToken != ast.getToken()) {
            lastVisitedToken = ast.getToken();
            for (PlSqlCheck astAndTokenVisitor : checks) {
                astAndTokenVisitor.visitToken(lastVisitedToken);
            }
        }
    }

    private static void visitNode(AstNode ast, PlSqlCheck[] nodeVisitors) {
        for (PlSqlCheck nodeVisitor : nodeVisitors) {
            nodeVisitor.visitNode(ast);
        }
    }

    private PlSqlCheck[] getNodeVisitors(AstNode ast) {
        PlSqlCheck[] nodeVisitors = visitorsByNodeType.get(ast.getType());
        if (nodeVisitors == null) {
            nodeVisitors = new PlSqlCheck[0];
        }
        return nodeVisitors;
    }

    private void putAstVisitors(AstNodeType type, List<PlSqlCheck> visitors) {
        visitorsByNodeType.put(type, visitors.toArray(new PlSqlCheck[visitors.size()]));
    }

    private List<PlSqlCheck> getAstVisitors(AstNodeType type) {
        PlSqlCheck[] visitorsByType = visitorsByNodeType.get(type);
        return visitorsByType == null ? new ArrayList<PlSqlCheck>()
                : new ArrayList<PlSqlCheck>(Arrays.asList(visitorsByType));
    }

}
