//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.checks

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ColumnHidesPlSqlNameCheckTest : BaseCheckTest() {

    private fun columns(vararg names: String) = names.mapIndexed { index, it -> ProjectExternalColumn.of(it, index + 1, null) }

    private val metadata = ProjectExternalMetadata.builder("test").completeSchema("APP")
        .relation(ProjectExternalRelation.of("APP", "CRATE", true, columns("ID", "SHELF_ID", "COLOR")))
        .relation(ProjectExternalRelation.of("APP", "LABEL", true, columns("ID")))
        .relation(ProjectExternalRelation.of("APP", "PALLET", true, columns("ID", "STOCK")))
        .build()

    private val fixture = getPath("column_hides_plsql_name.sql")

    @Test
    fun withMetadata() {
        PlSqlCheckVerifier.verifyWithExternalContext(fixture, ColumnHidesPlSqlNameCheck(), ProjectExternalContext.of(metadata, ProjectIdentifier.fromSource("APP")))
    }

    @Test
    fun withoutMetadataNothingIsReported(@TempDir dir: File) {
        val unmarked = File(dir, "unmarked.sql").apply { writeText(File(fixture).readText().replace(Regex("-- Noncompliant.*"), "")) }
        PlSqlCheckVerifier.verify(unmarked.path, ColumnHidesPlSqlNameCheck())
    }

    @Test
    fun aTableTheSourcesCreateWithoutMetadata() {
        PlSqlCheckVerifier.verifyWithExternalContext(
            getPath("column_hides_plsql_name_source_table.sql"), ColumnHidesPlSqlNameCheck(),
            ProjectExternalContext.of(ProjectExternalMetadata.none(), ProjectIdentifier.fromSource("APP"))
        )
    }
}
