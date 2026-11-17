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

class NullTestOnNotNullColumnCheckTest : BaseCheckTest() {

    private fun code(nullable: Boolean?) = listOf(ProjectExternalColumn.of("CODE", 1, null, nullable))

    private val metadata = ProjectExternalMetadata.builder("test").completeSchema("APP")
        .relation(ProjectExternalRelation.of("APP", "ORDERS", true, listOf(
            ProjectExternalColumn.of("CODE", 1, null, false),
            ProjectExternalColumn.of("NOTE", 2, null, true)
        )))
        .relation(ProjectExternalRelation.of("APP", "ITEMS", true, code(false)))
        // NOT NULL, and a primary key: no row holds null.
        .relation(ProjectExternalRelation.of("APP", "NN_TAB", true, code(false)))
        .relation(ProjectExternalRelation.of("APP", "PK_TAB", true, code(false)))
        // NOT NULL ENABLE NOVALIDATE, NOT NULL DISABLE, NOT NULL DEFERRABLE and CHECK (code IS NOT NULL) show NULLABLE = 'Y'.
        .relation(ProjectExternalRelation.of("APP", "NOVALIDATE_TAB", true, code(true)))
        .relation(ProjectExternalRelation.of("APP", "DISABLED_TAB", true, code(true)))
        .relation(ProjectExternalRelation.of("APP", "DEFERRED_TAB", true, code(true)))
        .relation(ProjectExternalRelation.of("APP", "CHECK_TAB", true, code(true)))
        // A view's column over a NOT NULL column, and over the outer-joined side of a join.
        .relation(ProjectExternalRelation.view("APP", "NN_VIEW", true, code(false)))
        .relation(ProjectExternalRelation.view("APP", "OJ_VIEW", true, code(true)))
        .relation(ProjectExternalRelation.of("APP", "UNSTATED_TAB", true, code(null)))
        .build()

    private val fixture = getPath("null_test_on_not_null_column.sql")

    @Test
    fun withMetadata() {
        PlSqlCheckVerifier.verifyWithExternalContext(
            fixture, NullTestOnNotNullColumnCheck(), ProjectExternalContext.of(metadata, ProjectIdentifier.fromSource("APP"))
        )
    }

    @Test
    fun withoutMetadataNothingIsReported(@TempDir dir: File) {
        val text = File(fixture).readLines().filterNot { it.trimStart().removePrefix("--").trimStart().startsWith("^") }
            .joinToString("\n").replace("-- Noncompliant", "--")
        val unmarked = File(dir, "unmarked.sql").apply { writeText(text) }
        PlSqlCheckVerifier.verify(unmarked.path, NullTestOnNotNullColumnCheck())
        PlSqlCheckVerifier.verifyWithExternalContext(
            unmarked.path, NullTestOnNotNullColumnCheck(),
            ProjectExternalContext.of(ProjectExternalMetadata.none(), ProjectIdentifier.fromSource("APP"))
        )
    }

    @Test
    fun theTablesCreatedInTheSourcesWithoutMetadata() {
        PlSqlCheckVerifier.verifyWithExternalContext(
            getPath("null_test_on_not_null_column_source_tables.sql"), NullTestOnNotNullColumnCheck(),
            ProjectExternalContext.of(ProjectExternalMetadata.none(), ProjectIdentifier.fromSource("APP"))
        )
    }

}
