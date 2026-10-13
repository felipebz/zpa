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
package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectBindingCompatibility.Outcome
import com.felipebz.zpa.api.project.ProjectBindingCompatibility.UnknownReason
import com.felipebz.zpa.api.project.ProjectTypedCallResolution.Status
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import com.felipebz.zpa.api.project.ProjectCallResolution.Status as Structural
import com.felipebz.zpa.api.project.ProjectDatatypeFamily as Family

/** Datatype narrowing over structural call resolutions, through public APIs; outcomes follow Oracle 26ai probes. */
@OptIn(ZpaExperimentalApi::class)
class ProjectTypedCallResolutionTest {
    private val hr = ProjectIdentifier.fromSource("HR")
    private val employees = ProjectExternalRelation.of(
        "HR", "EMPLOYEES", true, listOf(
            ProjectExternalColumn.of("SALARY", 1, ProjectExternalDatatype.of("NUMBER", null, null, null, 10, 2)),
            ProjectExternalColumn.of("ODD", 2, ProjectExternalDatatype.of("TIMESTAMP SOMETHING", null, null, null, null, null))
        )
    )
    private val md = metadata(employees)

    private fun metadata(vararg relations: ProjectExternalRelation, complete: Boolean = true): ProjectExternalMetadata {
        val builder = ProjectExternalMetadata.builder("fixture")
        if (complete) builder.completeSchema("HR")
        relations.forEach { builder.relation(it) }
        return builder.build()
    }

    private fun prepare(vararg files: Pair<String, String>, failing: Boolean = false) = ProjectSnapshots.prepare(
        files.map { (id, text) -> ProjectSourceInput.ofText(id, text) } +
            (if (failing) listOf(ProjectSourceInput.ofReader("bad.sql", ProjectSourceReader { throw IllegalStateException("x") })) else emptyList()),
        false
    )

    private fun spec(vararg declarations: String) = "CREATE PACKAGE p AS\n" + declarations.joinToString("\n") { "  $it" } + "\nEND p;"

    private fun positional() = ProjectCallArgument.positional()
    private fun named(label: String) = ProjectCallArgument.named(ProjectIdentifier.fromSource(label))

    private fun resolve(s: ProjectSnapshot, name: String, vararg arguments: ProjectCallArgument) = s.resolveCall(
        ProjectCallTarget.packageMember(ProjectQualifiedName.of("p"), ProjectIdentifier.fromSource(name)),
        ProjectCallKind.PROCEDURE, ProjectCallArguments.of(arguments.toList())
    )

    private class Call(val structural: ProjectCallResolution, val typed: ProjectTypedCallResolution)

    private fun call(
        source: String, name: String, arguments: List<ProjectCallArgument>, types: List<ProjectArgumentType>,
        m: ProjectExternalMetadata = md, failing: Boolean = false
    ): Call {
        val s = prepare("p.sql" to source, failing = failing)
        val structural = resolve(s, name, *arguments.toTypedArray())
        return Call(structural, s.narrowCall(structural, types, m, hr))
    }

    private fun signature(c: ProjectCallCandidate) = c.declaration.parameters.joinToString(",", "(", ")") { "${it.name} ${it.typeRef.name}" }
    private fun selected(t: ProjectTypedCallResolution) = signature(t.selected.get())
    private fun outcomes(t: ProjectTypedCallResolution) = t.evaluations.associate { signature(it.candidate) to it.outcome.name }

    private val ts08 = spec("PROCEDURE pick(n NUMBER);", "PROCEDURE pick(s VARCHAR2);")

