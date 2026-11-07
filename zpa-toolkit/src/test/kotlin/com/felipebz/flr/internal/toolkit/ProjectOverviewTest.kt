//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.flr.internal.toolkit

import com.felipebz.zpa.tooling.ProjectOverviewNode
import com.felipebz.zpa.toolkit.ZpaConfigurationModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProjectOverviewTest {
    private val overview = SourceCodeModel(ZpaConfigurationModel()).apply { setSourceCode(SOURCE) }.projectOverview!!

    @Test
    fun preparationAndPackageSpecificationAreSummarized() {
        assertThat(overview.labels()).anyMatch { it.startsWith("Preparation: PREPARED_WITH_DECLARATIONS · 1/1 files") }
        assertThat(overview.single("PACKAGE types_pkg (specification) @1:0").childLabels())
            .contains("callable coverage: complete")
    }

    @Test
    fun recordFieldsAndSubtypesDescribeTheirTypes() {
        assertThat(overview.single("field #0 customer address_rec").childLabels())
            .containsExactly("type: declared type types_pkg.address_rec (PACKAGE_TYPE)")
        assertThat(overview.singleStartingWith("SUBTYPE money IS NUMBER @").childLabels())
            .anyMatch { it.startsWith("type: built-in NUMERIC") }
    }

    @Test
    fun bodySubprogramsAreCorrelatedWithTheirSpecification() {
        assertThat(overview.single("PACKAGE BODY types_pkg · 2 subprogram(s)").labels())
            .contains("specification: RESOLVED")
    }

    @Test
    fun omittedCallablesAndSequencesAreListed() {
        assertThat(overview.single("Omitted standalone callables (1)").childLabels())
            .anyMatch { it.startsWith("opaque · written-name discovery: PARTIAL_DECLARATIONS") }
        assertThat(overview.labels()).anyMatch { it.startsWith("SEQUENCE s1 @") }
    }

    @Test
    fun fieldsOfMembersPointAtTheirAstNode() {
        val entries = overview.singleStartingWith("Fields of members").children
        // `w.customer.street`, each name linked to its node; the recovered wrapped unit leaves the file's answers undecided.
        assertThat(entries.filter { it.sourceRange?.startLine == 26 }.map { it.label })
            .containsExactly("customer → undecided [PARSE_RECOVERY]", "street → undecided [PARSE_RECOVERY]")
        assertThat(entries).allMatch { it.astNode != null }
    }

    private fun ProjectOverviewNode.all(): Sequence<ProjectOverviewNode> = sequence {
        yield(this@all)
        for (child in children) yieldAll(child.all())
    }

    private fun ProjectOverviewNode.labels(): List<String> = all().map { it.label }.toList()

    private fun ProjectOverviewNode.childLabels(): List<String> = children.map { it.label }

    private fun ProjectOverviewNode.single(label: String): ProjectOverviewNode = all().single { it.label == label }

    private fun ProjectOverviewNode.singleStartingWith(prefix: String): ProjectOverviewNode =
        all().single { it.label.startsWith(prefix) }

    private companion object {
        val SOURCE = """
            CREATE OR REPLACE PACKAGE types_pkg AS
              TYPE address_rec IS RECORD (street VARCHAR2(100));
              TYPE customer_rec IS RECORD (customer address_rec, id NUMBER);
              SUBTYPE money IS NUMBER(12,2);
              c_limit CONSTANT money := 10;
              PROCEDURE run(a IN NUMBER, b OUT VARCHAR2 DEFAULT NULL);
              FUNCTION total RETURN money;
            END types_pkg;
            /
            CREATE OR REPLACE PACKAGE BODY types_pkg AS
              PROCEDURE run(a IN NUMBER, b OUT VARCHAR2 DEFAULT NULL) IS
                v types_pkg.customer_rec;
              BEGIN
                b := v.customer.street;
              END;
              FUNCTION total RETURN money IS BEGIN RETURN 1; END;
            END types_pkg;
            /
            CREATE FUNCTION opaque wrapped
            abc
            /
            CREATE SEQUENCE s1;
            DECLARE
              w types_pkg.customer_rec;
            BEGIN
              w.customer.street := NULL;
            END;
        """.trimIndent()
    }
}
