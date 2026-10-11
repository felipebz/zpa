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
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/**
 * Strict extraction of package specification variables and constants. Valid syntax is compared with the
 * real parser (error recovery disabled) declaration by declaration; the extractor itself is AST-free.
 */
class PackageValueExtractionTest {
    private val fileId = FileId("fixture/values.sql")
    private val extractor = ProjectDeclarationExtractor()

    private fun extract(source: String) = extractor.extract(fileId, source)
    private fun values(source: String) = extract(source).filterIsInstance<PackageValueDeclaration>()
    private fun parse(source: String) = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(source)
    private fun spec(vararg declarations: String) = "CREATE PACKAGE p AS\n${declarations.joinToString("\n") { "  $it" }}\nEND p;"

    private fun AstNode.range() = SourceRange(fileId, token.line, token.column, lastToken.endLine, lastToken.endColumn)

    private data class Expected(
        val name: String, val constant: Boolean, val typeKind: String, val typeName: String, val typeRange: SourceRange,
        val range: SourceRange
    )

    private fun actual(value: PackageValueDeclaration) = Expected(
        value.name.originalSpelling, value.constant,
        when (val type = value.typeRef) {
            is NamedTypeRef -> "NAMED"
            is AnchoredTypeRef -> "ANCHORED:${type.anchor}"
            is RefTypeRef -> "REF"
        },
        value.typeRef.name.segments.joinToString(".") { it.originalSpelling }, value.typeRef.sourceRange, value.sourceRange
    )

    /** Leading `name{.name}` of a datatype, taken from AST tokens (after a leading REF). */
    private fun typeName(datatype: AstNode): String {
        val tokens = datatype.tokens.let { if (datatype.getFirstChild().type == PlSqlGrammar.REF_DATATYPE) it.drop(1) else it }
        val names = mutableListOf(tokens.first().originalValue)
        var index = 1
        while (index + 1 < tokens.size && tokens[index].type === PlSqlPunctuator.DOT) {
            names += tokens[index + 1].originalValue
            index += 2
        }
        return names.joinToString(".")
    }

    /** Variables that are direct members of a specification's declare section, in source order. */
    private fun expectedFromAst(source: String): List<Expected> = parse(source).getDescendants(PlSqlGrammar.CREATE_PACKAGE).flatMap { pkg ->
        pkg.getFirstChildOrNull(PlSqlGrammar.DECLARE_SECTION)?.getChildren(PlSqlGrammar.VARIABLE_DECLARATION).orEmpty().map { variable ->
            val datatype = variable.getFirstChild(PlSqlGrammar.DATATYPE)
            val kind = when (datatype.getFirstChild().type) {
                PlSqlGrammar.ANCHORED_DATATYPE -> "ANCHORED:${datatype.tokens.last().value.uppercase()}"
                PlSqlGrammar.REF_DATATYPE -> "REF"
                else -> "NAMED"
            }
            Expected(
                variable.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenOriginalValue,
                variable.hasDirectChildren(PlSqlKeyword.CONSTANT), kind, typeName(datatype), datatype.range(), variable.range()
            )
        }
    }

    private fun assertAgreesWithAst(source: String) {
        val expected = expectedFromAst(source) // throws when the parser rejects the source
        assertThat(expected).describedAs(source).isNotEmpty()
        val facts = values(source)
        assertThat(facts.map(::actual)).describedAs(source).isEqualTo(expected)
        facts.forEach {
            assertThat(it.fileId).isEqualTo(fileId)
            assertThat(it.role).isEqualTo(DeclarationRole.SPECIFICATION)
            assertThat(it.owner).isEqualTo(QualifiedName(OracleIdentifier.fromSource("p")))
            assertThat(it.kind).isEqualTo(if (it.constant) ProjectDeclarationKind.PACKAGE_CONSTANT else ProjectDeclarationKind.PACKAGE_VARIABLE)
        }
    }