    @Test
    fun characterActualAgainstNumberAndVarchar2StaysAmbiguous() {
        val c = call(ts08, "pick", listOf(positional()), listOf(ProjectArgumentType.builtIn("CHAR")))
        assertThat(c.structural.status).isEqualTo(Structural.AMBIGUOUS)
        assertThat(c.typed.status).isEqualTo(Status.AMBIGUOUS)
        assertThat(c.typed.selected).isEmpty
        assertThat(c.typed.evaluations.flatMap { it.bindings }.map { it.outcome }).containsExactly(Outcome.COMPATIBLE, Outcome.COMPATIBLE)
        assertThat(c.typed.evaluations.map { it.bindings.single().formalFamily.get() }).containsExactly(Family.NUMERIC, Family.CHARACTER)
        val unknown = call(ts08, "pick", listOf(positional()), listOf(ProjectArgumentType.unknown()))
        assertThat(unknown.typed.status).isEqualTo(Status.AMBIGUOUS)
        assertThat(unknown.typed.evaluations.map { it.bindings.single().unknownReason.get() }).containsOnly(UnknownReason.ACTUAL_UNKNOWN)
        // The named call is resolved structurally and stays so.
        val namedCall = call(ts08, "pick", listOf(named("s")), listOf(ProjectArgumentType.builtIn("CHAR")))
        assertThat(namedCall.structural.status).isEqualTo(Structural.RESOLVED)
        assertThat(namedCall.typed.status).isEqualTo(Status.RESOLVED)
        assertThat(namedCall.typed.selected.get()).isSameAs(namedCall.structural.selected.get())
    }

    @Test
    fun aDateActualRemovesTheNumberOverload() {
        val c = call(spec("PROCEDURE pick(x NUMBER);", "PROCEDURE pick(x DATE);"), "pick", listOf(positional()), listOf(ProjectArgumentType.builtIn("DATE")))
        assertThat(c.typed.status).isEqualTo(Status.RESOLVED)
        assertThat(selected(c.typed)).isEqualTo("(x DATE)")
        val rejected = c.typed.evaluations.first().bindings.single()
        assertThat(rejected.outcome).isEqualTo(Outcome.INCOMPATIBLE)
        assertThat(rejected.actualFamily).contains(Family.DATETIME)
        assertThat(rejected.formalFamily).contains(Family.NUMERIC)
        assertThat(rejected.unknownReason).isEmpty
    }

    @Test
    fun oneIncompatibleBindingRejectsDespiteAnUnknownOne() {
        val c = call(
            spec("PROCEDURE q(a NUMBER, b VARCHAR2);", "PROCEDURE q(a VARCHAR2, b NUMBER);"), "q", listOf(positional(), positional()),
            listOf(ProjectArgumentType.builtIn("DATE"), ProjectArgumentType.unknown())
        )
        assertThat(c.typed.status).isEqualTo(Status.RESOLVED)
        assertThat(selected(c.typed)).isEqualTo("(a VARCHAR2,b NUMBER)")
        // The survivor still has an unknown binding: RESOLVED only says no other known candidate can receive the call.
        val survivor = c.typed.evaluations.single { it.outcome == ProjectCandidateCompatibility.Outcome.RETAINED }
        assertThat(survivor.bindings.map { it.outcome }).containsExactly(Outcome.COMPATIBLE, Outcome.UNKNOWN)
        assertThat(c.typed.evaluations.first().bindings.map { it.outcome }).containsExactly(Outcome.INCOMPATIBLE, Outcome.UNKNOWN)
    }

    @Test
    fun compositeAndScalarActualsSeparateRecordAndScalarFormals() {
        val source = spec("TYPE rec_t IS RECORD (a NUMBER);", "TYPE tab_t IS TABLE OF NUMBER;", "PROCEDURE put(r rec_t);", "PROCEDURE put(v VARCHAR2);", "PROCEDURE put2(t tab_t);", "PROCEDURE put2(n NUMBER);")
        assertThat(selected(call(source, "put", listOf(positional()), listOf(ProjectArgumentType.composite())).typed)).isEqualTo("(r rec_t)")
        assertThat(selected(call(source, "put", listOf(positional()), listOf(ProjectArgumentType.builtIn("NUMBER"))).typed)).isEqualTo("(v VARCHAR2)")
        assertThat(selected(call(source, "put2", listOf(positional()), listOf(ProjectArgumentType.composite())).typed)).isEqualTo("(t tab_t)")
        assertThat(selected(call(source, "put2", listOf(positional()), listOf(ProjectArgumentType.builtIn("NUMBER"))).typed)).isEqualTo("(n NUMBER)")
        assertThat(call(source, "put2", listOf(positional()), listOf(ProjectArgumentType.builtIn("DATE"))).typed.status).isEqualTo(Status.NO_COMPATIBLE_CANDIDATE)
        // Two composites are never told apart.
        val both = call(spec("TYPE r1 IS RECORD (a NUMBER);", "TYPE r2 IS RECORD (a NUMBER);", "PROCEDURE put(x r1);", "PROCEDURE put(x r2);"), "put", listOf(positional()), listOf(ProjectArgumentType.composite()))
        assertThat(both.typed.status).isEqualTo(Status.AMBIGUOUS)
        assertThat(both.typed.evaluations.map { it.bindings.single().unknownReason.get() }).containsOnly(UnknownReason.UNSUPPORTED_PAIR)
    }

