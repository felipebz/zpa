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
package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.symbols.local.LocalSemanticSwitches
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class AttributeNamedAloneBaseTest {
    private var saved = true

    @BeforeEach
    fun save() {
        saved = LocalSemanticSwitches.selfMemberBase
        LocalSemanticSwitches.selfMemberBase = true
    }

    @AfterEach
    fun restore() {
        LocalSemanticSwitches.selfMemberBase = saved
    }

    private val types = "CREATE TYPE val_t AS OBJECT (x NUMBER, MEMBER FUNCTION report RETURN VARCHAR2);\n/\n" +
        "CREATE TYPE names_t AS TABLE OF VARCHAR2(30);\n/\n" +
        "CREATE TYPE holder_t AS OBJECT (inner val_t);\n/\n" +
        "CREATE TYPE sup AS OBJECT (expected val_t) NOT FINAL;\n/\n" +
        "CREATE TYPE t UNDER sup (names names_t, h holder_t, MEMBER PROCEDURE m, STATIC PROCEDURE s);\n/\n"

    private fun body(member: String, static: String = "NULL;") = "CREATE TYPE BODY t AS\n" +
        "  MEMBER PROCEDURE m IS n NUMBER; r VARCHAR2(30); BEGIN $member END;\n" +
        "  STATIC PROCEDURE s IS n NUMBER; BEGIN $static END;\nEND;\n/\n"

    @Test
    fun ownAndInheritedAttributesNamedAloneAreTheBase() {
        val rows = scan("types.sql" to types, "body.sql" to body("n := expected.x; n := names.count; n := h.inner.x; r := expected.report();"))
        assertThat(rows).containsExactly(
            "2 expected.x member Resolved val_t.x",
            "2 names.count member CollectionMethod",
            "2 h.inner.x path Completed",
            "2 expected.report() call VALUE RESOLVED",
            "2 expected.report member Method"
        )
    }

    @Test
    fun aLocalOfTheSameNameShadowsTheAttribute() {
        val shadow = "CREATE TYPE BODY t AS\n  MEMBER PROCEDURE m IS expected NUMBER; n NUMBER; BEGIN n := names.count; END;\n" +
            "  STATIC PROCEDURE s IS names NUMBER; n NUMBER; BEGIN n := names; END;\nEND;\n/\n"
        assertThat(scan("types.sql" to types, "body.sql" to shadow)).containsExactly("2 names.count member CollectionMethod")
        val local = "CREATE TYPE BODY t AS\n  MEMBER PROCEDURE m IS names val_t; n NUMBER; BEGIN n := names.x; END;\n" +
            "  STATIC PROCEDURE s IS BEGIN NULL; END;\nEND;\n/\n"
        // The local variable decides: its legacy symbol is the base, not the attribute.
        assertThat(scan("types.sql" to types, "body.sql" to local)).containsExactly("2 names.x member Resolved val_t.x")
    }

    @Test
    fun aStaticMethodKeepsTheGap() {
        assertThat(scan("types.sql" to types, "body.sql" to body("NULL;", "n := names.count;"))).isEmpty()
    }

    @Test
    fun noBaseWhenTheChainIsNotFullyKnown() {
        val orphan = "CREATE TYPE names_t AS TABLE OF VARCHAR2(30);\n/\nCREATE TYPE t UNDER missing_t (names names_t, MEMBER PROCEDURE m, STATIC PROCEDURE s);\n/\n"
        assertThat(scan("types.sql" to orphan, "body.sql" to body("n := names.count;"))).isEmpty()
    }

    @Test
    fun theSwitchOffRestoresTodaysResult() {
        LocalSemanticSwitches.selfMemberBase = false
        assertThat(scan("types.sql" to types, "body.sql" to body("n := expected.x; n := names.count;"))).isEmpty()
    }

    private fun scan(vararg sources: Pair<String, String>): List<String> {
        val context = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false))
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            init { subscribeTo(PlSqlGrammar.MEMBER_EXPRESSION, PlSqlGrammar.METHOD_CALL) }
            override fun visitNode(node: AstNode) {
                val semantic = node as SemanticAstNode
                val text = node.tokens.joinToString("") { it.originalValue }
                semantic.projectRecordMemberResolution?.let {
                    rows += "${node.token.line} $text member " + when (it) {
                        is ProjectRecordMemberResolution.Resolved -> "Resolved ${it.declaration.qualifiedName}.${it.field.name.originalSpelling}"
                        else -> it.javaClass.simpleName
                    }
                }
                semantic.projectRecordMemberPathResolution?.let { rows += "${node.token.line} $text path ${it.javaClass.simpleName}" }
                semantic.projectMethodCall?.let { rows += "${node.token.line} $text call ${it.receiver} ${it.status}" }
            }
        }
        val scanner = AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)
        val observed = sources.last().first
        for ((id, text) in sources) scanner.scanFile(Source(id, text), if (id == observed) listOf(check) else emptyList(), FileId(id))
        return rows
    }

    private class Source(private val id: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = id
        override fun path(): Path = Path.of(id)
        override fun type() = PlSqlFile.Type.MAIN
    }
}
