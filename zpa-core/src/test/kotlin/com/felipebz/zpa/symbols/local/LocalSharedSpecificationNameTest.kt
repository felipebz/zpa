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
@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.squid.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/**
 * A name a package body declares and its specification in another project file declares too: decided as it is with the
 * specification in the same file, when the project proves which body definition each specification heading has.
 */
class LocalSharedSpecificationNameTest {

    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private class Capture : PlSqlCheck() {
        var facts: LocalSemanticFacts? = null
        override fun leaveFile(node: AstNode) {
            facts = context.localSemanticFacts
        }
    }

    /** The facts of [fileName] in a project of [files]. */
    private fun facts(fileName: String, vararg files: Pair<String, String>): LocalSemanticFacts {
        val project = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation().prepare(files.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
        )
        val text = files.toMap().getValue(fileName)
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).parse(text)
        val capture = Capture()
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId(fileName)).all + capture)
            .walk(PlSqlVisitorContext(root, Source(fileName, text), null), SemanticScanCapabilities(createProjectAnalysis(project)))
        return capture.facts!!
    }

    /**
     * One row per reference of the body written on or after [fromLine] (a line of the body): the first segment's
     * outcome, without the project-coverage wrapper, and the call's outcome, local status and selected definition. Lines are the
     * body's own, [offset] lines below the start of the analyzed file.
     */
    private fun rows(facts: LocalSemanticFacts, offset: Int, fromLine: Int): List<String> {
        fun lines(text: String) = Regex("@(\\d+):").replace(text) { "@${it.groupValues[1].toInt() - offset}:" }
        val calls = facts.calls.associateBy { it.reference }
        return facts.references.indices.filter { facts.references[it].range.startLine - offset >= fromLine }.map { index ->
            val reference = facts.references[index]
            val first = facts.resolutions[index].first().let { (it as? LocalResolution.SpecificationCovered)?.outcome ?: it }
            val call = calls[reference]
            val local = call?.local?.let { local ->
                " " + local.status + local.selected?.let { " " + LocalOracleFixtureTest.declaration(facts.model, it) }.orEmpty()
            }.orEmpty()
            val name = (listOf(reference.name) + reference.segments.map { it.name }).joinToString(".") { it.originalSpelling }
            lines("${reference.range.startLine - offset}:${reference.range.startColumn} $name ${LocalOracleFixtureTest.render(facts.model, first)}" +
                (call?.let { " -> ${it.outcome}$local" } ?: ""))
        }
    }

    private fun split(spec: String, body: String, fromLine: Int, vararg others: Pair<String, String>) =
        rows(facts("b.sql", "s.sql" to spec, "b.sql" to body, *others), 0, fromLine)

    private fun sameFile(spec: String, body: String, fromLine: Int) =
        rows(facts("one.sql", "one.sql" to spec + body), spec.lines().size - 1, fromLine)

    /** The rows of the body split from its specification, which must equal the rows with both in one file. */
    private fun decided(spec: String, body: String, fromLine: Int): List<String> {
        val split = split(spec, body, fromLine)
        assertThat(split).isEqualTo(sameFile(spec, body, fromLine))
        return split
    }

    private val loA = "create package lo_a is\n  procedure p(x number);\n  procedure run;\nend;\n/\n"
    private val loABody = "create package body lo_a is\n  d date;\n  procedure p(x date) is begin null; end;\n" +
        "  procedure p(x number) is begin null; end;\n  procedure run is begin p(sysdate); p(1); p(d); lo_a.p(1); end;\nend;\n/\n"

    @Test
    fun aCallToASetOfSpecificationAndBodyOverloads() {
        assertThat(decided(loA, loABody, 5)).containsExactly(
            // The specification's p(number) first, as its heading comes first with the specification in the file.
            "5:25 p OverloadSet(PROCEDURE p@4:12, PROCEDURE p@3:12) -> LOCAL_CAPTURE AMBIGUOUS",
            "5:27 sysdate ProvenAbsent",
            "5:37 p OverloadSet(PROCEDURE p@4:12, PROCEDURE p@3:12) -> LOCAL_CAPTURE RESOLVED PROCEDURE p@4:12",
            "5:43 p OverloadSet(PROCEDURE p@4:12, PROCEDURE p@3:12) -> LOCAL_CAPTURE RESOLVED PROCEDURE p@3:12",
            "5:45 d Bound(VARIABLE d@2:2)",
            "5:49 lo_a.p Bound(PACKAGE_BODY lo_a@1:20) -> LOCAL_CAPTURE RESOLVED PROCEDURE p@4:12"
        )
    }

    @Test
    fun aPublicSubprogramDefinedAfterTheCaller() {
        val spec = "create package lo_c is\n  procedure p(x number);\n  procedure run;\nend;\n/\n"
        val body = "create package body lo_c is\n  procedure run is begin p(1); end;\n  procedure p(x number) is begin null; end;\nend;\n/\n"
        assertThat(decided(spec, body, 2)).containsExactly(
            "2:25 p OverloadSet(PROCEDURE p@3:12) -> LOCAL_CAPTURE RESOLVED PROCEDURE p@3:12",
            "3:16 number ProvenAbsent"
        )
    }

    @Test
    fun theEnclosingSubprogramAsQualifier() {
        val spec = "create package ntlm is\n  function get_blob(p_url varchar2) return blob;\nend;\n/\n"
        val body = "create package body ntlm is\n  function get_blob(p_url varchar2) return blob is\n    v varchar2(100);\n" +
            "  begin\n    v := get_blob.p_url;\n    return null;\n  end;\nend;\n/\n"
        assertThat(decided(spec, body, 5)).containsExactly(
            "5:4 v Bound(VARIABLE v@3:4)",
            "5:9 get_blob.p_url Bound(FUNCTION get_blob@2:11)"
        )
    }

    @Test
    fun aPublicFunctionCalledWithoutParenthesesAndDefinedLater() {
        val spec = "create package rnd is\n  function get_file_type return varchar2;\n  procedure run;\nend;\n/\n"
        val body = "create package body rnd is\n  procedure run is\n    v varchar2(10);\n  begin\n    v := get_file_type;\n  end;\n" +
            "  function get_file_type return varchar2 is begin return 'x'; end;\nend;\n/\n"
        assertThat(decided(spec, body, 5)).containsExactly(
            "5:4 v Bound(VARIABLE v@3:4)",
            "5:9 get_file_type OverloadSet(FUNCTION get_file_type@7:11) -> LOCAL_CAPTURE RESOLVED FUNCTION get_file_type@7:11",
            "7:32 varchar2 ProvenAbsent"
        )
    }

    @Test
    fun aBodyOnlyOverloadStillFollowsTheOrderRule() {
        // The body-only p(date) is defined after the caller, the specification's p(number) is visible throughout.
        val spec = "create package lo_d is\n  procedure p(x number);\n  procedure run;\nend;\n/\n"
        val body = "create package body lo_d is\n  procedure p(x number) is begin null; end;\n  procedure run is begin p(1); end;\n" +
            "  procedure p(x date) is begin null; end;\nend;\n/\n"
        assertThat(decided(spec, body, 3)).containsExactly("3:25 p Unsupported(OVERLOADS_PARTLY_LATER) -> UNSUPPORTED", "4:16 date ProvenAbsent")
        // Every overload public, one defined after the caller: the whole set is visible.
        val bothSpec = "create package lo_e is\n  procedure p(x number);\n  procedure p(x date);\n  procedure run;\nend;\n/\n"
        val bothBody = "create package body lo_e is\n  procedure p(x number) is begin null; end;\n  procedure run is begin p(1); end;\n" +
            "  procedure p(x date) is begin null; end;\nend;\n/\n"
        assertThat(decided(bothSpec, bothBody, 3)).containsExactly(
            "3:25 p OverloadSet(PROCEDURE p@2:12, PROCEDURE p@4:12) -> LOCAL_CAPTURE RESOLVED PROCEDURE p@2:12",
            "4:16 date ProvenAbsent"
        )
    }

    @Test
    fun headingsMatchAsOracleMatchesThem() {
        // Case, an omitted IN, a DEFAULT written in the specification only and NOCOPY do not change the heading.
        val spec = "create package lo_h is\n  procedure p(X IN OUT NOCOPY NUMBER, Y in Varchar2 default 'a');\n  procedure run;\nend;\n/\n"
        val body = "create package body lo_h is\n  procedure run is n number; begin p(n, 'b'); end;\n" +
            "  procedure p(x in out number, y varchar2) is begin null; end;\nend;\n/\n"
        assertThat(decided(spec, body, 2)).contains("2:35 p OverloadSet(PROCEDURE p@3:12) -> LOCAL_CAPTURE RESOLVED PROCEDURE p@3:12")
    }

    @Test
    fun aQualifiedNameWhoseFirstSegmentOnlyTheSpecificationDeclares() {
        val spec = "create package lo_m is\n  type t_tab is table of number index by pls_integer;\n  type t_rec is record (f number);\n" +
            "  g_tab t_tab;\n  g_rec t_rec;\n  procedure run;\nend;\n/\n"
        val body = "create package body lo_m is\n  procedure run is n number; begin n := g_tab.count; n := g_rec.f; end;\nend;\n/\n"
        assertThat(split(spec, body, 2).filter { it.contains(" g_") }).containsExactly(
            "2:40 g_tab.count SpecificationMember",
            "2:58 g_rec.f SpecificationMember"
        )
    }

    private val defaultSpec = "create package lo_d is\n  procedure p(x number, y varchar2 default 'a');\n  procedure run;\nend;\n/\n"

    @Test
    fun aDefaultWrittenOnlyInTheSpecificationAppliesInTheBody() {
        val body = "create package body lo_d is\n  procedure p(x number, y varchar2) is begin null; end;\n" +
            "  procedure run is begin p(1); p(x => 1); end;\nend;\n/\n"
        assertThat(decided(defaultSpec, body, 3)).containsExactly(
            "3:25 p OverloadSet(PROCEDURE p@2:12) -> LOCAL_CAPTURE RESOLVED PROCEDURE p@2:12",
            "3:31 p OverloadSet(PROCEDURE p@2:12) -> LOCAL_CAPTURE RESOLVED PROCEDURE p@2:12"
        )
    }

    @Test
    fun aSpecificationDefaultMakesABodyOnlyOverloadAmbiguous() {
        // PLS-00307 on Oracle: p(1) matches the body-only p(x) and the public p(x, y) with its specification default.
        val body = "create package body lo_d is\n  procedure p(x number) is begin null; end;\n" +
            "  procedure p(x number, y varchar2) is begin null; end;\n  procedure run is begin p(1); end;\nend;\n/\n"
        assertThat(decided(defaultSpec, body, 4)).containsExactly(
            "4:25 p OverloadSet(PROCEDURE p@3:12, PROCEDURE p@2:12) -> LOCAL_CAPTURE AMBIGUOUS"
        )
    }

    private fun gapStays(spec: String, body: String, fromLine: Int, vararg others: Pair<String, String>) {
        val p = split(spec, body, fromLine, *others).single { Regex("^\\d+:\\d+ p ").containsMatchIn(it) }
        assertThat(p).contains("Incomplete[SPECIFICATION_NOT_IN_FILE]")
    }

    private val run = "create package body lo_g is\n  procedure run is begin p(1); end;\n  procedure p(x number) is begin null; end;\nend;\n/\n"

    @Test
    fun noSpecificationOrMoreThanOneKeepsTheGap() {
        val spec = "create package lo_g is\n  procedure p(x number);\n  procedure run;\nend;\n/\n"
        assertThat(split("create package unrelated is procedure q; end;\n/\n", run, 2).single { " p " in it })
            .contains("Incomplete[SPECIFICATION_NOT_IN_FILE]")
        gapStays(spec, run, 2, "s2.sql" to spec)
    }

    @Test
    fun aSpecificationMemberOfTheNameNotFullyKnownKeepsTheGap() {
        // A call specification the extractor does not retain.
        gapStays("create package lo_g is\n  procedure p(x number);\n  procedure p(x date) as language java name 'A.p(java.sql.Date)';\n" +
            "  procedure run;\nend;\n/\n", run, 2)
        // Conditional compilation around a member of the name.
        gapStays("create package lo_g is\n  procedure p(x number);\n  \$if \$\$x \$then procedure p(x date); \$end\n  procedure run;\nend;\n/\n", run, 2)
        // A parse gap in the specification.
        gapStays("create package lo_g is\n  procedure p(x number);\n  procedure q(x number := ;\n  procedure run;\nend;\n/\n", run, 2)
        // The name is also a value of the specification.
        gapStays("create package lo_g is\n  p number;\n  procedure run;\nend;\n/\n", run, 2)
    }

    @Test
    fun aSpecificationOverloadWithoutACertainDefinitionKeepsTheGap() {
        // p(x date) has no definition (PLS-00323).
        gapStays("create package lo_g is\n  procedure p(x number);\n  procedure p(x date);\n  procedure run;\nend;\n/\n", run, 2)
        // A %TYPE heading against the type it names (PLS-00323 on Oracle).
        gapStays("create package lo_g is\n  procedure p(x t.c%type);\n  procedure run;\nend;\n/\n", run, 2)
        // Two definitions of the heading (PLS-00305).
        gapStays("create package lo_g is\n  procedure p(x number);\n  procedure run;\nend;\n/\n",
            "create package body lo_g is\n  procedure run is begin p(1); end;\n  procedure p(x number) is begin null; end;\n" +
                "  procedure p(x number) is begin null; end;\nend;\n/\n", 2)
    }

    @Test
    fun aSchemaQualificationDifferenceKeepsTheGap() {
        val spec = "create package lo_g is\n  procedure p(x number);\n  procedure run;\nend;\n/\n"
        gapStays(spec.replace("package lo_g", "package app.lo_g"), run, 2)
        gapStays(spec, run.replace("body lo_g", "body app.lo_g"), 2)
    }
}
