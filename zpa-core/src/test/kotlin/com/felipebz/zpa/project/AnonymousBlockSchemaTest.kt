//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(com.felipebz.zpa.api.annotations.ZpaExperimentalApi::class)

package com.felipebz.zpa.project

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** An anonymous block reads names in its file's schema; a type body finds its specification by schema for `SELF`. */
class AnonymousBlockSchemaTest {
    private class Source(private val id: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = id
        override fun path(): Path = Path.of(id)
        override fun type() = PlSqlFile.Type.MAIN
    }

    /** `line:text=answer` for the variable types, the member names and the call and sequence names of [file]. */
    private fun answers(defaultSchema: String?, file: String, vararg sources: Pair<String, String>): List<String> {
        val schemas = if (defaultSchema == null) ProjectExternalContext.none()
        else ProjectExternalContext.of(ProjectExternalMetadata.none(), ProjectIdentifier.fromSource(defaultSchema), ProjectSchemaPaths.none())
        val preparation = ProjectIndexPreparation.withSchemas(ProjectDeclarationExtractor(), schemas)
            .prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.DATATYPE).filter { it.parent.type == PlSqlGrammar.VARIABLE_DECLARATION }.forEach {
                    rows += "${it.token.line}:${it.tokens.joinToString("") { t -> t.originalValue }}=${context.valueCategories!!.typeName(it)}"
                }
                node.getDescendants(PlSqlGrammar.IDENTIFIER_NAME).filter { it.tokenOriginalValue in MEMBERS }.forEach {
                    rows += "${it.token.line}:${it.tokenOriginalValue}=${context.fieldBinding(it)}"
                }
                node.getDescendants(PlSqlGrammar.MEMBER_EXPRESSION).filter { it.tokenOriginalValue == "pk_r" && it.parent.type == PlSqlGrammar.ASSIGNMENT_STATEMENT }
                    .forEach { rows += "${it.token.line}:${it.tokens.joinToString("") { t -> t.originalValue }}=${context.valueCategory(it)}" }
            }
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, ProjectAnalysisContext.prepared(preparation))
            .scanFile(Source(file, sources.toMap().getValue(file)), listOf(check), FileId(file))
        return rows
    }

    private fun declarations(owner: String) = "d.sql" to
        "create or replace type ${owner}t_o as object (n number);\n/\n" +
        "create or replace package ${owner}pk_r as\n  type t_r is record (f number);\n  g t_r;\n  function f_one(p number) return t_r;\nend;\n/\n"

    private val body = "  v t_o;\n  r pk_r.t_r;\n  x number;\nbegin\n  v.n := 1;\n  r.f := 2;\n  pk_r.g.f := 3;\n  x := pk_r.f_one(1).f;\nend;\n/\n"
    private val block = "u.sql" to "declare\n$body"
    private val procedure = "u.sql" to "create or replace procedure use_it is\n$body"

    @Test
    fun anAnonymousBlockReadsNamesInItsFilesSchemaAsAUnitOfThatFile() {
        val inProcedure = answers("APP", "u.sql", declarations("app."), procedure)
        assertThat(inProcedure).contains(
            "2:t_o=Project(STANDALONE_TYPE app.t_o)", "3:pk_r.t_r=Project(PACKAGE_TYPE app.pk_r.t_r)",
            "6:n=Field(ATTRIBUTE#0@d.sql:1:42)", "7:f=Field(RECORD_FIELD#0@d.sql:4:22)", "8:f=Field(RECORD_FIELD#0@d.sql:4:22)"
        )
        assertThat(inProcedure).contains("9:f=Field(RECORD_FIELD#0@d.sql:4:22)")
        // The block: the same types, fields, package value and call as the procedure of the same file.
        assertThat(answers("APP", "u.sql", declarations("app."), block)).isEqualTo(inProcedure)
        // As when the declarations are written without the owner.
        assertThat(answers("APP", "u.sql", declarations(""), block)).isEqualTo(answers("APP", "u.sql", declarations(""), procedure))
    }

    /** What `sq.nextval` resolves to in [code], with `APP` as the default schema. */
    private fun sequence(code: String): List<SequenceReferenceResolution> {
        val schemas = ProjectExternalContext.of(ProjectExternalMetadata.none(), ProjectIdentifier.fromSource("APP"), ProjectSchemaPaths.none())
        val preparation = ProjectIndexPreparation.withSchemas(ProjectDeclarationExtractor(), schemas).prepare(
            listOf(ProjectSource(FileId("s.sql")) { "create sequence app.sq;\n" }, ProjectSource(FileId("u.sql")) { code }), concurrent = false
        )
        val check = object : PlSqlCheck() {
            val results = ArrayList<SequenceReferenceResolution>()
            override fun init() = subscribeTo(PlSqlGrammar.MEMBER_EXPRESSION)
            override fun visitNode(node: AstNode) {
                results += projectAnalysis().resolveSequenceReference(node)
            }
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, ProjectAnalysisContext.prepared(preparation))
            .scanFile(Source("u.sql", code), listOf(check), FileId("u.sql"))
        return check.results
    }

    @Test
    fun aSequenceReadInAnAnonymousBlockIsTheOneOfItsFilesSchema() {
        val code = "  n number;\nbegin\n  n := sq.nextval;\nend;\n/\n"
        assertThat(sequence("create or replace procedure use_it is\n$code")).containsExactly(SequenceReferenceResolution.RESOLVED_SEQUENCE)
        assertThat(sequence("declare\n$code")).containsExactly(SequenceReferenceResolution.RESOLVED_SEQUENCE)
    }

    @Test
    fun withoutAnySchemaTheBlockReadsOnlyWrittenNames() {
        val rows = answers(null, "u.sql", declarations("app."), block)
        assertThat(rows).contains("2:t_o=Unresolved(NOT_FOUND_IN_PROJECT)", "6:n=Unknown[DECLARATION_OUTSIDE_FILE]", "7:f=Unknown[DECLARATION_OUTSIDE_FILE]")
    }

    private val typeSpec = "t.tps" to "create or replace type SPEC as object (\n  n_val number,\n  member procedure bump,\n  member procedure twice\n);\n/\n"
    private fun typeBody(name: String) = "t.tpb" to "create or replace type body $name as\n  member procedure bump is\n  begin\n" +
        "    n_val := self.n_val + 1;\n  end;\n  member procedure twice is\n  begin\n    bump;\n    self.bump;\n  end;\nend;\n/\n"

    private fun selfAnswers(defaultSchema: String?, spec: String, body: String) =
        answers(defaultSchema, "t.tpb", typeSpec.first to typeSpec.second.replace("SPEC", spec), typeBody(body))
            .filter { it.contains("n_val") || it.contains("bump") }

    @Test
    fun theImplicitSelfOfATypeBodyFindsItsSpecificationWhicheverHalfWritesTheOwner() {
        val alike = selfAnswers("APP", "t_obj", "t_obj")
        assertThat(alike).contains("4:n_val=Field(ATTRIBUTE#0@t.tps:2:2)")
        assertThat(alike.filter { it.startsWith("4:n_val") }).hasSize(2).allMatch { it.endsWith("=Field(ATTRIBUTE#0@t.tps:2:2)") }
        assertThat(selfAnswers("APP", "app.t_obj", "t_obj")).isEqualTo(alike)
        assertThat(selfAnswers("APP", "t_obj", "app.t_obj")).isEqualTo(alike)
    }

    @Test
    fun withoutAnySchemaTheUnownedBodyIsAnotherType() {
        assertThat(selfAnswers(null, "app.t_obj", "t_obj").filter { it.startsWith("4:n_val") })
            .contains("4:n_val=Unknown[DECLARATION_OUTSIDE_FILE]")
    }

    companion object {
        private val MEMBERS = setOf("n", "f", "n_val", "bump")
    }
}
