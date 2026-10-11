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
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/**
 * Direct element references of standalone `CREATE TYPE` collections. Supported syntax is compared with the parser
 * (error recovery disabled); unsupported syntax or unreliable unit boundaries must yield no element.
 */
class StandaloneCollectionElementTest {
    private val fileId = FileId("fixture/standalone.sql")
    private val extractor = ProjectDeclarationExtractor()

    private fun types(source: String) = extractor.extract(fileId, source).filterIsInstance<StandaloneTypeDeclaration>()
    private fun parse(source: String) = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(source)
    private fun id(source: String) = OracleIdentifier.fromSource(source)

    private data class Element(val kind: String, val name: String, val range: SourceRange)

    private fun describe(ref: TypeRef) = Element(
        when (ref) {
            is NamedTypeRef -> "NAMED"
            is AnchoredTypeRef -> "ANCHORED:${ref.anchor}"
            is RefTypeRef -> "REF"
        },
        ref.name.segments.joinToString(".") { it.originalSpelling }, ref.sourceRange
    )

    private fun elementFromAst(datatype: AstNode): Element {
        val tokens = datatype.tokens
        val first = datatype.getFirstChild().type
        val ref = first == PlSqlGrammar.REF_DATATYPE
        val anchored = first == PlSqlGrammar.ANCHORED_DATATYPE
        val named = (if (ref) tokens.drop(1) else tokens).takeWhile { it.value != "%" }
            .let { list -> if (anchored || ref || first == PlSqlGrammar.CUSTOM_DATATYPE) list else list.take(1) }
            .filter { it.value != "." }
        val kind = when {
            ref -> "REF"
            anchored -> "ANCHORED:${tokens.last().value.uppercase()}"
            else -> "NAMED"
        }
        return Element(kind, named.joinToString(".") { it.originalValue }, SourceRange(fileId, tokens.first().line, tokens.first().column, tokens.last().endLine, tokens.last().endColumn))
    }

    /** Type name (as written) to the element `DATATYPE` of every standalone collection `CREATE TYPE`. */
    private fun astElements(source: String): Map<String, Element> = parse(source).getDescendants(PlSqlGrammar.CREATE_TYPE).mapNotNull { create ->
        val datatype = create.getFirstChildOrNull(PlSqlGrammar.DATATYPE)
            ?: (create.getFirstChildOrNull(PlSqlGrammar.NESTED_TABLE_DEFINITION) ?: create.getFirstChildOrNull(PlSqlGrammar.VARRAY_TYPE_DEFINITION))
                ?.getFirstChild(PlSqlGrammar.DATATYPE)
        datatype?.let { create.getFirstChild(PlSqlGrammar.UNIT_NAME).tokens.joinToString("") { token -> token.originalValue } to elementFromAst(it) }
    }.toMap()

    private fun assertAgreesWithAst(source: String, expectedCount: Int) {
        val expected = astElements(source)
        assertThat(expected).describedAs(source).hasSize(expectedCount)
        val facts = types(source).filter { it.name.segments.joinToString(".") { s -> s.originalSpelling } in expected }
        assertThat(facts).hasSize(expectedCount)
        facts.forEach { fact ->
            val name = fact.name.segments.joinToString(".") { it.originalSpelling }
            assertThat(fact.shape).describedAs(name).isEqualTo(ProjectTypeShape.COLLECTION)
            assertThat(fact.elementType?.let(::describe)).describedAs(name).isEqualTo(expected.getValue(name))
            assertThat(fact.role).isEqualTo(DeclarationRole.STANDALONE)
            assertThat(fact.kind).isEqualTo(ProjectDeclarationKind.STANDALONE_TYPE)
            assertThat(fact.fileId).isEqualTo(fileId)
        }
    }

