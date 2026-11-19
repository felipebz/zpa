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

/** Calls whose actuals or formals are of object types that only the external metadata describes. */
class ExternalObjectTypeCallTest {
    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private fun answers(
        project: String, user: String, call: String, metadata: ProjectExternalMetadata = this.metadata, bindings: Boolean = false
    ): List<String> {
        val sources = mapOf("project.sql" to project, "u.sql" to user)
        val external = ProjectExternalContext.of(metadata, ProjectIdentifier.fromSource("APP"), ProjectSchemaPaths.none())
        val context = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation.withSchemas(ProjectDeclarationExtractor(), external)
                .prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
        )
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.MEMBER_EXPRESSION).filter { it.tokens.joinToString("") { t -> t.originalValue } == call }.forEach {
                    rows += "${it.token.line}:" + when (val binding = this.context.callBinding(it)) {
                        is CallBinding.Project -> {
                            val resolution = binding.resolution
                            val selected = resolution.selected.map { s -> " (" + s.declaration.parameters.joinToString { p -> "${p.name} ${p.typeRef.name}" } + ")" }.orElse("")
                            val outcomes = if (!bindings) "" else resolution.selected.map { s ->
                                " " + resolution.evaluations.single { e -> e.candidate == s }.bindings.map { b -> b.outcome }
                            }.orElse("")
                            "${resolution.status}$selected$outcomes"
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

    private fun datatype(name: String, owner: String? = null) = ProjectExternalDatatype.of(name, owner, null, null, null, null)

    private fun publicType(builder: ProjectExternalMetadata.Builder, type: ProjectExternalObjectType): ProjectExternalMetadata.Builder {
        val name = type.name.originalSpelling.trim('"')
        return builder.objectType(type).synonym(ProjectExternalSynonym.of("PUBLIC", name, type.owner.originalSpelling.trim('"'), name, null))
    }

    private val anyValue = ProjectExternalObjectType.of(
        "SYS", "ANYDATA", ProjectExternalObjectType.Kind.OPAQUE, null, null, emptyList(),
        listOf(
            ProjectExternalTypeMethod.function("CONVERTNUMBER", true, listOf(ProjectExternalMethodParameter.of("NUM", ProjectExternalMethodParameter.Mode.IN, datatype("NUMBER"))), datatype("ANYDATA", "SYS")),
            ProjectExternalTypeMethod.function("CONVERTOBJECT", true, listOf(ProjectExternalMethodParameter.of("OBJ", ProjectExternalMethodParameter.Mode.IN, datatype("PL/SQL LONG RAW"))), datatype("ANYDATA", "SYS")),
            ProjectExternalTypeMethod.function("CONVERTOBJECT", true, listOf(ProjectExternalMethodParameter.of("OBJ", ProjectExternalMethodParameter.Mode.IN, datatype("PL/SQL LONG RAW"))), datatype("ANYDATA", "SYS")),
            ProjectExternalTypeMethod.function("GETTYPENAME", false, emptyList(), datatype("VARCHAR2"))
        )
    )

    private val element = ProjectExternalObjectType.of("SYS", "JSON_ELEMENT_T", ProjectExternalObjectType.Kind.OBJECT, null, null, emptyList(), emptyList())

    private val objectElement = ProjectExternalObjectType.of(
        "SYS", "JSON_OBJECT_T", ProjectExternalObjectType.Kind.OBJECT, "SYS", "JSON_ELEMENT_T", emptyList(),
        listOf(ProjectExternalTypeMethod.function("GET_SIZE", false, emptyList(), datatype("NUMBER")))
    )

    /** The schema APP complete, with three types of SYS behind their public synonyms. */
    private val metadata: ProjectExternalMetadata = listOf(anyValue, element, objectElement)
        .fold(ProjectExternalMetadata.builder("fixture").completeSchema("APP").completePublicSynonyms()) { builder, type -> publicType(builder, type) }
        .build()

    private val kit = "create package pk_kit as\n" +
        "  function chk(a anydata) return number;\n" +
        "  function chk(a json_element_t) return number;\n" +
        "  function chk(a number) return number;\n" +
        "  function chk(a varchar2) return number;\n" +
        "end;\n/\n" +
        "create type crate_obj as object (n number);\n/\n"

    @Test
    fun valuesOfTheMetadataTypesSelectTheirOverload() {
        val user = "create procedure run_crate is\n" +
            "  v anydata; o json_object_t; x crate_obj; n number;\n" +
            "begin\n" +
            "  n := pk_kit.chk(v);\n" +
            "  n := pk_kit.chk(o);\n" +
            "  n := pk_kit.chk(x);\n" +
            "  n := pk_kit.chk(anydata.convertObject(x));\n" +
            "  n := pk_kit.chk(json_object_t('{}'));\n" +
            "  n := pk_kit.chk(v.getTypeName());\n" +
            "  n := pk_kit.chk(sys.anydata.convertObject(x));\n" +
            "end;\n/\n"
        assertThat(answers(kit, user, "pk_kit.chk")).containsExactly(
            "4:RESOLVED (a anydata)",
            // A subtype goes to the formal of its supertype.
            "5:RESOLVED (a json_element_t)",
            // A project type under no type is none of the metadata's types (PLS-00306).
            "6:NO_COMPATIBLE_CANDIDATE",
            // A static method returns its result type; a constructor builds a value of its type.
            "7:RESOLVED (a anydata)",
            "8:RESOLVED (a json_element_t)",
            // A member method returns its result type.
            "9:RESOLVED (a varchar2)",
            "10:RESOLVED (a anydata)"
        )
    }

    private val odciList = ProjectExternalObjectType.of("SYS", "ODCIVARCHAR2LIST", ProjectExternalObjectType.Kind.COLLECTION, null, null, emptyList(), emptyList())

    private val withList: ProjectExternalMetadata = listOf(anyValue, element, objectElement, odciList)
        .fold(ProjectExternalMetadata.builder("fixture").completeSchema("APP").completePublicSynonyms()) { builder, type -> publicType(builder, type) }
        .build()

    @Test
    fun aFormalWrittenWithItsOwnerIsTheMetadataType() {
        val listKit = "create package pk_list as\n" +
            "  function chk(a anydata) return number;\n" +
            "  function chk(a json_element_t) return number;\n" +
            "  function chk(a number) return number;\n" +
            "  function chk(a sys.odcivarchar2list) return number;\n" +
            "end;\n/\n" +
            "create type crate_obj as object (n number);\n/\n"
        val user = "create procedure run_list is\n" +
            "  v anydata; x crate_obj; l sys.odcivarchar2list; n number;\n" +
            "begin\n" +
            "  n := pk_list.chk(v);\n" +
            "  n := pk_list.chk(l);\n" +
            "  n := pk_list.chk(x);\n" +
            "end;\n/\n"
        assertThat(answers(listKit, user, "pk_list.chk", withList, bindings = true)).containsExactly(
            "4:RESOLVED (a anydata) [COMPATIBLE]",
            "5:RESOLVED (a sys.odcivarchar2list) [COMPATIBLE]",
            // A collection type of the metadata takes no value of another type (PLS-00306).
            "6:NO_COMPATIBLE_CANDIDATE"
        )
    }

    @Test
    fun aCastToAnObjectTypeIsAValueOfIt() {
        val user = "create procedure run_cast is\n" +
            "  n number;\n" +
            "begin\n" +
            "  n := pk_kit.chk(cast(null as json_element_t));\n" +
            "  n := pk_kit.chk(cast(null as crate_obj));\n" +
            "end;\n/\n"
        assertThat(answers(kit, user, "pk_kit.chk")).containsExactly("4:RESOLVED (a json_element_t)", "5:NO_COMPATIBLE_CANDIDATE")
    }

    @Test
    fun withoutMetadataNothingIsDecided() {
        val user = "create procedure run_crate is\n" +
            "  v anydata; n number;\n" +
            "begin\n" +
            "  n := pk_kit.chk(v);\n" +
            "end;\n/\n"
        assertThat(answers(kit, user, "pk_kit.chk", ProjectExternalMetadata.none())).containsExactly("4:AMBIGUOUS")
    }

    private val shapes = ProjectExternalObjectType.of(
        "BASE", "BIN_SHAPE", ProjectExternalObjectType.Kind.OBJECT, null, null,
        listOf(ProjectExternalTypeAttribute.of("QTY", datatype("NUMBER"))), emptyList()
    )

    private val part = ProjectExternalObjectType.of(
        "BASE", "BIN_PART", ProjectExternalObjectType.Kind.OBJECT, "BASE", "BIN_SHAPE",
        listOf(ProjectExternalTypeAttribute.of("TAG", ProjectExternalDatatype.of("VARCHAR2", null, 5, ProjectExternalDatatype.LengthSemantics.CHAR, null, null))),
        emptyList()
    )

    /** A type whose supertype the metadata does not have. */
    private val orphan = ProjectExternalObjectType.of("BASE", "BIN_ORPHAN", ProjectExternalObjectType.Kind.OBJECT, "BASE", "BIN_GONE", emptyList(), emptyList())

    private val bins: ProjectExternalMetadata = listOf(anyValue, shapes, part, orphan)
        .fold(ProjectExternalMetadata.builder("fixture").completeSchema("APP").completePublicSynonyms()) { builder, type -> publicType(builder, type) }
        .build()

    private val bin = "create package pk_bin as\n" +
        "  function put(a anydata) return number;\n" +
        "  function put(a bin_shape) return number;\n" +
        "  function put(a number) return number;\n" +
        "  function put(a varchar2) return number;\n" +
        "end;\n/\n" +
        "create type crate_box under bin_shape (w number);\n/\n"

    @Test
    fun aProjectTypeUnderAMetadataTypeGoesToItsSupertype() {
        val user = "create procedure run_bin is\n" +
            "  b crate_box; p bin_part; n number;\n" +
            "begin\n" +
            "  n := pk_bin.put(b);\n" +
            "  n := pk_bin.put(p);\n" +
            "end;\n/\n"
        assertThat(answers(bin, user, "pk_bin.put", bins)).containsExactly("4:RESOLVED (a bin_shape)", "5:RESOLVED (a bin_shape)")
    }

    @Test
    fun aTypeWhoseSupertypeTheMetadataLacksIsNotProvedIncompatible() {
        val user = "create procedure run_bin is\n" +
            "  o bin_orphan; n number;\n" +
            "begin\n" +
            "  n := pk_bin.put(o);\n" +
            "end;\n/\n"
        // BIN_GONE may be under BIN_SHAPE: that formal is not ruled out. No type is under ANYDATA, which is not an object type.
        assertThat(answers(bin, user, "pk_bin.put", bins, bindings = true)).containsExactly("4:RESOLVED (a bin_shape) [UNKNOWN]")
    }

    @Test
    fun anAttributeOfAMetadataTypeIsTypedEvenWhenInherited() {
        val user = "create procedure run_bin is\n" +
            "  p bin_part; n number;\n" +
            "begin\n" +
            "  n := pk_bin.put(p.qty);\n" +
            "  n := pk_bin.put(p.tag);\n" +
            "end;\n/\n"
        assertThat(answers(bin, user, "pk_bin.put", bins)).containsExactly("4:RESOLVED (a number)", "5:RESOLVED (a varchar2)")
    }
}
