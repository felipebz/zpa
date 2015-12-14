/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015 Felipe Zorzo
 * felipe.b.zorzo@gmail.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plsqlopen;

import java.io.File;
import java.util.List;

import org.sonar.api.source.Symbolizable;
import org.sonar.plugins.plsqlopen.api.symbols.SymbolTable;
import org.sonar.squidbridge.SquidAstVisitorContextImpl;
import org.sonar.squidbridge.api.CodeCheck;
import org.sonar.squidbridge.api.CodeVisitor;
import org.sonar.squidbridge.api.SourceProject;

import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.Token;

public class DefaultPlSqlVisitorContext<G extends Grammar> extends SquidAstVisitorContextImpl<G> implements PlSqlVisitorContext {

    private SonarComponents components;
    private SymbolTable symbolTable;
    
    public DefaultPlSqlVisitorContext(SourceProject project, SonarComponents components) {
        super(project);
        this.components = components;
    }
    
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public void setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }
    
    @Override
    public Symbolizable getSymbolizable() {
        return components.symbolizableFor(components.inputFromIOFile(getFile()));
    }

    @Override
    public void createLineViolation(CodeCheck check, String message, AstNode node, Object... messageParameters) {
        createLineViolation(check, message, node.getToken(), messageParameters);
    }
    
    @Override
    public void createLineViolation(CodeCheck check, String message, Token token, Object... messageParameters) {
      createLineViolation(check, message, token.getLine(), messageParameters);
    }
    
    @Override
    public void createLineViolation(CodeCheck check, String message, int line, Object... messageParameters) {
        AnalyzerMessage checkMessage = new AnalyzerMessage(check, message, null, messageParameters);
        if (line > 0) {
            checkMessage.setLine(line);
        }
        components.reportIssue(checkMessage, components.inputFromIOFile(getFile()));
        log(checkMessage);
    }
    
    @Override
    public void createViolation(CodeVisitor check, String message, AstNode node, Object... messageParameters) {
        createViolation(check, message, node, ImmutableList.<Location>of(), messageParameters);
    }
    
    @Override
    public void createViolation(CodeVisitor check, String message, AstNode node, List<Location> secondary, Object... messageParameters) {
        AnalyzerMessage checkMessage = new AnalyzerMessage(check, message, AnalyzerMessage.textSpanFor(node), messageParameters);
        for (Location location : secondary) {
            AnalyzerMessage secondaryLocation = 
                    new AnalyzerMessage(check, location.msg, AnalyzerMessage.textSpanFor(location.node), messageParameters);
            checkMessage.addSecondaryLocation(secondaryLocation);
        }
        components.reportIssue(checkMessage, components.inputFromIOFile(getFile()));
        log(checkMessage);
    }

}
