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
            // A number plus an integer is a number.
            "10:pk_audit.log_value=Project(RESOLVED (p_val number) of 2)",
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

    private val keepOverloads = "create package pk_doc as\n  procedure keep(p doc_t);\n  procedure keep(p varchar2);\nend;\n/\n"

    @Test
    fun aSynonymJsonWrittenInTheSourcesIsReadAsTheMetadataOne() {
        val project = docType + "create synonym json for doc_t;\n" + keepOverloads
        assertThat(keeps(project, ProjectExternalMetadata.none(), DatabaseVersion(11, 2))).isEqualTo(thrice("Project(RESOLVED (p doc_t) of 2)"))
        assertThat(keeps(project, ProjectExternalMetadata.none(), DatabaseVersion(21, 0))).isEqualTo(thrice("Project(NO_COMPATIBLE_CANDIDATE of 2)"))
        assertThat(keeps(project, ProjectExternalMetadata.none(), null)).isEqualTo(thrice("Project(RESOLVED (p doc_t) of 2)"))
    }

    @Test
    fun callsAndTypesFollowTheSynonymsOfTheSources() {
        // A synonym of a package, and a chain of two synonyms that ends at a type: its value goes to that type's
        // overload, not to another object type's.
        val project = docType + keepOverloads + "create type memo_t as object (body varchar2(4000));\n/\n" +
            "create package pk_pick as\n  procedure pick(p doc_t);\n  procedure pick(p memo_t);\nend;\n/\n" +
            "create synonym pk_alias for pk_doc;\ncreate synonym doc_a1 for doc_a2;\ncreate synonym doc_a2 for doc_t;\n"
        val user = "create procedure doc_it is\n  c doc_a1;\nbegin\n  pk_alias.keep('x');\n  pk_pick.pick(c);\nend;\n/\n"
        assertThat(answers(project, user, fromLine = 4).filter { it.contains(".keep=") || it.contains(".pick=") }).containsExactly(
            "4:pk_alias.keep=Project(RESOLVED (p varchar2) of 2)",
            "5:pk_pick.pick=Project(RESOLVED (p doc_t) of 2)"
        )
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

    private val formatters = "create package pk_fmt as\n" +
        "  procedure put(e varchar2);\n  procedure put(e number);\n  procedure put(e date);\n  procedure put(e clob);\n" +
        "  procedure count_of(e number);\n  procedure count_of(e pls_integer);\nend;\n/\n"

    /** The selected overload of each `pk_fmt` call of [calls], one per line from line 9 on. */
    private fun formatted(vararg calls: String, declarations: String = ""): List<String> {
        val user = "create procedure fmt_it is\n  s varchar2(10);\n  n number;\n  d date;\n  c clob;\n  i pls_integer;\n" + declarations +
            "begin\n" + calls.joinToString("") { "  pk_fmt.$it;\n" } + "end;\n/\n"
        val first = 8 + declarations.count { it == '\n' }
        return answers(formatters, user, fromLine = first).filter { it.contains("pk_fmt.") }.map { it.substringAfter('=') }
    }

    private fun put(type: String) = "Project(RESOLVED (e $type) of 4)"

    @Test
    fun functionsOfStandardTypeTheirCalls() {
        assertThat(formatted(
            "put(to_char(n))", "put(to_number(s))", "put(to_clob(s))", "put(upper(s))", "put(substr(c, 1, 2))", "put(add_months(d, 1))",
            "put(trunc(d))", "put(round(n))", "put(nvl(n, 0))", "put(nvl(s, n))", "put(length(s))", "put(months_between(d, d))"
        )).containsExactly(
            put("varchar2"), put("number"), put("clob"), put("varchar2"), put("clob"), put("date"),
            // NVL takes the type of its first argument.
            put("date"), put("number"), put("number"), put("varchar2"), put("number"), put("number")
        )
    }

    @Test
    fun operatorsCastAndCaseTypeTheirResults() {
        assertThat(formatted(
            "put(n + 1)", "put(n * 2)", "put(n / 2)", "put(d + 1)", "put(d - 1)", "put(1 + d)", "put(d - d)", "put(s || n)", "put(c || s)",
            "put(cast(n as varchar2))", "put(case when n > 0 then 'a' else 'b' end)", "put(case when n > 0 then 'a' else n end)",
            "put(extract(year from d))", "put(trim(s))"
        )).containsExactly(
            put("number"), put("number"), put("number"), put("date"), put("date"), put("date"), put("number"), put("varchar2"), put("clob"),
            // A number before a character value among the results of a CASE.
            put("varchar2"), put("varchar2"), put("number"),
            put("number"), put("varchar2")
        )
    }

    @Test
    fun integerResultsPreferThePlsIntegerOverload() {
        val number = "Project(RESOLVED (e number) of 2)"
        val integer = "Project(RESOLVED (e pls_integer) of 2)"
        // LENGTH and INSTR of a VARCHAR2 return a BINARY_INTEGER, of a CLOB an INTEGER; `/` and MOD give a NUMBER.
        assertThat(formatted(
            "count_of(length(s))", "count_of(instr(s, 'a'))", "count_of(length(c))", "count_of(n + 1)", "count_of(round(n))",
            "count_of(i + 1)", "count_of(i / 2)", "count_of(abs(i))", "count_of(nvl(i, n))", "count_of(round(i))", "count_of(trunc(i))",
            "count_of(mod(i, 2))", "count_of(sign(n))", "count_of(ascii(s))", "count_of(coalesce(s, n))", "count_of((i + 1) * 2)", "count_of((n + 1))"
        )).containsExactly(
            integer, integer, number, number, number, integer, number, integer, number, integer, integer,
            number, integer, integer, number, integer, number
        )
    }

    private val tallies = "create package pk_tally as\n  function f(e number) return number;\n  function f(e pls_integer) return number;\n" +
        "  function g(e varchar2) return number;\n  function g(e number) return number;\nend;\n/\n"

    /** The selected overload of each expression of [expressions], called as `x := pk_tally.<call>` and in a SELECT. */
    private fun tallied(vararg expressions: String): List<Pair<String, String>> {
        val user = "create procedure tally_it is\n  s varchar2(10);\n  n number;\n  i pls_integer;\n  c clob;\n  x number;\nbegin\n" +
            expressions.joinToString("") { "  x := pk_tally.$it;\n  select pk_tally.$it into x from dual;\n" } + "end;\n/\n"
        val rows = answers(tallies, user, fromLine = 8).filter { it.contains("pk_tally.") }.map { it.substringAfter('=').substringAfter("RESOLVED ").substringBefore(" of") }
        return rows.chunked(2) { it[0] to it[1] }
    }

    @Test
    fun sqlHasNoPlsIntegerAndRejectsMixedResults() {
        // In SQL every number is a NUMBER, a PLS_INTEGER variable included.
        assertThat(tallied("f(length(s))", "f(instr(s, 'a'))", "f(ascii(s))", "f(sign(n))", "f(abs(i))", "f(i)", "f(i + i)")).containsOnly(
            "(e pls_integer)" to "(e number)"
        )
        assertThat(tallied("f(case when n > 0 then 1 else to_number(s) end)", "g(nvl(s, n))")).containsExactly(
            "(e number)" to "(e number)", "(e varchar2)" to "(e varchar2)"
        )
        // SQL rejects a CASE or a COALESCE of a character value and a number (ORA-00932).
        assertThat(tallied("g(case when n > 0 then 'a' else n end)", "g(coalesce(s, n))")).containsExactly(
            "(e number)" to "Project(AMBIGUOUS", "(e number)" to "Project(AMBIGUOUS"
        )
    }

    @Test
    fun timeZoneFieldsAndClobResults() {
        assertThat(formatted(
            "put(extract(timezone_hour from systimestamp))", "put(extract(timezone_region from systimestamp))",
            "put(case when n > 0 then c else c end)", "put(coalesce(c, c))"
        )).containsExactly(put("number"), put("varchar2"), put("clob"), put("clob"))
    }

    @Test
    fun aLocalFunctionNamedLikeOneOfStandardKeepsItsResult() {
        // The enclosing scope comes before STANDARD.
        val local = "  function upper(x varchar2) return date is begin return sysdate; end;\n"
        assertThat(formatted("put(upper(s))", declarations = local)).containsExactly(put("date"))
        // STANDARD comes before a schema-level function of the same name.
        val schemaLevel = "create function to_char(x number) return date is begin return sysdate; end;\n/\n" + formatters
        val user = "create procedure fmt_it is\n  n number;\nbegin\n  pk_fmt.put(to_char(n));\nend;\n/\n"
        assertThat(answers(schemaLevel, user, fromLine = 4).filter { it.contains("pk_fmt.put=") }.map { it.substringAfter('=') })
            .containsExactly(put("varchar2"))
    }

    @Test
    fun anOperandOfNoTypeLeavesTheExpressionUntyped() {
        assertThat(formatted("put(pk_unknown.v + 1)", "put(nvl(pk_unknown.v, n))", "put(upper(pk_unknown.v))"))
            .containsOnly("Project(AMBIGUOUS of 4)")
    }

    @Test
    fun aCharacterResultCannotChooseBetweenVarchar2AndRaw() {
        // RAW is a subtype of VARCHAR2 in STANDARD: a VARCHAR2 or a RAW value binds neither of the two before the other.
        val project = "create package pk_bytes as\n  procedure put(e varchar2);\n  procedure put(e raw);\n  procedure put(e number);\nend;\n/\n"
        val user = "create procedure bytes_it is\n  n number;\nbegin\n  pk_bytes.put(to_char(n));\n  pk_bytes.put(sys_guid());\n  pk_bytes.put(n + 1);\nend;\n/\n"
        assertThat(answers(project, user, fromLine = 4).filter { it.contains("pk_bytes.put=") }.map { it.substringAfter('=') })
            .containsExactly("Project(AMBIGUOUS of 3)", "Project(AMBIGUOUS of 3)", "Project(RESOLVED (e number) of 3)")
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
