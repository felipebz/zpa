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
import com.felipebz.zpa.squid.ConditionalCompilationSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** Actuals that call LENGTHB, CARDINALITY, EMPTY_BLOB or EMPTY_CLOB of STANDARD. */
class StandardFunctionActualTest {
    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private val project = "create type bin_list as table of varchar2(30);\n/\ncreate type bin_arr as varray(5) of number;\n/\n" +
        "create package pk_bin as\n" +
        "  function chk(a number) return number;\n" +
        "  function chk(a varchar2) return number;\n" +
        "  function chk(a clob) return number;\n" +
        "  function chk(a blob) return number;\n" +
        "  function chk(a date) return number;\n" +
        "  function get_list return bin_list;\n" +
        "end;\n/\n"

    private fun answers(body: String, declarations: String = ""): List<String> {
        val user = "create procedure run_bin is\n  t varchar2(100); c clob; l bin_list; d date; n number;$declarations\nbegin\n$body\nend;\n/\n"
        val sources = mapOf("project.sql" to project, "u.sql" to user)
        val external = ProjectExternalContext.of(ProjectExternalMetadata.none(), ProjectIdentifier.fromSource("APP"), ProjectSchemaPaths.none())
        val context = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation.withSchemas(ProjectDeclarationExtractor(), external)
                .prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
        )
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.MEMBER_EXPRESSION).filter { it.tokens.joinToString("") { t -> t.originalValue } == "pk_bin.chk" }.forEach {
                    rows += "${it.token.line}:" + when (val binding = this.context.callBinding(it)) {
                        is CallBinding.Project -> {
                            val resolution = binding.resolution
                            val reasons = resolution.evaluations.flatMap { e -> e.bindings.mapNotNull { b -> b.unknownReasonOrNull } }.toSet()
                            resolution.status.toString() +
                                resolution.selected.map { s -> " (" + s.declaration.parameters.joinToString { p -> "${p.name} ${p.typeRef.name}" } + ")" }.orElse("") +
                                if (reasons.isEmpty()) "" else " $reasons"
                        }
                        else -> binding.toString()
                    }
                }
            }
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, context, ConditionalCompilationSettings())
            .scanFile(Source("u.sql", user), listOf(check), FileId("u.sql"))
        return rows
    }

    @Test
    fun theResultOfEachFunctionSelectsItsOverload() {
        assertThat(
            answers(
                "  n := pk_bin.chk(lengthb(t));\n" +
                    "  n := pk_bin.chk(lengthb(c));\n" +
                    "  n := pk_bin.chk(cardinality(l));\n" +
                    "  n := pk_bin.chk(cardinality(pk_bin.get_list));\n" +
                    "  n := pk_bin.chk(empty_blob());\n" +
                    "  n := pk_bin.chk(empty_clob());"
            )
        ).containsExactly(
            "4:RESOLVED (a number)",
            // INTEGER for a CLOB.
            "5:RESOLVED (a number)",
            // PLS_INTEGER.
            "6:RESOLVED (a number)",
            "7:RESOLVED (a number)",
            "8:RESOLVED (a blob)",
            "9:RESOLVED (a clob)"
        )
    }

    @Test
    fun anArgumentOfNoTypeOrAnotherFamilyOrAWrongCountLeavesTheResultUnknown() {
        assertThat(
            answers(
                "  n := pk_bin.chk(lengthb(nothing_here));\n" +
                    "  n := pk_bin.chk(cardinality(nothing_here));\n" +
                    "  n := pk_bin.chk(cardinality(t));\n" +
                    "  n := pk_bin.chk(lengthb(l));\n" +
                    "  n := pk_bin.chk(empty_blob(1));"
            )
        ).allSatisfy { assertThat(it).contains("AMBIGUOUS").contains("ACTUAL_UNKNOWN") }.hasSize(5)
    }

    @Test
    fun cardinalityTakesANestedTableOnly() {
        val declarations = " type va is varray(3) of number; v va; type aa is table of number index by pls_integer; a aa;" +
            " type nt is table of number; lt nt; s bin_arr;"
        val rows = answers(
            "  n := pk_bin.chk(cardinality(v));\n" +
                "  n := pk_bin.chk(cardinality(a));\n" +
                "  n := pk_bin.chk(cardinality(s));\n" +
                "  n := pk_bin.chk(cardinality(lt));",
            declarations
        )
        // A VARRAY or an associative array is rejected (PLS-00306).
        assertThat(rows.take(3)).allSatisfy { assertThat(it).contains("AMBIGUOUS").contains("ACTUAL_UNKNOWN") }
        assertThat(rows.drop(3)).containsExactly("7:RESOLVED (a number)")
    }
}
