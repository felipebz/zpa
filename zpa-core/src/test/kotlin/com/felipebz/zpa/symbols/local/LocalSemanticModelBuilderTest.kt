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

@OptIn(ZpaExperimentalApi::class)
/** The per-file region and declaration model, on sources whose scoping was observed on Oracle. */
class LocalSemanticModelBuilderTest {

    private fun build(source: String, fileId: String = "block.sql", recovery: Boolean = false): LocalSemanticModel {
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, recovery)).parse(source)
        return LocalSemanticModelBuilder.build(root, FileId(fileId), source)
    }

    /** A compact, comparable projection of the whole model: one line per region with its declarations. */
    private fun outline(model: LocalSemanticModel): List<String> = model.regions.map { region ->
        val declarations = model.declarationsOf(region.id).joinToString(", ") { "${it.id} ${it.kind} ${it.name}" }
        "${region.id} ${region.kind}(${region.syntax}) parent=${region.parent} [$declarations] ${region.extraction}" +
            region.gaps.joinToString("") { " gap:${it.kind}:${it.syntax}" } +
            if (region.completeness.isComplete) "" else " ${region.completeness}"
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
        assertThat(model.regions).allSatisfy { assertThat(it.completeness).isSameAs(LocalRegionCompleteness.COMPLETE) }
        val (amount, v, c, code) = model.declarations
        assertThat(amount.typeRef).isEqualTo(NamedTypeRef(qualified("DATE"), amount.type!!.range))
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
            "R0 FILE(FILE) parent=null [D0 PROCEDURE t_ns01_a] ALL_ITEMS_EXTRACTED",
            "R1 SUBPROGRAM(CREATE_PROCEDURE) parent=R0 [D1 VARIABLE x] ALL_ITEMS_EXTRACTED",
            "R2 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R1 [D2 VARIABLE x] ALL_ITEMS_EXTRACTED",
            "R3 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R2 [D3 VARIABLE x] ALL_ITEMS_EXTRACTED"
        )
        // Same name, distinct facts with their own written types.
        val xs = model.declarations.filter { it.kind == LocalDeclarationKind.VARIABLE }
        assertThat(xs.map { it.typeRef!!.name.last.lookupName }).containsExactly("NUMBER", "VARCHAR2", "DATE")
        assertThat(xs.map { it.name }.distinct()).hasSize(1)

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
            "R1 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R0 [D0 VARIABLE a, D1 PROCEDURE p] ALL_ITEMS_EXTRACTED",
            "R2 SUBPROGRAM(PROCEDURE_DECLARATION) parent=R1 [] ALL_ITEMS_EXTRACTED",
            "R3 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R2 [D2 VARIABLE inner_p] ALL_ITEMS_EXTRACTED",
            "R4 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R1 [D3 VARIABLE in_if] ALL_ITEMS_EXTRACTED",
            // The loop index is a declaration of the loop's own region.
            "R5 FOR_LOOP(FOR_STATEMENT) parent=R1 [D4 LOOP_INDEX i] ALL_ITEMS_EXTRACTED",
            "R6 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R5 [D5 VARIABLE in_loop] ALL_ITEMS_EXTRACTED",
            "R7 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R1 [D6 VARIABLE in_handler] ALL_ITEMS_EXTRACTED"
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
        val (label, v) = model.declarations
        assertThat(label.kind).isEqualTo(LocalDeclarationKind.LABEL)
        assertThat(label.region).isEqualTo(blocks(model)[0].id)
        assertThat(label.range).isEqualTo(SourceRange(file, 2, 2, 2, 11))
        assertThat(label.nameRange).isEqualTo(SourceRange(file, 2, 4, 2, 9))
        assertThat(v.range).isEqualTo(SourceRange(file, 4, 4, 4, 24))
        assertThat(v.nameRange).isEqualTo(SourceRange(file, 4, 4, 4, 11))
        assertThat(v.type!!.range).isEqualTo(SourceRange(file, 4, 12, 4, 18))
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
    fun parseRecoveryIsObservableAndNeverLooksComplete() {
        // Recovery detaches the broken declarative part (holding `pkg`) from BEGIN ... END: the block that remains
        // looks valid and declares nothing, yet `pkg` was written.
        val model = build("DECLARE\n  pkg NUMBER;\n  ??? garbage;\nBEGIN\n  pkg.processar(1);\nEND;\n/\n", recovery = true)
        assertThat(model.regions).allSatisfy { assertThat(it.completeness.reasons).contains(LocalIncompleteness.PARSE_RECOVERY) }
        assertThat(model.declarations.map { it.name.lookupName }).doesNotContain("PKG")
        assertThat(model.root.gaps.map { it.kind }).contains(LocalGapKind.PARSE_RECOVERY)
        assertThat(model.root.extraction).isEqualTo(LocalExtraction.PARTIAL)
        // The surviving block's own AST shows nothing missing: only the recovery evidence reveals the hazard.
        assertThat(blocks(model).single().extraction).isEqualTo(LocalExtraction.ALL_ITEMS_EXTRACTED)
        assertThat(blocks(model).single().declarations).isEmpty()
        assertThat(blocks(model).single().completeness.isComplete).isFalse()
    }

    @Test
    fun finishedModelIsDetachedFromTheTree() {
        val source = "DECLARE\n  SUBTYPE amount_t IS DATE;\n  v amount_t;\n  pkg EXCEPTION;\n" +
            "  CURSOR c(p NUMBER) RETURN dual%ROWTYPE IS SELECT * FROM dual;\n  TYPE r IS RECORD (f NUMBER(5));\n" +
            "  TYPE aa IS TABLE OF VARCHAR2(10) INDEX BY PLS_INTEGER;\n  TYPE rc IS REF CURSOR;\n" +
            "  PROCEDURE q(a IN OUT NOCOPY NUMBER);\n  PROCEDURE q(a IN OUT NOCOPY NUMBER) IS BEGIN NULL; END;\n" +
            "  FUNCTION f(a NUMBER) RETURN NUMBER IS BEGIN RETURN a; END;\n" +
            "BEGIN\n  <<l>> DECLARE w VARCHAR2(10); BEGIN NULL; END;\n  FOR i IN 1 .. 2 LOOP \$IF \$\$x \$THEN NULL; \$END END LOOP;\nEND;\n" +
            "/\ncreate package pk is procedure a; end;\n/\ncreate package body pk is procedure a is begin null; end; end;\n/\n" +
            "create trigger trg before insert on t referencing new as n for each row begin null; end;\n/\n" +
            "create type ot as object (x number, member procedure m);\n/\ncreate type body ot as member procedure m is begin null; end; end;\n/\n"
        val model = build(source)
        assertThat(model.declarations.map { it.details?.javaClass?.simpleName }.toSet())
            .contains("LocalCursor", "LocalCursorParameter", "LocalRecordType", "LocalCollectionType", "LocalRefCursorType", "LocalSubprogram", "LocalParameter", "LocalLabel", "LocalLoopIndex", "LocalUnit", "LocalTrigger")
        assertThat(model.declarations.mapNotNull { (it.details as? LocalTrigger)?.correlationNames }).hasSize(1)
        val reached = reachable(model)
        assertThat(reached.filterIsInstance<LocalRegionCompleteness>().flatMap { it.reasons }).contains(LocalIncompleteness.CONDITIONAL_COMPILATION)
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
    fun pragmasDeclareNoNameAndLeaveNoGap() {
        val model = build("DECLARE PRAGMA AUTONOMOUS_TRANSACTION; v NUMBER; BEGIN NULL; END;\n/\n")
        val block = blocks(model).single()
        assertThat(block.gaps).describedAs(outline(model).toString()).isEmpty()
        assertThat(block.extraction).isEqualTo(LocalExtraction.ALL_ITEMS_EXTRACTED)
        assertThat(model.declarationsOf(block.id).map { it.name.lookupName }).containsExactly("V")
    }

    @Test
    fun conditionalCompilationMakesTheRegionIncomplete() {
        // The lexer keeps the $THEN branch and drops the rest, so `pkg` looks like an ordinary declaration and `other`
        // is absent: the extraction is complete for the kept tokens, but the region is not.
        val model = build("DECLARE v DATE; \$IF \$\$debug \$THEN pkg NUMBER; \$ELSE other NUMBER; \$END BEGIN NULL; END;\n/\n")
        val block = blocks(model).single()
        assertThat(model.declarationsOf(block.id).map { it.name.lookupName }).containsExactly("V", "PKG")
        assertThat(block.extraction).isEqualTo(LocalExtraction.ALL_ITEMS_EXTRACTED)
        assertThat(block.completeness.reasons).containsExactly(LocalIncompleteness.CONDITIONAL_COMPILATION)
    }

    private fun subprogram(model: LocalSemanticModel, name: String, index: Int = 0) =
        model.declarations.filter { it.details is LocalSubprogram && it.name.lookupName == name }[index]

    private fun LocalDeclaration.subprogram() = details as LocalSubprogram

    @Test
    fun standaloneSubprogramsAreDeclarationsOfTheFileWithTheirHeadings() {
        val model = build(
            """
            CREATE OR REPLACE PROCEDURE scott.p(a NUMBER, b IN OUT NOCOPY VARCHAR2, c OUT DATE, d IN NUMBER DEFAULT 1) IS
              v NUMBER;
            BEGIN
              NULL;
            END;
            /
            CREATE FUNCTION f(x employees.salary%TYPE) RETURN NUMBER IS
            BEGIN
              RETURN 1;
            END;
            /
            CREATE FUNCTION j RETURN NUMBER AS LANGUAGE JAVA NAME 'X.y() return int';
            /
            """.trimIndent()
        )
        assertThat(outline(model)).containsExactly(
            "R0 FILE(FILE) parent=null [D0 PROCEDURE p, D6 FUNCTION f, D8 FUNCTION j] ALL_ITEMS_EXTRACTED",
            "R1 SUBPROGRAM(CREATE_PROCEDURE) parent=R0 [D1 PARAMETER a, D2 PARAMETER b, D3 PARAMETER c, D4 PARAMETER d, D5 VARIABLE v] ALL_ITEMS_EXTRACTED",
            "R2 SUBPROGRAM(CREATE_FUNCTION) parent=R0 [D7 PARAMETER x] ALL_ITEMS_EXTRACTED"
        )
        val p = subprogram(model, "P")
        // The full written unit name, never only its first segment.
        assertThat(p.subprogram().unitName).isEqualTo(qualified("scott", "p"))
        assertThat(p.kind).isEqualTo(LocalDeclarationKind.PROCEDURE)
        assertThat(p.subprogram().form).isEqualTo(LocalSubprogramForm.BODY)
        assertThat(p.subprogram().body).isEqualTo(LocalRegionId(1))
        assertThat(p.subprogram().parameters.map { "${it.ordinal} ${it.name} ${it.mode} nocopy=${it.isNocopy} default=${it.hasDefault} ${it.type!!.typeRef!!.name}" })
            .containsExactly(
                "1 a IN nocopy=false default=false NUMBER",
                "2 b IN_OUT nocopy=true default=false VARCHAR2",
                "3 c OUT nocopy=false default=false DATE",
                "4 d IN nocopy=false default=true NUMBER"
            )
        // The region's parameter declarations carry the same parameter facts.
        assertThat(model.declarationsOf(LocalRegionId(1)).take(4).map { it.details })
            .containsExactlyElementsOf(p.subprogram().parameters)
        val f = subprogram(model, "F")
        assertThat(f.subprogram().returnType!!.typeRef!!.name).isEqualTo(qualified("NUMBER"))
        assertThat((f.subprogram().parameters.single().type!!.typeRef as AnchoredTypeRef).name).isEqualTo(qualified("employees", "salary"))
        val j = subprogram(model, "J")
        assertThat(j.subprogram().form).isEqualTo(LocalSubprogramForm.CALL_SPECIFICATION)
        assertThat(j.subprogram().body).isNull()
    }

    @Test
    fun parametersAndLocalsShareTheSubprogramRegion() {
        // Oracle accepts the unreferenced duplicate; a reference to `p` is an error (PLS-00410 family).
        val model = build(
            """
            create or replace procedure t_ns03_a is
              procedure inner(p number) is
                p varchar2(10) := 'local';
              begin
                say('unreferenced dup ok');
              end;
            begin
              inner(1);
            end;
            /
            """.trimIndent()
        )
        assertThat(outline(model)).containsExactly(
            "R0 FILE(FILE) parent=null [D0 PROCEDURE t_ns03_a] ALL_ITEMS_EXTRACTED",
            "R1 SUBPROGRAM(CREATE_PROCEDURE) parent=R0 [D1 PROCEDURE inner] ALL_ITEMS_EXTRACTED",
            "R2 SUBPROGRAM(PROCEDURE_DECLARATION) parent=R1 [D2 PARAMETER p, D3 VARIABLE p] ALL_ITEMS_EXTRACTED"
        )
        val (parameter, local) = model.declarationsOf(LocalRegionId(2))
        assertThat(parameter.name).isEqualTo(local.name)
        assertThat(parameter.id).isNotEqualTo(local.id)
        assertThat(subprogram(model, "INNER").subprogram().body).isEqualTo(LocalRegionId(2))
    }

    @Test
    fun forwardDeclarationsAreLinkedOnlyToTheirUniqueBody() {
        // Oracle accepts t_ns08_c: the forward declaration makes the later body callable from `a`.
        val ns08 = build(
            """
            create or replace procedure t_ns08_c is
              procedure ns08_b;
              procedure a is begin ns08_b; end;
              procedure ns08_b is begin say('local ns08_b'); end;
            begin
              a;
            end;
            /
            """.trimIndent()
        )
        val forward = subprogram(ns08, "NS08_B", 0)
        val body = subprogram(ns08, "NS08_B", 1)
        assertThat(forward.subprogram().form).isEqualTo(LocalSubprogramForm.HEADING_ONLY)
        assertThat(forward.subprogram().body).isNull()
        assertThat(forward.subprogram().counterpart).isEqualTo(body.id)
        assertThat(body.subprogram().counterpart).isEqualTo(forward.id)
        assertThat(subprogram(ns08, "A").subprogram().counterpart).isNull()

        val overloads = build(
            """
            DECLARE
              PROCEDURE k(x NUMBER);
              PROCEDURE k(x DATE);
              FUNCTION g(x NUMBER) RETURN NUMBER;
              PROCEDURE k(x DATE) IS BEGIN NULL; END;
              PROCEDURE k(x NUMBER) IS BEGIN NULL; END;
              FUNCTION g(x NUMBER) RETURN DATE IS BEGIN RETURN NULL; END;
            BEGIN
              NULL;
            END;
            """.trimIndent()
        )
        val (kNumber, kDate) = listOf(0, 1).map { subprogram(overloads, "K", it) }
        val (kDateBody, kNumberBody) = listOf(2, 3).map { subprogram(overloads, "K", it) }
        assertThat(kNumber.subprogram().counterpart).isEqualTo(kNumberBody.id)
        assertThat(kDate.subprogram().counterpart).isEqualTo(kDateBody.id)
        // Same name and parameters but a different written RETURN type: not linked.
        assertThat(subprogram(overloads, "G", 0).subprogram().counterpart).isNull()
        assertThat(subprogram(overloads, "G", 1).subprogram().counterpart).isNull()

        val ambiguous = build(
            """
            DECLARE
              PROCEDURE q(x NUMBER);
              PROCEDURE q(x NUMBER) IS BEGIN NULL; END;
              PROCEDURE q(x NUMBER) IS BEGIN NULL; END;
              PROCEDURE r(x NUMBER);
              PROCEDURE r(y NUMBER) IS BEGIN NULL; END;
              PROCEDURE s(x IN NUMBER);
              PROCEDURE s(x IN OUT NUMBER) IS BEGIN NULL; END;
            BEGIN
              NULL;
            END;
            """.trimIndent()
        )
        assertThat(ambiguous.declarations.mapNotNull { (it.details as? LocalSubprogram)?.counterpart }).isEmpty()
    }

    @Test
    fun headingTypesRecordWhereTheyAreLookedUp() {
        // Oracle: the parameter type `t` is the outer NUMBER; the RETURN type `t` is looked up in f's own region,
        // where the later local `t` makes it an error (PLS-00311).
        val model = build(
            """
            create or replace procedure lsd_g is
              subtype t is number;
              function f(a t) return t is subtype t is date; begin return sysdate; end;
            begin
              null;
            end;
            /
            """.trimIndent()
        )
        val procedure = LocalRegionId(1)
        val f = subprogram(model, "F")
        val own = f.subprogram().body!!
        val parameter = f.subprogram().parameters.single()
        assertThat(parameter.type!!.lookupRegion).isEqualTo(procedure)
        assertThat(f.subprogram().returnType!!.lookupRegion).isEqualTo(own)
        assertThat(model.declarationsOf(own).map { "${it.kind} ${it.name}" }).containsExactly("PARAMETER a", "SUBTYPE t")
        // The local `t` starts after the RETURN type: the position makes the declaration order explicit.
        val localT = model.declarationsOf(own).last()
        val returnStart = f.subprogram().returnType!!.range.let { it.startLine to it.startColumn }
        assertThat((localT.range.startLine to localT.range.startColumn) > returnStart).isTrue()
        // Ordinary declarations look their type up in their own region; a forward RETURN type in the enclosing one.
        assertThat(model.declarations.filter { it.kind == LocalDeclarationKind.SUBTYPE }.map { it.type!!.lookupRegion == it.region })
            .containsOnly(true)
        val forward = build("DECLARE FUNCTION f RETURN t; BEGIN NULL; END;\n")
        assertThat(subprogram(forward, "F").subprogram().returnType!!.lookupRegion).isEqualTo(LocalRegionId(1))
    }

    @Test
    fun loopsAndIteratorsOwnTheirIndices() {
        val model = build(
            """
            DECLARE
              TYPE t IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
              v t;
              CURSOR c IS SELECT 1 x FROM dual;
            BEGIN
              <<lp>>
              FOR i MUTABLE PLS_INTEGER IN 1 .. 3 LOOP
                NULL;
              END LOOP;
              FOR r IN c LOOP
                NULL;
              END LOOP;
              FORALL k IN 1 .. 3 DELETE FROM x WHERE id = k;
              v := t(FOR j IN 1 .. 3 => j);
            END;
            """.trimIndent()
        )
        assertThat(outline(model)).containsExactly(
            "R0 FILE(FILE) parent=null [] ALL_ITEMS_EXTRACTED",
            "R1 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R0 [D0 COLLECTION_TYPE t, D1 VARIABLE v, D2 CURSOR c, D3 LABEL lp] ALL_ITEMS_EXTRACTED",
            "R2 FOR_LOOP(FOR_STATEMENT) parent=R1 [D4 LOOP_INDEX i] ALL_ITEMS_EXTRACTED",
            "R3 FOR_LOOP(FOR_STATEMENT) parent=R1 [D5 LOOP_INDEX r] ALL_ITEMS_EXTRACTED",
            "R4 FORALL(FORALL_STATEMENT) parent=R1 [D6 LOOP_INDEX k] ALL_ITEMS_EXTRACTED",
            "R5 ITERATOR(BASIC_ITERATOR_CHOICE) parent=R1 [D7 LOOP_INDEX j] ALL_ITEMS_EXTRACTED"
        )
        val (i, r, k, j) = model.declarations.filter { it.kind == LocalDeclarationKind.LOOP_INDEX }
        val typed = i.details as LocalLoopIndex
        assertThat(typed.mutability).isEqualTo(LocalMutability.MUTABLE)
        assertThat(typed.isTypeImplicit).isFalse()
        assertThat(i.typeRef!!.name).isEqualTo(qualified("PLS_INTEGER"))
        // The index type is looked up where the loop is written.
        assertThat(i.type!!.lookupRegion).isEqualTo(LocalRegionId(1))
        // A cursor FOR loop index has an implicit row type: nothing is inferred.
        for (index in listOf(r, k, j)) {
            assertThat((index.details as LocalLoopIndex).isTypeImplicit).isTrue()
            assertThat(index.type).isNull()
        }
        assertThat((model.declarations[3].details as LocalLabel).labeled).isEqualTo(LocalRegionId(2))
    }

    @Test
    fun theForRegionContainsItsBounds() {
        // Oracle t_ad07_b: in `for i in 1..i` the bound `i` is the loop index itself (the index is in scope there).
        val model = build("create or replace procedure t_ad07_b is\nbegin\n  for i in 1..i loop\n    say('i='||i);\n  end loop;\nend;\n/\n")
        val loop = model.regions.single { it.kind == LocalRegionKind.FOR_LOOP }
        assertThat(loop.range).isEqualTo(SourceRange(FileId("block.sql"), 3, 2, 5, 11))
        val bound = SourceRange(FileId("block.sql"), 3, 14, 3, 15)
        assertThat(contains(loop.range, bound)).isTrue()
        assertThat(model.declarationsOf(loop.id).single().name.lookupName).isEqualTo("I")
    }

    @Test
    fun labelsAreDeclarationsOfTheEnclosingRegion() {
        val model = build(
            """
            create or replace procedure t_x09_b is
              x number := 1;
            begin
              <<x>>
              begin
                <<step>> null;
                <<lp>> loop exit; end loop;
              end;
            end;
            /
            <<top>>
            BEGIN
              NULL;
            END;
            /
            """.trimIndent()
        )
        assertThat(outline(model)).containsExactly(
            // The file region is not an Oracle region: a top-level block's label is visible only inside the block.
            "R0 FILE(FILE) parent=null [D0 PROCEDURE t_x09_b, D5 LABEL top] ALL_ITEMS_EXTRACTED",
            // Oracle t_x09_b: a label and a variable of the same name in one region are both accepted when unreferenced.
            "R1 SUBPROGRAM(CREATE_PROCEDURE) parent=R0 [D1 VARIABLE x, D2 LABEL x] ALL_ITEMS_EXTRACTED",
            "R2 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R1 [D3 LABEL step, D4 LABEL lp] ALL_ITEMS_EXTRACTED",
            "R3 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R0 [] ALL_ITEMS_EXTRACTED"
        )
        val labeled = model.declarations.filter { it.kind == LocalDeclarationKind.LABEL }.associate { it.name.originalSpelling to (it.details as LocalLabel).labeled }
        // A block label is linked to the block; statement and plain LOOP labels to nothing (they form no region).
        assertThat(labeled).containsExactlyInAnyOrderEntriesOf(mapOf("x" to LocalRegionId(2), "step" to null, "lp" to null, "top" to LocalRegionId(3)))
        assertThat(model.declarations[1].id).isNotEqualTo(model.declarations[2].id)
    }

    @Test
    fun remainingDeclarationItemsAreFacts() {
        val model = build(
            """
            DECLARE
              e EXCEPTION;
              e2 EXCEPTION;
              CURSOR c1 IS SELECT 1 FROM dual;
              CURSOR c2(x NUMBER, y DATE DEFAULT SYSDATE) RETURN dual%ROWTYPE IS SELECT * FROM dual WHERE x = 1;
              CURSOR c3 RETURN dual%ROWTYPE;
              TYPE rec_t IS RECORD (a NUMBER NOT NULL := 0, b VARCHAR2(10));
              TYPE nt IS TABLE OF DATE NOT NULL;
              TYPE aa IS TABLE OF NUMBER INDEX BY VARCHAR2(20);
              TYPE va IS VARRAY(10) OF rec_t;
              TYPE weak IS REF CURSOR;
              TYPE strong IS REF CURSOR RETURN dual%ROWTYPE;
            BEGIN
              NULL;
            END;
            """.trimIndent()
        )
        assertThat(outline(model)).containsExactly(
            "R0 FILE(FILE) parent=null [] ALL_ITEMS_EXTRACTED",
            "R1 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R0 [D0 EXCEPTION e, D1 EXCEPTION e2, D2 CURSOR c1, D3 CURSOR c2, D6 CURSOR c3, " +
                "D7 RECORD_TYPE rec_t, D8 COLLECTION_TYPE nt, D9 COLLECTION_TYPE aa, D10 COLLECTION_TYPE va, D11 REF_CURSOR_TYPE weak, " +
                "D12 REF_CURSOR_TYPE strong] ALL_ITEMS_EXTRACTED",
            "R2 CURSOR(CURSOR_DECLARATION) parent=R1 [D4 CURSOR_PARAMETER x, D5 CURSOR_PARAMETER y] ALL_ITEMS_EXTRACTED"
        )
        val byName = model.declarations.associateBy { it.name.lookupName }
        val c1 = byName.getValue("C1").details as LocalCursor
        val c2 = byName.getValue("C2").details as LocalCursor
        val c3 = byName.getValue("C3").details as LocalCursor
        assertThat(listOf(c1.parameters, c1.isForward, c1.returnType)).containsExactly(null, false, null)
        assertThat(c2.parameters).isEqualTo(LocalRegionId(2))
        assertThat((c2.returnType!!.typeRef as AnchoredTypeRef).anchor).isEqualTo(TypeAnchor.ROWTYPE)
        assertThat(c3.isForward).isTrue()
        assertThat(byName.getValue("Y").hasDefault).isTrue()
        assertThat((byName.getValue("Y").details as LocalCursorParameter).ordinal).isEqualTo(2)
        // Cursor parameter types are looked up where the cursor is declared.
        assertThat(byName.getValue("X").type!!.lookupRegion).isEqualTo(LocalRegionId(1))

        val record = byName.getValue("REC_T").details as LocalRecordType
        assertThat(record.fields.map { "${it.name} ${it.type!!.typeRef!!.name} notNull=${it.isNotNull} default=${it.hasDefault}" })
            .containsExactly("a NUMBER notNull=true default=true", "b VARCHAR2 notNull=false default=false")
        val nt = byName.getValue("NT")
        assertThat((nt.details as LocalCollectionType).form).isEqualTo(LocalCollectionForm.NESTED_TABLE)
        assertThat(nt.typeRef!!.name).isEqualTo(qualified("DATE"))
        assertThat(nt.isNotNull).isTrue()
        val aa = byName.getValue("AA").details as LocalCollectionType
        assertThat(aa.form).isEqualTo(LocalCollectionForm.ASSOCIATIVE_ARRAY)
        assertThat(aa.indexType!!.typeRef!!.name).isEqualTo(qualified("VARCHAR2"))
        assertThat((byName.getValue("VA").details as LocalCollectionType).form).isEqualTo(LocalCollectionForm.VARRAY)
        assertThat(byName.getValue("VA").typeRef!!.name).isEqualTo(qualified("rec_t"))
        assertThat((byName.getValue("WEAK").details as LocalRefCursorType).returnType).isNull()
        assertThat((byName.getValue("STRONG").details as LocalRefCursorType).returnType!!.typeRef).isInstanceOf(AnchoredTypeRef::class.java)
    }

    @Test
    fun packagesTriggersAndObjectTypesAreModeledRegions() {
        val model = build(
            """
            CREATE PACKAGE pkg AS
              PROCEDURE p(a NUMBER);
            END;
            /
            CREATE PACKAGE BODY pkg AS
              g NUMBER;
              PROCEDURE p(a NUMBER) IS v NUMBER; BEGIN NULL; END;
            END;
            /
            CREATE TRIGGER trg BEFORE INSERT ON t FOR EACH ROW
            DECLARE
              z NUMBER;
            BEGIN
              DECLARE w NUMBER; BEGIN NULL; END;
            END;
            /
            CREATE TYPE pt AS OBJECT (x NUMBER, MEMBER PROCEDURE m, MAP MEMBER FUNCTION o RETURN NUMBER);
            /
            CREATE TYPE BODY pt AS
              MEMBER PROCEDURE m IS BEGIN NULL; END;
              MAP MEMBER FUNCTION o RETURN NUMBER IS BEGIN RETURN x; END;
              CONSTRUCTOR FUNCTION pt(x NUMBER) RETURN SELF AS RESULT IS BEGIN RETURN; END;
            END;
            /
            """.trimIndent()
        )
        assertThat(outline(model)).containsExactly(
            "R0 FILE(FILE) parent=null [D0 PACKAGE pkg, D2 PACKAGE_BODY pkg, D7 TRIGGER trg, D10 OBJECT_TYPE pt, D14 TYPE_BODY pt] ALL_ITEMS_EXTRACTED",
            "R1 PACKAGE_SPEC(CREATE_PACKAGE) parent=R0 [D1 PROCEDURE p] ALL_ITEMS_EXTRACTED",
            "R2 PACKAGE_BODY(CREATE_PACKAGE_BODY) parent=R0 [D3 VARIABLE g, D4 PROCEDURE p] ALL_ITEMS_EXTRACTED",
            "R3 SUBPROGRAM(PROCEDURE_DECLARATION) parent=R2 [D5 PARAMETER a, D6 VARIABLE v] ALL_ITEMS_EXTRACTED",
            "R4 TRIGGER(CREATE_TRIGGER) parent=R0 [D8 VARIABLE z] ALL_ITEMS_EXTRACTED",
            "R5 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R4 [D9 VARIABLE w] ALL_ITEMS_EXTRACTED",
            "R6 OBJECT_TYPE(CREATE_TYPE) parent=R0 [D11 ATTRIBUTE x, D12 PROCEDURE m, D13 FUNCTION o] ALL_ITEMS_EXTRACTED",
            "R7 TYPE_BODY(CREATE_TYPE_BODY) parent=R0 [D15 PROCEDURE m, D16 FUNCTION o, D17 FUNCTION pt] ALL_ITEMS_EXTRACTED",
            "R8 SUBPROGRAM(PROCEDURE_DECLARATION) parent=R7 [] ALL_ITEMS_EXTRACTED",
            "R9 SUBPROGRAM(FUNCTION_DECLARATION) parent=R7 [] ALL_ITEMS_EXTRACTED",
            "R10 SUBPROGRAM(TYPE_CONSTRUCTOR) parent=R7 [D18 PARAMETER x] ALL_ITEMS_EXTRACTED"
        )
        assertWellNested(model)
    }

    /** Every child range lies inside its parent's, children follow the source, region starts are in document order. */
    private fun assertWellNested(model: LocalSemanticModel) {
        for (region in model.regions.drop(1)) {
            val parent = model.parent(region.id)!!
            assertThat(parent.children).contains(region.id)
            assertThat(contains(parent.range, region.range)).describedAs("$region in $parent").isTrue()
        }
        val starts = model.regions.drop(1).map { it.range.startLine to it.range.startColumn }
        assertThat(starts).isSortedAccordingTo(compareBy<Pair<Int, Int>>({ it.first }, { it.second }))
        val declarationStarts = model.declarations.map { it.range.startLine to it.range.startColumn }
        assertThat(declarationStarts).isSortedAccordingTo(compareBy<Pair<Int, Int>>({ it.first }, { it.second }))
    }

    @Test
    fun withClauseSubprogramsBelongToTheQueryNotToTheBlock() {
        val model = build(
            """
            DECLARE
              FUNCTION g RETURN NUMBER;
              CURSOR c IS WITH FUNCTION g RETURN NUMBER IS BEGIN RETURN 1; END; SELECT g FROM dual;
            BEGIN NULL; END;
            /
            """.trimIndent()
        )
        assertThat(outline(model)).containsExactly(
            "R0 FILE(FILE) parent=null [] ALL_ITEMS_EXTRACTED",
            "R1 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R0 [D0 FUNCTION g, D1 CURSOR c] ALL_ITEMS_EXTRACTED",
            "R2 SQL_WITH(WITH_CLAUSE) parent=R1 [D2 FUNCTION g] ALL_ITEMS_EXTRACTED",
            "R3 SUBPROGRAM(FUNCTION_DECLARATION) parent=R2 [] ALL_ITEMS_EXTRACTED"
        )
        val heading = model.declarations[0].subprogram()
        assertThat(heading.form).isEqualTo(LocalSubprogramForm.HEADING_ONLY)
        assertThat(heading.counterpart).isNull()
        val sqlFunction = model.declarations[2].subprogram()
        assertThat(sqlFunction.body).isEqualTo(LocalRegionId(3))
        assertThat(sqlFunction.counterpart).isNull()
        assertWellNested(model)
    }

    @Test
    fun withClauseSubprogramsOfAViewAreNotStandaloneUnits() {
        val model = build("CREATE OR REPLACE VIEW v AS WITH FUNCTION f RETURN NUMBER IS BEGIN RETURN 1; END; SELECT f FROM dual;\n/\n")
        assertThat(outline(model)).containsExactly(
            "R0 FILE(FILE) parent=null [] ALL_ITEMS_EXTRACTED",
            "R1 SQL_WITH(WITH_CLAUSE) parent=R0 [D0 FUNCTION f] ALL_ITEMS_EXTRACTED",
            "R2 SUBPROGRAM(FUNCTION_DECLARATION) parent=R1 [] ALL_ITEMS_EXTRACTED"
        )
        assertWellNested(model)
    }

    @Test
    fun withClauseInACursorForLoopIsNestedInTheLoop() {
        val model = build(
            """
            BEGIN
              FOR r IN (WITH FUNCTION f RETURN NUMBER IS BEGIN RETURN 1; END; SELECT f() x FROM dual) LOOP NULL; END LOOP;
            END;
            /
            """.trimIndent()
        )
        assertThat(outline(model)).containsExactly(
            "R0 FILE(FILE) parent=null [] ALL_ITEMS_EXTRACTED",
            "R1 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R0 [] ALL_ITEMS_EXTRACTED",
            "R2 FOR_LOOP(FOR_STATEMENT) parent=R1 [D0 LOOP_INDEX r] ALL_ITEMS_EXTRACTED",
            "R3 SQL_WITH(WITH_CLAUSE) parent=R2 [D1 FUNCTION f] ALL_ITEMS_EXTRACTED",
            "R4 SUBPROGRAM(FUNCTION_DECLARATION) parent=R3 [] ALL_ITEMS_EXTRACTED"
        )
        assertWellNested(model)
    }

    @Test
    fun withClauseSubprogramsShareOneRegionInOrder() {
        val model = build(
            """
            DECLARE
              CURSOR c IS WITH PROCEDURE p IS BEGIN NULL; END; FUNCTION f(a NUMBER) RETURN NUMBER IS BEGIN p; RETURN 1; END; SELECT f(1) FROM dual;
              CURSOR d IS WITH x AS (SELECT 1 a FROM dual) SELECT a FROM x;
            BEGIN NULL; END;
            /
            """.trimIndent()
        )
        // A plain factoring clause (cursor d) opens no region.
        assertThat(outline(model)).containsExactly(
            "R0 FILE(FILE) parent=null [] ALL_ITEMS_EXTRACTED",
            "R1 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R0 [D0 CURSOR c, D4 CURSOR d] ALL_ITEMS_EXTRACTED",
            "R2 SQL_WITH(WITH_CLAUSE) parent=R1 [D1 PROCEDURE p, D2 FUNCTION f] ALL_ITEMS_EXTRACTED",
            "R3 SUBPROGRAM(PROCEDURE_DECLARATION) parent=R2 [] ALL_ITEMS_EXTRACTED",
            "R4 SUBPROGRAM(FUNCTION_DECLARATION) parent=R2 [D3 PARAMETER a] ALL_ITEMS_EXTRACTED"
        )
        // Parameter types are looked up in the region enclosing the subprogram: the SQL_WITH region.
        assertThat(model.declarations[3].type!!.lookupRegion).isEqualTo(LocalRegionId(2))
        assertWellNested(model)
    }

    @Test
    fun aTypeUseIsNeverDeclaredEarlierThanItsOwnDeclaration() {
        // `SUBTYPE t IS t`: the type is looked up in the declaring region, and the owning declaration's range contains
        // the type's range, so it does not end before the type starts.
        val model = build("DECLARE\n  SUBTYPE t IS t;\nBEGIN\n  NULL;\nEND;\n")
        val t = model.declarations.single()
        assertThat(t.type!!.lookupRegion).isEqualTo(t.region)
        assertThat(contains(t.range, t.type.range)).isTrue()
    }

    private fun unit(model: LocalSemanticModel, kind: LocalDeclarationKind, index: Int = 0) =
        model.declarations.filter { it.kind == kind }[index].details as LocalUnit

    @Test
    fun packageSpecificationAndBodyAreLinked() {
        // Oracle: spec items are visible throughout the body regardless of order; a body-private item follows the
        // ordinary order rule; a spec and a body variable of the same name collide on reference.
        val model = build(
            """
            create or replace package ad4ok is
              x number := 1;
              procedure a;
              procedure b;
              function f(n number) return number;
            end;
            /
            create or replace package body ad4ok is
              x number := 2;
              procedure b;
              procedure a is begin b; end;
              procedure b is begin say('ad4ok.b'); end;
              function f(n number) return number is begin return n; end;
              procedure private_p is begin null; end;
            end;
            /
            """.trimIndent()
        )
        assertThat(outline(model)).containsExactly(
            "R0 FILE(FILE) parent=null [D0 PACKAGE ad4ok, D5 PACKAGE_BODY ad4ok] ALL_ITEMS_EXTRACTED",
            "R1 PACKAGE_SPEC(CREATE_PACKAGE) parent=R0 [D1 VARIABLE x, D2 PROCEDURE a, D3 PROCEDURE b, D4 FUNCTION f] ALL_ITEMS_EXTRACTED",
            "R2 PACKAGE_BODY(CREATE_PACKAGE_BODY) parent=R0 [D6 VARIABLE x, D7 PROCEDURE b, D8 PROCEDURE a, D9 PROCEDURE b, D10 FUNCTION f, D12 PROCEDURE private_p] ALL_ITEMS_EXTRACTED",
            "R3 SUBPROGRAM(PROCEDURE_DECLARATION) parent=R2 [] ALL_ITEMS_EXTRACTED",
            "R4 SUBPROGRAM(PROCEDURE_DECLARATION) parent=R2 [] ALL_ITEMS_EXTRACTED",
            "R5 SUBPROGRAM(FUNCTION_DECLARATION) parent=R2 [D11 PARAMETER n] ALL_ITEMS_EXTRACTED",
            "R6 SUBPROGRAM(PROCEDURE_DECLARATION) parent=R2 [] ALL_ITEMS_EXTRACTED"
        )
        val spec = unit(model, LocalDeclarationKind.PACKAGE)
        val body = unit(model, LocalDeclarationKind.PACKAGE_BODY)
        assertThat(spec.unitName).isEqualTo(qualified("ad4ok"))
        assertThat(spec.region).isEqualTo(LocalRegionId(1))
        assertThat(spec.counterpart).isEqualTo(LocalRegionId(2))
        assertThat(body.counterpart).isEqualTo(LocalRegionId(1))
        // Spec headings link to their implementations; the body forward `b` links to the same implementation.
        val links = model.declarations.filter { it.details is LocalSubprogram }
            .associate { "${it.id} ${it.name}" to (it.subprogram().specificationCounterpart to it.subprogram().counterpart) }
        assertThat(links).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "D2 a" to (LocalDeclarationId(8) to null),
                "D3 b" to (LocalDeclarationId(9) to null),
                "D4 f" to (LocalDeclarationId(10) to null),
                "D7 b" to (null to LocalDeclarationId(9)),
                "D8 a" to (LocalDeclarationId(2) to null),
                "D9 b" to (LocalDeclarationId(3) to LocalDeclarationId(7)),
                "D10 f" to (LocalDeclarationId(4) to null),
                "D12 private_p" to (null to null)
            )
        )
        // The spec and body variables `x` are distinct facts; the model does not merge the regions.
        assertThat(model.declarationsOf(LocalRegionId(1)).first().id).isNotEqualTo(model.declarationsOf(LocalRegionId(2)).first().id)
        assertWellNested(model)
    }

    @Test
    fun specificationAndBodyStayUnlinkedUnlessTheMatchIsUnique() {
        val bodyOnly = build("create package body p is procedure a is begin null; end; end;\n/\n")
        assertThat(unit(bodyOnly, LocalDeclarationKind.PACKAGE_BODY).counterpart).isNull()
        assertThat(bodyOnly.declarations.single { it.name.lookupName == "A" }.subprogram().specificationCounterpart).isNull()

        val twoSpecs = build("create package p is procedure a; end;\n/\ncreate package p is procedure a; end;\n/\ncreate package body p is procedure a is begin null; end; end;\n/\n")
        assertThat(listOf(0, 1).map { unit(twoSpecs, LocalDeclarationKind.PACKAGE, it).counterpart }).containsOnlyNulls()
        assertThat(unit(twoSpecs, LocalDeclarationKind.PACKAGE_BODY).counterpart).isNull()

        // The schema of an unqualified unit is unknown: scott.pk and pk are different written names.
        val schema = build("create package scott.pk is procedure a; end;\n/\ncreate package body pk is procedure a is begin null; end; end;\n/\n")
        assertThat(unit(schema, LocalDeclarationKind.PACKAGE).unitName).isEqualTo(qualified("scott", "pk"))
        assertThat(unit(schema, LocalDeclarationKind.PACKAGE).counterpart).isNull()
        assertThat(unit(schema, LocalDeclarationKind.PACKAGE_BODY).counterpart).isNull()

        val wrapped = build("create package w wrapped\na000000\nabcd\n/\ncreate package body w is procedure a is begin null; end; end;\n/\n")
        val wrappedSpec = unit(wrapped, LocalDeclarationKind.PACKAGE)
        assertThat(wrappedSpec.form).describedAs(outline(wrapped).toString()).isEqualTo(LocalUnitForm.WRAPPED)
        assertThat(wrappedSpec.region).isNull()
        assertThat(unit(wrapped, LocalDeclarationKind.PACKAGE_BODY).counterpart).isNull()
        for (model in listOf(bodyOnly, twoSpecs, schema, wrapped)) assertWellNested(model)
    }

    @Test
    fun packageInitializationSectionBelongsToTheBody() {
        val model = build(
            """
            create package body p is
              g number;
            begin
              <<init>>
              declare l number; begin null; end;
            end;
            /
            """.trimIndent()
        )
        assertThat(outline(model)).containsExactly(
            "R0 FILE(FILE) parent=null [D0 PACKAGE_BODY p] ALL_ITEMS_EXTRACTED",
            "R1 PACKAGE_BODY(CREATE_PACKAGE_BODY) parent=R0 [D1 VARIABLE g, D2 LABEL init] ALL_ITEMS_EXTRACTED",
            "R2 ANONYMOUS_BLOCK(BLOCK_STATEMENT) parent=R1 [D3 VARIABLE l] ALL_ITEMS_EXTRACTED"
        )
        assertThat((model.declarations[2].details as LocalLabel).labeled).isEqualTo(LocalRegionId(2))
        assertWellNested(model)
    }

    private fun trigger(model: LocalSemanticModel) = model.declarations.single { it.kind == LocalDeclarationKind.TRIGGER }.details as LocalTrigger

    @Test
    fun triggerBodiesAreRegionsAndCorrelationNamesAreTriggerFacts() {
        // Oracle tr5_bi and tr5_ref (both compile; tr5_ref renames NEW and OLD).
        val model = build(
            """
            create or replace trigger tr5_bi before insert on tr5 for each row
            when (new.a > 0)
            declare
              a number := 5;
            begin
              :new.b := a;
            end;
            /
            create or replace trigger tr5_ref before insert on tr5 referencing new as n old as o for each row
            follows tr5_bi
            begin
              :n.b := :n.b + 1000;
            end;
            /
            """.trimIndent()
        )
        assertThat(outline(model)).containsExactly(
            "R0 FILE(FILE) parent=null [D0 TRIGGER tr5_bi, D2 TRIGGER tr5_ref] ALL_ITEMS_EXTRACTED",
            "R1 TRIGGER(CREATE_TRIGGER) parent=R0 [D1 VARIABLE a] ALL_ITEMS_EXTRACTED",
            "R2 TRIGGER(CREATE_TRIGGER) parent=R0 [] ALL_ITEMS_EXTRACTED"
        )
        // The region starts at DECLARE: the WHEN condition is outside it.
        assertThat(model.region(LocalRegionId(1)).range.let { it.startLine to it.startColumn }).isEqualTo(3 to 0)
        val bi = model.declarations[0].details as LocalTrigger
        assertThat(bi.unitName).isEqualTo(qualified("tr5_bi"))
        assertThat(bi.body).isEqualTo(LocalRegionId(1))
        assertThat(bi.correlationNames.toString()).isEqualTo("new=NEW old=OLD parent=PARENT")
        val ref = model.declarations[2].details as LocalTrigger
        assertThat(ref.correlationNames!!.new).isEqualTo(OracleIdentifier.fromSource("n"))
        assertThat(ref.correlationNames.old).isEqualTo(OracleIdentifier.fromSource("o"))
        assertThat(ref.correlationNames.parent).isEqualTo(OracleIdentifier.fromSource("PARENT"))
        // The region of tr5_ref starts at BEGIN, after FOLLOWS.
        assertThat(model.region(LocalRegionId(2)).range.startLine).isEqualTo(11)
        assertWellNested(model)
    }

    @Test
    fun systemCallAndNewNamedTriggers() {
        val system = build("create trigger ddl_t after create on schema begin null; end;\n/\n")
        assertThat(trigger(system).correlationNames).isNull()
        assertThat(trigger(system).body).isEqualTo(LocalRegionId(1))

        val call = build("create trigger call_t before insert on t for each row call p(1)\n/\n")
        assertThat(outline(call)).containsExactly("R0 FILE(FILE) parent=null [D0 TRIGGER call_t] ALL_ITEMS_EXTRACTED")
        assertThat(trigger(call).body).isNull()

        // Oracle tr6_new compiles: a local variable named `new` is an ordinary declaration.
        val newNamed = build("create or replace trigger tr6_new before insert on tr6 for each row\ndeclare\n  new number := 1;\nbegin\n  :new.a := new;\nend;\n/\n")
        assertThat(outline(newNamed)).containsExactly(
            "R0 FILE(FILE) parent=null [D0 TRIGGER tr6_new] ALL_ITEMS_EXTRACTED",
            "R1 TRIGGER(CREATE_TRIGGER) parent=R0 [D1 VARIABLE new] ALL_ITEMS_EXTRACTED"
        )
        assertThat(trigger(newNamed).correlationNames!!.new).isEqualTo(newNamed.declarations[1].name)
        for (model in listOf(system, call, newNamed)) assertWellNested(model)
    }

    @Test
    fun compoundTriggerSectionsAndTimingPoints() {
        // Oracle tr5_c: g_cnt is shared by the timing points; l belongs to one.
        val model = build(
            """
            create or replace trigger tr5_c for update on tr5 compound trigger
              g_cnt pls_integer := 0;
              procedure bump is begin g_cnt := g_cnt + 1; end;
              before statement is
              begin
                g_cnt := 0;
              end before statement;
              before each row is
                l pls_integer := 1;
              begin
                g_cnt := g_cnt + l;
              end before each row;
            end tr5_c;
            /
            """.trimIndent()
        )
        assertThat(outline(model)).containsExactly(
            "R0 FILE(FILE) parent=null [D0 TRIGGER tr5_c] ALL_ITEMS_EXTRACTED",
            "R1 TRIGGER(COMPOUND_TRIGGER_BLOCK) parent=R0 [D1 VARIABLE g_cnt, D2 PROCEDURE bump] ALL_ITEMS_EXTRACTED",
            "R2 SUBPROGRAM(PROCEDURE_DECLARATION) parent=R1 [] ALL_ITEMS_EXTRACTED",
            "R3 TIMING_POINT(TIMING_POINT_SECTION) parent=R1 [] ALL_ITEMS_EXTRACTED",
            "R4 TIMING_POINT(TIMING_POINT_SECTION) parent=R1 [D3 VARIABLE l] ALL_ITEMS_EXTRACTED"
        )
        assertThat(trigger(model).correlationNames).isNotNull()
        assertWellNested(model)
    }

    @Test
    fun objectTypeSpecificationAndBody() {
        val model = build(
            """
            create type pt as object (
              x number not null,
              y varchar2(10),
              member procedure m,
              member function f return number,
              static function s return pt,
              map member function mp return number,
              constructor function pt(x number) return self as result
            );
            /
            create type body pt as
              member procedure m is begin null; end;
              member function f return number is begin return x; end;
              static function s return pt is begin return null; end;
              map member function mp return number is begin return x; end;
              constructor function pt(x number) return self as result is begin return; end;
            end;
            /
            """.trimIndent()
        )
        assertThat(outline(model)).containsExactly(
            "R0 FILE(FILE) parent=null [D0 OBJECT_TYPE pt, D8 TYPE_BODY pt] ALL_ITEMS_EXTRACTED",
            "R1 OBJECT_TYPE(CREATE_TYPE) parent=R0 [D1 ATTRIBUTE x, D2 ATTRIBUTE y, D3 PROCEDURE m, D4 FUNCTION f, D5 FUNCTION s, D6 FUNCTION mp, D7 FUNCTION pt] ALL_ITEMS_EXTRACTED",
            "R2 TYPE_BODY(CREATE_TYPE_BODY) parent=R0 [D9 PROCEDURE m, D10 FUNCTION f, D11 FUNCTION s, D12 FUNCTION mp, D13 FUNCTION pt] ALL_ITEMS_EXTRACTED",
            "R3 SUBPROGRAM(PROCEDURE_DECLARATION) parent=R2 [] ALL_ITEMS_EXTRACTED",
            "R4 SUBPROGRAM(FUNCTION_DECLARATION) parent=R2 [] ALL_ITEMS_EXTRACTED",
            "R5 SUBPROGRAM(FUNCTION_DECLARATION) parent=R2 [] ALL_ITEMS_EXTRACTED",
            "R6 SUBPROGRAM(FUNCTION_DECLARATION) parent=R2 [] ALL_ITEMS_EXTRACTED",
            "R7 SUBPROGRAM(TYPE_CONSTRUCTOR) parent=R2 [D14 PARAMETER x] ALL_ITEMS_EXTRACTED"
        )
        assertThat(unit(model, LocalDeclarationKind.OBJECT_TYPE).counterpart).isEqualTo(LocalRegionId(2))
        assertThat(unit(model, LocalDeclarationKind.TYPE_BODY).counterpart).isEqualTo(LocalRegionId(1))
        val attributes = model.declarations.filter { it.kind == LocalDeclarationKind.ATTRIBUTE }
        assertThat(attributes.map { "${it.name} ${it.typeRef!!.name} ${it.isNotNull}" }).containsExactly("x number true", "y varchar2 false")
        val methods = model.declarationsOf(LocalRegionId(1)).filter { it.details is LocalSubprogram }
        assertThat(methods.map { "${it.name} ${it.subprogram().methodKind} self=${it.subprogram().implicitSelf} ${it.subprogram().form}" })
            .containsExactly(
                "m MEMBER self=IN_OUT HEADING_ONLY",
                "f MEMBER self=IN HEADING_ONLY",
                "s STATIC self=null HEADING_ONLY",
                "mp MAP self=IN HEADING_ONLY",
                "pt CONSTRUCTOR self=IN_OUT HEADING_ONLY"
            )
        val constructor = methods.last().subprogram()
        assertThat(constructor.returnsSelf).isTrue()
        assertThat(constructor.returnType).isNull()
        // Every heading links to its implementation, constructor included.
        val implementations = model.declarationsOf(LocalRegionId(2))
        assertThat(methods.map { it.subprogram().specificationCounterpart }).containsExactlyElementsOf(implementations.map { it.id })
        assertThat(implementations.map { it.subprogram().specificationCounterpart }).containsExactlyElementsOf(methods.map { it.id })
        assertThat(implementations.map { it.subprogram().implicitSelf }).containsExactly(ParameterMode.IN_OUT, ParameterMode.IN, null, ParameterMode.IN, ParameterMode.IN_OUT)
        assertWellNested(model)
    }

    @Test
    fun explicitSelfIsAParameterAndSuppressesTheImplicitOne() {
        val model = build(
            """
            create type body pt as
              member function f(self in pt) return number is begin return 1; end;
              constructor function pt(self in out pt, x number) return self as result is begin return; end;
              order member function o(other pt) return number is begin return 0; end;
            end;
            /
            """.trimIndent()
        )
        assertThat(outline(model)).containsExactly(
            "R0 FILE(FILE) parent=null [D0 TYPE_BODY pt] ALL_ITEMS_EXTRACTED",
            "R1 TYPE_BODY(CREATE_TYPE_BODY) parent=R0 [D1 FUNCTION f, D3 FUNCTION pt, D6 FUNCTION o] ALL_ITEMS_EXTRACTED",
            "R2 SUBPROGRAM(FUNCTION_DECLARATION) parent=R1 [D2 PARAMETER self] ALL_ITEMS_EXTRACTED",
            "R3 SUBPROGRAM(TYPE_CONSTRUCTOR) parent=R1 [D4 PARAMETER self, D5 PARAMETER x] ALL_ITEMS_EXTRACTED",
            "R4 SUBPROGRAM(FUNCTION_DECLARATION) parent=R1 [D7 PARAMETER other] ALL_ITEMS_EXTRACTED"
        )
        val (f, ctor, o) = model.declarationsOf(LocalRegionId(1)).map { it.subprogram() }
        assertThat(f.implicitSelf).isNull()
        assertThat(ctor.implicitSelf).isNull()
        assertThat(ctor.parameters.map { "${it.ordinal} ${it.name} ${it.mode}" }).containsExactly("1 self IN_OUT", "2 x IN")
        assertThat(o.methodKind).isEqualTo(LocalMethodKind.ORDER)
        assertThat(o.implicitSelf).isEqualTo(ParameterMode.IN)
        // No spec in the file: the attributes come from elsewhere.
        assertThat(unit(model, LocalDeclarationKind.TYPE_BODY).counterpart).isNull()
        assertWellNested(model)
    }

    @Test
    fun collectionIncompleteAndSubtypeObjectTypes() {
        val model = build(
            """
            create type num_tab as table of number;
            /
            create type scott.names as varray(10) of varchar2(30) not null;
            /
            create type later;
            /
            create type emp_t under scott.person_t (salary number) not final;
            /
            """.trimIndent()
        )
        assertThat(outline(model)).containsExactly(
            "R0 FILE(FILE) parent=null [D0 COLLECTION_TYPE num_tab, D1 COLLECTION_TYPE names, D2 OBJECT_TYPE later, D3 OBJECT_TYPE emp_t] ALL_ITEMS_EXTRACTED",
            "R1 OBJECT_TYPE(CREATE_TYPE) parent=R0 [D4 ATTRIBUTE salary] ALL_ITEMS_EXTRACTED"
        )
        val (numTab, names, later, emp) = model.declarations
        assertThat((numTab.details as LocalCollectionType).form).isEqualTo(LocalCollectionForm.NESTED_TABLE)
        assertThat(numTab.typeRef!!.name).isEqualTo(qualified("number"))
        val namesType = names.details as LocalCollectionType
        assertThat(namesType.form).isEqualTo(LocalCollectionForm.VARRAY)
        assertThat(namesType.unitName).isEqualTo(qualified("scott", "names"))
        assertThat(names.isNotNull).isTrue()
        assertThat((later.details as LocalUnit).form).isEqualTo(LocalUnitForm.INCOMPLETE)
        assertThat(later.details.region).isNull()
        assertThat((emp.details as LocalUnit).supertype).isEqualTo(qualified("scott", "person_t"))
        assertWellNested(model)
    }

    @Test
    fun unitModelsAreDeterministicAndIndependent() {
        val source = "create package p is procedure a; end;\n/\ncreate package body p is procedure a is begin \$if \$\$x \$then null; \$end end; end;\n/\n" +
            "create trigger t for update on x compound trigger g number; before each row is l number; begin null; end before each row; end t;\n/\n" +
            "create type o as object (a number, member function f return number);\n/\ncreate type body o as member function f return number is begin return a; end; end;\n/\n"
        val full = { model: LocalSemanticModel -> outline(model) + model.declarations.map { "${it.id} ${(it.details as? LocalUnit)?.counterpart} ${(it.details as? LocalSubprogram)?.specificationCounterpart}" } }
        val expected = full(build(source))
        assertThat(expected).anyMatch { it.contains("INCOMPLETE[CONDITIONAL_COMPILATION]") }
        val pool = Executors.newFixedThreadPool(4)
        try {
            val results = (1..16).map { i -> pool.submit(Callable { full(build(if (i % 2 == 0) source else "BEGIN NULL; END;\n", "f$i.sql")) }) }.map { it.get() }
            assertThat(results.filterIndexed { i, _ -> i % 2 == 1 }).containsOnly(expected)
        } finally {
            pool.shutdown()
        }
    }

    @Test
    fun anIncompleteTypeDoesNotBlockTheLinkOfItsCompleteDefinition() {
        val model = build(
            """
            create type node_t;
            /
            create or replace type node_t as object (next_id number, member function size_of return number);
            /
            create type body node_t as
              member function size_of return number is begin return 1; end;
            end;
            /
            """.trimIndent()
        )
        assertThat(outline(model)).containsExactly(
            "R0 FILE(FILE) parent=null [D0 OBJECT_TYPE node_t, D1 OBJECT_TYPE node_t, D4 TYPE_BODY node_t] ALL_ITEMS_EXTRACTED",
            "R1 OBJECT_TYPE(CREATE_TYPE) parent=R0 [D2 ATTRIBUTE next_id, D3 FUNCTION size_of] ALL_ITEMS_EXTRACTED",
            "R2 TYPE_BODY(CREATE_TYPE_BODY) parent=R0 [D5 FUNCTION size_of] ALL_ITEMS_EXTRACTED",
            "R3 SUBPROGRAM(FUNCTION_DECLARATION) parent=R2 [] ALL_ITEMS_EXTRACTED"
        )
        val incomplete = model.declarations[0].details as LocalUnit
        assertThat(incomplete.form).isEqualTo(LocalUnitForm.INCOMPLETE)
        assertThat(incomplete.counterpart).isNull()
        assertThat((model.declarations[1].details as LocalUnit).counterpart).isEqualTo(LocalRegionId(2))
        assertThat(unit(model, LocalDeclarationKind.TYPE_BODY).counterpart).isEqualTo(LocalRegionId(1))
        assertThat(model.declarations[3].subprogram().specificationCounterpart).isEqualTo(LocalDeclarationId(5))
        assertThat(model.declarations[5].subprogram().specificationCounterpart).isEqualTo(LocalDeclarationId(3))
        // Two complete specifications still block the link.
        val twoComplete = build(
            "create type t as object (a number);\n/\ncreate or replace type t as object (a number);\n/\ncreate type body t as member procedure m is begin null; end; end;\n/\n"
        )
        assertThat(unit(twoComplete, LocalDeclarationKind.TYPE_BODY).counterpart).isNull()
        assertWellNested(model)
    }

    @Test
    fun alterTypeDeclaresNothing() {
        val model = build("alter type pt add member procedure extra cascade;\n/\n")
        assertThat(outline(model)).containsExactly("R0 FILE(FILE) parent=null [] ALL_ITEMS_EXTRACTED")
    }

    private fun rangesOf(model: LocalSemanticModel): List<SourceRange> =
        model.regions.flatMap { listOf(it.range) + it.gaps.map { gap -> gap.range } } +
            model.declarations.flatMap { listOfNotNull(it.range, it.nameRange, it.type?.range, it.typeRef?.sourceRange) }

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
