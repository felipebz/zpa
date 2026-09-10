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

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class ProjectDeclarationTest {
    private val fileId = FileId("fixture/package.sql")
    private val extractor = ProjectDeclarationExtractor()

    @Test
    fun extractsPackageDeclarationsAndBodySubprograms() {
        val declarations = extractor.extract(fileId, """
            CREATE OR REPLACE PACKAGE "Pack" AS
              TYPE t_record IS RECORD (id NUMBER);
              TYPE t_table IS TABLE OF t_record INDEX BY BINARY_INTEGER;
              TYPE t_varray IS VARRAY(3) OF VARCHAR2(30);
              TYPE t_cursor IS REF CURSOR;
              SUBTYPE t_sub IS "Owner"."Base"%TYPE;
              PROCEDURE overloaded(p IN NUMBER, q OUT NOCOPY "Owner"."Base"%ROWTYPE, r IN VARCHAR2 DEFAULT 'x');
              PROCEDURE overloaded(p IN NUMBER);
              FUNCTION overloaded(p IN OUT NOCOPY NUMBER := 1) RETURN "Owner"."Result"%TYPE;
            END "Pack";
            /
            CREATE PACKAGE BODY "Pack" AS
              PROCEDURE private_body_declaration IS BEGIN NULL; END;
            END "Pack";
            /
        """.trimIndent())

        val packageDeclaration = declarations.filterIsInstance<PackageDeclaration>().single()
        assertThat(packageDeclaration.name).isEqualTo(QualifiedName(OracleIdentifier.fromSource("\"Pack\"")))
        assertThat(packageDeclaration.role).isEqualTo(DeclarationRole.SPECIFICATION)

        val types = declarations.filterIsInstance<PackageTypeDeclaration>()
        assertThat(types).extracting<String> { it.name.lookupName }
            .containsExactly("T_RECORD", "T_TABLE", "T_VARRAY", "T_CURSOR")
        assertThat(types.map { it.shape }).containsExactly(
            ProjectTypeShape.RECORD,
            ProjectTypeShape.COLLECTION,
            ProjectTypeShape.COLLECTION,
            ProjectTypeShape.REF_CURSOR
        )

        val subtype = declarations.filterIsInstance<PackageSubtypeDeclaration>().single()
        assertThat(subtype.baseType).isInstanceOf(AnchoredTypeRef::class.java)
        assertThat((subtype.baseType as AnchoredTypeRef).anchor).isEqualTo(TypeAnchor.TYPE)
        assertThat(subtype.baseType.name).isEqualTo(
            QualifiedName(listOf(OracleIdentifier.fromSource("\"Owner\""), OracleIdentifier.fromSource("\"Base\"")))
        )

        val procedures = declarations.filterIsInstance<PackageProcedureDeclaration>()
        val specificationProcedures = procedures.filter { it.role == DeclarationRole.SPECIFICATION }
        assertThat(specificationProcedures).hasSize(2)
        assertThat(specificationProcedures.map { it.parameters.size }).containsExactly(3, 1)
        assertThat(specificationProcedures.first().parameters.map { it.ordinal }).containsExactly(1, 2, 3)
        assertThat(specificationProcedures.first().parameters.map { it.mode })
            .containsExactly(ParameterMode.IN, ParameterMode.OUT, ParameterMode.IN)
        assertThat(specificationProcedures.first().parameters[1].nocopy).isTrue
        assertThat(specificationProcedures.first().parameters[2].defaultPresent).isTrue

        val function = declarations.filterIsInstance<PackageFunctionDeclaration>()
            .single { it.role == DeclarationRole.SPECIFICATION }
        assertThat(function.parameters.single().mode).isEqualTo(ParameterMode.IN_OUT)
        assertThat(function.parameters.single().nocopy).isTrue
        assertThat(function.parameters.single().defaultPresent).isTrue
        assertThat(function.returnType).isInstanceOf(AnchoredTypeRef::class.java)

        val bodyProcedure = procedures.single { it.role == DeclarationRole.BODY }
        assertThat(bodyProcedure.name.lookupName).isEqualTo("PRIVATE_BODY_DECLARATION")
        assertThat(bodyProcedure.owner).isEqualTo(packageDeclaration.name)
        assertThat(bodyProcedure.parameters).isEmpty()
    }

    @Test
    fun extractsPackageBodyImplementationsAndSkipsForwardDeclarationsAndNestedSubprograms() {
        val declarations = extractor.extract(fileId, """
            CREATE OR REPLACE NONEDITIONABLE PACKAGE BODY p AS
              PROCEDURE public_work(value IN OUT NOCOPY CLOB) IS
              BEGIN
                NULL;
              END public_work;

              FUNCTION make_value(value IN NUMBER) RETURN VARCHAR2 IS
              BEGIN
                RETURN 'value';
              END make_value;

              PROCEDURE private_helper(value NUMBER);

              PROCEDURE private_helper(value NUMBER DEFAULT 1) IS
              BEGIN
                NULL;
              END private_helper;

              PROCEDURE outer_work IS
                PROCEDURE nested_work(value NUMBER) IS
                BEGIN
                  NULL;
                END nested_work;
              BEGIN
                NULL;
              END outer_work;
            BEGIN
              NULL;
            END p;
        """.trimIndent())

        val bodyProcedures = declarations.filterIsInstance<PackageProcedureDeclaration>()
            .filter { it.role == DeclarationRole.BODY }
        assertThat(bodyProcedures.map { it.name.lookupName })
            .containsExactly("PUBLIC_WORK", "PRIVATE_HELPER", "OUTER_WORK")
        assertThat(bodyProcedures.map { it.parameters.size }).containsExactly(1, 1, 0)
        assertThat(bodyProcedures.first().parameters.single().nocopy).isTrue
        assertThat(bodyProcedures.first().sourceRange.startLine).isEqualTo(2)
        assertThat(bodyProcedures.first().sourceRange.endLine).isEqualTo(5)
        assertThat(bodyProcedures[1].parameters.single().defaultPresent).isTrue
        assertThat(bodyProcedures).noneMatch { it.name.lookupName == "NESTED_WORK" }

        val bodyFunction = declarations.filterIsInstance<PackageFunctionDeclaration>()
            .single { it.role == DeclarationRole.BODY }
        assertThat(bodyFunction.owner).isEqualTo(QualifiedName(OracleIdentifier.fromSource("p")))
        assertThat(bodyFunction.name.lookupName).isEqualTo("MAKE_VALUE")
        assertThat(bodyFunction.parameters.single().typeRef).isEqualTo(
            NamedTypeRef(
                QualifiedName(OracleIdentifier.fromSource("NUMBER")),
                bodyFunction.parameters.single().typeRef.sourceRange
            )
        )
        assertThat(bodyFunction.returnType).isEqualTo(
            NamedTypeRef(QualifiedName(OracleIdentifier.fromSource("VARCHAR2")), bodyFunction.returnType.sourceRange)
        )
        assertThat(bodyFunction.sourceRange.startLine).isEqualTo(7)
        assertThat(bodyFunction.sourceRange.endLine).isEqualTo(10)
    }

    @Test
    fun preservesSpecificationAndBodyMetadataIndependently() {
        val specification = extractor.extract(FileId("p_spec.sql"), """
            CREATE PACKAGE p AS
              PROCEDURE test(value IN OUT NOCOPY CLOB DEFAULT NULL);
            END p;
        """.trimIndent()).filterIsInstance<PackageProcedureDeclaration>().single()
        val body = extractor.extract(FileId("p_body.sql"), """
            CREATE PACKAGE BODY p AS
              PROCEDURE test(value IN OUT CLOB) IS
              BEGIN
                NULL;
              END test;
            END p;
        """.trimIndent()).filterIsInstance<PackageProcedureDeclaration>().single()

        assertThat(specification.role).isEqualTo(DeclarationRole.SPECIFICATION)
        assertThat(body.role).isEqualTo(DeclarationRole.BODY)
        assertThat(specification.owner).isEqualTo(body.owner)
        assertThat(specification.name).isEqualTo(body.name)
        assertThat(specification.parameters.single().nocopy).isTrue
        assertThat(body.parameters.single().nocopy).isFalse
        assertThat(specification.parameters.single().defaultPresent).isTrue
        assertThat(body.parameters.single().defaultPresent).isFalse
        assertThat(specification.overloadIdentity()).isEqualTo(body.overloadIdentity())
        assertThat(specification.headerIdentity()).isEqualTo(body.headerIdentity())
        assertThat(specification.sourceRange).isNotEqualTo(body.sourceRange)
    }

    @Test
    fun preservesQuotedBodyIdentifiersAndAcceptedCreatePrefixes() {
        val declarations = extractor.extract(fileId, """
            CREATE OR REPLACE EDITIONABLE PACKAGE BODY "Pack" AS
              FUNCTION "Make Value"("Input" IN "Owner"."Number Type") RETURN "Owner"."Result Type" IS
              BEGIN
                RETURN NULL;
              END "Make Value";
            END "Pack";
        """.trimIndent())

        val function = declarations.filterIsInstance<PackageFunctionDeclaration>().single()
        assertThat(function.role).isEqualTo(DeclarationRole.BODY)
        assertThat(function.owner).isEqualTo(QualifiedName(OracleIdentifier.fromSource("\"Pack\"")))
        assertThat(function.name).isEqualTo(OracleIdentifier.fromSource("\"Make Value\""))
        assertThat(function.parameters.single().name).isEqualTo(OracleIdentifier.fromSource("\"Input\""))
        assertThat(function.parameters.single().typeRef).isEqualTo(
            NamedTypeRef(
                QualifiedName(listOf(OracleIdentifier.fromSource("\"Owner\""), OracleIdentifier.fromSource("\"Number Type\""))),
                function.parameters.single().typeRef.sourceRange
            )
        )
        assertThat(function.returnType).isEqualTo(
            NamedTypeRef(
                QualifiedName(listOf(OracleIdentifier.fromSource("\"Owner\""), OracleIdentifier.fromSource("\"Result Type\""))),
                function.returnType.sourceRange
            )
        )
    }

    @Test
    fun extractsPackageBodyCallSpecificationsAsImplementations() {
        val declarations = extractor.extract(fileId, """
            CREATE PACKAGE BODY p AS
              PROCEDURE external_work(value NUMBER) AS
                EXTERNAL NAME external_work LIBRARY native_library;
            END p;
        """.trimIndent())

        val procedure = declarations.filterIsInstance<PackageProcedureDeclaration>().single()
        assertThat(procedure.role).isEqualTo(DeclarationRole.BODY)
        assertThat(procedure.name.lookupName).isEqualTo("EXTERNAL_WORK")
        assertThat(procedure.parameters.single().typeRef).isEqualTo(
            NamedTypeRef(QualifiedName(OracleIdentifier.fromSource("NUMBER")), procedure.parameters.single().typeRef.sourceRange)
        )
    }

    @Test
    fun boundsPackageBodiesWithInitializationBeforeFollowingCreateUnits() {
        val declarations = extractor.extract(fileId, """
            CREATE PACKAGE BODY p AS
              PROCEDURE work IS
              BEGIN
                NULL;
              END work;
            BEGIN
              NULL;
            END p;

            CREATE PACKAGE q AS
              PROCEDURE q_work;
            END q;

            CREATE PACKAGE BODY q AS
              PROCEDURE q_work IS
              BEGIN
                NULL;
              END q_work;
            END q;

            CREATE TYPE after_packages AS OBJECT (id NUMBER);
        """.trimIndent())

        val procedures = declarations.filterIsInstance<PackageProcedureDeclaration>()
        assertThat(procedures.map { it.owner to it.name.lookupName })
            .containsExactly(
                QualifiedName(OracleIdentifier.fromSource("p")) to "WORK",
                QualifiedName(OracleIdentifier.fromSource("q")) to "Q_WORK",
                QualifiedName(OracleIdentifier.fromSource("q")) to "Q_WORK"
            )
        assertThat(procedures.map { it.role })
            .containsExactly(DeclarationRole.BODY, DeclarationRole.SPECIFICATION, DeclarationRole.BODY)
        assertThat(procedures[0].sourceRange.startLine).isEqualTo(2)
        assertThat(procedures[0].sourceRange.endLine).isEqualTo(5)
        assertThat(procedures[1].sourceRange.startLine).isEqualTo(11)
        assertThat(procedures[2].sourceRange.startLine).isEqualTo(15)
        assertThat(declarations.filterIsInstance<StandaloneTypeDeclaration>())
            .singleElement()
            .extracting { it.name }
            .isEqualTo(QualifiedName(OracleIdentifier.fromSource("after_packages")))
    }

    @Test
    fun doesNotEndPackageBodyAtSqlCaseExpression() {
        val declarations = extractor.extract(fileId, """
            CREATE PACKAGE BODY p AS
              PROCEDURE work IS
                result NUMBER;
              BEGIN
                SELECT CASE
                         WHEN 1 = 1 THEN 10
                         ELSE 20
                       END
                  INTO result
                  FROM dual;

                result := result + 1;
              END work;

              PROCEDURE after_work IS
              BEGIN
                NULL;
              END after_work;
            END p;
        """.trimIndent())

        val procedures = declarations.filterIsInstance<PackageProcedureDeclaration>()
        assertThat(procedures.map { it.name.lookupName }).containsExactly("WORK", "AFTER_WORK")
        assertThat(procedures[0].sourceRange.startLine).isEqualTo(2)
        assertThat(procedures[0].sourceRange.endLine).isEqualTo(13)
        assertThat(procedures[1].sourceRange.startLine).isEqualTo(15)
        assertThat(procedures[1].sourceRange.endLine).isEqualTo(18)
    }

    @Test
    fun doesNotEndPackageBodyAtDeclarativeSqlCaseExpression() {
        val declarations = extractor.extract(fileId, """
            CREATE PACKAGE BODY p AS
              initial_value NUMBER := CASE WHEN 1 = 1 THEN 1 ELSE 0 END;

              PROCEDURE work IS
              BEGIN
                NULL;
              END work;
            BEGIN
              NULL;
            END p;
        """.trimIndent())

        val procedure = declarations.filterIsInstance<PackageProcedureDeclaration>().single()
        assertThat(procedure.name.lookupName).isEqualTo("WORK")
        assertThat(procedure.sourceRange.startLine).isEqualTo(4)
        assertThat(procedure.sourceRange.endLine).isEqualTo(7)
    }

    @Test
    fun preservesPackageBoundaryWithNestedBlocksAndCaseStatements() {
        val declarations = extractor.extract(fileId, """
            CREATE PACKAGE BODY p AS
              PROCEDURE work IS
                result NUMBER;
                PROCEDURE nested_work IS
                BEGIN
                  NULL;
                END nested_work;
              BEGIN
                BEGIN
                  IF TRUE THEN
                    NULL;
                  END IF;
                  LOOP
                    EXIT;
                  END LOOP;
                  CASE
                    WHEN TRUE THEN NULL;
                    ELSE NULL;
                  END CASE;
                  result := CASE WHEN TRUE THEN 1 ELSE 0 END;
                END;
              END work;

              PROCEDURE after_work IS
              BEGIN
                NULL;
              END after_work;
            END p;
        """.trimIndent())

        val procedures = declarations.filterIsInstance<PackageProcedureDeclaration>()
        assertThat(procedures.map { it.name.lookupName }).containsExactly("WORK", "AFTER_WORK")
        assertThat(procedures).noneMatch { it.name.lookupName == "NESTED_WORK" }
        assertThat(procedures[0].sourceRange.endLine).isEqualTo(22)
        assertThat(procedures[1].sourceRange.startLine).isEqualTo(24)
    }

    @Test
    fun extractsStandaloneTypeFormsAndSkipsTypeBodies() {
        val declarations = extractor.extract(FileId("fixture/types.sql"), """
            CREATE OR REPLACE TYPE foo AS OBJECT (id NUMBER);
            /
            CREATE TYPE "Foo"."Bar" AS VARRAY(4) OF NUMBER;
            /
            CREATE TYPE table_t IS TABLE OF bar;
            /
            CREATE TYPE cursor_t AS REF CURSOR;
            /
            CREATE TYPE BODY foo AS MEMBER FUNCTION size RETURN NUMBER IS BEGIN RETURN 1; END; END;
            /
        """.trimIndent())

        val types = declarations.filterIsInstance<StandaloneTypeDeclaration>()
        assertThat(types).hasSize(4)
        assertThat(types[0].name).isEqualTo(QualifiedName(OracleIdentifier.fromSource("foo")))
        assertThat(types[0].shape).isEqualTo(ProjectTypeShape.OBJECT)
        assertThat(types[1].name).isEqualTo(
            QualifiedName(listOf(OracleIdentifier.fromSource("\"Foo\""), OracleIdentifier.fromSource("\"Bar\"")))
        )
        assertThat(types[1].shape).isEqualTo(ProjectTypeShape.COLLECTION)
        assertThat(types[2].shape).isEqualTo(ProjectTypeShape.COLLECTION)
        assertThat(types[3].shape).isEqualTo(ProjectTypeShape.REF_CURSOR)
    }

    @Test
    fun standaloneTypeFactsAgreeWithTheAstOracle() {
        val source = "CREATE OR REPLACE TYPE \"Foo\".\"Bar\" IS VARRAY(4) OF NUMBER;"
        val fileId = FileId("fixture/oracle.sql")
        val extracted = extractor.extract(fileId, source).filterIsInstance<StandaloneTypeDeclaration>().single()
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).parse(source)
        val ast = root.getDescendants(PlSqlGrammar.CREATE_TYPE).single()
        val typeIndex = ast.tokens.indexOfFirst { it.value == "TYPE" }
        val astName = ast.tokens.drop(typeIndex + 1)
            .takeWhile { it.type == GenericTokenType.IDENTIFIER || it.value == "." }
            .joinToString("") { it.originalValue }

        assertThat(extracted.name.toString()).isEqualTo(astName)
        assertThat(extracted.shape).isEqualTo(ProjectTypeShape.COLLECTION)
        assertThat(extracted.sourceRange).isEqualTo(
            SourceRange(fileId, ast.tokenOrNull!!.line, ast.tokenOrNull!!.column, ast.lastTokenOrNull!!.endLine, ast.lastTokenOrNull!!.endColumn)
        )
    }

    @Test
    fun preservesOracleIdentifierAndQualifiedNameSemantics() {
        val unquotedUpper = OracleIdentifier.fromSource("FOO")
        val unquotedLower = OracleIdentifier.fromSource("foo")
        val quotedLower = OracleIdentifier.fromSource("\"foo\"")
        val quotedMixed = OracleIdentifier.fromSource("\"Foo\"")

        assertThat(unquotedUpper).isEqualTo(unquotedLower)
        assertThat(unquotedUpper).isNotEqualTo(quotedLower)
        assertThat(quotedLower).isNotEqualTo(quotedMixed)
        assertThat(quotedMixed.originalSpelling).isEqualTo("\"Foo\"")
        assertThat(quotedMixed.lookupName).isEqualTo("Foo")
        assertThat(quotedMixed.quoted).isTrue

        assertThat(QualifiedName(listOf(unquotedUpper, quotedMixed)))
            .isEqualTo(QualifiedName(listOf(unquotedLower, OracleIdentifier.fromSource("\"Foo\""))))
        assertThat(QualifiedName(listOf(unquotedUpper, quotedMixed)))
            .isNotEqualTo(QualifiedName(listOf(unquotedUpper, quotedLower)))
    }

    @Test
    fun keepsOverloadsDistinctWithStableSignatures() {
        val declarations = extractor.extract(fileId, """
            CREATE PACKAGE p AS
              PROCEDURE work(value IN NUMBER);
              PROCEDURE work(value IN VARCHAR2);
              PROCEDURE work(value IN NUMBER, other OUT NOCOPY NUMBER);
            END p;
        """.trimIndent())

        val procedures = declarations.filterIsInstance<PackageProcedureDeclaration>()
        assertThat(procedures).hasSize(3)
        assertThat(procedures.map { it.overloadIdentity() }.distinct()).hasSize(3)
    }

    @Test
    fun separatesOverloadIdentityFromSpecificationComparisonMetadata() {
        val declarations = extractor.extract(fileId, """
            CREATE PACKAGE p AS
              PROCEDURE work(value IN OUT NOCOPY CLOB);
              PROCEDURE work(value IN OUT CLOB);
              PROCEDURE work(value IN CLOB DEFAULT NULL);
              PROCEDURE work(value IN CLOB);
            END p;
        """.trimIndent()).filterIsInstance<PackageProcedureDeclaration>()

        assertThat(declarations).hasSize(4)
        assertThat(declarations[0].overloadIdentity()).isEqualTo(declarations[1].overloadIdentity())
        assertThat(declarations[0].headerIdentity()).isEqualTo(declarations[1].headerIdentity())
        assertThat(declarations[0].parameters.single().nocopy).isTrue
        assertThat(declarations[1].parameters.single().nocopy).isFalse
        assertThat(declarations[2].overloadIdentity()).isEqualTo(declarations[3].overloadIdentity())
        assertThat(declarations[2].headerIdentity()).isEqualTo(declarations[3].headerIdentity())
        assertThat(declarations[2].parameters.single().defaultPresent).isTrue
        assertThat(declarations[3].parameters.single().defaultPresent).isFalse

        val body = PackageProcedureDeclaration(
            declarations[0].owner,
            declarations[0].name,
            declarations[0].parameters.map { it.copy(nocopy = false) },
            declarations[0].fileId,
            declarations[0].sourceRange,
            DeclarationRole.BODY
        )
        assertThat(declarations[0].headerIdentity()).isEqualTo(body.headerIdentity())
    }

    @Test
    fun preservesOracleNameOnlyOverloads() {
        val declarations = extractor.extract(fileId, """
            CREATE PACKAGE p AS
              PROCEDURE work(left IN CLOB);
              PROCEDURE work(right IN CLOB);
            END p;
        """.trimIndent()).filterIsInstance<PackageProcedureDeclaration>()

        assertThat(declarations).hasSize(2)
        assertThat(declarations[0].overloadIdentity()).isNotEqualTo(declarations[1].overloadIdentity())
    }

    @Test
    fun keepsModeOutOfOverloadIdentityButInHeaderCorrelation() {
        val declarations = extractor.extract(fileId, """
            CREATE PACKAGE p AS
              PROCEDURE work(value IN CLOB);
              PROCEDURE work(value OUT CLOB);
            END p;
        """.trimIndent()).filterIsInstance<PackageProcedureDeclaration>()

        assertThat(declarations[0].overloadIdentity()).isEqualTo(declarations[1].overloadIdentity())
        assertThat(declarations[0].headerIdentity()).isNotEqualTo(declarations[1].headerIdentity())
    }

    @Test
    fun typeReferenceIdentityIgnoresUnquotedSpellingButPreservesQuotedSpelling() {
        val range = SourceRange(fileId, 1, 0, 1, 5)
        val upper = NamedTypeRef(
            QualifiedName(listOf(OracleIdentifier.fromSource("PKG"), OracleIdentifier.fromSource("TYPE_NAME"))),
            range
        )
        val lower = NamedTypeRef(
            QualifiedName(listOf(OracleIdentifier.fromSource("pkg"), OracleIdentifier.fromSource("type_name"))),
            range
        )
        val quoted = NamedTypeRef(
            QualifiedName(listOf(OracleIdentifier.fromSource("\"Pkg\""), OracleIdentifier.fromSource("\"Type_Name\""))),
            range
        )

        assertThat(upper.structuralKey()).isEqualTo(lower.structuralKey())
        assertThat(upper.structuralKey()).isNotEqualTo(quoted.structuralKey())
        assertThat(AnchoredTypeRef(lower.name, TypeAnchor.TYPE, range).structuralKey())
            .isEqualTo(AnchoredTypeRef(upper.name, TypeAnchor.TYPE, range).structuralKey())

        val extracted = extractor.extract(fileId, """
            CREATE PACKAGE p AS
              SUBTYPE first IS PKG.TYPE_NAME;
              SUBTYPE second IS pkg . type_name;
            END p;
        """.trimIndent()).filterIsInstance<PackageSubtypeDeclaration>()
        assertThat(extracted[0].baseType.structuralKey()).isEqualTo(extracted[1].baseType.structuralKey())
    }

    @Test
    fun extractsQualifiedAndAnchoredTypeReferences() {
        val declarations = extractor.extract(fileId, """
            CREATE PACKAGE p AS
              SUBTYPE plain IS NUMBER;
              SUBTYPE qualified IS PKG.TYPE_NAME;
              SUBTYPE quoted IS "Pkg"."Type";
              SUBTYPE anchored IS "Pkg"."X"%ROWTYPE;
              PROCEDURE work(a IN x%TYPE, b IN pkg.x%ROWTYPE, c IN "Pkg"."X"%TYPE);
            END p;
        """.trimIndent())

        val subtypes = declarations.filterIsInstance<PackageSubtypeDeclaration>()
        assertThat(subtypes.map { it.baseType }).allMatch { it is NamedTypeRef || it is AnchoredTypeRef }
        assertThat(subtypes[1].baseType.name.segments).hasSize(2)
        assertThat(subtypes[2].baseType.name.segments.map { it.quoted }).containsExactly(true, true)
        assertThat((subtypes[3].baseType as AnchoredTypeRef).anchor).isEqualTo(TypeAnchor.ROWTYPE)
        val parameters = declarations.filterIsInstance<PackageProcedureDeclaration>().single().parameters
        assertThat(parameters.map { it.typeRef }).allMatch { it is NamedTypeRef || it is AnchoredTypeRef }
        assertThat((parameters[0].typeRef as AnchoredTypeRef).anchor).isEqualTo(TypeAnchor.TYPE)
        assertThat((parameters[1].typeRef as AnchoredTypeRef).anchor).isEqualTo(TypeAnchor.ROWTYPE)
        assertThat(parameters[2].typeRef.name.segments.map { it.quoted }).containsExactly(true, true)
    }
}
