/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2017 Felipe Zorzo
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
package org.sonar.plsqlopen;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.sonar.plsqlopen.checks.PlSqlVisitor;
import org.sonar.plsqlopen.metadata.FormsMetadata;
import org.sonar.plugins.plsqlopen.api.symbols.Scope;
import org.sonar.plugins.plsqlopen.api.symbols.SymbolTable;

import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.Token;

public class DefaultPlSqlVisitorContext implements PlSqlVisitorContext {

    private FormsMetadata formsMetadata;
    private SymbolTable symbolTable;
    private Scope scope;
    private AstNode rootTree;
    private PlSqlFile plSqlFile;
    private RecognitionException parsingException;
    private Collection<AnalyzerMessage> messages = new HashSet<>();
    
    public DefaultPlSqlVisitorContext(AstNode rootTree) {
        this(rootTree, null, null, null);
    }
    
    public DefaultPlSqlVisitorContext(AstNode rootTree, PlSqlFile plSqlFile, FormsMetadata formsMetadata) {
        this(rootTree, plSqlFile, null, formsMetadata);
    }

    public DefaultPlSqlVisitorContext(PlSqlFile plsqlFile, RecognitionException parsingException, FormsMetadata formsMetadata) {
        this(null, plsqlFile, parsingException, formsMetadata);
    }

    private DefaultPlSqlVisitorContext(AstNode rootTree, PlSqlFile plSqlFile, RecognitionException parsingException, FormsMetadata formsMetadata) {
        this.rootTree = rootTree;
        this.plSqlFile = plSqlFile;
        this.parsingException = parsingException;
        this.formsMetadata = formsMetadata;
    }
    
    public AstNode rootTree() {
        return rootTree;
    }

    public PlSqlFile plSqlFile() {
        return plSqlFile;
    }

    public RecognitionException parsingException() {
        return parsingException;
    }
    
    @Override
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    @Override
    public void setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }
    
    @Override
    public void setCurrentScope(Scope scope) {
        this.scope = scope;
    }
    
    @Override
    public Scope getCurrentScope() {
        return scope;
    }

    @Override
    public FormsMetadata getFormsMetadata() {
        return formsMetadata;
    }
    
    @Override
    public Collection<AnalyzerMessage> getIssues() {
        return messages;
    }
    
    @Override
    public void createFileViolation(PlSqlVisitor check, String message, Object... messageParameters) {
        AnalyzerMessage checkMessage = new AnalyzerMessage(check, message, null, messageParameters);
        checkMessage.setLine(0);
        messages.add(checkMessage);
    }
    
    @Override
    public void createLineViolation(PlSqlVisitor check, String message, AstNode node, Object... messageParameters) {
        createLineViolation(check, message, node.getToken(), messageParameters);
    }
    
    @Override
    public void createLineViolation(PlSqlVisitor check, String message, Token token, Object... messageParameters) {
      createLineViolation(check, message, token.getLine(), messageParameters);
    }
    
    @Override
    public void createLineViolation(PlSqlVisitor check, String message, int line, Object... messageParameters) {
        AnalyzerMessage checkMessage = new AnalyzerMessage(check, message, null, messageParameters);
        if (line > 0) {
            checkMessage.setLine(line);
        }
        messages.add(checkMessage);
    }
    
    @Override
    public void createViolation(PlSqlVisitor check, String message, AstNode node, Object... messageParameters) {
        createViolation(check, message, node, ImmutableList.<Location>of(), messageParameters);
    }
    
    @Override
    public void createViolation(PlSqlVisitor check, String message, AstNode node, List<Location> secondary, Object... messageParameters) {
        AnalyzerMessage checkMessage = new AnalyzerMessage(check, message, AnalyzerMessage.textSpanFor(node), messageParameters);
        for (Location location : secondary) {
            AnalyzerMessage secondaryLocation = 
                    new AnalyzerMessage(check, location.msg, AnalyzerMessage.textSpanFor(location.node), messageParameters);
            checkMessage.addSecondaryLocation(secondaryLocation);
        }
        messages.add(checkMessage);
    }

    @Override
    public File getFile() {
        return plSqlFile.file();
    }

}
