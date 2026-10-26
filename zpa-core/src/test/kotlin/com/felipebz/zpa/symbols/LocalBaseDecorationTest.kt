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

class LocalBaseDecorationTest {

    private val types = "CREATE PACKAGE pk AS TYPE r IS RECORD (fld NUMBER); END pk;\n/\n" +
        "CREATE TYPE t AS OBJECT (a NUMBER, MEMBER FUNCTION m RETURN NUMBER, STATIC FUNCTION s RETURN NUMBER);\n/\n" +
        "CREATE TYPE tt AS TABLE OF t;\n/\n"

    @Test
    fun aLocalVariableTypedByAPackageRecord() {
        assertThat(scan("CREATE PROCEDURE p IS\n  v pk.r;\n  n NUMBER;\nBEGIN\n  n := v.fld;\nEND;\n/\n"))
            .containsExactly("5:7 v.fld member Resolved(pk.r.fld)")
    }

    @Test
    fun aParameterTypedByASchemaObjectTypeIsAValueReceiver() {
        assertThat(scan("CREATE PROCEDURE p(o t) IS\n  n NUMBER;\nBEGIN\n  n := o.m();\nEND;\n/\n"))
            .contains("4:7 o.m() call VALUE RESOLVED t.m")
    }

    @Test
    fun aLocalCollectionVariableIsAnElementReceiver() {
        assertThat(scan("CREATE PROCEDURE p IS\n  v tt;\n  n NUMBER;\nBEGIN\n  n := v(1).a;\nEND;\n/\n"))
            .containsExactly("5:12 a member Resolved(t.a)")
    }

    @Test
    fun aQualifiedLocalNameIsNotAMemberOfTheQualifier() {
        // f returns a record with a field fld, but f.fld names f's own variable.
        val source = "CREATE FUNCTION f RETURN pk.r IS\n  fld NUMBER;\nBEGIN\n  f.fld := 1;\n  RETURN NULL;\nEND;\n/\n"
        assertThat(scan(source)).isEmpty()
    }

    @Test
    fun aQualifiedLocalNameThroughItsSubprogram() {
        val source = "CREATE PROCEDURE outer_proc IS\n  v pk.r;\n  n NUMBER;\nBEGIN\n  n := outer_proc.v.fld;\nEND;\n/\n"
        assertThat(scan(source)).isEmpty()
    }

    @Test
    fun aLabelQualifiedVariable() {
        val source = "BEGIN\n  <<blk>>\n  DECLARE\n    v pk.r;\n    n NUMBER;\n  BEGIN\n    n := blk.v.fld;\n    n := v.fld;\n  END;\nEND;\n/\n"
        assertThat(scan(source)).containsExactly("8:9 v.fld member Resolved(pk.r.fld)")
    }

    @Test
    fun anOverloadedBaseWithNoSingleCandidateGetsNoAnswer() {
        // The name resolves to an overload set of two functions: no single declaration, so no declared type.
        val source = "DECLARE\n  FUNCTION f RETURN pk.r IS BEGIN RETURN NULL; END;\n  FUNCTION f(x NUMBER) RETURN pk.r IS BEGIN RETURN NULL; END;\n" +
            "  n NUMBER;\nBEGIN\n  n := f.fld;\nEND;\n/\n"
        assertThat(scan(source)).isEmpty()
    }

    @Test
    fun aTypeNameReceiverStaysATypeNameReceiver() {
        assertThat(scan("CREATE PROCEDURE p IS\n  n NUMBER;\nBEGIN\n  n := t.s();\nEND;\n/\n"))
            .contains("4:7 t.s() call TYPE_NAME RESOLVED t.s")
    }

    private fun scan(source: String): List<String> {
        val sources = listOf("types.sql" to types, "use.sql" to source)
        val context = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false))
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                fun visit(n: AstNode) {
                    val semantic = n as SemanticAstNode
                    val at = "${n.token.line}:${n.token.column} ${n.tokens.joinToString("") { it.originalValue }}"
                    semantic.projectRecordMemberResolution?.let { rows += "$at member ${member(it)}" }
                    semantic.projectRecordMemberPathResolution?.let { rows += "$at path ${it.javaClass.simpleName}" }
                    semantic.projectMethodCall?.let { call ->
                        rows += "$at call ${call.receiver} ${call.status}" + (call.selected?.let { s -> " ${s.type.name}.${s.method?.name?.originalSpelling}" } ?: "")
                    }
                    n.children.forEach(::visit)
                }
                visit(node)
            }
        }
        val scanner = AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)
        for ((id, text) in sources) scanner.scanFile(Source(id, text), if (id == "use.sql") listOf(check) else emptyList(), FileId(id))
        return rows
    }

    private fun member(resolution: ProjectRecordMemberResolution) = when (resolution) {
        is ProjectRecordMemberResolution.Resolved -> "Resolved(${resolution.declaration.qualifiedName}.${resolution.field.name.originalSpelling})"
        is ProjectRecordMemberResolution.Method -> "Method"
        else -> resolution.javaClass.simpleName
    }

    private class Source(private val id: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = id
        override fun path(): Path = Path.of(id)
        override fun type() = PlSqlFile.Type.MAIN
    }
}
