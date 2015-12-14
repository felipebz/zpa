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
package org.sonar.plsqlopen.highlight;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

import org.sonar.api.source.Highlightable;
import org.sonar.plsqlopen.SourceFileOffsets;
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
    private Charset charset;
    
    public PlSqlHighlighter(PlSqlConfiguration conf){
        this.lexer = PlSqlLexer.create(conf);
        this.charset = conf.getCharset();
    }
    
    public void highlight(Highlightable highlightable, File file) {
        SourceFileOffsets offsets = new SourceFileOffsets(file, charset);
        List<Token> tokens = lexer.lex(file);
        doHighlight(highlightable, tokens, offsets);
    }
    
    private void doHighlight(Highlightable highlightable, List<Token> tokens, SourceFileOffsets offsets) {
        Highlightable.HighlightingBuilder highlighting = highlightable.newHighlighting();
        highlightStringsAndKeywords(highlighting, tokens, offsets);
        highlightComments(highlighting, tokens, offsets);
        highlighting.done();
    }
    
    private static void highlightComments(Highlightable.HighlightingBuilder highlighting, List<Token> tokens, SourceFileOffsets offsets) {
        String code;
        for (Token token : tokens) {
            if (!token.getTrivia().isEmpty()) {
                for (Trivia trivia : token.getTrivia()) {
                    if (trivia.getToken().getValue().startsWith("/**")) {
                        code = "j";
                    } else {
                        code = "cd";
                    }
                    highlight(highlighting, offsets.startOffset(trivia.getToken()), offsets.endOffset(trivia.getToken()), code);
                }
            }
        }
    }
    
    private void highlightStringsAndKeywords(Highlightable.HighlightingBuilder highlighting, List<Token> tokens, SourceFileOffsets offsets) {
        for (Token token : tokens) {
            if (isLiteral(token.getType())) {
                highlight(highlighting, offsets.startOffset(token), offsets.endOffset(token), "s");
            }
            if (isKeyword(token.getType())) {
                highlight(highlighting, offsets.startOffset(token), offsets.endOffset(token), "k");
            }
        }
    }
    
    private static void highlight(Highlightable.HighlightingBuilder highlighting, int startOffset, int endOffset, String code) {
        highlighting.highlight(startOffset, endOffset, code);
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
