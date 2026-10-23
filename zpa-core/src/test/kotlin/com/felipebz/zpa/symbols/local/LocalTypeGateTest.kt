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
import com.felipebz.zpa.squid.PlSqlAstWalker
import com.felipebz.zpa.squid.PlSqlConfiguration
import com.felipebz.zpa.squid.SemanticAnalysisPipeline
import com.felipebz.zpa.squid.SemanticScanCapabilities
import com.felipebz.zpa.squid.createProjectAnalysis
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** The project type resolution's lexical gate, taken from the local outcome when the switch is on. */
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
        override fun leaveFile(node: AstNode) {
            for (datatype in node.getDescendants(PlSqlGrammar.DATATYPE)) {
                val custom = datatype.getFirstChildOrNull(PlSqlGrammar.CUSTOM_DATATYPE) ?: continue
                val resolution = (datatype as SemanticAstNode).projectTypeResolution
                rows += "${datatype.token.line}: ${custom.tokenOriginalValue} -> ${resolution?.javaClass?.simpleName ?: "-"}"
            }
        }
    }

    private var found = LocalSemanticSwitches.typeNameGate

    @BeforeEach
    fun save() {
        found = LocalSemanticSwitches.typeNameGate
    }

    @AfterEach
    fun restore() {
        LocalSemanticSwitches.typeNameGate = found
    }

    private fun gate(text: String, vararg others: Pair<String, String>, on: Boolean, failing: Boolean = false): List<String> {
        LocalSemanticSwitches.typeNameGate = on
        val project = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation().prepare((others.toList() + ("main.sql" to text)).map { (id, t) -> ProjectSource(FileId(id)) { t } }, concurrent = false)
        )
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).parse(text)
        val capture = Capture()
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
        // Legacy finds the type lexically and suppresses the project; the local side hands the schema type to it.
        assertThat(gate(sameFileType, on = false)).containsExactly("4: t -> -")
        assertThat(gate(sameFileType, on = true)).containsExactly("4: t -> Resolved")
    }

    @Test
    fun aLocalRefCursorTypeIsNotProjectResolved() {
        // Legacy records no ref cursor type and asks the project; the local declaration decides.
        assertThat(gate(refCursor, on = false)).containsExactly("3: c_t -> NotFoundInProject")
        assertThat(gate(refCursor, on = true)).containsExactly("3: c_t -> -")
    }

    @Test
    fun aSpecificationMemberInAnotherFileIsStillProjectResolved() {
        assertThat(gate(body, specification, on = true)).containsExactly("3: t_rec -> Resolved")
        assertThat(gate(body, specification, on = false)).containsExactly("3: t_rec -> Resolved")
    }

    @Test
    fun uncertainAndUnpairedTypeNamesKeepTheLegacyDecision() {
        // A body whose specification the project does not hold: uncertain locally.
        val lone = "create package body nospec as\n  procedure run is\n    v some_t;\n  begin\n    null;\n  end;\nend;\n/\n"
        assertThat(gate(lone, on = true)).isEqualTo(gate(lone, on = false)).containsExactly("3: some_t -> NotFoundInProject")
        // A type name the local collector does not gather.
        val treat = "create type u as object (a number) not final;\n/\ncreate procedure q(o anydata) is v number; begin v := treat(o as u).a; end;\n/\n"
        assertThat(gate(treat, on = true)).isEqualTo(gate(treat, on = false))
    }

    @Test
    fun theSwitchOffOrAFailedPrepassGivesTheLegacyDecision() {
        for (source in listOf(sameFileType, refCursor)) {
            val legacy = gate(source, on = false)
            assertThat(gate(source, on = true, failing = true)).isEqualTo(legacy)
        }
    }

    @Test
    fun aSpecificationRefCursorTypeIsTheProjectsDeclaration() {
        // Declared in the specification: local and held by the project, so the project resolves it to that declaration.
        val source = "create package rc as\n  type c_t is ref cursor;\n  procedure p(x c_t);\nend;\n/\n" +
            "create package body rc as\n  procedure p(x c_t) is\n    v c_t;\n  begin\n    null;\n  end;\nend;\n/\n"
        val expected = listOf("3: c_t -> Resolved", "7: c_t -> Resolved", "8: c_t -> Resolved")
        assertThat(gate(source, on = false)).isEqualTo(expected)
        assertThat(gate(source, on = true)).isEqualTo(expected)
    }

    @Test
    fun aSubprogramRefCursorTypeShadowingASpecificationTypeIsSuppressed() {
        val source = "create package rs as\n  type c_t is record (a number);\n  procedure p;\nend;\n/\n" +
            "create package body rs as\n  procedure p is\n    type c_t is ref cursor;\n    v c_t;\n  begin\n    null;\n  end;\nend;\n/\n"
        // The inner declaration decides: no project resolution (legacy suppresses it too, finding the specification's type).
        assertThat(gate(source, on = true)).containsExactly("9: c_t -> -")
        assertThat(gate(source, on = false)).containsExactly("9: c_t -> -")
    }
}
