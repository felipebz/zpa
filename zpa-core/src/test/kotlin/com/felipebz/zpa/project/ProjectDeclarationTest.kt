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
    fun extractsPackageDeclarationsWithoutExposingBodyDeclarations() {
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
        assertThat(procedures).hasSize(2)
        assertThat(procedures.map { it.parameters.size }).containsExactly(3, 1)
        assertThat(procedures.first().parameters.map { it.ordinal }).containsExactly(1, 2, 3)
        assertThat(procedures.first().parameters.map { it.mode }).containsExactly(ParameterMode.IN, ParameterMode.OUT, ParameterMode.IN)
        assertThat(procedures.first().parameters[1].nocopy).isTrue
        assertThat(procedures.first().parameters[2].defaultPresent).isTrue

        val function = declarations.filterIsInstance<PackageFunctionDeclaration>().single()
        assertThat(function.parameters.single().mode).isEqualTo(ParameterMode.IN_OUT)
        assertThat(function.parameters.single().nocopy).isTrue
        assertThat(function.parameters.single().defaultPresent).isTrue
        assertThat(function.returnType).isInstanceOf(AnchoredTypeRef::class.java)

        assertThat(declarations).noneMatch { declaration ->
            declaration is PackageSubprogramDeclaration && declaration.name.lookupName == "PRIVATE_BODY_DECLARATION"
        }
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
