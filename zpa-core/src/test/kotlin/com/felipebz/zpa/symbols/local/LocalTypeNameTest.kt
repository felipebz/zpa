//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** The declaration a written type name denotes, as the local type walk answers it. */
class LocalTypeNameTest {
    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private val packageSpec = "create package p as\n" +
        "  type t_rec is record (n number);\n" +
        "  subtype t_alias is t_rec;\n" +
        "end;\n/\n"

    private val types = "create type t_obj as object (a number);\n/\n" +
        "create type t_twice as object (a number);\n/\n"

    private val twice = "create type t_twice as object (b number);\n/\n"

    private val user = "create procedure u is\n" +
        "  v p.t_rec;\n" +
        "  o t_obj;\n" +
        "  s p.t_alias;\n" +
        "  w t_twice;\n" +
        "  n t_none;\n" +
        "  type lr is record (x number);\n" +
        "  l lr;\n" +
        "  i pls_integer;\n" +
        "begin\n  null;\nend;\n/\n"

    private fun names(file: String, sources: Map<String, String>): List<String> {
        val context = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation().prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
        )
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.DATATYPE).filter { it.parent.type == PlSqlGrammar.VARIABLE_DECLARATION }.forEach {
                    rows += "${it.token.line}:${it.tokens.joinToString("") { t -> t.originalValue }}=${this.context.valueCategories!!.typeName(it)}"
                }
            }
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, context).scanFile(Source(file, sources.getValue(file)), listOf(check), FileId(file))
        return rows
    }

    @Test
    fun theFirstDeclarationAWrittenTypeNameReaches() {
        val sources = mapOf("p.sql" to packageSpec, "t.sql" to types, "w.sql" to twice, "u.sql" to user)
        assertThat(names("u.sql", sources)).containsExactly(
            "2:p.t_rec=Project(PACKAGE_TYPE p.t_rec)",
            "3:t_obj=Project(STANDALONE_TYPE t_obj)",
            // A subtype in another file is the declaration the name denotes, not the record type it is a subtype of.
            "4:p.t_alias=Project(PACKAGE_SUBTYPE p.t_alias)",
            // Two schema types of the name in the project: no one declaration.
            "5:t_twice=Unresolved(AMBIGUOUS)",
            "6:t_none=Unresolved(NOT_FOUND_IN_PROJECT)",
            "8:lr=Local(D6)",
            "9:pls_integer=BuiltIn(pls_integer)"
        )
    }

    @Test
    fun conditionalCompilationLeavesTheNameUndecided() {
        val conditional = user.replace("  type lr is record (x number);\n", "  \$if \$\$flag \$then type t_rec is record (y number); \$end\n  type lr is record (x number);\n")
        val sources = mapOf("p.sql" to packageSpec, "t.sql" to types, "u.sql" to conditional)
        assertThat(names("u.sql", sources)).allMatch { it.endsWith("Undecided[CONDITIONAL_COMPILATION]") }
    }
}
