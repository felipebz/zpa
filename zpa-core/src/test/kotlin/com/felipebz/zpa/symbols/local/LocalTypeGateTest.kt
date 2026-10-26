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
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
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

/** The project type resolution's lexical gate, decided from the local outcome of each type name. */
@OptIn(ZpaExperimentalApi::class)
class LocalTypeGateTest {

    private class Source(private val name: String, private val text: String, private val failing: Boolean) : PlSqlFile {
        override fun contents(): String = if (failing) error("injected failure") else text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    /** Each type name's project resolution after the walk, as `line: name -> kind` (`-` when the gate suppressed it). */
    private class Capture : PlSqlCheck() {
        val rows = ArrayList<String>()
        var facts: LocalSemanticFacts? = null
        override fun leaveFile(node: AstNode) {
            facts = context.localSemanticFacts
            for (datatype in node.getDescendants(PlSqlGrammar.DATATYPE)) {
                val custom = datatype.getFirstChildOrNull(PlSqlGrammar.CUSTOM_DATATYPE) ?: continue
                val resolution = (datatype as SemanticAstNode).projectTypeResolution
                rows += "${datatype.token.line}: ${custom.tokenOriginalValue} -> ${resolution?.javaClass?.simpleName ?: "-"}"
            }
        }
    }

    private var lastFacts: () -> LocalSemanticFacts? = { null }

    private fun gate(text: String, vararg others: Pair<String, String>, failing: Boolean = false): List<String> {
        val project = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation().prepare((others.toList() + ("main.sql" to text)).map { (id, t) -> ProjectSource(FileId(id)) { t } }, concurrent = false)
        )
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).parse(text)
        val capture = Capture()
        lastFacts = { capture.facts }
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("main.sql")).all + capture)
            .walk(PlSqlVisitorContext(root, Source("main.sql", text, failing), null), SemanticScanCapabilities(createProjectAnalysis(project)))
        return capture.rows
    }

    private val sameFileType = "create type t as object (a number);\n/\ncreate procedure p is\n  v t;\nbegin\n  null;\nend;\n/\n"
    private val refCursor = "create procedure p is\n  type c_t is ref cursor;\n  v c_t;\nbegin\n  null;\nend;\n/\n"
    private val specification = "spec.sql" to "create package cp as\n  type t_rec is record (a number);\nend;\n/\n"
    private val body = "create package body cp as\n  procedure run is\n    v t_rec;\n  begin\n    null;\n  end;\nend;\n/\n"

    @Test
    fun aSameFileSchemaTypeIsProjectResolved() {
        // A standalone type is a schema object, proven absent from the procedure's regions: the project describes it.
        assertThat(gate(sameFileType)).containsExactly("4: t -> Resolved")
    }

    @Test
    fun aLocalRefCursorTypeIsNotProjectResolved() {
        assertThat(gate(refCursor)).containsExactly("3: c_t -> -")
    }

    @Test
    fun aSpecificationMemberInAnotherFileIsProjectResolved() {
        assertThat(gate(body, specification)).containsExactly("3: t_rec -> Resolved")
    }

    @Test
    fun anUncertainNameWithOneCandidateIsDecidedAsThatCandidate() {
        // The conditional declaration makes the procedure's region incomplete: r's outcome is provisional, with the local
        // record type as its only candidate. It decides as that binding would, even with a schema type r in the project.
        val source = "create procedure p is\n  type r is record (a number);\n  \$if \$\$x \$then n number; \$end\n  v r;\nbegin\n  null;\nend;\n/\n"
        assertThat(gate(source, "r.sql" to "create type r as object (a number);\n/\n")).containsExactly("4: r -> -")
    }

    @Test
    fun anUncertainNameWithoutACandidateAndAnUnpairedTypeNameAreProjectResolved() {
        // A body whose specification the project does not hold: the name is uncertain and nothing in the file declares it.
        val lone = "create package body nospec as\n  procedure run is\n    v some_t;\n  begin\n    null;\n  end;\nend;\n/\n"
        assertThat(gate(lone)).containsExactly("3: some_t -> NotFoundInProject")
        // A type name the local collector does not gather.
        val treat = "create type u as object (a number) not final;\n/\ncreate procedure q(o anydata) is v number; begin v := treat(o as u).a; end;\n/\n"
        assertThat(gate(treat)).contains("3: u -> Resolved")
    }

    @Test
    fun aLocalCollectionTypeInCastIsNotProjectResolved() {
        val source = "create package pb as\n  function g return number;\nend;\n/\n" +
            "create package body pb as\n  type t is table of number;\n" +
            "  function f(a t default cast(null as t)) return number is begin return 1; end;\n" +
            "  function g return number is begin return f; end;\nend;\n/\n"
        assertThat(gate(source, "t.sql" to "create type t as table of number;\n/\n")).containsExactly("7: t -> -", "7: t -> -")
    }

    @Test
    fun aSubtypeDeclaredUnderTwoUndecidedConditionsIsLocal() {
        // Each undecided condition keeps its declaration: the provisional answer is a duplicate of two local subtypes.
        // (One `$IF … $ELSE` keeps a single branch, which the single-candidate rule already decides.)
        val source = "create procedure p is\n  \$if \$\$x \$then subtype s is number; \$end\n" +
            "  \$if \$\$y \$then subtype s is varchar2(10); \$end\n  v s;\nbegin\n  null;\nend;\n/\n"
        assertThat(gate(source, "s.sql" to "create type s as object (a number);\n/\n")).contains("4: s -> -")
        val facts = lastFacts()!!
        val reference = facts.references.indexOfFirst { isTypeName(it) && it.range.startLine == 4 }
        assertThat((facts.resolutions[reference].last() as LocalResolution.Incomplete).provisional).isInstanceOf(LocalResolution.Duplicate::class.java)
    }

    @Test
    fun treatAndIsOfNameSchemaObjectTypes() {
        val source = "create type u as object (a number) not final;\n/\n" +
            "create procedure q(p u) is v number; begin v := treat(p as u).a; if p is of (u) then null; end if; end;\n/\n"
        assertThat(gate(source)).containsExactly("3: u -> Resolved", "3: u -> Resolved", "3: u -> Resolved")
    }

    @Test
    fun aFailedPrepassLetsTheProjectResolveEveryTypeName() {
        assertThat(gate(sameFileType, failing = true)).containsExactly("4: t -> Resolved")
        assertThat(gate(refCursor, failing = true)).containsExactly("3: c_t -> NotFoundInProject")
    }

    @Test
    fun aSpecificationRefCursorTypeIsTheProjectsDeclaration() {
        // Declared in the specification: local and held by the project, so the project resolves it to that declaration.
        val source = "create package rc as\n  type c_t is ref cursor;\n  procedure p(x c_t);\nend;\n/\n" +
            "create package body rc as\n  procedure p(x c_t) is\n    v c_t;\n  begin\n    null;\n  end;\nend;\n/\n"
        assertThat(gate(source)).containsExactly("3: c_t -> Resolved", "7: c_t -> Resolved", "8: c_t -> Resolved")
    }

    @Test
    fun aSubprogramRefCursorTypeShadowingASpecificationTypeIsSuppressed() {
        val source = "create package rs as\n  type c_t is record (a number);\n  procedure p;\nend;\n/\n" +
            "create package body rs as\n  procedure p is\n    type c_t is ref cursor;\n    v c_t;\n  begin\n    null;\n  end;\nend;\n/\n"
        // The inner declaration decides: no project resolution.
        assertThat(gate(source)).containsExactly("9: c_t -> -")
    }
}
