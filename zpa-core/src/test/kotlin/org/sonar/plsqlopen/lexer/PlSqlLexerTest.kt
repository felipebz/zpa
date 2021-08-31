/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
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
package org.sonar.plsqlopen.lexer

import com.sonar.sslr.api.Token
import com.sonar.sslr.api.TokenType
import com.sonar.sslr.test.lexer.LexerMatchers.hasComment
import com.sonar.sslr.test.lexer.LexerMatchers.hasToken
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertThat
import org.junit.jupiter.api.Test
import org.sonar.plsqlopen.parser.PlSqlParser
import org.sonar.plsqlopen.squid.PlSqlConfiguration
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlPunctuator
import org.sonar.plugins.plsqlopen.api.PlSqlTokenType
import java.nio.charset.StandardCharsets

class PlSqlLexerTest {

    private val lexer = PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8))

    @Test
    fun multilineComment() {
        assertThat<List<Token>>(lexer.lex("/* multine \n comment */"), hasComment("/* multine \n comment */"))
        assertThat<List<Token>>(lexer.lex("/**/"), hasComment("/**/"))
    }

    @Test
    fun inlineComment() {
        assertThat<List<Token>>(lexer.lex("before -- inline \n new line"), hasComment("-- inline "))
        assertThat<List<Token>>(lexer.lex("--"), hasComment("--"))
    }

    @Test
    fun simpleStringLiteral() {
        assertThatIsToken("'Test'", PlSqlTokenType.STRING_LITERAL)
    }

    @Test
    fun simpleStringLiteralWithLineBreak() {
        assertThatIsToken("'First\nSecond'", PlSqlTokenType.STRING_LITERAL)
    }

    @Test
    fun simpleNationalCharsetStringLiteral() {
        assertThatIsToken("n'Test'", PlSqlTokenType.STRING_LITERAL)
    }

    @Test
    fun stringLiteralWithDoubleQuotationMarks() {
        assertThatIsToken("'I''m a string'", PlSqlTokenType.STRING_LITERAL)
    }

    @Test
    fun stringLiteralWithUserDefinedDelimiters() {
        assertThatIsToken("q'!I'm a string!'", PlSqlTokenType.STRING_LITERAL)
        assertThatIsToken("q'[I'm a string]'", PlSqlTokenType.STRING_LITERAL)
        assertThatIsToken("q'{I'm a string}'", PlSqlTokenType.STRING_LITERAL)
        assertThatIsToken("q'<I'm a string>'", PlSqlTokenType.STRING_LITERAL)
        assertThatIsToken("q'(I'm a string)'", PlSqlTokenType.STRING_LITERAL)

        assertThatIsToken("nq'!I'm a string!'", PlSqlTokenType.STRING_LITERAL)
        assertThatIsToken("nq'[I'm a string]'", PlSqlTokenType.STRING_LITERAL)
        assertThatIsToken("nq'{I'm a string}'", PlSqlTokenType.STRING_LITERAL)
        assertThatIsToken("nq'<I'm a string>'", PlSqlTokenType.STRING_LITERAL)
        assertThatIsToken("nq'(I'm a string)'", PlSqlTokenType.STRING_LITERAL)

        assertThatIsToken("q'!I'm a string q'[with nesting]'!'", PlSqlTokenType.STRING_LITERAL)
    }

    @Test
    fun stringLiteralWithUserDefinedDelimitersAndLineBreak() {
        assertThatIsToken("q'!First\nSecond!'", PlSqlTokenType.STRING_LITERAL)
    }

    @Test
    fun simpleIntegerLiteral() {
        assertThatIsToken("6", PlSqlTokenType.INTEGER_LITERAL)
    }

    @Test
    fun simpleNumberLiteral() {
        assertThatIsToken("6d", PlSqlTokenType.NUMBER_LITERAL)
        assertThatIsToken("6f", PlSqlTokenType.NUMBER_LITERAL)
        assertThatIsNotToken("6e", PlSqlTokenType.NUMBER_LITERAL)
    }

    @Test
    fun simpleRealLiteral() {
        assertThatIsToken("3.14159", PlSqlTokenType.NUMBER_LITERAL)
        assertThatIsToken("3.14159f", PlSqlTokenType.NUMBER_LITERAL)
        assertThatIsToken("3.14159d", PlSqlTokenType.NUMBER_LITERAL)
    }

    @Test
    fun realLiteralWithDecimalPointOnly() {
        assertThatIsToken(".5", PlSqlTokenType.NUMBER_LITERAL)
    }

    @Test
    fun realLiteralWithWholePartOnly() {
        assertThatIsToken("25.", PlSqlTokenType.NUMBER_LITERAL)
    }

    @Test
    fun simpleScientificNotationLiteral() {
        assertThatIsToken("2E5", PlSqlTokenType.NUMBER_LITERAL)
        assertThatIsToken("2E5f", PlSqlTokenType.NUMBER_LITERAL)
    }

    @Test
    fun scientificNotationLiteralWithNegativeExponent() {
        assertThatIsToken("1.0E-7", PlSqlTokenType.NUMBER_LITERAL)
    }

    @Test
    fun scientificNotationWithLowercaseSuffix() {
        assertThatIsToken("9.5e-3", PlSqlTokenType.NUMBER_LITERAL)
    }

    @Test
    fun cornerCases() {
        assertThatIsNotToken("1..", PlSqlTokenType.NUMBER_LITERAL)
        assertThatIsNotToken("..2", PlSqlTokenType.NUMBER_LITERAL)

        assertThatIsNotToken("e1", PlSqlTokenType.NUMBER_LITERAL)
    }

    @Test
    fun dateLiteral() {
        assertThatIsToken("DATE '2015-01-01'", PlSqlTokenType.DATE_LITERAL)
        assertThatIsToken("date '2015-01-01'", PlSqlTokenType.DATE_LITERAL)
        assertThatIsToken("date'2015-01-01'", PlSqlTokenType.DATE_LITERAL)
        assertThatIsToken("date    '2015-01-01'", PlSqlTokenType.DATE_LITERAL)
    }

    private fun assertThatIsToken(sourceCode: String, tokenType: TokenType) {
        assertThat<List<Token>>(lexer.lex(sourceCode), hasToken(sourceCode, tokenType))
    }

    private fun assertThatIsNotToken(sourceCode: String, tokenType: TokenType) {
        assertThat<List<Token>>(lexer.lex(sourceCode), not<List<Token>>(hasToken(sourceCode, tokenType)))
    }

    @Test
    fun checkLimitsOfStringLiteralWithUserDefinedDelimiters() {
        val p = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false))
        p.setRootRule(p.grammar.rule(PlSqlGrammar.BLOCK_STATEMENT))

        val node = p.parse("begin\n" +
                "x := q'!select 1 from dual!';\n" +
                "y := 'Another unrelated string!';\n" +
                "end;")

        assertThat(node.getDescendants(PlSqlGrammar.ASSIGNMENT_STATEMENT).size, `is`(2))
    }

    @Test
    fun checkLimitsOfStringLiteralWithUserDefinedDelimiters2() {
        val p = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false))
        p.setRootRule(p.grammar.rule(PlSqlGrammar.BLOCK_STATEMENT))

        val node = p.parse("begin\n" +
                "x := q'[" +
                "replace(foo, ')');" +
                "]';\n" +
                "end;")

        assertThat(node.getDescendants(PlSqlGrammar.ASSIGNMENT_STATEMENT).size, `is`(1))
    }

    @Test
    fun punctuatorWithSpace() {
        assertThatIsToken("<>", PlSqlPunctuator.NOTEQUALS)
        assertThatIsToken("<  >", PlSqlPunctuator.NOTEQUALS)
    }

    @Test
    fun conditionalCompilation() {
        val p = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false))
        p.setRootRule(p.grammar.rule(PlSqlGrammar.BLOCK_STATEMENT))

        val node = p.parse("begin\n" +
                "\$if $\$var \$then\n" +
                "  null;\n" +
                "\$end\n" +
                "end;")

        assertThat(node.getDescendants(PlSqlGrammar.NULL_STATEMENT).size, `is`(1))
    }

    @Test
    fun conditionalCompilationWithElse() {
        val p = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false))
        p.setRootRule(p.grammar.rule(PlSqlGrammar.BLOCK_STATEMENT))

        val node = p.parse("begin\n" +
                "\$if $\$var \$then\n" +
                "  null;\n" +
                "\$else\n" +
                "  null;\n" +
                "\$end\n" +
                "end;")

        assertThat(node.getDescendants(PlSqlGrammar.NULL_STATEMENT).size, `is`(1))
    }


    @Test
    fun ignoreErrorPreProcessor() {
        val p = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false))
        p.setRootRule(p.grammar.rule(PlSqlGrammar.BLOCK_STATEMENT))

        val node = p.parse("begin\n" +
                "\$if DBMS_DB_VERSION.VER_LE_10_1 \$then\n" +
                "  \$error 'unsupported database release' \$end\n" +
                "\$end\n" +
                "null;\n" +
                "end;")

        assertThat(node.getDescendants(PlSqlGrammar.NULL_STATEMENT).size, `is`(1))
    }
}
