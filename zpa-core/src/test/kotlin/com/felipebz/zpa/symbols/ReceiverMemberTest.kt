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
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** Members written after a collection element or a call result, resolved on the receiver's type. */
class ReceiverMemberTest {
    private val types = "CREATE TYPE t AS OBJECT (a NUMBER, MEMBER FUNCTION m RETURN NUMBER, MEMBER FUNCTION self_ RETURN t);\n/\n" +
        "CREATE TYPE tt AS TABLE OF t;\n/\n" +
        "CREATE FUNCTION f(x NUMBER) RETURN t IS BEGIN RETURN NULL; END;\n/\n" +
        "CREATE PACKAGE pk AS\n  TYPE r IS RECORD (fld NUMBER);\n  TYPE rt IS TABLE OF r INDEX BY PLS_INTEGER;\n  FUNCTION g RETURN r;\nEND;\n/\n"

    private fun body(declarations: String, vararg statements: String) =
        "CREATE PROCEDURE use_it IS\n  $declarations\nBEGIN\n" + statements.joinToString("") { "  $it\n" } + "END;\n/\n"

    @Test
    fun eachReceiverFormOfTheIssue() {
        val rows = scan("s.sql" to types, "u.sql" to body(
            "v tt; o t; n NUMBER; w pk.rt;",
            "n := v(1).a;", "n := v(1).m();", "n := w(1).fld;", "n := f(1).a;", "n := f(1).m();", "n := pk.g().fld;",
            "n := o.self_().a;", "n := o.self_.a;", "n := v(1).zz;"
        ))
        assertThat(rows).containsExactly(
            "4:12 a member Resolved(t.a)",
            "5:12 m() call RESOLVED t.m",
            "5:12 m member Method(t.m)",
            "6:12 fld member Resolved(pk.r.fld)",
            "7:12 a member Resolved(t.a)",
            "8:12 m() call RESOLVED t.m",
            "8:12 m member Method(t.m)",
            "9:14 fld member Resolved(pk.r.fld)",
            "10:7 o.self_() call RESOLVED t.self_",
            "10:7 o.self_ member Method(t.self_)",
            "10:17 a member Resolved(t.a)",
            // Oracle rejects a method named without parentheses before a member (PLS-00225): today's result stays.
            "11:7 o.self_.a path Stopped(MEMBER_IS_METHOD)",
            // t's lists are complete.
            "12:12 zz member NotFound(t)"
        )
    }

    @Test
    fun aChainTakesEachBaseFromTheStepBefore() {
        val spec = "CREATE TYPE c AS OBJECT (k NUMBER);\n/\nCREATE TYPE ct AS TABLE OF c;\n/\n" +
            "CREATE TYPE b AS OBJECT (items ct, MEMBER FUNCTION get(i NUMBER) RETURN c);\n/\nCREATE TYPE bt AS TABLE OF b;\n/\n"
        val rows = scan("s.sql" to spec, "u.sql" to body("v bt; n NUMBER;", "n := v(1).get(2).k;", "n := v(1).items(3).k;"))
        assertThat(rows).containsExactly(
            "4:12 get(2) call RESOLVED b.get",
            "4:12 get member Method(b.get)",
            "4:19 k member Resolved(c.k)",
            "5:12 items member Resolved(b.items)",
            "5:21 k member Resolved(c.k)"
        )
    }

    @Test
    fun receiversWithoutABase() {
        val spec = types + "CREATE PACKAGE amb AS\n  FUNCTION h(x NUMBER) RETURN t;\n  FUNCTION h(x VARCHAR2) RETURN t;\nEND;\n/\n" +
            "CREATE TYPE ut AS TABLE OF missing_type;\n/\n"
        val rows = scan("s.sql" to spec, "u.sql" to body(
            "u ut; n NUMBER;",
            // A name the local side does not prove a value: a project function nothing declares.
            "n := nothing(1).a;",
            // Both overloads return t, but the call is ambiguous (PLS-00307 on Oracle).
            "n := amb.h(NULL).a;",
            // The element type does not resolve.
            "n := u(1).a;"
        ))
        assertThat(rows).isEmpty()
    }

