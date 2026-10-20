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

import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.squid.PlSqlConfiguration
import com.felipebz.zpa.symbols.local.LocalReferenceContext.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/**
 * The lexical resolver on the Oracle source shapes of the cited probes. Each rule is checked on the source as written
 * (complete regions), with conditional compilation in a region the outcome relies on (incomplete, the same outcome as
 * provisional), and, where the outcome is decided outside the reference's own region, with conditional compilation in
 * a region it does not rely on (unchanged).
 */
class LocalResolverTest {

    private class Analysis(val model: LocalSemanticModel, val references: List<LocalReference>)

    private fun analyze(source: String, recovery: Boolean = false): Analysis {
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, recovery)).parse(source)
        val model = LocalSemanticModelBuilder.build(root, FileId("r.sql"), source)
        return Analysis(model, LocalReferenceCollector.collect(root, model))
    }

    private fun reference(analysis: Analysis, name: String, occurrence: Int, context: LocalReferenceContext?): LocalReference {
        val matches = analysis.references.filter { it.name == OracleIdentifier.fromSource(name) && (context == null || it.context == context) }
        check(occurrence < matches.size) { "no reference $occurrence to $name ($context) in ${analysis.references}" }
        return matches[occurrence]
    }

    private fun outcome(
        source: String,
        name: String,
        occurrence: Int = 0,
        context: LocalReferenceContext? = null,
        recovery: Boolean = false
    ): String {
        val analysis = analyze(source, recovery)
        return render(analysis.model, LocalResolver.resolve(analysis.model, reference(analysis, name, occurrence, context)))
    }

    private fun render(model: LocalSemanticModel, outcome: LocalResolution): String {
        fun d(id: LocalDeclarationId) = model.declaration(id).let { "${it.kind} ${it.name.originalSpelling}@${it.range.startLine}" }
        return when (outcome) {
            is LocalResolution.Bound -> "Bound(${d(outcome.declaration)})"
            is LocalResolution.OverloadSet -> "OverloadSet(${outcome.declarations.joinToString(", ", transform = ::d)})"
            is LocalResolution.WrongKind -> "WrongKind(${d(outcome.declaration)})"
            is LocalResolution.DeclaredLater -> "DeclaredLater(${d(outcome.declaration)})"
            is LocalResolution.Duplicate -> "Duplicate(${outcome.declarations.joinToString(", ", transform = ::d)})"
            LocalResolution.ProvenAbsent -> "ProvenAbsent"
            is LocalResolution.Incomplete ->
                "Incomplete${(outcome.regionReasons + outcome.lookupReasons).map { it.name }}(${render(model, outcome.provisional)})"
            is LocalResolution.Unsupported -> "Unsupported(${listOfNotNull(outcome.construct, outcome.kind, outcome.context).joinToString(" ")})"
            is LocalResolution.SqlColumnOrLocal -> "SqlColumnOrLocal(${d(outcome.declaration)})"
        }
    }

    /**
     * [template] marks with `/*A*/` a statement position in a region the outcome relies on, and with `/*S*/` one in a
     * region it does not rely on.
     */
    private fun assertRule(template: String, name: String, expected: String, occurrence: Int = 0, context: LocalReferenceContext? = null) {
        val plain = template.replace(A, "").replace(S, "")
        assertThat(outcome(plain, name, occurrence, context)).describedAs(plain).isEqualTo(expected)
        if (A in template) {
            val touched = template.replace(A, IF).replace(S, "")
            assertThat(outcome(touched, name, occurrence, context)).describedAs(touched).isEqualTo("Incomplete[CONDITIONAL_COMPILATION]($expected)")
        }
        if (S in template) {
            val elsewhere = template.replace(S, IF).replace(A, "")
            assertThat(outcome(elsewhere, name, occurrence, context)).describedAs(elsewhere).isEqualTo(expected)
        }
    }

    @Test
    fun anInnerLaterDeclarationIsAnOrderErrorNotTheOuterItem() {
        // NS05_B: `a number := b; b number := 1;` with an outer `b` is PLS-00320, never the outer b.
        assertRule(
            "create procedure t_ns05_b is\n  b number := 100;\nbegin\n  /*S*/ null;\n  declare\n    a number := b;\n    b number := 1;\n" +
                "  begin\n    /*A*/ null;\n  end;\nend;\n/\n",
            "b", "DeclaredLater(VARIABLE b@7)"
        )
        // NS06_B: a type used before the inner subtype of the same name.
        assertRule(
            "create procedure t_ns06_b is\n  subtype t is varchar2(10);\nbegin\n  declare\n    v t;\n    subtype t is number;\n" +
                "  begin\n    /*A*/ null;\n  end;\nend;\n/\n",
            "t", "DeclaredLater(SUBTYPE t@6)", context = TYPE
        )
        // NS08_B: a call to a later sibling hides the schema procedure of the same name (PLS-00313).
        assertRule(
            "create procedure t_ns08_b is\n  procedure a is begin ns08_b; end;\n  procedure ns08_b is begin null; end;\n" +
                "begin\n  /*A*/ a;\nend;\n/\n",
            "ns08_b", "DeclaredLater(PROCEDURE ns08_b@3)"
        )
        // NS05_C (control): without the inner b the outer one binds, and the inner block's state does not matter for it.
        assertThat(outcome("create procedure t is\n  b number := 100;\nbegin\n  declare\n    a number := b;\n  begin null; end;\nend;\n/\n", "b"))
            .isEqualTo("Bound(VARIABLE b@2)")
    }

    @Test
    fun specificationItemsAreVisibleThroughoutTheBody() {
        val spec = "create package ad4 is\n  procedure a;\n  procedure b;\nend;\n/\n"
        // AD04 ad4ok: `b` is defined after `a` in the body but declared in the specification.
        assertRule(
            spec + "create package body ad4 is\n  procedure a is begin /*A*/ b; end;\n  procedure b is begin null; end;\nend;\n/\n" +
                "create procedure other is begin /*S*/ null; end;\n/\n",
            "b", "OverloadSet(PROCEDURE b@8)"
        )
        // AD04 ad4bad: a private procedure defined later is an order error (PLS-00313).
        assertRule(
            spec + "create package body ad4 is\n  procedure a is begin /*A*/ c; end;\n  procedure b is begin null; end;\n" +
                "  procedure c is begin null; end;\nend;\n/\n",
            "c", "DeclaredLater(PROCEDURE c@9)"
        )
        // AD04 ad4dup: a specification variable and a body variable collide like one region (PLS-00371).
        assertRule(
            "create package ad4dup is\n  x number := 1;\n  procedure show;\nend;\n/\n" +
                "create package body ad4dup is\n  x number := 2;\n  procedure show is begin /*A*/ say(x); end;\nend;\n/\n",
            "x", "Duplicate(VARIABLE x@2, VARIABLE x@7)"
        )
        // AD04 ad4ord: inside the specification the ordinary order rule applies (PLS-00320).
        assertThat(outcome("create package ad4ord is\n  c1 constant number := c2;\n  c2 constant number := 1;\nend;\n/\n", "c2"))
            .isEqualTo("DeclaredLater(VARIABLE c2@3)")
    }

    @Test
    fun recursionAndForwardDeclarations() {
        // SP05_A: direct recursion, and mutual recursion through a forward declaration.
        val sp05a = "create procedure t_sp05_a is\n" +
            "  function fact(n number) return number is begin /*A*/ return n * fact(n - 1); end;\n" +
            "  function is_odd(n number) return boolean;\n" +
            "  function is_even(n number) return boolean is begin return is_odd(n - 1); end;\n" +
            "  function is_odd(n number) return boolean is begin return is_even(n - 1); end;\n" +
            "begin\n  null;\nend;\n/\n"
        assertRule(sp05a, "fact", "OverloadSet(FUNCTION fact@2)")
        assertThat(outcome(sp05a.replace(A, ""), "is_odd")).isEqualTo("OverloadSet(FUNCTION is_odd@5)")
        // SP05_B: without the forward declaration the later sibling is an order error (PLS-00313).
        assertThat(
            outcome(
                "create procedure t_sp05_b is\n  function is_even(n number) return boolean is begin return is_odd2(n - 1); end;\n" +
                    "  function is_odd2(n number) return boolean is begin return is_even(n - 1); end;\nbegin\n  null;\nend;\n/\n",
                "is_odd2"
            )
        ).isEqualTo("DeclaredLater(FUNCTION is_odd2@3)")
        // SP05_C: a forward declaration and two bodies of one heading (PLS-00305).
        assertThat(
            outcome(
                "create procedure t_sp05_c is\n  procedure f(n number);\n  procedure f(n number) is begin null; end;\n" +
                    "  procedure f(n number) is begin null; end;\nbegin\n  f(1);\nend;\n/\n",
                "f"
            )
        ).isEqualTo("Duplicate(PROCEDURE f@2, PROCEDURE f@3, PROCEDURE f@4)")
    }

    @Test
    fun duplicatesAndMixedCallableKinds() {
        // NS03: a parameter and a local of the same name share one region (PLS-00410).
        assertRule(
            "create procedure t_ns03_b is\n  procedure inner(p number) is\n    p varchar2(10) := 'local';\n" +
                "  begin\n    /*A*/ say(p);\n  end;\nbegin\n  /*S*/ inner(1);\nend;\n/\n",
            "p", "Duplicate(PARAMETER p@2, VARIABLE p@3)"
        )
        // NS04_B: two variables, referenced (PLS-00371).
        assertRule("create procedure t_ns04_b is\n  x number;\n  x varchar2(10);\nbegin\n  /*A*/ x := 1;\nend;\n/\n", "x", "Duplicate(VARIABLE x@2, VARIABLE x@3)")
        // NS04_D: a variable and a procedure (PLS-00305 at the declaration, PLS-00221 at the call).
        assertRule(
            "create procedure t_ns04_d is\n  x number;\n  procedure x(a number) is begin null; end;\nbegin\n  /*A*/ x(1);\nend;\n/\n",
            "x", "Duplicate(VARIABLE x@2, PROCEDURE x@3)", context = CALL_STATEMENT
        )
        // X09_A: an exception and a variable share the namespace (PLS-00371).
        assertRule("create procedure t_x09_a is\n  e exception;\n  e number;\nbegin\n  /*A*/ e := 1;\nend;\n/\n", "e", "Duplicate(EXCEPTION e@2, VARIABLE e@3)")
        // X09_C: a label in the body and a variable of the region (PLS-00371).
        assertRule(
            "create procedure t_x09_c is\n  x number := 1;\nbegin\n  <<x>>\n  begin\n    null;\n  end;\n  /*A*/ say(x);\nend;\n/\n",
            "x", "Duplicate(VARIABLE x@2, LABEL x@4)"
        )
        // X05_B: an inner exception hides an outer variable in RAISE and WHEN.
        val x05b = "create procedure t_x05_b is\n  e number := 1;\nbegin\n  declare\n    e exception;\n  begin\n    /*A*/ raise e;\n" +
            "  exception when e then null;\n  end;\n  say(e);\nend;\n/\n"
        assertRule(x05b, "e", "Bound(EXCEPTION e@5)", context = EXCEPTION)
        assertThat(outcome(x05b.replace(A, ""), "e", 1, EXCEPTION)).isEqualTo("Bound(EXCEPTION e@5)")
        assertThat(outcome(x05b.replace(A, ""), "e", 0, VALUE)).isEqualTo("Bound(VARIABLE e@2)")
    }

    @Test
    fun aLabelDecidesItsRegion() {
        // NS09_A: `outer.x` inside the inner block reaches the label of the enclosing region.
        assertRule(
            "create procedure t_ns09_a is\nbegin\n  <<Outer>>\n  declare\n    x number := 1;\n  begin\n    declare\n      x number := 2;\n" +
                "    begin\n      /*A*/ say(OUTER.x);\n    end;\n  end;\nend;\n/\n",
            "outer", "Bound(LABEL Outer@3)"
        )
        // NS10_C / NS14_A: the label captures `pkg10` over the whole enclosing region, before and after the block.
        val ns14a = "create procedure t_ns14_a is\nbegin\n  /*A*/ say(pkg14.x);\n  <<pkg14>>\n  declare\n    x varchar2(10);\n" +
            "  begin\n    null;\n  end;\n  say(pkg14.x);\nend;\n/\n"
        assertRule(ns14a, "pkg14", "Bound(LABEL pkg14@4)")
        assertThat(outcome(ns14a.replace(A, ""), "pkg14", 1)).isEqualTo("Bound(LABEL pkg14@4)")
        // X06_A: a record variable and a label of the same region (PLS-00371).
        assertRule(
            "create procedure t_x06_a is\n  type r_t is record(x varchar2(10));\n  b r_t;\nbegin\n  /*A*/ b.x := 'rec';\n  <<b>>\n" +
                "  declare\n    x varchar2(10);\n  begin\n    null;\n  end;\nend;\n/\n",
            "b", "Duplicate(VARIABLE b@3, LABEL b@6)"
        )
        // KC (VALUE x label): a label used as a value is captured and rejected.
        assertThat(outcome("create procedure t is\n  x number;\nbegin\n  <<k1>>\n  begin\n    x := k1;\n  end;\nend;\n/\n", "k1"))
            .isEqualTo("WrongKind(LABEL k1@4)")
        // NS14_D: two sibling blocks with the same label (PLS-00371).
        assertThat(
            outcome(
                "create procedure t_ns14_d is\nbegin\n  <<lab>>\n  begin null; end;\n  <<lab>>\n  declare x number; begin say(lab.x); end;\nend;\n/\n",
                "lab"
            )
        ).isEqualTo("Duplicate(LABEL lab@3, LABEL lab@5)")
    }

    @Test
    fun theInnermostOverloadSetHidesOuterOnes() {
        // SP07_B: an inner f(date) hides the outer f(number): only the inner one is a candidate (PLS-00306).
        assertRule(
            "create procedure t_sp07_b is\n  procedure f(p number) is begin null; end;\nbegin\n  /*S*/ null;\n" +
                "  declare\n    procedure f(p date) is begin null; end;\n  begin\n    /*A*/ f(1);\n  end;\nend;\n/\n",
            "f", "OverloadSet(PROCEDURE f@6)"
        )
        // SP08_A / SP06: overloads of one region form one set.
        assertThat(
            outcome("create procedure t is\n  procedure f8(p varchar2) is begin null; end;\n  procedure f8(p date) is begin null; end;\nbegin\n  f8(1);\nend;\n/\n", "f8")
        ).isEqualTo("OverloadSet(PROCEDURE f8@2, PROCEDURE f8@3)")
        // SP06_C / SP07_E: a variable named like a procedure is not callable (PLS-00221).
        assertRule("create procedure t_sp06_c is\n  sp6 number := 1;\nbegin\n  /*A*/ sp6;\nend;\n/\n", "sp6", "WrongKind(VARIABLE sp6@2)")
        assertRule(
            "create procedure t_sp07_e is\n  procedure f(p number) is begin null; end;\nbegin\n  declare\n    f number := 1;\n" +
                "  begin\n    /*A*/ f(1);\n  end;\nend;\n/\n",
            "f", "WrongKind(VARIABLE f@5)"
        )
    }

    @Test
    fun predefinedTypeNamesAreOrdinaryNames() {
        // LT07_C / LT08: a local subtype named like a predefined type binds the type position.
        assertRule("create procedure t_lt07_c is\n  subtype number is date;\n  v number;\nbegin\n  /*A*/ null;\nend;\n/\n", "number", "Bound(SUBTYPE number@2)", context = TYPE)
        // LT09: constrained syntax names the same local subtype.
        assertThat(outcome("create procedure t is\n  subtype varchar2 is number;\n  v varchar2(10);\nbegin\n  null;\nend;\n/\n", "varchar2", context = TYPE))
            .isEqualTo("Bound(SUBTYPE varchar2@2)")
        // LT07_D: a variable named `integer` hides the type and is not one (PLS-00488).
        assertRule("create procedure t_lt07_d is\n  integer number := 5;\n  v integer;\nbegin\n  /*A*/ null;\nend;\n/\n", "integer", "WrongKind(VARIABLE integer@2)", context = TYPE)
        // Without a local declaration a predefined name is only absent locally: STANDARD is the caller's next step.
        assertThat(outcome("create procedure t is\n  v number;\nbegin\n  null;\nend;\n/\n", "number", context = TYPE)).isEqualTo("ProvenAbsent")
    }

    @Test
    fun parameterTypesAndReturnTypesUseTheirOwnRegions() {
        // OP1X lsd_g2: a parameter type is looked up in the enclosing region, ignoring the subprogram's local `t`.
        assertRule(
            "create procedure lsd_g2 is\n  subtype t is number;\n  procedure p(a t) is subtype t is date; begin null; end;\nbegin\n  /*A*/ p(1);\nend;\n/\n",
            "t", "Bound(SUBTYPE t@2)", context = TYPE
        )
        // OP1X lsd_g1: the RETURN type is looked up in the function's own region, where `t` is declared later (PLS-00311).
        assertRule(
            "create procedure lsd_g1 is\n  subtype t is number;\n  function f return t is subtype t is date; begin /*A*/ return sysdate; end;\n" +
                "begin\n  null;\nend;\n/\n",
            "t", "DeclaredLater(SUBTYPE t@3)", occurrence = 0, context = TYPE
        )
        // OP1X lsd_g5: without a local `t` the RETURN type reaches the enclosing one.
        assertThat(
            outcome("create procedure lsd_g5 is\n  subtype t is number;\n  function f return t is subtype t2 is date; begin return 1; end;\nbegin\n  null;\nend;\n/\n", "t", context = TYPE)
        ).isEqualTo("Bound(SUBTYPE t@2)")
    }

    @Test
    fun aForIndexIsVisibleInItsOwnBounds() {
        // AD07_B: `for i in 1..i` compiles with no other `i`.
        assertRule("create procedure t_ad07_b is\nbegin\n  for i in 1..i loop\n    /*A*/ null;\n  end loop;\nend;\n/\n", "i", "Bound(LOOP_INDEX i@3)")
        // AD02_B: the index is not visible after the loop.
        assertThat(outcome("create procedure t_ad02_b is\nbegin\n  for j in 1..2 loop null; end loop;\n  say(j);\nend;\n/\n", "j"))
            .isEqualTo("ProvenAbsent")
    }

    @Test
    fun aSqlNameIsAColumnOrALocal() {
        // AD06_A: in SQL a column beats a local of the same name; INTO targets are PL/SQL.
        val ad06 = "create procedure t_ad06_a is\n  x number := 2;\n  n number;\nbegin\n  /*A*/ select count(*) into n from emp6 where x = x and c1 = 1;\nend;\n/\n"
        assertRule(ad06, "x", "SqlColumnOrLocal(VARIABLE x@2)", context = SQL_VALUE)
        assertRule(ad06, "n", "Bound(VARIABLE n@3)", context = VALUE)
        // A name no region declares may still be a column: never a proven absence.
        assertThat(outcome(ad06.replace(A, ""), "c1")).isEqualTo("Incomplete[SQL_COLUMN_UNKNOWN](ProvenAbsent)")
        // AD01_B: a cursor parameter in its query.
        assertThat(outcome("create procedure t is\n  cursor c(a number) is select 1 from t where b = a;\nbegin\n  null;\nend;\n/\n", "a", context = SQL_VALUE))
            .isEqualTo("SqlColumnOrLocal(CURSOR_PARAMETER a@2)")
    }

    @Test
    fun timingPointsSeeTheirSectionAndTriggerNamesAreNotQualifiers() {
        // AD05B tr5_c: a timing point sees the compound trigger's section declarations.
        assertRule(
            "create trigger tr5_c for update on tr5 compound trigger\n  g_cnt pls_integer := 0;\n  before each row is\n" +
                "    l pls_integer := 1;\n  begin\n    /*A*/ g_cnt := g_cnt + l;\n  end before each row;\nend tr5_c;\n/\n" +
                "create procedure other is begin /*S*/ null; end;\n/\n",
            "g_cnt", "Bound(VARIABLE g_cnt@2)"
        )
        // AD05 tr5_q: a trigger's name does not qualify inside it (PLS-00201).
        assertThat(outcome("create trigger tr5_q before insert on tr5 for each row\ndeclare\n  a number := 7;\nbegin\n  say(tr5_q.a);\nend;\n/\n", "tr5_q"))
            .isEqualTo("ProvenAbsent")
    }

    @Test
    fun identifiersCompareAsOracleIdentifiers() {
        // NS12_A: "x" and x are distinct; "X" is x.
        val ns12a = "create procedure t_ns12_a is\n  \"x\" number := 1;\n  x number := 2;\nbegin\n  /*A*/ say(\"x\" + x + \"X\");\nend;\n/\n"
        assertRule(ns12a, "\"x\"", "Bound(VARIABLE \"x\"@2)")
        assertThat(outcome(ns12a.replace(A, ""), "x")).isEqualTo("Bound(VARIABLE x@3)")
        assertThat(outcome(ns12a.replace(A, ""), "\"X\"")).isEqualTo("Bound(VARIABLE x@3)")
        // NS12_B: X and "X" are one name (PLS-00371).
        assertThat(outcome("create procedure t_ns12_b is\n  X number := 1;\n  \"X\" number := 2;\nbegin\n  say(x);\nend;\n/\n", "x"))
            .isEqualTo("Duplicate(VARIABLE X@2, VARIABLE \"X\"@3)")
        // OP4: a quoted type reference names an unquoted subtype.
        assertThat(outcome("create procedure t is\n  subtype t is date;\n  v \"T\";\nbegin\n  null;\nend;\n/\n", "\"T\"", context = TYPE))
            .isEqualTo("Bound(SUBTYPE t@2)")
    }

    @Test
    fun aCapturedFirstSegmentNeverReachesAbsence() {
        // S9B15 (prototype scenario C): a local `pkg` captures `pkg.processar`.
        assertRule("declare\n  pkg number;\n  v date;\nbegin\n  /*A*/ pkg.processar(v);\nend;\n/\n", "pkg", "Bound(VARIABLE pkg@2)")
        val analysis = analyze("declare\n  pkg number;\nbegin\n  pkg.processar(1);\nend;\n/\n")
        assertThat(reference(analysis, "pkg", 0, QUALIFIER).remainingSegments).isEqualTo(1)
        // NS04_E: inside the labeled block the block's variable wins over the label of the same name.
        assertRule(
            "create procedure t_ns04_e is\nbegin\n  <<x>>\n  declare\n    x number := 5;\n  begin\n    /*A*/ say(x.x);\n  end;\nend;\n/\n",
            "x", "Bound(VARIABLE x@5)", context = QUALIFIER
        )
        // NS11_A: the enclosing unit's name qualifies after every inner region.
        assertRule(
            "create procedure t_ns11_a is\n  x number := 1;\nbegin\n  declare\n    x number := 2;\n  begin\n    /*A*/ say(t_ns11_a.x);\n  end;\nend;\n/\n",
            "t_ns11_a", "Bound(PROCEDURE t_ns11_a@1)"
        )
        // LB_A: a top-level block's label qualifies inside its block.
        assertThat(outcome("<<b>>\ndeclare\n  x number := 1;\nbegin\n  say(b.x);\nend;\n/\n", "b")).isEqualTo("Bound(LABEL b@1)")
        // Without any capture the first segment is absent and the caller may look further.
        assertThat(outcome("declare\n  v date;\nbegin\n  pkg.processar(v);\nend;\n/\n", "pkg")).isEqualTo("ProvenAbsent")
    }

    @Test
    fun forwardCursorsAreOneEntityWithTheirDefinition() {
        // FC_A: a forward cursor and its definition in one block.
        assertRule(
            "create procedure t_fc_a is\n  cursor c return dual%rowtype;\n  cursor c return dual%rowtype is select * from dual;\n" +
                "  r c%rowtype;\nbegin\n  /*A*/ null;\nend;\n/\n",
            "c", "Bound(CURSOR c@3)", context = ANCHOR_ROWTYPE
        )
        // FC_B2: the forward declaration does not make the cursor usable before its definition (PLS-00320).
        assertThat(
            outcome(
                "create procedure t_fc_b2 is\n  cursor c return dual%rowtype;\n  r c%rowtype;\n  cursor c return dual%rowtype is select * from dual;\nbegin\n  null;\nend;\n/\n",
                "c", context = ANCHOR_ROWTYPE
            )
        ).isEqualTo("DeclaredLater(CURSOR c@4)")
        // FC_D2: a specification cursor and its body definition.
        assertThat(
            outcome(
                "create package pk_fc2 is\n  cursor c return dual%rowtype;\n  procedure run;\nend;\n/\ncreate package body pk_fc2 is\n" +
                    "  cursor c return dual%rowtype is select * from dual;\n  procedure run is r c%rowtype; begin null; end;\nend;\n/\n",
                "c", context = ANCHOR_ROWTYPE
            )
        ).isEqualTo("Bound(CURSOR c@7)")
        // FC_F: a forward cursor and a variable of the same name (PLS-00305).
        assertThat(
            outcome(
                "create procedure t_fc_f is\n  cursor c return dual%rowtype;\n  c number;\n  cursor c return dual%rowtype is select * from dual;\n" +
                    "  r c%rowtype;\nbegin\n  null;\nend;\n/\n",
                "c", context = ANCHOR_ROWTYPE
            )
        ).isEqualTo("Duplicate(CURSOR c@4, VARIABLE c@3)")
    }

    @Test
    fun kindsThatCannotBeUsedInAContextAreRejected() {
        // KC: a parameter cannot anchor %TYPE (PLS-00206); a constant can (AN_A).
        assertThat(outcome("create procedure t is\n  procedure q(k1 number) is v k1%type; begin null; end;\nbegin\n  null;\nend;\n/\n", "k1", context = ANCHOR_TYPE))
            .isEqualTo("WrongKind(PARAMETER k1@2)")
        assertThat(outcome("create procedure t is\n  k1 constant number := 1;\n  v k1%type;\nbegin\n  null;\nend;\n/\n", "k1", context = ANCHOR_TYPE))
            .isEqualTo("Bound(VARIABLE k1@2)")
        // KC: a variable as an exception (PLS-00485) hides the outer exception.
        assertThat(
            outcome("create procedure t is\n  k1 exception;\nbegin\n  declare\n    k1 number := 1;\n  begin\n    raise k1;\n  end;\nend;\n/\n", "k1", context = EXCEPTION)
        ).isEqualTo("WrongKind(VARIABLE k1@5)")
        // ST_A: a subtype name applied like a call converts its argument.
        assertThat(outcome("create procedure t is\n  subtype k1 is number;\n  x number;\nbegin\n  x := k1(5);\nend;\n/\n", "k1", context = CALL_EXPRESSION))
            .isEqualTo("Bound(SUBTYPE k1@2)")
        // AN_B / LT04_A: a variable anchors %ROWTYPE only when it is a cursor variable, which needs its type.
        assertThat(outcome("create procedure t is\n  k1 number;\n  v k1%rowtype;\nbegin\n  null;\nend;\n/\n", "k1", context = ANCHOR_ROWTYPE))
            .isEqualTo("Unsupported(KIND_IN_CONTEXT VARIABLE ANCHOR_ROWTYPE)")
        // KC: a procedure used as a value (PLS-00222), a function called as a statement (PLS-00221).
        assertThat(outcome("create procedure t is\n  x number;\n  procedure k1 is begin null; end;\nbegin\n  x := k1;\nend;\n/\n", "k1"))
            .isEqualTo("WrongKind(PROCEDURE k1@3)")
        assertThat(outcome("create procedure t is\n  function k1 return number is begin return 1; end;\nbegin\n  k1;\nend;\n/\n", "k1"))
            .isEqualTo("WrongKind(FUNCTION k1@2)")
    }

    @Test
    fun incompleteEvidenceNeverProvesAbsence() {
        // Recovery: a broken declarative part may have held the name (the prototype's recovery hazard).
        assertThat(outcome("declare\n  pkg number;\n  ??? garbage;\nbegin\n  pkg.processar(1);\nend;\n/\n", "pkg", recovery = true))
            .startsWith("Incomplete[PARTIAL_EXTRACTION, PARSE_RECOVERY](")
        // A package body whose specification is not in the file.
        assertThat(outcome("create package body p is\n  procedure a is begin b; end;\nend;\n/\n", "b"))
            .isEqualTo("Incomplete[SPECIFICATION_NOT_IN_FILE](ProvenAbsent)")
        // Conditional compilation in a region of the chain: absence is not proven, the provisional outcome says why not.
        assertThat(outcome("create procedure t is\n  v date;\nbegin\n  \$IF \$\$debug \$THEN null; \$END\n  pkg.processar(v);\nend;\n/\n", "pkg"))
            .isEqualTo("Incomplete[CONDITIONAL_COMPILATION](ProvenAbsent)")
        // An outer region the lookup never reaches does not matter for a binding decided inside.
        assertThat(
            outcome("create procedure t is\nbegin\n  \$IF \$\$debug \$THEN null; \$END\n  null;\n  declare\n    v date;\n  begin\n    say(v);\n  end;\nend;\n/\n", "v")
        ).isEqualTo("Bound(VARIABLE v@6)")
    }

    @Test
    fun unmodeledContextsAreUnsupported() {
        assertThat(outcome("create type ot as object (a number, member function f return number);\n/\n" +
            "create type body ot as member function f return number is begin return a; end; end;\n/\n", "a", context = VALUE))
            .isEqualTo("Unsupported(OBJECT_TYPE_MEMBER)")
        // PD_A–PD_D: parameter defaults follow neither the body's rules nor the enclosing region's.
        assertThat(outcome("create procedure t is\n  k1 number := 10;\n  procedure q(a number := k1) is begin null; end;\nbegin\n  q;\nend;\n/\n", "k1"))
            .isEqualTo("Unsupported(PARAMETER_DEFAULT)")
        assertThat(outcome("declare\n  cursor c is with function g return number is begin return x; end; select g from dual;\nbegin\n  null;\nend;\n/\n", "x"))
            .isEqualTo("Unsupported(SQL_WITH_SUBPROGRAM)")
    }

    @Test
    fun anUnextractedDeclarationIsNotAbsence() {
        // The prototype's `pkg EXCEPTION` shape: an item the model does not extract (here a declaration whose datatype was
        // grafted away) leaves its region PARTIAL, so `pkg` is not proven absent.
        val source = "declare\n  pkg number;\n  v date;\nbegin\n  pkg.processar(v);\nend;\n/\n"
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(source)
        val item = root.getDescendants(com.felipebz.zpa.api.PlSqlGrammar.VARIABLE_DECLARATION).first()
        item.children.remove(item.getFirstChild(com.felipebz.zpa.api.PlSqlGrammar.DATATYPE))
        val model = LocalSemanticModelBuilder.build(root, FileId("r.sql"), source)
        val analysis = Analysis(model, LocalReferenceCollector.collect(root, model))
        val outcome = LocalResolver.resolve(model, reference(analysis, "pkg", 0, QUALIFIER)) as LocalResolution.Incomplete
        assertThat(outcome.regionReasons).contains(LocalIncompleteness.PARTIAL_EXTRACTION)
        assertThat(outcome.provisional).isEqualTo(LocalResolution.ProvenAbsent)
    }

    @Test
    fun withoutSourceTextNothingIsProvenAbsent() {
        val source = "declare\n  v date;\nbegin\n  pkg.processar(v);\nend;\n/\n"
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(source)
        val model = LocalSemanticModelBuilder.build(root, FileId("r.sql"), null)
        val analysis = Analysis(model, LocalReferenceCollector.collect(root, model))
        assertThat(render(model, LocalResolver.resolve(model, reference(analysis, "pkg", 0, QUALIFIER))))
            .isEqualTo("Incomplete[SOURCE_TEXT_UNAVAILABLE](ProvenAbsent)")
        assertThat(render(model, LocalResolver.resolve(model, reference(analysis, "v", 0, VALUE))))
            .isEqualTo("Incomplete[SOURCE_TEXT_UNAVAILABLE](Bound(VARIABLE v@2))")
    }

    @Test
    fun referencesAreCollectedWithTheirContexts() {
        val source = """
            create procedure p(a number := d) is
              cursor cr(k number) is select col from t where b = k;
              v t.c%type;
              w cr%rowtype;
              e exception;
              pragma exception_init(e, -20001);
            begin
              x := f(1) + g + q.r + q.s(2) + c%rowcount;
              proc;
              proc(1);
              pk.proc(1);
              raise e;
              select col into y from t where c1 = z;
              insert into t (col) values (u);
              open cr(1);
              fetch cr into y;
              close cr;
              for r in cr(2) loop null; end loop;
              x := :bind + :new.a;
              goto lbl;
              <<lbl>> null;
            exception
              when e then null;
            end;
            /
        """.trimIndent()
        val analysis = analyze(source)
        assertThat(analysis.references.map { "${it.name.originalSpelling} ${it.context}${if (it.remainingSegments > 0) "+${it.remainingSegments}" else ""}${if (it.inParameterDefault) " default" else ""}" })
            .containsExactly(
                "number TYPE", "d VALUE default", "number TYPE", "col SQL_VALUE", "b SQL_VALUE", "k SQL_VALUE", "t QUALIFIER+1",
                "cr ANCHOR_ROWTYPE", "e EXCEPTION", "x VALUE", "f CALL_EXPRESSION", "g VALUE", "q QUALIFIER+1", "q QUALIFIER+1",
                "c QUALIFIER+1", "proc CALL_STATEMENT", "proc CALL_STATEMENT", "pk QUALIFIER+1", "e EXCEPTION", "col SQL_VALUE",
                "y VALUE", "c1 SQL_VALUE", "z SQL_VALUE", "u SQL_VALUE", "y VALUE", "x VALUE", "e EXCEPTION"
            )
        // Sorted by range, and every reference sits in the innermost region holding it.
        assertThat(analysis.references).isSortedAccordingTo(LocalReference.ORDER)
        val k = reference(analysis, "k", 0, SQL_VALUE)
        assertThat(analysis.model.region(k.region).kind).isEqualTo(LocalRegionKind.CURSOR)
    }

    @Test
    fun referencesAndOutcomesAreDeterministic() {
        val source = "create package p is\n  g number;\n  procedure a;\nend;\n/\ncreate package body p is\n" +
            "  procedure a is\n    v p.g%type;\n  begin\n    for i in 1..g loop say(i + v); end loop;\n    a;\n  end;\nend;\n/\n"
        fun all(order: (List<LocalReference>) -> List<LocalReference>): List<String> {
            val analysis = analyze(source)
            val outcomes = order(analysis.references).associateWith { LocalResolver.resolve(analysis.model, it) }
            return analysis.references.map { "$it -> ${render(analysis.model, outcomes.getValue(it))}" }
        }
        val forward = all { it }
        assertThat(all { it.reversed() }).isEqualTo(forward)
        assertThat(all { it.shuffled(java.util.Random(7)) }).isEqualTo(forward)
        assertThat(forward).anyMatch { it.contains("Bound(LOOP_INDEX i") }.anyMatch { it.contains("Bound(PACKAGE_BODY p") }
    }

    @Test
    fun aTopLevelBlockReliesOnTheFileRegionForItsLabels() {
        // The labels of a top-level block are file declarations outside the block's range: conditional compilation
        // around them touches only the file region. When `$$x` is FALSE Oracle sees `<<v>>`, which captures `v` (KC).
        val source = "\$IF \$\$x \$THEN <<w>> \$ELSE <<v>> \$END <<b>> BEGIN say(v); END;\n/\n"
        assertThat(outcome(source, "v", context = VALUE)).isEqualTo("Incomplete[CONDITIONAL_COMPILATION](ProvenAbsent)")
        assertThat(outcome(source.replace("say(v)", "say(v.x)"), "v", context = QUALIFIER))
            .isEqualTo("Incomplete[CONDITIONAL_COMPILATION](ProvenAbsent)")
    }

    @Test
    fun aCreateUnitDoesNotRelyOnTheFileRegion() {
        // The unit's region covers the whole CREATE, its own name included: hidden text in another unit does not matter.
        val units = "create procedure p1 is begin /*S*/ null; end;\n/\ncreate procedure p2 is begin /*A*/ say(pkg.x); say(p2.x); end;\n/\n"
        assertRule(units, "pkg", "ProvenAbsent")
        assertRule(units, "p2", "Bound(PROCEDURE p2@3)")
        assertRule(
            "create package pk is procedure a; end;\n/\ncreate package body pk is procedure a is begin say(pk.x); end; end;\n/\n" +
                "create procedure other is begin /*S*/ null; end;\n/\n",
            "pk", "Bound(PACKAGE_BODY pk@3)"
        )
    }

    @Test
    fun unsupportedIsNeverWrapped() {
        assertThat(outcome("create procedure t is\n  k1 number;\n  v k1%rowtype;\nbegin\n  \$IF \$\$x \$THEN NULL; \$END\nend;\n/\n", "k1", context = ANCHOR_ROWTYPE))
            .isEqualTo("Unsupported(KIND_IN_CONTEXT VARIABLE ANCHOR_ROWTYPE)")
    }

    companion object {
        private const val A = "/*A*/"
        private const val S = "/*S*/"
        private const val IF = "\$IF \$\$x \$THEN NULL; \$END"
    }
}
