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

import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.and;
import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.commentRegexp;
import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.o2n;
import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.or;
import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.regexp;
import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.g;

import org.sonar.plsqlopen.squid.PlSqlConfiguration;
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword;
import org.sonar.plugins.plsqlopen.api.PlSqlPunctuator;
import org.sonar.plugins.plsqlopen.api.PlSqlTokenType;

import com.sonar.sslr.impl.Lexer;
import com.sonar.sslr.impl.channel.BlackHoleChannel;
import com.sonar.sslr.impl.channel.IdentifierAndKeywordChannel;
import com.sonar.sslr.impl.channel.UnknownCharacterChannel;

public class PlSqlLexer {
    public static final String INLINE_COMMENT = "--[^\\n\\r]*+";
    public static final String MULTILINE_COMMENT = "/\\*[\\s\\S]*?\\*\\/";
    public static final String COMMENT = "(?:" + INLINE_COMMENT + "|" + MULTILINE_COMMENT + ")";
    
    public static final String INTEGER_LITERAL = "(?:\\d++)";
    
    // TODO: improve this regex
    public static final String REAL_LITERAL = "(?:"
            + "("
            + "\\d*+(?!\\.\\.)\\.\\d++"
            + "|\\d++(?!\\.\\.)\\.\\d*+)"
            + ")";
    
    public static final String SCIENTIFIC_LITERAL = "(?:"
            + "\\d++(\\.\\d*+)?[Ee](\\+|-)?\\d++"
            + ")";
    
    private static final String CUSTOM_DELIMITER_START = "[^\\s{\\[<\\(]"; // any except spacing
    private static final String CUSTOM_DELIMITER_END = "(\\5|}|]|>|\\))"; // same as the start, }, ], > or )
    public static final String STRING_LITERAL = "(?is)(?:"
            + or("'([^']|'')*+'", // simple text literal
                 "n?q?'" + or(g(g(CUSTOM_DELIMITER_START) +  ".*?(" + CUSTOM_DELIMITER_END + "')"),
                              g("\\(.*?(\\)')"),
                              g("\\[.*?(\\]')"),
                              g("<.*?(>')"),
                              g("\\{.*?(\\}')"))) // text with user-defined delimiter
            + ")";
    
    public static final String DATE_LITERAL = "(?i)(?:DATE '\\d{4}-\\d{2}-\\d{2}')";
    
    public static final String SIMPLE_IDENTIFIER = and("[\\w\\p{L}]", o2n("[\\w\\p{L}#$]"));
    
    public static final String QUOTED_IDENTIFIER = "\".+?\"";
    
    private PlSqlLexer() {
    }

    public static Lexer create(PlSqlConfiguration conf) {

        return Lexer
                .builder()
                .withCharset(conf.getCharset())
                .withFailIfNoChannelToConsumeOneCharacter(true)
                .withChannel(new BlackHoleChannel("\\s(?!&)"))
                .withChannel(commentRegexp(COMMENT))
                .withChannel(regexp(PlSqlTokenType.SCIENTIFIC_LITERAL, SCIENTIFIC_LITERAL))
                .withChannel(regexp(PlSqlTokenType.REAL_LITERAL, REAL_LITERAL))
                .withChannel(regexp(PlSqlTokenType.INTEGER_LITERAL, INTEGER_LITERAL))
                .withChannel(regexp(PlSqlTokenType.STRING_LITERAL, STRING_LITERAL))
                .withChannel(regexp(PlSqlTokenType.DATE_LITERAL, DATE_LITERAL))
                .withChannel(new IdentifierAndKeywordChannel(or(SIMPLE_IDENTIFIER, QUOTED_IDENTIFIER), false, PlSqlKeyword.values()))
                .withChannel(new RegexPunctuatorChannel(PlSqlPunctuator.values()))
                .withChannel(new BlackHoleChannel(and("\\s&&?", SIMPLE_IDENTIFIER)))
                .withChannel(new UnknownCharacterChannel())
                .build();
    }
}
