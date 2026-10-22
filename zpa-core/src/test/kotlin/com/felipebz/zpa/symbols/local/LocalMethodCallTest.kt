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

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectSnapshot
import com.felipebz.zpa.api.project.ProjectSnapshots
import com.felipebz.zpa.api.project.ProjectSourceInput
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/**
 * Calls inside a type body to methods of the type: named alone or through `SELF`, bound with the implicit `SELF`
 * left out of the written actuals, and narrowed like any local overload set. Expectations are what Oracle printed, or
 * AMBIGUOUS where narrowing does not rank.
 */
@OptIn(ZpaExperimentalApi::class)
class LocalMethodCallTest {

    private fun calls(source: String, snapshot: ProjectSnapshot = ProjectSnapshots.prepare(emptyList(), false)): Pair<LocalSemanticModel, List<LocalCallAnalysis>> {
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(source)
        val model = LocalSemanticModelBuilder.build(root, FileId("types.sql"), source)
        val references = LocalReferenceCollector.collect(root, model)
        return model to LocalForwardCalls(model, references, LocalCallCollector.collect(root), snapshot).analyze()
    }

    /** Each call on [line] as `target STATUS [KIND line]`, or `target -` with no local resolution. */
    private fun resolutions(source: String, line: Int): List<String> {
        val (model, calls) = calls(source)
        return calls.filter { it.reference.range.startLine == line }.map { call ->
            val local = call.local ?: return@map "${call.target} -"
            val selected = local.selected?.let { model.declaration(it) }?.let { " ${it.kind} ${it.nameRange.startLine}" } ?: ""
            "${call.target} ${local.status}$selected"
        }
    }

    private val tm1 = "create type tm1 as object (\n  a number,\n  member procedure m(x number),\n  member procedure m(x date),\n" +
        "  member procedure run,\n  member procedure run2,\n  member procedure g(x number, y number default 1),\n" +
        "  static procedure s(x number),\n  static procedure from_static\n) not final;\n/\n" +
        "create type body tm1 as\n  member procedure m(x number) is begin null; end;\n  member procedure m(x date) is begin null; end;\n" +
        "  member procedure run is d date; begin m(1); self.m(sysdate); m(x => 1); self.m(d); self.m(self => self, x => 1); end;\n" +
        "  member procedure run2 is begin g(1); self.g(x => 1, y => 2); g(y => 1); s(1); self.s(1); end;\n" +
        "  member procedure g(x number, y number default 1) is begin null; end;\n" +
        "  static procedure s(x number) is begin null; end;\n  static procedure from_static is begin s(1); m(1); end;\nend;\n/\n"

    @Test
    fun membersNamedAloneOrThroughSelf() {
        // Oracle: m(1) and m(x => 1) run m(number), self.m(sysdate) m(date); a written SELF is PLS-00306.
        assertThat(resolutions(tm1, 15)).containsExactly(
            "m RESOLVED PROCEDURE 13",
            // No evidence for sysdate.
            "self.m AMBIGUOUS",
            "m RESOLVED PROCEDURE 13",
            "self.m RESOLVED PROCEDURE 14",
            "self.m NO_APPLICABLE_CANDIDATE"
        )
        // Positional, named, omitted defaults, a missing formal; statics named alone or through SELF from a member are
        // invoked on the instance (PLS-00587) and keep that rejection.
        assertThat(resolutions(tm1, 16)).containsExactly(
            "g RESOLVED PROCEDURE 17", "self.g RESOLVED PROCEDURE 17", "g NO_APPLICABLE_CANDIDATE", "s -", "self.s -"
        )
        val (_, calls) = calls(tm1)
        assertThat(calls.filter { it.reference.range.startLine == 16 }.drop(3).map { it.firstSegment.javaClass.simpleName })
            .containsExactly("WrongKind", "ImplicitSelf")
        assertThat(calls.first { it.reference.range.startLine == 16 }.local!!.candidates.single().omitted).containsExactly(2)
    }

