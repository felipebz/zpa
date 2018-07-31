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