    @Test
    fun datatypeFormsAgreeWithTheParser() {
        listOf(
            spec("g NUMBER;", "d DATE;", "b BOOLEAN;", "pi PLS_INTEGER;", "bi BINARY_INTEGER;", "cl CLOB;", "sr SYS_REFCURSOR;"),
            spec("n NUMBER(10,2);", "v VARCHAR2(30 CHAR);", "w VARCHAR2 (30);", "s NUMBER(*,0);", "neg NUMBER(10,-2);", "r RAW(16);"),
            spec("t3 TIMESTAMP(3) WITH TIME ZONE;", "t4 TIMESTAMP WITH LOCAL TIME ZONE;", "iv INTERVAL DAY(2) TO SECOND(6);", "ym INTERVAL YEAR TO MONTH;"),
            spec("dp DOUBLE PRECISION;", "lr LONG RAW;", "vc VARCHAR2(10 BYTE);"),
            spec("q1 pkg.t;", "q2 sch.pkg.t;", "\"Q\" \"Sch\".\"Pkg\".\"T\";", "q3 sys.anydata;"),
            spec("a1 tab.col%TYPE;", "a2 tab%ROWTYPE;", "a3 sch.tab.col%TYPE;", "a4 other_pkg.value%TYPE;", "a5 \"T\".\"C\"%TYPE;", "a6 tab.col%type;"),
            spec("r1 REF obj_t;", "r2 REF sch.obj_t;", "r3 ref \"O\";")
        ).forEach(::assertAgreesWithAst)
    }

    @Test
    fun constantsNullabilityAndInitializersAgreeWithTheParser() {
        listOf(
            spec("x1 NUMBER NOT NULL := 1;", "x2 NUMBER NULL;", "x3 NUMBER NULL := 1;", "x4 NUMBER DEFAULT 1;", "x5 NUMBER NOT NULL DEFAULT 1;"),
            spec("c1 CONSTANT NUMBER := 1;", "c2 CONSTANT VARCHAR2(10) NOT NULL := 'x';", "c3 CONSTANT t.c%TYPE := 1;", "c4 constant boolean default true;"),
            spec("g NUMBER;\n  c CONSTANT NUMBER := 1;\n  \"Mixed Case\" VARCHAR2(10) := 'a';\n  UPPER NUMBER;\n  lower NUMBER;")
        ).forEach(::assertAgreesWithAst)
    }

    @Test
    fun initializersWithCallsCasesAndNestedStructureAgreeWithTheParser() {
        listOf(
            spec("e1 NUMBER := f(1, g(2, 3)) + (4 * 5);", "e2 NUMBER := CASE WHEN 1=1 THEN 1 ELSE 2 END;", "n NUMBER;"),
            spec("e3 NUMBER := CASE x WHEN 1 THEN 2 WHEN 3 THEN 4 END;", "e4 NUMBER := CASE WHEN 1=1 THEN CASE WHEN 2=2 THEN 1 ELSE 2 END ELSE 3 END;"),
            spec("e5 NUMBER := CASE (1+1) WHEN 2 THEN 1 END;", "e6 NUMBER := f(CASE WHEN a THEN 1 END, CASE WHEN b THEN 2 END);"),
            spec("e7 NUMBER := CASE CASE WHEN a THEN 1 END WHEN 1 THEN 2 END;", "e8 NUMBER := CASE WHEN CASE WHEN a THEN b END THEN 1 END;"),
            spec("e9 BOOLEAN := x IS NOT NULL AND y BETWEEN 1 AND 2 OR z LIKE 'a%' OR w IN (1, 2);", "e10 BOOLEAN := NOT (a OR b);"),
            spec("e11 NUMBER := -1;", "e12 NUMBER := +1;", "e13 VARCHAR2(10) := 'a' || 'b';", "e14 NUMBER := 2 ** 3;", "e15 NUMBER := a.b(1).c;"),
            spec("e16 DATE := DATE '2020-01-01';", "e17 TIMESTAMP := SYSTIMESTAMP - INTERVAL '1' DAY;", "e18 t := t(1, 2, 3);", "e19 t := t();"),
            spec("e20 NUMBER := f(a => 1, b => 2);", "e21 NUMBER := pkg.f(1) + pkg.g;", "e22 NUMBER := SQL%ROWCOUNT;")
        ).forEach(::assertAgreesWithAst)
    }