    @Test
    fun nullNeverEliminatesAndAllRejectedIsReported() {
        val source = spec("PROCEDURE pick(x NUMBER);", "PROCEDURE pick(x DATE);")
        val nul = call(source, "pick", listOf(positional()), listOf(ProjectArgumentType.nullLiteral()))
        assertThat(nul.typed.status).isEqualTo(Status.AMBIGUOUS)
        assertThat(nul.typed.evaluations.map { it.bindings.single().unknownReason.get() }).containsOnly(UnknownReason.NULL_LITERAL)
        val clob = call(source, "pick", listOf(positional()), listOf(ProjectArgumentType.builtIn("CLOB")))
        assertThat(clob.typed.status).isEqualTo(Status.NO_COMPATIBLE_CANDIDATE)
        assertThat(clob.typed.selected).isEmpty
        assertThat(outcomes(clob.typed).values).containsOnly("REJECTED")
        // A single structural candidate proven incompatible is not kept as RESOLVED either.
        val single = call(spec("PROCEDURE one(x DATE);"), "one", listOf(positional()), listOf(ProjectArgumentType.builtIn("NUMBER")))
        assertThat(single.structural.status).isEqualTo(Structural.RESOLVED)
        assertThat(single.typed.status).isEqualTo(Status.NO_COMPATIBLE_CANDIDATE)
    }

    @Test
    fun anchoredFormalsFollowTheSuppliedMetadata() {
        val source = spec("PROCEDURE pick(x employees.salary%TYPE);", "PROCEDURE pick(x DATE);", "PROCEDURE odd(x employees.odd%TYPE);", "PROCEDURE odd(x DATE);")
        val date = listOf(ProjectArgumentType.builtIn("DATE"))
        assertThat(selected(call(source, "pick", listOf(positional()), date).typed)).isEqualTo("(x DATE)")
        val none = call(source, "pick", listOf(positional()), date, ProjectExternalMetadata.none())
        assertThat(none.typed.status).isEqualTo(Status.AMBIGUOUS)
        assertThat(none.typed.evaluations.first().bindings.single().unknownReason).contains(UnknownReason.FORMAL_INCONCLUSIVE)
        val partial = metadata(ProjectExternalRelation.of("HR", "EMPLOYEES", false, emptyList()), complete = false)
        assertThat(call(source, "pick", listOf(positional()), date, partial).typed.status).isEqualTo(Status.AMBIGUOUS)
        // A dictionary spelling outside the known forms stays unclassified.
        val odd = call(source, "odd", listOf(positional()), date)
        assertThat(odd.typed.status).isEqualTo(Status.AMBIGUOUS)
        assertThat(odd.typed.evaluations.first().bindings.single().unknownReason).contains(UnknownReason.UNCLASSIFIED_TYPE)
        // A %ROWTYPE formal is a row whatever the metadata.
        val rows = call(spec("PROCEDURE r(x employees%ROWTYPE);", "PROCEDURE r(x NUMBER);"), "r", listOf(positional()), listOf(ProjectArgumentType.builtIn("NUMBER")), ProjectExternalMetadata.none())
        assertThat(selected(rows.typed)).isEqualTo("(x NUMBER)")
    }

    @Test
    fun conflictingSourceAndExternalReadingsAreInconclusive() {
        val files = arrayOf("q.sql" to "CREATE PACKAGE q AS g NUMBER; END q;", "p.sql" to spec("PROCEDURE pick(x q.g%TYPE);", "PROCEDURE pick(x DATE);"))
        val s = prepare(*files)
        val structural = resolve(s, "pick", positional())
        val date = listOf(ProjectArgumentType.builtIn("DATE"))
        assertThat(selected(s.narrowCall(structural, date, md, hr))).isEqualTo("(x DATE)")
        val conflicting = metadata(employees, ProjectExternalRelation.of("HR", "Q", true, listOf(ProjectExternalColumn.of("G", 1, null))))
        val c = s.narrowCall(structural, date, conflicting, hr)
        assertThat(c.status).isEqualTo(Status.AMBIGUOUS)
        assertThat(c.evaluations.first().bindings.single().unknownReason).contains(UnknownReason.FORMAL_INCONCLUSIVE)
    }