    @Test
    fun supportedStandaloneCollectionsAgreeWithTheParser() {
        assertAgreesWithAst(
            """
            CREATE TYPE a AS TABLE OF NUMBER;
            /
            CREATE TYPE b IS TABLE OF VARCHAR2(30);
            /
            CREATE TYPE c AS VARRAY(5) OF rec_t;
            /
            CREATE TYPE d AS ARRAY(5) OF NUMBER;
            /
            CREATE TYPE e AS VARYING ARRAY(5) OF app.obj_t;
            /
            CREATE TYPE f AS TABLE OF emp%ROWTYPE;
            /
            CREATE TYPE g AS TABLE OF (NUMBER NOT NULL) NOT PERSISTABLE;
            /
            CREATE OR REPLACE TYPE app.h AS TABLE OF REF obj_t;
            /
            CREATE TYPE i AS TABLE OF emp.sal%TYPE NOT NULL;
            /
            CREATE TYPE j AS VARRAY(10) OF (NUMBER(5,2)) PERSISTABLE;
            /
            CREATE TYPE "K" AS TABLE OF "Elem";
            /
            CREATE TYPE l AS TABLE OF TIMESTAMP(3) WITH TIME ZONE;
            /
            CREATE EDITIONABLE TYPE m AS VARRAY(c_max) OF q.t NULL;
            /
            """.trimIndent(),
            13
        )
    }

    @Test
    fun boundsNullabilityAndPersistabilityAreNeverPartOfTheElement() {
        val facts = types("CREATE TYPE g AS TABLE OF (NUMBER(5,2) NOT NULL) NOT PERSISTABLE;\n/\nCREATE TYPE v AS VARRAY(7) OF VARCHAR2(10) NOT NULL;").associateBy { it.name.last.originalSpelling }
        assertThat(facts.getValue("g").elementType!!.sourceRange).isEqualTo(SourceRange(fileId, 1, 27, 1, 38))
        assertThat(facts.getValue("v").elementType!!.sourceRange).isEqualTo(SourceRange(fileId, 3, 30, 3, 42))
        assertThat(facts.getValue("v").elementType!!.name).isEqualTo(QualifiedName(id("VARCHAR2")))
    }

    @Test
    fun arrayHeadsAreCollectionsAndOtherShapesAreUnchanged() {
        val facts = types(
            """
            CREATE TYPE a AS VARYING ARRAY(5) OF NUMBER;
            /
            CREATE TYPE o AS OBJECT (arr_x NUMBER, t_array ids);
            /
            CREATE TYPE bad AS ARRAY OF NUMBER;
            /
            """.trimIndent()
        ).associateBy { it.name.last.originalSpelling }
        assertThat(facts.getValue("a").shape).isEqualTo(ProjectTypeShape.COLLECTION)
        assertThat(facts.getValue("a").elementType).isNotNull()
        assertThat(facts.getValue("o").shape).isEqualTo(ProjectTypeShape.OBJECT)
        assertThat(facts.getValue("o").elementType).isNull()
        assertThat(facts.getValue("bad").shape).isNull()
    }

    @Test
    fun unsupportedOrMalformedDeclarationsKeepTheFactWithoutAnElement() {
        listOf(
            "CREATE TYPE t FORCE AS TABLE OF NUMBER;",
            "CREATE TYPE t AUTHID DEFINER AS TABLE OF NUMBER;",
            "CREATE TYPE t AS TABLE OF t2@link;",
            "CREATE TYPE t AS TABLE OF ;",
            "CREATE TYPE t AS TABLE OF NUMBER NUMBER;",
            "CREATE TYPE t AS TABLE OF f(a NUMBER);",
            "CREATE TYPE t AS TABLE OF NUMBER INDEX BY PLS_INTEGER;",
            "CREATE TYPE t AS VARRAY(x y) OF NUMBER;",
            "CREATE TYPE t AS VARRAY() OF NUMBER;",
            "CREATE TYPE t AS VARRAY(5) NUMBER;",
            "CREATE TYPE t AS TABLE OF (NUMBER;",
            "CREATE TYPE t AS TABLE OF (NUMBER) NOT;",
            "CREATE TYPE t AS TABLE OF (NUMBER) PERSISTABLE NUMBER;",
            "CREATE TYPE t AS TABLE OF () NOT PERSISTABLE;",
            "CREATE TYPE t AS TABLE OF (NUMBER NOT) NOT PERSISTABLE;"
        ).forEach { declaration ->
            val source = "$declaration\n/\nCREATE SEQUENCE s;"
            val facts = extractor.extract(fileId, source)
            val type = facts.filterIsInstance<StandaloneTypeDeclaration>().singleOrNull { it.name.last.originalSpelling == "t" }
            assertThat(type?.elementType).describedAs(declaration).isNull()
            assertThat(type).describedAs(declaration).isNotNull()
        }
    }

