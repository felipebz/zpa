/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.project

import com.felipebz.zpa.project.ProjectCallTestFixtures.context
import com.felipebz.zpa.project.ProjectCallTestFixtures.identifier
import com.felipebz.zpa.project.ProjectCallTestFixtures.index
import com.felipebz.zpa.project.ProjectCallTestFixtures.name
import com.felipebz.zpa.project.ProjectCallTestFixtures.packageSource
import com.felipebz.zpa.project.ProjectPackageProcedureResolution.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The zero-argument procedure adapter over the structural binder. */
class ProjectPackageProcedureResolverTest {
    private fun resolve(context: ProjectAnalysisContext, procedure: String = "setup", owner: String = "p") =
        ProjectPackageProcedureResolver(context).resolve(name(owner), identifier(procedure))

    private fun files(vararg sources: String, failed: Boolean = false, concurrent: Boolean = false) =
        context(sources.mapIndexed { i, text -> "$i.sql" to text }, concurrent, failed)

    /** The retained occurrence at (file, ordinal), to check the adapter returns that very fact. */
    private fun occurrence(context: ProjectAnalysisContext, file: String, ordinal: Int) =
        index(context).occurrencesFor(FileId(file))[ordinal].declaration

    private fun assertResolvedTo(result: ProjectPackageProcedureResolution, context: ProjectAnalysisContext, file: String, ordinal: Int) {
        assertThat(result).isInstanceOf(Resolved::class.java)
        assertThat((result as Resolved).declaration).isSameAs(occurrence(context, file, ordinal))
        assertThat(result.declaration.role).isEqualTo(DeclarationRole.SPECIFICATION)
    }

    @Test
    fun duplicateSpecificationsAreAmbiguousTargetsWithOrWithoutMembers() {
        assertThat(resolve(files(packageSource("PROCEDURE other;"), packageSource("PROCEDURE other;")))).isEqualTo(AmbiguousTarget)
        assertThat(resolve(files(packageSource("PROCEDURE setup;"), packageSource("PROCEDURE setup;")))).isEqualTo(AmbiguousTarget)
    }

    @Test
    fun omittedHeadersAreIncompleteNeverAbsentOrResolved() {
        // The name exists only as a skipped header.
        assertThat(resolve(files(packageSource("PROCEDURE setup(t TABLE) PROCEDURE coverage_recovery;")))).isEqualTo(Incomplete)
        // A retained zero-argument overload plus a skipped one.
        assertThat(resolve(files(packageSource("PROCEDURE setup; PROCEDURE setup(t TABLE) PROCEDURE coverage_recovery;")))).isEqualTo(Incomplete)
        // Quoted skipped header compared by normalized name.
        assertThat(resolve(files(packageSource("PROCEDURE setup; PROCEDURE \"SETUP\"(t TABLE) PROCEDURE coverage_recovery;")))).isEqualTo(Incomplete)
        // A gap on another name is irrelevant.
        val other = files(packageSource("PROCEDURE setup; PROCEDURE other(t TABLE) PROCEDURE coverage_recovery;"))
        assertResolvedTo(resolve(other), other, "0.sql", 1)
    }

    @Test
    fun absenceAndNonCallableNames() {
        assertThat(resolve(files(packageSource("PROCEDURE other;")))).isEqualTo(NotFound)
        assertThat(resolve(files(packageSource("FUNCTION setup RETURN NUMBER;")))).isEqualTo(NotCallable)
        assertThat(resolve(files(packageSource("PROCEDURE setup(a NUMBER);")))).isEqualTo(NotCallable)
        assertThat(resolve(files(packageSource("PROCEDURE setup(a NUMBER); FUNCTION setup RETURN NUMBER;")))).isEqualTo(NotCallable)
    }

    @Test
    fun failedPreparationIsIncompleteEvenWithAUniqueCandidate() {
        assertThat(resolve(files(packageSource("PROCEDURE setup(a NUMBER DEFAULT 1);"), failed = true))).isEqualTo(Incomplete)
        assertThat(resolve(files(packageSource("PROCEDURE other;"), failed = true))).isEqualTo(Incomplete)
    }