    @Test
    fun booleanPairsDependOnTheEnvironment() {
        val source = spec("PROCEDURE pick(x BOOLEAN);", "PROCEDURE pick(x VARCHAR2);")
        val b = call(source, "pick", listOf(positional()), listOf(ProjectArgumentType.builtIn("BOOLEAN")))
        assertThat(b.typed.status).isEqualTo(Status.AMBIGUOUS)
        assertThat(b.typed.evaluations[1].bindings.single().unknownReason).contains(UnknownReason.ENVIRONMENT_DEPENDENT)
        assertThat(selected(call(source, "pick", listOf(positional()), listOf(ProjectArgumentType.builtIn("DATE"))).typed)).isEqualTo("(x VARCHAR2)")
    }

    @Test
    fun defaultsAreNotEvaluatedAndMixedNotationUsesTheStructuralOrdinals() {
        val defaults = call(
            spec("PROCEDURE q(a NUMBER, b DATE DEFAULT SYSDATE);", "PROCEDURE q(a DATE);"), "q", listOf(positional()),
            listOf(ProjectArgumentType.builtIn("NUMBER"))
        )
        assertThat(selected(defaults.typed)).isEqualTo("(a NUMBER,b DATE)")
        assertThat(defaults.typed.evaluations.first().bindings).hasSize(1)
        val mixed = call(
            spec("PROCEDURE q(a NUMBER, b VARCHAR2 DEFAULT NULL, c DATE);", "PROCEDURE q(a NUMBER, b DATE DEFAULT NULL, c VARCHAR2);"), "q",
            listOf(positional(), named("c")), listOf(ProjectArgumentType.builtIn("NUMBER"), ProjectArgumentType.builtIn("NUMBER"))
        )
        assertThat(selected(mixed.typed)).isEqualTo("(a NUMBER,b DATE,c VARCHAR2)")
        val rejected = mixed.typed.evaluations.first().bindings
        assertThat(rejected.map { it.binding.argumentOrdinal to it.binding.parameter.name.originalSpelling }).containsExactly(1 to "a", 2 to "c")
        assertThat(rejected.map { it.outcome }).containsExactly(Outcome.COMPATIBLE, Outcome.INCOMPATIBLE)
    }

    @Test
    fun structurallyRejectedCandidatesAreLeftAlone() {
        val c = call(
            spec("PROCEDURE pick(x NUMBER);", "PROCEDURE pick(x DATE);", "PROCEDURE pick(x NUMBER, y NUMBER);", "FUNCTION pick(x VARCHAR2) RETURN NUMBER;"),
            "pick", listOf(positional()), listOf(ProjectArgumentType.builtIn("DATE"))
        )
        assertThat(c.structural.candidates.map { it.applicability.name }).containsExactly("APPLICABLE", "APPLICABLE", "MISSING_REQUIRED", "WRONG_KIND")
        assertThat(c.typed.evaluations.map { it.candidate }).containsExactly(c.structural.candidates[0], c.structural.candidates[1])
        assertThat(selected(c.typed)).isEqualTo("(x DATE)")
    }

    @Test
    fun uncertainDiscoveryIsNeverNarrowed() {
        val source = spec("PROCEDURE pick(x NUMBER);", "PROCEDURE pick(x DATE);")
        val incomplete = call(source, "pick", listOf(positional()), listOf(ProjectArgumentType.builtIn("DATE")), failing = true)
        assertThat(incomplete.structural.status).isEqualTo(Structural.INCOMPLETE_INDEX)
        assertThat(incomplete.typed.status).isEqualTo(Status.NOT_NARROWED)
        assertThat(incomplete.typed.selected).isEmpty
        assertThat(incomplete.typed.evaluations).hasSize(2)
        val partialSource = spec("PROCEDURE pick(x NUMBER);", "\$IF \$\$f \$THEN PROCEDURE pick(x VARCHAR2); \$END", "PROCEDURE pick(x DATE);")
        val partial = call(partialSource, "pick", listOf(positional()), listOf(ProjectArgumentType.builtIn("NUMBER")))
        assertThat(partial.structural.status).isEqualTo(Structural.PARTIAL_DECLARATIONS)
        assertThat(partial.typed.status).isEqualTo(Status.NOT_NARROWED)
        assertThat(partial.typed.selected).isEmpty
        val missing = call(source, "nothing", emptyList(), emptyList())
        assertThat(missing.typed.status).isEqualTo(Status.NOT_NARROWED)
        assertThat(missing.typed.evaluations).isEmpty()
    }

