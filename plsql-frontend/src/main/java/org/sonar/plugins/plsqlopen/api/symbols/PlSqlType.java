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
package org.sonar.plugins.plsqlopen.api.symbols;

import com.google.common.annotations.VisibleForTesting;
import com.sonar.sslr.api.AstNode;

public class PlSqlType {
    
    private Type type;
    private AstNode node;
    
    public PlSqlType(Type type) {
        this(type, null);
    }

    public PlSqlType(Type type, AstNode node) {
        this.type = type;
        this.node = node;
    }
    
    public boolean isCharacter() {
        return type == Type.CHARACTER;
    }
    
    public boolean isNumeric() {
        return type == Type.NUMERIC;
    }
    
    public boolean isUnknown() {
        return type == Type.UNKNOWN;
    }
    
    public AstNode node() {
        return node;
    }
    
    @VisibleForTesting
    public Type type() {
        return type;
    }
    
    @Override
    public String toString() {
        return "PlSqlType{" +
                "type=" + type +
                ", node=" + node + "}";
    }
    
    public enum Type {
        UNKNOWN,
        CHARACTER,
        NUMERIC,
        DATE,
        LOB,
        BOOLEAN,
        ROWTYPE
    }

}
