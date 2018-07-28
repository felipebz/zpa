/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2018 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plsqlopen.squid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.sonar.plsqlopen.PlSqlVisitorContext;
import org.sonar.plsqlopen.checks.PlSqlVisitor;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;

public class PlSqlAstWalker {

    private final Map<AstNodeType, PlSqlVisitor[]> visitorsByNodeType = new IdentityHashMap<AstNodeType, PlSqlVisitor[]>();
    private Collection<PlSqlVisitor> checks;
    private Token lastVisitedToken = null;

    public PlSqlAstWalker(Collection<PlSqlVisitor> checks) {
        this.checks = checks;
    }

    public void walk(PlSqlVisitorContext context) {
        for (PlSqlVisitor check : checks) {
            check.setContext(context);
            check.startScan();
            check.init();

            for (AstNodeType type : check.subscribedKinds()) {
                List<PlSqlVisitor> visitorsByType = getAstVisitors(type);
                visitorsByType.add(check);
                putAstVisitors(type, visitorsByType);
            }
        }
        
        AstNode tree = context.rootTree();
        if (tree != null) {
            for (PlSqlVisitor check : checks) {
                check.visitFile(tree);
            }
            visit(tree);
            for (PlSqlVisitor check : checks) {
                check.leaveFile(tree);
            }
        }
    }
    
    private void visit(AstNode ast) {
        PlSqlVisitor[] nodeVisitors = getNodeVisitors(ast);
        visitNode(ast, nodeVisitors);
        visitToken(ast);
        visitChildren(ast);
        leaveNode(ast, nodeVisitors);
    }

    private static void leaveNode(AstNode ast, PlSqlVisitor[] nodeVisitors) {
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
            for (PlSqlVisitor astAndTokenVisitor : checks) {
                astAndTokenVisitor.visitToken(lastVisitedToken);
            }
        }
    }

    private static void visitNode(AstNode ast, PlSqlVisitor[] nodeVisitors) {
        for (PlSqlVisitor nodeVisitor : nodeVisitors) {
            nodeVisitor.visitNode(ast);
        }
    }

    private PlSqlVisitor[] getNodeVisitors(AstNode ast) {
        PlSqlVisitor[] nodeVisitors = visitorsByNodeType.get(ast.getType());
        if (nodeVisitors == null) {
            nodeVisitors = new PlSqlVisitor[0];
        }
        return nodeVisitors;
    }

    private void putAstVisitors(AstNodeType type, List<PlSqlVisitor> visitors) {
        visitorsByNodeType.put(type, visitors.toArray(new PlSqlVisitor[visitors.size()]));
    }

    private List<PlSqlVisitor> getAstVisitors(AstNodeType type) {
        PlSqlVisitor[] visitorsByType = visitorsByNodeType.get(type);
        return visitorsByType == null ? new ArrayList<PlSqlVisitor>()
                : new ArrayList<PlSqlVisitor>(Arrays.asList(visitorsByType));
    }

}
