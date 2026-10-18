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
 * Direct element references of package collections. Supported syntax is compared with the parser (error recovery
 * disabled); anything else must yield a collection fact without an element.
 */
class PackageCollectionElementTest {
    private val fileId = FileId("fixture/collections.sql")
    private val extractor = ProjectDeclarationExtractor()

    private fun types(source: String) = extractor.extract(fileId, source).filterIsInstance<PackageTypeDeclaration>()
    private fun parse(source: String) = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(source)
    private fun spec(vararg declarations: String) = "CREATE PACKAGE p AS\n${declarations.joinToString("\n") { "  $it" }}\nEND p;"
    private fun id(source: String) = OracleIdentifier.fromSource(source)

    private data class Element(val kind: String, val name: String, val range: SourceRange, val link: String? = null)

    private fun describe(ref: TypeRef) = Element(
        when (ref) {
            is NamedTypeRef -> "NAMED"
            is AnchoredTypeRef -> "ANCHORED:${ref.anchor}"
            is RefTypeRef -> "REF"
        },
        ref.name.segments.joinToString(".") { it.originalSpelling }, ref.sourceRange,
        (ref as? AnchoredTypeRef)?.link?.segments?.joinToString(".") { it.originalSpelling }
    )

    private fun elementFromAst(datatype: AstNode): Element {
        val tokens = datatype.tokens
        val ref = datatype.getFirstChild().type == PlSqlGrammar.REF_DATATYPE
        val anchored = datatype.getFirstChild().type == PlSqlGrammar.ANCHORED_DATATYPE
        val names = tokens.drop(if (ref) 1 else 0).takeWhile { it.value != "%" && it.value != "@" }.filter { it.value != "." }
            .let { list -> if (anchored || ref || datatype.getFirstChild().type == PlSqlGrammar.CUSTOM_DATATYPE) list else list.take(1) }
        val link = tokens.dropWhile { it.value != "@" }.drop(1).takeWhile { it.value != "%" }.filter { it.value != "." }
            .takeIf { it.isNotEmpty() }?.joinToString(".") { it.originalValue }
        val kind = when {
            ref -> "REF"
            anchored -> "ANCHORED:${tokens.last().value.uppercase()}"
            else -> "NAMED"
        }
        return Element(kind, names.joinToString(".") { it.originalValue }, SourceRange(fileId, tokens.first().line, tokens.first().column, tokens.last().endLine, tokens.last().endColumn), link)
    }

    /** Collections that are direct members of a specification's declare section, with the element datatype node. */
    private fun astElements(source: String): Map<String, Element> = parse(source).getDescendants(PlSqlGrammar.CREATE_PACKAGE).flatMap { pkg ->
        pkg.getFirstChild(PlSqlGrammar.DECLARE_SECTION).children.mapNotNull { declaration ->
            val definition = declaration.getFirstChildOrNull(PlSqlGrammar.NESTED_TABLE_DEFINITION)
                ?: declaration.getFirstChildOrNull(PlSqlGrammar.VARRAY_TYPE_DEFINITION)
            definition?.let { declaration.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenOriginalValue to elementFromAst(it.getFirstChild(PlSqlGrammar.DATATYPE)) }
        }
    }.toMap()

    private fun assertAgreesWithAst(source: String, expectedCount: Int) {
        val expected = astElements(source)
        assertThat(expected).describedAs(source).hasSize(expectedCount)
        val facts = types(source).filter { it.name.originalSpelling in expected }
        assertThat(facts.map { it.name.originalSpelling }).containsExactlyInAnyOrderElementsOf(expected.keys)
        facts.forEach { fact ->
            assertThat(fact.shape).describedAs(fact.name.originalSpelling).isEqualTo(ProjectTypeShape.COLLECTION)
            assertThat(fact.recordFields).isEmpty()
            assertThat(fact.elementType?.let(::describe)).describedAs(fact.name.originalSpelling).isEqualTo(expected.getValue(fact.name.originalSpelling))
            assertThat(fact.owner).isEqualTo(QualifiedName(id("p")))
            assertThat(fact.role).isEqualTo(DeclarationRole.SPECIFICATION)
            assertThat(fact.kind).isEqualTo(ProjectDeclarationKind.PACKAGE_TYPE)
        }
    }

