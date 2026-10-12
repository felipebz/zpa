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

/** `CHARACTER SET` datatype clauses accepted by Oracle 26ai, in every declaration position the extractor retains. */
class DeclaredTypeCharacterSetExtractionTest {
    private val extractor = ProjectDeclarationExtractor()

    private fun extract(source: String, file: String = "a.sql") = extractor.extract(FileId(file), source)

    private fun identity(typeRef: TypeRef) = typeRef.headerIdentityKey()

    @Test
    fun everyRecordFieldIsKeptWithItsOriginalOrdinalAndCompleteType() {
        val source = """
            CREATE OR REPLACE PACKAGE rec_pkg AS
              v VARCHAR2(10);
              TYPE r IS RECORD (
                f1 NUMBER(5,2) NOT NULL := 1,
                f2 VARCHAR2(10 CHAR) DEFAULT 'x',
                f3 INTERVAL DAY(3) TO SECOND(2),
                f4 TIMESTAMP(3) WITH LOCAL TIME ZONE,
                f5 rec_src.c%TYPE,
                f6 rec_src%ROWTYPE,
                f7 REF rec_obj,
                f8 "REC_OBJ",
                f9 CHAR(1 BYTE),
                f10 INTERVAL YEAR(2) TO MONTH,
                f11 VARCHAR2(10) CHARACTER SET v%CHARSET,
                f12 LONG RAW,
                f13 DOUBLE PRECISION,
                f14 NATIONAL CHARACTER VARYING(10)
              );
            END rec_pkg;
        """.trimIndent()
        val record = extract(source).filterIsInstance<PackageTypeDeclaration>().single()
        val fields = record.recordFields
        assertThat(fields.map { it.name.originalSpelling }).containsExactlyElementsOf((1..14).map { "f$it" })
        assertThat(fields.map { it.ordinal }).containsExactlyElementsOf(0..13)
        val charset = fields[10]
        assertThat(charset.typeRef).isInstanceOf(NamedTypeRef::class.java)
        assertThat(charset.typeRef.name.toString()).isEqualTo("VARCHAR2")
        assertThat(identity(charset.typeRef)).contains("CHARACTER").contains("SET").contains("CHARSET")
        assertThat(charset.sourceRange.startLine).isEqualTo(14)
        assertThat(charset.typeRef.sourceRange.startColumn).isEqualTo(8)
        assertThat(charset.typeRef.sourceRange.endColumn).isEqualTo(44)
        val following = fields[11]
        assertThat(following.name.originalSpelling).isEqualTo("f12")
        assertThat(following.ordinal).isEqualTo(11)
        assertThat(following.sourceRange.startLine).isEqualTo(15)
    }

    @Test
    fun subtypeWithCharacterSetKeepsItsCompleteBaseType() {
        val source = """
            CREATE OR REPLACE PACKAGE sub_pkg AS
              v VARCHAR2(10);
              SUBTYPE s IS VARCHAR2(10) CHARACTER SET v%CHARSET;
              z NUMBER;
            END sub_pkg;
        """.trimIndent()
        val subtype = extract(source).filterIsInstance<PackageSubtypeDeclaration>().single()
        assertThat(subtype.name.originalSpelling).isEqualTo("s")
        assertThat(subtype.baseType.name.toString()).isEqualTo("VARCHAR2")
        assertThat(identity(subtype.baseType)).contains("CHARACTER").contains("CHARSET")
        assertThat(subtype.baseType.sourceRange.startLine).isEqualTo(3)
    }

