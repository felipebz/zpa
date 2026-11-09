//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectSnapshot
import com.felipebz.zpa.api.project.ProjectSnapshots
import com.felipebz.zpa.api.project.ProjectSourceInput
import com.felipebz.zpa.api.project.ProjectTypedCallResolution
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectCallCandidate.Applicability
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/**
 * Calls to local overload sets: each entity bound with the project's parameter-list binding, then narrowed with the
 * project's datatype comparison, formals walked at their declaration site. Expectations are what Oracle printed, or
 * AMBIGUOUS where Oracle ranks compatible overloads.
 */
@OptIn(ZpaExperimentalApi::class)
class LocalOverloadTest {

    private class Analysis(val model: LocalSemanticModel, val calls: List<LocalCallAnalysis>)

    private val empty = ProjectSnapshots.prepare(emptyList(), false)

    private fun analyze(source: String, snapshot: ProjectSnapshot = empty): Analysis {
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(source)
        val model = LocalSemanticModelBuilder.build(root, FileId("local.sql"), source)
        val references = LocalReferenceCollector.collect(root, model)
        return Analysis(model, LocalForwardCalls(model, references, LocalCallCollector.collect(root), snapshot).analyze())
    }

    /** Each call to a local set as `line: target STATUS`, plus `KIND line` of the selected entity. */
    private fun resolutions(source: String): List<String> {
        val analysis = analyze(source)
        return analysis.calls.filter { it.local != null }.map { call ->
            val local = call.local!!
            val selected = local.selected?.let { analysis.model.declaration(it) }?.let { " ${it.kind} ${it.nameRange.startLine}" } ?: ""
            "${call.reference.range.startLine}: ${call.target} ${local.status}$selected"
        }
    }

    private fun local(source: String, line: Int, occurrence: Int = 0): LocalCallResolution =
        analyze(source).calls.filter { it.local != null && it.reference.range.startLine == line }[occurrence].local!!

    @Test
    fun aSpecificationAndABodyPrivateSubprogramFormOneSet() {
        // Oracle: p(sysdate) runs the body-private DATE overload, p(1) the specification's NUMBER one.
        val source = "create package lo_a is\n  procedure p(x number);\n  procedure run;\nend;\n/\n" +
            "create package body lo_a is\n  d date;\n  procedure p(x date) is begin null; end;\n  procedure p(x number) is begin null; end;\n" +
            "  procedure run is begin p(sysdate); p(1); p(d); end;\nend;\n/\n"
        assertThat(resolutions(source)).containsExactly(
            // sysdate is a DATE.
            "10: p RESOLVED PROCEDURE 8",
            "10: p RESOLVED PROCEDURE 9",
            "10: p RESOLVED PROCEDURE 8"
        )
        assertThat(local(source, 10).candidates).hasSize(2)
    }

    @Test
    fun anInnerSetHidesTheOuterOneEntirely() {
        // Oracle: PLS-00306 for p(sysdate); the outer p(date) is never considered.
        val source = "declare\n  d date;\n  procedure p(x date) is begin null; end;\nbegin\n  declare\n    procedure p(x number) is begin null; end;\n" +
            "  begin\n    p(sysdate);\n    p(d);\n  end;\nend;\n/\n"
        assertThat(resolutions(source)).containsExactly(
            // sysdate is a DATE, which the only candidate rejects.
            "8: p NO_COMPATIBLE_CANDIDATE",
            "9: p NO_COMPATIBLE_CANDIDATE"
        )
    }

    @Test
    fun aForwardDeclarationAndItsBodyAreOneCandidate() {
        val source = "declare\n  d date;\n  procedure p(x number);\n  procedure p(x date) is begin null; end;\n  procedure p(x number) is begin null; end;\n" +
            "begin\n  p(1); p(d);\nend;\n/\n"
        assertThat(resolutions(source)).containsExactly("7: p RESOLVED PROCEDURE 5", "7: p RESOLVED PROCEDURE 4")
        assertThat(local(source, 7).candidates).hasSize(2)
    }