    @Test
    fun caseAndFunctionAreOrdinaryIdentifiersWhenTheParserSaysSo() {
        listOf(
            spec("case NUMBER;", "function NUMBER;", "\"CASE\" NUMBER := 1;", "\"FUNCTION\" NUMBER;"),
            spec("g NUMBER := function(1);", "h NUMBER := function(1, 2) + function(3);", "i NUMBER := FUNCTION(function(1));"),
            spec("g NUMBER := t.case + 1;", "h NUMBER := case(1);", "i NUMBER := case;", "j NUMBER := case.x;"),
            spec("g NUMBER := CASE WHEN 1=1 THEN case ELSE 2 END;", "h NUMBER := CASE WHEN 1=1 THEN case(1) ELSE function(2) END;"),
            spec("g NUMBER := CASE WHEN case THEN case ELSE case END;", "after NUMBER;"),
            spec("type NUMBER;", "\"TYPE\" NUMBER;", "subtype NUMBER;", "cursor NUMBER;", "constant NUMBER;")
        ).forEach(::assertAgreesWithAst)
    }

    @Test
    fun literalsCommentsAndQuotedIdentifiersCannotImitateKeywordsOrDelimiters() {
        listOf(
            spec("s1 VARCHAR2(20) := 'a; END; CASE WHEN';", "s2 NUMBER := 1 /* ; END; CASE */ + 2; -- ; END;", "next NUMBER;"),
            spec("\"END\" NUMBER := \"CASE\" + \"WHEN\";", "\"IS\" NUMBER;", "\"AS\" NUMBER := 1;", "\"(\" NUMBER;"),
            spec("s3 VARCHAR2(5) := q'[a;)(END]';", "s4 VARCHAR2(5) := 'it''s; fine';", "\"PROCEDURE\" NUMBER;"),
            spec("\"a;b\" NUMBER;", "\"x y\" NUMBER := 1;")
        ).forEach(::assertAgreesWithAst)
    }

    @Test
    fun valuesKeepDeclarationOrderAmongOtherMembers() {
        val source = """
            CREATE PACKAGE p AS
              g NUMBER;
              TYPE t IS RECORD (a NUMBER := 1, b VARCHAR2(5));
              SUBTYPE s IS NUMBER;
              c CONSTANT t := NULL;
              e EXCEPTION;
              PRAGMA EXCEPTION_INIT(e, -20001);
              PRAGMA SERIALLY_REUSABLE;
              CURSOR cu IS SELECT 1 FROM dual;
              CURSOR cr RETURN t%ROWTYPE;
              PROCEDURE q(a NUMBER);
              FUNCTION f RETURN NUMBER;
              h VARCHAR2(10) := 'x';
              TYPE ta IS TABLE OF NUMBER;
              TYPE rc IS REF CURSOR;
              z NUMBER;
            END p;
        """.trimIndent()
        parse(source)
        assertThat(extract(source).map { it.kind to it.qualifiedName().last.originalSpelling }).containsExactly(
            ProjectDeclarationKind.PACKAGE to "p",
            ProjectDeclarationKind.PACKAGE_VARIABLE to "g",
            ProjectDeclarationKind.PACKAGE_TYPE to "t",
            ProjectDeclarationKind.PACKAGE_SUBTYPE to "s",
            ProjectDeclarationKind.PACKAGE_CONSTANT to "c",
            ProjectDeclarationKind.PACKAGE_PROCEDURE to "q",
            ProjectDeclarationKind.PACKAGE_FUNCTION to "f",
            ProjectDeclarationKind.PACKAGE_VARIABLE to "h",
            ProjectDeclarationKind.PACKAGE_TYPE to "ta",
            ProjectDeclarationKind.PACKAGE_TYPE to "rc",
            ProjectDeclarationKind.PACKAGE_VARIABLE to "z"
        )
        assertAgreesWithAst(source)
        // Exceptions, cursors and pragmas are never values; the range and type syntax of `c` are exact.
        val c = values(source).single { it.name.originalSpelling == "c" }
        assertThat(c.sourceRange).isEqualTo(SourceRange(fileId, 5, 2, 5, 23))
        assertThat(c.typeRef.sourceRange).isEqualTo(SourceRange(fileId, 5, 13, 5, 14))
    }

