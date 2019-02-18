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
package org.sonar.plugins.plsqlopen.api.squid;

import com.sonar.sslr.api.AstNode;

import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType;
import org.sonar.plugins.plsqlopen.api.symbols.Symbol;

public class SemanticAstNode extends AstNode {

    private Symbol symbol;
    private PlSqlType plSqlType = PlSqlType.UNKNOWN;

    public SemanticAstNode(AstNode astNode) {
        super(astNode.getType(), astNode.getName(), astNode.getToken());
        super.setFromIndex(astNode.getFromIndex());
        super.setToIndex(astNode.getToIndex());
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;

        for (AstNode node : getChildren()) {
            ((SemanticAstNode)node).setSymbol(symbol);
        }
    }

    public PlSqlType getPlSqlType() {
        if (symbol != null) {
            return symbol.type();
        }
        return plSqlType;
    }

    public void setPlSqlType(PlSqlType plSqlType) {
        this.plSqlType = plSqlType;
    }
}
