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

/** Strict extraction of standalone CREATE FUNCTION units. */
class StandaloneFunctionExtractionTest {
    private val fileId = FileId("fixture/standalone.sql")
    private val extractor = ProjectDeclarationExtractor()
    private val good = "CREATE FUNCTION good RETURN NUMBER IS BEGIN RETURN 1; END;"

    private fun extract(source: String) = extractor.extract(fileId, source)
    private fun functions(source: String) = extract(source).filterIsInstance<StandaloneFunctionDeclaration>()
    private fun names(source: String) = functions(source).map { it.name.toString() }

    private fun astFunctions(source: String): List<AstNode> =
        PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).parse(source)
            .getDescendants(PlSqlGrammar.CREATE_FUNCTION)

    private fun AstNode.range() = SourceRange(fileId, token.line, token.column, lastToken.endLine, lastToken.endColumn)

    /** Valid syntax: every fact agrees with the parser's CREATE_FUNCTION node. */
    private fun assertAgreesWithAst(source: String) {
        val facts = functions(source)
        val nodes = astFunctions(source)
        assertThat(facts).describedAs(source).hasSameSizeAs(nodes).isNotEmpty
        facts.zip(nodes).forEach { (fact, node) ->
            assertThat(fact.sourceRange).describedAs(source).isEqualTo(node.range())
            assertThat(fact.kind).isEqualTo(ProjectDeclarationKind.STANDALONE_FUNCTION)
            assertThat(fact.role).isEqualTo(DeclarationRole.STANDALONE)
            assertThat(fact.name.toString()).isEqualTo(node.getFirstChild(PlSqlGrammar.UNIT_NAME).tokens.joinToString("") { it.originalValue })
            val astParameters = node.getFirstChildOrNull(PlSqlGrammar.PARAMETER_DECLARATIONS)
                ?.getChildren(PlSqlGrammar.PARAMETER_DECLARATION).orEmpty()
            assertThat(fact.parameters.map { it.name.originalSpelling }).isEqualTo(astParameters.map { it.token.originalValue })
            fact.parameters.zip(astParameters).forEach { (parameter, ast) ->
                assertThat(parameter.sourceRange).isEqualTo(ast.range())
                assertThat(parameter.typeRef.sourceRange).isEqualTo(ast.getFirstChild(PlSqlGrammar.DATATYPE).range())
            }
            assertThat(fact.parameters.map { it.ordinal }).isEqualTo((1..fact.parameters.size).toList())
            // Return type: exactly the DATATYPE node, never a trailing property.
            val datatype = node.getFirstChild(PlSqlGrammar.DATATYPE)
            assertThat(fact.returnType.sourceRange).describedAs(source).isEqualTo(datatype.range())
            assertThat(fact.deterministic).describedAs(source)
                .isEqualTo(node.tokens.any { it.type === PlSqlKeyword.DETERMINISTIC })
        }
    }

    /** The private heading identity must equal that of the same datatype with no trailing property. */
    private fun assertReturnIdentityIsPlain(datatype: String, withTail: String) {
        val plain = functions("CREATE FUNCTION f RETURN $datatype IS BEGIN RETURN 1; END;").single().returnType
        val withProperty = functions("CREATE FUNCTION f RETURN $withTail").single().returnType
        val trailing = withTail
        assertThat(withProperty.headerIdentityKey()).describedAs(trailing).isEqualTo(plain.headerIdentityKey())
        assertThat(withProperty.structuralKey()).isEqualTo(plain.structuralKey())
        assertThat(withProperty.sourceRange.endColumn - withProperty.sourceRange.startColumn).describedAs(trailing)
            .isEqualTo(plain.sourceRange.endColumn - plain.sourceRange.startColumn)
    }

    @Test
    fun validFunctionsAgreeWithTheAstOracle() {
        listOf(
            "CREATE FUNCTION f RETURN NUMBER IS BEGIN RETURN 1; END;",
            "CREATE OR REPLACE FUNCTION app.f RETURN VARCHAR2 AS BEGIN RETURN 'x'; END f;",
            "CREATE OR REPLACE EDITIONABLE FUNCTION \"App\".\"Do.It\" RETURN NUMBER IS BEGIN RETURN 1; END;",
            "CREATE NONEDITIONABLE FUNCTION IF NOT EXISTS f RETURN NUMBER IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION f SHARING = METADATA RETURN NUMBER IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION f(a NUMBER, b IN VARCHAR2 DEFAULT 'x', c OUT NUMBER, d IN OUT NOCOPY CLOB, e NUMBER := 1) RETURN NUMBER IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION f(a t.col%TYPE, b REF obj_t, c NUMBER(10,2)) RETURN t.col%TYPE IS BEGIN RETURN NULL; END;",
            "CREATE FUNCTION f RETURN t%ROWTYPE IS BEGIN RETURN NULL; END;",
            "CREATE FUNCTION f RETURN REF obj_t IS BEGIN RETURN NULL; END;",
            "CREATE FUNCTION f RETURN NUMBER(10,2) IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION f RETURN VARCHAR2(10 CHAR) IS BEGIN RETURN 'x'; END;",
            "CREATE FUNCTION f RETURN pkg.t_rec IS BEGIN RETURN NULL; END;",
            "CREATE FUNCTION f RETURN NUMBER DETERMINISTIC IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION f RETURN NUMBER IS PROCEDURE p IS BEGIN NULL; END; FUNCTION g RETURN NUMBER IS BEGIN RETURN 1; END; " +
                "BEGIN CASE WHEN 1 = 1 THEN NULL; ELSE NULL; END CASE; RETURN g; END;",
            "CREATE FUNCTION f RETURN NUMBER AS LANGUAGE JAVA NAME 'a.B.c()';",
            "CREATE FUNCTION f RETURN NUMBER AS EXTERNAL LIBRARY lib PARAMETERS (CONTEXT, RETURN INDICATOR);",
            "CREATE FUNCTION f RETURN NUMBER AS LANGUAGE C NAME n LIBRARY lib WITH CONTEXT;",
            "CREATE FUNCTION f RETURN NUMBER AS MLE MODULE m ENV e SIGNATURE 'f()';",
            "CREATE FUNCTION f RETURN NUMBER AS MLE LANGUAGE JAVASCRIPT {{ BEGIN END; CREATE ; }};",
            "CREATE FUNCTION \"CREATE\"(\"IS\" NUMBER, \";\" NUMBER) RETURN NUMBER IS BEGIN RETURN 1; END;"
        ).forEach(::assertAgreesWithAst)
    }

    @Test
    fun everyPropertyAndBodylessFormAgreesWithTheAstOracle() {
        listOf(
            "CREATE FUNCTION f RETURN NUMBER DETERMINISTIC IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION f RETURN NUMBER PIPELINED IS BEGIN RETURN; END;",
            "CREATE FUNCTION f RETURN NUMBER PIPELINED ROW POLYMORPHIC IS BEGIN RETURN; END;",
            "CREATE FUNCTION f RETURN NUMBER PIPELINED TABLE POLYMORPHIC IS BEGIN RETURN; END;",
            "CREATE FUNCTION f RETURN NUMBER PIPELINED USING impl;",
            "CREATE FUNCTION f RETURN NUMBER PIPELINED ROW POLYMORPHIC USING s.impl;",
            "CREATE FUNCTION f RETURN NUMBER DETERMINISTIC PIPELINED USING impl;",
            "CREATE FUNCTION f RETURN NUMBER SQL_MACRO AS BEGIN RETURN 'x'; END;",
            "CREATE FUNCTION f RETURN VARCHAR2 SQL_MACRO(TABLE) AS BEGIN RETURN 'x'; END;",
            "CREATE FUNCTION f RETURN VARCHAR2 SQL_MACRO(TYPE => SCALAR) AS BEGIN RETURN 'x'; END;",
            "CREATE FUNCTION f RETURN NUMBER PARALLEL_ENABLE IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION f RETURN NUMBER PARALLEL_ENABLE(PARTITION c BY ANY) IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION f RETURN NUMBER PARALLEL_ENABLE(PARTITION c BY HASH(a, b) ORDER d BY (e)) IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION f RETURN NUMBER ORDER x BY (a) IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION f RETURN NUMBER CLUSTER x BY (a, b) IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION f RETURN NUMBER SHARD_ENABLE IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION f RETURN NUMBER RESULT_CACHE IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION f RETURN NUMBER RESULT_CACHE RELIES_ON(t, s.u) IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION f RETURN NUMBER DETERMINISTIC RESULT_CACHE IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION f RETURN NUMBER AUTHID DEFINER IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION f RETURN NUMBER DEFAULT COLLATION USING_NLS_COMP IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION f RETURN NUMBER ACCESSIBLE BY (FUNCTION g, PACKAGE s.q, r) IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION f RETURN NUMBER AGGREGATE USING impl;",
            "CREATE FUNCTION f(x NUMBER) RETURN NUMBER AGGREGATE USING s.impl;",
            "CREATE FUNCTION f RETURN NUMBER DETERMINISTIC AGGREGATE USING impl;",
            "CREATE FUNCTION f RETURN NUMBER;",
            "CREATE FUNCTION f(a NUMBER) RETURN NUMBER DETERMINISTIC;",
            "CREATE FUNCTION f RETURN NUMBER AUTHID CURRENT_USER DETERMINISTIC RESULT_CACHE SHARD_ENABLE IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION f RETURN NUMBER RESULT_CACHE AUTHID DEFINER DETERMINISTIC IS BEGIN RETURN 1; END;"
        ).forEach(::assertAgreesWithAst)
    }

    @Test
    fun returnTypeIdentityNeverIncludesTrailingProperties() {
        mapOf(
            "NUMBER AGGREGATE USING impl;" to "NUMBER",
            "NUMBER ORDER x BY (a) IS BEGIN RETURN 1; END;" to "NUMBER",
            "NUMBER CLUSTER x BY (a) IS BEGIN RETURN 1; END;" to "NUMBER",
            "NUMBER PARALLEL_ENABLE(PARTITION c BY HASH(a, b) ORDER d BY (e)) IS BEGIN RETURN 1; END;" to "NUMBER",
            "NUMBER DETERMINISTIC RESULT_CACHE IS BEGIN RETURN 1; END;" to "NUMBER",
            "NUMBER PIPELINED USING impl;" to "NUMBER",
            "NUMBER PIPELINED IS BEGIN RETURN; END;" to "NUMBER",
            "NUMBER SQL_MACRO(TABLE) AS BEGIN RETURN 'x'; END;" to "NUMBER",
            "NUMBER SHARD_ENABLE IS BEGIN RETURN 1; END;" to "NUMBER",
            "NUMBER AUTHID DEFINER IS BEGIN RETURN 1; END;" to "NUMBER",
            "NUMBER DEFAULT COLLATION USING_NLS_COMP IS BEGIN RETURN 1; END;" to "NUMBER",
            "NUMBER ACCESSIBLE BY (g) IS BEGIN RETURN 1; END;" to "NUMBER",
            "NUMBER(10,2) DETERMINISTIC IS BEGIN RETURN 1; END;" to "NUMBER(10,2)",
            "VARCHAR2(10 CHAR) ORDER x BY (a) IS BEGIN RETURN 1; END;" to "VARCHAR2(10 CHAR)",
            "t.col%TYPE AGGREGATE USING impl;" to "t.col%TYPE",
            "TIMESTAMP(6) WITH LOCAL TIME ZONE RESULT_CACHE IS BEGIN RETURN 1; END;" to "TIMESTAMP(6) WITH LOCAL TIME ZONE"
        ).forEach { (withTail, plainType) -> assertReturnIdentityIsPlain(plainType, withTail) }
    }

    @Test
    fun keepsParameterAndReturnMetadata() {
        val f = functions("CREATE FUNCTION f(a NUMBER, b OUT NUMBER) RETURN t.col%TYPE DETERMINISTIC IS BEGIN RETURN NULL; END;").single()
        assertThat(f.parameters.map { it.mode }).containsExactly(ParameterMode.IN, ParameterMode.OUT)
        assertThat(f.returnType).isInstanceOf(AnchoredTypeRef::class.java)
        assertThat(f.deterministic).isTrue()
        assertThat(functions("CREATE FUNCTION f RETURN NUMBER IS BEGIN RETURN 1; END;").single().deterministic).isFalse()
        assertThat(functions("CREATE FUNCTION f RETURN REF obj_t IS BEGIN RETURN NULL; END;").single().returnType)
            .isInstanceOf(RefTypeRef::class.java)
        assertThat(functions("CREATE FUNCTION app.\"Run\" RETURN NUMBER IS BEGIN RETURN 1; END;").single().name)
            .isEqualTo(QualifiedName(listOf(OracleIdentifier.fromSource("APP"), OracleIdentifier.fromSource("\"Run\""))))
    }

    @Test
    fun mixedUnitsAreEachExtractedWithoutNestedFacts() {
        val source = """
            CREATE OR REPLACE FUNCTION a RETURN NUMBER IS
              FUNCTION nested RETURN NUMBER IS BEGIN RETURN 1; END;
            BEGIN
              RETURN nested;
            END a;
            /
            CREATE PROCEDURE p(x NUMBER) IS BEGIN NULL; END;
            /
            CREATE PACKAGE pkg AS FUNCTION m RETURN NUMBER; END pkg;
            /
            CREATE TYPE t AS OBJECT (id NUMBER);
            CREATE SEQUENCE s;
            CREATE FUNCTION b(x NUMBER) RETURN NUMBER DETERMINISTIC;
            /
        """.trimIndent()
        assertThat(extract(source).map { it.kind }).containsExactly(
            ProjectDeclarationKind.STANDALONE_FUNCTION, ProjectDeclarationKind.STANDALONE_PROCEDURE,
            ProjectDeclarationKind.PACKAGE, ProjectDeclarationKind.PACKAGE_FUNCTION,
            ProjectDeclarationKind.STANDALONE_TYPE, ProjectDeclarationKind.SEQUENCE, ProjectDeclarationKind.STANDALONE_FUNCTION)
        assertThat(names(source)).containsExactly("a", "b")
        assertAgreesWithAst(source)
        assertThat(extract(source).filterIsInstance<StandaloneProcedureDeclaration>().single().parameters).hasSize(1)
    }

    @Test
    fun wrappedFunctionsAreSkippedAndFollowingUnitsSurvive() {
        val source = "CREATE OR REPLACE FUNCTION w wrapped\na000000\nCREATE FUNCTION fake RETURN NUMBER IS BEGIN RETURN 1; END;\n/\n$good\n/\n"
        assertThat(names(source)).containsExactly("good")
    }

    @Test
    fun malformedFunctionsYieldNoFactAndTheNextUnitSurvives() {
        listOf(
            "CREATE FUNCTION bad IS BEGIN RETURN 1; END;",                      // missing RETURN
            "CREATE FUNCTION bad RETURN IS BEGIN RETURN 1; END;",               // missing datatype
            "CREATE FUNCTION bad RETURN ??? IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION bad RETURN NUMBER(10,2 IS BEGIN RETURN 1; END;",   // unclosed datatype parentheses
            "CREATE FUNCTION bad RETURN NUMBER(10,2] IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION bad RETURN NUMBER extra IS BEGIN RETURN 1; END;",  // contaminated datatype
            "CREATE FUNCTION bad RETURN NUMBER UNKNOWN_PROP IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION bad RETURN NUMBER SQL_MACRO(GARBAGE) AS BEGIN RETURN 1; END;",
            "CREATE FUNCTION bad RETURN NUMBER ORDER x BY () IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION bad RETURN NUMBER RESULT_CACHE RELIES_ON() IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION bad RETURN NUMBER AGGREGATE USING;",               // incomplete AGGREGATE USING
            "CREATE FUNCTION bad RETURN NUMBER AGGREGATE impl;",
            "CREATE FUNCTION bad RETURN NUMBER AGGREGATE USING impl",           // no semicolon before next unit
            "CREATE FUNCTION bad RETURN NUMBER PIPELINED USING;",
            "CREATE FUNCTION bad RETURN NUMBER PIPELINED USING impl",
            "CREATE FUNCTION bad RETURN NUMBER PIPELINED POLYMORPHIC USING impl;",
            "CREATE FUNCTION bad RETURN NUMBER",                                // header only, no terminator
            "CREATE FUNCTION bad(a NUMBER b NUMBER) RETURN NUMBER IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION bad(IN NUMBER) RETURN NUMBER IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION bad() RETURN NUMBER IS BEGIN RETURN 1; END;",
            "CREATE OR REPLACE FUNCTION IF NOT EXISTS bad RETURN NUMBER IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION a.b.bad RETURN NUMBER IS BEGIN RETURN 1; END;",
            "CREATE FUNCTION bad RETURN NUMBER IS BEGIN RETURN 1; END",         // missing final semicolon
            "CREATE FUNCTION bad RETURN NUMBER IS BEGIN RETURN 1;",             // unclosed implementation
            "CREATE FUNCTION bad RETURN NUMBER IS\n/\nBEGIN RETURN 1; END;",
            "CREATE FUNCTION bad RETURN NUMBER AS LANGUAGE SQL LIBRARY lib;",
            "CREATE FUNCTION bad RETURN NUMBER AS MLE LANGUAGE JAVASCRIPT {{ never closed;"
        ).forEach { bad ->
            listOf(good to "good", "CREATE PROCEDURE good IS BEGIN NULL; END;" to "good").forEach { (next, nextName) ->
                val source = "$bad\n$next\n"
                val facts = extract(source)
                assertThat(facts.map { it.kind to it.qualifiedName().toString() }).describedAs(bad).containsExactly(
                    (if (next == good) ProjectDeclarationKind.STANDALONE_FUNCTION else ProjectDeclarationKind.STANDALONE_PROCEDURE) to nextName)
                assertThat(facts.single().sourceRange.startLine).isEqualTo(source.lines().indexOfFirst { it == next } + 1)
                assertThat(facts.single().sourceRange.startColumn).isEqualTo(0)
            }
        }
    }

    @Test
    fun quotedKeywordLookalikesNeverActAsKeywordsOrPunctuation() {
        val f = functions("CREATE FUNCTION f(\"ORDER\" NUMBER, \"(\" NUMBER) RETURN \"DEFAULT\".\"AGGREGATE\" IS BEGIN RETURN 1; END;").single()
        assertThat(f.returnType.name.toString()).isEqualTo("\"DEFAULT\".\"AGGREGATE\"")
        assertThat(f.parameters.map { it.name.originalSpelling }).containsExactly("\"ORDER\"", "\"(\"")
        assertThat(names("\"CREATE\" FUNCTION f RETURN NUMBER IS BEGIN RETURN 1; END;")).isEmpty()
        assertThat(names("CREATE FUNCTION bad RETURN NUMBER \";\" IS BEGIN RETURN 1; END;\n$good")).containsExactly("good")
        assertThat(functions("CREATE FUNCTION f RETURN \"IS\" IS BEGIN RETURN 1; END;").single().returnType.name.toString()).isEqualTo("\"IS\"")
    }

    @Test
    fun functionRangesNeverCrossACreateOrLoneSlash() {
        val corpus = listOf(
            "CREATE FUNCTION a RETURN NUMBER\nCREATE FUNCTION b RETURN NUMBER IS BEGIN RETURN 1; END;\n/\n",
            "CREATE FUNCTION a RETURN NUMBER AGGREGATE USING\n/\nCREATE FUNCTION b RETURN NUMBER;\n",
            "CREATE FUNCTION a RETURN NUMBER IS BEGIN RETURN 4\n/\n2; END;\n/\nCREATE FUNCTION b RETURN NUMBER;\n"
        )
        corpus.forEach { source ->
            val creates = PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).lex(source)
                .filter { it.type === PlSqlKeyword.CREATE }.map { it.line to it.column }
            val slashes = source.lines().withIndex().filter { it.value.trim() == "/" }.map { it.index + 1 }
            functions(source).forEach { fact ->
                val r = fact.sourceRange
                assertThat(creates.filter { (l, c) -> (l > r.startLine || (l == r.startLine && c > r.startColumn)) &&
                    (l < r.endLine || (l == r.endLine && c < r.endColumn)) }).describedAs("$source / $fact").isEmpty()
                assertThat(slashes.filter { it in r.startLine..r.endLine }).describedAs("$source / $fact").isEmpty()
            }
        }
    }

    @Test
    fun standaloneOnlyFunctionProjectBecomesPreparedWithDeclarations() {
        val result = ProjectIndexPreparation().prepare(listOf(ProjectSource(fileId) { good }), false)
        assertThat(ProjectAnalysisContext.prepared(result).state.kind).isEqualTo(ProjectAnalysisContext.State.Kind.PREPARED_WITH_DECLARATIONS)
        val collision = ProjectIndexPreparation().prepare(listOf(
            ProjectSource(FileId("a.sql")) { "CREATE FUNCTION app.f RETURN NUMBER IS BEGIN RETURN 1; END;" },
            ProjectSource(FileId("b.sql")) { "CREATE PACKAGE app AS FUNCTION f RETURN NUMBER; END app;" },
            ProjectSource(FileId("c.sql")) { "CREATE FUNCTION app.f RETURN NUMBER IS BEGIN RETURN 1; END;" }
        ), false)
        val name = QualifiedName(listOf(OracleIdentifier.fromSource("app"), OracleIdentifier.fromSource("f")))
        assertThat(collision.index.findDeclarations(name).map { it.kind }).containsExactly(
            ProjectDeclarationKind.STANDALONE_FUNCTION, ProjectDeclarationKind.PACKAGE_FUNCTION, ProjectDeclarationKind.STANDALONE_FUNCTION)
        assertThat(collision.index.findSubprograms(QualifiedName(listOf(OracleIdentifier.fromSource("app"))), OracleIdentifier.fromSource("f"))).hasSize(1)
    }
}
