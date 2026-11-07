//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(com.felipebz.zpa.api.annotations.ZpaExperimentalApi::class)

package com.felipebz.zpa.api.project

import com.felipebz.zpa.project.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The holder of a retained type reference is found among the references of its file, grouped by range once. */
class RetainedReferenceLookupTest {
    private val spec = "create package pk_orders as\n" +
        "  g_total number;\n" +
        "  type t_line is record (qty number, price pk_orders.g_total%type);\n" +
        "  procedure add_line(p_qty number, p_price pk_orders.g_total%type);\n" +
        "  function line_total(p_line t_line) return number;\n" +
        "end;\n/\n"

    private val snapshot = ProjectAnalysisContext.prepared(
        ProjectIndexPreparation().prepare(listOf(ProjectSource(FileId("pk.sql")) { spec }), concurrent = false)
    ).createSnapshot()

    private fun references(): List<ProjectTypeReference> = snapshot.declarations.flatMap { view ->
        when (view) {
            is ProjectPackageValueView -> listOf(view.typeRef)
            is ProjectPackageTypeView -> view.recordFields.map { it.typeRef }
            is ProjectSubprogramView -> view.parameters.map { it.typeRef } + listOfNotNull((view as? ProjectFunctionView)?.returnType)
            else -> emptyList()
        }
    }

    @Test
    fun eachRetainedReferenceFindsItsHolderAndWhatMayCaptureIt() {
        val holders = references().map { ReferenceHolders.find(snapshot.projectIndex, it) }
        assertThat(holders).allMatch { it != null }
        assertThat(holders.map { it!!.fact.qualifiedName().toString() to it.capturingNames.map(Any::toString) }).containsExactly(
            "pk_orders.g_total" to emptyList(),
            "pk_orders.t_line" to listOf("qty", "price"),
            "pk_orders.t_line" to listOf("qty", "price"),
            "pk_orders.add_line" to listOf("p_qty", "p_price"),
            "pk_orders.add_line" to listOf("p_qty", "p_price"),
            "pk_orders.line_total" to listOf("p_line"),
            "pk_orders.line_total" to listOf("p_line")
        )
    }

    @Test
    fun theFilesReferencesAreGroupedOnceForEveryLookup() {
        references().forEach { ReferenceHolders.find(snapshot.projectIndex, it) }
        var builds = 0
        // Already grouped by the lookups above: asking again, from the index or a schema view, builds nothing.
        snapshot.projectIndex.retainedTypeRefs(FileId("pk.sql")) { builds++; emptyMap() }
        snapshot.projectIndex.preferring(OracleIdentifier.fromSource("app")).retainedTypeRefs(FileId("pk.sql")) { builds++; emptyMap() }
        assertThat(builds).isZero
    }
}
