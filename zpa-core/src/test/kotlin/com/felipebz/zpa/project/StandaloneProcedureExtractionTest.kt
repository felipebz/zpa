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
import com.felipebz.zpa.lexer.PlSqlLexer
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/** Strict extraction of standalone CREATE PROCEDURE units. */
class StandaloneProcedureExtractionTest {
    private val fileId = FileId("fixture/standalone.sql")
    private val extractor = ProjectDeclarationExtractor()

    private fun extract(source: String) = extractor.extract(fileId, source)
    private fun procedures(source: String) = extract(source).filterIsInstance<StandaloneProcedureDeclaration>()
    private fun names(source: String) = procedures(source).map { it.name.toString() }

    private val grammarAcceptedExclusions = setOf(
        "AS EXTERNAL LIBRARY lib PARAMETERS (x, );", "AS EXTERNAL LIBRARY lib PARAMETERS (x RAW y);",
        "AS EXTERNAL LIBRARY lib PARAMETERS (x RAW INDICATOR);", "RESULT_CACHE RELIES_ON(1) IS BEGIN NULL; END;"
    )

    private fun parserAccepts(source: String): Boolean = try {
        PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(source)
        true
    } catch (e: Exception) {
        false
    }

    private fun astProcedures(source: String): List<AstNode> =
        PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).parse(source)
            .getDescendants(PlSqlGrammar.CREATE_PROCEDURE)

    private fun AstNode.range() = SourceRange(fileId, token.line, token.column, lastToken.endLine, lastToken.endColumn)

    /** Valid syntax: every fact agrees with the parser's CREATE_PROCEDURE node. */
    private fun assertAgreesWithAst(source: String) {
        val facts = procedures(source)
        val nodes = astProcedures(source)
        assertThat(facts).describedAs(source).hasSameSizeAs(nodes).isNotEmpty
        facts.zip(nodes).forEach { (fact, node) ->
            assertThat(fact.sourceRange).describedAs(source).isEqualTo(node.range())
            val unitName = node.getFirstChild(PlSqlGrammar.UNIT_NAME)
            assertThat(fact.name.toString()).isEqualTo(unitName.tokens.joinToString("") { it.originalValue })
            val astParameters = node.getFirstChildOrNull(PlSqlGrammar.PARAMETER_DECLARATIONS)
                ?.getChildren(PlSqlGrammar.PARAMETER_DECLARATION).orEmpty()
            assertThat(fact.parameters.map { it.name.originalSpelling })
                .isEqualTo(astParameters.map { it.token.originalValue })
            fact.parameters.zip(astParameters).forEach { (parameter, ast) ->
                assertThat(parameter.sourceRange).isEqualTo(ast.range())
                val datatype = ast.getFirstChild(PlSqlGrammar.DATATYPE)
                assertThat(parameter.typeRef.sourceRange).isEqualTo(datatype.range())
                assertThat(parameter.defaultPresent).isEqualTo(ast.hasDirectChildren(PlSqlGrammar.DEFAULT_VALUE_ASSIGNMENT))
            }
            assertThat(fact.parameters.map { it.ordinal }).isEqualTo((1..fact.parameters.size).toList())
        }
    }

    @Test
    fun validProceduresAgreeWithTheAstOracle() {
        listOf(
            "CREATE PROCEDURE p IS BEGIN NULL; END;",
            "CREATE OR REPLACE PROCEDURE app.p AS BEGIN NULL; END p;",
            "CREATE OR REPLACE EDITIONABLE PROCEDURE \"App\".\"Do.It\" IS BEGIN NULL; END;",
            "CREATE NONEDITIONABLE PROCEDURE IF NOT EXISTS p IS BEGIN NULL; END;",
            "CREATE PROCEDURE p SHARING = METADATA IS BEGIN NULL; END;",
            "CREATE PROCEDURE p(a NUMBER, b IN VARCHAR2 DEFAULT 'x', c OUT NUMBER, d IN OUT NOCOPY CLOB, e NUMBER := 1) IS BEGIN NULL; END;",
            "CREATE PROCEDURE p(a t.col%TYPE, b t%ROWTYPE, c REF obj_t, d NUMBER(10,2), e VARCHAR2(10 CHAR), f TIMESTAMP(6) WITH LOCAL TIME ZONE) IS BEGIN NULL; END;",
            "CREATE PROCEDURE p AUTHID DEFINER ACCESSIBLE BY (FUNCTION g, PACKAGE q) DEFAULT COLLATION USING_NLS_COMP AS BEGIN NULL; END;",
            "CREATE PROCEDURE p IS x NUMBER; PROCEDURE inner_p IS BEGIN NULL; END; FUNCTION inner_f RETURN NUMBER IS BEGIN RETURN 1; END; " +
                "BEGIN CASE WHEN x = 1 THEN NULL; ELSE NULL; END CASE; IF x > 1 THEN NULL; END IF; LOOP EXIT; END LOOP; END;",
            "CREATE PROCEDURE p AS LANGUAGE JAVA NAME 'a.B.c()';",
            "CREATE PROCEDURE p AS EXTERNAL LIBRARY lib NAME \"c_p\" PARAMETERS (CONTEXT);",
            "CREATE PROCEDURE p AS LANGUAGE C LIBRARY lib;",
            "CREATE PROCEDURE p AS MLE MODULE m ENV e SIGNATURE 'f()';",
            "CREATE PROCEDURE p AS MLE LANGUAGE JAVASCRIPT {{ if (x) { y(); } BEGIN END; CREATE ; }};",
            "CREATE PROCEDURE \"CREATE\"(\"IS\" NUMBER, \";\" NUMBER, \"(\" \"END\") IS BEGIN NULL; END;"
        ).forEach(::assertAgreesWithAst)
    }

    @Test
    fun validCallSpecificationsAndPropertyClausesAgreeWithTheAstOracle() {
        listOf(
            "CREATE PROCEDURE p AS LANGUAGE C NAME n LIBRARY lib AGENT IN (a, b) WITH CONTEXT PARAMETERS (CONTEXT, x INDICATOR, RETURN LENGTH);",
            "CREATE PROCEDURE p AS EXTERNAL LIBRARY lib NAME n;",
            "CREATE PROCEDURE p AS EXTERNAL LIBRARY lib PARAMETERS (x BY REFERENCE RAW, y INDICATOR STRUCT);",
            "CREATE PROCEDURE p AS LANGUAGE C LIBRARY lib WITH CONTEXT;",
            "CREATE PROCEDURE p AS EXTERNAL LIBRARY lib PARAMETERS (CONTEXT, x BY REFERENCE RAW, y INDICATOR STRUCT);",
            "CREATE PROCEDURE p AS EXTERNAL LIBRARY lib PARAMETERS (SELF TDO, SELF INDICATOR, SELF, RETURN LENGTH, z, w BY REFERENCE);",
            "CREATE PROCEDURE p AS EXTERNAL LIBRARY lib PARAMETERS (v NUMBER(10,2), u INDICATOR TDO BY REFERENCE VARCHAR2, t LENGTH INDICATOR);",
            "CREATE PROCEDURE p AS EXTERNAL LIBRARY lib PARAMETERS (CONTEXT x, RETURN CONTEXT, SELF INDICATOR STRUCT STRUCT);",
            "CREATE PROCEDURE p SQL_MACRO IS BEGIN NULL; END;",
            "CREATE PROCEDURE p SQL_MACRO(TABLE) IS BEGIN NULL; END;",
            "CREATE PROCEDURE p SQL_MACRO(TYPE => SCALAR) IS BEGIN NULL; END;",
            "CREATE PROCEDURE p PARALLEL_ENABLE IS BEGIN NULL; END;",
            "CREATE PROCEDURE p PARALLEL_ENABLE(PARTITION c BY ANY) IS BEGIN NULL; END;",
            "CREATE PROCEDURE p PARALLEL_ENABLE(PARTITION c BY HASH(a, b) ORDER d BY (e)) IS BEGIN NULL; END;",
            "CREATE PROCEDURE p PARALLEL_ENABLE(PARTITION c BY RANGE (a)) CLUSTER d BY (e, f) IS BEGIN NULL; END;",
            "CREATE PROCEDURE p RESULT_CACHE IS BEGIN NULL; END;",
            "CREATE PROCEDURE p RESULT_CACHE RELIES_ON(t, s.u) IS BEGIN NULL; END;",
            "CREATE PROCEDURE p ACCESSIBLE BY (FUNCTION g, PACKAGE s.q, r) IS BEGIN NULL; END;",
            "CREATE PROCEDURE p AUTHID CURRENT_USER DETERMINISTIC SHARD_ENABLE PIPELINED IS BEGIN NULL; END;"
        ).forEach(::assertAgreesWithAst)
    }

    @Test
    fun malformedCallSpecificationsAndPropertyClausesYieldNoFact() {
        listOf(
            "AS LANGUAGE SQL LIBRARY lib;", "AS EXTERNAL;", "AS LANGUAGE C;",
            "AS EXTERNAL LIBRARY;", "AS EXTERNAL LIBRARY lib WITH CONTEXT AGENT IN (a);", "AS EXTERNAL LIBRARY lib AGENT IN ();",
            "AS EXTERNAL LIBRARY lib PARAMETERS ();", "AS EXTERNAL LIBRARY lib PARAMETERS (x, );",
            "AS EXTERNAL LIBRARY lib PARAMETERS (CONTEXT END);", "AS EXTERNAL LIBRARY lib PARAMETERS (CONTEXT CONTEXT END);",
           
            "AS EXTERNAL LIBRARY lib PARAMETERS (x BY RAW);", "AS EXTERNAL LIBRARY lib PARAMETERS (x BY REFERENCE BY REFERENCE);",
            "AS EXTERNAL LIBRARY lib PARAMETERS (x RAW y);",
            "AS EXTERNAL LIBRARY lib PARAMETERS (x RAW INDICATOR);", "AS EXTERNAL LIBRARY lib PARAMETERS (x [RAW));",
            "AS EXTERNAL LIBRARY lib PARAMETERS (x NUMBER(10,2]));", "AS EXTERNAL LIBRARY lib PARAMETERS (1);", "AS EXTERNAL LIBRARY lib extra;",
            "AS LANGUAGE JAVA NAME x;", "AS MLE MODULE m SIGNATURE x;", "AS MLE LANGUAGE;",
            "SQL_MACRO(GARBAGE) IS BEGIN NULL; END;", "SQL_MACRO() IS BEGIN NULL; END;", "SQL_MACRO(TYPE TABLE) IS BEGIN NULL; END;",
            "PARALLEL_ENABLE(GARBAGE) IS BEGIN NULL; END;", "PARALLEL_ENABLE(PARTITION c BY HASH()) IS BEGIN NULL; END;",
            "PARALLEL_ENABLE(PARTITION c BY ANY ORDER d BY ()) IS BEGIN NULL; END;",
            "RESULT_CACHE RELIES_ON() IS BEGIN NULL; END;", "RESULT_CACHE RELIES_ON(1) IS BEGIN NULL; END;",
            "ACCESSIBLE BY () IS BEGIN NULL; END;", "ACCESSIBLE BY (1) IS BEGIN NULL; END;", "ACCESSIBLE BY (a,) IS BEGIN NULL; END;",
            "ORDER c BY () IS BEGIN NULL; END;", "CLUSTER c (a) IS BEGIN NULL; END;", "AUTHID nobody IS BEGIN NULL; END;"
        ).forEach { tail ->
            // Syntax oracle: the real grammar (error recovery off) must reject the malformed unit too.
            // Syntax oracle: the grammar (error recovery off) must reject the unit, except for the
            // grammar-accepted forms the extractor deliberately declines (documented exclusions:
            // missing/trailing commas between external parameters, multi-word external datatypes,
            // non-name RELIES_ON entries).
            if (tail !in grammarAcceptedExclusions) {
                assertThat(parserAccepts("CREATE PROCEDURE bad $tail")).describedAs("parser accepts: $tail").isFalse()
            }
            val source = "CREATE PROCEDURE bad $tail\nCREATE PROCEDURE good IS BEGIN NULL; END;\n"
            val facts = procedures(source)
            assertThat(facts.map { it.name.toString() }).describedAs(tail).containsExactly("good")
            assertThat(facts.single().sourceRange).isEqualTo(SourceRange(fileId, 2, 0, 2, 41))
        }
    }

    @Test
    fun keepsParameterMetadata() {
        val p = procedures("CREATE PROCEDURE p(a NUMBER, b IN VARCHAR2 DEFAULT 'x', c OUT NUMBER, d IN OUT NOCOPY CLOB, e NUMBER := 1) IS BEGIN NULL; END;").single()
        assertThat(p.parameters.map { it.mode }).containsExactly(
            ParameterMode.IN, ParameterMode.IN, ParameterMode.OUT, ParameterMode.IN_OUT, ParameterMode.IN)
        assertThat(p.parameters.map { it.nocopy }).containsExactly(false, false, false, true, false)
        assertThat(p.parameters.map { it.defaultPresent }).containsExactly(false, true, false, false, true)
        assertThat(p.role).isEqualTo(DeclarationRole.STANDALONE)
        val anchored = procedures("CREATE PROCEDURE p(a t.col%TYPE, b t%ROWTYPE, c REF obj_t) IS BEGIN NULL; END;").single()
        assertThat(anchored.parameters.map { it.typeRef::class }).containsExactly(
            AnchoredTypeRef::class, AnchoredTypeRef::class, RefTypeRef::class)
        val quoted = procedures("CREATE PROCEDURE app.\"Run\" IS BEGIN NULL; END;").single()
        assertThat(quoted.name).isEqualTo(QualifiedName(listOf(OracleIdentifier.fromSource("APP"), OracleIdentifier.fromSource("\"Run\""))))
        assertThat(procedures("CREATE PROCEDURE p IS BEGIN NULL; END;").single().parameters).isEmpty()
    }

    @Test
    fun multipleAndMixedUnitsAreEachExtractedWithoutNestedFacts() {
        val source = """
            CREATE OR REPLACE PROCEDURE a IS
              PROCEDURE nested IS BEGIN NULL; END;
            BEGIN
              nested;
            END a;
            /
            CREATE PACKAGE pkg AS PROCEDURE m(x NUMBER); END pkg;
            /
            CREATE TYPE t AS OBJECT (id NUMBER);
            /
            CREATE SEQUENCE s;
            CREATE PROCEDURE b(x NUMBER) AS BEGIN NULL; END;
            /
            BEGIN a; END;
            /
        """.trimIndent()
        val facts = extract(source)
        assertThat(facts.map { it.kind }).containsExactly(
            ProjectDeclarationKind.STANDALONE_PROCEDURE, ProjectDeclarationKind.PACKAGE,
            ProjectDeclarationKind.PACKAGE_PROCEDURE, ProjectDeclarationKind.STANDALONE_TYPE,
            ProjectDeclarationKind.SEQUENCE, ProjectDeclarationKind.STANDALONE_PROCEDURE)
        assertThat(names(source)).containsExactly("a", "b")
        assertAgreesWithAst(source)
        // Package facts are unchanged by the new path.
        assertThat(facts.filterIsInstance<PackageProcedureDeclaration>().single().parameters.single().name.originalSpelling).isEqualTo("x")
    }

    @Test
    fun wrappedProceduresAreSkippedAndFollowingUnitsSurvive() {
        val source = "CREATE OR REPLACE PROCEDURE w wrapped\na000000\nCREATE PROCEDURE fake IS BEGIN NULL; END;\n/\n" +
            "CREATE PROCEDURE good IS BEGIN NULL; END;\n/\n"
        assertThat(names(source)).containsExactly("good")
    }

    @Test
    fun malformedParameterListsYieldNoFact() {
        listOf(
            "(IN NUMBER)", "(a NUMBER b NUMBER)", "(a NUMBER,)", "(,a NUMBER)", "(a NUMBER,,b NUMBER)", "()",
            "(a NUMBER", "(a NUMBER))", "(a IN NOCOPY NUMBER)", "(a OUT NUMBER DEFAULT 1)", "(a NUMBER DEFAULT)",
            "(a NUMBER := )", "(a)", "(a NUMBER extra)", "(a \"(\" NUMBER)", "(a NUMBER; b NUMBER)"
        ).forEach { header ->
            val source = "CREATE PROCEDURE bad$header IS BEGIN NULL; END;\n/\nCREATE PROCEDURE good IS BEGIN NULL; END;\n/\n"
            assertThat(names(source)).describedAs(header).containsExactly("good")
        }
    }

    @Test
    fun quotedCLanguageIsAnIntentionalConservativeExclusion() {
        val source = "CREATE PROCEDURE p AS LANGUAGE \"C\" LIBRARY lib;\nCREATE PROCEDURE good IS BEGIN NULL; END;\n"
        assertThat(parserAccepts(source.lines().first())).isTrue()
        assertThat(names(source)).containsExactly("good")
    }

    @Test
    fun crossedAndUnmatchedDelimitersYieldNoFact() {
        listOf(
            "(a (NUMBER])", "(a NUMBER(10,2])", "(a NUMBER DEFAULT f([1)])", "(a NUMBER DEFAULT f(1])", "(a [NUMBER))",
            "(a NUMBER)]", "(a NUMBER DEFAULT [1)", "([a NUMBER])", "(a NUMBER, b [NUMBER)"
        ).forEach { header ->
            val source = "CREATE PROCEDURE bad$header IS BEGIN NULL; END;\nCREATE PROCEDURE good IS BEGIN NULL; END;\n"
            assertThat(procedures(source).map { it.name.toString() }).describedAs(header).containsExactly("good")
            assertThat(procedures(source).single().sourceRange).isEqualTo(SourceRange(fileId, 2, 0, 2, 41))
        }
        // Properly nested mixed delimiters in a default expression remain accepted.
        assertThat(names("CREATE PROCEDURE ok(a NUMBER DEFAULT f(arr[1])) IS BEGIN NULL; END;")).containsExactly("ok")
    }

    @Test
    fun malformedUnitsDoNotSwallowFollowingUnits() {
        listOf(
            // Header without IS/AS before the next CREATE: the next unit's body must not be borrowed.
            "CREATE PROCEDURE bad\nCREATE PROCEDURE good IS BEGIN NULL; END;\n/\n",
            "CREATE PROCEDURE bad IS\n/\nBEGIN NULL; END;\n/\nCREATE PROCEDURE good IS BEGIN NULL; END;\n/\n",
            "CREATE PROCEDURE bad IS BEGIN NULL; END\nCREATE PROCEDURE good IS BEGIN NULL; END;\n",
            "CREATE PROCEDURE bad IS BEGIN CASE WHEN x THEN NULL; END;\nCREATE PROCEDURE good IS BEGIN NULL; END;\n",
            "CREATE OR REPLACE PROCEDURE IF NOT EXISTS bad IS BEGIN NULL; END;\nCREATE PROCEDURE good IS BEGIN NULL; END;\n",
            "CREATE PROCEDURE a.b.bad IS BEGIN NULL; END;\nCREATE PROCEDURE good IS BEGIN NULL; END;\n",
            "CREATE PROCEDURE bad;\nCREATE PROCEDURE good IS BEGIN NULL; END;\n",
            "CREATE PROCEDURE bad UNKNOWN_CLAUSE IS BEGIN NULL; END;\nCREATE PROCEDURE good IS BEGIN NULL; END;\n",
            "CREATE PROCEDURE bad IS \"LANGUAGE\" JAVA NAME 'x';\nCREATE PROCEDURE good IS BEGIN NULL; END;\n",
            "CREATE PROCEDURE bad AS MLE LANGUAGE JAVASCRIPT {{ never closed;\nCREATE PROCEDURE good IS BEGIN NULL; END;\n",
            "CREATE PROCEDURE bad IS BEGIN x := 1\n/\n2; END;\n/\nCREATE PROCEDURE good IS BEGIN NULL; END;\n",
        ).forEach { source ->
            val facts = procedures(source)
            assertThat(facts.map { it.name.toString() }).describedAs(source).containsExactly("good")
        }
        assertThat(procedures("CREATE PROCEDURE bad IS BEGIN NULL; END")).isEmpty()
        // IF is tracked like BEGIN and CASE: an IF without END IF leaves the body unclosed, so `bad` (which Oracle
        // rejects) gives no fact, and the scan still never reaches the next unit.
        val unclosedIf = procedures("CREATE PROCEDURE bad IS BEGIN IF x THEN NULL; END;\nCREATE PROCEDURE good IS BEGIN NULL; END;\n")
        assertThat(unclosedIf.map { it.name.toString() }).containsExactly("good")
    }

    @Test
    fun anInnerBlockLabelledLoopDoesNotLeaveTheBodyOpen() {
        // Oracle accepts any identifier after a block's END, including LOOP.
        val source = "CREATE PROCEDURE p(a NUMBER) IS\nBEGIN\n  FOR i IN 1 .. a LOOP\n    BEGIN\n      NULL;\n    END LOOP;\n  END LOOP;\nEND;\n/\n" +
            "CREATE PROCEDURE next_one IS BEGIN NULL; END;\n/\n"
        val facts = procedures(source)
        assertThat(facts.map { it.name.toString() }).containsExactly("p", "next_one")
        assertThat(facts[0].sourceRange.endLine).isEqualTo(8)
        assertAgreesWithAst(source)
    }

    /**
     * Independent oracle: real CREATE keywords come from the lexer's token types (so quoted
     * identifiers, comments and strings are excluded); SQL*Plus terminators are source lines whose
     * text is exactly `/`. Neither may lie inside an emitted fact range after its first token.
     */
    @Test
    fun noFactRangeCrossesACreateOrLoneSlash() {
        val corpus = mapOf(
            "CREATE PROCEDURE a IS BEGIN NULL; END;\nCREATE PROCEDURE b IS BEGIN NULL; END;" to listOf("a", "b"),
            "CREATE PROCEDURE a\nCREATE PROCEDURE b IS BEGIN NULL; END;\n/\nCREATE PROCEDURE c AS BEGIN NULL; END;" to listOf("b", "c"),
            "CREATE PROCEDURE a IS\n/\nCREATE PROCEDURE b AS LANGUAGE JAVA NAME 'x';\n/\n" to listOf("b"),
            "CREATE PROCEDURE a IS BEGIN x := 4\n/\n2; END;\n/\nCREATE PROCEDURE b IS BEGIN NULL; END;\n" to listOf("b"),
            // Not boundaries: quoted identifier, comment, string, division inside an expression.
            "CREATE PROCEDURE a IS \"CREATE\" NUMBER; BEGIN -- CREATE PROCEDURE x\n x := 'CREATE PROCEDURE y'; x := 4 / 2;\n x := 4\n / 2; END;\n/\n" to listOf("a")
        )
        corpus.forEach { (source, expected) ->
            val facts = procedures(source)
            assertThat(facts.map { it.name.toString() }).describedAs(source).isEqualTo(expected)
            facts.forEach { fact -> assertThat(boundaryViolations(source, fact.sourceRange)).describedAs("$source / $fact").isEmpty() }
        }
        // The oracle itself detects real boundaries and ignores lookalikes.
        val twoUnits = "CREATE PROCEDURE a IS BEGIN NULL; END;\nCREATE PROCEDURE b IS BEGIN NULL; END;"
        assertThat(boundaryViolations(twoUnits, SourceRange(fileId, 1, 0, 2, 41))).containsExactly("CREATE at 2:0")
        val slash = "CREATE PROCEDURE a IS BEGIN NULL;\n/\nEND;"
        assertThat(boundaryViolations(slash, SourceRange(fileId, 1, 0, 3, 4))).containsExactly("/ at line 2")
        val lookalikes = "CREATE PROCEDURE a IS \"CREATE\" NUMBER; BEGIN -- CREATE\n x := 'CREATE'; x := 4\n / 2; END;"
        assertThat(boundaryViolations(lookalikes, SourceRange(fileId, 1, 0, 3, 10))).isEmpty()
    }

    private fun boundaryViolations(source: String, r: SourceRange): List<String> {
        val creates = PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).lex(source)
            .filter { it.type === PlSqlKeyword.CREATE }
            .filter { (it.line > r.startLine || (it.line == r.startLine && it.column > r.startColumn)) &&
                (it.line < r.endLine || (it.line == r.endLine && it.column < r.endColumn)) }
            .map { "CREATE at ${it.line}:${it.column}" }
        val slashes = source.lines().withIndex().filter { it.value.trim() == "/" }.map { it.index + 1 }
            .filter { it in r.startLine..r.endLine }.map { "/ at line $it" }
        return creates + slashes
    }

    @Test
    fun quotedIdentifiersNeverActAsKeywordsOrPunctuators() {
        assertThat(names("\"CREATE\" PROCEDURE p IS BEGIN NULL; END;")).isEmpty()
        val p = procedures("CREATE PROCEDURE \"IS\"(\"END\" NUMBER, \"(\" \"CREATE\") AS BEGIN NULL; END;").single()
        assertThat(p.name.toString()).isEqualTo("\"IS\"")
        assertThat(p.parameters.map { it.name.originalSpelling }).containsExactly("\"END\"", "\"(\"")
        assertThat(names("CREATE PROCEDURE bad(a NUMBER \";\" b NUMBER) IS BEGIN NULL; END;")).isEmpty()
    }

    @Test
    fun standaloneOnlyProjectBecomesPreparedWithDeclarations() {
        val result = ProjectIndexPreparation().prepare(listOf(ProjectSource(fileId) { "CREATE PROCEDURE p IS BEGIN NULL; END;" }), false)
        assertThat(ProjectAnalysisContext.prepared(result).state.kind)
            .isEqualTo(ProjectAnalysisContext.State.Kind.PREPARED_WITH_DECLARATIONS)
        assertThat(result.index.findDeclarations(QualifiedName(listOf(OracleIdentifier.fromSource("p"))))).hasSize(1)
    }
}