    @Test
    fun aDefaultedParameterOrAParameterNameDistinguishesOnlyThroughNotation() {
        // Oracle: PLS-00307, p(a,b), PLS-00307.
        val defaulted = "declare\n  procedure p(a number) is begin null; end;\n  procedure p(a number, b number default 1) is begin null; end;\n" +
            "begin\n  p(1); p(1, 2); p(a => 1);\nend;\n/\n"
        assertThat(resolutions(defaulted)).containsExactly("5: p AMBIGUOUS", "5: p RESOLVED PROCEDURE 3", "5: p AMBIGUOUS")
        // Oracle: p(a), PLS-00307.
        val named = "declare\n  procedure p(a number) is begin null; end;\n  procedure p(b number) is begin null; end;\n" +
            "begin\n  p(a => 1); p(1);\nend;\n/\n"
        assertThat(resolutions(named)).containsExactly("5: p RESOLVED PROCEDURE 2", "5: p AMBIGUOUS")
        assertThat(local(named, 5).candidates.map { it.applicability }).containsExactly(Applicability.APPLICABLE, Applicability.UNKNOWN_LABEL)
    }

    @Test
    fun anIntegerLiteralBindsThePlsIntegerOverload() {
        // Oracle runs the PLS_INTEGER overload for a literal 1.
        val source = "declare\n  procedure p(x number) is begin null; end;\n  procedure p(x pls_integer) is begin null; end;\nbegin\n  p(1);\nend;\n/\n"
        assertThat(resolutions(source)).containsExactly("5: p RESOLVED PROCEDURE 3")
    }

    @Test
    fun aStatementCallsTheProcedureAndAnExpressionTheFunction() {
        // Oracle: a procedure and a function of one name coexist; each call form reaches its own.
        val source = "declare\n  v number;\n  procedure p(x number) is begin null; end;\n  function p(x number) return number is begin return 7; end;\n" +
            "begin\n  p(1);\n  v := p(1);\nend;\n/\n"
        assertThat(resolutions(source)).containsExactly("6: p RESOLVED PROCEDURE 3", "7: p RESOLVED FUNCTION 4")
        assertThat(local(source, 6).candidates.map { it.applicability }).containsExactly(Applicability.APPLICABLE, Applicability.WRONG_KIND)
    }

    @Test
    fun notationIsBoundAsTheProjectBindsIt() {
        val source = "declare\n  procedure g(a number, b number default 2, c number default 3) is begin null; end;\nbegin\n" +
            "  g(1); g(1, c => 9); g(c => 7, a => 0);\n  g(1, d => 2); g(1, a => 2); g(1, 2, 3, 4); g(b => 1); g(a => 1, 5);\nend;\n/\n"
        val analysis = analyze(source)
        fun candidate(line: Int, occurrence: Int) = analysis.calls.filter { it.reference.range.startLine == line }[occurrence].local!!
        // Positional, mixed and named in any order; omitted defaults in formal order.
        assertThat((0..2).map { candidate(4, it).status }).containsOnly(LocalCallResolution.Status.RESOLVED)
        assertThat(candidate(4, 0).candidates.single().omitted).containsExactly(2, 3)
        assertThat(candidate(4, 1).candidates.single().omitted).containsExactly(2)
        assertThat(candidate(4, 2).candidates.single().bindings.map { it.parameterOrdinal }).containsExactly(3, 1)
        // An unknown label, a formal bound twice, too many actuals, a missing required formal; then invalid notation.
        val rejected = (0..3).map { candidate(5, it) }
        assertThat(rejected.map { it.status }).containsOnly(LocalCallResolution.Status.NO_APPLICABLE_CANDIDATE)
        assertThat(rejected.map { it.candidates.single().let { c -> "${c.applicability} ${c.rejectedArgumentOrdinal} ${c.missing}" } }).containsExactly(
            "UNKNOWN_LABEL 2 []", "DUPLICATE_BINDING 2 []", "TOO_MANY_ARGUMENTS 4 []", "MISSING_REQUIRED null [1]"
        )
        assertThat(candidate(5, 4).status).isEqualTo(LocalCallResolution.Status.INVALID_NOTATION)
        assertThat(candidate(5, 4).candidates).isEmpty()
    }

