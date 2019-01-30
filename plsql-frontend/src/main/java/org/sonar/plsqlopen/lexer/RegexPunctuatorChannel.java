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
package org.sonar.plsqlopen.lexer;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sonar.sslr.channel.Channel;
import org.sonar.sslr.channel.CodeReader;

import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.impl.Lexer;
import com.sonar.sslr.impl.LexerException;

public class RegexPunctuatorChannel extends Channel<Lexer> {
    private final Map<TokenType, Matcher> tokenMatchers = new LinkedHashMap<>();
    private final StringBuilder tmpBuilder = new StringBuilder();
    private final Token.Builder tokenBuilder = Token.builder();

    private static class PunctuatorComparator implements Comparator<TokenType> {

        @Override
        public int compare(TokenType a, TokenType b) {
            if (a.getValue().length() == b.getValue().length()) {
                return 0;
            }
            return a.getValue().length() > b.getValue().length() ? -1 : 1;
        }

    }

    public RegexPunctuatorChannel(TokenType... punctuators) {
        Arrays.sort(punctuators, new PunctuatorComparator());

        for (TokenType punctuator : punctuators) {
            tokenMatchers.put(punctuator, Pattern.compile(punctuator.getValue()).matcher(""));
        }
    }

    @Override
    public boolean consume(CodeReader code, Lexer lexer) {
        for (Map.Entry<TokenType, Matcher> tokenMatcher : tokenMatchers.entrySet()) {
            TokenType punctuator = tokenMatcher.getKey();
            Matcher matcher = tokenMatcher.getValue();
            try {
                if (code.popTo(matcher, tmpBuilder) > 0) {
                    String value = tmpBuilder.toString();

                    Token token = tokenBuilder
                        .setType(punctuator)
                        .setValueAndOriginalValue(value)
                        .setURI(lexer.getURI())
                        .setLine(code.getPreviousCursor().getLine())
                        .setColumn(code.getPreviousCursor().getColumn())
                        .build();

                    lexer.addToken(token);

                    tmpBuilder.delete(0, tmpBuilder.length());
                    return true;
                }
            } catch (StackOverflowError e) {
                throw new LexerException("The regular expression " + punctuator.getValue() + " has led to a stack overflow error.", e);
            }
        }
        return false;
    }
}