    @Test
    fun characterSetFormalsAndReturnsKeepCompleteSignatures() {
        val spec = """
            CREATE PACKAGE app AS
              PROCEDURE cs(p VARCHAR2 CHARACTER SET ANY_CS, q VARCHAR2 CHARACTER SET p%CHARSET DEFAULT 'x', r NUMBER);
              FUNCTION ret(p NUMBER) RETURN VARCHAR2 CHARACTER SET ANY_CS;
              FUNCTION plain(p NUMBER) RETURN NUMBER;
            END app;
        """.trimIndent()
        val body = """
            CREATE PACKAGE BODY app AS
              PROCEDURE cs(p VARCHAR2 CHARACTER SET ANY_CS, q VARCHAR2 CHARACTER SET p%CHARSET DEFAULT 'x', r NUMBER) IS BEGIN NULL; END;
            END app;
        """.trimIndent()
        val specFacts = extract(spec, "spec.sql")
        assertThat(specFacts.filterIsInstance<PackageDeclaration>().single().callableCoverage).isEqualTo(PackageCallableCoverage.COMPLETE)
        val procedure = specFacts.filterIsInstance<PackageProcedureDeclaration>().single()
        assertThat(procedure.parameters.map { it.name.originalSpelling }).containsExactly("p", "q", "r")
        assertThat(procedure.parameters.map { it.ordinal }).containsExactly(1, 2, 3)
        assertThat(procedure.parameters.map { it.defaultPresent }).containsExactly(false, true, false)
        assertThat(identity(procedure.parameters[0].typeRef)).contains("ANY_CS")
        val functions = specFacts.filterIsInstance<PackageFunctionDeclaration>()
        assertThat(identity(functions[0].returnType)).contains("ANY_CS")
        assertThat((functions[1].returnType as NamedTypeRef).headerTypeIdentity).isNull()
        assertThat(functions[1].returnType.name.toString()).isEqualTo("NUMBER")

        val bodyProcedure = extract(body, "body.sql").filterIsInstance<PackageProcedureDeclaration>().single()
        assertThat(bodyProcedure.headerIdentity()).isEqualTo(procedure.headerIdentity())

        val standalone = extract(
            "CREATE PROCEDURE app.run(p VARCHAR2 CHARACTER SET ANY_CS) IS BEGIN NULL; END;\n/\n" +
                "CREATE FUNCTION app.conv(p VARCHAR2) RETURN VARCHAR2 CHARACTER SET p%CHARSET IS BEGIN RETURN p; END;",
            "standalone.sql"
        )
        assertThat(standalone.map { it.qualifiedName().toString() }).containsExactly("app.run", "app.conv")
        val run = standalone.filterIsInstance<StandaloneProcedureDeclaration>().single()
        assertThat(run.parameters.single().ordinal).isEqualTo(1)
        assertThat(identity(run.parameters.single().typeRef)).contains("ANY_CS")
    }

    @Test
    fun unsupportedTrailingDatatypeSyntaxIsNotAccepted() {
        val source = """
            CREATE PACKAGE bad AS
              SUBTYPE s1 IS VARCHAR2(10) CHARACTER SET;
              SUBTYPE s2 IS VARCHAR2(10) CHARACTER SET v%TYPE;
              SUBTYPE s3 IS NUMBER EXTRA;
              PROCEDURE p1(a VARCHAR2 CHARACTER SET ANY_CS EXTRA);
              PROCEDURE p2(a VARCHAR2 CHARACTER SET);
              FUNCTION f1 RETURN VARCHAR2 CHARACTER SET a.b%CHARSET%CHARSET;
              PROCEDURE ok;
            END bad;
        """.trimIndent()
        val facts = extract(source)
        assertThat(facts.filterIsInstance<PackageSubtypeDeclaration>()).isEmpty()
        assertThat(facts.filterIsInstance<PackageSubprogramDeclaration>().map { it.name.originalSpelling }).containsExactly("ok")
        assertThat(facts.filterIsInstance<PackageDeclaration>().single().callableCoverage.omittedNames)
            .containsExactlyInAnyOrder("P1", "P2", "F1")
        assertThat(extract("CREATE PROCEDURE app.run(p VARCHAR2 CHARACTER SET ANY_CS EXTRA) IS BEGIN NULL; END;")).isEmpty()
    }

    @Test
    fun writtenNameCompetesWithARetainedCharacterSetStandalone() {
        val context = ProjectCallTestFixtures.context(listOf(
            "spec.sql" to "CREATE PACKAGE app AS PROCEDURE run(a NUMBER); END app;",
            "standalone.sql" to "CREATE PROCEDURE app.run(p VARCHAR2 CHARACTER SET ANY_CS) IS BEGIN NULL; END;"
        ))
        val result = ProjectCallBinder(context).resolve(
            ProjectCallTarget.written(ProjectCallTestFixtures.name("app", "run")),
            ProjectCallKind.PROCEDURE,
            ProjectCallTestFixtures.arguments(null)
        )
        assertThat(result.status).isEqualTo(ProjectCallResolution.Status.AMBIGUOUS_TARGET)
        assertThat(result.uncertainties).containsExactly(ProjectCallUncertainty.NAMESPACE_COMPETITION)
        assertThat(result.candidates.map { it.declaration.declaration.role })
            .containsExactly(DeclarationRole.SPECIFICATION, DeclarationRole.STANDALONE)
        assertThat(result.selected).isNull()
    }
}
