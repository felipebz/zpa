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
package org.sonar.plsqlopen.symbols;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.symbol.NewSymbol;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.plsqlopen.TokenLocation;
import org.sonar.plsqlopen.checks.PlSqlCheck;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword;
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType;
import org.sonar.plugins.plsqlopen.api.symbols.Scope;
import org.sonar.plugins.plsqlopen.api.symbols.Symbol;
import org.sonar.plugins.plsqlopen.api.symbols.SymbolTableImpl;

import com.google.common.base.Preconditions;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;

public class SymbolVisitor extends PlSqlCheck {

    private static final AstNodeType[] scopeHolders = { 
            PlSqlGrammar.CREATE_PROCEDURE,
            PlSqlGrammar.PROCEDURE_DECLARATION,
            PlSqlGrammar.CREATE_FUNCTION,
            PlSqlGrammar.FUNCTION_DECLARATION,
            PlSqlGrammar.CREATE_PACKAGE,
            PlSqlGrammar.CREATE_PACKAGE_BODY,
            PlSqlGrammar.CREATE_TRIGGER,
            PlSqlGrammar.TYPE_CONSTRUCTOR,
            PlSqlGrammar.CREATE_TYPE,
            PlSqlGrammar.CREATE_TYPE_BODY,
            PlSqlGrammar.BLOCK_STATEMENT,
            PlSqlGrammar.FOR_STATEMENT,
            PlSqlGrammar.CURSOR_DECLARATION};
    
    private SymbolTableImpl symbolTable;
    private Scope currentScope;
    private NewSymbolTable symbolizable;
    private DefaultTypeSolver typeSolver;
    
    public SymbolVisitor() {
        // don't need context and inputFile
    }
    
    public SymbolVisitor(SensorContext context, InputFile inputFile, DefaultTypeSolver typeSolver) {
        symbolizable = context.newSymbolTable().onFile(inputFile);
        this.typeSolver = typeSolver;
    }

    @Override
    public void init() {
        subscribeTo(scopeHolders);
    }
    
    @Override
    public void visitFile(AstNode ast) {
        symbolTable = new SymbolTableImpl();
        
        // ast is null when the file has a parsing error
        if (ast != null) {
            visit(ast);
        }
        
        getContext().setSymbolTable(symbolTable);
    }
    
    @Override
    public void visitNode(AstNode astNode) {
        if (astNode.is(scopeHolders)) {
            getContext().setCurrentScope(symbolTable.getScopeFor(astNode));
        }
    }
    
    @Override
    public void leaveNode(AstNode astNode) {
        if (astNode.is(scopeHolders)) {
            getContext().setCurrentScope(getContext().getCurrentScope().outer());
        }
    }
    
    @Override
    public void leaveFile(AstNode node) {
        if (symbolizable != null) {
            for (Symbol symbol : symbolTable.getSymbols()) {
                AstNode symbolNode = symbol.declaration();
                
                TokenLocation symbolLocation = TokenLocation.from(symbolNode.getToken());
                NewSymbol newSymbol = symbolizable.newSymbol(symbolLocation.line(), symbolLocation.column(), 
                        symbolLocation.endLine(), symbolLocation.endColumn());
                
                for (AstNode usage : symbol.usages()) {
                    TokenLocation usageLocation = TokenLocation.from(usage.getToken());
                    newSymbol.newReference(usageLocation.line(), usageLocation.column(), usageLocation.endLine(), usageLocation.endColumn());
                }
            }
            symbolizable.save();
        }
        
        symbolTable = null;
        currentScope = null;
        symbolizable = null;
    }

    private void visit(AstNode ast) {
        visitNodeInternal(ast);
        visitChildren(ast);
        
        if (ast.is(scopeHolders)) {
            leaveScope();
        }
    }

    private void visitChildren(AstNode ast) {
        for (AstNode child : ast.getChildren()) {
            visit(child);
        }
    }

