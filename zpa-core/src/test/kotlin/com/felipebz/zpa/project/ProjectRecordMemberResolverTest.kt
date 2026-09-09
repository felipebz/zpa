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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProjectRecordMemberResolverTest {
    private val extractor = ProjectDeclarationExtractor()
    private val resolver = ProjectRecordMemberResolver()

    @Test
    fun resolvesTheExactRecordField() {
        val record = record("TYPE t IS RECORD (id NUMBER, customer VARCHAR2(100));")
        val field = record.recordFields[1]

        val result = resolver.resolve(record, OracleIdentifier.fromSource("CUSTOMER"))

        assertThat(result).isEqualTo(ProjectRecordMemberResolution.Resolved(record, field))
    }

    @Test
    fun returnsNotFoundForAnUnknownField() {
        val record = record("TYPE t IS RECORD (id NUMBER);")

        assertThat(resolver.resolve(record, OracleIdentifier.fromSource("missing")))
            .isEqualTo(ProjectRecordMemberResolution.NotFound(record))
    }

    @Test
    fun preservesOracleIdentifierMatching() {
        val record = record("TYPE t IS RECORD (FIELD NUMBER, \"Field\" NUMBER, \"FIELD\" NUMBER);")

        assertThat(resolver.resolve(record, OracleIdentifier.fromSource("field")))
            .isEqualTo(ProjectRecordMemberResolution.Resolved(record, record.recordFields[0]))
        assertThat(resolver.resolve(record, OracleIdentifier.fromSource("\"Field\"")))
            .isEqualTo(ProjectRecordMemberResolution.Resolved(record, record.recordFields[1]))
        assertThat(resolver.resolve(record, OracleIdentifier.fromSource("\"FIELD\"")))
            .isEqualTo(ProjectRecordMemberResolution.Resolved(record, record.recordFields[2]))
    }

    @Test
    fun preservesAmbiguityAndOrdersDuplicateFieldsByOrdinal() {
        val record = PackageTypeDeclaration(
            QualifiedName(OracleIdentifier.fromSource("p")),
            OracleIdentifier.fromSource("t"),
            ProjectTypeShape.RECORD,
            listOf(
                field("id", 3),
                field("ID", 1),
                field("id", 2)
            ),
            FileId("record.sql"),
            range()
        )

        val result = resolver.resolve(record, OracleIdentifier.fromSource("id"))

        assertThat(result).isInstanceOf(ProjectRecordMemberResolution.Ambiguous::class.java)
        assertThat((result as ProjectRecordMemberResolution.Ambiguous).candidates.map { it.ordinal })
            .containsExactly(1, 2, 3)
    }

    @Test
    fun rejectsNonRecordAndUnsupportedProjectTypes() {
        val collection = extractor.extract(FileId("types.sql"), "CREATE PACKAGE p AS TYPE t IS TABLE OF NUMBER; END p;")
            .filterIsInstance<PackageTypeDeclaration>().single()
        val standalone = extractor.extract(FileId("standalone.sql"), "CREATE TYPE t AS OBJECT (id NUMBER);")
            .filterIsInstance<StandaloneTypeDeclaration>().single()
        val subtype = extractor.extract(FileId("subtype.sql"), "CREATE PACKAGE p AS SUBTYPE t IS NUMBER; END p;")
            .filterIsInstance<PackageSubtypeDeclaration>().single()

        assertThat(resolver.resolve(collection, OracleIdentifier.fromSource("id")))
            .isEqualTo(ProjectRecordMemberResolution.UnsupportedType(collection))
        assertThat(resolver.resolve(standalone, OracleIdentifier.fromSource("id")))
            .isEqualTo(ProjectRecordMemberResolution.UnsupportedType(standalone))
        assertThat(resolver.resolve(subtype, OracleIdentifier.fromSource("id")))
            .isEqualTo(ProjectRecordMemberResolution.UnsupportedType(subtype))
    }

    private fun record(source: String): PackageTypeDeclaration =
        extractor.extract(FileId("record.sql"), "CREATE PACKAGE p AS $source END p;")
            .filterIsInstance<PackageTypeDeclaration>().single()

    private fun field(name: String, ordinal: Int) = ProjectRecordField(
        OracleIdentifier.fromSource(name), ordinal,
        NamedTypeRef(QualifiedName(OracleIdentifier.fromSource("NUMBER")), range()), range()
    )

    private fun range() = SourceRange(FileId("record.sql"), 1, 0, 1, 1)
}