    @Test
    fun supportedCollectionHeadsAndDatatypesAgreeWithTheParser() {
        assertAgreesWithAst(
            spec(
                "TYPE rec_t IS RECORD(n NUMBER);",
                "TYPE a IS TABLE OF rec_t;",
                "TYPE b IS TABLE OF NUMBER(5,2);",
                "TYPE c IS TABLE OF VARCHAR2(30 CHAR) NOT NULL;",
                "TYPE d IS TABLE OF emp%ROWTYPE;",
                "TYPE e IS TABLE OF emp.sal%TYPE INDEX BY PLS_INTEGER;",
                "TYPE f IS TABLE OF REF obj_t;",
                "TYPE g IS TABLE OF q.type_name;",
                "TYPE h IS VARRAY(5) OF rec_t;",
                "TYPE i IS ARRAY(3) OF NUMBER;",
                "TYPE j IS VARYING ARRAY(10) OF NUMBER;",
                "TYPE k IS TABLE OF a;",
                "TYPE l IS TABLE OF \"Mixed\" NULL;",
                "TYPE m IS TABLE OF app.q.t INDEX BY BINARY_INTEGER;",
                "TYPE n IS TABLE OF TIMESTAMP(3) WITH TIME ZONE;",
                "TYPE o IS TABLE OF emp.sal@link%TYPE;",
                "TYPE p2 IS TABLE OF s.t@internet.intranet%ROWTYPE;",
                "TYPE q2 IS TABLE OF t@\"LNK\"%ROWTYPE;"
            ),
            17
        )
    }

    @Test
    fun theIndexDatatypeIsNeverPartOfTheElement() {
        val source = spec(
            "TYPE a IS TABLE OF NUMBER INDEX BY VARCHAR2(10);",
            "TYPE b IS TABLE OF rec_t NOT NULL INDEX BY PLS_INTEGER NOT NULL;",
            "TYPE c IS TABLE OF p2.r INDEX BY p3.idx_t;"
        )
        assertAgreesWithAst(source, 3)
        val facts = types(source).associateBy { it.name.originalSpelling }
        assertThat(facts.getValue("a").elementType!!.name).isEqualTo(QualifiedName(id("NUMBER")))
        assertThat(facts.getValue("b").elementType!!.name).isEqualTo(QualifiedName(id("rec_t")))
        assertThat(facts.getValue("c").elementType!!.name).isEqualTo(QualifiedName(listOf(id("p2"), id("r"))))
    }

    @Test
    fun varrayBoundsAreValidatedButNeverRetained() {
        assertAgreesWithAst(
            spec("c_max CONSTANT PLS_INTEGER := 10;", "TYPE a IS VARRAY(c_max) OF NUMBER;", "TYPE b IS VARRAY(app.c_max) OF DATE NOT NULL;"),
            2
        )
    }

    @Test
    fun quotedElementAndTypeNamesKeepIdentitySemantics() {
        val source = spec("TYPE \"Rows\" IS TABLE OF \"Elem\";", "TYPE rows2 IS TABLE OF elem;")
        assertAgreesWithAst(source, 2)
        val facts = types(source)
        assertThat(facts[0].name.quoted).isTrue()
        assertThat(facts[0].elementType!!.name.last.quoted).isTrue()
        assertThat(facts[1].elementType!!.name.last.quoted).isFalse()
        assertThat(facts[0].elementType!!.name).isNotEqualTo(facts[1].elementType!!.name)
    }