    @Test
    fun withoutEvidenceTheStructuralOutcomeIsKept() {
        val ambiguous = call(spec("PROCEDURE pick(x NUMBER);", "PROCEDURE pick(x DATE);"), "pick", listOf(positional()), listOf(ProjectArgumentType.unknown()))
        assertThat(ambiguous.typed.status).isEqualTo(Status.AMBIGUOUS)
        val resolved = call(spec("PROCEDURE one(x NUMBER);"), "one", listOf(positional()), listOf(ProjectArgumentType.unknown()))
        assertThat(resolved.typed.status).isEqualTo(Status.RESOLVED)
        assertThat(resolved.typed.selected.get()).isSameAs(resolved.structural.selected.get())
        val zero = call(spec("PROCEDURE boot;", "PROCEDURE boot(x NUMBER DEFAULT 1);"), "boot", emptyList(), emptyList())
        assertThat(zero.typed.status).isEqualTo(Status.AMBIGUOUS)
    }

    @Test
    fun predefinedNamesShadowedByPackageMembersAreNotClassified() {
        // Oracle 26ai: inside the package, `number` is the subtype (a DATE) and receives a DATE actual.
        val shadowed = call(spec("SUBTYPE number IS DATE;", "PROCEDURE pick(x number);", "PROCEDURE pick(x VARCHAR2);"), "pick", listOf(positional()), listOf(ProjectArgumentType.builtIn("DATE")))
        assertThat(shadowed.typed.status).isEqualTo(Status.AMBIGUOUS)
        assertThat(shadowed.typed.evaluations.first().bindings.single().unknownReason).contains(UnknownReason.FORMAL_INCONCLUSIVE)
        val chained = call(spec("SUBTYPE date IS NUMBER;", "SUBTYPE d2 IS date;", "PROCEDURE pick(x d2);", "PROCEDURE pick(x VARCHAR2);"), "pick", listOf(positional()), listOf(ProjectArgumentType.builtIn("TIMESTAMP")))
        assertThat(chained.typed.status).isEqualTo(Status.AMBIGUOUS)
        // An unrelated member name keeps the predefined meaning.
        val plain = call(spec("SUBTYPE money_t IS NUMBER;", "PROCEDURE pick(x money_t);", "PROCEDURE pick(x VARCHAR2);"), "pick", listOf(positional()), listOf(ProjectArgumentType.builtIn("DATE")))
        assertThat(selected(plain.typed)).isEqualTo("(x VARCHAR2)")
    }

