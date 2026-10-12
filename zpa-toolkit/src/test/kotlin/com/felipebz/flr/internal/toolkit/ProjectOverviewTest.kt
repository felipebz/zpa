/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
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
            .anyMatch { it.startsWith("ptf · written-name discovery: PARTIAL_DECLARATIONS") }
        assertThat(overview.labels()).anyMatch { it.startsWith("SEQUENCE s1 @") }
    }

    @Test
    fun recordMemberResolutionsPointAtTheirAstNode() {
        // Inside the package's own body the spec types resolve lexically, so the Project System records nothing there.
        val entries = overview.singleStartingWith("Resolutions in this file (").children
            .filter { it.label.startsWith("RECORD member paths") || it.label.startsWith("RECORD members") }
            .flatMap { it.children }
        assertThat(entries).anyMatch { it.astNode != null && it.sourceRange?.startLine == 25 }
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
            CREATE FUNCTION ptf(tab TABLE) RETURN TABLE PIPELINED ROW POLYMORPHIC USING types_pkg;
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