    @Test
    fun unsupportedOrMalformedElementsAreNotFabricatedAndLaterMembersSurvive() {
        listOf(
            "TYPE bad IS TABLE OF t@link;" to true,
            "TYPE bad IS TABLE OF emp.sal@%TYPE;" to false,
            "TYPE bad IS TABLE OF REF t@link%TYPE;" to false,
            "TYPE bad IS TABLE OF ;" to false,
            "TYPE bad IS TABLE NUMBER;" to false,
            "TYPE bad IS VARRAY OF NUMBER;" to false,
            "TYPE bad IS TABLE OF NUMBER INDEX BY;" to false,
            "TYPE bad IS TABLE OF NUMBER INDEX PLS_INTEGER;" to false,
            "TYPE bad IS TABLE OF NUMBER NUMBER;" to false,
            "TYPE bad IS TABLE OF NUMBER NOT;" to false,
            "TYPE bad IS TABLE OF NUMBER NULL NOT NULL;" to false,
            "TYPE bad IS TABLE OF f(a NUMBER);" to false,
            "TYPE bad IS TABLE OF NUMBER(1 2);" to false,
            "TYPE bad IS TABLE OF NUMBER INDEX BY PLS_INTEGER NUMBER;" to false,
            "TYPE bad IS TABLE OF NUMBER INDEX BY f(a NUMBER);" to false,
            "TYPE bad IS VARRAY(x y) OF NUMBER;" to false,
            "TYPE bad IS VARRAY() OF NUMBER;" to false,
            "TYPE bad IS VARRAY(5) NUMBER;" to false,
            "TYPE bad IS ARRAY OF NUMBER;" to false,
            "TYPE bad IS TABLE OF rec_t INDEX BY PLS_INTEGER INDEX BY PLS_INTEGER;" to false
        ).forEach { (declaration, parserAccepts) ->
            val source = spec(declaration, "g NUMBER;", "PROCEDURE ok;")
            val accepted = runCatching { parse(source) }.isSuccess
            assertThat(accepted).describedAs(declaration).isEqualTo(parserAccepts)
            val facts = extractor.extract(fileId, source)
            val bad = facts.filterIsInstance<PackageTypeDeclaration>().single { it.name.originalSpelling == "bad" }
            assertThat(bad.elementType).describedAs(declaration).isNull()
            assertThat(facts.filterIsInstance<PackageValueDeclaration>().map { it.name.originalSpelling }).describedAs(declaration).containsExactly("g")
            assertThat(facts.filterIsInstance<PackageProcedureDeclaration>().map { it.name.originalSpelling }).describedAs(declaration).containsExactly("ok")
        }
    }

    @Test
    fun brokenDelimitersDropOnlyWhatTheStrictWalkAlreadyDropped() {
        // An unbalanced bound makes the whole specification unwalkable, as before: no element and no fact.
        assertThat(extractor.extract(fileId, spec("TYPE bad IS VARRAY(5 OF NUMBER;"))).isEmpty()
    }

    @Test
    fun arrayHeadsAreCollectionsWithoutSearchingAnywhereForTheWord() {
        val facts = types(
            spec(
                "TYPE a IS VARYING ARRAY(10) OF NUMBER;",
                "TYPE r IS RECORD(array_x NUMBER, t_array VARCHAR2(10));",
                "TYPE c IS REF CURSOR;",
                "TYPE x IS TABLE OF \"ARRAY\";"
            )
        ).associateBy { it.name.originalSpelling }
        assertThat(facts.getValue("a").shape).isEqualTo(ProjectTypeShape.COLLECTION)
        assertThat(facts.getValue("r").shape).isEqualTo(ProjectTypeShape.RECORD)
        assertThat(facts.getValue("r").elementType).isNull()
        assertThat(facts.getValue("r").recordFields.map { it.name.originalSpelling }).containsExactly("array_x", "t_array")
        assertThat(facts.getValue("c").shape).isEqualTo(ProjectTypeShape.REF_CURSOR)
        assertThat(facts.getValue("c").elementType).isNull()
        assertThat(facts.getValue("x").elementType!!.name.last.quoted).isTrue()
        // An ARRAY head that is not structurally valid stays unclassified, as before.
        assertThat(types(spec("TYPE z IS ARRAY OF NUMBER;")).single().shape).isNull()
    }

