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
import com.felipebz.flr.api.Token
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Calls from local code to the Project System: the first segment's outcome decides whether the project is consulted,
 * and each written actual supplies datatype evidence from the local type walk. Overloads are those Oracle printed.
 */
@OptIn(ZpaExperimentalApi::class)
class LocalForwardCallTest {

    internal class Analysis(val model: LocalSemanticModel, val references: List<LocalReference>, val facts: List<LocalCall>, val calls: List<LocalCallAnalysis>)

    private val snapshot = ProjectSnapshots.prepare(
        listOf(ProjectSourceInput.ofText("pkg.sql", "create package pkg as\n  procedure processar(p number);\n  procedure processar(p date);\nend pkg;\n/\n")),
        false
    )

    internal fun analyze(source: String, snapshot: ProjectSnapshot, fileId: String = "caller.sql", recovery: Boolean = false): Analysis {
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, recovery)).parse(source)
        val model = LocalSemanticModelBuilder.build(root, FileId(fileId), source)
        val references = LocalReferenceCollector.collect(root, model)
        val facts = LocalCallCollector.collect(root)
        return Analysis(model, references, facts, LocalForwardCalls(model, references, facts, snapshot).analyze())
    }

    private fun formal(candidate: ProjectCallCandidate) = candidate.declaration.parameters.joinToString(",") { it.typeRef.name.toString() }

    /** A call as `line: outcome target(evidence…) -> status selected-formals`. */
    private fun render(call: LocalCallAnalysis): String {
        val typed = call.typed
        val result = when {
            typed == null -> ""
            typed.status == ProjectTypedCallResolution.Status.RESOLVED -> " -> RESOLVED ${formal(typed.selected.get())}"
            else -> " -> ${typed.status}"
        }
        return "${call.reference.range.startLine}: ${call.outcome} ${call.target}(${call.actuals.joinToString(", ") { it.evidence.argumentType.toString() }})$result"
    }

    private fun block(declarations: String, call: String = "pkg.processar(v);") = "DECLARE\n$declarations\nBEGIN\n  $call\nEND;\n/\n"

    private fun single(source: String) = analyze(source, snapshot).calls.single { it.target.toString().endsWith("processar") }

    @Test
    fun aLocalSubtypeSuppliesItsDatatype() {
        // Oracle runs pkg.processar(DATE).
        val analysis = analyze(block("  SUBTYPE amount_t IS DATE;\n  v amount_t;"), snapshot)
        val call = analysis.calls.single()
        assertThat(call.firstSegment).isEqualTo(LocalResolution.ProvenAbsent)
        assertThat(call.outcome).isEqualTo(LocalCallOutcome.PROJECT)
        val evidence = call.actuals.single().evidence
        assertThat(evidence.terminal).isEqualTo(LocalTypeEvidence.Terminal.BUILT_IN)
        assertThat(evidence.followed.map { analysis.model.declaration(it).name.lookupName }).containsExactly("V", "AMOUNT_T")
        // The structural result keeps both overloads; narrowing removes NUMBER.
        val typed = call.typed!!
        assertThat(typed.structural.status).isEqualTo(ProjectCallResolution.Status.AMBIGUOUS)
        assertThat(typed.structural.candidates.map(::formal)).containsExactly("number", "date")
        assertThat(typed.status).isEqualTo(ProjectTypedCallResolution.Status.RESOLVED)
        assertThat(typed.selected.get()).isSameAs(typed.structural.candidates[1])
    }

    @Test
    fun aLocalSubtypeNamedLikeAPredefinedTypeWins() {
        // A local SUBTYPE number IS DATE makes `v number` a DATE.
        val analysis = analyze(block("  SUBTYPE number IS DATE;\n  v number;"), snapshot)
        assertThat(analysis.calls.map(::render)).containsExactly("5: PROJECT pkg.processar(BUILT_IN DATETIME) -> RESOLVED date")
        assertThat(analysis.calls.single().actuals.single().evidence.followed.map { analysis.model.declaration(it).kind.name })
            .containsExactly("VARIABLE", "SUBTYPE")
    }

    @Test
    fun aLocalCaptureOfTheQualifierNeverReachesTheProject() {
        // A local `pkg` hides the package (PLS-00487); no fallback.
        val call = single(block("  pkg NUMBER;", "pkg.processar(1);"))
        assertThat(call.outcome).isEqualTo(LocalCallOutcome.LOCAL_CAPTURE)
        assertThat(call.firstSegment).isInstanceOf(LocalResolution.Bound::class.java)
        assertThat(call.typed).isNull()
        // A label named like the package captures it too.
        val labeled = single("BEGIN\n  <<pkg>>\n  DECLARE v DATE;\n  BEGIN\n    pkg.processar(v);\n  END;\nEND;\n/\n")
        assertThat(labeled.outcome).isEqualTo(LocalCallOutcome.LOCAL_CAPTURE)
        assertThat(labeled.typed).isNull()
    }

    @Test
    fun anIncompleteChainNeverReachesTheProject() {
        // Conditional compilation in the declarative part may declare `pkg`.
        val conditional = single(block("  v DATE;\n  \$IF \$\$debug \$THEN pkg NUMBER; \$END"))
        assertThat(conditional.outcome).isEqualTo(LocalCallOutcome.INCOMPLETE)
        assertThat(conditional.typed).isNull()
        // A package body whose specification is not in the file may declare `pkg`.
        val lone = analyze("create package body b is\n  procedure t is begin pkg.processar(1); end;\nend;\n/\n", snapshot).calls.single()
        assertThat(lone.outcome).isEqualTo(LocalCallOutcome.INCOMPLETE)
        assertThat(lone.firstSegment.toString()).contains("SPECIFICATION_NOT_IN_FILE")
        assertThat(lone.typed).isNull()
        // Recovery may have detached a declaration of `pkg`.
        val recovered = analyze(block("  pkg NUMBER;\n  ??? garbage;", "pkg.processar(1);"), snapshot, recovery = true).calls
            .filter { it.target.toString().endsWith("processar") }
        assertThat(recovered).allSatisfy { assertThat(it.typed).isNull() }
        // An unmodeled outcome: a reference in a parameter default.
        val unsupported = analyze("create procedure p(a number := pkg.f(1)) is begin null; end;\n/\n", snapshot).calls.single()
        assertThat(unsupported.outcome).isEqualTo(LocalCallOutcome.UNSUPPORTED)
        assertThat(unsupported.typed).isNull()
    }

    @Test
    fun literalsAndNotationAreTheWrittenActuals() {
        val call = single(
            block(
                "  v DATE;",
                "pkg.processar(1, 1.5d, 2f, 'a', '', N'x', NULL, TRUE, DATE '2020-01-01', TIMESTAMP '2020-01-01 00:00:00', " +
                    "INTERVAL '1' DAY, -1, p => v);"
            )
        )
        assertThat(call.actuals.map { it.actual.literal?.name }).containsExactly(
            "NUMBER", "BINARY_DOUBLE", "BINARY_FLOAT", "CHAR", "CHAR", "NCHAR", "NULL", "BOOLEAN", "DATE", "TIMESTAMP", "INTERVAL_DAY_TO_SECOND", null, null
        )
        assertThat(call.actuals.map { it.evidence.argumentType.toString() }).containsExactly(
            "BUILT_IN NUMERIC", "BUILT_IN NUMERIC", "BUILT_IN NUMERIC", "BUILT_IN CHARACTER", "BUILT_IN CHARACTER",
            "BUILT_IN CHARACTER", "NULL_LITERAL", "BUILT_IN BOOLEAN", "BUILT_IN DATETIME", "BUILT_IN DATETIME", "BUILT_IN INTERVAL",
            "UNKNOWN", "BUILT_IN DATETIME"
        )
        assertThat(call.actuals.last().actual.label!!.lookupName).isEqualTo("P")
        assertThat(call.typed!!.structural.arguments.arguments.map { it.isNamed }.last()).isTrue()
        // No such overload: structurally nothing applies, whatever the evidence.
        assertThat(call.typed!!.status).isNotEqualTo(ProjectTypedCallResolution.Status.RESOLVED)
    }

    /** The calls to `pkg.processar` of a comparison source, the package prepared from that same source. */
    private fun processarCalls(name: String): List<String> {
        val text = File("src/test/resources/local-semantic/sources/$name.sql").readText()
        val snapshot = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("$name.sql", text)), false)
        return analyze(text, snapshot, "$name.sql").calls.filter { it.target.toString().endsWith("processar", ignoreCase = true) }.map(::render)
    }

    @Test
    fun theOverloadsOraclePrinted() {
        assertThat(processarCalls("local_subtype_argument")).containsExactly("24: PROJECT pkg.processar(BUILT_IN DATETIME) -> RESOLVED date")
        assertThat(processarCalls("local_subtype_argument_variants")).containsExactly(
            "27: PROJECT pkg.processar(BUILT_IN NUMERIC) -> RESOLVED number",    // inner amount_t NUMBER
            "40: PROJECT pkg.processar(BUILT_IN DATETIME) -> RESOLVED date",     // outer v keeps the outer amount_t
            "41: PROJECT pkg.processar(BUILT_IN NUMERIC) -> RESOLVED number",
            "49: PROJECT pkg.processar(DESCRIBED DATETIME) -> RESOLVED date",    // v pkg_other.g_amt%TYPE: the project's anchor
            "56: PROJECT pkg.processar(DESCRIBED DATETIME) -> RESOLVED date",    // v pkg_other.amt_t
            "63: PROJECT pkg.processar(BUILT_IN NUMERIC) -> RESOLVED number",
            "68: PROJECT pkg.processar(BUILT_IN NUMERIC) -> RESOLVED number",    // literal 1
            "69: PROJECT pkg.processar(UNKNOWN) -> AMBIGUOUS",                   // sysdate: Oracle DATE; a name proven absent has no evidence
            "70: PROJECT pkg.processar(BUILT_IN DATETIME) -> RESOLVED date",     // DATE literal
            "78: PROJECT pkg.processar(DESCRIBED NUMERIC) -> RESOLVED number",   // local subtype of pkg_other.amt_n
            "90: PROJECT pkg.processar(BUILT_IN DATETIME) -> RESOLVED date",     // o.v, the labeled outer v
            "91: PROJECT pkg.processar(BUILT_IN NUMERIC) -> RESOLVED number",
            "101: PROJECT pkg.processar(BUILT_IN DATETIME) -> RESOLVED date",    // local SUBTYPE integer IS DATE
            "110: PROJECT pkg.processar(BUILT_IN DATETIME) -> RESOLVED date",    // v a%TYPE
            "119: PROJECT pkg.processar(BUILT_IN DATETIME) -> RESOLVED date",    // r.f, a record field
            "135: PROJECT pkg.processar(DESCRIBED) -> AMBIGUOUS",                // %TYPE on a package subtype: PLS-00206 at the declaration
            "138: PROJECT pkg.processar(BUILT_IN CHARACTER) -> AMBIGUOUS",       // PLS-00307
            "141: PROJECT pkg.processar(NULL_LITERAL) -> AMBIGUOUS",             // PLS-00307
            "144: LOCAL_CAPTURE pkg.processar(BUILT_IN DATETIME)",               // PLS-00487
            "147: PROJECT pkg.processar(BUILT_IN DATETIME) -> RESOLVED date",    // the source is invalid for Oracle (PLS-00103)
            "147: LOCAL_CAPTURE processar(BUILT_IN DATETIME)",
            "150: PROJECT pkg.processar(BUILT_IN NUMERIC) -> RESOLVED number",   // the sibling block's subtype does not leak
            "153: PROJECT pkg.processar(BUILT_IN CHARACTER) -> AMBIGUOUS",       // PLS-00307
            "156: PROJECT pkg.processar(BUILT_IN DATETIME) -> RESOLVED date",    // TIMESTAMP
            "159: PROJECT pkg.processar(BUILT_IN NUMERIC) -> RESOLVED number"    // PLS_INTEGER
        )
        assertThat(processarCalls("predefined_type_named_local_subtype_argument")).containsExactly(
            "31: PROJECT pkg.processar(BUILT_IN DATETIME) -> RESOLVED date",     // local SUBTYPE number IS DATE
            "34: PROJECT pkg.processar(BUILT_IN DATETIME) -> RESOLVED date",
            "34: LOCAL_CAPTURE processar(BUILT_IN DATETIME)",                    // the local processar(varchar2)
            "37: PROJECT pkg.processar(BUILT_IN DATETIME) -> RESOLVED date",     // local SUBTYPE varchar2 IS DATE
            "40: PROJECT pkg.processar(BUILT_IN DATETIME) -> RESOLVED date",     // an anchor to the outer a
            "43: PROJECT pkg.processar(UNKNOWN) -> AMBIGUOUS",                   // a function's result: Oracle DATE, no evidence
            "46: PROJECT pkg.processar(UNKNOWN) -> AMBIGUOUS",                   // an element c(1): Oracle DATE, no evidence
            "49: PROJECT pkg.processar(UNKNOWN) -> AMBIGUOUS",                   // v's anchor is captured by a local (PLS-00487)
            "52: PROJECT pkg.processar(UNKNOWN) -> AMBIGUOUS"                    // NVL(...): Oracle DATE, no evidence
        )
    }

    @Test
    fun projectCalleesOfTheComparisonAreResolved() {
        fun projectCalls(name: String): List<String> {
            val text = File("src/test/resources/local-semantic/sources/$name.sql").readText()
            val snapshot = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("$name.sql", text)), false)
            return analyze(text, snapshot, "$name.sql").calls.filter { it.outcome == LocalCallOutcome.PROJECT && it.target.segments.size > 1 }
                .map { "${it.target} -> ${it.typed!!.status}" }
        }
        // Package members called from a standalone procedure: the members Oracle ran.
        assertThat(projectCalls("local_and_package_subprograms")).containsExactly("pk_s.sp -> RESOLVED", "pk_s.caller -> RESOLVED", "pk_s.caller2 -> RESOLVED")
        assertThat(projectCalls("local_overloads_and_outer_subprograms")).containsExactly("pk_n.run -> RESOLVED")
        assertThat(projectCalls("package_specification_and_body")).containsExactly("pk_ok.a -> RESOLVED", "pk_q.show -> RESOLVED")
        // cfg.x(r): the package procedure x, not the parameter's field; rv.f needs the field's type.
        assertThat(projectCalls("parameter_named_like_its_package")).containsExactly("ov.k -> AMBIGUOUS", "cfg.x -> RESOLVED")
    }

    @Test
    fun aShadowedPredefinedNameFromTheProjectNeverEliminates() {
        // The project describes `g integer` as the predefined family, with the package as the scope that may shadow it:
        // narrowing finds the package's own `integer` and keeps both overloads.
        val text = "create package pk is\n  subtype integer is date;\n  g integer;\n  procedure processar(p number);\n  procedure processar(p date);\nend;\n/\n" +
            "create procedure outside is begin pk.processar(pk.g); end;\n/\n"
        val prepared = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("s.sql", text)), false)
        assertThat(analyze(text, prepared, "s.sql").calls.map(::render)).containsExactly("8: PROJECT pk.processar(DESCRIBED NUMERIC) -> AMBIGUOUS")
    }

    @Test
    fun callsAndFactsAreDeterministicAndDetached() {
        val source = block("  SUBTYPE amount_t IS DATE;\n  TYPE r_t IS RECORD (f amount_t);\n  r r_t;\n  v r.f%TYPE;", "pkg.processar(v); pkg.processar(r.f); pkg.processar('a');")
        val first = analyze(source, snapshot)
        assertThat(analyze(source, snapshot).calls.map(::render)).isEqualTo(first.calls.map(::render))
        // The order references are analyzed in does not matter.
        val reversed = LocalForwardCalls(first.model, first.references.reversed(), first.facts.reversed(), snapshot).analyze().map(::render)
        assertThat(reversed.reversed()).isEqualTo(first.calls.map(::render))
        assertThat(first.calls.map(::render)).allMatch { it.endsWith("RESOLVED date") || it.contains("CHARACTER") }
        // Facts and results reach no syntax, legacy symbol or snapshot; project results are the project's own values.
        val reached = reachable(listOf(first.facts, first.calls))
        assertThat(reached.filter { it is AstNode || it is Token || it is Symbol || it is ProjectSnapshot }).isEmpty()
        assertThat(reached.filterIsInstance<String>()).noneMatch { it.contains("BEGIN") }
    }

    /** Objects reachable through fields, stopping at library internals and at the project's API values. */
    private fun reachable(root: Any): List<Any> {
        val seen = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        val pending = ArrayDeque(listOf(root))
        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            if (!seen.add(current)) continue
            if (current.javaClass.name.startsWith("com.felipebz.zpa.api.project.")) continue
            when (current) {
                is Iterable<*> -> current.filterNotNull().forEach(pending::add)
                is Map<*, *> -> (current.keys + current.values).filterNotNull().forEach(pending::add)
                else -> {
                    var type: Class<*>? = current.javaClass
                    while (type != null && !type.name.startsWith("java.") && !type.name.startsWith("kotlin.")) {
                        for (field in type.declaredFields) {
                            if (field.type.isPrimitive || java.lang.reflect.Modifier.isStatic(field.modifiers)) continue
                            field.isAccessible = true
                            field.get(current)?.let(pending::add)
                        }
                        type = type.superclass
                    }
                }
            }
        }
        return seen.toList()
    }
}
