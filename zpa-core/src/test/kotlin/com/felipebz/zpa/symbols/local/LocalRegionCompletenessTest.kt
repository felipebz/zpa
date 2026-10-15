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
package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.squid.PlSqlConfiguration
import com.felipebz.zpa.symbols.local.LocalIncompleteness.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/** Per-region completeness: which regions may miss declarations Oracle sees, and why. */
class LocalRegionCompletenessTest {

    private fun parse(source: String, recovery: Boolean = false): AstNode =
        PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, recovery)).parse(source)

    private fun build(source: String, recovery: Boolean = false) = LocalSemanticModelBuilder.build(parse(source, recovery), FileId("f.sql"), source)

    /** One line per region: kind, first declaration names and the completeness verdict. */
    private fun verdicts(model: LocalSemanticModel): List<String> = model.regions.map { region ->
        val names = model.declarationsOf(region.id).joinToString(",") { it.name.originalSpelling }
        "${region.kind}[$names] ${region.completeness}"
    }

    @Test
    fun aFileWithoutEvidenceIsComplete() {
        val model = build("CREATE PACKAGE BODY p AS\n  g NUMBER;\n  PROCEDURE a IS x NUMBER; BEGIN NULL; END;\nEND;\n/\n")
        assertThat(model.regions).allSatisfy { assertThat(it.completeness).isSameAs(LocalRegionCompleteness.COMPLETE) }
    }

    @Test
    fun nestedSelectionInElse() {
        // Only `x := 1;` is kept; the skipped branch could have opened a declaration section.
        val model = build("BEGIN\n  \$IF \$\$a \$THEN x := 1; \$ELSE \$IF \$\$b \$THEN x := 2; \$END x := 3; \$END\nEND;\n/\n")
        assertThat(verdicts(model)).containsExactly(
            "FILE[] INCOMPLETE[CONDITIONAL_COMPILATION]",
            "ANONYMOUS_BLOCK[] INCOMPLETE[CONDITIONAL_COMPILATION]"
        )
    }

    @Test
    fun splitHeadingInAPackageBody() {
        val model = build(
            """
            CREATE PACKAGE BODY p AS
              FUNCTION f ${'$'}IF ${'$'}${'$'}new ${'$'}THEN (a NUMBER) ${'$'}ELSE (a VARCHAR2, b NUMBER) ${'$'}END RETURN NUMBER IS
              BEGIN
                RETURN 1;
              END;
              FUNCTION g RETURN NUMBER IS BEGIN RETURN 2; END;
            END;
            /
            """.trimIndent()
        )
        assertThat(verdicts(model)).containsExactly(
            "FILE[p] INCOMPLETE[CONDITIONAL_COMPILATION]",
            "PACKAGE_BODY[f,g] INCOMPLETE[CONDITIONAL_COMPILATION]",
            "SUBPROGRAM[a] INCOMPLETE[CONDITIONAL_COMPILATION]",
            "SUBPROGRAM[] COMPLETE"
        )
    }

    @Test
    fun keptBranchAfterAnErrorOnlyBranchIsConditional() {
        // The inner block touches no run: only the kept-branch span makes it incomplete.
        val model = build(
            "BEGIN\n  \$IF v \$THEN \$ERROR 'unsupported' \$END \$ELSE\n    x := 1;\n    DECLARE z NUMBER; BEGIN NULL; END;\n    y := 2;\n  \$END\nEND;\n/\n"
        )
        assertThat(verdicts(model)).containsExactly(
            "FILE[] INCOMPLETE[CONDITIONAL_COMPILATION]",
            "ANONYMOUS_BLOCK[] INCOMPLETE[CONDITIONAL_COMPILATION]",
            "ANONYMOUS_BLOCK[z] INCOMPLETE[CONDITIONAL_COMPILATION]"
        )
    }

    @Test
    fun aStandaloneErrorDirectiveIsNotAPoint() {
        // Every branch it covers is $ERROR-only: no PL/SQL there can declare anything.
        val model = build("DECLARE\n  x NUMBER;\n  \$ERROR 'not supported' \$END\n  y NUMBER;\nBEGIN\n  NULL;\nEND;\n/\n")
        assertThat(verdicts(model)).containsExactly("FILE[] COMPLETE", "ANONYMOUS_BLOCK[x,y] COMPLETE")
        val allErrors = build("DECLARE\n  x NUMBER;\n  \$IF v \$THEN \$ERROR 'a' \$END \$END\nBEGIN\n  NULL;\nEND;\n/\n")
        assertThat(verdicts(allErrors)).containsExactly("FILE[] COMPLETE", "ANONYMOUS_BLOCK[x] COMPLETE")
    }

    @Test
    fun aRegionStartingRightAfterHiddenTextIsTouched() {
        // The empty first branch is kept; the skipped `DECLARE y NUMBER;` belonged to the inner block, whose first token
        // carries both runs. No run lies inside its range, yet it is incomplete.
        val model = build("BEGIN\n  \$IF c \$THEN \$ELSE DECLARE y NUMBER; \$END BEGIN NULL; END;\nEND;\n/\n")
        assertThat(verdicts(model)).containsExactly(
            "FILE[] INCOMPLETE[CONDITIONAL_COMPILATION]",
            "ANONYMOUS_BLOCK[] INCOMPLETE[CONDITIONAL_COMPILATION]",
            "ANONYMOUS_BLOCK[] INCOMPLETE[CONDITIONAL_COMPILATION]"
        )
    }

    @Test
    fun independentConstructsTouchOnlyTheirOwnRegions() {
        val model = build(
            """
            CREATE PACKAGE BODY p AS
              PROCEDURE a IS
                ${'$'}IF ${'$'}${'$'}x ${'$'}THEN v NUMBER; ${'$'}END
              BEGIN NULL; END;
              PROCEDURE b IS w NUMBER; BEGIN NULL; END;
              PROCEDURE c IS
              BEGIN
                ${'$'}IF ${'$'}${'$'}y ${'$'}THEN NULL; ${'$'}ELSE c2(); ${'$'}END
              END;
            END;
            /
            """.trimIndent()
        )
        assertThat(verdicts(model)).containsExactly(
            "FILE[p] INCOMPLETE[CONDITIONAL_COMPILATION]",
            "PACKAGE_BODY[a,b,c] INCOMPLETE[CONDITIONAL_COMPILATION]",
            "SUBPROGRAM[v] INCOMPLETE[CONDITIONAL_COMPILATION]",
            "SUBPROGRAM[w] COMPLETE",
            "SUBPROGRAM[] INCOMPLETE[CONDITIONAL_COMPILATION]"
        )
    }

    @Test
    fun unpairedRunsFallBackToOneFileSpan() {
        // A stray $END (consumed as a closing run) breaks the pairing: one span from the first to the last conditional
        // point covers procedure b too, although no run touches it.
        val model = build(
            """
            CREATE PACKAGE BODY p AS
              PROCEDURE a IS BEGIN NULL; ${'$'}END END;
              PROCEDURE b IS w NUMBER; BEGIN NULL; END;
              PROCEDURE c IS BEGIN ${'$'}IF ${'$'}${'$'}y ${'$'}THEN NULL; ${'$'}END END;
              PROCEDURE d IS z NUMBER; BEGIN NULL; END;
            END;
            /
            """.trimIndent()
        )
        assertThat(verdicts(model)).containsExactly(
            "FILE[p] INCOMPLETE[CONDITIONAL_COMPILATION]",
            "PACKAGE_BODY[a,b,c,d] INCOMPLETE[CONDITIONAL_COMPILATION]",
            "SUBPROGRAM[] INCOMPLETE[CONDITIONAL_COMPILATION]",
            "SUBPROGRAM[w] INCOMPLETE[CONDITIONAL_COMPILATION]",
            "SUBPROGRAM[] INCOMPLETE[CONDITIONAL_COMPILATION]",
            "SUBPROGRAM[z] COMPLETE"
        )
    }

    @Test
    fun aSubstitutionTouchesItsRegion() {
        val model = build("BEGIN\n  DECLARE\n    x NUMBER;\n    &more_declarations\n  BEGIN NULL; END;\n  DECLARE y NUMBER; BEGIN NULL; END;\nEND;\n/\n")
        assertThat(verdicts(model)).containsExactly(
            "FILE[] INCOMPLETE[SUBSTITUTION]",
            "ANONYMOUS_BLOCK[] INCOMPLETE[SUBSTITUTION]",
            "ANONYMOUS_BLOCK[x] INCOMPLETE[SUBSTITUTION]",
            "ANONYMOUS_BLOCK[y] COMPLETE"
        )
        // Text before the first token touches the region starting there; text after the last token, only the file.
        assertThat(verdicts(build(" &head\nBEGIN NULL; END;\n/\nBEGIN NULL; END;\n/\n"))).containsExactly(
            "FILE[] INCOMPLETE[SUBSTITUTION]", "ANONYMOUS_BLOCK[] INCOMPLETE[SUBSTITUTION]", "ANONYMOUS_BLOCK[] COMPLETE"
        )
        assertThat(verdicts(build("BEGIN NULL; END;\n/\n-- trailer\n&tail\n"))).containsExactly(
            "FILE[] INCOMPLETE[SUBSTITUTION]", "ANONYMOUS_BLOCK[] COMPLETE"
        )
    }

    @Test
    fun recoveryAfterAnUnseparatedBlock() {
        // Shape of alexandria's string_util demo: a block with no `/` after it, then another `declare`. The second
        // `declare` section is skipped and the next unit starts at BEGIN, without `l_test1`.
        val model = build(
            "declare\n  l_val varchar2(32000);\nbegin\n  l_val := 'x';\nend;\n\n-- randomize array of strings\n\n" +
                "declare\n  l_test1 t_str_array ('one', 'two', 'three', 'four');\nbegin\n  l_test1 := randomize_array (l_test1);\nend;\n",
            recovery = true
        )
        assertThat(model.declarations.map { it.name.originalSpelling }).doesNotContain("l_test1")
        assertThat(model.regions).allSatisfy { assertThat(it.completeness.reasons).contains(PARSE_RECOVERY) }
        // The extraction gap of the file region does not spread to its children.
        assertThat(model.root.completeness.reasons).contains(PARTIAL_EXTRACTION)
        assertThat(model.regions.drop(1)).allSatisfy { assertThat(it.completeness.reasons).doesNotContain(PARTIAL_EXTRACTION) }
    }

    @Test
    fun recoveryInsideAProcedureLeavesAGhostHeading() {
        val model = build("CREATE PROCEDURE p IS\n  x NUMBER;\nBEGIN\n  x := ;\nEND;\n/\n", recovery = true)
        assertThat(verdicts(model)).containsExactly("FILE[p] INCOMPLETE[PARTIAL_EXTRACTION, PARSE_RECOVERY]")
    }

    @Test
    fun aUnitWellBeforeTheFirstRecoveryStaysComplete() {
        val model = build(
            "BEGIN NULL; END;\n/\nDECLARE a NUMBER; BEGIN NULL; END;\n/\nselect * from;\nBEGIN NULL; END;\n/\n",
            recovery = true
        )
        assertThat(verdicts(model)).containsExactly(
            "FILE[] INCOMPLETE[PARTIAL_EXTRACTION, PARSE_RECOVERY]",
            "ANONYMOUS_BLOCK[] COMPLETE",
            // The unit right before the recovery may have lost text to it.
            "ANONYMOUS_BLOCK[a] INCOMPLETE[PARSE_RECOVERY]",
            "ANONYMOUS_BLOCK[] INCOMPLETE[PARSE_RECOVERY]"
        )
    }

    @Test
    fun recoveryBelowTheFileLevelTaintsEveryRegion() {
        // The grammar only recovers at the file level; a deeper RECOVERY node is grafted to pin the rule.
        val source = "BEGIN NULL; END;\n/\nBEGIN NULL; END;\n/\n"
        val root = parse(source)
        val statements = root.getDescendants(PlSqlGrammar.STATEMENTS).first()
        val original = statements.children.toList()
        statements.children.clear()
        val recovery = AstNode(PlSqlGrammar.RECOVERY, "RECOVERY", original.first().token)
        recovery.addChild(original.first())
        statements.addChild(recovery)
        original.drop(1).forEach(statements::addChild)
        val model = LocalSemanticModelBuilder.build(root, FileId("f.sql"), source)
        assertThat(model.regions).allSatisfy { assertThat(it.completeness.reasons).contains(PARSE_RECOVERY) }
    }

    @Test
    fun missingSourceOrUnlocatableTokens() {
        val source = "DECLARE x NUMBER; BEGIN NULL; END;\n/\n"
        val withoutSource = LocalSemanticModelBuilder.build(parse(source), FileId("f.sql"), null)
        assertThat(withoutSource.regions).allSatisfy { assertThat(it.completeness.reasons).containsExactly(SOURCE_TEXT_UNAVAILABLE) }
        // A source that does not match the tokens: no gap can be ruled out.
        val shifted = LocalSemanticModelBuilder.build(parse(source), FileId("f.sql"), " $source")
        assertThat(shifted.regions).allSatisfy { assertThat(it.completeness.reasons).containsExactly(UNVERIFIED_SOURCE_POSITIONS) }
    }

    @Test
    fun theVerdictIsDetachedAndShared() {
        val model = build("BEGIN \$IF a \$THEN NULL; \$END END;\n/\nBEGIN NULL; END;\n/\n")
        assertThat(model.regions.last().completeness).isSameAs(LocalRegionCompleteness.COMPLETE)
        assertThat(model.regions.first().completeness.reasons).containsExactly(CONDITIONAL_COMPILATION)
    }
}