    @Test
    fun factInvariantEqualityAndOrdering() {
        val range = SourceRange(fileId, 1, 0, 1, 10)
        val element = NamedTypeRef(QualifiedName(id("rec_t")), range)
        fun type(shape: ProjectTypeShape?, element: TypeRef?) =
            PackageTypeDeclaration(QualifiedName(id("p")), id("t"), shape, emptyList(), fileId, range, element)
        assertThatThrownBy { type(ProjectTypeShape.RECORD, element) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { type(null, element) }.isInstanceOf(IllegalArgumentException::class.java)
        val a = type(ProjectTypeShape.COLLECTION, element)
        assertThat(a).isEqualTo(type(ProjectTypeShape.COLLECTION, NamedTypeRef(QualifiedName(id("rec_t")), range)))
        assertThat(a.hashCode()).isEqualTo(type(ProjectTypeShape.COLLECTION, element).hashCode())
        assertThat(a).isNotEqualTo(type(ProjectTypeShape.COLLECTION, null))
        assertThat(a).isNotEqualTo(type(ProjectTypeShape.COLLECTION, NamedTypeRef(QualifiedName(id("other")), range)))
        assertThat(a.toString()).contains("rec_t")
    }

    private fun prepare(concurrent: Boolean, files: List<Pair<String, String>>) = ProjectIndexPreparation().prepare(
        files.map { (name, text) -> ProjectSource(FileId(name), ProjectSourceReader { text }) }, concurrent
    )

    @Test
    fun elementMetadataDoesNotChangeIdentityLookupOrOrdering() {
        val one = spec("TYPE rec_t IS RECORD(n NUMBER);", "TYPE rows_t IS TABLE OF rec_t INDEX BY PLS_INTEGER;", "TYPE v_t IS VARRAY(5) OF rec_t;", "g NUMBER;", "SUBTYPE s IS rec_t;", "PROCEDURE rows_t;")
        val files = listOf("b.sql" to one, "a.sql" to one.replace("rec_t INDEX", "other_t INDEX"))
        val serial = prepare(false, files).index
        val concurrent = prepare(true, files).index
        val reversed = prepare(false, files.reversed()).index
        listOf(concurrent, reversed).forEach { other ->
            assertThat(other.declarations).isEqualTo(serial.declarations)
            assertThat(other.occurrences.map { it.fileId.value to it.ordinal }).isEqualTo(serial.occurrences.map { it.fileId.value to it.ordinal })
        }
        val rows = serial.findTypes(QualifiedName(id("p")), id("rows_t")).filterIsInstance<PackageTypeDeclaration>()
        assertThat(rows).hasSize(2)
        assertThat(rows.map { it.fileId.value }).containsExactly("a.sql", "b.sql")
        assertThat(rows.map { (it.elementType as NamedTypeRef).name.last.originalSpelling }).containsExactlyInAnyOrder("other_t", "rec_t")
        assertThat(serial.findSubprograms(QualifiedName(id("p")), id("rows_t"))).hasSize(2)
        assertThat(serial.findTypes(QualifiedName(id("p")), id("g"))).isEmpty()
        assertThat(serial.findTypes(QualifiedName(id("p")), id("s")).first()).isInstanceOf(PackageSubtypeDeclaration::class.java)
        val record = serial.findTypes(QualifiedName(id("p")), id("rec_t")).first() as PackageTypeDeclaration
        assertThat(record.elementType).isNull()
        assertThat(record.recordFields.map { it.name.originalSpelling }).containsExactly("n")
    }
}
