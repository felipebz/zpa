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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class EnclosingTypeNameTest {
    private val spec = "CREATE TYPE t AS OBJECT (a NUMBER, MEMBER FUNCTION f(SELF IN t) RETURN NUMBER, STATIC FUNCTION s RETURN NUMBER);\n/\n"
    private val body = "CREATE TYPE BODY t AS\n" +
        "  MEMBER FUNCTION f(SELF IN t) RETURN NUMBER IS v t; BEGIN RETURN self.a + v.a; END;\n" +
        "  STATIC FUNCTION s RETURN NUMBER IS w t; BEGIN RETURN w.a; END;\n" +
        "END;\n/\n"

    @Test
    fun theOwnNameResolvesWithTheSpecificationInAnotherFile() {
        assertThat(scan("spec.sql" to spec, "body.sql" to body)).containsExactly(
            "2 t type spec.sql", "2 t type spec.sql", "2 self.a Resolved t.a", "2 v.a Resolved t.a",
            "3 t type spec.sql", "3 w.a Resolved t.a"
        )
    }

    @Test
    fun theOwnNameResolvesWithTheSpecificationInTheSameFile() {
        assertThat(scan("t.sql" to spec + body).filter { !it.startsWith("1 ") }).containsExactly(
            "4 t type t.sql", "4 t type t.sql", "4 self.a Resolved t.a", "4 v.a Resolved t.a",
            "5 t type t.sql", "5 w.a Resolved t.a"
        )
    }

    @Test
    fun aSubtypeResolvesItsOwnNameOnlyWithTheWholeChainInTheProject() {
        val sup = "CREATE TYPE sup AS OBJECT (a NUMBER) NOT FINAL;\n/\n"
        val sub = "CREATE TYPE sub UNDER sup (b NUMBER, MEMBER FUNCTION f RETURN NUMBER);\n/\n"
        val subBody = "CREATE TYPE BODY sub AS\n  MEMBER FUNCTION f RETURN NUMBER IS v sub; BEGIN RETURN v.b; END;\nEND;\n/\n"
        assertThat(scan("sup.sql" to sup, "sub.sql" to sub, "body.sql" to subBody)).containsExactly("2 sub type sub.sql", "2 v.b Resolved sub.b")
        // The supertype outside the project may declare an attribute of that name.
        assertThat(scan("sub.sql" to sub, "body.sql" to subBody)).isEmpty()
    }

    @Test
    fun anAttributeOrMethodOfTheOwnNameKeepsItUndecided() {
        // An attribute or method named like a type captures a type position (PLS-00488).
        val odd = "CREATE TYPE t AS OBJECT (t NUMBER, MEMBER FUNCTION f RETURN NUMBER);\n/\n"
        val oddBody = "CREATE TYPE BODY t AS\n  MEMBER FUNCTION f RETURN NUMBER IS v t; BEGIN RETURN 1; END;\nEND;\n/\n"
        assertThat(scan("spec.sql" to odd, "body.sql" to oddBody)).isEmpty()
    }

    @Test
    fun aLocalTypeShadowsTheSchemaType() {
        val other = "CREATE TYPE t2 AS OBJECT (z NUMBER);\n/\n"
        val shadow = "CREATE TYPE BODY t AS\n  MEMBER FUNCTION f(SELF IN t) RETURN NUMBER IS\n    TYPE t2 IS RECORD (q NUMBER);\n" +
            "    v t2;\n  BEGIN v.q := 7; RETURN v.q; END;\n  STATIC FUNCTION s RETURN NUMBER IS BEGIN RETURN 1; END;\nEND;\n/\n"
        assertThat(scan("t2.sql" to other, "spec.sql" to spec, "body.sql" to shadow)).containsExactly("2 t type spec.sql")
    }

    private fun scan(vararg sources: Pair<String, String>): List<String> {
        val context = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false))
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            init { subscribeTo(PlSqlGrammar.DATATYPE, PlSqlGrammar.MEMBER_EXPRESSION) }
            override fun visitNode(node: AstNode) {
                val semantic = node as SemanticAstNode
                val text = node.tokens.joinToString("") { it.originalValue }
                (semantic.projectTypeResolution as? ProjectTypeResolution.Resolved)?.let { rows += "${node.token.line} $text type ${it.declaration.fileId.value}" }
                semantic.projectRecordMemberResolution?.let {
                    rows += "${node.token.line} $text " + when (it) {
                        is ProjectRecordMemberResolution.Resolved -> "Resolved ${it.declaration.qualifiedName}.${it.field.name.originalSpelling}"
                        else -> it.javaClass.simpleName
                    }
                }
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
