//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.flr.internal.toolkit

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.tooling.FieldBindingState
import com.felipebz.zpa.tooling.TypeNameState
import com.felipebz.zpa.toolkit.ZpaConfigurationModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SemanticInspectionTest {
    @Test
    fun selectedDatatypeExposesTheProjectTypeItNamesAndItsCategory() {
        val model = model(typeSource())
        val datatype = model.findNode(PlSqlGrammar.DATATYPE) {
            it.tokenText().replace(" ", "").contains("types_pkg.customer_rec")
        }

        val inspection = model.inspect(datatype)!!
        val typeName = inspection.typeName!!
        val declaration = typeName.declaration!!

        assertThat(typeName.state).isEqualTo(TypeNameState.PROJECT)
        assertThat(typeName.reference).isEqualTo("types_pkg.customer_rec")
        assertThat(declaration.qualifiedName).isEqualTo("types_pkg.customer_rec")
        assertThat(declaration.kind).isEqualTo("PACKAGE_TYPE")
        assertThat(declaration.fileId).isEqualTo("zpa-toolkit://current-buffer")
        assertThat(declaration.sourceRange.startLine).isEqualTo(8)
        assertThat(declaration.sourceRange.endLine).isEqualTo(10)
        assertThat(inspection.typeCategory).isEqualTo("Record(RECORD_TYPE)")
    }

    @Test
    fun aTypeNameOfTheSameUnitIsLocalWithTheProjectDeclarationItIs() {
        val model = model(typeSource())
        val datatype = model.findNode(PlSqlGrammar.DATATYPE) { it.tokenText() == "address_rec" }

        val typeName = model.inspect(datatype)!!.typeName!!

        assertThat(typeName.state).isEqualTo(TypeNameState.LOCAL)
        assertThat(typeName.declaration!!.qualifiedName).isEqualTo("types_pkg.address_rec")
        assertThat(typeName.localDeclaration!!.startLine).isEqualTo(5)
    }

    @Test
    fun selectedMemberExposesItsFieldAndTheValueCategory() {
        val model = model(typeSource())
        val inspection = model.inspect(model.findMember(listOf("value", "customer")))!!
        val field = inspection.fields.single()

        assertThat(field.name).isEqualTo("customer")
        assertThat(field.state).isEqualTo(FieldBindingState.FIELD)
        assertThat(field.kind).isEqualTo("RECORD_FIELD")
        assertThat(field.ordinal).isEqualTo(0)
        assertThat(field.declaration!!.startLine).isEqualTo(9)
        assertThat(inspection.valueCategory).isEqualTo("Record(RECORD_TYPE)")
    }

    @Test
    fun selectedVariableShowsSymbolProjectTypeDeclarationSeparately() {
        val model = model(typeSource())
        val variable = model.findNode(PlSqlGrammar.IDENTIFIER_NAME) {
            it.tokenOriginalValue.equals("value", ignoreCase = true) &&
                it.parentOrNull?.type === PlSqlGrammar.VARIABLE_DECLARATION
        }

        val declaration = model.inspect(variable)!!.declaredProjectType!!

        assertThat(declaration.qualifiedName).isEqualTo("types_pkg.customer_rec")
        assertThat(declaration.kind).isEqualTo("PACKAGE_TYPE")
    }

    @Test
    fun aMemberChainListsTheFieldOfEachName() {
        val model = model(typeSource())
        val inspection = model.inspect(model.findMember(listOf("value", "customer", "address", "city")))!!

        assertThat(inspection.fields.map { it.name }).containsExactly("customer", "address", "city")
        assertThat(inspection.fields.map { it.state }).containsOnly(FieldBindingState.FIELD)
        assertThat(inspection.fields.map { it.declaration!!.startLine }).containsExactly(9, 6, 3)
        assertThat(inspection.valueCategory).isEqualTo("Scalar(CHARACTER)")
    }

    @Test
    fun aNameTheProjectRecordDoesNotHaveIsUndecided() {
        val model = model(typeSource())
        val inspection = model.inspect(model.findMember(listOf("value", "customer", "address", "missing")))!!

        assertThat(inspection.fields.map { it.state })
            .containsExactly(FieldBindingState.FIELD, FieldBindingState.FIELD, FieldBindingState.UNKNOWN)
    }

    @Test
    fun anAmbiguousTypeNameHasNoDeclaration() {
        val source = """
            DECLARE value p.t; BEGIN NULL; END;
            CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;
            CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;
        """.trimIndent()
        val model = model(source)
        val datatype = model.findNode(PlSqlGrammar.DATATYPE) {
            it.tokenText().replace(" ", "").contains("p.t")
        }

        val typeName = model.inspect(datatype)!!.typeName!!

        assertThat(typeName.state).isEqualTo(TypeNameState.AMBIGUOUS)
        assertThat(typeName.declaration).isNull()
    }

    @Test
    fun aFieldNamedTwiceIsUndecided() {
        val source = """
            CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER, id NUMBER); END p;
            DECLARE value p.t; BEGIN value.id := NULL; END;
        """.trimIndent()
        val model = model(source)

        val field = model.inspect(model.findMember(listOf("value", "id")))!!.fields.single()

        assertThat(field.state).isEqualTo(FieldBindingState.UNKNOWN)
        assertThat(field.reasons).containsExactly("DECLARATION_OUTSIDE_FILE")
    }

    @Test
    fun legacyOnlyLocalRecordStillExposesLegacySummaryWithoutLocalAnswers() {
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
        val symbol = inspection.legacy!!.symbol!!

        assertThat(symbol.name).isEqualToIgnoringCase("value")
        assertThat(symbol.kind).isEqualTo("VARIABLE")
        assertThat(inspection.typeName).isNull()
        assertThat(inspection.fields).isEmpty()
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