    private void visitNodeInternal(AstNode node) {
        
        if (node.getType() == PlSqlGrammar.VARIABLE_DECLARATION) {
            visitVariableDeclaration(node);
        
        } else if (node.getType() == PlSqlGrammar.VARIABLE_NAME) {
            visitVariableName(node);
        
        } else if (node.getType() == PlSqlGrammar.CURSOR_DECLARATION) {
            visitCursor(node);
        
        } else if (node.getType() == PlSqlGrammar.BLOCK_STATEMENT) {
            visitBlock(node);
        
        } else if (node.getType() == PlSqlGrammar.FOR_STATEMENT) {
            visitFor(node);
        
        } else if (node.getType() == PlSqlGrammar.PARAMETER_DECLARATION ||
                node.getType() == PlSqlGrammar.CURSOR_PARAMETER_DECLARATION) {
            visitParameterDeclaration(node);
        
        } else if (node.getType() == PlSqlGrammar.CREATE_PROCEDURE ||
                node.getType() == PlSqlGrammar.PROCEDURE_DECLARATION ||
                node.getType() == PlSqlGrammar.CREATE_FUNCTION ||
                node.getType() == PlSqlGrammar.FUNCTION_DECLARATION ||
                node.getType() == PlSqlGrammar.CREATE_TRIGGER ||
                node.getType() == PlSqlGrammar.CREATE_TYPE ||
                node.getType() == PlSqlGrammar.CREATE_TYPE_BODY ||
                node.getType() == PlSqlGrammar.TYPE_CONSTRUCTOR) {
            visitUnit(node);
        
        } else if (node.getType() == PlSqlGrammar.CREATE_PACKAGE || 
                node.getType() == PlSqlGrammar.CREATE_PACKAGE_BODY) {
            visitPackage(node);
        }
    }
    
    private void visitUnit(AstNode node) {
        boolean autonomousTransaction = node.select()
                .children(PlSqlGrammar.DECLARE_SECTION)
                .children(PlSqlGrammar.PRAGMA_DECLARATION)
                .children(PlSqlGrammar.AUTONOMOUS_TRANSACTION_PRAGMA).isNotEmpty();
        boolean exceptionHandler = node.select()
                .children(PlSqlGrammar.STATEMENTS_SECTION)
                .children(PlSqlGrammar.EXCEPTION_HANDLER).isNotEmpty();
        enterScope(node, autonomousTransaction, exceptionHandler);
    }
    
    private void visitPackage(AstNode node) {
        enterScope(node, false, false);
    }
    
    private void visitCursor(AstNode node) {
        AstNode identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME);
        createSymbol(identifier, Symbol.Kind.CURSOR, null);
        enterScope(node, null, null);
    }
    
    private void visitBlock(AstNode node) {
        boolean exceptionHandler = node.select()
                .children(PlSqlGrammar.STATEMENTS_SECTION)
                .children(PlSqlGrammar.EXCEPTION_HANDLER).isNotEmpty();
        enterScope(node, null, exceptionHandler);
    }
    
    private void visitFor(AstNode node) {
        enterScope(node, null, null);
        AstNode identifier = node.getFirstChild(PlSqlKeyword.FOR).getNextSibling();
        createSymbol(identifier, Symbol.Kind.VARIABLE, null);
    }
    
    private void visitVariableDeclaration(AstNode node) {
        AstNode identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME);
        AstNode datatype = node.getFirstChild(PlSqlGrammar.DATATYPE);
        
        PlSqlType type = solveType(datatype);
        createSymbol(identifier, Symbol.Kind.VARIABLE, type);
    }
    
    private void visitParameterDeclaration(AstNode node) {
        AstNode identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME);
        AstNode datatype = node.getFirstChild(PlSqlGrammar.DATATYPE);
        
        PlSqlType type = solveType(datatype);
        createSymbol(identifier, Symbol.Kind.PARAMETER, type).addModifiers(node.getChildren(PlSqlKeyword.IN, PlSqlKeyword.OUT));
    }
    
    private void visitVariableName(AstNode node) {
        AstNode identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME);
        if (identifier != null && currentScope != null) {
            Symbol symbol = currentScope.getSymbol(identifier.getTokenOriginalValue());
            if (symbol != null) {
                symbol.addUsage(identifier);
            }
        }
    }
    
    private Symbol createSymbol(AstNode identifier, Symbol.Kind kind, PlSqlType type) {
        return symbolTable.declareSymbol(identifier, kind, currentScope, type);
    }
    
    private void enterScope(AstNode node, Boolean autonomousTransaction, Boolean exceptionHandler) {
        boolean autonomous = false;
        
        if (autonomousTransaction != null) {
            autonomous = autonomousTransaction;  
        } else if (currentScope != null) {
            autonomous = currentScope.isAutonomousTransaction();
        }
        
        boolean exception = false;
        
        if (currentScope != null) {
            exception = currentScope.hasExceptionHandler() ? true : Boolean.TRUE.equals(exceptionHandler);
        } else if (exceptionHandler != null) {
            exception = exceptionHandler;  
        }
        
        currentScope = new Scope(currentScope, node, autonomous, exception);
        symbolTable.addScope(currentScope);
    }
    
    private void leaveScope() {
        Preconditions.checkState(currentScope != null, "Current scope should never be null when calling method \"leaveScope\"");
        currentScope = currentScope.outer();
    }
    
    private PlSqlType solveType(AstNode node) {
        PlSqlType type = null;
        if (typeSolver != null) {
            type = typeSolver.solve(node);
        }
        return type;
    }

}