    @Test
    fun aStaticCallsStaticsAndNeverMembers() {
        // Oracle: s(1) inside a static runs; a member from a static is rejected.
        assertThat(resolutions(tm1, 19)).containsExactly("s RESOLVED PROCEDURE 18", "m -")
        val (_, calls) = calls(tm1)
        assertThat(calls.last { it.reference.range.startLine == 19 }.firstSegment).isInstanceOf(LocalResolution.InstanceMemberInStatic::class.java)
    }

    @Test
    fun aProcedureAndAFunctionMemberEachInTheirForm() {
        val source = "create type tb as object (a number, member procedure p(x number), member function p(x number) return number, member procedure run);\n/\n" +
            "create type body tb as\n  member procedure p(x number) is begin null; end;\n  member function p(x number) return number is begin return 1; end;\n" +
            "  member procedure run is n number; begin p(1); n := p(1); end;\nend;\n/\n"
        assertThat(resolutions(source, 6)).containsExactly("p RESOLVED PROCEDURE 4", "p RESOLVED FUNCTION 5")
    }

    @Test
    fun anOverridingPairIsOneCandidate() {
        // Oracle runs the override for both calls, with no PLS-00307.
        val source = "create type d1 as object (a number, member procedure m(x number)) not final;\n/\n" +
            "create type body d1 as\n  member procedure m(x number) is begin null; end;\nend;\n/\n" +
            "create type d2 under d1 (overriding member procedure m(x number), member procedure run);\n/\n" +
            "create type body d2 as\n  overriding member procedure m(x number) is begin null; end;\n  member procedure run is begin self.m(1); m(1); end;\nend;\n/\n"
        assertThat(resolutions(source, 11)).containsExactly("self.m RESOLVED PROCEDURE 10", "m RESOLVED PROCEDURE 10")
    }

    @Test
    fun aSupertypeOverloadIsReachedThroughSelf() {
        val source = "create type e1 as object (a number, member procedure m(x number)) not final;\n/\n" +
            "create type body e1 as\n  member procedure m(x number) is begin null; end;\nend;\n/\n" +
            "create type e2 under e1 (member procedure m(x date), member procedure run);\n/\n" +
            "create type body e2 as\n  member procedure m(x date) is begin null; end;\n  member procedure run is d date; begin self.m(1); self.m(d); end;\nend;\n/\n"
        // The supertype's method is represented by its specification heading: its body is not part of the type scope.
        assertThat(resolutions(source, 11)).containsExactly("self.m RESOLVED PROCEDURE 1", "self.m RESOLVED PROCEDURE 10")
    }

    @Test
    fun anIncompleteTypeScopeIsNeverDecided() {
        // A supertype outside the file may declare further overloads, reached through SELF.
        val missing = "create type f2 under missing_t (member procedure m(x number), member procedure run);\n/\n" +
            "create type body f2 as\n  member procedure m(x number) is begin null; end;\n  member procedure run is begin self.m(1); end;\nend;\n/\n"
        assertThat(resolutions(missing, 5)).containsExactly("self.m INCOMPLETE")
        // Conditional compilation in the type body.
        val conditional = "create type g1 as object (a number, member procedure m(x number), member procedure run);\n/\n" +
            "create type body g1 as\n  member procedure m(x number) is begin null; end;\n" +
            "  member procedure run is begin m(1); end;\n  \$IF \$\$x \$THEN \$END\nend;\n/\n"
        assertThat(resolutions(conditional, 5)).containsExactly("m INCOMPLETE")
    }

    @Test
    fun callsThroughAValueOrATypeNameAreUnchanged() {
        val source = tm1 + "create procedure caller is\n  v tm1;\nbegin\n  v.m(1); tm1.s(1); v := tm1(1);\nend;\n/\n"
        val prepared = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("types.sql", source)), false)
        val line = calls(source, prepared).second.filter { it.reference.range.startLine == 25 }
        assertThat(line.map { "${it.target} ${it.outcome} ${it.local}" }).containsExactly(
            "v.m LOCAL_CAPTURE null", "tm1.s PROJECT null", "tm1 PROJECT null"
        )
        assertThat(LocalResolver.resolveQualified(calls(source).first, line.first().reference).last())
            .isEqualTo(LocalResolution.Unsupported(LocalUnsupportedConstruct.MEMBER_NEEDS_TYPING))
    }
}
