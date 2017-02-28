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
package org.sonar.plsqlopen.highlight;

import java.io.File;
import java.util.List;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.plsqlopen.TokenLocation;
import org.sonar.plsqlopen.lexer.PlSqlLexer;
import org.sonar.plsqlopen.squid.PlSqlConfiguration;
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword;
import org.sonar.plugins.plsqlopen.api.PlSqlTokenType;

import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.api.Trivia;
import com.sonar.sslr.impl.Lexer;

public class PlSqlHighlighter {

    private Lexer lexer;
    private NewHighlighting highlighting;
    
    public PlSqlHighlighter(PlSqlConfiguration conf){
        this.lexer = PlSqlLexer.create(conf);
    }
    
    public void highlight(SensorContext context, InputFile inputFile) {
        highlighting = context.newHighlighting().onFile(inputFile);
        File file = inputFile.file();
        List<Token> tokens = lexer.lex(file);
        doHighlight(tokens);
    }
    
    private void doHighlight(List<Token> tokens) {
        highlightStringsAndKeywords(tokens);
        highlightComments(tokens);
        highlighting.save();
    }
    
    private void highlightComments(List<Token> tokens) {
        TypeOfText code;
        for (Token token : tokens) {
            if (token.getTrivia().isEmpty()) {
                continue;
            }
            
            for (Trivia trivia : token.getTrivia()) {
                if (trivia.getToken().getValue().startsWith("/**")) {
                    code = TypeOfText.STRUCTURED_COMMENT;
                } else {
                    code = TypeOfText.COMMENT;
                }
                highlight(trivia.getToken(), code);
            }
        }
    }
    
    private void highlightStringsAndKeywords(List<Token> tokens) {
        for (Token token : tokens) {
            if (isLiteral(token.getType())) {
                highlight(token, TypeOfText.STRING);
            }
            if (isKeyword(token.getType())) {
                highlight(token, TypeOfText.KEYWORD);
            }
        }
    }
    
    private void highlight(Token token, TypeOfText code) {
        TokenLocation location = TokenLocation.from(token);
        highlighting.highlight(location.line(), location.column(), location.endLine(), location.endColumn(), code);
    }
    
    public boolean isLiteral(TokenType type) {
        for (TokenType literalType : PlSqlTokenType.values()) {
            if (literalType.equals(type)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isKeyword(TokenType type) {
        for (TokenType keywordType : PlSqlKeyword.values()) {
            if (keywordType.equals(type)) {
                return true;
            }
        }
        return false;
    }

}