    @Test
    fun pragmasAreNotValuesButAVariableNamedPragmaIs() {
        val pragmas = spec(
            "PRAGMA SERIALLY_REUSABLE;", "PRAGMA AUTONOMOUS_TRANSACTION;", "PRAGMA UDF;", "PRAGMA EXCEPTION_INIT(e, -20001);",
            "PRAGMA RESTRICT_REFERENCES(DEFAULT, WNDS);", "PRAGMA DEPRECATE(f, 'use g');", "PRAGMA COVERAGE('NOT_FEATURE');",
            "pragma serially_reusable;", "e EXCEPTION;"
        )
        parse(pragmas)
        assertThat(parse(pragmas).getDescendants(PlSqlGrammar.VARIABLE_DECLARATION)).isEmpty()
        assertThat(values(pragmas)).isEmpty()
        assertAgreesWithAst(spec("pragma NUMBER;", "pragma t.x%TYPE := 1;", "PRAGMA SERIALLY_REUSABLE;", "pragma VARCHAR2(10);", "pragma coverage;", "pragma udf2;"))
        // Valid but deliberately excluded: the parser accepts these as variables, the extractor stays conservative.
        listOf(spec("pragma interface(x);"), spec("e CONSTANT EXCEPTION;")).forEach {
            assertThat(parse(it).getDescendants(PlSqlGrammar.VARIABLE_DECLARATION)).hasSize(1)
            assertThat(values(it)).isEmpty()
        }
    }

    @Test
    fun multipleUnitsAndSameNamedValuesKeepSeparateFacts() {
        val source = "CREATE PACKAGE a AS g NUMBER; g VARCHAR2(5); END a;\n/\nCREATE PACKAGE b.c AS g NUMBER; END c;\n"
        val facts = values(source)
        assertThat(facts.map { it.owner.toString() to it.name.originalSpelling }).containsExactly("a" to "g", "a" to "g", "b.c" to "g")
        assertThat(facts[0]).isNotEqualTo(facts[1])
        assertThat(facts.map { it.qualifiedName().toString() }).containsExactly("a.g", "a.g", "b.c.g")
        assertThat(facts[0].sourceRange.startLine).isEqualTo(1)
        assertThat(facts[2].sourceRange.startLine).isEqualTo(4 - 1)
    }

    @Test
    fun emptyAndValueOnlyPackages() {
        assertThat(extract("CREATE PACKAGE p AS END p;").map { it.kind }).containsExactly(ProjectDeclarationKind.PACKAGE)
        assertThat(extract(spec("g NUMBER;", "h NUMBER;")).map { it.kind }).containsExactly(
            ProjectDeclarationKind.PACKAGE, ProjectDeclarationKind.PACKAGE_VARIABLE, ProjectDeclarationKind.PACKAGE_VARIABLE
        )
    }

    @Test
    fun bodiesLocalsAndStandaloneUnitsNeverProduceValues() {
        val source = """
            CREATE PACKAGE p AS PROCEDURE q; END p;
            /
            CREATE PACKAGE BODY p AS
              priv NUMBER := CASE WHEN 1=1 THEN 1 END;
              c CONSTANT NUMBER := 1;
              PROCEDURE q IS loc NUMBER; BEGIN NULL; END;
            END p;
            /
            CREATE FUNCTION f(a NUMBER) RETURN NUMBER IS loc VARCHAR2(5) := 'x'; BEGIN RETURN a; END;
            /
            CREATE PROCEDURE sp(a NUMBER DEFAULT 1) IS lv NUMBER; BEGIN NULL; END;
            CREATE TYPE t AS OBJECT (id NUMBER);
        """.trimIndent()
        assertThat(values(source)).isEmpty()
        // Record fields, parameters and subprogram locals inside a specification are not members either.
        val withNested = spec("TYPE r IS RECORD (a NUMBER := 1, b VARCHAR2(5));", "PROCEDURE q(p1 NUMBER := 1, p2 OUT NOCOPY VARCHAR2);", "FUNCTION f(p3 NUMBER DEFAULT 2) RETURN NUMBER;")
        assertThat(values(withNested)).isEmpty()
    }

    @Test
    fun aStandaloneUnitWithTheSameTextualNameStaysADistinctFact() {
        val facts = extract("CREATE PACKAGE p AS g NUMBER; END p;\n/\nCREATE FUNCTION p.g RETURN NUMBER IS BEGIN RETURN 1; END;")
        assertThat(facts.map { it.kind to it.qualifiedName().toString() }).containsExactly(
            ProjectDeclarationKind.PACKAGE to "p", ProjectDeclarationKind.PACKAGE_VARIABLE to "p.g",
            ProjectDeclarationKind.STANDALONE_FUNCTION to "p.g"
        )
    }

