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
package com.felipebz.zpa.project

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.squid.PlSqlConfiguration
import com.felipebz.zpa.tooling.ProjectRecordMemberResolutionState
import com.felipebz.zpa.tooling.SemanticInspectionService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class CollectionMethodMemberTest {
    private val methods = listOf("count", "delete", "exists", "extend", "first", "last", "limit", "next", "prior", "trim")

    private fun prepare(vararg sources: Pair<String, String>) =
        ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false))

    private fun types(context: ProjectAnalysisContext) =
        (context.state as ProjectAnalysisContext.State.Prepared).result.index.declarations.filterIsInstance<ProjectTypeDeclaration>()

    private fun member(context: ProjectAnalysisContext, type: ProjectTypeDeclaration, name: String) =
        ProjectRecordMemberResolver(context).resolve(type, OracleIdentifier.fromSource(name))

    @Test
    fun everyCollectionMethodOfASchemaNestedTableAndAPackageAssociativeArray() {
        val context = prepare(
            "nt.sql" to "CREATE TYPE nt AS TABLE OF NUMBER;\n/\n",
            "pk.sql" to "CREATE PACKAGE pk AS TYPE aa IS TABLE OF NUMBER INDEX BY PLS_INTEGER; END pk;\n/\n"
        )
        for (type in types(context)) {
            for (name in methods) {
                // EXTEND and TRIM on an associative array are calls Oracle rejects (PLS-00306), not unknown members.
                assertThat(member(context, type, name)).describedAs("${type.qualifiedName}.$name")
                    .isEqualTo(ProjectRecordMemberResolution.CollectionMethod(type, OracleIdentifier.fromSource(name)))
            }
        }
    }

    @Test
    fun aCollectionHasNoOtherMembersNotEvenTheElementAttributes() {
        val context = prepare(
            "types.sql" to "CREATE TYPE obj AS OBJECT (a NUMBER);\n/\nCREATE TYPE nt AS TABLE OF obj;\n/\n"
        )
        val nt = types(context).single { it.qualifiedName.last.lookupName == "NT" }
        // Oracle: PLS-00302 for v.foo, and for v.a although the element type has an attribute a.
        assertThat(member(context, nt, "foo")).isEqualTo(ProjectRecordMemberResolution.NotFound(nt))
        assertThat(member(context, nt, "a")).isEqualTo(ProjectRecordMemberResolution.NotFound(nt))
    }

    @Test
    fun aPathStopsAtACollectionMethodWhetherItIsLastOrNot() {
        val types = "CREATE TYPE nt AS TABLE OF NUMBER;\n/\nCREATE TYPE holder AS OBJECT (items nt);\n/\n"
        val use = "CREATE PROCEDURE use_it IS\n  h holder;\n  n NUMBER;\nBEGIN\n  n := h.items.count;\n  n := h.items.count.x;\nEND;\n/\n"
        val context = prepare("types.sql" to types, "use.sql" to use)
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            init { subscribeTo(PlSqlGrammar.MEMBER_EXPRESSION) }
            override fun visitNode(node: AstNode) {
                val path = (node as SemanticAstNode).projectRecordMemberPathResolution as? ProjectRecordMemberPathResolution.Stopped ?: return
                rows += "${node.token.line} ${path.segments.size} ${path.nextMember.originalSpelling} ${path.reason}"
            }
        }
        val scanner = AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)
        scanner.scanFile(Source("types.sql", types), emptyList(), FileId("types.sql"))
        scanner.scanFile(Source("use.sql", use), listOf(check), FileId("use.sql"))
        assertThat(rows).containsExactly("5 1 count MEMBER_IS_COLLECTION_METHOD", "6 1 count MEMBER_IS_COLLECTION_METHOD")
    }

    @Test
    fun theToolingReportsTheCollectionMethodState() {
        val pk = "CREATE PACKAGE pk AS TYPE aa IS TABLE OF NUMBER INDEX BY PLS_INTEGER; END pk;\n/\n"
        val use = "CREATE PROCEDURE use_it IS\n  a pk.aa;\n  n NUMBER;\nBEGIN\n  n := a.count;\nEND;\n/\n"
        val context = prepare("pk.sql" to pk, "use.sql" to use)
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(use)
        val result = SemanticInspectionService().analyze(root, FileId("use.sql"), context)
        val states = root.getDescendants(PlSqlGrammar.MEMBER_EXPRESSION).mapNotNull { result.inspect(it).projectRecordMemberResolution?.state }
        assertThat(states).containsExactly(ProjectRecordMemberResolutionState.COLLECTION_METHOD)
    }

    private class Source(private val id: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = id
        override fun path(): Path = Path.of(id)
        override fun type() = PlSqlFile.Type.MAIN
    }
}
