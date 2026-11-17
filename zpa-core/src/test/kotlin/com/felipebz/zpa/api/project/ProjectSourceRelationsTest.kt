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

    @Test
    fun addingAConstraintAddsNoColumn() {
        val script = "create table bin_slot (bin_id integer, slot_no integer, owner varchar2(30));\n" +
            "alter table bin_slot add constraint bin_slot_pk primary key (bin_id, slot_no);\n" +
            "alter table bin_slot add constraint bin_slot_fk foreign key (bin_id) references bin (id);\n" +
            "alter table bin_slot add (constraint bin_slot_uk unique (owner));\n" +
            "alter table bin_slot add check (slot_no > 0);\n" +
            "alter table bin_slot add primary key (bin_id);\n" +
            "alter table bin_slot add unique (owner);\n" +
            "alter table bin_slot add foreign key (bin_id) references bin (id);\n" +
            "alter table bin_slot add (check (slot_no < 10), unique (slot_no));\n"
        val snapshot = snapshot(script)
        val relation = snapshot.findRelation(depot, id("bin_slot"), ProjectExternalMetadata.none()).relation.get()
        assertThat(relation.isColumnsComplete).isTrue()
        assertThat(relation.columns.map { it.name.identifier.lookupName }).containsExactly("BIN_ID", "SLOT_NO", "OWNER")
        assertThat(snapshot.findColumn(depot, id("bin_slot"), id("label"), ProjectExternalMetadata.none()).status).isEqualTo(Status.COLUMN_NOT_FOUND)
        // A column added among constraints: the table has columns the CREATE TABLE does not list.
        val mixed = snapshot(script + "alter table bin_slot add (extra number, constraint bin_slot_ck check (extra > 0));\n")
        assertThat(mixed.findRelation(depot, id("bin_slot"), ProjectExternalMetadata.none()).relation.get().isColumnsComplete).isFalse()
        assertThat(mixed.findColumn(depot, id("bin_slot"), id("label"), ProjectExternalMetadata.none()).status).isEqualTo(Status.NOT_COVERED)
    }

    @Test
    fun aQuotedNameThatSpellsAKeywordIsAColumn() {
        val snapshot = snapshot(
            "create table crate_mark (\"CHECK\" number, qty number);\n" +
                "create table crate_flag (qty number, \"PRIMARY\" varchar2(3), \"CONSTRAINT\" number);\n" +
                "create table crate_low (\"check\" number, qty number);\n" +
                "create table crate_hold (qty number);\n" +
                "alter table crate_hold add (\"CHECK\" number);\n"
        )
        fun relation(name: String) = snapshot.findRelation(depot, id(name), ProjectExternalMetadata.none()).relation.get()
        fun columns(name: String) = relation(name).columns.map { it.name.identifier.lookupName }
        assertThat(columns("crate_mark")).containsExactly("CHECK", "QTY")
        assertThat(relation("crate_mark").isColumnsComplete).isTrue()
        assertThat(snapshot.findColumn(depot, id("crate_mark"), id("\"CHECK\""), ProjectExternalMetadata.none()).status).isEqualTo(Status.FOUND)
        assertThat(columns("crate_flag")).containsExactly("QTY", "PRIMARY", "CONSTRAINT")
        assertThat(relation("crate_flag").isColumnsComplete).isTrue()
        assertThat(columns("crate_low")).containsExactly("check", "QTY")
        // The quoted name adds a column, not a constraint.
        assertThat(relation("crate_hold").isColumnsComplete).isFalse()
    }

    @Test
    fun addingAPartitionOrASupplementalLogAddsNoColumn() {
        val snapshot = snapshot(
            "create table crate_hist (id number not null, taken date) partition by range (taken) " +
                "(partition p1 values less than (date '2020-01-01'));\n" +
                "alter table crate_hist add partition p2 values less than (date '2021-01-01');\n" +
                "alter table crate_hist add supplemental log data (all) columns;\n" +
                "alter table crate_hist add supplemental log group crate_hist_log (id, taken) always;\n"
        )
        val relation = snapshot.findRelation(depot, id("crate_hist"), ProjectExternalMetadata.none()).relation.get()
        assertThat(relation.isColumnsComplete).isTrue()
        assertThat(relation.columns.map { it.name.identifier.lookupName }).containsExactly("ID", "TAKEN")
        assertThat(snapshot.findColumn(depot, id("crate_hist"), id("id"), ProjectExternalMetadata.none()).column.get().nullable).contains(false)
        assertThat(snapshot.findColumn(depot, id("crate_hist"), id("label"), ProjectExternalMetadata.none()).status).isEqualTo(Status.COLUMN_NOT_FOUND)
    }
}