    @Test
    fun zeroFormalAndAllDefaultedOverloadsAreAmbiguousWithoutPreference() {
        assertThat(resolve(files(packageSource("PROCEDURE setup; PROCEDURE setup(a NUMBER DEFAULT 1);")))).isEqualTo(AmbiguousCall)
        val zero = files(packageSource("PROCEDURE setup;"))
        assertResolvedTo(resolve(zero), zero, "0.sql", 1)
        val defaulted = files(packageSource("PROCEDURE setup(a NUMBER DEFAULT 1, b VARCHAR2 := 'x'); PROCEDURE setup(c NUMBER);"))
        assertResolvedTo(resolve(defaulted), defaulted, "0.sql", 1)
        // A function overload never competes with a procedure call.
        val mixed = files(packageSource("FUNCTION setup RETURN NUMBER; PROCEDURE setup;"))
        assertResolvedTo(resolve(mixed), mixed, "0.sql", 2)
    }

    @Test
    fun bodiesAreNeverCallableAndAPackageWithoutSpecificationIsUnknown() {
        val body = "CREATE PACKAGE BODY p AS PROCEDURE setup IS BEGIN NULL; END; END p;"
        assertThat(resolve(files(body))).isEqualTo(UnknownTarget)
        assertThat(resolve(files())).isEqualTo(UnknownTarget)
        assertThat(resolve(files(packageSource("PROCEDURE other;")), owner = "q")).isEqualTo(UnknownTarget)
        // Matching specification and body: the specification occurrence is selected, the body defaults are not used.
        val both = files(packageSource("PROCEDURE setup(a NUMBER DEFAULT 1);"),
            "CREATE PACKAGE BODY p AS PROCEDURE setup(a NUMBER) IS BEGIN NULL; END; END p;")
        assertResolvedTo(resolve(both), both, "0.sql", 1)
        // A body-only procedure behind a specification without it is absent from the public surface.
        assertThat(resolve(files(packageSource("PROCEDURE other;"), body))).isEqualTo(NotFound)
    }

    @Test
    fun conditionalCompilationMakesEveryNameIncomplete() {
        val conditional = files("CREATE PACKAGE p AS\n\$IF DBMS_DB_VERSION.VER_LE_12 \$THEN\n PROCEDURE setup;\n\$ELSE\n PROCEDURE setup(a NUMBER);\n\$END\nEND p;")
        assertThat(resolve(conditional)).isEqualTo(Incomplete)
        assertThat(resolve(conditional, "anything")).isEqualTo(Incomplete)
    }

    @Test
    fun notPrepared() {
        assertThat(resolve(ProjectAnalysisContext.NOT_PREPARED)).isEqualTo(NotPrepared)
    }

    @Test
    fun canonicalIdentifierIdentity() {
        val quoted = files("CREATE PACKAGE \"P\" AS PROCEDURE \"SETUP\"; PROCEDURE \"setup\"; END;")
        assertResolvedTo(resolve(quoted), quoted, "0.sql", 1)
        assertResolvedTo(ProjectPackageProcedureResolver(quoted).resolve(name("p"), identifier("\"setup\"")), quoted, "0.sql", 2)
        assertThat(ProjectPackageProcedureResolver(quoted).resolve(name("p"), identifier("\"Setup\""))).isEqualTo(NotFound)
    }

    @Test
    fun serialConcurrentAndReversedPreparationAgreeOnTheSelectedOccurrence() {
        val sources = listOf(
            packageSource("PROCEDURE setup(a NUMBER DEFAULT 1); PROCEDURE setup(b NUMBER); PROCEDURE many; PROCEDURE many(x NUMBER := 1);"),
            "CREATE PACKAGE BODY p AS PROCEDURE setup(a NUMBER) IS BEGIN NULL; END; END p;",
            packageSource("PROCEDURE cleanup;", "q")
        )
        fun summary(context: ProjectAnalysisContext) = listOf("setup" to "p", "many" to "p", "cleanup" to "q", "absent" to "p").map { (procedure, owner) ->
            when (val result = resolve(context, procedure, owner)) {
                is Resolved -> "${result.declaration.fileId.value}#${index(context).occurrencesFor(result.declaration.fileId).indexOfFirst { it.declaration === result.declaration }}"
                else -> result.toString()
            }
        }
        val names = sources.indices.map { "$it.sql" }
        val serial = summary(context(names.zip(sources)))
        assertThat(serial).containsExactly("0.sql#1", "AmbiguousCall", "2.sql#1", "NotFound")
        assertThat(summary(context(names.zip(sources), concurrent = true))).isEqualTo(serial)
        assertThat(summary(context(names.zip(sources).reversed()))).isEqualTo(serial)
    }
}
