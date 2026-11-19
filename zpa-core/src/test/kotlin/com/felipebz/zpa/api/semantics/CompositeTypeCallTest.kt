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

/** Calls that pass a record, a collection, a row or a ref cursor where another composite type is expected. */
class CompositeTypeCallTest {
    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    /** `line:status (selected formals) [binding outcomes]` for every call of a `pk_` package in [user]. */
    private fun answers(user: String, metadata: ProjectExternalMetadata = this.metadata): List<String> {
        val sources = mapOf("project.sql" to project, "u.sql" to user)
        val external = ProjectExternalContext.of(metadata, ProjectIdentifier.fromSource("APP"), ProjectSchemaPaths.none())
        val context = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation.withSchemas(ProjectDeclarationExtractor(), external)
                .prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
        )
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.MEMBER_EXPRESSION)
                    .filter { it.tokenValue.startsWith("PK_") && it.getFirstAncestorOrNull(PlSqlGrammar.DATATYPE) == null }.forEach {
                    rows += "${it.token.line}:" + when (val binding = this.context.callBinding(it)) {
                        is CallBinding.Project -> {
                            val resolution = binding.resolution
                            val selected = resolution.selected.map { s ->
                                " (" + s.declaration.parameters.joinToString { p -> "${p.name} ${p.typeRef.name}" } + ") " +
                                    resolution.evaluations.single { e -> e.candidate == s }.bindings.map { b -> b.outcome }
                            }.orElse("")
                            "${resolution.status}$selected"
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

    private fun publicType(builder: ProjectExternalMetadata.Builder, type: ProjectExternalObjectType): ProjectExternalMetadata.Builder {
        val name = type.name.originalSpelling.trim('"')
        return builder.objectType(type).synonym(ProjectExternalSynonym.of("PUBLIC", name, type.owner.originalSpelling.trim('"'), name, null))
    }

    private val opaque = ProjectExternalObjectType.of("SYS", "ANYDATA", ProjectExternalObjectType.Kind.OPAQUE, null, null, emptyList(), emptyList())
    private val names = ProjectExternalObjectType.of("SYS", "ODCIVARCHAR2LIST", ProjectExternalObjectType.Kind.COLLECTION, null, null, emptyList(), emptyList())

    /** The schema APP complete, two types of SYS behind their public synonyms, and a public synonym of a project collection. */
    private val metadata: ProjectExternalMetadata = listOf(opaque, names)
        .fold(ProjectExternalMetadata.builder("fixture").completeSchema("APP").completePublicSynonyms()) { builder, type -> publicType(builder, type) }
        .synonym(ProjectExternalSynonym.of("PUBLIC", "CRATE_LIST_PUB", "APP", "CRATE_LIST", null))
        .build()

    private val project = "create package pk_list as\n" +
        "  type t_tab is table of varchar2(100);\n" +
        "  type t_rec is record (n number);\n" +
        "  subtype s_list is sys.odcivarchar2list;\n" +
        "  subtype s_obj is crate_obj;\n" +
        "  function chk(a anydata) return number;\n" +
        "  function chk(a sys.odcivarchar2list) return number;\n" +
        "  function chk(a number) return number;\n" +
        "  function two(a t_tab) return number;\n" +
        "  function two(a number) return number;\n" +
        "  function rc(a sys_refcursor) return number;\n" +
        "  function rc(a number) return number;\n" +
        "end;\n/\n" +
        "create package pk_take as\n" +
        "  procedure crate(x crate_list);\n" +
        "  procedure tab(x pk_list.t_tab);\n" +
        "  procedure any_value(x anydata);\n" +
        "  procedure cursor_value(x sys_refcursor);\n" +
        "  procedure obj(x crate_obj);\n" +
        "  procedure missing(x zz_missing_t);\n" +
        "  procedure dict(x dbms_sql.varchar2_table);\n" +
        "end;\n/\n" +
        "create type crate_list as table of varchar2(100);\n/\n" +
        "create type crate_obj as object (n number);\n/\n"

    @Test
    fun aDeclaredOrSchemaCollectionIsNoOtherCompositeType() {
        val user = "create procedure run_list is\n" +
            "  t pk_list.t_tab; r pk_list.t_rec; s pk_list.s_list; c crate_list; l sys.odcivarchar2list; n number;\n" +
            "  type t_loc is table of number; lt t_loc;\n" +
            "begin\n" +
            "  n := pk_list.chk(t);\n" +
            "  n := pk_list.chk(r);\n" +
            "  n := pk_list.chk(s);\n" +
            "  n := pk_list.chk(c);\n" +
            "  n := pk_list.chk(lt);\n" +
            "  n := pk_list.two(l);\n" +
            "  n := pk_list.two(c);\n" +
            "  n := pk_list.rc(t);\n" +
            "  n := pk_list.rc(r);\n" +
            "end;\n/\n"
        assertThat(answers(user)).containsExactly(
            "5:NO_COMPATIBLE_CANDIDATE",
            "6:NO_COMPATIBLE_CANDIDATE",
            // A subtype of a schema type is that type.
            "7:RESOLVED (a sys.odcivarchar2list) [COMPATIBLE]",
            "8:NO_COMPATIBLE_CANDIDATE",
            "9:NO_COMPATIBLE_CANDIDATE",
            "10:NO_COMPATIBLE_CANDIDATE",
            "11:NO_COMPATIBLE_CANDIDATE",
            "12:NO_COMPATIBLE_CANDIDATE",
            "13:NO_COMPATIBLE_CANDIDATE"
        )
    }

    @Test
    fun aRowASubtypeAndASynonymAgainstSchemaTypes() {
        val user = "create procedure run_take is\n" +
            "  t pk_list.t_tab; c crate_list; d dual%rowtype; o pk_list.s_obj; p crate_list_pub;\n" +
            "begin\n" +
            "  pk_take.crate(t);\n" +
            "  pk_take.tab(c);\n" +
            "  pk_take.any_value(d);\n" +
            "  pk_take.cursor_value(d);\n" +
            "  pk_take.obj(o);\n" +
            "  pk_take.crate(p);\n" +
            "end;\n/\n"
        assertThat(answers(user)).containsExactly(
            // A declared collection and a schema collection with the same element reject each other.
            "4:NO_COMPATIBLE_CANDIDATE",
            "5:NO_COMPATIBLE_CANDIDATE",
            // A row is no opaque type and no ref cursor.
            "6:NO_COMPATIBLE_CANDIDATE",
            "7:NO_COMPATIBLE_CANDIDATE",
            // A package subtype of an object type, and a synonym of a collection, are that type.
            "8:RESOLVED (x crate_obj) [COMPATIBLE]",
            "9:RESOLVED (x crate_list) [COMPATIBLE]"
        )
    }

    @Test
    fun aTypeThatIsNotKnownStaysUndecided() {
        val user = "create procedure run_unknown is\n" +
            "  t pk_list.t_tab; m zz_missing_t; v dbms_sql.varchar2_table;\n" +
            "begin\n" +
            "  pk_take.missing(t);\n" +
            "  pk_take.tab(m);\n" +
            "  pk_take.dict(t);\n" +
            "  pk_take.tab(v);\n" +
            "  pk_take.crate(v);\n" +
            "end;\n/\n"
        assertThat(answers(user)).allMatch { it.substringAfter(':').startsWith("RESOLVED") && it.endsWith("[UNKNOWN]") }.hasSize(5)
    }
}
