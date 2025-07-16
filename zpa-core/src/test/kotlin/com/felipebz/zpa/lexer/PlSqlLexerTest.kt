/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
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
package com.felipebz.zpa.lexer

import com.felipebz.flr.api.TokenType
import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class PlSqlLexerTest {

    private val lexer = PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8))

    @Test
    fun multilineComment() {
        assertThat(lexer.lex("/* multine \n comment */")).hasComment("/* multine \n comment */")
        assertThat(lexer.lex("/**/")).hasComment("/**/")
    }

    @Test
    fun inlineComment() {
        assertThat(lexer.lex("before -- inline \n new line")).hasComment("-- inline ")
        assertThat(lexer.lex("--")).hasComment("--")
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
        assertThatIsToken("n'Test string'", PlSqlTokenType.STRING_LITERAL)
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

    @Test
    fun timestampLiteral() {
        assertThatIsToken("TIMESTAMP '2015-01-01 01:01:01'", PlSqlTokenType.TIMESTAMP_LITERAL)
        assertThatIsToken("timestamp '2015-01-01 01:01:01'", PlSqlTokenType.TIMESTAMP_LITERAL)
        assertThatIsToken("timestamp'2015-01-01 01:01:01'", PlSqlTokenType.TIMESTAMP_LITERAL)
        assertThatIsToken("timestamp    '2015-01-01 01:01:01'", PlSqlTokenType.TIMESTAMP_LITERAL)
        assertThatIsToken("TIMESTAMP '2015-01-01 01:01:01.0'", PlSqlTokenType.TIMESTAMP_LITERAL)
        assertThatIsToken("TIMESTAMP '2015-01-01 01:01:01.123456789'", PlSqlTokenType.TIMESTAMP_LITERAL)
        assertThatIsToken("TIMESTAMP '2015-01-01 01:01:01 -3:00'", PlSqlTokenType.TIMESTAMP_LITERAL)
        assertThatIsToken("TIMESTAMP '2015-01-01 01:01:01 -03:00'", PlSqlTokenType.TIMESTAMP_LITERAL)
        assertThatIsToken("TIMESTAMP '2015-01-01 01:01:01 America/Sao_Paulo'", PlSqlTokenType.TIMESTAMP_LITERAL)
        assertThatIsToken("TIMESTAMP '2015-01-01 01:01:01 America/Sao_Paulo -03'", PlSqlTokenType.TIMESTAMP_LITERAL)
    }

    private fun assertThatIsToken(sourceCode: String, tokenType: TokenType) {
        assertThat(lexer.lex(sourceCode)).hasToken(sourceCode, tokenType)
    }

    private fun assertThatIsNotToken(sourceCode: String, tokenType: TokenType) {
        assertThat(lexer.lex(sourceCode)).doesNotHaveToken(sourceCode, tokenType)
    }

    @Test
    fun checkLimitsOfStringLiteralWithUserDefinedDelimiters() {
        val p = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false))
        p.setRootRule(p.grammar.rule(PlSqlGrammar.BLOCK_STATEMENT))

        val node = p.parse("begin\n" +
                "x := q'!select 1 from dual!';\n" +
                "y := 'Another unrelated string!';\n" +
                "end;")

        assertThat(node.getDescendants(PlSqlGrammar.ASSIGNMENT_STATEMENT)).hasSize(2)
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

        assertThat(node.getDescendants(PlSqlGrammar.ASSIGNMENT_STATEMENT)).hasSize(1)
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

        assertThat(node.getDescendants(PlSqlGrammar.NULL_STATEMENT)).hasSize(1)
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

        assertThat(node.getDescendants(PlSqlGrammar.NULL_STATEMENT)).hasSize(1)
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

        assertThat(node.getDescendants(PlSqlGrammar.NULL_STATEMENT)).hasSize(1)
    }
}
