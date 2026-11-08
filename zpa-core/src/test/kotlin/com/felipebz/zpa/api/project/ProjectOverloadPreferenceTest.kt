//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectTypedCallResolution.Status
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Predefined subtypes, schema-object formals and Oracle's preference among compatible overloads, through the public
 * narrowing. Every expected binding is the overload Oracle binds (or the error it raises) for the same overloads and
 * actuals.
 */
@OptIn(ZpaExperimentalApi::class)
class ProjectOverloadPreferenceTest {
    private class Narrowed(val typed: ProjectTypedCallResolution) {
        val status get() = typed.status
        val selected: String? get() = typed.selected.map { c -> c.declaration.parameters.joinToString(", ") { "${it.name} ${it.typeRef.name}" } }.orElse(null)
    }

    private fun narrow(
        spec: String,
        arguments: List<ProjectCallArgument>,
        types: List<ProjectArgumentType>,
        kind: ProjectCallKind = ProjectCallKind.FUNCTION
    ): Narrowed {
        val s = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("p.sql", "create or replace package pk_pick as\n$spec\nend;\n/\n")), false)
        val structural = s.resolveCall(
            ProjectCallTarget.packageMember(ProjectQualifiedName.of("pk_pick"), ProjectIdentifier.fromSource("take")),
            kind, ProjectCallArguments.of(arguments)
        )
        return Narrowed(s.narrowCall(structural, types, ProjectExternalMetadata.none(), null))
    }

    private fun functions(vararg formals: String) = formals.joinToString("\n") { "  function take($it) return number;" }
    private fun procedures(vararg formals: String) = formals.joinToString("\n") { "  procedure take($it);" }
    private fun call(formals: Array<out String>, actual: String) =
        narrow(procedures(*formals), positional(), builtIn(actual), ProjectCallKind.PROCEDURE)
    private fun positional(count: Int = 1) = List(count) { ProjectCallArgument.positional() }
    private fun named(vararg labels: String) = labels.map { ProjectCallArgument.named(ProjectIdentifier.fromSource(it)) }
    private fun builtIn(vararg names: String) = names.map { ProjectArgumentType.builtIn(it) }

    @Test
    fun aPredefinedSubtypeFormalClassifiesThroughItsBase() {
        val spec = functions("p_val number", "p_val timestamp_tz_unconstrained", "p_val dsinterval_unconstrained", "p_val bool")
        // Each actual rejects every formal of another family: only its own survives.
        assertThat(narrow(spec, positional(), builtIn("TIMESTAMP WITH TIME ZONE")).selected).isEqualTo("p_val timestamp_tz_unconstrained")
        assertThat(narrow(spec, positional(), builtIn("INTERVAL DAY TO SECOND")).selected).isEqualTo("p_val dsinterval_unconstrained")
        assertThat(narrow(spec, positional(), builtIn("BOOLEAN")).selected).isEqualTo("p_val bool")
        // The two interval kinds reject each other.
        val intervals = functions("p_val yminterval_unconstrained", "p_val dsinterval_unconstrained")
        assertThat(narrow(intervals, positional(), builtIn("INTERVAL YEAR TO MONTH")).selected).isEqualTo("p_val yminterval_unconstrained")
    }

    @Test
    fun aPackageMemberNamedLikeAPredefinedSubtypeKeepsTheFormalUnknown() {
        assertThat(narrow(functions("p_val bool", "p_val number"), positional(), builtIn("NUMBER")).selected).isEqualTo("p_val number")
        val spec = "  subtype bool is number;\n" + functions("p_val bool", "p_val number")
        assertThat(narrow(spec, positional(), builtIn("NUMBER")).status).isEqualTo(Status.AMBIGUOUS)
    }

    @Test
    fun aSchemaTypeFormalRejectsAScalarActual() {
        val spec = functions("p_val shape_obj_t", "p_val number")
        assertThat(narrow(spec, positional(), builtIn("NUMBER")).selected).isEqualTo("p_val number")
        assertThat(narrow(spec, positional(), builtIn("CHAR")).selected).isEqualTo("p_val number")
        // A NULL or unknown actual proves nothing about it; a composite one only rules out the scalar formal.
        assertThat(narrow(spec, positional(), listOf(ProjectArgumentType.composite())).selected).isEqualTo("p_val shape_obj_t")
        assertThat(narrow(spec, positional(), listOf(ProjectArgumentType.nullLiteral())).status).isEqualTo(Status.AMBIGUOUS)
        assertThat(narrow(spec, positional(), listOf(ProjectArgumentType.unknown())).status).isEqualTo(Status.AMBIGUOUS)
        // A name the package declares is not a schema object: here a VARCHAR2, which a character value prefers.
        val declared = "  subtype shape_obj_t is varchar2(10);\n" + functions("p_val shape_obj_t", "p_val number")
        assertThat(narrow(declared, positional(), builtIn("CHAR")).selected).isEqualTo("p_val shape_obj_t")
        // The cursor type of STANDARD rejects scalars too.
        assertThat(narrow(functions("p_val sys_refcursor", "p_val date"), positional(), builtIn("DATE")).selected).isEqualTo("p_val date")
    }

    @Test
    fun theClosestCompatibleCandidateBinds() {
        // NUMBER over PLS_INTEGER for a NUMBER, PLS_INTEGER for an integer literal's own type, BINARY_DOUBLE for 1d.
        val numbers = functions("p_val number", "p_val pls_integer")
        assertThat(narrow(numbers, positional(), builtIn("NUMBER")).selected).isEqualTo("p_val number")
        assertThat(narrow(numbers, positional(), builtIn("PLS_INTEGER")).selected).isEqualTo("p_val pls_integer")
        val doubles = functions("p_val number", "p_val binary_double")
        assertThat(narrow(doubles, positional(), builtIn("BINARY_DOUBLE")).selected).isEqualTo("p_val binary_double")
        assertThat(narrow(doubles, positional(), builtIn("NUMBER")).selected).isEqualTo("p_val number")
        // A character value prefers VARCHAR2 to CLOB; a date prefers DATE to TIMESTAMP and to VARCHAR2.
        assertThat(narrow(functions("p_val varchar2", "p_val clob"), positional(), builtIn("CHAR")).selected).isEqualTo("p_val varchar2")
        assertThat(narrow(functions("p_val date", "p_val timestamp"), positional(), builtIn("DATE")).selected).isEqualTo("p_val date")
        assertThat(narrow(functions("p_val varchar2", "p_val date"), positional(), builtIn("DATE")).selected).isEqualTo("p_val date")
        // Several arguments, named notation and an omitted default.
        val pairs = functions("p_a number, p_b varchar2", "p_a varchar2, p_b number")
        assertThat(narrow(pairs, positional(2), builtIn("NUMBER", "CHAR")).selected).isEqualTo("p_a number, p_b varchar2")
        val swapped = functions("p_a number, p_b varchar2", "p_b number, p_a varchar2")
        assertThat(narrow(swapped, named("p_b", "p_a"), builtIn("NUMBER", "CHAR")).selected).isEqualTo("p_b number, p_a varchar2")
        val defaulted = functions("p_a number, p_b varchar2 := null", "p_a varchar2")
        assertThat(narrow(defaulted, positional(), builtIn("NUMBER")).selected).isEqualTo("p_a number, p_b varchar2")
        assertThat(narrow(defaulted, positional(), builtIn("NUMBER")).typed.ranked).isTrue
    }

    @Test
    fun aCallOracleCannotDecideStaysAmbiguous() {
        // Ties Oracle reports with PLS-00307: a subtype and its base, VARCHAR2 and CHAR, a defaulted parameter only.
        assertThat(narrow(functions("p_val number", "p_val integer"), positional(), builtIn("NUMBER")).status).isEqualTo(Status.AMBIGUOUS)
        assertThat(narrow(functions("p_val varchar2", "p_val char"), positional(), builtIn("CHAR")).status).isEqualTo(Status.AMBIGUOUS)
        assertThat(narrow(functions("p_val number", "p_val number, p_b varchar2 := null"), positional(), builtIn("NUMBER")).status).isEqualTo(Status.AMBIGUOUS)
        // Each candidate better on a different argument; no candidate of the actual's own type group.
        val crossed = functions("p_a number, p_b varchar2", "p_a varchar2, p_b number")
        assertThat(narrow(crossed, positional(2), builtIn("NUMBER", "NUMBER")).status).isEqualTo(Status.AMBIGUOUS)
        assertThat(narrow(functions("p_val timestamp", "p_val varchar2"), positional(), builtIn("DATE")).status).isEqualTo(Status.AMBIGUOUS)
        // NULL, an actual with no exact type within its family, and an unknown binding in another candidate.
        val numbers = functions("p_val number", "p_val varchar2")
        assertThat(narrow(numbers, positional(), listOf(ProjectArgumentType.nullLiteral())).status).isEqualTo(Status.AMBIGUOUS)
        assertThat(narrow(functions("p_val timestamp", "p_val timestamp with time zone"), positional(), builtIn("TIMESTAMP")).status)
            .isEqualTo(Status.AMBIGUOUS)
        assertThat(narrow(functions("p_val number", "p_val unknown_pkg.some_t"), positional(), builtIn("NUMBER")).status).isEqualTo(Status.AMBIGUOUS)
    }

    @Test
    fun theModeOfTheFormalsDoesNotChangeThePreference() {
        // With a variable as the actual, Oracle binds the same overload as for IN formals.
        assertThat(call(arrayOf("p_val out pls_integer", "p_val out binary_double"), "NUMBER").selected).isEqualTo("p_val binary_double")
        assertThat(call(arrayOf("p_val out number", "p_val out pls_integer"), "NUMBER").selected).isEqualTo("p_val number")
        assertThat(call(arrayOf("p_val in out number", "p_val in out pls_integer"), "PLS_INTEGER").selected).isEqualTo("p_val pls_integer")
        assertThat(call(arrayOf("p_val in out binary_float", "p_val in out binary_double"), "NUMBER").selected).isEqualTo("p_val binary_float")
        assertThat(call(arrayOf("p_val out varchar2", "p_val out clob"), "VARCHAR2").selected).isEqualTo("p_val varchar2")
        assertThat(call(arrayOf("p_val out varchar2", "p_val out clob"), "CLOB").selected).isEqualTo("p_val clob")
        assertThat(call(arrayOf("p_val out date", "p_val out varchar2"), "DATE").selected).isEqualTo("p_val date")
        assertThat(call(arrayOf("p_val in number", "p_val out pls_integer"), "NUMBER").selected).isEqualTo("p_val number")
        // Modes do not tell a subtype from its base either.
        assertThat(call(arrayOf("p_val out number", "p_val out integer"), "NUMBER").status).isEqualTo(Status.AMBIGUOUS)
    }

    @Test
    fun aCandidateWithAnOmittedDefaultRanksByTheFormalItsActualBinds() {
        // The omitted default does not count; the bound formal decides.
        assertThat(narrow(functions("p_a pls_integer", "p_a number, p_b varchar2 := null"), positional(), builtIn("PLS_INTEGER")).selected)
            .isEqualTo("p_a pls_integer")
        assertThat(narrow(functions("p_a number", "p_a pls_integer, p_b varchar2 := null"), positional(), builtIn("PLS_INTEGER")).selected)
            .isEqualTo("p_a pls_integer, p_b varchar2")
        assertThat(narrow(functions("p_a varchar2", "p_a clob, p_b number := 0"), positional(), builtIn("CHAR")).selected).isEqualTo("p_a varchar2")
        assertThat(narrow(functions("p_a binary_double", "p_a number, p_b date := null"), positional(), builtIn("NUMBER")).selected)
            .isEqualTo("p_a number, p_b date")
        // Equal bound formals: the omitted default does not tell them apart (PLS-00307).
        assertThat(narrow(functions("p_a number", "p_a number, p_b date := null"), positional(), builtIn("NUMBER")).status).isEqualTo(Status.AMBIGUOUS)
    }

    @Test
    fun rawRanksWithTheCharacterTypes() {
        // RAW is a subtype of VARCHAR2 in STANDARD.
        assertThat(narrow(functions("p_val raw", "p_val number"), positional(), builtIn("VARCHAR2")).selected).isEqualTo("p_val raw")
        assertThat(narrow(functions("p_val varchar2", "p_val blob"), positional(), builtIn("RAW")).selected).isEqualTo("p_val varchar2")
        assertThat(narrow(functions("p_val raw", "p_val clob"), positional(), builtIn("CHAR")).selected).isEqualTo("p_val raw")
        // RAW and VARCHAR2 formals: PLS-00307 for either actual.
        assertThat(narrow(functions("p_val raw", "p_val varchar2"), positional(), builtIn("RAW")).status).isEqualTo(Status.AMBIGUOUS)
        assertThat(narrow(functions("p_val raw", "p_val varchar2"), positional(), builtIn("VARCHAR2")).status).isEqualTo(Status.AMBIGUOUS)
    }
}
