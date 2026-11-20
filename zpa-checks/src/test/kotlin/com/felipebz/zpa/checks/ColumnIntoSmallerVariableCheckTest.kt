//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.checks

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.api.project.ProjectExternalDatatype.LengthSemantics
import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ColumnIntoSmallerVariableCheckTest : BaseCheckTest() {

    private fun text(name: String, length: Int, semantics: LengthSemantics?) = ProjectExternalDatatype.of(name, null, length, semantics, null, null)
    private fun number(precision: Int?, scale: Int?) = ProjectExternalDatatype.of("NUMBER", null, null, null, precision, scale)

    private val metadata = ProjectExternalMetadata.builder("test").completeSchema("APP")
        .relation(ProjectExternalRelation.of("APP", "CRATE", true, listOf(
            ProjectExternalColumn.of("ID", 1, number(null, null)),
            ProjectExternalColumn.of("CODE", 2, text("VARCHAR2", 10, LengthSemantics.CHAR)),
            ProjectExternalColumn.of("WIDE", 3, text("VARCHAR2", 5, LengthSemantics.CHAR)),
            ProjectExternalColumn.of("PAD", 4, text("CHAR", 10, LengthSemantics.BYTE)),
            ProjectExternalColumn.of("NAME", 5, text("NVARCHAR2", 10, LengthSemantics.CHAR)),
            ProjectExternalColumn.of("HASH", 6, ProjectExternalDatatype.of("RAW", null, 16, LengthSemantics.BYTE, null, null)),
            ProjectExternalColumn.of("QTY", 7, number(6, 0)),
            ProjectExternalColumn.of("PRICE", 8, number(7, 2)),
            ProjectExternalColumn.of("LOOSE", 9, number(null, null)),
            ProjectExternalColumn.of("WIDE2", 10, text("VARCHAR2", 10, LengthSemantics.CHAR))
        )))
        .relation(ProjectExternalRelation.of("APP", "BIN", true, listOf(ProjectExternalColumn.of("SHORT_CODE", 1, text("VARCHAR2", 3, LengthSemantics.BYTE)))))
        .build()

    private val fixture = getPath("column_into_smaller_variable.sql")

    @Test
    fun withMetadata() {
        PlSqlCheckVerifier.verifyWithExternalContext(fixture, ColumnIntoSmallerVariableCheck(), ProjectExternalContext.of(metadata, ProjectIdentifier.fromSource("APP")))
    }

    @Test
    fun withoutMetadataNothingIsReported(@TempDir dir: File) {
        val unmarked = File(dir, "unmarked.sql").apply { writeText(File(fixture).readText().replace(Regex("-- Noncompliant.*"), "")) }
        PlSqlCheckVerifier.verify(unmarked.path, ColumnIntoSmallerVariableCheck())
    }
}
