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
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.project.ProjectSnapshot
import com.felipebz.zpa.api.project.ProjectTypedCallResolution
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.*

/**
 * A package body whose specification is another project file: the lookups that stop only at the missing specification
 * are decided with the project's member-name coverage of it, and only then.
 */
@OptIn(ZpaExperimentalApi::class)
class LocalSpecificationCoverageTest {

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

    private val specification = "create package cp as\n  g number;\n  type t_rec is record (a number);\n  cursor c is select 1 a from dual;\n" +
        "  procedure p(x number);\nend;\n/\n"
    private val other = "create package other_pkg as\n  procedure run(x number);\nend;\n/\n"
    private val body = "create package body cp as\n  b_only number;\n  procedure p(x number) is\n    v t_rec;\n  begin\n" +
        "    g := 1;\n    open c;\n    other_pkg.run(1);\n    b_only := 2;\n    p(1);\n  end;\nend;\n/\n"

    /** The body file analyzed in a project of [files] (the body included), or with no project when [prepared] is false. */
    private fun facts(bodyText: String, vararg files: Pair<String, String>, prepared: Boolean = true): LocalSemanticFacts {
        val project = if (!prepared) ProjectAnalysisContext.NOT_PREPARED else ProjectAnalysisContext.prepared(
            ProjectIndexPreparation().prepare((files.toList() + ("body.sql" to bodyText)).map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
        )
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).parse(bodyText)
        val capture = Capture()
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("body.sql")).all + capture)
            .walk(PlSqlVisitorContext(root, Source("body.sql", bodyText), null), SemanticScanCapabilities(createProjectAnalysis(project)))
        return capture.facts!!
    }

    /** The first outcome of the first reference named [name] on [line]. */
    private fun outcome(facts: LocalSemanticFacts, name: String, line: Int): String {
        val index = facts.references.indices.first { facts.references[it].name.originalSpelling == name && facts.references[it].range.startLine == line }
        return LocalOracleFixtureTest.render(facts.model, facts.resolutions[index].first())
    }

    @Test
    fun specificationMembersAbsentNamesAndBodyDeclarations() {
        val facts = facts(body, "spec.sql" to specification, "other.sql" to other)
        // A variable, a type and a cursor of the specification: members of the package in another file.
        assertThat(outcome(facts, "t_rec", 4)).isEqualTo("SpecificationCovered(SpecificationMember)")
        assertThat(outcome(facts, "g", 6)).isEqualTo("SpecificationCovered(SpecificationMember)")
        assertThat(outcome(facts, "c", 7)).isEqualTo("SpecificationCovered(SpecificationMember)")
        // In neither: absent, decided with the project's coverage, and forwarded like any absence.
        assertThat(outcome(facts, "other_pkg", 8)).isEqualTo("SpecificationCovered(ProvenAbsent)")
        val call = facts.calls.single { it.target.toString() == "other_pkg.run" }
        assertThat(call.outcome).isEqualTo(LocalCallOutcome.PROJECT)
        assertThat(call.typed!!.status).isEqualTo(ProjectTypedCallResolution.Status.RESOLVED)
        // Declared by the body only: the body's decision.
        assertThat(outcome(facts, "b_only", 9)).isEqualTo("SpecificationCovered(Bound(VARIABLE b_only@2:2))")
        // Declared by both: the specification's heading and its body definition are one subprogram.
        assertThat(outcome(facts, "p", 10)).isEqualTo("SpecificationCovered(OverloadSet(PROCEDURE p@3:12))")
    }

    @Test
    fun theTypeWalkReadsTheSameDecisions() {
        val spec = "create package cq as\n  g date;\n  subtype s_t is date;\n  procedure run;\nend;\n/\n"
        val processar = "create package pkg as\n  procedure processar(p number);\n  procedure processar(p date);\nend;\n/\n"
        val cqBody = "create package body cq as\n  procedure run is\n    d date;\n    n number;\n    g number;\n    sd s_t;\n  begin\n" +
            "    pkg.processar(d);\n    pkg.processar(n);\n    pkg.processar(g);\n    pkg.processar(sd);\n    pkg.processar(cq.g);\n  end;\nend;\n/\n"
        fun rows(facts: LocalSemanticFacts) = facts.calls.filter { it.target.toString() == "pkg.processar" }.map { call ->
            val typed = call.typed
            val selected = if (typed?.status == ProjectTypedCallResolution.Status.RESOLVED) " " + typed.selected.get().declaration.parameters.single().typeRef.name else ""
            "${call.reference.range.startLine}: ${call.actuals.single().evidence.argumentType.family.map { it.name }.orElse("UNKNOWN")} ${typed?.status ?: call.outcome}$selected"
        }
        assertThat(rows(facts(cqBody, "spec.sql" to spec, "pkg.sql" to processar))).containsExactly(
            "8: DATETIME RESOLVED date",
            "9: NUMERIC RESOLVED number",
            // A local that shadows the specification's g.
            "10: NUMERIC RESOLVED number",
            // A specification subtype and variable, described through the package.
            "11: DATETIME RESOLVED date",
            "12: DATETIME RESOLVED date"
        )
        // Without a project the body's lookups keep the gap: no evidence, and the calls are not forwarded.
        assertThat(rows(facts(cqBody, prepared = false))).containsExactly(
            "8: UNKNOWN INCOMPLETE", "9: UNKNOWN INCOMPLETE", "10: UNKNOWN INCOMPLETE", "11: UNKNOWN INCOMPLETE", "12: UNKNOWN INCOMPLETE"
        )
    }

    @Test
    fun theGapStaysWithoutOneCompleteSpecification() {
        val absent = { facts: LocalSemanticFacts -> outcome(facts, "other_pkg", 8) }
        val stays = "Incomplete[SPECIFICATION_NOT_IN_FILE](ProvenAbsent)"
        // Two specification occurrences of the package.
        assertThat(absent(facts(body, "spec.sql" to specification, "spec2.sql" to specification, "other.sql" to other))).isEqualTo(stays)
        // Conditional compilation in the specification: a member inside it is unknown, and so is every absence.
        val conditional = "create package cp as\n  \$IF \$\$x \$THEN g number; \$END\n  type t_rec is record (a number);\n" +
            "  cursor c is select 1 a from dual;\n  procedure p(x number);\nend;\n/\n"
        val withIf = facts(body, "spec.sql" to conditional, "other.sql" to other)
        assertThat(outcome(withIf, "g", 6)).startsWith("Incomplete[SPECIFICATION_NOT_IN_FILE]")
        assertThat(absent(withIf)).isEqualTo(stays)
        // A specification whose name coverage has a gap (a substitution).
        val substituted = "create package cp as\n  g number;\n  &extra_members\n  type t_rec is record (a number);\n" +
            "  cursor c is select 1 a from dual;\n  procedure p(x number);\nend;\n/\n"
        assertThat(absent(facts(body, "spec.sql" to substituted, "other.sql" to other))).isEqualTo(stays)
        // No project.
        assertThat(absent(facts(body, prepared = false))).isEqualTo(stays)
        // No specification in the project at all.
        assertThat(absent(facts(body, "other.sql" to other))).isEqualTo(stays)
    }

    @Test
    fun aTypeBodyNameIsDecidedFromTheProjectSpecification() {
        val typeBody = "create type body tt as\n  member procedure m is\n  begin\n    other_pkg.run(1);\n  end;\nend;\n/\n"
        val facts = facts(typeBody, "type.sql" to "create type tt as object (a number, member procedure m);\n/\n", "other.sql" to other)
        assertThat(outcome(facts, "other_pkg", 4)).isEqualTo("SpecificationCovered(ProvenAbsent)")
        // Without the specification in the project the gap stays.
        assertThat(outcome(facts(typeBody, "other.sql" to other), "other_pkg", 4)).startsWith("Incomplete[SPECIFICATION_NOT_IN_FILE]")
    }

    private val sup = "create type sup as object (inherited number, member function f return number) not final;\n/\n"
    private val sub = "create type sub under sup (own number, member procedure m, static function s return number,\n" +
        "  constructor function sub(p number) return self as result);\n/\n"
    private val subBody = "create type body sub as\n" +
        "  member procedure m is\n" +
        "    v number;\n" +
        "    w other_pkg.t;\n" +
        "  begin\n" +
        "    v := own + inherited + f;\n" +
        "    other_pkg.run(1);\n" +
        "  end;\n" +
        "  static function s return number is begin return own; end;\n" +
        "  constructor function sub(p number) return self as result is x own; begin return; end;\n" +
        "end;\n/\n"

    @Test
    fun namesInATypeBodyAreDecidedFromTheProjectMemberLists() {
        val facts = facts(subBody, "sup.sql" to sup, "sub.sql" to sub, "other.sql" to other)
        assertThat(outcome(facts, "own", 6)).isEqualTo("SpecificationCovered(SpecificationMember)")
        assertThat(outcome(facts, "inherited", 6)).isEqualTo("SpecificationCovered(SpecificationMember)")
        assertThat(outcome(facts, "f", 6)).isEqualTo("SpecificationCovered(SpecificationMember)")
        assertThat(outcome(facts, "other_pkg", 7)).isEqualTo("SpecificationCovered(ProvenAbsent)")
        assertThat(outcome(facts, "other_pkg", 4)).isEqualTo("SpecificationCovered(ProvenAbsent)")
        // An attribute named alone in a static method is rejected by the local rules: the gap stays.
        assertThat(outcome(facts, "own", 9)).startsWith("Incomplete")
        // An attribute name in a type position is captured by the attribute (PLS-00488).
        assertThat(outcome(facts, "own", 10)).isEqualTo("SpecificationCovered(SpecificationMember)")
        assertThat(facts.localTypeGate(facts.references.first { it.name.originalSpelling == "own" && it.range.startLine == 10 }.range))
            .isEqualTo(LocalTypeGate.Local)
        // A local declaration shadows the members.
        assertThat(outcome(facts, "v", 6)).startsWith("Bound")
    }

    @Test
    fun typeBodyNamesStayUncertainWhenTheChainIsNotFullyKnown() {
        // The supertype outside the project.
        assertThat(outcome(facts(subBody, "sub.sql" to sub, "other.sql" to other), "other_pkg", 7)).startsWith("Incomplete")
        // An ALTER TYPE of the type.
        val altered = facts(subBody, "sup.sql" to sup, "sub.sql" to sub, "other.sql" to other, "alter.sql" to "alter type sub add member function g return number cascade;\n")
        assertThat(outcome(altered, "other_pkg", 7)).startsWith("Incomplete")
        // Two specifications of the type.
        val twice = facts(subBody, "sup.sql" to sup, "sub.sql" to sub, "sub2.sql" to sub, "other.sql" to other)
        assertThat(outcome(twice, "other_pkg", 7)).startsWith("Incomplete")
    }

    @Test
    fun factsAreDeterministicAndDetached() {
        val first = facts(body, "spec.sql" to specification, "other.sql" to other)
        val second = facts(body, "spec.sql" to specification, "other.sql" to other)
        fun projection(facts: LocalSemanticFacts) = facts.resolutions.map { steps -> steps.joinToString(" / ") { LocalOracleFixtureTest.render(facts.model, it) } } +
            facts.calls.map { it.toString() }
        assertThat(projection(second)).isEqualTo(projection(first))
        val reached = reachable(first)
        assertThat(reached.filter { it is AstNode || it is Token || it is Symbol || it is ProjectSnapshot || it is ProjectSymbolIndex || it is IndexedDeclaration })
            .isEmpty()
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
