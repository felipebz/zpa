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

import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.SourceRange
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/** Segment ranges of qualified references, and names written as members of a value. */
class LocalSegmentAndMemberTest {

    private class Analysis(val references: List<LocalReference>, val members: List<LocalMember>)

    private fun analyze(source: String): Analysis {
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8)).parse(source)
        val model = LocalSemanticModelBuilder.build(root, FILE, source)
        return Analysis(LocalReferenceCollector.collect(root, model), LocalMemberCollector.collect(root, FILE))
    }

    private fun segments(analysis: Analysis, first: String): List<String> =
        analysis.references.single { it.name == OracleIdentifier.fromSource(first) && it.segments.isNotEmpty() }
            .segments.map { "${it.name}${if (it.isAttribute) "%" else ""}@${span(it.range)}" }

    private fun span(range: SourceRange) = "${range.startLine}:${range.startColumn}-${range.endLine}:${range.endColumn}"

    private fun members(source: String) = analyze(source).members.map { "${it.name}@${span(it.range)} after ${span(it.follows)}" }

    @Test
    fun everySegmentOfAQualifiedNameHasItsRange() {
        val analysis = analyze("begin\n  x := a.b.c;\n  y := \"A b\".c;\nend;\n/\n")
        assertThat(segments(analysis, "a")).containsExactly("b@2:9-2:10", "c@2:11-2:12")
        assertThat(segments(analysis, "\"A b\"")).containsExactly("c@3:13-3:14")
    }

    @Test
    fun segmentsOfAWrittenTypeAndOfALabelQualifiedName() {
        val analysis = analyze("<<block>>\ndeclare\n  v pkg.v%type;\nbegin\n  block.v := 1;\nend;\n/\n")
        assertThat(segments(analysis, "pkg")).containsExactly("v@3:8-3:9")
        assertThat(segments(analysis, "block")).containsExactly("v@5:8-5:9")
    }

    @Test
    fun aSegmentAfterPercentIsAnAttributeWithItsRange() {
        val analysis = analyze("begin\n  if c%found then\n    null;\n  end if;\nend;\n/\n")
        assertThat(segments(analysis, "c")).containsExactly("found%@2:7-2:12")
    }

    @Test
    fun namesAfterACallOrAnElementAccessAreMembers() {
        assertThat(members("begin\n  x := a(i).b.c;\nend;\n/\n"))
            .containsExactly("b@2:12-2:13 after 2:7-2:11", "c@2:14-2:15 after 2:7-2:13")
        assertThat(members("begin\n  x := a(i).b(j).c;\nend;\n/\n"))
            .containsExactly("b@2:12-2:13 after 2:7-2:11", "c@2:17-2:18 after 2:7-2:16")
        assertThat(members("begin\n  f(x).field := 1;\nend;\n/\n")).containsExactly("field@2:7-2:12 after 2:2-2:6")
    }

    @Test
    fun namesAfterTreatOrAParenthesizedExpressionAreMembers() {
        assertThat(members("begin\n  x := treat(v as t).n;\nend;\n/\n")).containsExactly("n@2:21-2:22 after 2:7-2:20")
        assertThat(members("begin\n  x := (y).n;\nend;\n/\n")).containsExactly("n@2:11-2:12 after 2:7-2:10")
    }

    @Test
    fun segmentsOfAWrittenTypeComeFromItsQualifiedName() {
        val analysis = analyze("declare\n  v a.a%type;\n  w \"P k\".t%type;\nbegin\n  null;\nend;\n/\n")
        assertThat(segments(analysis, "a")).containsExactly("a@2:6-2:7")
        assertThat(segments(analysis, "\"P k\"")).containsExactly("t@3:10-3:11")
    }

    @Test
    fun aStarAfterARelationIsNotAMember() {
        assertThat(members("begin\n  select d1.*, f(x).n into r from dual;\nend;\n/\n")).containsExactly("n@2:20-2:21 after 2:15-2:19")
    }

    @Test
    fun namesInTheSqlCallCommandAreMembers() {
        assertThat(members("CALL warehouse_typ(456, 'w').ret_name() INTO :x;\n")).containsExactly("ret_name@1:29-1:37 after 1:5-1:28")
    }

    @Test
    fun anOrdinaryQualifiedNameHasNoMember() {
        assertThat(members("begin\n  x := pkg.v;\n  pkg.p(1);\nend;\n/\n")).isEmpty()
    }

    @Test
    fun membersBindNothing() {
        val analysis = analyze("declare\n  b number;\nbegin\n  x := a(i).b;\nend;\n/\n")
        assertThat(analysis.members).hasSize(1)
        assertThat(analysis.references.map { it.name.toString() }).doesNotContain("b")
    }

    private companion object {
        val FILE = FileId("m.sql")
    }
}
