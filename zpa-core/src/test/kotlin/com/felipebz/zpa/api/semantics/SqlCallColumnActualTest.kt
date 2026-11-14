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
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** The overload a call written in static SQL binds when an actual is a column, or a PL/SQL name no column takes. */
class SqlCallColumnActualTest {
    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private val project = "create package pk_ov as\n" +
        "  function f(p_val varchar2) return number;\n  function f(p_val number) return number;\n" +
        "  function f(p_val date) return number;\n  function f(p_val binary_double) return number;\n" +
        "  function f_pi(p_val number) return number;\n  function f_pi(p_val pls_integer) return number;\n" +
        "  function f_bd(p_val number) return number;\n  function f_bd(p_val binary_double) return number;\nend;\n/\n"

    private val user = "create procedure run_sql(p_num number, p_txt varchar2) is\n" +
        "  v number;\n" +
        "begin\n" +
        "  select pk_ov.f(o.code) into v from orders o;\n" +
        "  select pk_ov.f(qty) into v from orders;\n" +
        "  select count(*) into v from orders where pk_ov.f(code) = 1;\n" +
        "  select pk_ov.f(p_num) into v from orders;\n" +
        "  select pk_ov.f(p_txt) into v from orders o where o.qty = 1;\n" +
        "  update orders set qty = pk_ov.f(code);\n" +
        "  insert into orders (qty) select pk_ov.f(i.bd) from items i;\n" +
        "  for r in (select pk_ov.f(o.dt) x from orders o) loop\n" +
        "    null;\n" +
        "  end loop;\n" +
        "  select pk_ov.f_pi(1) into v from dual;\n" +
        "  select pk_ov.f_bd(1) into v from dual;\n" +
        "  select pk_ov.f(n) into v from (select qty n from orders);\n" +
        "  select pk_ov.f(x_col) into v from missing_tab;\n" +
        "end;\n/\n"

    private val metadata = ProjectExternalMetadata.builder("test").completeSchema("SHOP")
        .relation(ProjectExternalRelation.of("SHOP", "ORDERS", true, listOf(
            ProjectExternalColumn.of("CODE", 1, ProjectExternalDatatype.of("VARCHAR2", null, 10, ProjectExternalDatatype.LengthSemantics.BYTE, null, null)),
            ProjectExternalColumn.of("QTY", 2, ProjectExternalDatatype.of("NUMBER", null, null, null, null, null)),
            ProjectExternalColumn.of("DT", 3, ProjectExternalDatatype.of("DATE", null, null, null, null, null)))))
        .relation(ProjectExternalRelation.of("SHOP", "ITEMS", true, listOf(
            ProjectExternalColumn.of("BD", 1, ProjectExternalDatatype.of("BINARY_DOUBLE", null, null, null, null, null)))))
        .build()

    private fun render(binding: CallBinding): String = when (binding) {
        is CallBinding.Project -> {
            val resolution = binding.resolution
            val selected = resolution.selected.map { " (" + it.declaration.parameters.joinToString { p -> p.typeRef.name.toString().uppercase() } + ")" }.orElse("")
            "${resolution.status}$selected"
        }
        else -> binding.toString()
    }

    /** `line:call=answer` for each call of the package. */
    private fun answers(withMetadata: Boolean): List<String> {
        val external = ProjectExternalContext.of(if (withMetadata) metadata else ProjectExternalMetadata.none(), ProjectIdentifier.fromSource("SHOP"), ProjectSchemaPaths.none())
        val sources = mapOf("project.sql" to project, "u.sql" to user)
        val context = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation.withSchemas(ProjectDeclarationExtractor(), external).prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
        )
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.MEMBER_EXPRESSION).filter { it.tokens.first().originalValue == "pk_ov" }.forEach {
                    rows += "${it.token.line}:${it.tokens.joinToString("") { t -> t.originalValue }}=${render(this.context.callBinding(it))}"
                }
            }
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, context).scanFile(Source("u.sql", user), listOf(check), FileId("u.sql"))
        return rows
    }

    @Test
    fun withTheRelationsColumns() {
        assertThat(answers(withMetadata = true)).containsExactly(
            "4:pk_ov.f=RESOLVED (VARCHAR2)",
            "5:pk_ov.f=RESOLVED (NUMBER)",
            "6:pk_ov.f=RESOLVED (VARCHAR2)",
            // No column of the statement takes the name: the parameter.
            "7:pk_ov.f=RESOLVED (NUMBER)",
            "8:pk_ov.f=RESOLVED (VARCHAR2)",
            "9:pk_ov.f=RESOLVED (VARCHAR2)",
            "10:pk_ov.f=RESOLVED (BINARY_DOUBLE)",
            "11:pk_ov.f=RESOLVED (DATE)",
            // In SQL an integer literal is a NUMBER.
            "14:pk_ov.f_pi=RESOLVED (NUMBER)",
            "15:pk_ov.f_bd=RESOLVED (NUMBER)",
            // A column of an inline view, and one whose relation's columns are not known.
            "16:pk_ov.f=AMBIGUOUS",
            "17:pk_ov.f=AMBIGUOUS"
        )
    }

    @Test
    fun withoutTheRelationsColumns() {
        assertThat(answers(withMetadata = false)).containsExactly(
            "4:pk_ov.f=AMBIGUOUS",
            "5:pk_ov.f=AMBIGUOUS",
            "6:pk_ov.f=AMBIGUOUS",
            "7:pk_ov.f=AMBIGUOUS",
            "8:pk_ov.f=AMBIGUOUS",
            "9:pk_ov.f=AMBIGUOUS",
            "10:pk_ov.f=AMBIGUOUS",
            "11:pk_ov.f=AMBIGUOUS",
            "14:pk_ov.f_pi=RESOLVED (NUMBER)",
            "15:pk_ov.f_bd=RESOLVED (NUMBER)",
            "16:pk_ov.f=AMBIGUOUS",
            "17:pk_ov.f=AMBIGUOUS"
        )
    }
}