    @Test
    fun anElementIsNeverAddedWhenTheDeclarationRunsIntoAnotherUnit() {
        // No terminating semicolon before a lone `/`: the existing fact range is unreliable, so there is no element.
        val missing = types("CREATE TYPE t AS TABLE OF NUMBER\n/\nCREATE TYPE u AS TABLE OF DATE;\n/\n")
        assertThat(missing.first { it.name.last.originalSpelling == "t" }.elementType).isNull()
        // The same without a lone `/`: the next CREATE bounds the unit.
        val adjacent = types("CREATE TYPE t AS TABLE OF NUMBER\nCREATE TYPE u AS TABLE OF DATE;")
        assertThat(adjacent.first { it.name.last.originalSpelling == "t" }.elementType).isNull()
        // A unit that ends at EOF without a semicolon is reliably delimited and the parser accepts it.
        val eof = "CREATE TYPE t AS TABLE OF NUMBER"
        assertAgreesWithAst(eof, 1)
        // A valid unit followed by others keeps its element and the following facts.
        val ok = types("CREATE TYPE t AS TABLE OF NUMBER;\nCREATE TYPE u AS VARRAY(3) OF DATE;")
        assertThat(ok.map { it.elementType!!.name.last.originalSpelling }).containsExactly("NUMBER", "DATE")
        assertThat(ok[0].sourceRange.endLine).isEqualTo(1)
    }

    @Test
    fun factInvariantEqualityAndOrdering() {
        val range = SourceRange(fileId, 1, 0, 1, 10)
        val element = NamedTypeRef(QualifiedName(id("NUMBER")), range)
        fun type(shape: ProjectTypeShape?, element: TypeRef?) = StandaloneTypeDeclaration(QualifiedName(id("t")), shape, fileId, range, element)
        assertThatThrownBy { type(ProjectTypeShape.OBJECT, element) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { type(null, element) }.isInstanceOf(IllegalArgumentException::class.java)
        val a = type(ProjectTypeShape.COLLECTION, element)
        assertThat(a).isEqualTo(type(ProjectTypeShape.COLLECTION, NamedTypeRef(QualifiedName(id("NUMBER")), range)))
        assertThat(a.hashCode()).isEqualTo(type(ProjectTypeShape.COLLECTION, element).hashCode())
        assertThat(a).isNotEqualTo(type(ProjectTypeShape.COLLECTION, null))
        assertThat(StandaloneTypeDeclaration(QualifiedName(id("t")), ProjectTypeShape.COLLECTION, fileId, range)).isEqualTo(type(ProjectTypeShape.COLLECTION, null))
    }

    private fun prepare(concurrent: Boolean, files: List<Pair<String, String>>) = ProjectIndexPreparation().prepare(
        files.map { (name, text) -> ProjectSource(FileId(name), ProjectSourceReader { text }) }, concurrent
    )

    @Test
    fun elementMetadataDoesNotChangeLookupDuplicatesOrDeterminism() {
        val files = listOf(
            "b.sql" to "CREATE TYPE t AS TABLE OF NUMBER;\n/\nCREATE TYPE o AS OBJECT (a NUMBER);",
            "a.sql" to "CREATE TYPE t AS TABLE OF DATE;\n/\nCREATE TYPE t AS TABLE OF DATE;"
        )
        val serial = prepare(false, files).index
        listOf(prepare(true, files).index, prepare(false, files.reversed()).index).forEach { other ->
            assertThat(other.declarations).isEqualTo(serial.declarations)
            assertThat(other.occurrences.map { it.fileId.value to it.ordinal }).isEqualTo(serial.occurrences.map { it.fileId.value to it.ordinal })
        }
        val found = serial.findTypes(null, id("t")).filterIsInstance<StandaloneTypeDeclaration>()
        assertThat(found).hasSize(3)
        assertThat(found.map { (it.elementType as NamedTypeRef).name.last.originalSpelling }).containsExactly("DATE", "DATE", "NUMBER")
        assertThat(serial.findTypes(null, id("o")).single()).isInstanceOf(StandaloneTypeDeclaration::class.java)
    }
}
