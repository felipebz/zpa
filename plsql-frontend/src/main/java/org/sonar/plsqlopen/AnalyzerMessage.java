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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.CodeVisitor;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;

public class AnalyzerMessage extends CheckMessage {

    @Nullable
    private TextSpan textSpan;
    private final List<AnalyzerMessage> secondaryLocations = new ArrayList<>();
    
    public AnalyzerMessage(CodeVisitor check, String message, int line, Object... messageArguments) {
        this(check, message, line > 0 ? new TextSpan(line, -1, line, -1) : null, messageArguments);
    }
    
    public AnalyzerMessage(CodeVisitor check, String message, @Nullable TextSpan textSpan, Object... messageArguments) {
        super(check, message, messageArguments);
        this.textSpan = textSpan;
        if (textSpan != null) {
            setLine(textSpan.startLine);
        }
    }
    
    @Nullable
    public TextSpan getLocation() {
        return textSpan;
    }
    
    public List<AnalyzerMessage> getSecondaryLocations() {
        return secondaryLocations;
    }
    
    public void addSecondaryLocation(AnalyzerMessage location) {
        secondaryLocations.add(location);
    }
    
    public static final class TextSpan {
        public final int startLine;
        public final int startCharacter;
        public final int endLine;
        public final int endCharacter;

        public TextSpan(int startLine, int startCharacter, int endLine, int endCharacter) {
            this.startLine = startLine;
            this.startCharacter = startCharacter;
            this.endLine = endLine;
            this.endCharacter = endCharacter;
        }

        @Override
        public String toString() {
            return "(" + startLine + ":" + startCharacter + ")-(" + endLine + ":" + endCharacter + ")";
        }
    }

    public static AnalyzerMessage.TextSpan textSpanFor(AstNode node) {
        Token firstToken = node.getToken();
        Token lastToken = node.getLastToken();
        return new AnalyzerMessage.TextSpan(
                firstToken.getLine(),
                firstToken.getColumn(),
                lastToken.getLine(),
                lastToken.getColumn() + lastToken.getOriginalValue().length());
    }
    
}