    /** Range of the first occurrence of [text] in [source] (0-based column, inclusive end like the facts). */
    private fun rangeOf(source: String, text: String): SourceRange {
        val offset = source.indexOf(text)
        require(offset >= 0)
        fun position(at: Int): Pair<Int, Int> {
            val before = source.substring(0, at)
            return before.count { it == '\n' } + 1 to at - (before.lastIndexOf('\n') + 1)
        }
        val (startLine, startColumn) = position(offset)
        val (endLine, endColumn) = position(offset + text.length)
        return SourceRange(fileId, startLine, startColumn, endLine, endColumn)
    }

    @Test
    fun malformedDeclarationsYieldNoFactAndKeepFollowingValues() {
        listOf(
            "bad NUMBER b;",                                   // extra identifier in the datatype
            "bad NUMBER :=;",                                  // empty initializer
            "bad NUMBER DEFAULT;",
            "bad NUMBER := 1 +;",
            "bad NUMBER := CASE WHEN x THEN 1;",               // unterminated CASE
            "bad NUMBER := CASE x END;",                       // CASE without WHEN
            "bad NUMBER := CASE WHEN x THEN END;",             // empty THEN branch
            "bad NUMBER := CASE WHEN x 1 END;",                // missing THEN
            "bad NUMBER := x THEN 1;",                         // stray THEN
            "bad NUMBER := 1, 2;",                             // top-level comma
            "bad NUMBER := f(,1);",
            "bad NUMBER := f(1,);",
            "bad NUMBER NOT := 1;",
            "bad NUMBER NOT NULL NOT NULL;",
            "bad NUMBER := := 1;",
            "bad NUMBER(a NUMBER);",                           // not a type argument list
            "bad NUMBER() ;",
            "bad NUMBER(1,) ;",
            "bad;",                                            // no datatype
            "bad CONSTANT;",
            "bad := 1;",
            "bad EXCEPTION;"                                   // an exception, not a value
        ).forEach { malformed ->
            val source = "CREATE PACKAGE p AS\n  $malformed\n  good NUMBER := 1;\nEND p;"
            val facts = values(source)
            assertThat(facts.map { it.name.originalSpelling }).describedAs(source).doesNotContain("bad")
            if (malformed != "bad EXCEPTION;") {
                assertThatThrownBy { parse(source) }.describedAs("parser must reject: $source").isInstanceOf(Exception::class.java)
            }
            assertThat(facts.map { it.name.originalSpelling }).describedAs(source).containsExactly("good")
            assertThat(facts.single().sourceRange).describedAs(source).isEqualTo(rangeOf(source, "good NUMBER := 1;"))
            assertThat(facts.single().typeRef.sourceRange).describedAs(source).isEqualTo(rangeOf(source, "NUMBER := 1;").let {
                SourceRange(fileId, it.startLine, it.startColumn, it.endLine, it.startColumn + "NUMBER".length)
            })
        }
    }

    @Test
    fun parserAcceptedButUnsupportedDatatypesAreExcludedWithoutAffectingNeighbours() {
        listOf(
            "bad VARCHAR2(f(1));",      // type argument that is an expression
            "bad TABLE;",               // bare TABLE datatype
            "bad SYS.X@link;"           // database link
        ).forEach { declaration ->
            val source = spec(declaration, "good NUMBER;")
            val bad = parse(source).getDescendants(PlSqlGrammar.VARIABLE_DECLARATION)
            // The parser's verdict is recorded so the exclusion is known to be conservatism, not rejection.
            assertThat(bad).describedAs(declaration).hasSize(2)
            assertThat(values(source).map { it.name.originalSpelling }).describedAs(declaration).containsExactly("good")
        }
    }

