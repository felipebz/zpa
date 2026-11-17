//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectExternalLookup.Status
import com.felipebz.zpa.project.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProjectSourceRelationsTest {
    private val sources = "create table parcel (id number primary key, route varchar2(12) not null);\n" +
        "create table parcel_log as select * from parcel;\n"

    private val depot = ProjectIdentifier.fromSource("DEPOT")
    private fun id(name: String) = ProjectIdentifier.fromSource(name)

    private fun snapshot(text: String = sources): ProjectSnapshot {
        val schemas = ProjectExternalContext.of(ProjectExternalMetadata.none(), depot, ProjectSchemaPaths.none())
        return ProjectAnalysisContext.prepared(
            ProjectIndexPreparation.withSchemas(ProjectDeclarationExtractor(), schemas).prepare(listOf(ProjectSource(FileId("t.sql")) { text }), concurrent = false)
        ).createSnapshot()
    }

    @Test
    fun aTableOfTheSourcesIsFoundWithoutMetadata() {
        val snapshot = snapshot()
        val relation = snapshot.findRelation(depot, id("parcel"), ProjectExternalMetadata.none())
        assertThat(relation.status).isEqualTo(Status.FOUND)
        assertThat(relation.relation.get().isColumnsComplete).isTrue()
        val column = snapshot.findColumn(depot, id("parcel"), id("route"), ProjectExternalMetadata.none())
        assertThat(column.status).isEqualTo(Status.FOUND)
        assertThat(column.column.get().nullable).contains(false)
        assertThat(column.column.get().datatype.get().name).isEqualTo("VARCHAR2(12)")
        // A relation no source creates is never proved absent by the sources.
        assertThat(snapshot.findRelation(depot, id("other"), ProjectExternalMetadata.none()).status).isEqualTo(Status.NOT_COVERED)
    }

    @Test
    fun anUnlistedColumnIsAbsentOnlyFromACompleteTable() {
        val snapshot = snapshot()
        assertThat(snapshot.findColumn(depot, id("parcel"), id("weight"), ProjectExternalMetadata.none()).status).isEqualTo(Status.COLUMN_NOT_FOUND)
        assertThat(snapshot.findColumn(depot, id("parcel_log"), id("weight"), ProjectExternalMetadata.none()).status).isEqualTo(Status.NOT_COVERED)
    }

    @Test
    fun theMetadataRelationComesBeforeTheTableOfTheSources() {
        val metadata = ProjectExternalMetadata.builder("fixture").completeSchema("DEPOT")
            .relation(ProjectExternalRelation.of("DEPOT", "PARCEL", true, listOf(ProjectExternalColumn.of("ID", 1, null), ProjectExternalColumn.of("WEIGHT", 2, null))))
            .build()
        val snapshot = snapshot()
        assertThat(snapshot.findColumn(depot, id("parcel"), id("weight"), metadata).status).isEqualTo(Status.FOUND)
        assertThat(snapshot.findColumn(depot, id("parcel"), id("route"), metadata).status).isEqualTo(Status.COLUMN_NOT_FOUND)
        // The metadata does not have this one: the source table fills it, with the metadata's origin.
        val filled = snapshot.findRelation(depot, id("parcel_log"), metadata)
        assertThat(filled.status).isEqualTo(Status.FOUND)
        assertThat(filled.origin).contains("fixture")
    }

    @Test
    fun theRelationsOfTheSourcesAreListedOnceAsTheLookupReturnsThem() {
        val snapshot = snapshot(sources + "create table parcel (other number);\n")
        assertThat(snapshot.sourceRelations.map { "${it.schema.identifier.lookupName}.${it.name.identifier.lookupName}" })
            .containsExactly("DEPOT.PARCEL", "DEPOT.PARCEL_LOG")
        assertThat(snapshot.sourceRelations.first()).isSameAs(snapshot.findRelation(depot, id("parcel"), ProjectExternalMetadata.none()).relation.get())
        assertThat(snapshot().sourceRelations).hasSize(2)
        assertThat(snapshot("create procedure p is begin null; end;\n/\n").sourceRelations).isEmpty()
    }
}