    @Test
    fun furtherArgumentListsIndexTheValueBefore() {
        val spec = types + "CREATE TYPE ttt AS TABLE OF tt;\n/\nCREATE FUNCTION fc(x NUMBER) RETURN tt IS BEGIN RETURN NULL; END;\n/\n"
        val rows = scan("s.sql" to spec, "u.sql" to body("vv ttt; n NUMBER;", "n := vv(1)(2).a;", "n := fc(4)(1).a;"))
        // A call followed by another argument list is not a modeled call form: its result has no type.
        assertThat(rows).containsExactly("4:16 a member Resolved(t.a)")
    }

    @Test
    fun aLocalFunctionResultAndAPackageVariableElement() {
        val spec = types + "CREATE PACKAGE vars AS\n  g_items pk.rt;\nEND;\n/\n"
        val use = "CREATE PROCEDURE use_it IS\n  n NUMBER;\n  FUNCTION lf RETURN t IS BEGIN RETURN NULL; END;\nBEGIN\n" +
            "  n := lf().a;\n  n := vars.g_items(1).fld;\nEND;\n/\n"
        assertThat(scan("s.sql" to spec, "u.sql" to use)).containsExactly(
            "5:12 a member Resolved(t.a)",
            "6:23 fld member Resolved(pk.r.fld)"
        )
    }

    @Test
    fun anAttributeNamedAloneInATypeBody() {
        val spec = "CREATE TYPE c AS OBJECT (k NUMBER);\n/\nCREATE TYPE ct AS TABLE OF c;\n/\n" +
            "CREATE TYPE holder AS OBJECT (items ct, MEMBER FUNCTION first_k RETURN NUMBER);\n/\n"
        val typeBody = "CREATE TYPE BODY holder AS\n  MEMBER FUNCTION first_k RETURN NUMBER IS\n  BEGIN\n    RETURN items(1).k;\n  END;\nEND;\n/\n"
        assertThat(scan("s.sql" to spec, "b.sql" to typeBody).filter { it.contains(" k ") }).containsExactly("4:20 k member Resolved(c.k)")
    }

    /** Every member decoration of the last file: `line:column text kind result`. */
    private fun scan(vararg sources: Pair<String, String>): List<String> {
        val context = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false))
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                fun visit(n: AstNode) {
                    val semantic = n as SemanticAstNode
                    val text = n.tokens.joinToString("") { it.originalValue }
                    val at = "${n.token.line}:${n.token.column} $text"
                    semantic.projectRecordMemberResolution?.let { rows += "$at member ${member(it)}" }
                    semantic.projectRecordMemberPathResolution?.let { rows += "$at path ${path(it)}" }
                    semantic.projectMethodCall?.let { call ->
                        rows += "$at call ${call.status}" + (call.selected?.let { s -> " ${s.type.name}.${s.method?.name?.originalSpelling}" } ?: "")
                    }
                    n.children.forEach(::visit)
                }
                visit(node)
            }
        }
        val scanner = AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)
        val observed = sources.last().first
        for ((id, text) in sources) scanner.scanFile(Source(id, text), if (id == observed) listOf(check) else emptyList(), FileId(id))
        return rows
    }

    private fun typeName(type: ProjectTypeDeclaration) = when (type) {
        is PackageTypeDeclaration -> "${type.owner}.${type.name.originalSpelling}"
        is StandaloneTypeDeclaration -> type.name.toString()
        else -> type.toString()
    }

    private fun member(resolution: ProjectRecordMemberResolution) = when (resolution) {
        is ProjectRecordMemberResolution.Resolved -> "Resolved(${typeName(resolution.declaration)}.${resolution.field.name.originalSpelling})"
        is ProjectRecordMemberResolution.Method -> "Method(${typeName(resolution.declaration)}.${resolution.overloads.first().method.name.originalSpelling})"
        else -> "${resolution.javaClass.simpleName}(${typeName(resolution.declaration)})"
    }

    private fun path(resolution: ProjectRecordMemberPathResolution) = when (resolution) {
        is ProjectRecordMemberPathResolution.Completed -> "Completed(${resolution.segments.joinToString(".") { it.field.name.originalSpelling }})"
        is ProjectRecordMemberPathResolution.Stopped -> "Stopped(${resolution.reason})"
    }

    private class Source(private val id: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = id
        override fun path(): Path = Path.of(id)
        override fun type() = PlSqlFile.Type.MAIN
    }
}
