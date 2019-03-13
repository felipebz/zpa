/*
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import org.sonar.plsqlopen.squid.PlSqlAstWalker;
import org.sonar.plugins.plsqlopen.api.PlSqlVisitorContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PlSqlVisitor {

    private PlSqlVisitorContext context;
    private final Set<AstNodeType> astNodeTypesToVisit = new HashSet<>();

    public Set<AstNodeType> subscribedKinds() {
        return astNodeTypesToVisit;
    }
    
    public void startScan() {
        // default implementation does nothing
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

    public void visitComment(Trivia trivia, String content) {
        // default implementation does nothing
    }

    public void leaveNode(AstNode node) {
        // default implementation does nothing
    }

    public PlSqlVisitorContext getContext() {
        return context;
    }
    
    public void subscribeTo(AstNodeType... astNodeTypes) {
        Collections.addAll(astNodeTypesToVisit, astNodeTypes);
    }

    public void scanFile(PlSqlVisitorContext context) {
        
        PlSqlAstWalker walker = new PlSqlAstWalker(Collections.singleton(this));
        walker.walk(context);
    }

    public void setContext(PlSqlVisitorContext context) {
        this.context = context;
    }

}