    @Test
    fun describedPackageValuesHonourShadowingInTheirPackage() {
        // Oracle 26ai: inside p, NUMBER is the DATE subtype; q.pick(p.g) and q.pick(p.h) both select pick(x DATE).
        val s = prepare(
            "p.sql" to "CREATE PACKAGE p AS SUBTYPE number IS DATE; g number; h NUMBER; END p;",
            "u.sql" to "CREATE PACKAGE u AS SUBTYPE money_t IS NUMBER; n NUMBER; m money_t; k n%TYPE; END u;",
            "d1.sql" to "CREATE PACKAGE dup AS n NUMBER; END dup;",
            "d2.sql" to "CREATE PACKAGE dup AS n NUMBER; END dup;",
            "q.sql" to "CREATE PACKAGE q AS PROCEDURE pick(x NUMBER); PROCEDURE pick(x DATE); END q;",
            "sp.sql" to "CREATE PROCEDURE sp(x NUMBER) IS BEGIN NULL; END;"
        )
        val structural = s.resolveCall(
            ProjectCallTarget.packageMember(ProjectQualifiedName.of("q"), ProjectIdentifier.fromSource("pick")),
            ProjectCallKind.PROCEDURE, ProjectCallArguments.of(listOf(positional()))
        )
        fun value(vararg n: String) = s.findDeclarations(ProjectQualifiedName.of(*n)).declarations.first() as ProjectPackageValueView
        fun described(owner: String, name: String, withOwner: Boolean = true): ProjectArgumentType {
            val d = s.describeExternalType(value(owner, name).typeRef, ProjectQualifiedName.of(owner), md, hr)
            return if (withOwner) ProjectArgumentType.described(d, ProjectQualifiedName.of(owner)) else ProjectArgumentType.described(d)
        }
        fun narrow(type: ProjectArgumentType) = s.narrowCall(structural, listOf(type), md, hr)

        // Direct references with no source steps: the package scope decides.
        for (name in listOf("g", "h")) {
            val t = narrow(described("p", name))
            assertThat(t.status).describedAs(name).isEqualTo(Status.AMBIGUOUS)
            assertThat(t.evaluations.map { it.bindings.single().outcome }).containsOnly(Outcome.UNKNOWN)
            assertThat(t.evaluations.first().bindings.single().unknownReason).contains(UnknownReason.ACTUAL_UNKNOWN)
            // Without the owner the scope cannot be checked: never a definitive classification.
            assertThat(described("p", name, withOwner = false).family).isEmpty
            assertThat(narrow(described("p", name, withOwner = false)).status).isEqualTo(Status.AMBIGUOUS)
        }
        // An unshadowed package keeps the precise classification.
        assertThat(selected(narrow(described("u", "n")))).isEqualTo("(x NUMBER)")
        assertThat(described("u", "n", withOwner = false).family).isEmpty
        // Chains name their package through the retained steps, with or without the owner.
        assertThat(selected(narrow(described("u", "m", withOwner = false)))).isEqualTo("(x NUMBER)")
        assertThat(selected(narrow(described("u", "k", withOwner = false)))).isEqualTo("(x NUMBER)")
        // Duplicate or unknown package specifications cannot be checked.
        assertThat(narrow(described("dup", "n")).status).isEqualTo(Status.AMBIGUOUS)
        val standalone = s.findDeclarations(ProjectQualifiedName.of("sp")).declarations.single() as ProjectSubprogramView
        val anonymous = s.describeExternalType(standalone.parameters.single().typeRef, null, md, hr)
        assertThat(narrow(ProjectArgumentType.described(anonymous, ProjectQualifiedName.of("missing_pkg"))).status).isEqualTo(Status.AMBIGUOUS)
        // No package scope at all (a standalone subprogram): the predefined meaning holds.
        assertThat(selected(narrow(ProjectArgumentType.described(anonymous, null)))).isEqualTo("(x NUMBER)")
        assertThat(ProjectArgumentType.described(anonymous).family).isEmpty
        // External column datatypes do not inherit the package's shadowing.
        val column = s.describeExternalType(
            ProjectWrittenAnchor.of(ProjectQualifiedName.of("employees", "salary"), ProjectTypeReference.Anchor.TYPE), ProjectQualifiedName.of("p"), md, hr
        )
        assertThat(column.status).isEqualTo(ProjectExternalTypeDescription.Status.RESOLVED)
        assertThat(selected(narrow(ProjectArgumentType.described(column, ProjectQualifiedName.of("p"))))).isEqualTo("(x NUMBER)")
        assertThat(selected(narrow(ProjectArgumentType.described(column)))).isEqualTo("(x NUMBER)")
        // Caller-asserted built-ins stay the caller's lexical responsibility.
        assertThat(selected(narrow(ProjectArgumentType.builtIn("NUMBER")))).isEqualTo("(x NUMBER)")
    }

