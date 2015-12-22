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
import org.sonar.plugins.plsqlopen.api.symbols.Scope;
import org.sonar.plugins.plsqlopen.api.symbols.SymbolTable;
import org.sonar.squidbridge.api.CodeCheck;
import org.sonar.squidbridge.api.CodeVisitor;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;

public interface PlSqlVisitorContext {
    
    SymbolTable getSymbolTable();
    
    File getFile();
    
    void setSymbolTable(SymbolTable symbolTable);
    
    Symbolizable getSymbolizable();
    
    void setCurrentScope(Scope scope);
    
    Scope getCurrentScope();

    void createLineViolation(CodeCheck check, String message, AstNode node, Object... messageParameters);
    
    void createLineViolation(CodeCheck check, String message, Token token, Object... messageParameters);
    
    void createLineViolation(CodeCheck check, String message, int line, Object... messageParameters);
    
    void createViolation(CodeVisitor check, String message, AstNode node, Object... messageParameters);

    void createViolation(CodeVisitor check, String message, AstNode node, List<Location> secondary,
            Object... messageParameters);

    class Location {
        public final String msg;
        public final AstNode node;

        public Location(String msg, AstNode node) {
            this.msg = msg;
            this.node = node;
        }
    }
    
}
