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

/** The overload a call binds when an actual is a field of a record of a relation's row or a `SUBSTR` call. */
class OverloadFieldActualTest {
    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private val project = "create package pk_ov as\n" +
        "  procedure put(p_val varchar2);\n  procedure put(p_val number);\n" +
        "  procedure put_lob(p_val varchar2);\n  procedure put_lob(p_val clob);\n" +
        "  procedure put_nat(p_val varchar2);\n  procedure put_nat(p_val nvarchar2);\n" +
        "  procedure put_n(p_val number);\n  procedure put_n(p_val binary_double);\n" +
        "  procedure put_t(p_val date);\n  procedure put_t(p_val timestamp);\nend;\n/\n"

    private val compound = "create procedure run_union is\n" +
        "  cursor c1 is select qty from orders union all select bd from items;\n" +
        "begin\n" +
        "  for r in c1 loop\n" +
        "    pk_ov.put_n(r.qty);\n" +
        "  end loop;\n" +
        "  for r in (select dt from orders union all select ts from items) loop\n" +
        "    pk_ov.put_t(r.dt);\n" +
        "  end loop;\n" +
        "  for r in (select qty from orders union select qty from orders) loop\n" +
        "    pk_ov.put(r.qty);\n" +
        "  end loop;\n" +
        "  for r in (select code from orders minus select code from items) loop\n" +
        "    pk_ov.put(r.code);\n" +
        "  end loop;\n" +
        "  for r in (select code from orders union all select flag from items) loop\n" +
        "    pk_ov.put(r.code);\n" +
        "  end loop;\n" +
        "  for r in (select qty from orders union all select code || 'x' from items) loop\n" +
        "    pk_ov.put(r.qty);\n" +
        "  end loop;\n" +
        "end;\n/\n"

    private val user = "create procedure run_orders(c varchar2, l clob, ch char, nl nclob, n number) is\n" +
        "  t orders%rowtype;\n" +
        "  cursor cur is select code, qty from orders;\n" +
        "  r2 cur%rowtype;\n" +
        "begin\n" +
        "  pk_ov.put(t.code);\n" +
        "  pk_ov.put(t.qty);\n" +
        "  for r in (select code, qty from orders) loop\n" +
        "    pk_ov.put(r.code);\n" +
        "    pk_ov.put(r.qty);\n" +
        "  end loop;\n" +
        "  for r in cur loop\n" +
        "    pk_ov.put(r.code);\n" +
        "  end loop;\n" +
        "  fetch cur into r2;\n" +
        "  pk_ov.put(r2.qty);\n" +
        "  pk_ov.put(substr(c, 1, 3));\n" +
        "  pk_ov.put_lob(substr(l, 1, 3));\n" +
        "  for r in (select code || 'x' code, count(*) qty from orders) loop\n" +
        "    pk_ov.put(r.code);\n" +
        "    pk_ov.put(r.qty);\n" +
        "  end loop;\n" +
        "  pk_ov.put_lob(substr(ch, 1, 2));\n" +
        "  pk_ov.put_lob(substr(nl, 1, 2));\n" +
        "  pk_ov.put(substr(n, 1, 2));\n" +
        "  pk_ov.put_nat(substr(c, 1, 2));\n" +
        "  pk_ov.put(substr(c));\n" +
        "end;\n/\n"

    private val metadata = ProjectExternalMetadata.builder("test").completeSchema("SHOP")
        .relation(ProjectExternalRelation.of("SHOP", "ORDERS", true, listOf(
            ProjectExternalColumn.of("CODE", 1, ProjectExternalDatatype.of("VARCHAR2", null, 10, ProjectExternalDatatype.LengthSemantics.BYTE, null, null)),
            ProjectExternalColumn.of("QTY", 2, ProjectExternalDatatype.of("NUMBER", null, null, null, null, null)),
            ProjectExternalColumn.of("DT", 3, ProjectExternalDatatype.of("DATE", null, null, null, null, null)))))
        .relation(ProjectExternalRelation.of("SHOP", "ITEMS", true, listOf(
            ProjectExternalColumn.of("CODE", 1, ProjectExternalDatatype.of("VARCHAR2", null, 5, ProjectExternalDatatype.LengthSemantics.BYTE, null, null)),
            ProjectExternalColumn.of("TS", 2, ProjectExternalDatatype.of("TIMESTAMP(6)", null, null, null, null, 6)),
            ProjectExternalColumn.of("BD", 3, ProjectExternalDatatype.of("BINARY_DOUBLE", null, null, null, null, null)),
            ProjectExternalColumn.of("FLAG", 4, ProjectExternalDatatype.of("CHAR", null, 1, ProjectExternalDatatype.LengthSemantics.BYTE, null, null)))))
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
    private fun answers(withMetadata: Boolean, metadataAfterPreparation: Boolean = false, user: String = this.user): List<String> {
        val prepared = if (withMetadata && !metadataAfterPreparation) metadata else ProjectExternalMetadata.none()
        val external = ProjectExternalContext.of(prepared, ProjectIdentifier.fromSource("SHOP"), ProjectSchemaPaths.none())
        val sources = mapOf("project.sql" to project, "u.sql" to user)
        val context = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation.withSchemas(ProjectDeclarationExtractor(), external).prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
        ).let { if (metadataAfterPreparation) it.withMetadata(metadata) else it }
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

