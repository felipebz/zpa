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

class NotInWithNullCheckTest : BaseCheckTest() {

    private fun table(name: String, vararg columns: Pair<String, Boolean?>) =
        ProjectExternalRelation.of("APP", name, true, columns.mapIndexed { i, (column, nullable) -> ProjectExternalColumn.of(column, i + 1, null, nullable) })

    private val metadata = ProjectExternalMetadata.builder("test").completeSchema("APP")
        .relation(table("SHELF", "ID" to false, "CODE" to true))
        .relation(table("CRATE", "SHELF_ID" to true, "COLOR" to true, "LABEL_ID" to true))
        .relation(table("SEALED_CRATE", "SHELF_ID" to false, "LABEL_ID" to true))
        .relation(table("LOOSE_CRATE", "SHELF_ID" to null))
        .relation(table("LABEL", "ID" to false))
        .build()

    private val fixture = getPath("not_in_with_null.sql")

    @Test
    fun withMetadata() {
        PlSqlCheckVerifier.verifyWithExternalContext(fixture, NotInWithNullCheck(), ProjectExternalContext.of(metadata, ProjectIdentifier.fromSource("APP")))
    }

    @Test
    fun withoutMetadataOnlyTheListsAreReported(@TempDir dir: File) {
        val text = File(fixture).readLines().joinToString("\n") { line ->
            val list = Regex("not in \\((?!select)").containsMatchIn(line) && !line.contains("not (")
            if (list) line else line.replace("-- Noncompliant", "--")
        }
        val unmarked = File(dir, "lists.sql").apply { writeText(text) }
        PlSqlCheckVerifier.verify(unmarked.path, NotInWithNullCheck())
        PlSqlCheckVerifier.verifyWithExternalContext(
            unmarked.path, NotInWithNullCheck(), ProjectExternalContext.of(ProjectExternalMetadata.none(), ProjectIdentifier.fromSource("APP"))
        )
    }

    @Test
    fun theTablesCreatedInTheSources() {
        PlSqlCheckVerifier.verifyWithExternalContext(
            getPath("not_in_with_null_source_tables.sql"), NotInWithNullCheck(),
            ProjectExternalContext.of(ProjectExternalMetadata.none(), ProjectIdentifier.fromSource("APP"))
        )
    }
}
