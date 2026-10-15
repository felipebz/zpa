/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
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

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.api.RecognitionException
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class ConditionalCompilationChannelTest {

    private val lexer = PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8))

    /** The values of the tokens kept between `BEGIN` and `END;` of a block wrapping [body]. */
    private fun kept(body: String): String {
        val values = lexer.lex("BEGIN\n$body\nEND;").filter { it.type !== GenericTokenType.EOF }.map { it.originalValue }
        assertThat(values.first()).isEqualTo("BEGIN")
        assertThat(values.takeLast(2)).containsExactly("END", ";")
        return values.drop(1).dropLast(2).joinToString(" ")
    }

    private fun parse(source: String, rule: PlSqlGrammar? = null): AstNode {
        val parser = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false))
        if (rule != null) parser.setRootRule(parser.grammar.rule(rule))
        return parser.parse(source)
    }

    private fun parseBlock(body: String) = parse("BEGIN\n$body\nEND;", PlSqlGrammar.BLOCK_STATEMENT)

    @Test
    fun nestedSelectionInsideElseDoesNotEndTheOuterBranch() {
        assertThat(kept("\$IF \$\$a \$THEN x := 1; \$ELSE \$IF \$\$b \$THEN x := 2; \$END x := 3; \$END")).isEqualTo("x := 1 ;")
    }

    @Test
    fun nestedSelectionInsideElsif() {
        assertThat(kept("\$IF \$\$a \$THEN x := 1; \$ELSIF \$\$b \$THEN \$IF \$\$c \$THEN x := 2; \$END x := 3; \$ELSE x := 4; \$END"))
            .isEqualTo("x := 1 ;")
    }

    @Test
    fun threeLevelsKeepOnlyTheFirstBranchPath() {
        val source = """
            ${'$'}IF ${'$'}${'$'}a ${'$'}THEN
              ${'$'}IF ${'$'}${'$'}b ${'$'}THEN
                ${'$'}IF ${'$'}${'$'}c ${'$'}THEN x := 1; ${'$'}ELSIF ${'$'}${'$'}d ${'$'}THEN x := 2; ${'$'}ELSE x := 3; ${'$'}END
                y := 1;
              ${'$'}ELSIF ${'$'}${'$'}e ${'$'}THEN
                ${'$'}IF ${'$'}${'$'}f ${'$'}THEN x := 4; ${'$'}ELSE x := 5; ${'$'}END
              ${'$'}ELSE x := 6;
              ${'$'}END
              z := 1;
            ${'$'}ELSIF ${'$'}${'$'}g ${'$'}THEN
              ${'$'}IF ${'$'}${'$'}h ${'$'}THEN x := 7; ${'$'}ELSIF ${'$'}${'$'}i ${'$'}THEN x := 8; ${'$'}ELSE x := 9; ${'$'}END
            ${'$'}ELSE
              ${'$'}IF ${'$'}${'$'}j ${'$'}THEN x := 10; ${'$'}ELSE x := 11; ${'$'}END
            ${'$'}END
        """.trimIndent()
        assertThat(kept(source)).isEqualTo("x := 1 ; y := 1 ; z := 1 ;")
        assertThat(parseBlock(source).getDescendants(PlSqlGrammar.ASSIGNMENT_STATEMENT)).hasSize(3)
    }

    @Test
    fun directiveWordsInStringsCommentsAndQuotedLiteralsDoNotCount() {
        assertThat(kept("\$IF \$\$d \$THEN NULL; \$ELSE put_line('use \$END to close'); \$END")).isEqualTo("NULL ;")
        assertThat(parseBlock("\$IF \$\$d \$THEN NULL; \$ELSE put_line('use \$END to close'); \$END").getDescendants(PlSqlGrammar.NULL_STATEMENT)).hasSize(1)
        assertThat(kept("\$IF \$\$d \$THEN NULL; \$ELSE -- old \$END\nx := 1; \$END")).isEqualTo("NULL ;")
        assertThat(kept("\$IF \$\$d \$THEN NULL; \$ELSE /* \$END */ x := 1; \$END")).isEqualTo("NULL ;")
        assertThat(kept("\$IF \$\$a \$THEN NULL; \$ELSE x := q'[it's \$END here]'; \$END")).isEqualTo("NULL ;")
        assertThat(kept("\$IF \$\$a \$THEN NULL; \$ELSE x := NQ'<\$END>'; y := \"\$END\"; \$END")).isEqualTo("NULL ;")
        assertThat(kept("\$IF \$\$a -- note: \$THEN\nAND \$\$b \$THEN x := 1; \$END")).isEqualTo("x := 1 ;")
        assertThat(kept("\$IF \$\$a \$THEN NULL; \$ELSE a\$end := 1; my\$elsif := 2; \$END")).isEqualTo("NULL ;")
    }

    @Test
    fun anErrorOnlyBranchIsNotSelected() {
        assertThat(kept("\$IF v \$THEN \$ERROR 'unsupported' \$END \$ELSE put_line('ok'); \$END")).isEqualTo("put_line ( 'ok' ) ;")
        assertThat(parseBlock("\$IF v \$THEN \$ERROR 'unsupported' \$END \$ELSE put_line('ok'); \$END")).isNotNull
        // A branch holding an $ERROR plus other text is selectable.
        assertThat(kept("\$IF v \$THEN \$ERROR 'x' \$END NULL; \$ELSE x := 1; \$END")).isEqualTo("NULL ;")
        // No selectable branch: nothing is kept.
        assertThat(kept("\$IF v \$THEN \$ERROR 'a' \$END \$ELSIF w \$THEN \$ERROR 'b' \$END \$END NULL;")).isEqualTo("NULL ;")
    }

    @Test
    fun aDirectiveGluedToANumberIsSeen() {
        assertThat(kept("y := \$IF \$\$b \$THEN 1 \$ELSE 2\$END;")).isEqualTo("y := 1 ;")
        assertThat(parseBlock("y := \$IF \$\$b \$THEN 1 \$ELSE 2\$END;").getDescendants(PlSqlGrammar.ASSIGNMENT_STATEMENT)).hasSize(1)
        // Both branches must be seen for the ERROR-only first branch to be skipped.
        assertThat(kept("\$IF v \$THEN \$ERROR 'x' \$END \$ELSE x := 1\$END")).isEqualTo("x := 1")
    }

    @Test
    fun selectionInsideADeclaration() {
        val node = parse("DECLARE\n  v NUMBER := \$IF \$\$x IS NULL \$THEN \$ERROR 'x' \$END \$ELSE \$\$x \$END;\nBEGIN\n  NULL;\nEND;", PlSqlGrammar.BLOCK_STATEMENT)
        assertThat(node.getDescendants(PlSqlGrammar.VARIABLE_DECLARATION)).hasSize(1)
        val values = lexer.lex("v NUMBER := \$IF \$\$x IS NULL \$THEN \$ERROR 'x' \$END \$ELSE \$\$x \$END;").map { it.originalValue }
        assertThat(values.joinToString(" ")).startsWith("v NUMBER := ").endsWith("x ; EOF")
    }

    @Test
    fun ordinaryIdentifiersWithDollarSigns() {
        assertThat(parseBlock("my\$elsif := 1;\na\$end := 2;").getDescendants(PlSqlGrammar.ASSIGNMENT_STATEMENT)).hasSize(2)
        // `${'$'}IFX` is not `${'$'}IF`: the channel leaves it to the following channels.
        assertThat(lexer.lex("\$IFX").first().originalValue).isEqualTo("\$")
    }

    @Test
    fun malformedDirectivesAreReportedWhereTheyAre() {
        val missingEnd = "BEGIN\n\$IF \$\$a \$THEN x := 1;\nEND;\n/\nBEGIN NULL; END;\n"
        val missingThen = "BEGIN\n\$IF \$\$a x := 1; \$END\nEND;\n/\nBEGIN NULL; END;\n"
        for (source in listOf(missingEnd, missingThen)) {
            // The lexer does not throw and keeps the rest of the file.
            val tokens = lexer.lex(source)
            assertThat(tokens.count { it.originalValue.equals("NULL", ignoreCase = true) }).describedAs(source).isEqualTo(1)
            assertThat(tokens.first { it.originalValue == "\$" }.line).isEqualTo(2)
            assertThatThrownBy { parse(source) }.describedAs(source).isInstanceOf(RecognitionException::class.java)
        }
    }

    @Test
    fun positionsAfterASkippedRegion() {
        val tokens = lexer.lex("BEGIN\n\$IF \$\$a \$THEN\n  x := 1;\n\$ELSE\n  x := 2;\n  y := 3;\n\$END\n  z := 4;\nEND;")
        val z = tokens.single { it.originalValue == "z" }
        assertThat(z.line).isEqualTo(8)
        assertThat(z.column).isEqualTo(2)
        val x = tokens.single { it.originalValue == "x" }
        assertThat(x.line to x.column).isEqualTo(3 to 2)
    }

    @Test
    fun loggerShapeParses() {
        val node = parse(
            """
            CREATE OR REPLACE FUNCTION f RETURN NUMBER IS
              l_value NUMBER;
            BEGIN
              ${'$'}if ${'$'}${'$'}no_op ${'$'}then
                null;
              ${'$'}else
                l_value := 1;
                ${'$'}if ${'$'}${'$'}rac_lt_11_2 ${'$'}then
                  l_value := 2;
                ${'$'}else
                  l_value := 3;
                ${'$'}end
                l_value := l_value + 1;
              ${'$'}end
              RETURN l_value;
            END;
            """.trimIndent()
        )
        assertThat(node.getDescendants(PlSqlGrammar.NULL_STATEMENT)).hasSize(1)
        assertThat(node.getDescendants(PlSqlGrammar.ASSIGNMENT_STATEMENT)).isEmpty()
    }

    private fun trivia(source: String, value: String) =
        lexer.lex(source).first { it.originalValue == value }.trivia.map { "${if (it.isComment) "COMMENT" else "SKIPPED_TEXT"} ${it.token.type} ${it.token.line}:${it.token.column} ${it.token.originalValue}" }

    @Test
    fun removedTextIsReportedAsTrivia() {
        val source = "BEGIN\n  -- note\n  \$IF \$\$a \$THEN x := 1; \$ELSE y := 2; \$END z := 3;\nEND;"
        assertThat(trivia(source, "x")).containsExactly(
            "COMMENT COMMENT 2:2 -- note",
            "SKIPPED_TEXT CONDITIONAL_BRANCH_BOUNDARY 3:2 \$IF \$\$a \$THEN"
        )
        assertThat(trivia(source, "z")).containsExactly("SKIPPED_TEXT CONDITIONAL_BRANCH_BOUNDARY 3:24 \$ELSE y := 2; \$END")
        val tokens = lexer.lex(source)
        val x = tokens.first { it.originalValue == "x" }
        assertThat(x.trivia.last().token.value).isEqualTo(x.trivia.last().token.originalValue)
        // Kept tokens are unchanged.
        assertThat(tokens.map { it.originalValue }.joinToString(" ")).isEqualTo("BEGIN x := 1 ; z := 3 ; END ; EOF")
    }

    @Test
    fun directivesKeepingNoTextAreCompleteDirectives() {
        assertThat(trivia("BEGIN \$ERROR 'e' \$END NULL; END;", "NULL")).containsExactly("SKIPPED_TEXT CONDITIONAL_DIRECTIVE 1:6 \$ERROR 'e' \$END")
        assertThat(trivia("BEGIN \$IF a \$THEN \$ERROR 'e' \$END \$END NULL; END;", "NULL"))
            .containsExactly("SKIPPED_TEXT CONDITIONAL_DIRECTIVE 1:6 \$IF a \$THEN \$ERROR 'e' \$END \$END")
        // A bare $END closing the kept branch is a boundary.
        assertThat(trivia("BEGIN \$IF a \$THEN NULL; \$END x := 1; END;", "x")).containsExactly("SKIPPED_TEXT CONDITIONAL_BRANCH_BOUNDARY 1:24 \$END")
        // A run right before the end of the file attaches to EOF.
        val eof = lexer.lex("NULL;\n\$END").last()
        assertThat(eof.type).isEqualTo(GenericTokenType.EOF)
        assertThat(eof.trivia.map { "${it.token.type} ${it.token.originalValue}" }).containsExactly("CONDITIONAL_BRANCH_BOUNDARY \$END")
    }

    @Test
    fun commentVisitorsReceiveOnlyComments() {
        val comments = ArrayList<String>()
        val visitor = object : com.felipebz.zpa.api.checks.PlSqlCheck() {
            override fun visitComment(trivia: com.felipebz.flr.api.Trivia, content: String) {
                comments += trivia.token.originalValue
            }
        }
        val root = parse("BEGIN\n  -- one\n  \$IF \$\$a \$THEN NULL; /* two */ \$ELSE x := 1; \$END\n  \$ERROR 'e' \$END\n  NULL;\nEND;")
        com.felipebz.zpa.squid.PlSqlAstWalker(listOf(visitor)).walk(com.felipebz.zpa.api.PlSqlVisitorContext(root, null, null))
        assertThat(comments).containsExactly("-- one", "/* two */")
    }

    // Known limitation of the branch policy (conditions are not evaluated): the empty first branch is selected, so the
    // procedure body is empty and the parse fails. Evaluating conditions (a later step) addresses it.
    @Test
    fun emptyFirstBranchIsStillSelected() {
        assertThatThrownBy { parse("CREATE PROCEDURE p IS BEGIN \$IF \$\$legacy \$THEN \$ELSE NULL; \$END END;") }
            .isInstanceOf(RecognitionException::class.java)
    }
}
