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
package org.sonar.plsqlopen.symbols;

import java.nio.charset.Charset;

import org.sonar.api.source.Symbolizable;
import org.sonar.api.source.Symbolizable.SymbolTableBuilder;
import org.sonar.plsqlopen.SourceFileOffsets;
import org.sonar.plsqlopen.checks.AbstractBaseCheck;
import org.sonar.plsqlopen.squid.CharsetAwareVisitor;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword;
import org.sonar.plugins.plsqlopen.api.symbols.Scope;
import org.sonar.plugins.plsqlopen.api.symbols.Symbol;
import org.sonar.plugins.plsqlopen.api.symbols.SymbolTableImpl;

import com.google.common.base.Preconditions;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;

public class SymbolVisitor extends AbstractBaseCheck implements CharsetAwareVisitor {

    private static final AstNodeType[] scopeHolders = { 
            PlSqlGrammar.CREATE_PROCEDURE,
            PlSqlGrammar.PROCEDURE_DECLARATION,
            PlSqlGrammar.CREATE_FUNCTION,
            PlSqlGrammar.FUNCTION_DECLARATION,
            PlSqlGrammar.CREATE_PACKAGE,
            PlSqlGrammar.CREATE_PACKAGE_BODY,
            PlSqlGrammar.BLOCK_STATEMENT,
            PlSqlGrammar.FOR_STATEMENT};
    
    private SymbolTableImpl symbolTable;
    private Scope currentScope;
    private Charset charset;
    private SourceFileOffsets offsets;
    private Symbolizable symbolizable;
    private SymbolTableBuilder symbolTableBuilder;
    
    @Override
    public void setCharset(Charset charset) {
        this.charset = charset;
    }
    
    @Override
    public void visitFile(AstNode ast) {
        symbolTable = new SymbolTableImpl();
        offsets = new SourceFileOffsets(getPlSqlContext().getFile(), charset);
        symbolizable = getPlSqlContext().getSymbolizable();
        if (symbolizable != null) {
            symbolTableBuilder = symbolizable.newSymbolTableBuilder();
        }
        
        // ast is null when the file has a parsing error
        if (ast != null) {
            visit(ast);
        }
        
        getPlSqlContext().setSymbolTable(symbolTable);
    }
    
    @Override
    public void leaveFile(AstNode node) {
        if (symbolizable != null) {
            for (Symbol symbol : symbolTable.getSymbols()) {
                AstNode symbolNode = symbol.declaration();
                
                org.sonar.api.source.Symbol newSymbol = symbolTableBuilder.newSymbol(
                        offsets.startOffset(symbolNode.getToken()), offsets.endOffset(symbolNode.getToken()));
                
                for (AstNode usage : symbol.usages()) {
                    symbolTableBuilder.newReference(newSymbol, offsets.startOffset(usage.getToken()));
                }
            }
            symbolizable.setSymbolTable(symbolTableBuilder.build());
        }
        
        symbolTable = null;
        currentScope = null;
        offsets = null;
        symbolizable = null;
        symbolTableBuilder = null;
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
        if (node.is(scopeHolders)) {

            enterScope(node);

            if (node.is(PlSqlGrammar.FOR_STATEMENT)) {
                AstNode identifier = node.getFirstChild(PlSqlKeyword.FOR).getNextSibling();
                createSymbol(identifier, Symbol.Kind.VARIABLE);
            }

        } else if (currentScope != null && node.is(PlSqlGrammar.VARIABLE_DECLARATION, PlSqlGrammar.VARIABLE_NAME)) {

            AstNode identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME);
            if (node.is(PlSqlGrammar.VARIABLE_DECLARATION)) {
                createSymbol(identifier, Symbol.Kind.VARIABLE);
            } else if (node.is(PlSqlGrammar.VARIABLE_NAME)) {
                if (identifier != null) {
                    Symbol symbol = currentScope.getSymbol(identifier.getTokenOriginalValue());
                    if (symbol != null) {
                        symbol.addUsage(identifier);
                    }
                }
            }
        }
    }
    
    private Symbol createSymbol(AstNode identifier, Symbol.Kind kind) {
        return symbolTable.declareSymbol(identifier, kind, currentScope);
    }
    
    private void enterScope(AstNode node) {
        currentScope = new Scope(currentScope, node);
        symbolTable.addScope(currentScope);
    }
    
    private void leaveScope() {
        Preconditions.checkState(currentScope != null, "Current scope should never be null when calling method \"leaveScope\"");
        currentScope = currentScope.outer();
    }

}
