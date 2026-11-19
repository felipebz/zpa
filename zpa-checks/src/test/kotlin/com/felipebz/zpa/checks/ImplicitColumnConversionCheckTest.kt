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

class ImplicitColumnConversionCheckTest : BaseCheckTest() {

    private fun type(name: String) = ProjectExternalDatatype.of(name, null, null, null, null, null)

    private val metadata = ProjectExternalMetadata.builder("test").completeSchema("APP")
        .relation(ProjectExternalRelation.of("APP", "CRATE", true, listOf(
            ProjectExternalColumn.of("ID", 1, type("NUMBER")),
            ProjectExternalColumn.of("CODE", 2, type("VARCHAR2")),
            ProjectExternalColumn.of("FIXED", 3, type("CHAR")),
            ProjectExternalColumn.of("WIDE", 4, type("NVARCHAR2")),
            ProjectExternalColumn.of("NOTE", 5, type("CLOB")),
            ProjectExternalColumn.of("LOOSE", 6, null)
        )))
        .relation(ProjectExternalRelation.of("APP", "BIN", true, listOf(ProjectExternalColumn.of("ID", 1, type("NUMBER")))))
        .build()

    private val fixture = getPath("implicit_column_conversion.sql")

    @Test
    fun withMetadata() {
        PlSqlCheckVerifier.verifyWithExternalContext(fixture, ImplicitColumnConversionCheck(), ProjectExternalContext.of(metadata, ProjectIdentifier.fromSource("APP")))
    }

    @Test
    fun withoutMetadataOnlyTheCreatedTableIsReported(@TempDir dir: File) {
        val text = File(fixture).readLines().joinToString("\n") { if (it.contains("box_tag")) it else it.replace("-- Noncompliant", "--") }
        val unmarked = File(dir, "created.sql").apply { writeText(text) }
        PlSqlCheckVerifier.verifyWithExternalContext(
            unmarked.path, ImplicitColumnConversionCheck(), ProjectExternalContext.of(ProjectExternalMetadata.none(), ProjectIdentifier.fromSource("APP"))
        )
    }
}
