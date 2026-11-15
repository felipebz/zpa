//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.api.semantics

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.squid.ConditionalCompilationSettings
import com.felipebz.zpa.squid.DatabaseVersion
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** The decision of the local analysis for each call, answered by the call's name. */
class CallBindingTest {
    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private val project = "create package pk_audit as\n" +
        "  procedure log_value(p_val number);\n  procedure log_value(p_val varchar2);\n  function fmt return varchar2;\nend;\n/\n"

    private val user = "create procedure run_job is\n" +
        "  v_amount number;\n" +
        "  type nums is table of number;\n" +
        "  v_nums nums;\n" +
        "  x varchar2(10);\n" +
        "  procedure note(p number) is begin null; end;\n" +
        "  procedure note(p date) is begin null; end;\n" +
        "begin\n" +
        "  pk_audit.log_value(v_amount);\n" +
        "  pk_audit.log_value(v_amount + 1);\n" +
        "  note(sysdate);\n" +
        "  x := pk_audit.fmt;\n" +
        "  x := v_nums(1);\n" +
        "  x := v_amount;\n" +
        "  select pk_audit.fmt into x from dual;\n" +
        "end;\n/\n"

    private fun render(binding: CallBinding): String = when (binding) {
        is CallBinding.Project -> {
            val resolution = binding.resolution
            val selected = resolution.selected.map { " (" + it.declaration.parameters.joinToString { p -> "${p.name} ${p.typeRef.name}" } + ")" }.orElse("")
            "Project(${resolution.status}$selected of ${resolution.structural.candidates.size})"
        }
        is CallBinding.Local -> "Local(${binding.status} ${binding.selected} of ${binding.candidates})"
        else -> binding.toString()
    }

