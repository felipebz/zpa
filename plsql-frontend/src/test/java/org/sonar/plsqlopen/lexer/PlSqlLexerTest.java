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
package org.sonar.plsqlopen.lexer;

import static com.sonar.sslr.test.lexer.LexerMatchers.hasComment;
import static com.sonar.sslr.test.lexer.LexerMatchers.hasToken;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.sonar.plsqlopen.lexer.PlSqlLexer;
import org.sonar.plsqlopen.squid.PlSqlConfiguration;
import org.sonar.plugins.plsqlopen.api.PlSqlTokenType;

import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.impl.Lexer;

public class PlSqlLexerTest {
    private static Lexer lexer;

    static {
        lexer = PlSqlLexer.create(new PlSqlConfiguration(StandardCharsets.UTF_8));
    }

    @Test
    public void multilineComment() {
        assertThat(lexer.lex("/* multine \n comment */"), hasComment("/* multine \n comment */"));
        assertThat(lexer.lex("/**/"), hasComment("/**/"));
    }

    @Test
    public void inlineComment() {
        assertThat(lexer.lex("before -- inline \n new line"), hasComment("-- inline "));
        assertThat(lexer.lex("--"), hasComment("--"));
    }
    
    @Test
    public void simpleStringLiteral() {
        assertThatIsToken("'Test'", PlSqlTokenType.STRING_LITERAL);
    }
    
    @Test
    public void simpleStringLiteralWithLineBreak() {
        assertThatIsToken("'First\nSecond'", PlSqlTokenType.STRING_LITERAL);
    }
    
    @Test
    public void simpleNationalCharsetStringLiteral() {
        assertThatIsToken("n'Test'", PlSqlTokenType.STRING_LITERAL);
    }
    
    @Test
    public void stringLiteralWithDoubleQuotationMarks() {
        assertThatIsToken("'I''m a string'", PlSqlTokenType.STRING_LITERAL);
    }
    
    @Test
    public void stringLiteralWithUserDefinedDelimiters() {
        assertThatIsToken("q'!I'm a string!'", PlSqlTokenType.STRING_LITERAL);
        assertThatIsToken("q'[I'm a string]'", PlSqlTokenType.STRING_LITERAL);
        assertThatIsToken("q'{I'm a string}'", PlSqlTokenType.STRING_LITERAL);
        assertThatIsToken("q'<I'm a string>'", PlSqlTokenType.STRING_LITERAL);
        assertThatIsToken("q'(I'm a string)'", PlSqlTokenType.STRING_LITERAL);
        
        assertThatIsToken("nq'!I'm a string!'", PlSqlTokenType.STRING_LITERAL);
        assertThatIsToken("nq'[I'm a string]'", PlSqlTokenType.STRING_LITERAL);
        assertThatIsToken("nq'{I'm a string}'", PlSqlTokenType.STRING_LITERAL);
        assertThatIsToken("nq'<I'm a string>'", PlSqlTokenType.STRING_LITERAL);
        assertThatIsToken("nq'(I'm a string)'", PlSqlTokenType.STRING_LITERAL);
    }
    
    @Test
    public void stringLiteralWithUserDefinedDelimitersAndLineBreak() {
        assertThatIsToken("q'!First\nSecond!'", PlSqlTokenType.STRING_LITERAL);
    }
    
    @Test
    public void simpleIntegerLiteral() {
        assertThatIsToken("6", PlSqlTokenType.INTEGER_LITERAL);
    }
    
    @Test
    public void simpleRealLiteral() {
        assertThatIsToken("3.14159", PlSqlTokenType.REAL_LITERAL);
    }
    
    @Test
    public void realLiteralWithDecimalPointOnly() {
        assertThatIsToken(".5", PlSqlTokenType.REAL_LITERAL);
    }
    
    @Test
    public void realLiteralWithWholePartOnly() {
        assertThatIsToken("25.", PlSqlTokenType.REAL_LITERAL);
    }
    
    @Test
    public void simpleScientificNotationLiteral() {
        assertThatIsToken("2E5", PlSqlTokenType.SCIENTIFIC_LITERAL);
    }
    
    @Test
    public void scientificNotationLiteralWithNegativeExponent() {
        assertThatIsToken("1.0E-7", PlSqlTokenType.SCIENTIFIC_LITERAL);
    }
    
    @Test
    public void scientificNotationWithLowercaseSuffix() {
        assertThatIsToken("9.5e-3", PlSqlTokenType.SCIENTIFIC_LITERAL);
    }
    
    @Test
    public void cornerCases() {
        assertThatIsNotToken("1..", PlSqlTokenType.REAL_LITERAL);
        assertThatIsNotToken("..2", PlSqlTokenType.REAL_LITERAL);
        
        assertThatIsNotToken("e1", PlSqlTokenType.SCIENTIFIC_LITERAL);
    }
    
    @Test
    public void dateLiteral() {
        assertThatIsToken("DATE '2015-01-01'", PlSqlTokenType.DATE_LITERAL);
        assertThatIsToken("date '2015-01-01'", PlSqlTokenType.DATE_LITERAL);
    }
    
    private void assertThatIsToken(String sourceCode, TokenType tokenType) {
        assertThat(lexer.lex(sourceCode), hasToken(sourceCode, tokenType));
    }
    
    private void assertThatIsNotToken(String sourceCode, TokenType tokenType) {
        assertThat(lexer.lex(sourceCode), not(hasToken(sourceCode, tokenType)));
    }
}
