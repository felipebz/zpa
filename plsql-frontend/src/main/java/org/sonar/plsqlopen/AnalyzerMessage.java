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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import org.sonar.plsqlopen.checks.PlSqlVisitor;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;

public class AnalyzerMessage {

    @Nullable
    private TextSpan textSpan;
    private final List<AnalyzerMessage> secondaryLocations = new ArrayList<>();
    private PlSqlVisitor check;
    private String message;
    private Object[] messageArguments;
    private Double cost;
    private Integer line;
    
    public AnalyzerMessage(PlSqlVisitor check, String message, int line, Object... messageArguments) {
        this(check, message, line > 0 ? new TextSpan(line, -1, line, -1) : null, messageArguments);
    }
    
    public AnalyzerMessage(PlSqlVisitor check, String message, @Nullable TextSpan textSpan, Object... messageArguments) {
        this.check = check;
        this.message = message;
        this.textSpan = textSpan;
        this.messageArguments = messageArguments;
        if (textSpan != null) {
            setLine(textSpan.startLine);
        }
    }
    
    public PlSqlVisitor getCheck() {
        return check;
    }

    public String getDefaultMessage() {
        return message;
    }

    public Object[] getMessageArguments() {
        return messageArguments;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public Double getCost() {
        return cost;
    }
    
    public void setLine(int line) {
        this.line = line;
    }

    public Integer getLine() {
        return line;
    }

    public String getText(Locale locale) {
        if (messageArguments.length == 0) {
            return message;
        } else {
            return MessageFormat.format(message, messageArguments);
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
