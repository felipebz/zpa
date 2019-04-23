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
package org.sonar.plugins.plsqlopen.api;

import javax.annotation.Nullable;

import org.sonar.plsqlopen.metadata.FormsMetadata;
import org.sonar.plugins.plsqlopen.api.symbols.Scope;
import org.sonar.plugins.plsqlopen.api.symbols.SymbolTable;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.RecognitionException;

public class PlSqlVisitorContext {

    private final AstNode rootTree;
    private final PlSqlFile plSqlFile;
    private final RecognitionException parsingException;
    private SymbolTable symbolTable;
    private Scope scope;
    private FormsMetadata formsMetadata;

    public PlSqlVisitorContext(AstNode rootTree, PlSqlFile plsqlFile, @Nullable FormsMetadata metadata) {
        this(rootTree, plsqlFile, null, metadata);
    }

    public PlSqlVisitorContext(PlSqlFile plsqlFile, RecognitionException parsingException, @Nullable FormsMetadata metadata) {
        this(null, plsqlFile, parsingException, metadata);
    }

    private PlSqlVisitorContext(@Nullable AstNode rootTree, PlSqlFile plsqlFile, @Nullable RecognitionException parsingException, @Nullable FormsMetadata metadata) {
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
    
}
