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
package com.felipebz.zpa.project.extraction

import com.felipebz.zpa.lexer.PlSqlLexer
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/** Boundary scanning of subprogram bodies, checked with exact token indices on hand-lexed sources. */
class BodyBoundaryScannerTest {
    private class Fixture(text: String) {
        val stream = TokenStream(FileId("body.sql"), PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).lex(text))
        val scanner = BodyBoundaryScanner(stream)
        val size = stream.tokens.size

        /** Index of the [n]-th (0-based) token whose upper-cased value is [value]. */
        fun at(value: String, n: Int = 0): Int =
            stream.tokens.indices.filter { stream.valueAt(it) == value }[n]
    }

    @Test
    fun implementationEndIsTheSemicolonAfterTheOutermostEnd() {
        val f = Fixture("CREATE PROCEDURE p IS BEGIN NULL; END;")
        assertThat(f.scanner.implementationEnd(f.at("IS") + 1, f.size)).isEqualTo(f.at(";", 1))
        val g = Fixture("CREATE FUNCTION f RETURN NUMBER IS BEGIN RETURN 1; END f;")
        assertThat(g.scanner.implementationEnd(g.at("IS") + 1, g.size)).isEqualTo(g.at(";", 1))
    }

    @Test
    fun nestedSubprogramsAndBlocksAreSkippedBeforeTheBodyStarts() {
        val f = Fixture("IS PROCEDURE n IS BEGIN NULL; END; FUNCTION g RETURN NUMBER IS BEGIN RETURN 1; END; BEGIN n; BEGIN NULL; END; END;")
        assertThat(f.scanner.implementationEnd(1, f.size)).isEqualTo(f.at(";", 7))
        assertThat(f.stream.isPunctuator(f.at(";", 7) + 1, com.felipebz.zpa.api.PlSqlPunctuator.SEMICOLON)).isFalse()
        // The nested helper alone ends at its own terminating semicolon.
        assertThat(f.scanner.bodySubprogramEnd(f.at("PROCEDURE"), f.size)).isEqualTo(f.at(";", 1))
        assertThat(f.scanner.bodySubprogramEnd(f.at("FUNCTION"), f.size)).isEqualTo(f.at(";", 3))
    }

    @Test
    fun caseExpressionsEndIfAndLoopDoNotCloseTheBody() {
        val f = Fixture("IS BEGIN x := CASE WHEN 1 = 1 THEN 1 END; IF a THEN NULL; END IF; LOOP EXIT; END LOOP; CASE a WHEN 1 THEN NULL; END CASE; END;")
        val last = f.stream.tokens.indices.last { f.stream.isPunctuator(it, com.felipebz.zpa.api.PlSqlPunctuator.SEMICOLON) }
        assertThat(f.scanner.implementationEnd(1, f.size)).isEqualTo(last)
        assertThat(f.scanner.statementStart(f.at("CASE", 0))).isEqualTo(BlockKind.CASE)
        assertThat(f.scanner.statementStart(f.at("IF", 0))).isEqualTo(BlockKind.IF)
        assertThat(f.scanner.statementStart(f.at("LOOP", 0))).isEqualTo(BlockKind.LOOP)
        // `END CASE`, `END IF` and `END LOOP` open nothing.
        assertThat(f.scanner.statementStart(f.at("CASE", 2))).isNull()
        assertThat(f.scanner.statementStart(f.at("IF", 1))).isNull()
        assertThat(f.scanner.statementStart(f.at("LOOP", 1))).isNull()
    }

    @Test
    fun callSpecificationsEndAtTheirSemicolonAndNeverBeyondTheLimit() {
        val f = Fixture("IS LANGUAGE JAVA NAME 'A.b()'; PROCEDURE other;")
        assertThat(f.scanner.implementationEnd(1, f.size)).isEqualTo(f.at(";", 0))
        assertThat(f.scanner.implementationEnd(1, 3)).isEqualTo(3)
    }

    @Test
    fun closeBlockPopsTheInnermostBlockAndTreatsEndIfAndEndLoopOnABlockAsALabel() {
        val f = Fixture("END; END IF; END LOOP; END; END;")
        // On their own statement, END IF / END LOOP close it.
        val statements = mutableListOf(BlockKind.BEGIN, BlockKind.LOOP, BlockKind.IF)
        assertThat(f.scanner.closeBlock(f.at("END", 1), statements)).isEqualTo(BlockClose.STATEMENT)
        assertThat(f.scanner.closeBlock(f.at("END", 2), statements)).isEqualTo(BlockClose.STATEMENT)
        assertThat(statements).containsExactly(BlockKind.BEGIN)
        // On a CASE or BEGIN the word is a label: the block closes as with END x.
        val blocks = mutableListOf(BlockKind.BEGIN, BlockKind.BEGIN, BlockKind.CASE)
        assertThat(f.scanner.closeBlock(f.at("END", 1), blocks)).isEqualTo(BlockClose.CASE)
        assertThat(f.scanner.closeBlock(f.at("END", 2), blocks)).isEqualTo(BlockClose.BEGIN)
        assertThat(f.scanner.closeBlock(f.at("END", 0), blocks)).isEqualTo(BlockClose.BEGIN)
        assertThat(blocks).isEmpty()
        assertThat(f.scanner.closeBlock(f.at("END", 4), blocks)).isEqualTo(BlockClose.UNBALANCED)
        assertThat(blocks).isEmpty()
    }

    @Test
    fun blocksLabelledLoopOrIfInsideLoopsAndIfs() {
        val source = "IS BEGIN FOR i IN 1 .. 2 LOOP BEGIN NULL; END LOOP; END LOOP; " +
            "WHILE a LOOP IF b THEN BEGIN NULL; END IF; ELSIF c THEN LOOP BEGIN NULL; END; END LOOP; END IF; END LOOP; " +
            "BEGIN NULL; END CASE; END q; PROCEDURE next IS BEGIN NULL; END;"
        val f = Fixture(source)
        assertThat(f.scanner.implementationEnd(1, f.size)).isEqualTo(f.at(";", 12))
        assertThat(f.scanner.executableBlockEnd(f.at("BEGIN", 0), f.size)).isEqualTo(f.at(";", 12))
        // Conditional-compilation $IF opens nothing.
        val conditional = Fixture("IS BEGIN \$IF \$\$a \$THEN NULL; \$ELSE x := 1; \$END END; PROCEDURE next IS BEGIN NULL; END;")
        assertThat(conditional.scanner.implementationEnd(1, conditional.size)).isEqualTo(conditional.at(";", 1))
    }

    @Test
    fun aUnitWhoseOwnEndIsWrittenEndLoopStillEnds() {
        // Oracle rejects it (the end label must match the unit name), but the body still ends there.
        val f = Fixture("IS BEGIN NULL; END LOOP; PROCEDURE next IS BEGIN NULL; END;")
        assertThat(f.scanner.implementationEnd(1, f.size)).isEqualTo(f.at(";", 1))
    }

    @Test
    fun unterminatedOrUnbalancedBodiesYieldNull() {
        val unterminated = Fixture("IS BEGIN NULL;")
        assertThat(unterminated.scanner.implementationEnd(1, unterminated.size)).isNull()
        val stray = Fixture("IS NULL; END;")
        assertThat(stray.scanner.implementationEnd(1, stray.size)).isNull()
    }

    @Test
    fun headerDelimiterDistinguishesImplementationFromForwardDeclaration() {
        val impl = Fixture("PROCEDURE p(a NUMBER DEFAULT f(1)) IS BEGIN NULL; END;")
        val delimiter = impl.scanner.firstBodyHeaderDelimiter(impl.at(")", 1) + 1, impl.size)!!
        assertThat(delimiter.kind).isEqualTo(BodyHeaderDelimiterKind.IMPLEMENTATION)
        assertThat(delimiter.index).isEqualTo(impl.at("IS"))
        val forward = Fixture("FUNCTION f(a NUMBER) RETURN NUMBER;")
        val fwd = forward.scanner.firstBodyHeaderDelimiter(forward.at(")") + 1, forward.size)!!
        assertThat(fwd.kind).isEqualTo(BodyHeaderDelimiterKind.FORWARD_DECLARATION)
        assertThat(fwd.index).isEqualTo(forward.at(";"))
        assertThat(forward.scanner.bodySubprogramEnd(0, forward.size)).isEqualTo(forward.at(";"))
        val none = Fixture("PROCEDURE p")
        assertThat(none.scanner.firstBodyHeaderDelimiter(0, none.size)).isNull()
    }

    @Test
    fun executableBlockEndAndKeywordPredicates() {
        val f = Fixture("BEGIN BEGIN NULL; END; NULL; END; PROCEDURE q; x")
        assertThat(f.scanner.executableBlockEnd(0, f.size)).isEqualTo(f.at(";", 3))
        assertThat(f.scanner.isSubprogramKeyword(f.at("PROCEDURE"))).isTrue()
        assertThat(f.scanner.isSubprogramKeyword(f.at("BEGIN"))).isFalse()
        assertThat(f.scanner.isSubprogramKeyword(f.size + 5)).isFalse()
        val open = Fixture("BEGIN NULL;")
        assertThat(open.scanner.executableBlockEnd(0, open.size)).isNull()
    }
}
