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

/** Legacy package body walking, with the extractor built from the shared per-extraction components. */
class PackageBodyExtractorTest {
    private val fileId = FileId("body.sql")
    private val owner = QualifiedName(OracleIdentifier.fromSource("p"))

    private class Fixture(text: String, fileId: FileId) {
        val stream = TokenStream(fileId, PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).lex(text))
        private val references = TypeReferences(stream)
        val extractor = PackageBodyExtractor(stream, BodyBoundaryScanner(stream), LegacyMemberParser(stream, references), references)
    }

    /** Parses `CREATE PACKAGE BODY p AS ...`: the name `p` is the fourth token, the rest starts right after it. */
    private fun body(text: String): Pair<ParsedUnit?, Fixture> {
        val fixture = Fixture(text, fileId)
        return fixture.extractor.parsePackageBody(owner, 4) to fixture
    }

    @Test
    fun procedureAndFunctionAreExtractedInSourceOrderWithBodyRole() {
        val text = "CREATE PACKAGE BODY p AS\n  PROCEDURE q(a IN NUMBER, b OUT NOCOPY VARCHAR2 DEFAULT 'x', c IN OUT t.x%TYPE) IS BEGIN NULL; END;\n" +
            "  FUNCTION f(a NUMBER) RETURN VARCHAR2(10) DETERMINISTIC IS BEGIN RETURN 'x'; END f;\nEND p;"
        val (unit, fixture) = body(text)
        val facts = unit!!.declarations
        assertThat(facts.map { it.kind }).containsExactly(ProjectDeclarationKind.PACKAGE_PROCEDURE, ProjectDeclarationKind.PACKAGE_FUNCTION)
        assertThat(facts.map { it.role }).containsOnly(DeclarationRole.BODY)
        assertThat(facts.map { it.fileId }).containsOnly(fileId)
        val q = facts[0] as PackageProcedureDeclaration
        assertThat(q.owner).isEqualTo(owner)
        assertThat(q.name.originalSpelling).isEqualTo("q")
        assertThat(q.sourceRange).isEqualTo(SourceRange(fileId, 2, 2, 2, 100))
        assertThat(q.parameters.map { it.ordinal }).containsExactly(1, 2, 3)
        assertThat(q.parameters.map { it.mode }).containsExactly(ParameterMode.IN, ParameterMode.OUT, ParameterMode.IN_OUT)
        assertThat(q.parameters.map { it.nocopy }).containsExactly(false, true, false)
        assertThat(q.parameters.map { it.defaultPresent }).containsExactly(false, true, false)
        assertThat(q.parameters[2].typeRef).isInstanceOf(AnchoredTypeRef::class.java)
        val f = facts[1] as PackageFunctionDeclaration
        assertThat((f.returnType as NamedTypeRef).name.toString()).isEqualTo("VARCHAR2")
        assertThat(f.deterministic).describedAs("body headings use the package terminator list").isFalse()
        assertThat(f.sourceRange.startLine).isEqualTo(3)
        // The unit resumes after the package terminator.
        assertThat(unit.nextIndex).isEqualTo(fixture.stream.tokens.size - 1)
    }

    @Test
    fun forwardDeclarationsOverloadsAndNestedSubprograms() {
        val text = "CREATE PACKAGE BODY p AS\n  PROCEDURE fwd(a NUMBER);\n  PROCEDURE o IS BEGIN NULL; END;\n  PROCEDURE o(a NUMBER) IS\n" +
            "    PROCEDURE local IS BEGIN NULL; END;\n    FUNCTION lf RETURN NUMBER IS BEGIN RETURN CASE WHEN 1 = 1 THEN 1 END; END;\n" +
            "  BEGIN local; CASE a WHEN 1 THEN NULL; END CASE; IF a > 1 THEN NULL; END IF; END;\n  PROCEDURE fwd(a NUMBER) IS BEGIN NULL; END;\nEND p;"
        val facts = body(text).first!!.declarations
        assertThat(facts.map { it.qualifiedName().toString() }).containsExactly("p.o", "p.o", "p.fwd")
        assertThat(facts.map { (it as PackageProcedureDeclaration).parameters.size }).containsExactly(0, 1, 1)
        assertThat(facts.map { it.sourceRange.startLine }).containsExactly(3, 4, 8)
    }

    @Test
    fun initializationSectionEndsTheWalkAndMissingHeaderYieldsNull() {
        val text = "CREATE PACKAGE BODY p AS\n  PROCEDURE q IS BEGIN NULL; END;\nBEGIN\n  NULL;\nEND p;"
        val (unit, fixture) = body(text)
        assertThat(unit!!.declarations).hasSize(1)
        assertThat(unit.nextIndex).isEqualTo(fixture.stream.tokens.size - 1)
        assertThat(body("CREATE PACKAGE BODY p").first).isNull()
        assertThat(body("CREATE PACKAGE BODY p END p;").first).describedAs("no IS/AS").isNull()
        assertThat(body("CREATE PACKAGE BODY p AS END p;").first!!.declarations).isEmpty()
    }

    @Test
    fun theFacadeExtractsSeveralIndependentUnitsAndKeepsSpecificationAndBodyRoles() {
        val facts = ProjectDeclarationExtractor().extract(
            fileId,
            "CREATE PACKAGE a AS PROCEDURE x; END a;\n/\nCREATE PACKAGE BODY a AS PROCEDURE x IS BEGIN NULL; END; END a;\n/\n" +
                "CREATE PACKAGE BODY b AS FUNCTION y RETURN NUMBER IS BEGIN RETURN 1; END; END b;\n/\nCREATE SEQUENCE s;"
        )
        assertThat(facts.map { it.role to it.qualifiedName().toString() }).containsExactly(
            DeclarationRole.SPECIFICATION to "a", DeclarationRole.SPECIFICATION to "a.x",
            DeclarationRole.BODY to "a.x", DeclarationRole.BODY to "b.y", DeclarationRole.STANDALONE to "s"
        )
    }
}
