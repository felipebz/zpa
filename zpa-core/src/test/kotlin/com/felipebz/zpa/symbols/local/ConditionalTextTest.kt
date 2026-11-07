//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.project.OracleIdentifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConditionalTextTest {
    private fun words(text: String) = ConditionalText.words(text, 1, 0)
    private fun balanced(text: String) = ConditionalText.isBlockBalanced(words(text))
    private fun mentions(text: String, name: String) = ConditionalText.piece(words(text), wide = false).affects(OracleIdentifier.fromSource(name))

    private val quoted = listOf("q'[it's end g_total]'", "Q'{it's end g_total}'", "nq'<it's end g_total>'", "q'!it's end g_total!'", "N'it''s end g_total'")

    @Test
    fun quotedLiteralsAreSkippedWithTheirDelimiters() {
        for (literal in quoted) {
            // The apostrophe and the END inside the literal change neither the balance nor the mentions.
            assertThat(balanced("\$ELSE dbms_output.put_line($literal); \$END")).describedAs(literal).isTrue
            assertThat(balanced("\$ELSE dbms_output.put_line($literal); end; \$END")).describedAs(literal).isFalse
            assertThat(mentions("\$ELSE dbms_output.put_line($literal); \$END", "g_total")).describedAs(literal).isFalse
            assertThat(mentions("\$ELSE dbms_output.put_line($literal); g_total := 1; \$END", "g_total")).describedAs(literal).isTrue
        }
    }

    @Test
    fun blocksBalanceByTheirKeywords() {
        assertThat(balanced("\$IF \$\$debug \$THEN procedure dbg is begin null; end;")).isTrue
        assertThat(balanced("\$ELSE if x then null; end if; for i in 1..2 loop null; end loop; case when x then 1 end; \$END")).isTrue
        assertThat(balanced("\$ELSE null; end; function f return number is begin return 1; \$END")).isFalse
        assertThat(balanced("\$ELSE null; end if; \$END")).isFalse
        assertThat(balanced("\$ELSE begin null; \$END")).isFalse
        // Directive words, inquiry directives, strings, comments and quoted identifiers are not keywords.
        assertThat(balanced("\$IF \$\$end \$THEN x := 'end'; -- end\n /* end */ \"END\" := 1; \$END")).isTrue
    }

    @Test
    fun mentionsCompareAsIdentifiers() {
        assertThat(mentions("\$ELSE G_Total := 1; \$END", "g_total")).isTrue
        assertThat(mentions("\$ELSE \"G_TOTAL\" := 1; \$END", "g_total")).isTrue
        assertThat(mentions("\$ELSE \"g_total\" := 1; \$END", "g_total")).isFalse
        assertThat(mentions("\$ELSE \"g_total\" := 1; \$END", "\"g_total\"")).isTrue
        assertThat(mentions("\$IF pk_cfg.g_total \$THEN", "g_total")).isTrue
        assertThat(mentions("\$IF \$\$g_total \$THEN", "g_total")).isFalse
        assertThat(mentions("\$ELSE x := 'g_total'; -- g_total\n \$END", "g_total")).isFalse
    }

    @Test
    fun aMentionAtTheExceptedPositionDoesNotCount() {
        val piece = ConditionalText.piece(ConditionalText.words("\$IF \$\$x \$THEN\n  v_dbg number;", 3, 2), wide = false)
        assertThat(piece.affects(OracleIdentifier.fromSource("v_dbg"))).isTrue
        // `v_dbg` is on the second line of the text, which starts at line 3: line 4, column 2.
        assertThat(piece.affects(OracleIdentifier.fromSource("v_dbg"), LocalConditionalPiece.position(4, 2))).isFalse
    }

    @Test
    fun aNumberEndsWhereOracleEndsIt() {
        assertThat(mentions("\$ELSE for i in 1..g_total loop null; end loop; \$END", "g_total")).isTrue
        assertThat(mentions("\$ELSE x := v(1..g_total); \$END", "g_total")).isTrue
        assertThat(words("x := 1.5e-3 + 2f + 3.25D;").map { it.text }).containsExactly("x")
        assertThat(balanced("\$ELSE for i in 1..end_pos loop null; end loop; \$END")).isTrue
    }

    @Test
    fun aWordAfterTwoAnglesIsALabel() {
        val piece = ConditionalText.piece(words("\$IF \$\$lbl \$THEN <<outer>> x := 1;"), wide = false)
        assertThat(piece.labels(OracleIdentifier.fromSource("outer"))).isTrue
        assertThat(piece.labels(OracleIdentifier.fromSource("x"))).isFalse
    }
}
