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

import java.util.regex.Pattern;

import com.sonar.sslr.api.Token;

public class TokenLocation { 
    private static final Pattern pattern = Pattern.compile("\r?\n|\r");
    
    private int line;
    private int column;
    private int endLine;
    private int endColumn;
    
    private TokenLocation(int line, int column, int endLine, int endColumn) {
        this.line = line;
        this.column = column;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }
    
    public int line() {
        return line;
    }
    
    public int column() {
        return column;
    }
    
    public int endLine() {
        return endLine;
    }
    
    public int endColumn() {
        return endColumn;
    }
    
    public static TokenLocation from(Token token) {
        int lineCount = 0;
        int lastLineLength = 0;
        
        if (token.getValue().contains("\n") || token.getValue().contains("\r")) {
            String[] lines = pattern.split(token.getValue());
            lineCount = lines.length;
            lastLineLength = lines[lines.length - 1].length();;
        } else {
            lineCount = 1;
        }
        
        int endLineOffset = token.getColumn() + token.getValue().length();
        int endLine = token.getLine() + lineCount - 1;
        if (endLine != token.getLine()) {
            endLineOffset = lastLineLength;
        }
        return new TokenLocation(token.getLine(), token.getColumn(), endLine, endLineOffset);
    }

}