    private val withColumns = listOf(
        "6:pk_ov.put=RESOLVED (VARCHAR2)",
        "7:pk_ov.put=RESOLVED (NUMBER)",
        // The field of a query's row: its select item's column.
        "9:pk_ov.put=RESOLVED (VARCHAR2)",
        "10:pk_ov.put=RESOLVED (NUMBER)",
        "13:pk_ov.put=RESOLVED (VARCHAR2)",
        "16:pk_ov.put=RESOLVED (NUMBER)",
        "17:pk_ov.put=RESOLVED (VARCHAR2)",
        "18:pk_ov.put_lob=RESOLVED (CLOB)",
        // A select item that is not a plain column is not typed.
        "20:pk_ov.put=AMBIGUOUS",
        "21:pk_ov.put=AMBIGUOUS",
        // SUBSTR of a CHAR is a VARCHAR2, of an NCLOB a CLOB, of a number a VARCHAR2.
        "23:pk_ov.put_lob=RESOLVED (VARCHAR2)",
        "24:pk_ov.put_lob=RESOLVED (CLOB)",
        "25:pk_ov.put=RESOLVED (VARCHAR2)",
        // Its character set follows its argument: Oracle finds this call ambiguous (PLS-00307).
        "26:pk_ov.put_nat=AMBIGUOUS",
        // One argument: Oracle rejects it (PLS-00306).
        "27:pk_ov.put=AMBIGUOUS"
    )

    @Test
    fun withTheRelationsColumns() {
        assertThat(answers(withMetadata = true)).containsExactlyElementsOf(withColumns)
    }

    @Test
    fun withTheRelationsColumnsGivenAfterThePreparation() {
        assertThat(answers(withMetadata = true, metadataAfterPreparation = true)).containsExactlyElementsOf(withColumns)
    }

    @Test
    fun withoutTheRelationsColumns() {
        assertThat(answers(withMetadata = false)).containsExactly(
            "6:pk_ov.put=AMBIGUOUS",
            "7:pk_ov.put=AMBIGUOUS",
            "9:pk_ov.put=AMBIGUOUS",
            "10:pk_ov.put=AMBIGUOUS",
            "13:pk_ov.put=AMBIGUOUS",
            "16:pk_ov.put=AMBIGUOUS",
            // SUBSTR does not need the metadata.
            "17:pk_ov.put=RESOLVED (VARCHAR2)",
            "18:pk_ov.put_lob=RESOLVED (CLOB)",
            "20:pk_ov.put=AMBIGUOUS",
            "21:pk_ov.put=AMBIGUOUS",
            "23:pk_ov.put_lob=RESOLVED (VARCHAR2)",
            "24:pk_ov.put_lob=RESOLVED (CLOB)",
            "25:pk_ov.put=RESOLVED (VARCHAR2)",
            "26:pk_ov.put_nat=AMBIGUOUS",
            "27:pk_ov.put=AMBIGUOUS"
        )
    }

    @Test
    fun aFieldOfACompoundQueryTakesTheTypeOfEveryBranch() {
        assertThat(answers(withMetadata = true, user = compound)).containsExactly(
            // NUMBER and BINARY_DOUBLE give BINARY_DOUBLE; DATE and TIMESTAMP give TIMESTAMP.
            "5:pk_ov.put_n=RESOLVED (BINARY_DOUBLE)",
            "8:pk_ov.put_t=RESOLVED (TIMESTAMP)",
            "11:pk_ov.put=RESOLVED (NUMBER)",
            "14:pk_ov.put=RESOLVED (VARCHAR2)",
            // VARCHAR2 and CHAR.
            "17:pk_ov.put=RESOLVED (VARCHAR2)",
            // A branch whose item is not typed.
            "20:pk_ov.put=AMBIGUOUS"
        )
        assertThat(answers(withMetadata = false, user = compound)).containsExactly(
            "5:pk_ov.put_n=AMBIGUOUS", "8:pk_ov.put_t=AMBIGUOUS", "11:pk_ov.put=AMBIGUOUS", "14:pk_ov.put=AMBIGUOUS",
            "17:pk_ov.put=AMBIGUOUS", "20:pk_ov.put=AMBIGUOUS"
        )
    }
}
