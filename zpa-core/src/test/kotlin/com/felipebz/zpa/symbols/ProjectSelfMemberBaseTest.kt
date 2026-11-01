//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class ProjectSelfMemberBaseTest {
    private val spec = "CREATE TYPE t AS OBJECT (\n  a NUMBER,\n  MEMBER FUNCTION f RETURN NUMBER,\n  MEMBER PROCEDURE p,\n" +
        "  CONSTRUCTOR FUNCTION t RETURN SELF AS RESULT,\n  STATIC FUNCTION s RETURN NUMBER\n);\n/\n"
    private val body = "CREATE TYPE BODY t AS\n" +
        "  MEMBER FUNCTION f RETURN NUMBER IS BEGIN RETURN self.a; END;\n" +
        "  MEMBER PROCEDURE p IS\n    PROCEDURE nested IS BEGIN dbms_output.put_line(self.a); END;\n  BEGIN self.a := 1; nested; END;\n" +
        "  CONSTRUCTOR FUNCTION t RETURN SELF AS RESULT IS BEGIN self.a := 0; RETURN; END;\n" +
        "  STATIC FUNCTION s RETURN NUMBER IS BEGIN RETURN 1; END;\n" +
        "END;\n/\n"

    @Test
    fun selfAttributesResolveInMethodsWithTheSpecificationHereOrElsewhere() {
        val expected = listOf("2 self.a Resolved t.a", "4 self.a Resolved t.a", "5 self.a Resolved t.a", "6 self.a Resolved t.a")
        assertThat(scan("t.sql" to spec + body)).containsExactlyElementsOf(expected.map { it.replace(Regex("^(\\d+)")) { m -> (m.value.toInt() + 8).toString() } })
        assertThat(scan("spec.sql" to spec, "body.sql" to body)).containsExactlyElementsOf(expected)
    }

    @Test
    fun inheritedAttributesGoThroughTheChain() {
        val subBody = "CREATE TYPE BODY sub AS\n  OVERRIDING MEMBER FUNCTION f RETURN NUMBER IS BEGIN RETURN self.a + self.b; END;\nEND;\n/\n"
        val subSpec = "CREATE TYPE sub UNDER sup (b NUMBER, OVERRIDING MEMBER FUNCTION f RETURN NUMBER);\n/\n"
        val sup = "CREATE TYPE sup AS OBJECT (a NUMBER, MEMBER FUNCTION f RETURN NUMBER) NOT FINAL;\n/\n"
        assertThat(scan("sup.sql" to sup, "sub.sql" to subSpec, "body.sql" to subBody))
            .containsExactly("2 self.a Resolved sup.a", "2 self.b Resolved sub.b")
        assertThat(scan("sub.sql" to subSpec, "body.sql" to subBody))
            .containsExactly("2 self.a Incomplete", "2 self.b Resolved sub.b")
    }

    @Test
    fun pathsThroughSelf() {
        val types = "CREATE PACKAGE pk AS TYPE r IS RECORD (x NUMBER); END pk;\n/\n" +
            "CREATE TYPE addr AS OBJECT (city VARCHAR2(30));\n/\n" +
            "CREATE TYPE person AS OBJECT (home addr, pos pk.r, MEMBER PROCEDURE m);\n/\n"
        val body = "CREATE TYPE BODY person AS\n  MEMBER PROCEDURE m IS BEGIN\n    dbms_output.put_line(self.home.city);\n" +
            "    dbms_output.put_line(self.pos.x);\n  END;\nEND;\n/\n"
        assertThat(scan("types.sql" to types, "body.sql" to body))
            .containsExactly("3 self.home.city Completed", "4 self.pos.x Completed")
    }

    @Test
    fun noBaseInAStaticMethodOrForAnAmbiguousType() {
        val staticBody = "CREATE TYPE BODY t AS\n  STATIC FUNCTION s RETURN NUMBER IS BEGIN RETURN self.a; END;\nEND;\n/\n"
        assertThat(scan("spec.sql" to spec, "body.sql" to staticBody)).isEmpty()
        assertThat(scan("spec.sql" to spec, "again.sql" to spec, "body.sql" to body)).isEmpty()
    }

    @Test
    fun anExplicitSelfParameterIsAValueBase() {
        val explicit = "CREATE TYPE BODY t AS\n  CONSTRUCTOR FUNCTION t(SELF IN OUT t) RETURN SELF AS RESULT IS BEGIN self.a := 0; RETURN; END;\nEND;\n/\n"
        assertThat(scan("t.sql" to spec + explicit)).containsExactly("10 self.a Resolved t.a")
        assertThat(scan("spec.sql" to spec, "body.sql" to explicit)).containsExactly("2 self.a Resolved t.a")
    }

    private fun scan(vararg sources: Pair<String, String>): List<String> {
        val context = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false))
        val observed = sources.last().first
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            init { subscribeTo(PlSqlGrammar.MEMBER_EXPRESSION) }
            override fun visitNode(node: AstNode) {
                val semantic = node as SemanticAstNode
                val text = node.tokens.joinToString("") { it.originalValue }
                semantic.projectRecordMemberResolution?.let {
                    rows += "${node.token.line} $text " + when (it) {
                        is ProjectRecordMemberResolution.Resolved -> "Resolved ${it.declaration.qualifiedName}.${it.field.name.originalSpelling}"
                        else -> it.javaClass.simpleName
                    }
                }
                semantic.projectRecordMemberPathResolution?.let { rows += "${node.token.line} $text ${it.javaClass.simpleName}" }
            }
        }
        val scanner = AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)
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
