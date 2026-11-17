//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.project.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProjectSourceSynonymsTest {
    private val sources = "create synonym crate_s for crate_log;\n" +
        "create public synonym crate_p for depot.crate_hist;\n"

    private val depot = ProjectIdentifier.fromSource("DEPOT")
    private val public = ProjectIdentifier.fromSource("PUBLIC")
    private fun id(name: String) = ProjectIdentifier.fromSource(name)
    private fun none() = ProjectExternalMetadata.none()

    private fun snapshot(text: String = sources): ProjectSnapshot {
        val schemas = ProjectExternalContext.of(ProjectExternalMetadata.none(), depot, ProjectSchemaPaths.none())
        return ProjectAnalysisContext.prepared(
            ProjectIndexPreparation.withSchemas(ProjectDeclarationExtractor(), schemas).prepare(listOf(ProjectSource(FileId("s.sql")) { text }), concurrent = false)
        ).createSnapshot()
    }

    private fun target(synonym: ProjectExternalSynonym) =
        synonym.targetOwner.map { "${it.identifier.lookupName}." }.orElse("") + synonym.targetName.identifier.lookupName

    @Test
    fun theSynonymsOfTheSourcesAreFoundWithoutMetadata() {
        val snapshot = snapshot()
        val private = snapshot.findSynonym(depot, id("crate_s"), none())
        assertThat(private).isPresent
        assertThat(target(private.get())).isEqualTo("DEPOT.CRATE_LOG")
        assertThat(private.get().isPublic).isFalse()
        val publicSynonym = snapshot.findSynonym(public, id("crate_p"), none())
        assertThat(publicSynonym).isPresent
        assertThat(publicSynonym.get().isPublic).isTrue()
        assertThat(target(publicSynonym.get())).isEqualTo("DEPOT.CRATE_HIST")
        assertThat(snapshot.findSynonym(depot, id("crate_x"), none())).isEmpty
    }

    @Test
    fun aSynonymOfTheSourcesComesBeforeTheMetadata() {
        val metadata = ProjectExternalMetadata.builder("fixture").completeSchema("DEPOT")
            .synonym(ProjectExternalSynonym.of("DEPOT", "CRATE_S", "DEPOT", "CRATE_OLD", null))
            .synonym(ProjectExternalSynonym.of("DEPOT", "CRATE_M", "DEPOT", "CRATE_BIN", null))
            .build()
        val snapshot = snapshot()
        assertThat(target(snapshot.findSynonym(depot, id("crate_s"), metadata).get())).isEqualTo("DEPOT.CRATE_LOG")
        // Only the metadata has this one.
        assertThat(target(snapshot.findSynonym(depot, id("crate_m"), metadata).get())).isEqualTo("DEPOT.CRATE_BIN")
        assertThat(snapshot("create procedure p is begin null; end;\n/\n").findSynonym(depot, id("crate_m"), metadata).map(::target))
            .contains("DEPOT.CRATE_BIN")
    }

    @Test
    fun theSynonymsOfTheSourcesAreListedOnceAsTheLookupReturnsThem() {
        val snapshot = snapshot()
        val listed = snapshot.sourceSynonyms
        assertThat(listed.map { "${it.owner.identifier.lookupName}.${it.name.identifier.lookupName}" }).containsExactlyInAnyOrder("DEPOT.CRATE_S", "PUBLIC.CRATE_P")
        val metadata = ProjectExternalMetadata.builder("fixture").synonym(ProjectExternalSynonym.of("DEPOT", "CRATE_S", "DEPOT", "CRATE_OLD", null)).build()
        for (synonym in listed) {
            assertThat(snapshot.findSynonym(synonym.owner, synonym.name, none()).get()).isSameAs(synonym)
            assertThat(snapshot.findSynonym(synonym.owner, synonym.name, metadata).get()).isSameAs(synonym)
        }
        assertThat(snapshot("create procedure p is begin null; end;\n/\n").sourceSynonyms).isEmpty()
    }
}
