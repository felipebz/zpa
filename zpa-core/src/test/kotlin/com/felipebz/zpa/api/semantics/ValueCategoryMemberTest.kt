//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.api.semantics

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** Member chains on values whose record or object type is declared in another file of the project. */
class ValueCategoryMemberTest {
    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private val packageSpec = "create package p as\n" +
        "  type t_in is record (d date);\n" +
        "  type t_tab is table of t_in;\n" +
        "  type t_rec is record (n number, i t_in, tab t_tab, o t_sub);\n" +
        "  g t_rec;\n" +
        "  cursor c_emp is select id, salary from employees;\n" +
        "  r_emp c_emp%rowtype;\n" +
        "end;\n/\n"

    private val types = "create type t_obj as object (a number, b varchar2(10), member procedure m) not final;\n/\n" +
        "create type t_sub under t_obj (c date, inner t_obj);\n/\n"

    private val callerSpec = "create package q as\n  procedure r;\nend;\n/\n"

    private val callerBody = "create package body q as\n" +
        "  procedure r is\n" +
        "    v p.t_rec;\n" +
        "    o t_obj;\n" +
        "    s t_sub;\n" +
        "    x number;\n" +
        "  begin\n" +
        "    x := v.n;\n" +
        "    x := v.i.d;\n" +
        "    x := o.a;\n" +
        "    x := p.g.n;\n" +
        "    x := v.zz;\n" +
        "    x := s.a;\n" +
        "    x := s.inner.b;\n" +
        "    x := v.tab;\n" +
        "    x := v.o.c;\n" +
        "    x := v.i;\n" +
        "    x := p.r_emp.salary;\n" +
        "  end;\n" +
        "end;\n/\n"

    private val typeBody = "create type body t_obj as\n" +
        "  member procedure m is x number; begin x := self.a; x := a; end;\n" +
        "end;\n/\n"

