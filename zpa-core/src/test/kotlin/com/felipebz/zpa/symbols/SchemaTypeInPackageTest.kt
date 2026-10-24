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

class SchemaTypeInPackageTest {
    private val types = "CREATE TYPE str_list AS TABLE OF VARCHAR2(4000);\n/\nCREATE TYPE point AS OBJECT (x NUMBER, y NUMBER);\n/\n"
    private val pk = "CREATE PACKAGE pk AS\n  FUNCTION f(p str_list) RETURN point;\nEND pk;\n/\n" +
        "CREATE PACKAGE BODY pk AS\n  FUNCTION f(p str_list) RETURN point IS v point; BEGIN v := point(1, 2); RETURN v; END;\n" +
        "  FUNCTION g RETURN NUMBER IS v point; BEGIN RETURN v.x; END;\nEND pk;\n/\n"

    @Test
    fun schemaTypesResolveInsideASpecificationAndABody() {
        assertThat(scan("types.sql" to types, "pk.sql" to pk)).containsExactly(
            "2 str_list Resolved types.sql", "2 point Resolved types.sql",
            "6 str_list Resolved types.sql", "6 point Resolved types.sql", "6 point Resolved types.sql",
            "7 point Resolved types.sql", "7 v.x member Resolved point.x"
        )
    }

    @Test
    fun aPackageTypeOfTheSameNameWins() {
        val own = "CREATE PACKAGE pk AS\n  TYPE point IS RECORD (z NUMBER);\n  FUNCTION f RETURN point;\nEND pk;\n/\n"
        val body = "CREATE PACKAGE BODY pk AS\n  FUNCTION f RETURN point IS v point; BEGIN RETURN v; END;\nEND pk;\n/\n"
        assertThat(scan("types.sql" to types, "spec.sql" to own, "body.sql" to body))
            .containsExactly("2 point Resolved spec.sql", "2 point Resolved spec.sql")
    }

    @Test
    fun noSchemaTypeWhenThePackageDeclarationsAreNotFullyKnown() {
        // Conditional compilation leaves the specification's member names incomplete.
        val conditional = "CREATE PACKAGE pk AS\n  \$IF \$\$flag \$THEN TYPE other IS RECORD (z NUMBER); \$END\n  FUNCTION f RETURN NUMBER;\nEND pk;\n/\n"
        val body = "CREATE PACKAGE BODY pk AS\n  FUNCTION f RETURN NUMBER IS v point; BEGIN RETURN 1; END;\nEND pk;\n/\n"
        assertThat(scan("types.sql" to types, "spec.sql" to conditional, "body.sql" to body)).containsExactly("2 point NotFoundInProject")
        // No specification in the project.
        assertThat(scan("types.sql" to types, "body.sql" to body)).containsExactly("2 point NotFoundInProject")
    }

    @Test
    fun twoSchemaTypesOfTheNameAreAmbiguous() {
        val again = "CREATE TYPE point AS OBJECT (x NUMBER);\n/\n"
        val body = "CREATE PACKAGE pk AS\n  FUNCTION f RETURN NUMBER;\nEND pk;\n/\nCREATE PACKAGE BODY pk AS\n  FUNCTION f RETURN NUMBER IS v point; BEGIN RETURN 1; END;\nEND pk;\n/\n"
        assertThat(scan("types.sql" to types, "again.sql" to again, "pk.sql" to body)).containsExactly("6 point Ambiguous")
    }

    private fun scan(vararg sources: Pair<String, String>): List<String> {
        val context = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false))
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            init { subscribeTo(PlSqlGrammar.DATATYPE, PlSqlGrammar.MEMBER_EXPRESSION) }
            override fun visitNode(node: AstNode) {
                val semantic = node as SemanticAstNode
                val text = node.tokens.joinToString("") { it.originalValue }
                when (val r = semantic.projectTypeResolution) {
                    null -> Unit
                    is ProjectTypeResolution.Resolved -> rows += "${node.token.line} $text Resolved ${r.declaration.fileId.value}"
                    else -> rows += "${node.token.line} $text ${r.javaClass.simpleName}"
                }
                (semantic.projectRecordMemberResolution as? ProjectRecordMemberResolution.Resolved)?.let {
                    rows += "${node.token.line} $text member Resolved ${it.declaration.qualifiedName}.${it.field.name.originalSpelling}"
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
