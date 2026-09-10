/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
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
package com.felipebz.zpa.symbols

import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.symbols.PlSqlType
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.api.symbols.datatype.NumericDatatype
import com.felipebz.zpa.api.symbols.datatype.RecordDatatype
import com.felipebz.zpa.api.symbols.datatype.UnknownDatatype
import com.felipebz.zpa.project.AnchoredTypeRef
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.NamedTypeRef
import com.felipebz.zpa.project.PackageTypeDeclaration
import com.felipebz.zpa.project.ProjectIndexPreparationFailure
import com.felipebz.zpa.project.ProjectRecordField
import com.felipebz.zpa.project.ProjectRecordFieldTypeResolution
import com.felipebz.zpa.project.ProjectRecordMemberPathResolution
import com.felipebz.zpa.project.ProjectRecordMemberPathSegment
import com.felipebz.zpa.project.ProjectTypeResolution
import com.felipebz.zpa.project.ProjectTypeShape
import com.felipebz.zpa.project.QualifiedName
import com.felipebz.zpa.project.RefTypeRef
import com.felipebz.zpa.project.SourceRange
import com.felipebz.zpa.project.TypeAnchor
import com.felipebz.zpa.project.TypeRef
import com.felipebz.zpa.project.TypeRefSemanticResolution
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EffectiveSemanticTypeQueryTest {
    @Test
    fun knownLegacyNumericTypeTakesPrecedenceOverProjectFacts() {
        val node = node()
        node.plSqlDatatype = NumericDatatype()
        node.projectRecordFieldTypeResolution = fieldResolution(
            field("id", named("NUMBER")),
            TypeRefSemanticResolution.Project(
                named("NUMBER"),
                ProjectTypeResolution.Resolved(named("NUMBER"), record("number_type"))
            )
        )

        assertThat(EffectiveSemanticTypeQuery.typeCategory(node)).isEqualTo(
            EffectiveTypeCategory.Known(PlSqlType.NUMERIC, EffectiveTypeSource.LEGACY)
        )
    }

    @Test
    fun legacyRecordTypeIsReportedAsRecord() {
        val node = node()
        node.plSqlDatatype = RecordDatatype(null, null, emptyList())

        assertThat(EffectiveSemanticTypeQuery.typeCategory(node)).isEqualTo(
            EffectiveTypeCategory.Known(PlSqlType.RECORD, EffectiveTypeSource.LEGACY)
        )
    }

    @Test
    fun projectRecordSymbolIsReportedAsRecordWithoutCreatingADatatype() {
        val node = node()
        val symbol = Symbol(null, Symbol.Kind.VARIABLE, ScopeImpl(), UnknownDatatype)
        symbol.projectTypeDeclaration = record("project_record")
        node.symbol = symbol

        assertThat(EffectiveSemanticTypeQuery.typeCategory(node)).isEqualTo(
            EffectiveTypeCategory.Known(PlSqlType.RECORD, EffectiveTypeSource.PROJECT_DECLARATION)
        )
        assertThat(node.plSqlDatatype).isSameAs(UnknownDatatype)
    }

    @Test
    fun projectTypeResolutionCanProvideRecordCategory() {
        val reference = named("p.t")
        val declaration = record("t")
        val node = node()
        node.projectTypeResolution = ProjectTypeResolution.Resolved(reference, declaration)

        assertThat(EffectiveSemanticTypeQuery.typeCategory(node)).isEqualTo(
            EffectiveTypeCategory.Known(PlSqlType.RECORD, EffectiveTypeSource.PROJECT_DECLARATION)
        )
    }

    @Test
    fun directBuiltInFieldUsesItsSemanticCategory() {
        val field = field("id", named("NUMBER"))
        val node = node()
        node.projectRecordFieldTypeResolution = fieldResolution(
            field,
            TypeRefSemanticResolution.BuiltIn(named("NUMBER"), PlSqlType.NUMERIC)
        )

        assertThat(EffectiveSemanticTypeQuery.typeCategory(node)).isEqualTo(
            EffectiveTypeCategory.Known(PlSqlType.NUMERIC, EffectiveTypeSource.BUILT_IN_TYPE_REF)
        )
    }

    @Test
    fun directCharacterFieldUsesItsSemanticCategory() {
        val field = field("name", named("VARCHAR2"))
        val node = node()
        node.projectRecordFieldTypeResolution = fieldResolution(
            field,
            TypeRefSemanticResolution.BuiltIn(named("VARCHAR2"), PlSqlType.CHARACTER)
        )

        assertThat(EffectiveSemanticTypeQuery.typeCategory(node)).isEqualTo(
            EffectiveTypeCategory.Known(PlSqlType.CHARACTER, EffectiveTypeSource.BUILT_IN_TYPE_REF)
        )
        assertThat(node.plSqlDatatype).isSameAs(UnknownDatatype)
    }

    @Test
    fun directProjectRecordFieldUsesRecordCategory() {
        val reference = named("p.t")
        val field = field("child", reference)
        val node = node()
        node.projectRecordFieldTypeResolution = fieldResolution(
            field,
            TypeRefSemanticResolution.Project(
                reference,
                ProjectTypeResolution.Resolved(reference, record("t"))
            )
        )

        assertThat(EffectiveSemanticTypeQuery.typeCategory(node)).isEqualTo(
            EffectiveTypeCategory.Known(PlSqlType.RECORD, EffectiveTypeSource.PROJECT_DECLARATION)
        )
    }

    @Test
    fun completedPathUsesOnlyTheTerminalFieldCategory() {
        val firstField = field("child", named("p.child"))
        val terminalField = field("id", named("NUMBER"))
        val node = node()
        node.projectRecordMemberPathResolution = ProjectRecordMemberPathResolution.Completed(listOf(
            ProjectRecordMemberPathSegment(
                firstField,
                fieldResolution(
                    firstField,
                    TypeRefSemanticResolution.Project(
                        firstField.typeRef as NamedTypeRef,
                        ProjectTypeResolution.Resolved(firstField.typeRef, record("child"))
                    )
                )
            ),
            ProjectRecordMemberPathSegment(
                terminalField,
                fieldResolution(
                    terminalField,
                    TypeRefSemanticResolution.BuiltIn(
                        terminalField.typeRef as NamedTypeRef,
                        PlSqlType.NUMERIC
                    )
                )
            )
        ))

        assertThat(EffectiveSemanticTypeQuery.typeCategory(node)).isEqualTo(
            EffectiveTypeCategory.Known(PlSqlType.NUMERIC, EffectiveTypeSource.BUILT_IN_TYPE_REF)
        )
    }

    @Test
    fun completedPathCanEndInAProjectRecord() {
        val firstField = field("child", named("p.child"))
        val terminalField = field("nested", named("p.nested"))
        val terminalType = record("nested")
        val node = node()
        node.projectRecordMemberPathResolution = ProjectRecordMemberPathResolution.Completed(listOf(
            ProjectRecordMemberPathSegment(firstField, fieldResolution(
                firstField,
                TypeRefSemanticResolution.Project(
                    firstField.typeRef as NamedTypeRef,
                    ProjectTypeResolution.Resolved(firstField.typeRef, record("child"))
                )
            )),
            ProjectRecordMemberPathSegment(terminalField, fieldResolution(
                terminalField,
                TypeRefSemanticResolution.Project(
                    terminalField.typeRef as NamedTypeRef,
                    ProjectTypeResolution.Resolved(terminalField.typeRef, terminalType)
                )
            ))
        ))

        assertThat(EffectiveSemanticTypeQuery.typeCategory(node)).isEqualTo(
            EffectiveTypeCategory.Known(PlSqlType.RECORD, EffectiveTypeSource.PROJECT_DECLARATION)
        )
    }

    @Test
    fun stoppedPathDoesNotReportTheLastResolvedPrefixType() {
        val field = field("child", named("p.child"))
        val node = node()
        node.projectRecordMemberPathResolution = ProjectRecordMemberPathResolution.Stopped(
            listOf(ProjectRecordMemberPathSegment(
                field,
                fieldResolution(field, TypeRefSemanticResolution.Project(
                    field.typeRef as NamedTypeRef,
                    ProjectTypeResolution.NotFoundInProject(field.typeRef)
                ))
            )),
            identifier("missing"),
            1,
            ProjectRecordMemberPathResolution.StopReason.FIELD_TYPE_UNRESOLVED
        )

        assertThat(EffectiveSemanticTypeQuery.typeCategory(node))
            .isEqualTo(EffectiveTypeCategory.Unknown)
    }

    @Test
    fun unresolvedProjectStatesRemainUnknown() {
        val cases = listOf<TypeRefSemanticResolution>(
            TypeRefSemanticResolution.Project(
                named("ambiguous_type"),
                ProjectTypeResolution.Ambiguous(named("ambiguous_type"), listOf(record("one"), record("two")))
            ),
            TypeRefSemanticResolution.Project(
                named("incomplete_type"),
                ProjectTypeResolution.IncompleteIndex(
                    named("incomplete_type"),
                    listOf(record("known")),
                    listOf(ProjectIndexPreparationFailure(FileId("broken.sql"), "test.failure"))
                )
            ),
            TypeRefSemanticResolution.Project(
                named("missing_type"),
                ProjectTypeResolution.NotFoundInProject(named("missing_type"))
            ),
            TypeRefSemanticResolution.Project(
                named("not_prepared_type"),
                ProjectTypeResolution.NotPrepared(named("not_prepared_type"))
            )
        )

        cases.forEach { semanticResolution ->
            val node = node()
            node.projectRecordFieldTypeResolution = fieldResolution(
                field("value", semanticResolution.reference),
                semanticResolution
            )
            assertThat(EffectiveSemanticTypeQuery.typeCategory(node))
                .describedAs("$semanticResolution")
                .isEqualTo(EffectiveTypeCategory.Unknown)
        }
    }

    @Test
    fun unsupportedFieldReferencesRemainUnknown() {
        val anchored = AnchoredTypeRef(
            QualifiedName(identifier("some_table")),
            TypeAnchor.TYPE,
            range()
        )
        val ref = RefTypeRef(QualifiedName(identifier("some_object_type")), range())

        listOf<TypeRef>(anchored, ref).forEach { typeRef ->
            val node = node()
            node.projectRecordFieldTypeResolution = fieldResolution(
                field("value", typeRef),
                TypeRefSemanticResolution.Unsupported(typeRef)
            )
            assertThat(EffectiveSemanticTypeQuery.typeCategory(node))
                .isEqualTo(EffectiveTypeCategory.Unknown)
        }
    }

    @Test
    fun nonRecordProjectShapesRemainUnknown() {
        val node = node()
        node.projectTypeResolution = ProjectTypeResolution.Resolved(
            named("p.collection_type"),
            PackageTypeDeclaration(
                QualifiedName(identifier("p")),
                identifier("collection_type"),
                ProjectTypeShape.COLLECTION,
                emptyList(),
                FileId("collection.sql"),
                range("collection.sql")
            )
        )

        assertThat(EffectiveSemanticTypeQuery.typeCategory(node))
            .isEqualTo(EffectiveTypeCategory.Unknown)
    }

    @Test
    fun absentProjectFactsRemainUnknown() {
        assertThat(EffectiveSemanticTypeQuery.typeCategory(node()))
            .isEqualTo(EffectiveTypeCategory.Unknown)
    }

    private fun node() = SemanticAstNode(PlSqlGrammar.DATATYPE, "DATATYPE", null)

    private fun fieldResolution(
        field: ProjectRecordField,
        resolution: TypeRefSemanticResolution
    ) = ProjectRecordFieldTypeResolution(field, resolution)

    private fun record(name: String) = PackageTypeDeclaration(
        QualifiedName(identifier("p")),
        identifier(name),
        ProjectTypeShape.RECORD,
        emptyList(),
        FileId("$name.sql"),
        range("$name.sql")
    )

    private fun field(name: String, typeRef: TypeRef) = ProjectRecordField(
        identifier(name),
        0,
        typeRef,
        range()
    )

    private fun named(name: String) = NamedTypeRef(
        QualifiedName(name.split('.').map(::identifier)),
        range()
    )

    private fun identifier(name: String) = com.felipebz.zpa.project.OracleIdentifier.fromSource(name)

    private fun range(file: String = "test.sql") = SourceRange(FileId(file), 1, 0, 1, 1)
}
