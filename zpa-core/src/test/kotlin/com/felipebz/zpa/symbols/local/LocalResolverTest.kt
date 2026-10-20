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
 * The lexical resolver on source shapes observed on Oracle. Each rule is checked as written, with conditional compilation
 * in a region the outcome relies on, and in a region it does not rely on.
 */
class LocalResolverTest {

    internal class Analysis(val model: LocalSemanticModel, val references: List<LocalReference>)

    internal fun analyze(source: String, recovery: Boolean = false): Analysis {
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, recovery)).parse(source)
        val model = LocalSemanticModelBuilder.build(root, FileId("r.sql"), source)
        return Analysis(model, LocalReferenceCollector.collect(root, model))
    }

    internal fun reference(analysis: Analysis, name: String, occurrence: Int, context: LocalReferenceContext?): LocalReference {
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

    internal fun render(model: LocalSemanticModel, outcome: LocalResolution): String {
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
            LocalResolution.Undeclared -> "Undeclared"
            is LocalResolution.OutOfScope -> "OutOfScope(${d(outcome.declaration)})"
            is LocalResolution.Field -> "Field(${d(outcome.recordType)}.${(model.declaration(outcome.recordType).details as LocalRecordType).fields[outcome.ordinal].name.originalSpelling})"
            is LocalResolution.CollectionMethod -> "CollectionMethod(${d(outcome.collectionType)})"
            is LocalResolution.ImplicitSelf -> "ImplicitSelf(${d(outcome.method)})"
            is LocalResolution.InstanceMemberInStatic -> "InstanceMemberInStatic(${d(outcome.declaration)})"
            LocalResolution.SqlRelation -> "SqlRelation"
            is LocalResolution.LocalFunctionInSql -> "LocalFunctionInSql(${outcome.declarations.joinToString(", ", transform = ::d)})"
            is LocalResolution.SqlBuiltinOrLocal -> "SqlBuiltinOrLocal(${outcome.declarations.joinToString(", ", transform = ::d)})"
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
        // `a number := b; b number := 1;` with an outer `b` is PLS-00320, never the outer b.
        assertRule(
            "create procedure later_inner_declaration is\n  b number := 100;\nbegin\n  /*S*/ null;\n  declare\n    a number := b;\n    b number := 1;\n" +
                "  begin\n    /*A*/ null;\n  end;\nend;\n/\n",
            "b", "DeclaredLater(VARIABLE b@7)"
        )
        // A type used before the inner subtype of the same name.
        assertRule(
            "create procedure later_inner_type is\n  subtype t is varchar2(10);\nbegin\n  declare\n    v t;\n    subtype t is number;\n" +
                "  begin\n    /*A*/ null;\n  end;\nend;\n/\n",
            "t", "DeclaredLater(SUBTYPE t@6)", context = TYPE
        )
        // A call to a later sibling hides the schema procedure of the same name (PLS-00313).
        assertRule(
            "create procedure later_sibling is\n  procedure a is begin later_proc; end;\n  procedure later_proc is begin null; end;\n" +
                "begin\n  /*A*/ a;\nend;\n/\n",
            "later_proc", "DeclaredLater(PROCEDURE later_proc@3)"
        )
        // Without the inner b the outer one binds, and the inner block's state does not matter for it.
        assertThat(outcome("create procedure t is\n  b number := 100;\nbegin\n  declare\n    a number := b;\n  begin null; end;\nend;\n/\n", "b"))
            .isEqualTo("Bound(VARIABLE b@2)")
    }

    @Test
    fun specificationItemsAreVisibleThroughoutTheBody() {
        val spec = "create package pk_ad is\n  procedure a;\n  procedure b;\nend;\n/\n"
        // `b` is defined after `a` in the body but declared in the specification.
        assertRule(
            spec + "create package body pk_ad is\n  procedure a is begin /*A*/ b; end;\n  procedure b is begin null; end;\nend;\n/\n" +
                "create procedure other is begin /*S*/ null; end;\n/\n",
            "b", "OverloadSet(PROCEDURE b@8)"
        )
        // A private procedure defined later is an order error (PLS-00313).
        assertRule(
            spec + "create package body pk_ad is\n  procedure a is begin /*A*/ c; end;\n  procedure b is begin null; end;\n" +
                "  procedure c is begin null; end;\nend;\n/\n",
            "c", "DeclaredLater(PROCEDURE c@9)"
        )
        // A specification variable and a body variable collide like one region (PLS-00371).
        assertRule(
            "create package pk_dup is\n  x number := 1;\n  procedure show;\nend;\n/\n" +
                "create package body pk_dup is\n  x number := 2;\n  procedure show is begin /*A*/ say(x); end;\nend;\n/\n",
            "x", "Duplicate(VARIABLE x@2, VARIABLE x@7)"
        )
        // Inside the specification the ordinary order rule applies (PLS-00320).
        assertThat(outcome("create package pk_ord is\n  c1 constant number := c2;\n  c2 constant number := 1;\nend;\n/\n", "c2"))
            .isEqualTo("DeclaredLater(VARIABLE c2@3)")
    }

    @Test
    fun recursionAndForwardDeclarations() {
        // Direct recursion, and mutual recursion through a forward declaration.
        val recursion = "create procedure recursion is\n" +
            "  function fact(n number) return number is begin /*A*/ return n * fact(n - 1); end;\n" +
            "  function is_odd(n number) return boolean;\n" +
            "  function is_even(n number) return boolean is begin return is_odd(n - 1); end;\n" +
            "  function is_odd(n number) return boolean is begin return is_even(n - 1); end;\n" +
            "begin\n  null;\nend;\n/\n"
        assertRule(recursion, "fact", "OverloadSet(FUNCTION fact@2)")
        assertThat(outcome(recursion.replace(A, ""), "is_odd")).isEqualTo("OverloadSet(FUNCTION is_odd@5)")
        // Without the forward declaration the later sibling is an order error (PLS-00313).
        assertThat(
            outcome(
                "create procedure recursion_without_forward is\n  function is_even(n number) return boolean is begin return is_odd2(n - 1); end;\n" +
                    "  function is_odd2(n number) return boolean is begin return is_even(n - 1); end;\nbegin\n  null;\nend;\n/\n",
                "is_odd2"
            )
        ).isEqualTo("DeclaredLater(FUNCTION is_odd2@3)")
        // A forward declaration and two bodies of one heading (PLS-00305).
        assertThat(
            outcome(
                "create procedure two_bodies is\n  procedure f(n number);\n  procedure f(n number) is begin null; end;\n" +
                    "  procedure f(n number) is begin null; end;\nbegin\n  f(1);\nend;\n/\n",
                "f"
            )
        ).isEqualTo("Duplicate(PROCEDURE f@2, PROCEDURE f@3, PROCEDURE f@4)")
    }

    @Test
    fun duplicatesAndMixedCallableKinds() {
        // A parameter and a local of the same name share one region (PLS-00410).
        assertRule(
            "create procedure param_and_local is\n  procedure inner(p number) is\n    p varchar2(10) := 'local';\n" +
                "  begin\n    /*A*/ say(p);\n  end;\nbegin\n  /*S*/ inner(1);\nend;\n/\n",
            "p", "Duplicate(PARAMETER p@2, VARIABLE p@3)"
        )
        // Two variables, referenced (PLS-00371).
        assertRule("create procedure two_variables is\n  x number;\n  x varchar2(10);\nbegin\n  /*A*/ x := 1;\nend;\n/\n", "x", "Duplicate(VARIABLE x@2, VARIABLE x@3)")
        // A variable and a procedure (PLS-00305 at the declaration, PLS-00221 at the call).
        assertRule(
            "create procedure variable_and_procedure is\n  x number;\n  procedure x(a number) is begin null; end;\nbegin\n  /*A*/ x(1);\nend;\n/\n",
            "x", "Duplicate(VARIABLE x@2, PROCEDURE x@3)", context = CALL_STATEMENT
        )
        // An exception and a variable share the namespace (PLS-00371).
        assertRule("create procedure exception_and_variable is\n  e exception;\n  e number;\nbegin\n  /*A*/ e := 1;\nend;\n/\n", "e", "Duplicate(EXCEPTION e@2, VARIABLE e@3)")
        // A label in the body and a variable of the region (PLS-00371).
        assertRule(
            "create procedure label_and_variable is\n  x number := 1;\nbegin\n  <<x>>\n  begin\n    null;\n  end;\n  /*A*/ say(x);\nend;\n/\n",
            "x", "Duplicate(VARIABLE x@2, LABEL x@4)"
        )
        // An inner exception hides an outer variable in RAISE and WHEN.
        val innerException = "create procedure inner_exception is\n  e number := 1;\nbegin\n  declare\n    e exception;\n  begin\n    /*A*/ raise e;\n" +
            "  exception when e then null;\n  end;\n  say(e);\nend;\n/\n"
        assertRule(innerException, "e", "Bound(EXCEPTION e@5)", context = EXCEPTION)
        assertThat(outcome(innerException.replace(A, ""), "e", 1, EXCEPTION)).isEqualTo("Bound(EXCEPTION e@5)")
        assertThat(outcome(innerException.replace(A, ""), "e", 0, VALUE)).isEqualTo("Bound(VARIABLE e@2)")
    }

    @Test
    fun aLabelDecidesItsRegion() {
        // `outer.x` inside the inner block reaches the label of the enclosing region.
        assertRule(
            "create procedure outer_label is\nbegin\n  <<Outer>>\n  declare\n    x number := 1;\n  begin\n    declare\n      x number := 2;\n" +
                "    begin\n      /*A*/ say(OUTER.x);\n    end;\n  end;\nend;\n/\n",
            "outer", "Bound(LABEL Outer@3)"
        )
        // The label captures `pkg_a` over the whole enclosing region, before and after the block.
        val labelLikePackage = "create procedure label_like_package is\nbegin\n  /*A*/ say(pkg_b.x);\n  <<pkg_b>>\n  declare\n    x varchar2(10);\n" +
            "  begin\n    null;\n  end;\n  say(pkg_b.x);\nend;\n/\n"
        assertRule(labelLikePackage, "pkg_b", "Bound(LABEL pkg_b@4)")
        assertThat(outcome(labelLikePackage.replace(A, ""), "pkg_b", 1)).isEqualTo("Bound(LABEL pkg_b@4)")
        // A record variable and a label of the same region (PLS-00371).
        assertRule(
            "create procedure record_and_label is\n  type r_t is record(x varchar2(10));\n  b r_t;\nbegin\n  /*A*/ b.x := 'rec';\n  <<b>>\n" +
                "  declare\n    x varchar2(10);\n  begin\n    null;\n  end;\nend;\n/\n",
            "b", "Duplicate(VARIABLE b@3, LABEL b@6)"
        )
        // A label used as a value is captured and rejected.
        assertThat(outcome("create procedure t is\n  x number;\nbegin\n  <<k1>>\n  begin\n    x := k1;\n  end;\nend;\n/\n", "k1"))
            .isEqualTo("WrongKind(LABEL k1@4)")
        // Two sibling blocks with the same label (PLS-00371).
        assertThat(
            outcome(
                "create procedure sibling_labels is\nbegin\n  <<lab>>\n  begin null; end;\n  <<lab>>\n  declare x number; begin say(lab.x); end;\nend;\n/\n",
                "lab"
            )
        ).isEqualTo("Duplicate(LABEL lab@3, LABEL lab@5)")
    }

    @Test
    fun theInnermostOverloadSetHidesOuterOnes() {
        // An inner f(date) hides the outer f(number): only the inner one is a candidate (PLS-00306).
        assertRule(
            "create procedure nested_overload is\n  procedure f(p number) is begin null; end;\nbegin\n  /*S*/ null;\n" +
                "  declare\n    procedure f(p date) is begin null; end;\n  begin\n    /*A*/ f(1);\n  end;\nend;\n/\n",
            "f", "OverloadSet(PROCEDURE f@6)"
        )
        // Overloads of one region form one set.
        assertThat(
            outcome("create procedure t is\n  procedure fo(p varchar2) is begin null; end;\n  procedure fo(p date) is begin null; end;\nbegin\n  fo(1);\nend;\n/\n", "fo")
        ).isEqualTo("OverloadSet(PROCEDURE fo@2, PROCEDURE fo@3)")
        // A variable named like a procedure is not callable (PLS-00221).
        assertRule("create procedure variable_not_callable is\n  sp number := 1;\nbegin\n  /*A*/ sp;\nend;\n/\n", "sp", "WrongKind(VARIABLE sp@2)")
        assertRule(
            "create procedure inner_variable_not_callable is\n  procedure f(p number) is begin null; end;\nbegin\n  declare\n    f number := 1;\n" +
                "  begin\n    /*A*/ f(1);\n  end;\nend;\n/\n",
            "f", "WrongKind(VARIABLE f@5)"
        )
    }

    @Test
    fun predefinedTypeNamesAreOrdinaryNames() {
        // A local subtype named like a predefined type binds the type position.
        assertRule("create procedure subtype_named_number is\n  subtype number is date;\n  v number;\nbegin\n  /*A*/ null;\nend;\n/\n", "number", "Bound(SUBTYPE number@2)", context = TYPE)
        // Constrained syntax names the same local subtype.
        assertThat(outcome("create procedure t is\n  subtype varchar2 is number;\n  v varchar2(10);\nbegin\n  null;\nend;\n/\n", "varchar2", context = TYPE))
            .isEqualTo("Bound(SUBTYPE varchar2@2)")
        // A variable named `integer` hides the type and is not one (PLS-00488).
        assertRule("create procedure variable_named_integer is\n  integer number := 5;\n  v integer;\nbegin\n  /*A*/ null;\nend;\n/\n", "integer", "WrongKind(VARIABLE integer@2)", context = TYPE)
        // Without a local declaration a predefined name is only absent locally: STANDARD is the caller's next step.
        assertThat(outcome("create procedure t is\n  v number;\nbegin\n  null;\nend;\n/\n", "number", context = TYPE)).isEqualTo("ProvenAbsent")
    }

    @Test
    fun parameterTypesAndReturnTypesUseTheirOwnRegions() {
        // A parameter type is looked up in the enclosing region, ignoring the subprogram's local `t`.
        assertRule(
            "create procedure param_type_scope is\n  subtype t is number;\n  procedure p(a t) is subtype t is date; begin null; end;\nbegin\n  /*A*/ p(1);\nend;\n/\n",
            "t", "Bound(SUBTYPE t@2)", context = TYPE
        )
        // The RETURN type is looked up in the function's own region, where `t` is declared later (PLS-00311).
        assertRule(
            "create procedure return_type_order is\n  subtype t is number;\n  function f return t is subtype t is date; begin /*A*/ return sysdate; end;\n" +
                "begin\n  null;\nend;\n/\n",
            "t", "DeclaredLater(SUBTYPE t@3)", occurrence = 0, context = TYPE
        )
        // Without a local `t` the RETURN type reaches the enclosing one.
        assertThat(
            outcome("create procedure return_type_outer is\n  subtype t is number;\n  function f return t is subtype t2 is date; begin return 1; end;\nbegin\n  null;\nend;\n/\n", "t", context = TYPE)
        ).isEqualTo("Bound(SUBTYPE t@2)")
    }

    @Test
    fun aForIndexIsVisibleInItsOwnBounds() {
        // `for i in 1..i` compiles with no other `i`.
        assertRule("create procedure index_in_bounds is\nbegin\n  for i in 1..i loop\n    /*A*/ null;\n  end loop;\nend;\n/\n", "i", "Bound(LOOP_INDEX i@3)")
        // The index is not visible after the loop.
        assertThat(outcome("create procedure index_after_loop is\nbegin\n  for j in 1..2 loop null; end loop;\n  say(j);\nend;\n/\n", "j"))
            .isEqualTo("ProvenAbsent")
    }

    @Test
    fun aSqlNameIsAColumnOrALocal() {
        // In SQL a column beats a local of the same name; INTO targets are PL/SQL.
        val sqlColumn = "create procedure sql_column is\n  x number := 2;\n  n number;\nbegin\n  /*A*/ select count(*) into n from emp where x = x and c1 = 1;\nend;\n/\n"
        assertRule(sqlColumn, "x", "SqlColumnOrLocal(VARIABLE x@2)", context = SQL_VALUE)
        assertRule(sqlColumn, "n", "Bound(VARIABLE n@3)", context = VALUE)
        // A name no region declares may still be a column: never a proven absence.
        assertThat(outcome(sqlColumn.replace(A, ""), "c1")).isEqualTo("Incomplete[SQL_COLUMN_UNKNOWN](ProvenAbsent)")
        // A cursor parameter in its query.
        assertThat(outcome("create procedure t is\n  cursor c(a number) is select 1 from t where b = a;\nbegin\n  null;\nend;\n/\n", "a", context = SQL_VALUE))
            .isEqualTo("SqlColumnOrLocal(CURSOR_PARAMETER a@2)")
    }

    @Test
    fun timingPointsSeeTheirSectionAndTriggerNamesAreNotQualifiers() {
        // A timing point sees the compound trigger's section declarations.
        assertRule(
            "create trigger trg_c for update on orders compound trigger\n  g_cnt pls_integer := 0;\n  before each row is\n" +
                "    l pls_integer := 1;\n  begin\n    /*A*/ g_cnt := g_cnt + l;\n  end before each row;\nend trg_c;\n/\n" +
                "create procedure other is begin /*S*/ null; end;\n/\n",
            "g_cnt", "Bound(VARIABLE g_cnt@2)"
        )
        // A trigger's name does not qualify inside it (PLS-00201).
        assertThat(outcome("create trigger trg_q before insert on orders for each row\ndeclare\n  a number := 7;\nbegin\n  say(trg_q.a);\nend;\n/\n", "trg_q"))
            .isEqualTo("ProvenAbsent")
    }

    @Test
    fun identifiersCompareAsOracleIdentifiers() {
        // "x" and x are distinct; "X" is x.
        val quotedNames = "create procedure quoted_names is\n  \"x\" number := 1;\n  x number := 2;\nbegin\n  /*A*/ say(\"x\" + x + \"X\");\nend;\n/\n"
        assertRule(quotedNames, "\"x\"", "Bound(VARIABLE \"x\"@2)")
        assertThat(outcome(quotedNames.replace(A, ""), "x")).isEqualTo("Bound(VARIABLE x@3)")
        assertThat(outcome(quotedNames.replace(A, ""), "\"X\"")).isEqualTo("Bound(VARIABLE x@3)")
        // X and "X" are one name (PLS-00371).
        assertThat(outcome("create procedure quoted_duplicate is\n  X number := 1;\n  \"X\" number := 2;\nbegin\n  say(x);\nend;\n/\n", "x"))
            .isEqualTo("Duplicate(VARIABLE X@2, VARIABLE \"X\"@3)")
        // A quoted type reference names an unquoted subtype.
        assertThat(outcome("create procedure t is\n  subtype t is date;\n  v \"T\";\nbegin\n  null;\nend;\n/\n", "\"T\"", context = TYPE))
            .isEqualTo("Bound(SUBTYPE t@2)")
    }

    @Test
    fun aCapturedFirstSegmentNeverReachesAbsence() {
        // A local `pkg` captures `pkg.processar`.
        assertRule("declare\n  pkg number;\n  v date;\nbegin\n  /*A*/ pkg.processar(v);\nend;\n/\n", "pkg", "Bound(VARIABLE pkg@2)")
        val analysis = analyze("declare\n  pkg number;\nbegin\n  pkg.processar(1);\nend;\n/\n")
        assertThat(reference(analysis, "pkg", 0, QUALIFIER).remainingSegments).isEqualTo(1)
        // Inside the labeled block the block's variable wins over the label of the same name.
        assertRule(
            "create procedure variable_beats_label is\nbegin\n  <<x>>\n  declare\n    x number := 5;\n  begin\n    /*A*/ say(x.x);\n  end;\nend;\n/\n",
            "x", "Bound(VARIABLE x@5)", context = QUALIFIER
        )
        // The enclosing unit's name qualifies after every inner region.
        assertRule(
            "create procedure unit_name_qualifier is\n  x number := 1;\nbegin\n  declare\n    x number := 2;\n  begin\n    /*A*/ say(unit_name_qualifier.x);\n  end;\nend;\n/\n",
            "unit_name_qualifier", "Bound(PROCEDURE unit_name_qualifier@1)"
        )
        // A top-level block's label qualifies inside its block.
        assertThat(outcome("<<b>>\ndeclare\n  x number := 1;\nbegin\n  say(b.x);\nend;\n/\n", "b")).isEqualTo("Bound(LABEL b@1)")
        // Without any capture the first segment is absent and the caller may look further.
        assertThat(outcome("declare\n  v date;\nbegin\n  pkg.processar(v);\nend;\n/\n", "pkg")).isEqualTo("ProvenAbsent")
    }

    @Test
    fun forwardCursorsAreOneEntityWithTheirDefinition() {
        // A forward cursor and its definition in one block.
        assertRule(
            "create procedure forward_cursor is\n  cursor c return dual%rowtype;\n  cursor c return dual%rowtype is select * from dual;\n" +
                "  r c%rowtype;\nbegin\n  /*A*/ null;\nend;\n/\n",
            "c", "Bound(CURSOR c@3)", context = ANCHOR_ROWTYPE
        )
        // The forward declaration does not make the cursor usable before its definition (PLS-00320).
        assertThat(
            outcome(
                "create procedure cursor_before_definition is\n  cursor c return dual%rowtype;\n  r c%rowtype;\n  cursor c return dual%rowtype is select * from dual;\nbegin\n  null;\nend;\n/\n",
                "c", context = ANCHOR_ROWTYPE
            )
        ).isEqualTo("DeclaredLater(CURSOR c@4)")
        // A specification cursor and its body definition.
        assertThat(
            outcome(
                "create package pk_cur is\n  cursor c return dual%rowtype;\n  procedure run;\nend;\n/\ncreate package body pk_cur is\n" +
                    "  cursor c return dual%rowtype is select * from dual;\n  procedure run is r c%rowtype; begin null; end;\nend;\n/\n",
                "c", context = ANCHOR_ROWTYPE
            )
        ).isEqualTo("Bound(CURSOR c@7)")
        // A forward cursor and a variable of the same name (PLS-00305).
        assertThat(
            outcome(
                "create procedure cursor_and_variable is\n  cursor c return dual%rowtype;\n  c number;\n  cursor c return dual%rowtype is select * from dual;\n" +
                    "  r c%rowtype;\nbegin\n  null;\nend;\n/\n",
                "c", context = ANCHOR_ROWTYPE
            )
        ).isEqualTo("Duplicate(CURSOR c@4, VARIABLE c@3)")
    }

    @Test
    fun kindsThatCannotBeUsedInAContextAreRejected() {
        // A parameter cannot anchor %TYPE (PLS-00206); a constant can.
        assertThat(outcome("create procedure t is\n  procedure q(k1 number) is v k1%type; begin null; end;\nbegin\n  null;\nend;\n/\n", "k1", context = ANCHOR_TYPE))
            .isEqualTo("WrongKind(PARAMETER k1@2)")
        assertThat(outcome("create procedure t is\n  k1 constant number := 1;\n  v k1%type;\nbegin\n  null;\nend;\n/\n", "k1", context = ANCHOR_TYPE))
            .isEqualTo("Bound(VARIABLE k1@2)")
        // A variable as an exception (PLS-00485) hides the outer exception.
        assertThat(
            outcome("create procedure t is\n  k1 exception;\nbegin\n  declare\n    k1 number := 1;\n  begin\n    raise k1;\n  end;\nend;\n/\n", "k1", context = EXCEPTION)
        ).isEqualTo("WrongKind(VARIABLE k1@5)")
        // A subtype name applied like a call converts its argument.
        assertThat(outcome("create procedure t is\n  subtype k1 is number;\n  x number;\nbegin\n  x := k1(5);\nend;\n/\n", "k1", context = CALL_EXPRESSION))
            .isEqualTo("Bound(SUBTYPE k1@2)")
        // A variable anchors %ROWTYPE only when it is a cursor variable, which needs its type.
        assertThat(outcome("create procedure t is\n  k1 number;\n  v k1%rowtype;\nbegin\n  null;\nend;\n/\n", "k1", context = ANCHOR_ROWTYPE))
            .isEqualTo("Unsupported(KIND_IN_CONTEXT VARIABLE ANCHOR_ROWTYPE)")
        // A procedure used as a value (PLS-00222), a function called as a statement (PLS-00221).
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
        // In an object type specification only single-segment type names are observed.
        assertThat(outcome("create type ot as object (a pkg.t, member function f return number);\n/\n", "pkg"))
            .isEqualTo("Unsupported(OBJECT_TYPE_SPECIFICATION)")
        // Parameter defaults follow neither the body's rules nor the enclosing region's.
        assertThat(outcome("create procedure t is\n  k1 number := 10;\n  procedure q(a number := k1) is begin null; end;\nbegin\n  q;\nend;\n/\n", "k1"))
            .isEqualTo("Unsupported(PARAMETER_DEFAULT)")
        assertThat(outcome("declare\n  cursor c is with function g return number is begin return x; end; select g from dual;\nbegin\n  null;\nend;\n/\n", "x"))
            .isEqualTo("Unsupported(SQL_WITH_SUBPROGRAM)")
    }

    @Test
    fun anUnextractedDeclarationIsNotAbsence() {
        // The prototype's `pkg EXCEPTION` shape: an item the model does not extract (here a declaration whose datatype was
        // Grafted away) leaves its region PARTIAL, so `pkg` is not proven absent.
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
              select e.col, f(2) into y from t e;
              insert into t (col) values (u);
              open cr(1);
              fetch cr into y;
              close cr;
              for r in cr(2) loop null; end loop;
              x := :bind + :new.a;
              goto lbl;
              <<lbl>> loop exit lbl; continue lbl; end loop lbl;
              open rc for select 1 from dual;
            exception
              when e then null;
            end;
            /
        """.trimIndent()
        val analysis = analyze(source)
        assertThat(analysis.references.map {
            "${it.name.originalSpelling} ${it.context}${it.segments.joinToString("")}" +
                (if (it.segments.isNotEmpty()) " as ${it.lastSegmentContext}" else "") + (if (it.inParameterDefault) " default" else "")
        }).containsExactly(
            "number TYPE", "d VALUE default", "number TYPE", "col SQL_VALUE", "b SQL_VALUE", "k SQL_VALUE", "t QUALIFIER.c as ANCHOR_TYPE",
            "cr ANCHOR_ROWTYPE", "e EXCEPTION", "x VALUE", "f CALL_EXPRESSION", "g VALUE", "q QUALIFIER.r as VALUE",
            "q QUALIFIER.s as CALL_EXPRESSION", "c QUALIFIER%rowcount as VALUE", "proc CALL_STATEMENT", "proc CALL_STATEMENT",
            "pk QUALIFIER.proc as CALL_STATEMENT", "e EXCEPTION", "col SQL_VALUE", "y VALUE", "c1 SQL_VALUE", "z SQL_VALUE",
            "e QUALIFIER.col as SQL_VALUE", "f SQL_CALL", "y VALUE", "u INSERT_VALUE", "cr CURSOR_OPERAND", "cr CURSOR_OPERAND", "y VALUE", "cr CURSOR_OPERAND", "cr FOR_CONTROL",
            "x VALUE", "lbl GOTO_TARGET", "lbl LOOP_TARGET", "lbl LOOP_TARGET", "rc OPEN_FOR_OPERAND", "e EXCEPTION"
        )
        // `END lbl` is matched, not looked up: it is not a reference.
        assertThat(analysis.references.count { it.name.lookupName == "LBL" }).isEqualTo(3)
        // Sorted by range, and every reference sits in the innermost region holding it.
        assertThat(analysis.references).isSortedAccordingTo(LocalReference.ORDER)
        val k = reference(analysis, "k", 0, SQL_VALUE)
        assertThat(analysis.model.region(k.region).kind).isEqualTo(LocalRegionKind.CURSOR)
        // A SQL name carries its statement's relations; PL/SQL names carry none.
        assertThat(reference(analysis, "e", 0, QUALIFIER).sql?.relations?.map { it.lookupName }).containsExactly("E")
        assertThat(reference(analysis, "u", 0, INSERT_VALUE).sql).isNull()
        assertThat(reference(analysis, "y", 0, VALUE).sql).isNull()
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
        // Around them touches only the file region. When `$$x` is FALSE Oracle sees `<<v>>`, which captures `v`.
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
