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

class OuterJoinDiscardedByConditionCheckTest : BaseCheckTest() {

    private fun columns(vararg names: String) = names.mapIndexed { index, it -> ProjectExternalColumn.of(it, index + 1, null) }

    private val metadata = ProjectExternalMetadata.builder("test").completeSchema("APP")
        .relation(ProjectExternalRelation.of("APP", "SHELF", true, columns("ID", "CODE", "N")))
        .relation(ProjectExternalRelation.of("APP", "CRATE", true, columns("ID", "SHELF_ID", "COLOR", "LABEL_ID", "LO", "HI")))
        .relation(ProjectExternalRelation.of("APP", "LABEL", true, columns("ID")))
        .build()

    private val external = ProjectExternalContext.of(metadata, ProjectIdentifier.fromSource("APP"))

    @Test
    fun withoutMetadata() {
        PlSqlCheckVerifier.verify(getPath("outer_join_discarded_by_condition.sql"), OuterJoinDiscardedByConditionCheck())
    }

    @Test
    fun withMetadata() {
        PlSqlCheckVerifier.verifyWithExternalContext(getPath("outer_join_discarded_by_condition.sql"), OuterJoinDiscardedByConditionCheck(), external)
    }

    @Test
    fun anUnqualifiedNameNeedsTheMetadata(@TempDir dir: File) {
        val fixture = getPath("outer_join_discarded_by_condition_metadata.sql")
        PlSqlCheckVerifier.verifyWithExternalContext(fixture, OuterJoinDiscardedByConditionCheck(), external)
        val unmarked = File(dir, "unmarked.sql").apply { writeText(File(fixture).readText().replace("-- Noncompliant", "--")) }
        PlSqlCheckVerifier.verify(unmarked.path, OuterJoinDiscardedByConditionCheck())
    }
}