    private fun answers(
        project: String = this.project, user: String = this.user, fromLine: Int = 9,
        metadata: ProjectExternalMetadata = ProjectExternalMetadata.none(), version: DatabaseVersion? = null
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
                node.getDescendants(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION).filter { it.token.line >= fromLine }.forEach {
                    rows += "${it.token.line}:${it.tokens.joinToString("") { t -> t.originalValue }}=${render(this.context.callBinding(it))}"
                }
            }
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, context, ConditionalCompilationSettings(databaseVersion = version))
            .scanFile(Source("u.sql", user), listOf(check), FileId("u.sql"))
        return rows
    }

    @Test
    fun theDecisionOfEachCall() {
        assertThat(answers()).containsExactly(
            // Two overloads that differ only by a parameter type: the actual's type selects one.
            "9:pk_audit.log_value=Project(RESOLVED (p_val number) of 2)",
            "9:pk_audit=Unavailable(NOT_A_COVERED_NAME)",
            "9:v_amount=Unavailable(NOT_A_COVERED_NAME)",
            // An actual of no known type leaves both.
            "10:pk_audit.log_value=Project(AMBIGUOUS of 2)",
            "10:pk_audit=Unavailable(NOT_A_COVERED_NAME)",
            "10:v_amount=Unavailable(NOT_A_COVERED_NAME)",
            // A local overloaded procedure binds its declaration, among both of its set.
            "11:note=Local(RESOLVED PROCEDURE@7:12 of [PROCEDURE@6:12, PROCEDURE@7:12])",
            "11:sysdate=Unavailable(NOT_A_COVERED_NAME)",
            "12:x=Unavailable(NOT_A_COVERED_NAME)",
            // A function named without parentheses.
            "12:pk_audit.fmt=Project(RESOLVED () of 1)",
            "12:pk_audit=Unavailable(NOT_A_COVERED_NAME)",
            "13:x=Unavailable(NOT_A_COVERED_NAME)",
            // An element of a local collection is kept local.
            "13:v_nums=KeptLocal(LOCAL_CAPTURE)",
            "14:x=Unavailable(NOT_A_COVERED_NAME)",
            "14:v_amount=Unavailable(NOT_A_COVERED_NAME)",
            // A function named without parentheses in SQL that no source takes: Oracle calls it, as in PL/SQL.
            "15:pk_audit.fmt=Project(RESOLVED () of 1)",
            "15:pk_audit=Unavailable(NOT_A_COVERED_NAME)",
            "15:x=Unavailable(NOT_A_COVERED_NAME)"
        )
    }

    @Test
    fun anArgumentThatCannotBePassedToItsParameterRulesTheOverloadOut() {
        // With PLSQL_IMPLICIT_CONVERSION_BOOL at its default, a text cannot be passed to BOOLEAN and TRUE cannot be passed
        // to VARCHAR2: Oracle rules out that overload whatever the other arguments are. NULL, or a call that omits the
        // parameters the overloads differ by, still matches both (PLS-00307).
        val project = "create package pk_cab as\n" +
            "  procedure cab(a varchar2, b varchar2, c varchar2, d varchar2, e number, f varchar2 default null, g number default null);\n" +
            "  procedure cab(a varchar2, b varchar2, c varchar2, d varchar2, e number, h boolean default false);\nend;\n/\n" +
            "create package pk_lin as nr_linha number; end;\n/\n"
        val user = "create procedure p_cab is\n" +
            "begin\n" +
            "  pk_cab.cab('x', 'NUMBER', 'm', 15, 1, 'z');\n" +
            "  pk_cab.cab('x', 'NUMBER', null, 15, pk_lin.nr_linha, '');\n" +
            "  pk_cab.cab('x', 'NUMBER', 'm', 15, 1, true);\n" +
            "  pk_cab.cab('x', 'NUMBER', 'm', 15, 1, null);\n" +
            "  pk_cab.cab('x', 'NUMBER', 'm', 15, 1);\n" +
            "  pk_cab.cab('x', 'NUMBER', 'm', 15, 1, h => true);\n" +
            "end;\n/\n"
        val first = "(a varchar2, b varchar2, c varchar2, d varchar2, e number, f varchar2, g number)"
        val second = "(a varchar2, b varchar2, c varchar2, d varchar2, e number, h boolean)"
        assertThat(answers(project, user, fromLine = 3).filter { it.substringAfter(':').startsWith("pk_cab.cab=") }).containsExactly(
            "3:pk_cab.cab=Project(RESOLVED $first of 2)",
            "4:pk_cab.cab=Project(RESOLVED $first of 2)",
            "5:pk_cab.cab=Project(RESOLVED $second of 2)",
            "6:pk_cab.cab=Project(AMBIGUOUS of 2)",
            "7:pk_cab.cab=Project(AMBIGUOUS of 2)",
            "8:pk_cab.cab=Project(RESOLVED $second of 2)"
        )
    }

    private val shapes = "create type crate as object (n number);\n/\n" +
        "create type shape_t as object (n number) not final;\n/\n" +
        "create type circle_t under shape_t (r number);\n/\n" +
        "create type square_t under shape_t (side number);\n/\n"

    @Test
    fun anObjectTypeArgumentBindsTheOverloadOfItsTypeOrNearestSupertype() {
        val project = shapes + "create package pk_draw as\n" +
            "  procedure tag(o crate, e varchar2);\n  procedure tag(o crate, e number);\n" +
            "  procedure stamp(o in out nocopy crate, path varchar2, e varchar2, base number default 1);\n" +
            "  procedure stamp(o in out nocopy crate, path varchar2, e date, base number default 1);\n" +
            "  procedure fill(e shape_t);\n  procedure fill(e circle_t);\n  procedure fill(e varchar2);\n" +
            "  procedure trace(e shape_t);\n  procedure trace(e square_t);\n" +
            "  procedure label(e shape_t, x varchar2);\n  procedure label(e shape_t, x number);\n" +
            "  procedure pack(e crate);\n  procedure pack(e varchar2);\n  procedure pack(e number);\n" +
            "end;\n/\n"
        val user = "create procedure draw_all is\n" +
            "  c crate; sh shape_t; ci circle_t; u unheard_t;\n" +
            "begin\n" +
            "  pk_draw.tag(c, 'y');\n" +
            "  pk_draw.stamp(c, 'a', 'y');\n" +
            "  pk_draw.stamp(c, 'a', date '2020-01-01');\n" +
            "  pk_draw.fill(sh);\n" +
            "  pk_draw.trace(ci);\n" +
            "  pk_draw.fill(ci);\n" +
            "  pk_draw.fill(circle_t(1, 2));\n" +
            "  pk_draw.label(sh, 'y');\n" +
            "  pk_draw.pack(new crate(1));\n" +
            "  pk_draw.fill(u);\n" +
            "end;\n/\n"
        assertThat(answers(project, user, fromLine = 4).filter { it.substringAfter(':').startsWith("pk_draw.") }).containsExactly(
            // The same object type in every candidate is an exact match: the other actual chooses.
            "4:pk_draw.tag=Project(RESOLVED (o crate, e varchar2) of 2)",
            "5:pk_draw.stamp=Project(RESOLVED (o crate, path varchar2, e varchar2, base number) of 2)",
            "6:pk_draw.stamp=Project(RESOLVED (o crate, path varchar2, e date, base number) of 2)",
            // A supertype value cannot go to a subtype formal, nor a subtype to a sibling's.
            "7:pk_draw.fill=Project(RESOLVED (e shape_t) of 3)",
            "8:pk_draw.trace=Project(RESOLVED (e shape_t) of 2)",
            // Among the formals that take it, the nearest type in the hierarchy.
            "9:pk_draw.fill=Project(RESOLVED (e circle_t) of 3)",
            "10:pk_draw.fill=Project(RESOLVED (e circle_t) of 3)",
            "11:pk_draw.label=Project(RESOLVED (e shape_t, x varchar2) of 2)",
            // NEW t(…) is the constructor call t(…).
            "12:pk_draw.pack=Project(RESOLVED (e crate) of 3)",
            // A type the project does not declare: a synonym or an unknown relation could still connect the names.
            "13:pk_draw.fill=Project(AMBIGUOUS of 3)"
        )
    }

    @Test
    fun theJsonConstructorBindsTheJsonOverload() {
        val overloads = "create package pk_doc as\n  procedure put(p json);\n  procedure put(p varchar2);\nend;\n/\n"
        val json = "3:pk_doc.put=Project(RESOLVED (p json) of 2)"
        // The predefined JSON type.
        val builtIn = "create procedure doc_it is\nbegin\n  pk_doc.put(json('{}'));\nend;\n/\n"
        assertThat(answers(overloads, builtIn, fromLine = 3).filter { it.contains("pk_doc.put=") }).containsExactly(json)
        // A project type named json, and a synonym json for a project type: JSON(…) and NEW JSON(…) are its constructor
        // where JSON is not a predefined type, and a `json` formal names that type.
        val user = "create procedure doc_it is\nbegin\n  pk_doc.put(json('{}'));\n  pk_doc.put(new json('{}'));\nend;\n/\n"
        val projects = listOf(
            "create type json as object (body varchar2(4000));\n/\n" + overloads,
            "create type doc_t as object (body varchar2(4000));\n/\ncreate synonym json for doc_t;\n" + overloads
        )
        for (project in projects) {
            assertThat(answers(project, user, fromLine = 3).filter { it.contains("pk_doc.put=") })
                .containsExactly(json, "4:pk_doc.put=Project(RESOLVED (p json) of 2)")
        }
    }

    private val docType = "create type doc_t as object (body varchar2(4000));\n/\n"

    private val jsonSynonym = ProjectExternalMetadata.builder("fixture").completeSchema("APP").completePublicSynonyms()
        .synonym(ProjectExternalSynonym.of("PUBLIC", "JSON", "APP", "DOC_T", null))
        .build()

    private val jsonCalls = "create procedure doc_it is\n  j json;\nbegin\n" +
        "  pk_doc.keep(j);\n  pk_doc.keep(json('{}'));\n  pk_doc.keep(new json('{}'));\nend;\n/\n"

    /** The answer for each of the three calls of [jsonCalls]. */
    private fun keeps(project: String, metadata: ProjectExternalMetadata, version: DatabaseVersion?) =
        answers(project, jsonCalls, fromLine = 4, metadata = metadata, version = version).filter { it.contains("pk_doc.keep=") }.map { it.substringAfter('=') }

    private fun thrice(answer: String) = listOf(answer, answer, answer)

    @Test
    fun aWrittenJsonNamesTheSchemaSynonymBeforeThePredefinedType() {
        val project = docType + "create package pk_doc as\n  procedure keep(p doc_t);\n  procedure keep(p varchar2);\nend;\n/\n"
        // Before 21 `json` is the synonym's type, in the declaration and in JSON(…) and NEW JSON(…).
        assertThat(keeps(project, jsonSynonym, DatabaseVersion(19, 0))).isEqualTo(thrice("Project(RESOLVED (p doc_t) of 2)"))
        // From 21 on it is the predefined JSON type, which neither formal takes.
        assertThat(keeps(project, jsonSynonym, DatabaseVersion(21, 0))).isEqualTo(thrice("Project(NO_COMPATIBLE_CANDIDATE of 2)"))
        // Without the version, doc_t is not ruled out; varchar2 is, in both readings.
        assertThat(keeps(project, jsonSynonym, null)).isEqualTo(thrice("Project(RESOLVED (p doc_t) of 2)"))
        // Without a type or synonym json, `json` is the predefined type whatever the version.
        assertThat(keeps(project, ProjectExternalMetadata.none(), DatabaseVersion(19, 0))).isEqualTo(thrice("Project(NO_COMPATIBLE_CANDIDATE of 2)"))
    }

    @Test
    fun aWrittenJsonNamesTheProjectTypeJsonBeforeThePredefinedType() {
        val project = "create type json as object (body varchar2(4000));\n/\n" +
            "create package pk_doc as\n  procedure keep(p json);\n  procedure keep(p varchar2);\nend;\n/\n"
        for (version in listOf(DatabaseVersion(11, 2), DatabaseVersion(21, 0), null)) {
            assertThat(keeps(project, ProjectExternalMetadata.none(), version)).describedAs("$version").isEqualTo(thrice("Project(RESOLVED (p json) of 2)"))
        }
    }

    @Test
    fun aJsonFormalTakesAJsonActualInEveryReading() {
        val project = docType + "create package pk_doc as\n  procedure keep(p json);\n  procedure keep(p varchar2);\nend;\n/\n"
        for (version in listOf(DatabaseVersion(11, 2), DatabaseVersion(21, 0), null)) {
            assertThat(keeps(project, jsonSynonym, version)).describedAs("$version").isEqualTo(thrice("Project(RESOLVED (p json) of 2)"))
        }
    }

    private val readings = listOf(DatabaseVersion(11, 2), DatabaseVersion(21, 0), null)

    @Test
    fun aJsonFormalRejectsScalarActualsInEveryReading() {
        val project = docType + "create package pk_doc as\n  procedure put(p json);\n  procedure put(p varchar2);\nend;\n/\n"
        val user = "create procedure doc_it is\n  s varchar2(10);\nbegin\n  pk_doc.put('x');\n  pk_doc.put(s);\n  pk_doc.put(1);\nend;\n/\n"
        for (version in readings) {
            assertThat(answers(project, user, fromLine = 4, metadata = jsonSynonym, version = version).filter { it.contains("pk_doc.put=") }.map { it.substringAfter('=') })
                .describedAs("$version").isEqualTo(thrice("Project(RESOLVED (p varchar2) of 2)"))
        }
    }

    @Test
    fun aJsonFormalOfAMemberMethodRejectsScalarActualsInEveryReading() {
        // The method's result tells which overload took the call: keep(p varchar2) only for render(p varchar2).
        val project = docType +
            "create type doc_box as object (n number,\n  member function render(p json) return doc_t,\n  member function render(p varchar2) return varchar2);\n/\n" +
            "create package pk_doc as\n  procedure keep(p doc_t);\n  procedure keep(p varchar2);\nend;\n/\n"
        val user = "create procedure doc_it is\n  b doc_box;\nbegin\n  pk_doc.keep(b.render('x'));\nend;\n/\n"
        for (version in readings) {
            assertThat(answers(project, user, fromLine = 4, metadata = jsonSynonym, version = version).filter { it.contains("pk_doc.keep=") })
                .describedAs("$version").containsExactly("4:pk_doc.keep=Project(RESOLVED (p varchar2) of 2)")
        }
    }

    @Test
    fun aLocalOverloadSetOrdersObjectTypesTheSameWay() {
        val user = "create procedure draw_local is\n" +
            "  ci circle_t;\n" +
            "  procedure paint(e shape_t) is begin null; end;\n" +
            "  procedure paint(e circle_t) is begin null; end;\n" +
            "begin\n" +
            "  paint(ci);\n" +
            "end;\n/\n"
        assertThat(answers(shapes, user, fromLine = 6).filter { it.startsWith("6:paint=") })
            .containsExactly("6:paint=Local(RESOLVED PROCEDURE@4:12 of [PROCEDURE@3:12, PROCEDURE@4:12])")
    }

    @Test
    fun aFileThatWasNotAnalyzed() {
        val context = PlSqlVisitorContext(PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(user), null, null)
        val name = context.rootTree()!!.getDescendants(PlSqlGrammar.MEMBER_EXPRESSION).first()
        assertThat(context.callBinding(name)).isEqualTo(CallBinding.Unavailable(UsageUnavailability.FILE_NOT_ANALYZED))
    }
}
