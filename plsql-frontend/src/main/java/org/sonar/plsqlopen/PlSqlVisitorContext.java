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
package org.sonar.plsqlopen;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import org.sonar.plsqlopen.checks.IssueLocation;
import org.sonar.plsqlopen.checks.PlSqlCheck;
import org.sonar.plsqlopen.checks.PlSqlCheck.PreciseIssue;
import org.sonar.plsqlopen.metadata.FormsMetadata;
import org.sonar.plugins.plsqlopen.api.symbols.Scope;
import org.sonar.plugins.plsqlopen.api.symbols.SymbolTable;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.Token;

public class PlSqlVisitorContext {

    private final AstNode rootTree;
    private final PlSqlFile plSqlFile;
    private final RecognitionException parsingException;
    private SymbolTable symbolTable;
    private Scope scope;
    private FormsMetadata formsMetadata;

    public PlSqlVisitorContext(AstNode rootTree, PlSqlFile plsqlFile, FormsMetadata metadata) {
        this(rootTree, plsqlFile, null, metadata);
    }

    public PlSqlVisitorContext(AstNode rootTree, PlSqlFile plsqlFile) {
        this(rootTree, plsqlFile, null, null);
    }

    public PlSqlVisitorContext(PlSqlFile plsqlFile, RecognitionException parsingException, FormsMetadata metadata) {
        this(null, plsqlFile, parsingException, metadata);
    }
    
    private PlSqlVisitorContext(AstNode rootTree, PlSqlFile plsqlFile, RecognitionException parsingException, FormsMetadata metadata) {
        this.rootTree = rootTree;
        this.plSqlFile = plsqlFile;
        this.parsingException = parsingException;
        this.formsMetadata = metadata;
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

    /***
     * Creates a file level violation.
     * @param check Rule associated with the violation.
     * @param message Description of the violation.
     * @param messageParameters Parameters used to format the description.
     * @deprecated since 2.2. Use {@link PlSqlCheck#addFileIssue(String)}.
     */
    @Deprecated
    public void createFileViolation(PlSqlCheck check, String message, Object... messageParameters) {
        check.addFileIssue(MessageFormat.format(message, messageParameters));
    }
    
    /***
     * Creates a line level violation.
     * @param check Rule associated with the violation.
     * @param message Description of the violation.
     * @param node Node that causes the violation.
     * @param messageParameters Parameters used to format the description.
     * @deprecated since 2.2. Use {@link PlSqlCheck#addLineIssue(String, int)}.
     */
    @Deprecated
    public void createLineViolation(PlSqlCheck check, String message, AstNode node, Object... messageParameters) {
        createLineViolation(check, message, node.getTokenLine(), messageParameters);
    }
    
    /***
     * Creates a line level violation.
     * @param check Rule associated with the violation.
     * @param message Description of the violation.
     * @param token Token that causes the violation.
     * @param messageParameters Parameters used to format the description.
     * @deprecated since 2.2. Use {@link PlSqlCheck#addLineIssue(String, int)}.
     */
    @Deprecated
    public void createLineViolation(PlSqlCheck check, String message, Token token, Object... messageParameters) {
        createLineViolation(check, message, token.getLine(), messageParameters);
    }
    
    /***
     * Creates a line level violation.
     * @param check Rule associated with the violation.
     * @param message Description of the violation.
     * @param line Line where the violation occurs.
     * @param messageParameters Parameters used to format the description.
     * @deprecated since 2.2. Use {@link PlSqlCheck#addLineIssue(String, int)}.
     */
    @Deprecated
    public void createLineViolation(PlSqlCheck check, String message, int line, Object... messageParameters) {
        check.addLineIssue(MessageFormat.format(message, messageParameters), line);
    }
    
    /***
     * Creates a violation.
     * @param check Rule associated with the violation.
     * @param message Description of the violation.
     * @param node Node that causes the violation.
     * @param messageParameters Parameters used to format the description.
     * @deprecated since 2.2. Use {@link PlSqlCheck#addIssue(AstNode, String)} or {@link PlSqlCheck#addIssue(Token, String)}.
     */
    @Deprecated
    public void createViolation(PlSqlCheck check, String message, AstNode node, Object... messageParameters) {
        createViolation(check, message, node, Collections.emptyList(), messageParameters);
    }
    
    /***
     * Creates a violation.
     * @param check Rule associated with the violation.
     * @param message Description of the violation.
     * @param node Node that causes the violation.
     * @param secondary List of locations.
     * @param messageParameters Parameters used to format the description.
     * @deprecated since 2.2. Use {@link PlSqlCheck#addIssue(org.sonar.plsqlopen.checks.IssueLocation)}.
     */
    @Deprecated
    public void createViolation(PlSqlCheck check, String message, AstNode node, List<Location> secondary, Object... messageParameters) {
        PreciseIssue issue = check.addIssue(node, MessageFormat.format(message, messageParameters));
        for (Location location : secondary) {
            issue.secondary(location.node, location.msg);
        }
    }

    public void setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }
    
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public void setCurrentScope(Scope scope) {
        this.scope = scope;
    }

    public Scope getCurrentScope() {
        return scope;
    }
    
    public void setFormsMetadata(FormsMetadata formsMetadata) {
        this.formsMetadata = formsMetadata;
    }

    public FormsMetadata getFormsMetadata() {
        return formsMetadata;
    }
    
    /***
     * @deprecated since 2.2. Use {@link IssueLocation}.
     */
    @Deprecated
    public static class Location {
        public final String msg;
        public final AstNode node;

        public Location(String msg, AstNode node) {
            this.msg = msg;
            this.node = node;
        }
    }
    
}
