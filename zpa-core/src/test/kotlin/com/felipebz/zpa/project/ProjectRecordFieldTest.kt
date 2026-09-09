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
package com.felipebz.zpa.project

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.Token
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.symbols.Scope
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.api.symbols.datatype.PlSqlDatatype
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class ProjectRecordFieldTest {
    private val fileId = FileId("fixture/records.sql")
    private val extractor = ProjectDeclarationExtractor()

    @Test
    fun extractsOrderedFieldsWithExactTypesAndRanges() {
        val source = "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER, name VARCHAR2(100)); END p;"
        val record = recordTypes(source).single()

        assertThat(record.recordFields.map { it.name.lookupName }).containsExactly("ID", "NAME")
        assertThat(record.recordFields.map { it.ordinal }).containsExactly(0, 1)
        assertNamedType(record.recordFields[0].typeRef, "NUMBER")
        assertNamedType(record.recordFields[1].typeRef, "VARCHAR2")
        assertThat(record.recordFields[0].sourceRange).isEqualTo(rangeOf(source, "id NUMBER"))
        assertThat(record.recordFields[1].sourceRange).isEqualTo(rangeOf(source, "name VARCHAR2(100)"))
        assertThat(record.recordFields[1].typeRef.sourceRange).isEqualTo(rangeOf(source, "VARCHAR2(100)"))
    }

    @Test
    fun preservesQualifiedAnchoredAndQuotedNames() {
        val source = """
            CREATE PACKAGE p AS
              TYPE t IS RECORD (
                customer customer_pkg.customer_type,
                id some_table.id%TYPE,
                "Mixed Field" "Owner"."Mixed Type",
                reference REF "Owner"."Object Type"
              );
            END p;
        """.trimIndent()
        PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).parse(source)
        val fields = recordTypes(source).single().recordFields

        assertThat((fields[0].typeRef as NamedTypeRef).name).isEqualTo(name("customer_pkg", "customer_type"))
        val anchored = fields[1].typeRef as AnchoredTypeRef
        assertThat(anchored.name).isEqualTo(name("some_table", "id"))
        assertThat(anchored.anchor).isEqualTo(TypeAnchor.TYPE)
        assertThat(fields[2].name).isEqualTo(OracleIdentifier.fromSource("\"Mixed Field\""))
        assertThat((fields[2].typeRef as NamedTypeRef).name)
            .isEqualTo(name("\"Owner\"", "\"Mixed Type\""))
        assertThat((fields[3].typeRef as RefTypeRef).name)
            .isEqualTo(name("\"Owner\"", "\"Object Type\""))
    }

    @Test
    fun tokenFieldsAgreeWithIndependentAstStructure() {
        val source = """
            CREATE PACKAGE p AS
              TYPE t IS RECORD (
                customer customer_pkg.customer_type,
                id "Some Table"."Mixed Id"%TYPE,
                amount NUMBER(10, 2) DEFAULT 0,
                reference REF "Owner"."Object Type"
              );
            END p;
        """.trimIndent()
        val extracted = recordTypes(source).single().recordFields
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).parse(source)
        val astFields = root.getDescendants(PlSqlGrammar.RECORD_DECLARATION).single()
            .getChildren(PlSqlGrammar.RECORD_FIELD_DECLARATION)
            .mapIndexed { ordinal, field ->
                val identifier = field.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
                val datatype = field.getFirstChild(PlSqlGrammar.DATATYPE)
                val ref = datatype.tokens.first().value.equals("REF", true)
                val nameStart = if (ref) 1 else 0
                val nameSegments = mutableListOf(OracleIdentifier.fromSource(datatype.tokens[nameStart].originalValue))
                var tokenIndex = nameStart + 1
                while (tokenIndex + 1 < datatype.tokens.size && datatype.tokens[tokenIndex].originalValue == ".") {
                    nameSegments += OracleIdentifier.fromSource(datatype.tokens[tokenIndex + 1].originalValue)
                    tokenIndex += 2
                }
                val qualifiedName = QualifiedName(nameSegments)
                val datatypeRange = SourceRange(
                    fileId,
                    datatype.tokenOrNull!!.line,
                    datatype.tokenOrNull!!.column,
                    datatype.lastTokenOrNull!!.endLine,
                    datatype.lastTokenOrNull!!.endColumn
                )
                val typeRef = if (datatype.tokens.any { it.originalValue == "%" }) {
                    val anchor = if (datatype.tokens.any { it.value.equals("ROWTYPE", true) }) {
                        TypeAnchor.ROWTYPE
                    } else {
                        TypeAnchor.TYPE
                    }
                    AnchoredTypeRef(qualifiedName, anchor, datatypeRange)
                } else if (ref) {
                    RefTypeRef(qualifiedName, datatypeRange)
                } else {
                    NamedTypeRef(qualifiedName, datatypeRange)
                }
                ProjectRecordField(
                    OracleIdentifier.fromSource(identifier.tokenOriginalValue),
                    ordinal,
                    typeRef,
                    SourceRange(
                        fileId,
                        field.tokenOrNull!!.line,
                        field.tokenOrNull!!.column,
                        field.lastTokenOrNull!!.endLine,
                        field.lastTokenOrNull!!.endColumn
                    )
                )
            }

        assertThat(extracted).containsExactlyElementsOf(astFields)
    }

    @Test
    fun keepsFieldsIndependentAcrossRecords() {
        val records = recordTypes("""
            CREATE PACKAGE p AS
              TYPE first_record IS RECORD (first_id NUMBER, first_name VARCHAR2(30));
              TYPE second_record IS RECORD (second_id NUMBER);
            END p;
        """.trimIndent())

        assertThat(records).hasSize(2)
        assertThat(records[0].recordFields.map { it.name.lookupName }).containsExactly("FIRST_ID", "FIRST_NAME")
        assertThat(records[1].recordFields.map { it.name.lookupName }).containsExactly("SECOND_ID")
    }

    @Test
    fun fieldOrderAndIndexCandidatesAreIndependentOfFileAdditionOrder() {
        val firstFile = FileId("a.sql")
        val secondFile = FileId("b.sql")
        val first = extractor.extract(firstFile, "CREATE PACKAGE p AS TYPE t IS RECORD (z NUMBER, a NUMBER); END p;")
        val second = extractor.extract(secondFile, "CREATE PACKAGE p AS TYPE t IS RECORD (b NUMBER, c NUMBER); END p;")
        val forward = ProjectSymbolIndexBuilder().also {
            it.add(firstFile, first)
            it.add(secondFile, second)
        }.build()
        val reverse = ProjectSymbolIndexBuilder().also {
            it.add(secondFile, second)
            it.add(firstFile, first)
        }.build()
        val owner = name("p")
        val typeName = OracleIdentifier.fromSource("t")

        val forwardCandidates = forward.findTypes(owner, typeName).filterIsInstance<PackageTypeDeclaration>()
        val reverseCandidates = reverse.findTypes(owner, typeName).filterIsInstance<PackageTypeDeclaration>()
        assertThat(forwardCandidates).containsExactlyElementsOf(reverseCandidates)
        assertThat(forwardCandidates.map { it.fileId.value }).containsExactly("a.sql", "b.sql")
        assertThat(forwardCandidates[0].recordFields.map { it.name.lookupName }).containsExactly("Z", "A")
        assertThat(forwardCandidates[1].recordFields.map { it.name.lookupName }).containsExactly("B", "C")
    }

    @Test
    fun nestedDatatypeDelimitersDoNotSplitFields() {
        val source = """
            CREATE PACKAGE p AS
              TYPE t IS RECORD (
                amount NUMBER(10, 2) NOT NULL DEFAULT ROUND(1, 2),
                label VARCHAR2(100) := SUBSTR('a,b', 1, 1),
                created_at TIMESTAMP(6) WITH TIME ZONE
              );
            END p;
        """.trimIndent()
        PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).parse(source)

        val fields = recordTypes(source).single().recordFields
        assertThat(fields.map { it.name.lookupName }).containsExactly("AMOUNT", "LABEL", "CREATED_AT")
        assertThat(fields.map { it.typeRef.name.last.lookupName }).containsExactly("NUMBER", "VARCHAR2", "TIMESTAMP")
    }

    @Test
    fun nonRecordTypesCannotContainRecordFields() {
        val declarations = extractor.extract(fileId, """
            CREATE PACKAGE p AS
              TYPE collection_type IS TABLE OF NUMBER;
              TYPE cursor_type IS REF CURSOR;
              SUBTYPE subtype_type IS NUMBER;
            END p;
        """.trimIndent())

        assertThat(declarations.filterIsInstance<PackageTypeDeclaration>())
            .allMatch { it.recordFields.isEmpty() }
        assertThatThrownBy {
            PackageTypeDeclaration(
                name("p"), OracleIdentifier.fromSource("bad"), ProjectTypeShape.COLLECTION,
                listOf(field("value")), fileId, sourceRange()
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun retainedFieldStateIsImmutableAndProjectModelOnly() {
        val mutableFields = mutableListOf(field("first"))
        val declaration = PackageTypeDeclaration(
            name("p"), OracleIdentifier.fromSource("t"), ProjectTypeShape.RECORD,
            mutableFields, fileId, sourceRange()
        )
        mutableFields += field("second")

        assertThat(declaration.recordFields).hasSize(1)
        assertThatThrownBy { (declaration.recordFields as MutableList<ProjectRecordField>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)

        val forbidden = listOf(
            AstNode::class.java,
            Token::class.java,
            Scope::class.java,
            Symbol::class.java,
            PlSqlDatatype::class.java
        )
        assertThat(ProjectRecordField::class.java.declaredFields.map { it.type })
            .noneMatch { fieldType -> forbidden.any { it.isAssignableFrom(fieldType) } }
    }

    private fun recordTypes(source: String) = extractor.extract(fileId, source)
        .filterIsInstance<PackageTypeDeclaration>()
        .filter { it.shape == ProjectTypeShape.RECORD }

    private fun assertNamedType(typeRef: TypeRef, expected: String) {
        assertThat(typeRef).isInstanceOf(NamedTypeRef::class.java)
        assertThat(typeRef.name).isEqualTo(name(expected))
    }

    private fun name(vararg segments: String) = QualifiedName(segments.map(OracleIdentifier::fromSource))

    private fun rangeOf(source: String, text: String): SourceRange {
        val start = source.indexOf(text)
        return SourceRange(fileId, 1, start, 1, start + text.length)
    }

    private fun sourceRange() = SourceRange(fileId, 1, 0, 1, 1)

    private fun field(name: String) = ProjectRecordField(
        OracleIdentifier.fromSource(name),
        0,
        NamedTypeRef(this.name("number"), sourceRange()),
        sourceRange()
    )
}