    @Test
    fun formalsAndActualsAreWalkedLocally() {
        val source = "declare\n  subtype dt_t is date;\n  d date;\n  s dt_t;\n  procedure p(x number) is begin null; end;\n" +
            "  procedure p(x date) is begin null; end;\n  procedure q(x dt_t) is begin null; end;\n  procedure q(x boolean) is begin null; end;\n" +
            "begin\n  p(d); p(s); q(1); q(d); p(sysdate); p(null);\n" +
            "  declare\n    subtype number is date;\n    v number;\n  begin\n    p(v);\n  end;\nend;\n/\n"
        assertThat(resolutions(source)).containsExactly(
            "10: p RESOLVED PROCEDURE 6",
            // A local subtype as the actual.
            "10: p RESOLVED PROCEDURE 6",
            // A local subtype as the formal: q(dt_t) is a DATE, which a number cannot reach; NUMBER to BOOLEAN is not a
            // pair the compatibility matrix decides, so q(boolean) stays.
            "10: q RESOLVED PROCEDURE 8",
            "10: q RESOLVED PROCEDURE 7",
            // sysdate is STANDARD's DATE; NULL keeps every candidate.
            "10: p RESOLVED PROCEDURE 6",
            "10: p AMBIGUOUS",
            // A predefined-type-named local subtype as the actual; the formals keep their own declaration site.
            "15: p RESOLVED PROCEDURE 6"
        )
        // sysdate rejects p(number) and reaches p(date); NULL proves nothing.
        assertThat(local(source, 10, 4).candidates.flatMap { it.bindings }.map { it.outcome.name }).containsExactly("INCOMPATIBLE", "COMPATIBLE")
        assertThat(local(source, 10, 5).candidates.flatMap { it.bindings }.map { it.reason?.name }).containsOnly("NULL_LITERAL")
        // A predefined-type-named local subtype as the formal: p(x date) is a NUMBER, which a DATE literal cannot reach.
        val shadowing = "declare\n  subtype date is number;\n  procedure p(x date) is begin null; end;\n  procedure p(x varchar2) is begin null; end;\n" +
            "begin\n  p(date '2020-01-01');\nend;\n/\n"
        assertThat(resolutions(shadowing)).containsExactly("6: p RESOLVED PROCEDURE 4")
    }

    @Test
    fun anIncompleteSetIsNeverDecided() {
        // Conditional compilation in the deciding region that writes `p` may declare another overload.
        val conditional = "declare\n  procedure p(x number) is begin null; end;\n  \$IF \$\$x \$THEN v number; \$ELSE procedure p(x date) is begin null; end; \$END\nbegin\n  p(1);\nend;\n/\n"
        assertThat(resolutions(conditional)).containsExactly("5: p INCOMPLETE")
        // Text that never writes `p` declares no overload of it.
        assertThat(resolutions(conditional.replace("\$ELSE procedure p(x date) is begin null; end; ", ""))).containsExactly("5: p RESOLVED PROCEDURE 2")
        // A package body whose specification is not in the file: the specification may declare another overload.
        val lone = "create package body b is\n  procedure p(x number) is begin null; end;\n  procedure t is begin p(1); end;\nend;\n/\n"
        assertThat(resolutions(lone)).containsExactly("3: p INCOMPLETE")
        assertThat(local(lone, 3).candidates).isEmpty()
    }

    @Test
    fun aCallSpecificationShowsItsParameters() {
        val source = "declare\n  d date;\n  procedure p(x number) is language java name 'P.p(int)';\n  procedure p(x date) is begin null; end;\n" +
            "begin\n  p(d);\nend;\n/\n"
        assertThat(resolutions(source)).containsExactly("6: p RESOLVED PROCEDURE 4")
    }

    @Test
    fun aResolvedLocalFunctionGivesItsResult() {
        val snapshot = ProjectSnapshots.prepare(
            listOf(ProjectSourceInput.ofText("pkg.sql", "create package pkg as\n  procedure processar(p number);\n  procedure processar(p date);\nend;\n/\n")), false
        )
        val source = "declare\n  d date;\n  cursor c is select 1 a from dual;\n  function f(x date) return date is begin return null; end;\n" +
            "  function f(x boolean) return number is begin return 0; end;\nbegin\n" +
            "  pkg.processar(f(d)); pkg.processar(f(true)); pkg.processar(f(null));\n  for c in c loop pkg.processar(c); end loop;\nend;\n/\n"
        val calls = analyze(source, snapshot).calls.filter { it.target.toString() == "pkg.processar" }.map { call ->
            val typed = call.typed!!
            val selected = if (typed.status == ProjectTypedCallResolution.Status.RESOLVED) " " + typed.selected.get().declaration.parameters.single().typeRef.name else ""
            "${call.actuals.single().evidence.argumentType} ${typed.status}$selected"
        }
        assertThat(calls).containsExactly(
            "BUILT_IN DATETIME RESOLVED date",
            "BUILT_IN NUMERIC RESOLVED number",
            // f(null) is ambiguous: no result evidence.
            "UNKNOWN AMBIGUOUS",
            // `for c in c`: the control is the cursor, looked up where the loop is written, so the index is a row.
            "COMPOSITE COMPOSITE NO_COMPATIBLE_CANDIDATE"
        )
    }

