//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(com.felipebz.zpa.api.annotations.ZpaExperimentalApi::class)

package com.felipebz.zpa.api.project

import com.felipebz.zpa.project.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** A specification and a body that compile in the same schema are the same package, whichever half writes the owner. */
class WrittenOwnerPairingTest {
    private val members = "  type t is record (f number);\n  g number;\n  cursor c is select 1 x from dual;\n  e exception;\n"

    private fun snapshot(spec: String, body: String, defaultSchema: String? = "APP"): ProjectSnapshot {
        val schemas = if (defaultSchema == null) ProjectExternalContext.none()
        else ProjectExternalContext.of(ProjectExternalMetadata.none(), ProjectIdentifier.fromSource(defaultSchema), ProjectSchemaPaths.none())
        val result = ProjectIndexPreparation.withSchemas(ProjectDeclarationExtractor(), schemas).prepare(
            listOf(
                ProjectSource(FileId("pk.pks")) { "create or replace package $spec is\n$members  procedure p;\nend;\n/\n" },
                ProjectSource(FileId("pk.pkb")) {
                    "create or replace package body $body is\n  procedure p is begin null; end;\n  procedure q is begin p; end;\nend;\n/\n"
                }
            ),
            concurrent = false
        )
        return ProjectAnalysisContext.prepared(result).createSnapshot()
    }

    private val forms = listOf("app.pk_m" to "pk_m", "pk_m" to "app.pk_m", "pk_m" to "pk_m", "app.pk_m" to "app.pk_m")

    private fun correlation(s: ProjectSnapshot, name: String) = s.correlateSpecification(
        s.declarations.filterIsInstance<ProjectPackageSubprogramView>()
            .single { it.role == ProjectDeclarationRole.BODY && it.qualifiedName.segments.last().toString().equals(name, true) }.id
    ).status

    private fun call(s: ProjectSnapshot, vararg name: String) = s.preferring(ProjectIdentifier.fromSource("app")).resolveCall(
        ProjectCallTarget.written(ProjectQualifiedName.of(*name)), ProjectCallKind.PROCEDURE, ProjectCallArguments.of(emptyList())
    )

    @Test
    fun aMembersBodyPairsWithItsSpecificationInBothWritings() {
        for ((spec, body) in forms) {
            val s = snapshot(spec, body)
            assertThat(correlation(s, "p")).describedAs("$spec / $body").isEqualTo(ProjectSpecificationCorrelation.Status.RESOLVED)
            assertThat(correlation(s, "q")).describedAs("$spec / $body").isEqualTo(ProjectSpecificationCorrelation.Status.NOT_FOUND)
        }
    }

    @Test
    fun aCallWithOrWithoutTheSchemaReachesTheSpecificationInBothWritings() {
        for ((spec, body) in forms) {
            val s = snapshot(spec, body)
            for (written in listOf(arrayOf("app", "pk_m", "p"), arrayOf("pk_m", "p"))) {
                val resolution = call(s, *written)
                assertThat(resolution.status).describedAs("$spec / $body: ${written.joinToString(".")}").isEqualTo(ProjectCallResolution.Status.RESOLVED)
                assertThat(resolution.selected.get().declaration.role).isEqualTo(ProjectDeclarationRole.SPECIFICATION)
            }
        }
    }

    /** What the specification's members answer, from a unit of APP, by the package written with and without its schema. */
    private fun memberAnswers(s: ProjectSnapshot): List<String> {
        val app = s.preferring(ProjectIdentifier.fromSource("app"))
        return listOf(arrayOf("pk_m"), arrayOf("app", "pk_m")).flatMap { owner ->
            val type = app.resolveNamedType(ProjectQualifiedName.of(*owner, "t"))
            val values = listOf("g", "c", "e").map { name ->
                val value = app.resolvePackageValue(ProjectQualifiedName.of(*owner, name), null)
                "$name=${value.status}:${value.candidates.map { it.kind }}"
            }
            listOf("t=${type.status}:${type.declaration.map { it.fileId }.orElse("-")}") + values
        }
    }

    @Test
    fun theSpecificationsValuesTypesCursorsAndExceptionsAnswerAsWhenBothHalvesAreWrittenAlike() {
        val alike = memberAnswers(snapshot("pk_m", "pk_m"))
        assertThat(alike).contains("t=RESOLVED:pk.pks", "g=RESOLVED:[PACKAGE_VARIABLE]")
        assertThat(memberAnswers(snapshot("app.pk_m", "app.pk_m"))).isEqualTo(alike)
        assertThat(memberAnswers(snapshot("app.pk_m", "pk_m"))).isEqualTo(alike)
        assertThat(memberAnswers(snapshot("pk_m", "app.pk_m"))).isEqualTo(alike)
    }

    @Test
    fun withoutAnySchemaTheUnownedHalfIsAnotherPackage() {
        // No default schema: `pk_m` has no schema and is not `app.pk_m`.
        for ((spec, body) in listOf("app.pk_m" to "pk_m", "pk_m" to "app.pk_m")) {
            val s = snapshot(spec, body, defaultSchema = null)
            assertThat(correlation(s, "p")).describedAs("$spec / $body").isEqualTo(ProjectSpecificationCorrelation.Status.NOT_FOUND)
            val written = s.resolveCall(
                ProjectCallTarget.written(ProjectQualifiedName.of("app", "pk_m", "p")), ProjectCallKind.PROCEDURE, ProjectCallArguments.of(emptyList())
            )
            assertThat(written.status).describedAs("$spec / $body").isEqualTo(
                if (spec == "app.pk_m") ProjectCallResolution.Status.RESOLVED else ProjectCallResolution.Status.UNKNOWN_PACKAGE
            )
        }
    }
}
