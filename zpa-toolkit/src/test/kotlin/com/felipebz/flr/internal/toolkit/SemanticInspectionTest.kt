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

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.tooling.ProjectRecordFieldTypeResolutionState
import com.felipebz.zpa.tooling.ProjectRecordMemberPathResolutionState
import com.felipebz.zpa.tooling.ProjectRecordMemberResolutionState
import com.felipebz.zpa.tooling.ProjectTypeResolutionState
import com.felipebz.zpa.toolkit.ZpaConfigurationModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SemanticInspectionTest {
    @Test
    fun selectedDatatypeExposesResolvedProjectTypeDeclarationAndRange() {
        val model = model(typeSource())
        val datatype = model.findNode(PlSqlGrammar.DATATYPE) {
            it.tokenText().replace(" ", "").contains("types_pkg.customer_rec")
        }

        val inspection = model.inspect(datatype)!!
        val resolution = inspection.projectTypeResolution!!
        val declaration = resolution.declaration!!

        assertThat(resolution.state).isEqualTo(ProjectTypeResolutionState.RESOLVED)
        assertThat(resolution.reference).isEqualTo("types_pkg.customer_rec")
        assertThat(declaration.qualifiedName).isEqualTo("types_pkg.customer_rec")
        assertThat(declaration.kind).isEqualTo("PACKAGE_TYPE")
        assertThat(declaration.fileId).isEqualTo("zpa-toolkit://current-buffer")
        assertThat(declaration.sourceRange.startLine).isEqualTo(8)
        assertThat(declaration.sourceRange.endLine).isEqualTo(10)
    }

    @Test
    fun selectedDirectRecordMemberExposesFieldAndFieldTypeResolution() {
        val model = model(typeSource())
        val member = model.findMember(listOf("value", "customer"))

        val inspection = model.inspect(member)!!
        val resolution = inspection.projectRecordMemberResolution!!
        val field = resolution.field!!
        val fieldType = inspection.projectRecordFieldTypeResolution!!
        val fieldTypeResolution = fieldType.resolution!!

        assertThat(resolution.state).isEqualTo(ProjectRecordMemberResolutionState.RESOLVED)
        assertThat(field.name).isEqualTo("customer")
        assertThat(field.ordinal).isEqualTo(0)
        assertThat(field.typeRef.name).isEqualTo("address_rec")
        assertThat(field.sourceRange.startLine).isEqualTo(9)
        assertThat(fieldType.state)
            .isEqualTo(ProjectRecordFieldTypeResolutionState.NAMED)
        assertThat(fieldTypeResolution.state)
            .isEqualTo(ProjectTypeResolutionState.RESOLVED)
        assertThat(fieldTypeResolution.declaration!!.qualifiedName)
            .isEqualTo("types_pkg.address_rec")
    }

    @Test
    fun selectedVariableShowsSymbolProjectTypeDeclarationSeparately() {
        val model = model(typeSource())
        val variable = model.findNode(PlSqlGrammar.IDENTIFIER_NAME) {
            it.tokenOriginalValue.equals("value", ignoreCase = true) &&
                it.parentOrNull?.type === PlSqlGrammar.VARIABLE_DECLARATION
        }

        val declaration = model.inspect(variable)!!.symbolProjectTypeDeclaration!!

        assertThat(declaration.qualifiedName).isEqualTo("types_pkg.customer_rec")
        assertThat(declaration.kind).isEqualTo("PACKAGE_TYPE")
    }

    @Test
    fun selectedRecordPathExposesOrderedSegmentsAndTerminalTypeState() {
        val model = model(typeSource())
        val path = model.findMember(listOf("value", "customer", "address", "city"))

        val inspection = model.inspect(path)!!
        val resolution = inspection.projectRecordMemberPathResolution!!

        assertThat(resolution.state).isEqualTo(ProjectRecordMemberPathResolutionState.COMPLETED)
        assertThat(resolution.segments.map { it.ordinal }).containsExactly(0, 1, 2)
        assertThat(resolution.segments.map { it.field.name }).containsExactly("customer", "address", "city")
        assertThat(resolution.segments.map { it.fieldTypeResolution.state })
            .containsExactly(
                ProjectRecordFieldTypeResolutionState.NAMED,
                ProjectRecordFieldTypeResolutionState.NAMED,
                ProjectRecordFieldTypeResolutionState.NAMED
            )
        assertThat(resolution.segments.last().fieldTypeResolution.resolution!!.state)
            .isEqualTo(ProjectTypeResolutionState.NOT_FOUND_IN_PROJECT)
    }

    @Test
    fun stoppedRecordPathExposesResolvedPrefixNextMemberAndReason() {
        val model = model(typeSource())
        val path = model.findMember(listOf("value", "customer", "address", "missing"))

        val inspection = model.inspect(path)!!
        val resolution = inspection.projectRecordMemberPathResolution!!

        assertThat(resolution.state).isEqualTo(ProjectRecordMemberPathResolutionState.STOPPED)
        assertThat(resolution.segments.map { it.field.name }).containsExactly("customer", "address")
        assertThat(resolution.segments.map { it.ordinal }).containsExactly(0, 1)
        assertThat(resolution.nextMember).isEqualTo("missing")
        assertThat(resolution.nextMemberOrdinal).isEqualTo(2)
        assertThat(resolution.stopReason).isEqualTo("MEMBER_NOT_FOUND")
        assertThat(resolution.segments.last().fieldTypeResolution.resolution!!.state)
            .isEqualTo(ProjectTypeResolutionState.RESOLVED)
    }

    @Test
    fun ambiguousProjectTypeCandidatesArePresentedDeterministically() {
        val source = """
            DECLARE value p.t; BEGIN NULL; END;
            CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;
            CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;
        """.trimIndent()
        val model = model(source)
        val datatype = model.findNode(PlSqlGrammar.DATATYPE) {
            it.tokenText().replace(" ", "").contains("p.t")
        }

        val resolution = model.inspect(datatype)!!.projectTypeResolution!!

        assertThat(resolution.state).isEqualTo(ProjectTypeResolutionState.AMBIGUOUS)
        assertThat(resolution.candidates.map { it.fileId }).containsExactly(
            "zpa-toolkit://current-buffer",
            "zpa-toolkit://current-buffer"
        )
        assertThat(resolution.candidates.map { it.sourceRange.startLine }).containsExactly(2, 3)
    }

    @Test
    fun ambiguousProjectRecordMembersArePresentedDeterministically() {
        val source = """
            CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER, id NUMBER); END p;
            DECLARE value p.t; BEGIN value.id := NULL; END;
        """.trimIndent()
        val model = model(source)
        val member = model.findMember(listOf("value", "id"))

        val resolution = model.inspect(member)!!.projectRecordMemberResolution!!

        assertThat(resolution.state).isEqualTo(ProjectRecordMemberResolutionState.AMBIGUOUS)
        assertThat(resolution.candidates.map { it.name }).containsExactly("id", "id")
        assertThat(resolution.candidates.map { it.ordinal }).containsExactly(0, 1)
    }

    @Test
    fun anchoredAndRefRecordFieldTypesArePresentedAsUnsupported() {
        val source = """
            CREATE PACKAGE p AS
              TYPE t IS RECORD (
                anchored some_table.id%TYPE,
                reference REF some_object_type
              );
            END p;
            DECLARE value p.t;
            BEGIN value.anchored := NULL; value.reference := NULL; END;
        """.trimIndent()
        val model = model(source)

        val anchored = model.inspect(model.findMember(listOf("value", "anchored")))!!
        val ref = model.inspect(model.findMember(listOf("value", "reference")))!!
        val anchoredType = anchored.projectRecordFieldTypeResolution!!
        val refType = ref.projectRecordFieldTypeResolution!!

        assertThat(anchoredType.state)
            .isEqualTo(ProjectRecordFieldTypeResolutionState.UNSUPPORTED)
        assertThat(anchoredType.typeRef.kind).isEqualTo("Anchored")
        assertThat(refType.state)
            .isEqualTo(ProjectRecordFieldTypeResolutionState.UNSUPPORTED)
        assertThat(refType.typeRef.kind).isEqualTo("Ref")
    }

    @Test
    fun legacyOnlyLocalRecordStillExposesLegacySummaryWithoutProjectMetadata() {
        val source = """
            DECLARE
              TYPE local_rec IS RECORD (id NUMBER);
              value local_rec;
            BEGIN
              value.id := 1;
            END;
        """.trimIndent()
        val model = model(source)
        val declaration = model.findNode(PlSqlGrammar.IDENTIFIER_NAME) {
            it.tokenOriginalValue.equals("value", ignoreCase = true) &&
                it.parentOrNull?.type === PlSqlGrammar.VARIABLE_DECLARATION
        }

        val inspection = model.inspect(declaration)!!
        val legacy = inspection.legacy!!
        val symbol = legacy.symbol!!

        assertThat(legacy).isNotNull
        assertThat(symbol.name).isEqualToIgnoringCase("value")
        assertThat(symbol.kind).isEqualTo("VARIABLE")
        assertThat(inspection.projectTypeResolution).isNull()
        assertThat(inspection.projectRecordMemberResolution).isNull()
        assertThat(inspection.projectRecordMemberPathResolution).isNull()
    }

    private fun model(source: String): SourceCodeModel = SourceCodeModel(ZpaConfigurationModel()).apply {
        setSourceCode(source)
    }

    private fun SourceCodeModel.findMember(parts: List<String>): AstNode =
        astNode.descendantsAndSelf().single {
            it.type === PlSqlGrammar.MEMBER_EXPRESSION &&
                it.getChildren(PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.VARIABLE_NAME)
                    .map { child -> child.tokenOriginalValue } == parts
        }

    private fun SourceCodeModel.findNode(type: com.felipebz.flr.api.AstNodeType, predicate: (AstNode) -> Boolean): AstNode =
        astNode.descendantsAndSelf().single { it.type === type && predicate(it) }

    private fun AstNode.tokenText() = tokens.joinToString(" ") { it.originalValue }

    private fun AstNode.descendantsAndSelf(): Sequence<AstNode> = sequence {
        yield(this@descendantsAndSelf)
        for (child in children) {
            yieldAll(child.descendantsAndSelf())
        }
    }

    private fun typeSource() = """
        CREATE PACKAGE types_pkg AS
          TYPE city_rec IS RECORD (
            city VARCHAR2(100)
          );
          TYPE address_rec IS RECORD (
            address city_rec
          );
          TYPE customer_rec IS RECORD (
            customer address_rec
          );
        END types_pkg;
        DECLARE
          value types_pkg.customer_rec;
        BEGIN
          value.customer := NULL;
          value.customer.address.city := NULL;
          value.customer.address.missing := NULL;
        END;
    """.trimIndent()
}
