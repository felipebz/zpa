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
package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.Token
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectSnapshot
import com.felipebz.zpa.api.symbols.Scope
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.collections.ArrayDeque
import kotlin.collections.Iterable
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.any
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3
import kotlin.collections.component4
import kotlin.collections.component5
import kotlin.collections.distinct
import kotlin.collections.drop
import kotlin.collections.filter
import kotlin.collections.filterIndexed
import kotlin.collections.filterIsInstance
import kotlin.collections.filterNotNull
import kotlin.collections.first
import kotlin.collections.flatMap
import kotlin.collections.forEach
import kotlin.collections.indices
import kotlin.collections.isNotEmpty
import kotlin.collections.joinToString
import kotlin.collections.last
import kotlin.collections.listOf
import kotlin.collections.listOfNotNull
import kotlin.collections.map
import kotlin.collections.plus
import kotlin.collections.single
import kotlin.collections.toList

@OptIn(ZpaExperimentalApi::class)
/** The per-file region and declaration model, on sources whose scoping was observed on Oracle. */
class LocalSemanticModelBuilderTest {

    private fun build(source: String, fileId: String = "block.sql", recovery: Boolean = false): LocalSemanticModel {
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, recovery)).parse(source)
        return LocalSemanticModelBuilder.build(root, FileId(fileId))
    }

    /** A compact, comparable projection of the whole model: one line per region with its declarations. */
    private fun outline(model: LocalSemanticModel): List<String> = model.regions.map { region ->
        val declarations = model.declarationsOf(region.id).joinToString(", ") { "${it.id} ${it.kind} ${it.name}" }
        "${region.id} ${region.kind}(${region.syntax}) parent=${region.parent} [$declarations] ${region.extraction}" +
            region.gaps.joinToString("") { " gap:${it.kind}:${it.syntax}" }
    }

    private fun blocks(model: LocalSemanticModel) = model.regions.filter { it.kind == LocalRegionKind.ANONYMOUS_BLOCK }

    @Test
    fun singleBlockWithVariablesAndSubtypes() {
        val model = build(
            """
            DECLARE
              SUBTYPE amount_t IS DATE;
              v amount_t;
              c CONSTANT NUMBER(10,2) NOT NULL := 1;
              SUBTYPE code_t IS VARCHAR2(10) NOT NULL;
            BEGIN
              NULL;
            END;
            /
            """.trimIndent()
        )
        assertThat(outline(model)).containsExactly(
            "R0 FILE(FILE) parent=null [] ALL_ITEMS_EXTRACTED",
            "R1 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R0 [D0 SUBTYPE amount_t, D1 VARIABLE v, D2 VARIABLE c, D3 SUBTYPE code_t] ALL_ITEMS_EXTRACTED"
        )
        assertThat(model.completeness).isEqualTo(LocalCompleteness.NOT_ESTABLISHED)
        val (amount, v, c, code) = model.declarations
        assertThat(amount.typeRef).isEqualTo(NamedTypeRef(qualified("DATE"), amount.typeRange))
        assertThat(v.typeRef!!.name).isEqualTo(qualified("amount_t"))
        assertThat(c.typeRef!!.name).isEqualTo(qualified("NUMBER"))
        assertThat(c.typeRef!!.sourceRange).isEqualTo(SourceRange(FileId("block.sql"), 4, 13, 4, 25))
        assertThat(listOf(c.isConstant, c.isNotNull, v.isConstant, v.isNotNull)).containsExactly(true, true, false, false)
        assertThat(code.isNotNull).isTrue()
        assertThat(model.declarations.map { it.position }).containsExactly(0, 1, 2, 3)
    }

    @Test
    fun writtenTypesArePreservedAsSyntaxEvenWhenShadowedOrUnresolvable() {
        // Oracle: `SUBTYPE number IS DATE` makes `v number` a DATE; the model keeps only the written name.
        val model = build(
            """
            DECLARE
              SUBTYPE number IS DATE;
              v number;
              w no_such_pkg.no_such_t;
              r employees%ROWTYPE;
              s employees.salary%TYPE;
            BEGIN
              NULL;
            END;
            """.trimIndent()
        )
        val (number, v, w, r, s) = model.declarations
        assertThat(number.name).isEqualTo(OracleIdentifier.fromSource("NUMBER"))
        assertThat(v.typeRef).isInstanceOf(NamedTypeRef::class.java)
        assertThat(v.typeRef!!.name).isEqualTo(qualified("number"))
        assertThat(w.typeRef!!.name).isEqualTo(qualified("no_such_pkg", "no_such_t"))
        assertThat((r.typeRef as AnchoredTypeRef).anchor).isEqualTo(TypeAnchor.ROWTYPE)
        assertThat((s.typeRef as AnchoredTypeRef).name).isEqualTo(qualified("employees", "salary"))
        assertThat(blocks(model).single().extraction).isEqualTo(LocalExtraction.ALL_ITEMS_EXTRACTED)
    }

    @Test
    fun nestedBlocksOwnTheirDeclarations() {
        // Oracle accepts this procedure: three levels reuse `x`; the innermost declaration wins at each level.
        val model = build(
            """
            create or replace procedure t_ns01_a is
              x number := 1;
            begin
              declare
                x varchar2(10) := 'inner';
              begin
                say('inner x='||x);
                declare
                  x date := date '2001-01-01';
                begin
                  say('innermost x='||to_char(x,'YYYY'));
                end;
                say('back inner x='||x);
              end;
              say('outer x='||x);
            end;
            /
            """.trimIndent()
        )
        assertThat(outline(model)).containsExactly(
            "R0 FILE(FILE) parent=null [] ALL_ITEMS_EXTRACTED",
            "R1 UNMODELED(CREATE_PROCEDURE) parent=R0 [] NOT_EXTRACTED",
            "R2 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R1 [D0 VARIABLE x] ALL_ITEMS_EXTRACTED",
            "R3 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R2 [D1 VARIABLE x] ALL_ITEMS_EXTRACTED"
        )
        // Same name, distinct facts with their own written types.
        assertThat(model.declarations.map { it.typeRef!!.name.last.lookupName }).containsExactly("VARCHAR2", "DATE")
        assertThat(model.declarations.map { it.name }.distinct()).hasSize(1)

        val anonymous = build(
            """
            DECLARE
              x NUMBER;
            BEGIN
              DECLARE
                x VARCHAR2(10);
              BEGIN
                DECLARE
                  x DATE;
                BEGIN
                  NULL;
                END;
              END;
            END;
            """.trimIndent()
        )
        val chain = generateSequence(anonymous.regions.last()) { anonymous.parent(it.id) }.map { it.id.ordinal }.toList()
        assertThat(chain).containsExactly(3, 2, 1, 0)
        assertThat(anonymous.regions.drop(1).map { anonymous.declarationsOf(it.id).single().typeRef!!.name.toString() })
            .containsExactly("NUMBER", "VARCHAR2", "DATE")
    }

    @Test
    fun siblingRegionsWithIdenticalNames() {
        val model = build(
            """
            BEGIN
              DECLARE x NUMBER; BEGIN NULL; END;
              DECLARE x DATE; BEGIN NULL; END;
            END;
            /
            DECLARE
              x NUMBER;
            BEGIN
              NULL;
            END;
            /
            """.trimIndent()
        )
        assertThat(outline(model)).containsExactly(
            "R0 FILE(FILE) parent=null [] ALL_ITEMS_EXTRACTED",
            "R1 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R0 [] ALL_ITEMS_EXTRACTED",
            "R2 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R1 [D0 VARIABLE x] ALL_ITEMS_EXTRACTED",
            "R3 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R1 [D1 VARIABLE x] ALL_ITEMS_EXTRACTED",
            "R4 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R0 [D2 VARIABLE x] ALL_ITEMS_EXTRACTED"
        )
        assertThat(model.root.children).containsExactly(LocalRegionId(1), LocalRegionId(4))
        assertThat(model.region(LocalRegionId(1)).children).containsExactly(LocalRegionId(2), LocalRegionId(3))
        assertThat(model.declarations.map { it.region }).containsExactly(LocalRegionId(2), LocalRegionId(3), LocalRegionId(4))
    }

    @Test
    fun repeatedNamesInOneRegionStayDistinct() {
        // Oracle accepts unreferenced duplicates; a reference is PLS-00371. Both are facts; neither wins.
        val model = build("DECLARE\n  x number;\n  x varchar2(10);\nBEGIN\n  NULL;\nEND;\n")
        val (first, second) = model.declarations
        assertThat(first.name).isEqualTo(second.name)
        assertThat(first.id).isNotEqualTo(second.id)
        assertThat(listOf(first.position, second.position)).containsExactly(0, 1)
        assertThat(first.nameRange).isNotEqualTo(second.nameRange)
        assertThat(model.declarationsOf(blocks(model).single().id)).containsExactly(first, second)
    }

    @Test
    fun blocksNestedInStatementsAndHandlersHaveTheirEnclosingRegionAsParent() {
        val model = build(
            """
            DECLARE
              a NUMBER;
              PROCEDURE p IS
              BEGIN
                DECLARE inner_p NUMBER; BEGIN NULL; END;
              END;
            BEGIN
              IF a = 1 THEN
                DECLARE in_if NUMBER; BEGIN NULL; END;
              END IF;
              FOR i IN 1 .. 3 LOOP
                DECLARE in_loop NUMBER; BEGIN NULL; END;
              END LOOP;
            EXCEPTION
              WHEN OTHERS THEN
                DECLARE in_handler NUMBER; BEGIN NULL; END;
            END;
            """.trimIndent()
        )
        assertThat(outline(model)).containsExactly(
            "R0 FILE(FILE) parent=null [] ALL_ITEMS_EXTRACTED",
            "R1 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R0 [D0 VARIABLE a] PARTIAL gap:UNSUPPORTED_DECLARATION:PROCEDURE_DECLARATION",
            "R2 UNMODELED(PROCEDURE_DECLARATION) parent=R1 [] NOT_EXTRACTED",
            "R3 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R2 [D1 VARIABLE inner_p] ALL_ITEMS_EXTRACTED",
            "R4 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R1 [D2 VARIABLE in_if] ALL_ITEMS_EXTRACTED",
            // The loop index is a declaration of the loop's own region, which is not modeled yet.
            "R5 UNMODELED(FOR_STATEMENT) parent=R1 [] NOT_EXTRACTED",
            "R6 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R5 [D3 VARIABLE in_loop] ALL_ITEMS_EXTRACTED",
            "R7 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R1 [D4 VARIABLE in_handler] ALL_ITEMS_EXTRACTED"
        )
        // Every child range lies inside its parent's range, and every parent lists its children.
        for (region in model.regions.drop(1)) {
            val parent = model.parent(region.id)!!
            assertThat(parent.children).contains(region.id)
            assertThat(contains(parent.range, region.range)).describedAs("$region in $parent").isTrue()
        }
    }

    @Test
    fun orderingIsDeterministicAndFollowsTheSource() {
        val source = """
            DECLARE
              a NUMBER;
            BEGIN
              DECLARE b NUMBER; c NUMBER; BEGIN NULL; END;
              DECLARE d NUMBER; BEGIN
                DECLARE e NUMBER; BEGIN NULL; END;
              END;
            END;
            """.trimIndent()
        val first = build(source)
        val second = build(source)
        assertThat(outline(second)).isEqualTo(outline(first))
        assertThat(first.declarations.map { it.name.lookupName }).containsExactly("A", "B", "C", "D", "E")
        assertThat(first.regions.map { it.id.ordinal }).isEqualTo(first.regions.indices.toList())
        assertThat(first.declarations.map { it.id.ordinal }).isEqualTo(first.declarations.indices.toList())
        val starts = first.regions.drop(1).map { it.range.startLine to it.range.startColumn }
        assertThat(starts).isSortedAccordingTo(compareBy<Pair<Int, Int>>({ it.first }, { it.second }))
        val declarationStarts = first.declarations.map { it.range.startLine to it.range.startColumn }
        assertThat(declarationStarts).isSortedAccordingTo(compareBy<Pair<Int, Int>>({ it.first }, { it.second }))
    }

    @Test
    fun sourceRangesAndIdentity() {
        val model = build("BEGIN\n  <<outer>>\n  DECLARE\n    v_total NUMBER := 0;\n  BEGIN\n    NULL;\n  END outer;\nEND;\n")
        val file = FileId("block.sql")
        val inner = blocks(model)[1]
        // The label belongs to the enclosing block: the region starts at DECLARE.
        assertThat(inner.range).isEqualTo(SourceRange(file, 3, 2, 7, 12))
        assertThat(blocks(model)[0].gaps.single().let { it.kind to it.range }).isEqualTo(LocalGapKind.LABEL to SourceRange(file, 2, 2, 2, 11))
        val v = model.declarations.single()
        assertThat(v.range).isEqualTo(SourceRange(file, 4, 4, 4, 24))
        assertThat(v.nameRange).isEqualTo(SourceRange(file, 4, 4, 4, 11))
        assertThat(v.typeRange).isEqualTo(SourceRange(file, 4, 12, 4, 18))
        assertThat(model.declaration(v.id)).isSameAs(v)
        assertThat(model.region(v.region)).isSameAs(inner)
        assertThat(model.root.range).isEqualTo(SourceRange(file, 1, 0, 8, 4))
    }

    @Test
    fun identifiersKeepOracleQuotingAndCaseSemantics() {
        // Oracle: "V" equals v; "v" is a different name; spelling is preserved.
        val model = build("DECLARE\n  \"V\" NUMBER;\n  v DATE;\n  \"v\" DATE;\n  SUBTYPE \"date\" IS NUMBER;\n  w \"date\";\nBEGIN\n  NULL;\nEND;\n")
        val (quotedUpper, plain, quotedLower, subtype, w) = model.declarations
        assertThat(quotedUpper.name).isEqualTo(plain.name)
        assertThat(quotedUpper.name.originalSpelling).isEqualTo("\"V\"")
        assertThat(quotedUpper.name.quoted).isTrue()
        assertThat(quotedLower.name).isNotEqualTo(plain.name)
        assertThat(quotedLower.name.lookupName).isEqualTo("v")
        assertThat(subtype.name).isNotEqualTo(OracleIdentifier.fromSource("DATE"))
        assertThat(w.typeRef!!.name.last).isEqualTo(subtype.name)
        assertThat(w.typeRef!!.name.last.quoted).isTrue()
    }

    @Test
    fun unsupportedDeclarationsAndLabelsAreGapsNotAbsences() {
        val model = build(
            """
            <<outer>>
            DECLARE
              pkg EXCEPTION;
              v DATE;
              CURSOR c IS SELECT 1 FROM dual;
              TYPE rec_t IS RECORD (a NUMBER);
              FUNCTION f RETURN NUMBER IS BEGIN RETURN 1; END;
              w NUMBER;
            BEGIN
              <<step>> NULL;
            END;
            """.trimIndent()
        )
        val block = blocks(model).single()
        assertThat(model.declarationsOf(block.id).map { it.name.lookupName }).containsExactly("V", "W")
        assertThat(model.declarationsOf(block.id).map { it.position }).containsExactly(0, 1)
        assertThat(block.extraction).isEqualTo(LocalExtraction.PARTIAL)
        assertThat(block.gaps.map { "${it.kind} ${it.syntax}" }).containsExactly(
            "UNSUPPORTED_DECLARATION EXCEPTION_DECLARATION",
            "UNSUPPORTED_DECLARATION CURSOR_DECLARATION",
            "UNSUPPORTED_DECLARATION RECORD_DECLARATION",
            "UNSUPPORTED_DECLARATION FUNCTION_DECLARATION",
            "LABEL LABEL"
        )
        // The block's own label is a declaration of the file-level region holding the block.
        assertThat(model.root.gaps.map { it.kind }).containsExactly(LocalGapKind.LABEL)
        assertThat(model.root.extraction).isEqualTo(LocalExtraction.PARTIAL)
    }

    @Test
    fun parseRecoveryIsObservableAndNeverLooksComplete() {
        // Recovery detaches the broken declarative part (holding `pkg`) from BEGIN ... END: the block that remains
        // looks valid and declares nothing, yet `pkg` was written.
        val model = build("DECLARE\n  pkg NUMBER;\n  ??? garbage;\nBEGIN\n  pkg.processar(1);\nEND;\n/\n", recovery = true)
        assertThat(model.completeness).isEqualTo(LocalCompleteness.PARSE_RECOVERY_OBSERVED)
        assertThat(model.declarations.map { it.name.lookupName }).doesNotContain("PKG")
        assertThat(model.root.gaps.map { it.kind }).contains(LocalGapKind.PARSE_RECOVERY)
        assertThat(model.root.extraction).isEqualTo(LocalExtraction.PARTIAL)
        // The surviving block's own AST shows nothing missing: only the file-level evidence reveals the hazard.
        assertThat(blocks(model).single().extraction).isEqualTo(LocalExtraction.ALL_ITEMS_EXTRACTED)
        assertThat(blocks(model).single().declarations).isEmpty()
        // Without recovery there is still no completeness claim: conditional compilation is invisible in the tree.
        val clean = build("DECLARE\n  v DATE;\n  \$IF \$\$debug \$THEN pkg NUMBER; \$END\nBEGIN\n  NULL;\nEND;\n")
        assertThat(clean.completeness).isEqualTo(LocalCompleteness.NOT_ESTABLISHED)
        assertThat(LocalCompleteness.entries).noneMatch { it.name.contains("COMPLETE") && !it.name.contains("NOT") }
    }

    @Test
    fun finishedModelIsDetachedFromTheTree() {
        val source = "DECLARE\n  SUBTYPE amount_t IS DATE;\n  v amount_t;\n  pkg EXCEPTION;\nBEGIN\n  <<l>> DECLARE w VARCHAR2(10); BEGIN NULL; END;\nEND;\n"
        val reached = reachable(build(source))
        assertThat(reached.filter { it is AstNode || it is Token || it is Symbol || it is Scope || it is ProjectSnapshot }).isEmpty()
        assertThat(reached.filterIsInstance<String>()).noneMatch { it.contains("BEGIN") || it.contains("amount_t IS") || it.contains("garbage") }
        assertThat(reached.map { it.javaClass.name }.filter { it.startsWith("com.felipebz.flr") || it.contains(".symbols.") && !it.contains(".symbols.local.") })
            .isEmpty()
    }

    @Test
    fun modelsOfSeparateFilesAreIndependent() {
        val a = "DECLARE\n  x NUMBER;\nBEGIN\n  DECLARE y DATE; BEGIN NULL; END;\nEND;\n"
        val b = "DECLARE\n  z VARCHAR2(1);\nBEGIN\n  NULL;\nEND;\n"
        val modelA = build(a, "a.sql")
        val outlineA = outline(modelA)
        val modelB = build(b, "b.sql")
        assertThat(outline(modelA)).isEqualTo(outlineA)
        assertThat(modelB.declarations.single().id).isEqualTo(LocalDeclarationId(0))
        assertThat(rangesOf(modelA)).allMatch { it.fileId == FileId("a.sql") }
        assertThat(rangesOf(modelB)).allMatch { it.fileId == FileId("b.sql") }
        // Builds on several threads give the same models as sequential builds: no shared construction state.
        val pool = Executors.newFixedThreadPool(4)
        try {
            val results = (1..16).map { i -> pool.submit(Callable { outline(build(if (i % 2 == 0) a else b, if (i % 2 == 0) "a.sql" else "b.sql")) }) }.map { it.get() }
            assertThat(results.filterIndexed { i, _ -> i % 2 == 1 }).containsOnly(outlineA)
            assertThat(results.filterIndexed { i, _ -> i % 2 == 0 }).containsOnly(outline(modelB))
        } finally {
            pool.shutdown()
        }
    }

    @Test
    fun forallIndexFormsAnUnmodeledRegion() {
        val model = build("DECLARE n NUMBER; BEGIN <<lbl>> FORALL i IN 1 .. 3 DELETE FROM x WHERE id = i; END;\n/\n")
        val block = blocks(model).single()
        val forall = model.regions.single { it.syntax == "FORALL_STATEMENT" }
        assertThat(forall.kind).describedAs(outline(model).toString()).isEqualTo(LocalRegionKind.UNMODELED)
        assertThat(forall.parent).isEqualTo(block.id)
        assertThat(forall.extraction).isEqualTo(LocalExtraction.NOT_EXTRACTED)
        assertThat(model.declarationsOf(block.id).map { it.name.lookupName }).containsExactly("N")
        assertThat(block.gaps.map { "${it.kind}:${it.syntax}" }).containsExactly("LABEL:LABEL")
        assertThat(block.extraction).isEqualTo(LocalExtraction.PARTIAL)
    }

    @Test
    fun typeMembersFormOneRegionEach() {
        val spec = build(
            """
            CREATE TYPE pt AS OBJECT (
              x NUMBER,
              MEMBER PROCEDURE p,
              STATIC FUNCTION s RETURN NUMBER,
              MAP MEMBER FUNCTION m RETURN NUMBER
            );
            /
            """.trimIndent()
        )
        val type = spec.regions.single { it.syntax == "CREATE_TYPE" }
        val members = spec.regions.filter { it.parent == type.id }
        assertThat(members.map { it.syntax }).describedAs(outline(spec).toString())
            .containsExactly("PROCEDURE_DECLARATION", "FUNCTION_DECLARATION", "FUNCTION_DECLARATION")
        assertThat(spec.regions).hasSize(3 + 2)
        assertThat(spec.regions.map { it.syntax }).doesNotContain("TYPE_SUBPROGRAM", "MAP_ORDER_FUNCTION")

        val body = build(
            """
            CREATE TYPE BODY pt AS
              MEMBER PROCEDURE p IS BEGIN DECLARE v NUMBER; BEGIN NULL; END; END;
              STATIC FUNCTION s RETURN NUMBER IS BEGIN RETURN 1; END;
              MAP MEMBER FUNCTION m RETURN NUMBER IS BEGIN RETURN x; END;
            END;
            /
            """.trimIndent()
        )
        val typeBody = body.regions.single { it.syntax == "CREATE_TYPE_BODY" }
        val bodyMembers = body.regions.filter { it.parent == typeBody.id }
        assertThat(bodyMembers.map { it.syntax }).describedAs(outline(body).toString())
            .containsExactly("PROCEDURE_DECLARATION", "FUNCTION_DECLARATION", "FUNCTION_DECLARATION")
        assertThat(body.regions.map { it.syntax }).doesNotContain("TYPE_SUBPROGRAM", "MAP_ORDER_FUNCTION")
        val inner = blocks(body).single { model -> body.declarationsOf(model.id).any { it.name.lookupName == "V" } }
        assertThat(inner.parent).isEqualTo(bodyMembers.first().id)
    }

    @Test
    fun pragmasDeclareNoNameAndLeaveNoGap() {
        val model = build("DECLARE PRAGMA AUTONOMOUS_TRANSACTION; v NUMBER; BEGIN NULL; END;\n/\n")
        val block = blocks(model).single()
        assertThat(block.gaps).describedAs(outline(model).toString()).isEmpty()
        assertThat(block.extraction).isEqualTo(LocalExtraction.ALL_ITEMS_EXTRACTED)
        assertThat(model.declarationsOf(block.id).map { it.name.lookupName }).containsExactly("V")
    }

    @Test
    fun conditionalCompilationIsInvisibleToTheModel() {
        // Known #436 hazard, pinned as current behavior: the lexer keeps the $THEN branch and drops the rest, so `pkg`
        // looks like an ordinary declaration, `other` is absent, and the block still claims ALL_ITEMS_EXTRACTED.
        // #436 must change this test.
        val model = build("DECLARE v DATE; \$IF \$\$debug \$THEN pkg NUMBER; \$ELSE other NUMBER; \$END BEGIN NULL; END;\n/\n")
        val block = blocks(model).single()
        assertThat(model.declarationsOf(block.id).map { it.name.lookupName }).containsExactly("V", "PKG")
        assertThat(block.extraction).isEqualTo(LocalExtraction.ALL_ITEMS_EXTRACTED)
        assertThat(model.completeness).isEqualTo(LocalCompleteness.NOT_ESTABLISHED)
    }

    @Test
    fun qualifiedExpressionIteratorsFormUnmodeledRegions() {
        val model = build(
            "DECLARE TYPE t IS TABLE OF NUMBER INDEX BY PLS_INTEGER; v t; BEGIN v := t(FOR i IN 1 .. 3 => i); END;\n/\n"
        )
        val block = blocks(model).single()
        val iterator = model.regions.single { it.syntax == "BASIC_ITERATOR_CHOICE" }
        assertThat(iterator.parent).describedAs(outline(model).toString()).isEqualTo(block.id)
        assertThat(iterator.extraction).isEqualTo(LocalExtraction.NOT_EXTRACTED)
    }

    private fun rangesOf(model: LocalSemanticModel): List<SourceRange> =
        model.regions.flatMap { listOf(it.range) + it.gaps.map { gap -> gap.range } } +
            model.declarations.flatMap { listOfNotNull(it.range, it.nameRange, it.typeRange, it.typeRef?.sourceRange) }

    private fun qualified(vararg segments: String) = com.felipebz.zpa.project.QualifiedName(segments.map(OracleIdentifier::fromSource))

    private fun contains(outer: SourceRange, inner: SourceRange): Boolean =
        (outer.startLine to outer.startColumn) <= (inner.startLine to inner.startColumn) &&
            (inner.endLine to inner.endColumn) <= (outer.endLine to outer.endColumn)

    private operator fun Pair<Int, Int>.compareTo(other: Pair<Int, Int>) = compareValuesBy(this, other, { it.first }, { it.second })

    /** Objects reachable through the fields of [root], stopping at JDK and Kotlin library internals. */
    private fun reachable(root: Any): List<Any> {
        val seen = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        val pending = ArrayDeque(listOf(root))
        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            if (!seen.add(current)) continue
            when (current) {
                is Iterable<*> -> current.filterNotNull().forEach(pending::add)
                is Map<*, *> -> (current.keys + current.values).filterNotNull().forEach(pending::add)
                else -> {
                    var type: Class<*>? = current.javaClass
                    while (type != null && !type.name.startsWith("java.") && !type.name.startsWith("kotlin.")) {
                        for (field in type.declaredFields) {
                            if (field.type.isPrimitive || java.lang.reflect.Modifier.isStatic(field.modifiers)) continue
                            field.isAccessible = true
                            field.get(current)?.let(pending::add)
                        }
                        type = type.superclass
                    }
                }
            }
        }
        return seen.toList()
    }
}