    private val processar = ProjectSnapshots.prepare(
        listOf(ProjectSourceInput.ofText("pkg.sql", "create package pkg as\n  procedure processar(p number);\n  procedure processar(p date);\nend;\n/\n" +
            "create package pf as\n  type dates_t is table of date;\n  function d return dates_t;\nend;\n/\n")), false
    )

    /** The actual's evidence and the call's status, for each call to `p` or `pkg.processar` on [line]. */
    private fun actuals(source: String, line: Int): List<String> = analyze(source, processar).calls
        .filter { it.reference.range.startLine == line && it.target.toString().let { t -> t == "p" || t == "pkg.processar" } }
        .map { "${it.target} ${it.actuals.single().evidence.argumentType} ${it.typed?.status ?: it.local!!.status}" }

    @Test
    fun anActualWithChainedArgumentListsIsAnElementOfTheResultBefore() {
        // Oracle runs the DATE overloads: an element of an element, an element of a function result.
        val source = "declare\n  type nt is table of date;\n  type nnt is table of nt;\n  c nnt;\n  n nt;\n" +
            "  function h return nt is begin return null; end;\n  function f(x number) return date is begin return null; end;\n" +
            "  procedure p(x date) is begin null; end;\n  procedure p(x number) is begin null; end;\nbegin\n" +
            "  pkg.processar(c(1)(2)); p(h()(1)); pkg.processar(pf.d()(1));\n" +
            "  pkg.processar(n(1)); p(h()); pkg.processar(f(1));\nend;\n/\n"
        assertThat(actuals(source, 11)).containsExactly(
            "pkg.processar BUILT_IN DATETIME RESOLVED", "p BUILT_IN DATETIME RESOLVED", "pkg.processar DESCRIBED DATETIME RESOLVED"
        )
        // One argument list keeps its evidence.
        assertThat(actuals(source, 12)).containsExactly(
            "pkg.processar BUILT_IN DATETIME RESOLVED", "p COMPOSITE COMPOSITE NO_COMPATIBLE_CANDIDATE", "pkg.processar BUILT_IN DATETIME RESOLVED"
        )
    }

    @Test
    fun aParameterlessFunctionCalledWithActualsIsRejected() {
        // Oracle: PLS-00306 for h(1) as an actual and as an assigned value; with h(x varchar2) beside it, h(1) calls that one.
        val nt = "  type nt is table of date;\n  function h return nt is begin return null; end;\n"
        val asActual = "declare\n$nt  procedure p(x date) is begin null; end;\n  procedure p(x number) is begin null; end;\nbegin\n  p(h(1));\nend;\n/\n"
        assertThat(resolutions(asActual)).containsExactly("7: p AMBIGUOUS", "7: h NO_APPLICABLE_CANDIDATE")
        assertThat(local(asActual, 7, 1).candidates.single().applicability).isEqualTo(Applicability.TOO_MANY_ARGUMENTS)
        val assigned = "declare\n  d date;\n${nt}begin\n  d := h(1);\nend;\n/\n"
        assertThat(resolutions(assigned)).containsExactly("6: h NO_APPLICABLE_CANDIDATE")
        val mixed = "declare\n$nt  function h(x varchar2) return number is begin return 5; end;\n" +
            "  procedure p(x date) is begin null; end;\n  procedure p(x number) is begin null; end;\nbegin\n  p(h(1));\nend;\n/\n"
        assertThat(resolutions(mixed)).containsExactly("8: p RESOLVED PROCEDURE 6", "8: h RESOLVED FUNCTION 4")
    }
}
