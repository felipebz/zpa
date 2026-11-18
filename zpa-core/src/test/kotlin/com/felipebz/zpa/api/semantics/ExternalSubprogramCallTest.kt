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
import com.felipebz.zpa.api.project.ProjectExternalMethodParameter.Mode
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.squid.ConditionalCompilationSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** Actuals that call a subprogram only the external metadata describes. */
class ExternalSubprogramCallTest {
    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private fun answers(project: String, user: String, metadata: ProjectExternalMetadata = this.metadata): List<String> {
        val sources = mapOf("project.sql" to project, "u.sql" to user)
        val external = ProjectExternalContext.of(metadata, ProjectIdentifier.fromSource("APP"), ProjectSchemaPaths.none())
        val context = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation.withSchemas(ProjectDeclarationExtractor(), external)
                .prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
        )
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.MEMBER_EXPRESSION).filter { it.tokens.joinToString("") { t -> t.originalValue } == "pk_kit.chk" }.forEach {
                    rows += "${it.token.line}:" + when (val binding = this.context.callBinding(it)) {
                        is CallBinding.Project -> {
                            val resolution = binding.resolution
                            resolution.status.toString() +
                                resolution.selected.map { s -> " (" + s.declaration.parameters.joinToString { p -> "${p.name} ${p.typeRef.name}" } + ")" }.orElse("")
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
    private fun parameter(name: String, type: String, defaulted: Boolean? = false, packageType: String? = null, owner: String? = null) =
        ProjectExternalSubprogramParameter.of(name, Mode.IN, datatype(type, owner), packageType, defaulted)

    /** The packages of SYS behind public synonyms, with the schema APP complete. */
    private fun metadata(vararg subprograms: ProjectExternalSubprogram): ProjectExternalMetadata {
        val builder = ProjectExternalMetadata.builder("fixture").completeSchema("APP").completePublicSynonyms()
        subprograms.map { it.packageName.get().originalSpelling.trim('"') }.distinct().forEach {
            builder.synonym(ProjectExternalSynonym.of("PUBLIC", it, "SYS", it, null))
        }
        subprograms.forEach { builder.subprogram(it) }
        return builder.build()
    }

    private val lobs = listOf(
        ProjectExternalSubprogram.function("SYS", "BIN_LOB", "SIZE_OF", 1, listOf(parameter("LOC", "BLOB")), datatype("INTEGER")),
        ProjectExternalSubprogram.function("SYS", "BIN_LOB", "SIZE_OF", 2, listOf(parameter("LOC", "CLOB")), datatype("INTEGER")),
        ProjectExternalSubprogram.function("SYS", "BIN_LOB", "PART", 1, listOf(parameter("LOC", "BLOB"), parameter("AMT", "INTEGER", true), parameter("OFF", "INTEGER", true)), datatype("RAW")),
        ProjectExternalSubprogram.function("SYS", "BIN_LOB", "PART", 2, listOf(parameter("LOC", "CLOB"), parameter("AMT", "INTEGER", true), parameter("OFF", "INTEGER", true)), datatype("VARCHAR2")),
        ProjectExternalSubprogram.function("SYS", "BIN_UTIL", "ERROR_TEXT", null, emptyList(), datatype("VARCHAR2")),
        // A default the dictionary does not state.
        ProjectExternalSubprogram.function("SYS", "BIN_UTIL", "PADDED", 1, listOf(parameter("P", "VARCHAR2"), parameter("W", "NUMBER", null)), datatype("VARCHAR2")),
        ProjectExternalSubprogram.function("SYS", "BIN_UTIL", "PADDED", 2, listOf(parameter("P", "VARCHAR2"), parameter("W", "NUMBER"), parameter("F", "VARCHAR2")), datatype("NUMBER")),
        ProjectExternalSubprogram.function("SYS", "BIN_UTIL", "COUNT_OF", 1, listOf(parameter("T", "BIN_UTIL", packageType = "LIST_T", owner = "SYS")), datatype("NUMBER")),
        ProjectExternalSubprogram.function("SYS", "BIN_UTIL", "COUNT_OF", 2, listOf(parameter("T", "VARCHAR2")), datatype("VARCHAR2"))
    )

    private val metadata = metadata(*lobs.toTypedArray())

    private val kit = "create package pk_kit as\n" +
        "  function chk(a number) return number;\n" +
        "  function chk(a varchar2) return number;\n" +
        "  function chk(a date) return number;\n" +
        "end;\n/\n"

    @Test
    fun theResultOfASubprogramOfTheMetadataSelectsTheOverload() {
        val user = "create procedure run_kit(c clob, b blob) is\n" +
            "  n number;\n" +
            "begin\n" +
            "  n := pk_kit.chk(bin_lob.size_of(c));\n" +
            "  n := pk_kit.chk(bin_lob.part(c, 10, 1));\n" +
            "  n := pk_kit.chk(bin_lob.part(b, 10, 1));\n" +
            "  n := pk_kit.chk(bin_util.error_text);\n" +
            "  n := pk_kit.chk(sys.bin_lob.size_of(b));\n" +
            "end;\n/\n"
        assertThat(answers(kit, user)).containsExactly(
            // Every SIZE_OF returns INTEGER.
            "4:RESOLVED (a number)",
            "5:RESOLVED (a varchar2)",
            // RAW converts to VARCHAR2.
            "6:RESOLVED (a varchar2)",
            "7:RESOLVED (a varchar2)",
            "8:RESOLVED (a number)"
        )
    }

    @Test
    fun withoutMetadataNothingChanges() {
        val user = "create procedure run_kit(c clob) is\n  n number;\nbegin\n  n := pk_kit.chk(bin_lob.size_of(c));\nend;\n/\n"
        assertThat(answers(kit, user, ProjectExternalMetadata.none())).containsExactly("4:AMBIGUOUS")
    }

    @Test
    fun aPackageOfTheProjectWinsOverTheMetadata() {
        val project = kit + "create package bin_lob as\n  function size_of(c clob) return date;\nend;\n/\n"
        val user = "create procedure run_kit(c clob) is\n  n number;\nbegin\n  n := pk_kit.chk(bin_lob.size_of(c));\nend;\n/\n"
        assertThat(answers(project, user)).containsExactly("4:RESOLVED (a date)")
    }

    /** A complete schema BIN_TOOL with a standalone function, and APP complete or not. */
    private fun toolMetadata(appComplete: Boolean): ProjectExternalMetadata {
        val builder = ProjectExternalMetadata.builder("fixture").completeSchema("BIN_TOOL").completePublicSynonyms()
        if (appComplete) builder.completeSchema("APP")
        return builder.subprogram(ProjectExternalSubprogram.function("BIN_TOOL", null, "SIZE_OF", null, listOf(parameter("C", "CLOB")), datatype("DATE"))).build()
    }

    private val toolCall = "create procedure run_kit is\n  n number;\nbegin\n  n := pk_kit.chk(bin_tool.size_of(null));\nend;\n/\n"

    @Test
    fun theFirstSegmentIsASchemaOnlyWhenNoObjectTakesIt() {
        assertThat(answers(kit, toolCall, toolMetadata(appComplete = true))).containsExactly("4:RESOLVED (a date)")
        // A package of the project takes the name.
        val project = kit + "create package bin_tool as\n  function size_of(c clob) return number;\n  function size_of(c varchar2) return varchar2;\nend;\n/\n"
        assertThat(answers(project, toolCall, toolMetadata(appComplete = true))).containsExactly("4:AMBIGUOUS")
        // APP may hold an object of that name.
        assertThat(answers(kit, toolCall, toolMetadata(appComplete = false))).containsExactly("4:AMBIGUOUS")
    }

    @Test
    fun aThreeSegmentNameWhoseFirstSegmentIsAPackageOfTheProject() {
        val metadata = ProjectExternalMetadata.builder("fixture").completeSchema("APP").completeSchema("BIN_HOLD").completePublicSynonyms()
            .subprogram(ProjectExternalSubprogram.function("BIN_HOLD", "BIN_LOB", "SIZE_OF", null, listOf(parameter("LOC", "CLOB")), datatype("INTEGER")))
            .build()
        val user = "create procedure run_kit(c clob) is\n  n number;\nbegin\n  n := pk_kit.chk(bin_hold.bin_lob.size_of(c));\nend;\n/\n"
        assertThat(answers(kit, user, metadata)).containsExactly("4:RESOLVED (a number)")
        val project = kit + "create package bin_hold as\n  bin_lob varchar2(10);\nend;\n/\n"
        assertThat(answers(project, user, metadata)).containsExactly("4:AMBIGUOUS")
    }

    @Test
    fun twoResultsOfPackageTypesOfOnePackageAreNotTheSameType() {
        val metadata = metadata(
            ProjectExternalSubprogram.function("SYS", "BIN_MAKE", "MAKE", 1, listOf(parameter("P", "VARCHAR2", true)), datatype("BIN_MAKE", "SYS"), "A_T"),
            ProjectExternalSubprogram.function("SYS", "BIN_MAKE", "MAKE", 2, listOf(parameter("Q", "NUMBER", true)), datatype("BIN_MAKE", "SYS"), "B_T")
        )
        val user = "create procedure run_kit is\n  n number;\nbegin\n  n := pk_kit.chk(bin_make.make);\nend;\n/\n"
        assertThat(answers(kit, user, metadata)).containsExactly("4:AMBIGUOUS")
    }

    @Test
    fun aParameterOfAPackageTypeOfTheMetadataIsNeverDecided() {
        val user = "create procedure run_kit is\n" +
            "  l sys.bin_util.list_t; o pk_other.list_t; n number;\n" +
            "begin\n" +
            "  n := pk_kit.chk(bin_util.count_of(l));\n" +
            "  n := pk_kit.chk(bin_util.count_of(o));\n" +
            "end;\n/\n"
        assertThat(answers(kit, user)).containsExactly("4:AMBIGUOUS", "5:AMBIGUOUS")
    }

    @Test
    fun anOmittedParameterOfUnknownDefaultProvesNothing() {
        val user = "create procedure run_kit(t varchar2) is\n  n number;\nbegin\n  n := pk_kit.chk(bin_util.padded(t));\n" +
            "  n := pk_kit.chk(bin_util.padded(t, 1, 'x'));\nend;\n/\n"
        // PADDED(t) may bind the first overload only, whose default is unknown: its VARCHAR2 is the only result left.
        assertThat(answers(kit, user)).containsExactly("4:RESOLVED (a varchar2)", "5:RESOLVED (a number)")
    }
}