    @Test
    fun describedActualsUseConclusiveDescriptionsOnly() {
        val s = prepare("p.sql" to spec("PROCEDURE pick(x NUMBER);", "PROCEDURE pick(x DATE);"))
        val structural = resolve(s, "pick", positional())
        val anchor = ProjectWrittenAnchor.of(ProjectQualifiedName.of("employees", "salary"), ProjectTypeReference.Anchor.TYPE)
        val salary = ProjectArgumentType.described(s.describeExternalType(anchor, null, md, hr))
        assertThat(salary.family).contains(Family.NUMERIC)
        assertThat(selected(s.narrowCall(structural, listOf(salary), md, hr))).isEqualTo("(x NUMBER)")
        val unavailable = ProjectArgumentType.described(s.describeExternalType(anchor, null, ProjectExternalMetadata.none(), hr))
        assertThat(unavailable.family).isEmpty
        val c = s.narrowCall(structural, listOf(unavailable), md, hr)
        assertThat(c.status).isEqualTo(Status.AMBIGUOUS)
        assertThat(c.evaluations.first().bindings.single().unknownReason).contains(UnknownReason.ACTUAL_UNKNOWN)
        val row = ProjectArgumentType.described(s.describeExternalType(ProjectWrittenAnchor.of(ProjectQualifiedName.of("employees"), ProjectTypeReference.Anchor.ROWTYPE), null, ProjectExternalMetadata.none(), hr))
        assertThat(row.family).contains(Family.COMPOSITE)
        assertThat(ProjectArgumentType.builtIn("LONG").family).isEmpty
        assertThat(s.narrowCall(structural, listOf(ProjectArgumentType.builtIn("LONG")), md, hr).evaluations.first().bindings.single().unknownReason)
            .contains(UnknownReason.UNCLASSIFIED_TYPE)
    }

    @Test
    fun inputsAreValidatedAndResultsKeepStructuralIdentity() {
        val source = spec("PROCEDURE pick(x NUMBER);", "PROCEDURE pick(x DATE);")
        val s = prepare("p.sql" to source)
        val other = prepare("p.sql" to source)
        val structural = resolve(s, "pick", positional())
        val types = mutableListOf(ProjectArgumentType.builtIn("DATE"))
        val typed = s.narrowCall(structural, types, md, hr)
        types[0] = ProjectArgumentType.builtIn("NUMBER")
        assertThat(typed.structural).isSameAs(structural)
        assertThat(typed.evaluations.map { it.candidate }).containsExactlyElementsOf(structural.candidates)
        typed.evaluations.forEach { e -> e.bindings.forEachIndexed { i, b -> assertThat(b.binding).isSameAs(e.candidate.bindings[i]) } }
        assertThat(typed.selected.get()).isSameAs(structural.candidates[1])
        assertThatThrownBy { (typed.evaluations as MutableList<ProjectCandidateCompatibility>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { (typed.evaluations[0].bindings as MutableList<ProjectBindingCompatibility>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { other.narrowCall(structural, listOf(ProjectArgumentType.unknown()), md, hr) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { s.narrowCall(structural, emptyList(), md, hr) }.isInstanceOf(IllegalArgumentException::class.java)
        @Suppress("UNCHECKED_CAST")
        val withNull = listOf<ProjectArgumentType?>(null) as List<ProjectArgumentType>
        assertThatThrownBy { s.narrowCall(structural, withNull, md, hr) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { ProjectArgumentType.builtIn(" ") }.isInstanceOf(IllegalArgumentException::class.java)
        assertThat(ProjectTypedCallResolution.Status.entries.map { it.name }).containsExactly("RESOLVED", "AMBIGUOUS", "NO_COMPATIBLE_CANDIDATE", "NOT_NARROWED")
        assertThat(UnknownReason.entries.map { it.name }).containsExactly(
            "ACTUAL_UNKNOWN", "NULL_LITERAL", "FORMAL_INCONCLUSIVE", "ENVIRONMENT_DEPENDENT", "UNSUPPORTED_PAIR", "UNCLASSIFIED_TYPE"
        )
    }

    @Test
    fun concurrentQueriesAreDeterministic() {
        val s = prepare("p.sql" to spec("PROCEDURE q(a NUMBER, b VARCHAR2);", "PROCEDURE q(a VARCHAR2, b NUMBER);"))
        val structural = resolve(s, "q", positional(), positional())
        val types = listOf(ProjectArgumentType.builtIn("DATE"), ProjectArgumentType.nullLiteral())
        val expected = s.narrowCall(structural, types, md, hr).evaluations.flatMap { e -> e.bindings.map { it.toString() } }
        val pool = Executors.newFixedThreadPool(8)
        try {
            val results = (1..64).map { pool.submit<List<String>> { s.narrowCall(structural, types, md, hr).evaluations.flatMap { e -> e.bindings.map { it.toString() } } } }
            assertThat(results.map { it.get() }.toSet()).containsExactly(expected)
        } finally {
            pool.shutdown()
        }
    }
}