    @Test
    fun brokenDelimitersAndMissingSemicolonsLoseOnlyWhatCannotBeBounded() {
        val good = "CREATE PACKAGE good AS PROCEDURE r; h NUMBER; END good;"
        listOf(
            "CREATE PACKAGE bad AS g NUMBER := (1; h NUMBER; END bad;\n$good",
            "CREATE PACKAGE bad AS g NUMBER := 1); h NUMBER; END bad;\n$good",
            "CREATE PACKAGE bad AS g NUMBER := (1]; h NUMBER; END bad;\n$good",
            "CREATE PACKAGE bad AS g NUMBER := f(1, [2); h NUMBER; END bad;\n$good",
            "CREATE PACKAGE bad AS g NUMBER := 1; h NUMBER;\n$good",             // no END
            "CREATE PACKAGE bad AS g NUMBER := 1; h NUMBER; END\n$good"          // END without `;`
        ).forEach { source ->
            val facts = extract(source)
            assertThat(facts.map { it.qualifiedName().toString() }).describedAs(source).containsExactly("good", "good.r", "good.h")
            assertThat(facts.last().sourceRange.startColumn).isEqualTo(36)
        }
        // A missing `;` before a reserved PROCEDURE: the broken value is dropped, the procedure and later values survive.
        val missing = extract("CREATE PACKAGE p AS a NUMBER := 1\n  PROCEDURE q;\n  b NUMBER;\nEND p;")
        assertThat(missing.map { it.qualifiedName().toString() }).containsExactly("p", "p.q", "p.b")
        // Two values fused by a missing `;` cannot be bounded: neither is emitted, the rest is.
        val fused = extract("CREATE PACKAGE p AS a NUMBER b NUMBER; c NUMBER; END p;")
        assertThat(fused.map { it.qualifiedName().toString() }).containsExactly("p", "p.c")
    }

    @Test
    fun unsupportedDatatypesAreRejectedNotPartiallyExtracted() {
        listOf(
            "g NUMBER(10 ,2 3);", "g TIMESTAMP(3) FOO;", "g t.x%FOO;", "g REF t%TYPE;", "g t%TYPE(1);",
            "g t.%TYPE;", "g t..x;", "g \"A\"\"\".b%TYPE c;"
        ).forEach {
            val source = spec(it, "after NUMBER;")
            assertThat(values(source).map { v -> v.name.originalSpelling }).describedAs(it).containsExactly("after")
        }
    }

    @Test
    fun initializerAdmissionIsConservativeNotAParser() {
        // Not validated: two adjacent operands pass the shape check although the parser rejects them.
        assertThatThrownBy { parse(spec("g NUMBER := 1 2;")) }.isInstanceOf(Exception::class.java)
        assertThat(values(spec("g NUMBER := 1 2;")).map { it.name.originalSpelling }).containsExactly("g")
        // Deep nesting beyond the admission bound is rejected instead of examined.
        val deep = "(".repeat(200) + "1" + ")".repeat(200)
        assertThat(values(spec("g NUMBER := $deep;", "h NUMBER;")).map { it.name.originalSpelling }).containsExactly("h")
        val caseChain = "CASE WHEN 1=1 THEN ".repeat(200) + "1" + " END".repeat(200)
        assertThat(values(spec("g NUMBER := $caseChain;", "h NUMBER;")).map { it.name.originalSpelling }).containsExactly("h")
    }

    @Test
    fun manyAmbiguousCaseTokensDoNotBlowUp() {
        val source = spec("g NUMBER := ${"case ".repeat(5000)}1;", "h NUMBER;")
        val start = System.nanoTime()
        val names = values(source).map { it.name.originalSpelling }
        assertThat(System.nanoTime() - start).isLessThan(5_000_000_000L)
        assertThat(names).contains("h")
    }

    @Test
    fun factsRetainNoSourceStructures() {
        val fact = values(spec("g NUMBER := f(1, 2);")).single()
        // Only value-like model types; FileId is an inline class and is stored as its String.
        assertThat(PackageValueDeclaration::class.java.declaredFields.map { it.type.name }).containsExactlyInAnyOrder(
            "com.felipebz.zpa.project.QualifiedName", "com.felipebz.zpa.project.OracleIdentifier", "boolean",
            "com.felipebz.zpa.project.TypeRef", "java.lang.String", "com.felipebz.zpa.project.SourceRange",
            "com.felipebz.zpa.project.ProjectDeclarationKind", "com.felipebz.zpa.project.DeclarationRole"
        )
        assertThat(fact.toString()).doesNotContain("f(1, 2)")
    }
}
