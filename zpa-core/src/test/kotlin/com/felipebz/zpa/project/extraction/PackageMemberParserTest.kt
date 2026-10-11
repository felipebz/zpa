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
package com.felipebz.zpa.project.extraction

import com.felipebz.zpa.lexer.PlSqlLexer
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/** Package member parsers called directly on hand-lexed fragments, and through a specification and a body. */
class PackageMemberParserTest {
    private val fileId = FileId("members.sql")
    private val owner = QualifiedName(OracleIdentifier.fromSource("p"))

    private class Parser(val stream: TokenStream, val parser: PackageMemberParser)

    private fun parser(text: String): Parser {
        val tokens = PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).lex(text)
        val stream = TokenStream(fileId, tokens)
        return Parser(stream, PackageMemberParser(stream, TypeReferences(stream)))
    }

    @Test
    fun packageTypesCarryShapeRecordFieldsAndRanges() {
        val p = parser("TYPE r IS RECORD (a NUMBER := 1, \"B c\" t.x%TYPE, d VARCHAR2(5) NOT NULL);")
        val parsed = p.parser.parsePackageType(0, owner)!!
        val type = parsed.declaration as PackageTypeDeclaration
        assertThat(type.shape).isEqualTo(ProjectTypeShape.RECORD)
        assertThat(type.name.originalSpelling).isEqualTo("r")
        assertThat(type.sourceRange).isEqualTo(SourceRange(fileId, 1, 0, 1, 73))
        assertThat(parsed.nextIndex).isEqualTo(p.stream.tokens.size - 1)
        // Record-field ordinals are zero-based.
        assertThat(type.recordFields.map { it.ordinal }).containsExactly(0, 1, 2)
        assertThat(type.recordFields.map { it.name.originalSpelling }).containsExactly("a", "\"B c\"", "d")
        assertThat(type.recordFields[1].typeRef).isInstanceOf(AnchoredTypeRef::class.java)
        assertThat(type.recordFields[0].sourceRange).isEqualTo(SourceRange(fileId, 1, 18, 1, 31))
        listOf(
            "TYPE o AS OBJECT (a NUMBER);" to ProjectTypeShape.OBJECT,
            "TYPE t IS TABLE OF NUMBER;" to ProjectTypeShape.COLLECTION,
            "TYPE v IS VARRAY(3) OF NUMBER;" to ProjectTypeShape.COLLECTION,
            "TYPE c IS REF CURSOR;" to ProjectTypeShape.REF_CURSOR
        ).forEach { (text, shape) ->
            val declaration = parser(text).parser.parsePackageType(0, owner)!!.declaration as PackageTypeDeclaration
            assertThat(declaration.shape).describedAs(text).isEqualTo(shape)
            assertThat(declaration.recordFields).describedAs(text).isEmpty()
        }
        assertThat(parser("TYPE ;").parser.parsePackageType(0, owner)).isNull()
    }

    @Test
    fun packageSubtypesResolveTheirBaseTypeUpToRange() {
        val sub = parser("SUBTYPE s IS t.c%TYPE;").parser.parsePackageSubtype(0, owner)!!.declaration as PackageSubtypeDeclaration
        assertThat(sub.baseType).isInstanceOf(AnchoredTypeRef::class.java)
        val ranged = parser("SUBTYPE s IS sch.num RANGE 1 .. 10;").parser.parsePackageSubtype(0, owner)!!.declaration as PackageSubtypeDeclaration
        assertThat((ranged.baseType as NamedTypeRef).name.toString()).isEqualTo("sch.num")
        assertThat(ranged.baseType.sourceRange).isEqualTo(SourceRange(fileId, 1, 13, 1, 20))
        assertThat(parser("SUBTYPE s;").parser.parsePackageSubtype(0, owner)).isNull()
    }

    @Test
    fun procedureParametersKeepModesNocopyDefaultsAndOneBasedOrdinals() {
        val p = parser("PROCEDURE q(a IN NUMBER, b OUT NOCOPY VARCHAR2, c IN OUT t.x%TYPE, d NUMBER DEFAULT f(1, 2), \"E\" REF o, g NUMBER := 1);")
        val parsed = p.parser.parseSubprogram(0, owner)!!
        val procedure = parsed.declaration as PackageProcedureDeclaration
        assertThat(procedure.role).isEqualTo(DeclarationRole.SPECIFICATION)
        assertThat(procedure.fileId).isEqualTo(fileId)
        assertThat(procedure.parameters.map { it.ordinal }).containsExactly(1, 2, 3, 4, 5, 6)
        assertThat(procedure.parameters.map { it.name.originalSpelling }).containsExactly("a", "b", "c", "d", "\"E\"", "g")
        assertThat(procedure.parameters.map { it.mode }).containsExactly(
            ParameterMode.IN, ParameterMode.OUT, ParameterMode.IN_OUT, ParameterMode.IN, ParameterMode.IN, ParameterMode.IN
        )
        assertThat(procedure.parameters.map { it.nocopy }).containsExactly(false, true, false, false, false, false)
        assertThat(procedure.parameters.map { it.defaultPresent }).containsExactly(false, false, false, true, false, true)
        assertThat(procedure.parameters[4].typeRef).isInstanceOf(RefTypeRef::class.java)
        assertThat(procedure.parameters[3].typeRef.sourceRange).isEqualTo(SourceRange(fileId, 1, 69, 1, 75))
        assertThat(procedure.parameters[0].sourceRange).isEqualTo(SourceRange(fileId, 1, 12, 1, 23))
        assertThat(parsed.nextIndex).isEqualTo(p.stream.tokens.size - 1)
    }

    @Test
    fun functionsCarryReturnTypeAndDeterministicModifier() {
        val f = parser("FUNCTION f(a NUMBER) RETURN VARCHAR2(10) DETERMINISTIC;").parser.parseSubprogram(0, owner)!!.declaration as PackageFunctionDeclaration
        assertThat(f.deterministic).isTrue()
        assertThat(f.returnType.sourceRange).isEqualTo(SourceRange(fileId, 1, 28, 1, 40))
        assertThat(f.parameters).hasSize(1)
        val g = parser("FUNCTION g RETURN t.c%TYPE;").parser.parseSubprogram(0, owner)!!.declaration as PackageFunctionDeclaration
        assertThat(g.deterministic).isFalse()
        assertThat(g.parameters).isEmpty()
        assertThat(g.returnType).isInstanceOf(AnchoredTypeRef::class.java)
        assertThat(parser("FUNCTION h(a NUMBER);").parser.parseSubprogram(0, owner)).describedAs("no RETURN").isNull()
        assertThat(parser("PROCEDURE ;").parser.parseSubprogram(0, owner)).isNull()
    }

    @Test
    fun parameterListsSplitOnlyOnTopLevelCommasAndAllowEmptyLists() {
        val p = parser("PROCEDURE q(a t(1, 2), b f((1, 2), [3, 4]));")
        val q = p.parser.parseSubprogram(0, owner)!!.declaration as PackageProcedureDeclaration
        assertThat(q.parameters.map { it.name.originalSpelling }).containsExactly("a", "b")
        val empty = parser("PROCEDURE q();").parser.parseSubprogram(0, owner)!!.declaration as PackageProcedureDeclaration
        assertThat(empty.parameters).isEmpty()
        val open = p.stream.tokens.indexOfFirst { it.value == "(" }
        val close = p.stream.tokens.indexOfLast { it.value == ")" }
        assertThat(p.parser.parseParameters(open + 1, close)).hasSize(2)
    }

    @Test
    fun theSameParserServesSpecificationsAndBodies() {
        val spec = ProjectDeclarationExtractor().extract(fileId, "CREATE PACKAGE p AS PROCEDURE q(a IN NUMBER, b OUT NOCOPY VARCHAR2 DEFAULT 'x'); END p;")
            .filterIsInstance<PackageProcedureDeclaration>().single()
        val body = ProjectDeclarationExtractor().extract(fileId, "CREATE PACKAGE BODY p AS PROCEDURE q(a IN NUMBER, b OUT NOCOPY VARCHAR2 DEFAULT 'x') IS BEGIN NULL; END; END p;")
            .filterIsInstance<PackageProcedureDeclaration>().single()
        assertThat(spec.role).isEqualTo(DeclarationRole.SPECIFICATION)
        assertThat(body.role).isEqualTo(DeclarationRole.BODY)
        assertThat(body.parameters.map { listOf(it.name, it.ordinal, it.mode, it.nocopy, it.defaultPresent) })
            .isEqualTo(spec.parameters.map { listOf(it.name, it.ordinal, it.mode, it.nocopy, it.defaultPresent) })
        assertThat(spec.parameters.map { it.ordinal }).containsExactly(1, 2)
        val function = ProjectDeclarationExtractor().extract(fileId, "CREATE PACKAGE BODY p AS FUNCTION f(a NUMBER) RETURN NUMBER DETERMINISTIC IS BEGIN RETURN 1; END; END p;")
            .filterIsInstance<PackageFunctionDeclaration>().single()
        assertThat(function.deterministic).describedAs("body function heading uses FUNCTION_RETURN_TYPE_TERMINATORS").isFalse()
        assertThat((function.returnType as NamedTypeRef).name.toString()).isEqualTo("NUMBER")
    }
}
