//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(com.felipebz.zpa.api.annotations.ZpaExperimentalApi::class)

package com.felipebz.zpa.api.project

import com.felipebz.zpa.project.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** A snapshot seen from a unit's schema: every public lookup reads an unqualified name there first. */
class ProjectSnapshotSchemaPreferenceTest {
    private val base = "create type base_t as object (id number);\n/\n"
    private val pk = "create package pk_dup as\n  g number;\n  procedure run;\nend;\n/\n"

    private fun snapshot(vararg sources: Pair<String, String>): ProjectSnapshot {
        val schemas = ProjectExternalContext.of(ProjectExternalMetadata.none(), null, ProjectSchemaPaths.parse(null, "a/**=APP_A,b/**=APP_B"))
        val result = ProjectIndexPreparation.withSchemas(ProjectDeclarationExtractor(), schemas)
            .prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
        return ProjectAnalysisContext.prepared(result).createSnapshot()
    }

    private val project = snapshot("a/base.sql" to base, "b/base.sql" to base, "a/pk.sql" to pk, "b/pk.sql" to pk)
    private val appB = project.preferring(ProjectIdentifier.fromSource("app_b"))

    private fun call(s: ProjectSnapshot, vararg name: String) = s.resolveCall(
        ProjectCallTarget.written(ProjectQualifiedName.of(*name)), ProjectCallKind.PROCEDURE, ProjectCallArguments.of(emptyList())
    )

    @Test
    fun theUnitsSchemaDecidesBetweenDeclarationsOfTheSameName() {
        val type = appB.resolveNamedType(ProjectQualifiedName.of("base_t"))
        assertThat(type.status).isEqualTo(ProjectNamedTypeResolution.Status.RESOLVED)
        assertThat(type.declaration.get().fileId).isEqualTo("b/base.sql")
        val run = call(appB, "pk_dup", "run")
        assertThat(run.status).isEqualTo(ProjectCallResolution.Status.RESOLVED)
        assertThat(run.selected.get().declaration.fileId).isEqualTo("b/pk.sql")

        val described = appB.describeType(ProjectQualifiedName.of("base_t"), null)
        assertThat(described.status).isEqualTo(ProjectTypeDescription.Status.DECLARED_TYPE)
        assertThat(described.declaration.get().fileId).isEqualTo("b/base.sql")
        val value = appB.resolvePackageValue(ProjectQualifiedName.of("pk_dup", "g"), null)
        assertThat(value.status).isEqualTo(ProjectValueResolution.Status.RESOLVED)
        assertThat(value.candidates.single().fileId).isEqualTo("b/pk.sql")
    }

    @Test
    fun withoutAPreferenceBothStayAmbiguous() {
        assertThat(project.resolveNamedType(ProjectQualifiedName.of("base_t")).status).isEqualTo(ProjectNamedTypeResolution.Status.AMBIGUOUS)
        assertThat(call(project, "pk_dup", "run").status).isEqualTo(ProjectCallResolution.Status.AMBIGUOUS_TARGET)
        assertThat(project.resolvePackageValue(ProjectQualifiedName.of("pk_dup", "g"), null).status).isEqualTo(ProjectValueResolution.Status.AMBIGUOUS)
    }

    @Test
    fun aWrittenOwnerResolvesAsBefore() {
        for (s in listOf(project, appB)) {
            assertThat(s.resolveNamedType(ProjectQualifiedName.of("app_b", "base_t")).declaration.get().fileId).isEqualTo("b/base.sql")
            assertThat(call(s, "app_a", "pk_dup", "run").selected.get().declaration.fileId).isEqualTo("a/pk.sql")
        }
    }

    @Test
    fun aNullSchemaIsThisSnapshot() {
        assertThat(project.preferring(null)).isSameAs(project)
        assertThat(appB.preferring(null)).isSameAs(appB)
    }

    @Test
    fun omittedStandaloneCallablesAreListed() {
        // A heading the extractor identifies but cannot retain as a declaration.
        val s = snapshot("a/broken.sql" to "create procedure broken_proc(p in out nocopy) is\nbegin\n  null;\nend;\n/\n", "a/base.sql" to base)
        assertThat(s.omittedStandaloneCallables).containsExactly(ProjectQualifiedName.of("broken_proc"))
    }
}