    /** `line:value=category` for the value of every assignment of [file], scanned with [context]. */
    private fun assigned(file: String, context: ProjectAnalysisContext, sources: Map<String, String>): List<String> {
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                for (assignment in node.getDescendants(PlSqlGrammar.ASSIGNMENT_STATEMENT)) {
                    val value = assignment.children[2].let { if (it.type == PlSqlGrammar.MEMBER_EXPRESSION || it.type == PlSqlGrammar.VARIABLE_NAME) it else it.firstChild }
                    rows += "${assignment.token.line}:${value.tokens.joinToString("") { it.originalValue }}=${this.context.valueCategory(value)}"
                }
            }
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, context).scanFile(Source(file, sources.getValue(file)), listOf(check), FileId(file))
        return rows
    }

    private fun prepared(sources: Map<String, String>): ProjectAnalysisContext = ProjectAnalysisContext.prepared(
        ProjectIndexPreparation().prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
    )

    private val sources = mapOf("p.sql" to packageSpec, "t.sql" to types, "qs.sql" to callerSpec, "u.sql" to callerBody, "tb.sql" to typeBody)

    @Test
    fun membersOfProjectRecordAndObjectTypes() {
        assertThat(assigned("u.sql", prepared(sources), sources)).containsExactly(
            // A field of a package record type, at any depth.
            "8:v.n=Scalar(NUMERIC)",
            "9:v.i.d=Scalar(DATETIME)",
            // An object attribute.
            "10:o.a=Scalar(NUMERIC)",
            // A chain from a package variable of another file.
            "11:p.g.n=Scalar(NUMERIC)",
            // No such field.
            "12:v.zz=Unknown[UNSUPPORTED_REFERENCE]",
            // An attribute inherited from the supertype, and an attribute typed by an object type.
            "13:s.a=Scalar(NUMERIC)",
            "14:s.inner.b=Scalar(CHARACTER)",
            // A field typed by a collection gives its element; a field typed by a record or an object type, its category.
            "15:v.tab=Collection(NESTED_TABLE of Record(RECORD_TYPE))",
            "16:v.o.c=Scalar(DATETIME)",
            "17:v.i=Record(RECORD_TYPE)",
            // The project does not read a cursor's select list: its row's fields stay undecided.
            "18:p.r_emp.salary=Unknown[UNSUPPORTED_REFERENCE]"
        )
    }

    @Test
    fun selfAndTheBareAttributeInATypeBodyWhoseSpecificationIsInAnotherFile() {
        assertThat(assigned("tb.sql", prepared(sources), sources)).containsExactly("2:self.a=Scalar(NUMERIC)", "2:a=Scalar(NUMERIC)")
    }

    @Test
    fun aChainThroughAPackageVariableWrittenWithTheSchema() {
        val withSchema = mapOf("p.sql" to packageSpec.replace("create package p as", "create package hr.p as"),
            "s.sql" to "create procedure s is x number; begin x := hr.p.g.n; x := hr.p.g.i.d; end;\n/\n")
        assertThat(assigned("s.sql", prepared(withSchema), withSchema)).containsExactly("1:hr.p.g.n=Scalar(NUMERIC)", "1:hr.p.g.i.d=Scalar(DATETIME)")
    }

    @Test
    fun withoutAPreparedProjectTheChainsStayUnknown() {
        assertThat(assigned("u.sql", ProjectAnalysisContext.NOT_PREPARED, sources)).allMatch { it.substringAfter('=').startsWith("Unknown") }
        assertThat(assigned("tb.sql", ProjectAnalysisContext.NOT_PREPARED, sources)).allMatch { it.substringAfter('=').startsWith("Unknown") }
    }

    @Test
    fun conditionalCompilationKeepsTheChainUndecided() {
        // A dropped branch that ends a block it does not open may restructure the body: every chain stays undecided.
        val conditional = callerBody.replace("    x number;\n", "    x number;\n    \$if \$\$flag \$then y number; \$else end; \$end\n")
        val withIf = sources + ("u.sql" to conditional)
        assertThat(assigned("u.sql", prepared(withIf), withIf)).allMatch { it.substringAfter('=').startsWith("Unknown[") && "CONDITIONAL_COMPILATION" in it }
        // Conditional text that writes none of the chains' names leaves them decided.
        val unrelated = sources + ("u.sql" to conditional.replace("\$else end; ", ""))
        assertThat(assigned("u.sql", prepared(unrelated), unrelated)).noneMatch { "CONDITIONAL_COMPILATION" in it }
    }

    @Test
    fun theColumnsOfATableRowtypeAreCategorizedAsTheColumnsTypeAnchors() {
        val metadata = ProjectExternalMetadata.builder("fixture").completeSchema("HR").relation(
            ProjectExternalRelation.of(
                "HR", "EMPLOYEES", true, listOf(
                    ProjectExternalColumn.of("ID", 1, ProjectExternalDatatype.of("NUMBER", null, null, null, 8, 0)),
                    ProjectExternalColumn.of("NAME", 2, ProjectExternalDatatype.of("VARCHAR2", null, 30, null, null, null))
                )
            )
        ).build()
        val rows = mapOf("r.sql" to "create procedure r is e employees%rowtype; y employees.id%type; x number; begin x := e.id; x := e.name; x := e.nope; x := y; end;\n/\n")
        val context = prepared(rows).withExternalContext(ProjectExternalContext.of(metadata, ProjectIdentifier.fromSource("\"HR\"")))
        // A column of the row is categorized as the column's `%TYPE` is (`y`): the category does not read external
        // column types yet, so both are undecided. A column the table does not have is undecided too.
        assertThat(assigned("r.sql", context, rows)).containsExactly(
            "1:e.id=Unknown[DECLARATION_OUTSIDE_FILE]", "1:e.name=Unknown[DECLARATION_OUTSIDE_FILE]",
            "1:e.nope=Unknown[DECLARATION_OUTSIDE_FILE]", "1:y=Unknown[DECLARATION_OUTSIDE_FILE]"
        )
        // Without the metadata, nothing confirms the row's columns.
        assertThat(assigned("r.sql", prepared(rows), rows)).allMatch { it.substringAfter('=').startsWith("Unknown") }
    }
}
