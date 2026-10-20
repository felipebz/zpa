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

import com.felipebz.zpa.symbols.local.LocalReferenceContext.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Qualified names, label targets, cursor operands and `FOR` controls on source shapes observed on Oracle. Each rule is
 * checked as written, with conditional compilation in a region a step relies on, and in a region no step relies on.
 */
class LocalQualifiedResolverTest {

    private val support = LocalResolverTest()

    internal fun steps(source: String, name: String, occurrence: Int = 0, context: LocalReferenceContext? = null): String {
        val analysis = support.analyze(source)
        val reference = support.reference(analysis, name, occurrence, context)
        return LocalResolver.resolveQualified(analysis.model, reference).joinToString(" / ") { support.render(analysis.model, it) }
    }

    /** `/*A*/` marks a region a step relies on, `/*S*/` one it does not; [fromStep] is the first step touched. */
    internal fun assertSteps(
        template: String, name: String, expected: List<String>, occurrence: Int = 0, context: LocalReferenceContext? = null, fromStep: Int = 0
    ) {
        val plain = template.replace(A, "").replace(S, "")
        assertThat(steps(plain, name, occurrence, context)).describedAs(plain).isEqualTo(expected.joinToString(" / "))
        if (A in template) {
            val touched = template.replace(A, IF).replace(S, "")
            val wrapped = expected.mapIndexed { index, step ->
                if (index < fromStep || step.startsWith("Unsupported")) step else "Incomplete[CONDITIONAL_COMPILATION]($step)"
            }
            assertThat(steps(touched, name, occurrence, context)).describedAs(touched).isEqualTo(wrapped.joinToString(" / "))
        }
        if (S in template) {
            val elsewhere = template.replace(S, IF).replace(A, "")
            assertThat(steps(elsewhere, name, occurrence, context)).describedAs(elsewhere).isEqualTo(expected.joinToString(" / "))
        }
    }

    private val other = "create procedure other is begin /*S*/ null; end;\n/\n"

    @Test
    fun aLabelQualifiesOnlyItsRegion() {
        // `outer.x` reaches the labeled block's x past the inner shadow.
        assertSteps(
            "create procedure outer_label is\nbegin\n  <<Outer>>\n  declare\n    x number := 1;\n  begin\n    /*A*/ null;\n    declare\n      x number := 2;\n" +
                "    begin\n      say(OUTER.x);\n    end;\n  end;\nend;\n/\n" + other,
            "outer", listOf("Bound(LABEL Outer@3)", "Bound(VARIABLE x@5)")
        )
        // Nested same-named labels: the innermost wins.
        assertSteps(
            "create procedure nested_labels is\nbegin\n  <<a>>\n  declare\n    x varchar2(10);\n  begin\n    <<a>>\n    declare\n      x varchar2(10);\n" +
                "    begin\n      /*A*/ say(a.x);\n    end;\n  end;\nend;\n/\n" + other,
            "a", listOf("Bound(LABEL a@7)", "Bound(VARIABLE x@9)")
        )
        // A sibling block's label (PLS-00219).
        assertSteps(
            "create procedure sibling_label is\nbegin\n  <<b1>>\n  declare\n    x number := 1;\n  begin\n    null;\n  end;\n  <<b2>>\n" +
                "  begin\n    /*A*/ say(b1.x);\n  end;\nend;\n/\n" + other,
            "b1", listOf("Bound(LABEL b1@3)", "OutOfScope(LABEL b1@3)")
        )
        // The labeled region's order rule.
        assertThat(steps("create procedure t is\nbegin\n  <<b>> declare y number := b.x; x number := 7; begin null; end;\nend;\n/\n", "b"))
            .isEqualTo("Bound(LABEL b@3) / DeclaredLater(VARIABLE x@3)")
    }

    @Test
    fun aLabelNamedLikeAPackageNeverFallsBack() {
        val block = "create procedure label_like_package is\nbegin\n  <<pkg_a>>\n  declare\n    x varchar2(10);\n  begin\n    /*A*/ say(pkg_a.MEMBER);\n  end;\n" +
            "  say(pkg_a.x);\nend;\n/\n" + other
        // The label's variable.
        assertSteps(block.replace("MEMBER", "x"), "pkg_a", listOf("Bound(LABEL pkg_a@3)", "Bound(VARIABLE x@5)"))
        // A member the block lacks (PLS-00302), never the package's.
        assertSteps(block.replace("MEMBER", "y"), "pkg_a", listOf("Bound(LABEL pkg_a@3)", "Undeclared"))
        // After the labeled block (PLS-00219).
        assertThat(steps(block.replace(A, "").replace(S, "").replace("MEMBER", "x"), "pkg_a", 1))
            .isEqualTo("Bound(LABEL pkg_a@3) / OutOfScope(LABEL pkg_a@3)")
        // A label as a type qualifier.
        val labelTypeQualifier = "create procedure label_type_qualifier is\nbegin\n  <<type_block>>\n  declare subtype t is date;\n  begin\n    /*A*/ declare v type_block.t; begin null; end;\n  end;\nend;\n/\n"
        assertSteps(labelTypeQualifier, "type_block", listOf("Bound(LABEL type_block@3)", "Bound(SUBTYPE t@4)"))
        assertThat(steps(labelTypeQualifier.replace(A, "").replace("subtype t is date", "x number"), "type_block"))
            .isEqualTo("Bound(LABEL type_block@3) / Undeclared")
    }

    @Test
    fun aSubprogramNameQualifiesItsOwnRegion() {
        // Past the inner shadow.
        assertSteps(
            "create procedure unit_past_shadow is\n  x number := 1;\nbegin\n  /*A*/ null;\n  declare\n    x number := 2;\n  begin\n    say(unit_past_shadow.x);\n  end;\nend;\n/\n" + other,
            "unit_past_shadow", listOf("Bound(PROCEDURE unit_past_shadow@1)", "Bound(VARIABLE x@2)")
        )
        // Nested subprograms qualify their parameters and locals.
        val nestedSubprograms = "create procedure nested_subprograms is\n  procedure outerp(v varchar2) is\n    w varchar2(10);\n    procedure innerp(v varchar2) is\n" +
            "      w varchar2(10);\n    begin\n      say(outerp.v || innerp.v || outerp.w);\n    end;\n  begin\n    innerp('x');\n  end;\nbegin\n  outerp('y');\nend;\n/\n"
        assertThat(steps(nestedSubprograms, "outerp", 0, QUALIFIER)).isEqualTo("Bound(PROCEDURE outerp@2) / Bound(PARAMETER v@2)")
        assertThat(steps(nestedSubprograms, "innerp", 0, QUALIFIER)).isEqualTo("Bound(PROCEDURE innerp@4) / Bound(PARAMETER v@4)")
        assertThat(steps(nestedSubprograms, "outerp", 1, QUALIFIER)).isEqualTo("Bound(PROCEDURE outerp@2) / Bound(VARIABLE w@3)")
        // In its own declaration initializer.
        assertThat(steps("create procedure t is\n  procedure p(x number) is\n    x2 number := p.x;\n  begin\n    null;\n  end;\nbegin\n  p(5);\nend;\n/\n", "p"))
            .isEqualTo("Bound(PROCEDURE p@2) / Bound(PARAMETER x@2)")
        // A parameter past an inner block's shadow.
        assertThat(steps("create procedure t is\n  procedure p(a number) is begin declare a number := 0; begin say(p.a); end; end;\nbegin\n  p(4);\nend;\n/\n", "p", 0, QUALIFIER))
            .isEqualTo("Bound(PROCEDURE p@2) / Bound(PARAMETER a@2)")
    }

    @Test
    fun aSubprogramNameDoesNotReachUnnamedInnerBlocks() {
        // `t` only in an inner unnamed block (PLS-00302).
        assertSteps(
            "create procedure unnamed_inner_block is\nbegin\n  /*A*/ null;\n  declare subtype t is date; v unnamed_inner_block.t; begin null; end;\nend;\n/\n" + other,
            "unnamed_inner_block", listOf("Bound(PROCEDURE unnamed_inner_block@1)", "Undeclared")
        )
        // The procedure's own t past the inner one.
        assertSteps(
            "create procedure own_type_past_shadow is\n  subtype t is date;\nbegin\n  /*A*/ null;\n  declare subtype t is number; v own_type_past_shadow.t; begin null; end;\nend;\n/\n" + other,
            "own_type_past_shadow", listOf("Bound(PROCEDURE own_type_past_shadow@1)", "Bound(SUBTYPE t@2)")
        )
        // A subprogram name outside its body (PLS-00225).
        assertThat(steps("create procedure t is\n  x number;\n  procedure k1(p number) is begin null; end;\nbegin\n  x := k1.p;\nend;\n/\n", "k1"))
            .isEqualTo("OverloadSet(PROCEDURE k1@3) / OutOfScope(PROCEDURE k1@3)")
    }

    @Test
    fun aPackageBodysOwnNameSeesPrivateItems() {
        // A body-private type and a specification type through the package's own name.
        val privateItems = "create package private_items as\n  type t is record (f number);\n  procedure x;\nend;\n/\n" +
            "create package body private_items as\n  type bt is record (g date);\n  procedure x is\n    v1 private_items.bt;\n    v4 private_items.t;\n  begin\n    /*A*/ null;\n  end;\nend;\n/\n" + other
        assertSteps(privateItems, "private_items", listOf("Bound(PACKAGE_BODY private_items@6)", "Bound(RECORD_TYPE bt@7)"))
        assertSteps(privateItems, "private_items", listOf("Bound(PACKAGE_BODY private_items@6)", "Bound(RECORD_TYPE t@2)"), occurrence = 1)
        // Without the specification in the file a missing member may be in it: never an error.
        assertThat(steps("create package body p as\n  procedure x is begin say(p.zzz); end;\nend;\n/\n", "p"))
            .isEqualTo("Incomplete[SPECIFICATION_NOT_IN_FILE](Bound(PACKAGE_BODY p@1)) / Incomplete[SPECIFICATION_NOT_IN_FILE](Undeclared)")
        // `schema.pkg.x` is not local; the schema is absent like any undeclared first segment.
        assertThat(steps("create package body schema_pkg as\n  type bt is record (g date);\n  procedure x is v3 app_schema.schema_pkg.bt; begin null; end;\nend;\n/\n", "app_schema"))
            .isEqualTo("Incomplete[SPECIFICATION_NOT_IN_FILE](ProvenAbsent)")
    }

    @Test
    fun aLocalRecordValueQualifiesItsFields() {
        val records = "create procedure record_fields is\n  type r is record(x varchar2(10));\n  px r;\nbegin\n  /*A*/ say(px.MEMBER);\nend;\n/\n" + other
        // The record field, not package px's.
        assertSteps(records.replace("MEMBER", "x"), "px", listOf("Bound(VARIABLE px@3)", "Field(RECORD_TYPE r@2.x)"))
        // A missing field (PLS-00302), no fallback.
        assertSteps(records.replace("MEMBER", "y"), "px", listOf("Bound(VARIABLE px@3)", "Undeclared"))
        // After the block declaring px, the first segment is absent and the caller may try the package.
        assertThat(steps("create procedure after_inner_block is\nbegin\n  declare px number; begin null; end;\n  say(px.y);\nend;\n/\n", "px"))
            .isEqualTo("ProvenAbsent")
        // A field anchors %TYPE; a missing field is PLS-00302, never the table column.
        val fieldAnchor = "create procedure field_anchor is\n  type rec_t is record(a varchar2(10));\n  tb rec_t;\n  x tb.MEMBER%type;\nbegin\n  /*A*/ null;\nend;\n/\n"
        assertSteps(fieldAnchor.replace("MEMBER", "a"), "tb", listOf("Bound(VARIABLE tb@3)", "Field(RECORD_TYPE rec_t@2.a)"), context = QUALIFIER)
        assertSteps(fieldAnchor.replace("MEMBER", "z"), "tb", listOf("Bound(VARIABLE tb@3)", "Undeclared"), context = QUALIFIER)
        // Nested record types.
        val nested = "create procedure t is\n  type ri is record (z number);\n  type ro is record (i ri);\n  v ro;\nbegin\n  v.i.MEMBER := 5;\nend;\n/\n"
        assertThat(steps(nested.replace("MEMBER", "z"), "v")).isEqualTo("Bound(VARIABLE v@4) / Field(RECORD_TYPE ro@3.i) / Field(RECORD_TYPE ri@2.z)")
        assertThat(steps(nested.replace("MEMBER", "w"), "v")).isEqualTo("Bound(VARIABLE v@4) / Field(RECORD_TYPE ro@3.i) / Undeclared")
    }

    @Test
    fun aCapturedQualifierNamesItsOwnEntity() {
        // A local variable named like its procedure captures the qualified name.
        assertSteps(
            "create procedure variable_like_procedure(f date) is\n  type rt is record (f number);\n  variable_like_procedure rt;\nbegin\n  /*A*/ variable_like_procedure.f := 1;\nend;\n/\n" + other,
            "variable_like_procedure", listOf("Bound(VARIABLE variable_like_procedure@3)", "Field(RECORD_TYPE rt@2.f)")
        )
        // A parameter named like its package captures the qualified name.
        assertThat(
            steps(
                "create package cfg_pkg as\n  type rt is record (f number);\n  f date;\n  procedure x(cfg_pkg rt);\nend;\n/\n" +
                    "create package body cfg_pkg as\n  procedure x(cfg_pkg rt) is begin cfg_pkg.f := 1; end;\nend;\n/\n",
                "cfg_pkg", 0, QUALIFIER
            )
        ).isEqualTo("Bound(PARAMETER cfg_pkg@8) / Field(RECORD_TYPE rt@2.f)")
        // The PLS-00487 of a number variable needs its type.
        assertThat(steps("declare\n  pkg number;\nbegin\n  pkg.processar(1);\nend;\n/\n", "pkg"))
            .isEqualTo("Bound(VARIABLE pkg@2) / Unsupported(MEMBER_NEEDS_TYPING)")
    }

    @Test
    fun membersOfCollectionsTypesAndOtherKinds() {
        // Collection methods, and any other member of a nested table (PLS-00302).
        val coll = "create procedure t is\n  type ct is table of number;\n  v ct := ct(1);\nbegin\n  say(v.MEMBER);\nend;\n/\n"
        assertThat(steps(coll.replace("MEMBER", "count"), "v")).isEqualTo("Bound(VARIABLE v@3) / CollectionMethod(COLLECTION_TYPE ct@2)")
        assertThat(steps(coll.replace("MEMBER", "foo"), "v")).isEqualTo("Bound(VARIABLE v@3) / Undeclared")
        // An exception, a subtype and a record type take no member.
        assertThat(steps("create procedure t is\n  e exception;\n  x number;\nbegin\n  x := e.a;\nend;\n/\n", "e"))
            .isEqualTo("Bound(EXCEPTION e@2) / WrongKind(EXCEPTION e@2)")
        assertThat(steps("create procedure t is\n  subtype st is number;\n  x number;\nbegin\n  x := st.a;\nend;\n/\n", "st"))
            .isEqualTo("Bound(SUBTYPE st@2) / WrongKind(SUBTYPE st@2)")
        assertThat(steps("create procedure t is\n  type r is record (a number);\n  x number;\nbegin\n  x := r.a;\nend;\n/\n", "r"))
            .isEqualTo("Bound(RECORD_TYPE r@2) / WrongKind(RECORD_TYPE r@2)")
        // A cursor qualifies its parameter inside its own query only.
        assertThat(steps("create procedure t is\n  cursor c is select 1 a from dual;\n  x number;\nbegin\n  x := c.a;\nend;\n/\n", "c"))
            .isEqualTo("Bound(CURSOR c@2) / OutOfScope(CURSOR c@2)")
        // An attribute ends the qualified outcome: it is not a member lookup.
        assertThat(steps("create procedure t is\n  cursor c is select 1 a from dual;\n  x number;\nbegin\n  x := c%rowcount;\nend;\n/\n", "c"))
            .isEqualTo("Bound(CURSOR c@2)")
        // A cursor FOR loop record needs the query's row type.
        assertThat(steps("create procedure t is\nbegin\n  for r in (select 1 a from dual) loop say(r.a); end loop;\nend;\n/\n", "r"))
            .isEqualTo("Bound(LOOP_INDEX r@3) / Unsupported(MEMBER_NEEDS_TYPING)")
    }

    @Test
    fun labelTargets() {
        // The block's own label, not the outer label of the same name.
        assertSteps(
            "create procedure own_label_goto is\nbegin\n  <<l>> null;\n  begin\n    /*A*/ goto l;\n    <<l>> null;\n  end;\nend;\n/\n" + other,
            "l", listOf("Bound(LABEL l@6)"), context = GOTO_TARGET
        )
        // A forward GOTO; out of an IF branch.
        assertThat(steps("create procedure t is\nbegin\n  if 1 = 1 then goto l; end if;\n  <<l>> null;\nend;\n/\n", "l")).isEqualTo("Bound(LABEL l@4)")
        // Into an IF branch or a loop body (PLS-00375, PLS-00201); inside the same branch.
        assertThat(steps("create procedure t is\nbegin\n  goto l;\n  if 1 = 1 then <<l>> null; end if;\nend;\n/\n", "l")).isEqualTo("OutOfScope(LABEL l@4)")
        assertThat(steps("create procedure t is\nbegin\n  goto l;\n  loop <<l>> exit; end loop;\nend;\n/\n", "l")).isEqualTo("OutOfScope(LABEL l@4)")
        assertThat(steps("create procedure t is\nbegin\n  if 1 = 1 then goto l; <<l>> null; end if;\nend;\n/\n", "l")).isEqualTo("Bound(LABEL l@3)")
        // From a handler into its own block (PLS-00375).
        assertThat(steps("create procedure t is\nbegin\n  <<l>> null;\nexception when others then goto l;\nend;\n/\n", "l")).isEqualTo("OutOfScope(LABEL l@3)")
        // A label of a nested or sibling block, or none at all (PLS-00201): never an absence.
        assertThat(steps("create procedure t is\nbegin\n  goto l;\n  begin <<l>> null; end;\nend;\n/\n", "l")).isEqualTo("Undeclared")
        assertThat(steps("create procedure t is\nbegin\n  loop exit nolabel; end loop;\nend;\n/\n", "nolabel")).isEqualTo("Undeclared")
        // EXIT names a block's label (PLS-00373); a sibling loop's label (PLS-00374); enclosing loops.
        assertThat(steps("create procedure t is\nbegin\n  <<b>> begin\n    loop exit b; end loop;\n  end;\nend;\n/\n", "b", context = LOOP_TARGET))
            .isEqualTo("WrongKind(LABEL b@3)")
        assertThat(steps("create procedure t is\nbegin\n  <<a>> loop exit; end loop;\n  loop exit a; end loop;\nend;\n/\n", "a", context = LOOP_TARGET))
            .isEqualTo("OutOfScope(LABEL a@3)")
        assertThat(steps("create procedure t is\nbegin\n  <<o>> for i in 1..2 loop\n    for j in 1..3 loop continue o; end loop;\n  end loop;\nend;\n/\n", "o"))
            .isEqualTo("Bound(LABEL o@3)")
        assertThat(steps("create procedure t is\nbegin\n  <<o>> loop\n    begin exit o; end;\n  end loop;\nend;\n/\n", "o")).isEqualTo("Bound(LABEL o@3)")
        // A variable or a second label of the same name (PLS-00371).
        assertThat(steps("create procedure t is\n  x number;\nbegin\n  <<x>> loop exit x; end loop;\nend;\n/\n", "x", context = LOOP_TARGET))
            .isEqualTo("Duplicate(VARIABLE x@2, LABEL x@4)")
        assertThat(steps("create procedure t is\nbegin\n  goto l;\n  <<l>> null;\n  <<l>> null;\nend;\n/\n", "l")).isEqualTo("Duplicate(LABEL l@4, LABEL l@5)")
    }

    @Test
    fun aGotoNeverCrossesBranchesNorLeavesASubprogram() {
        // From one IF or CASE branch into another (PLS-00375).
        assertThat(steps("create procedure then_to_elsif(c number) is\nbegin\n  if c = 1 then goto l; elsif c = 2 then <<l>> null; end if;\nend;\n/\n", "l"))
            .isEqualTo("OutOfScope(LABEL l@3)")
        assertThat(steps("create procedure then_to_else(c number) is\nbegin\n  if c = 1 then goto l; else <<l>> null; end if;\nend;\n/\n", "l"))
            .isEqualTo("OutOfScope(LABEL l@3)")
        assertThat(steps("create procedure when_to_when(c number) is\nbegin\n  case when c = 1 then goto l; when c = 2 then <<l>> null; end case;\nend;\n/\n", "l"))
            .isEqualTo("OutOfScope(LABEL l@3)")
        assertThat(steps("create procedure when_to_else(c number) is\nbegin\n  case when c = 1 then goto l; else <<l>> null; end case;\nend;\n/\n", "l"))
            .isEqualTo("OutOfScope(LABEL l@3)")
        // Within one branch.
        assertSteps(
            "create procedure same_branch is\nbegin\n  if 1 = 1 then /*A*/ goto l; null; <<l>> null; end if;\nend;\n/\n" + other,
            "l", listOf("Bound(LABEL l@3)"), context = GOTO_TARGET
        )
        // Out of a nested procedure to the enclosing body's label (PLS-00375).
        assertSteps(
            "create procedure goto_out_of_subprogram is\n  procedure q is begin /*A*/ goto l; end;\nbegin\n  q;\n  <<l>> null;\nend;\n/\n" + other,
            "l", listOf("OutOfScope(LABEL l@5)"), context = GOTO_TARGET
        )
        // From a nested block, with or without declarations, and from a FOR loop body: allowed.
        assertSteps(
            "create procedure goto_out_of_block is\nbegin\n  declare x number; begin /*A*/ goto l; end;\n  <<l>> null;\nend;\n/\n" + other,
            "l", listOf("Bound(LABEL l@4)"), context = GOTO_TARGET
        )
        assertSteps(
            "create procedure goto_out_of_loop is\nbegin\n  for i in 1..3 loop /*A*/ goto l; end loop;\n  <<l>> null;\nend;\n/\n" + other,
            "l", listOf("Bound(LABEL l@4)"), context = GOTO_TARGET
        )
    }

    @Test
    fun exitAndContinueNeverLeaveASubprogram() {
        // A procedure declared in a block inside loop `l` cannot exit or continue it (PLS-00374).
        assertSteps(
            "create procedure exit_out_of_subprogram is\nbegin\n  <<l>> loop\n    declare\n      procedure q is begin /*A*/ exit l; end;\n" +
                "    begin\n      q; exit;\n    end;\n  end loop;\nend;\n/\n" + other,
            "l", listOf("OutOfScope(LABEL l@3)"), context = LOOP_TARGET
        )
        assertThat(
            steps(
                "create procedure continue_out_of_subprogram is\nbegin\n  <<l>> for i in 1..2 loop\n    declare\n      procedure q is begin continue l; end;\n" +
                    "    begin\n      q;\n    end;\n  end loop;\nend;\n/\n",
                "l", context = LOOP_TARGET
            )
        ).isEqualTo("OutOfScope(LABEL l@3)")
    }

    @Test
    fun collectionMembersDependOnTheCollectionForm() {
        val assoc = "create procedure assoc_members is\n  type aa is table of number index by pls_integer;\n  v aa;\nbegin\n  say(v.MEMBER);\nend;\n/\n"
        for (method in listOf("count", "first", "limit", "next", "prior", "last", "exists", "delete")) {
            assertThat(steps(assoc.replace("MEMBER", method), "v")).describedAs(method).isEqualTo("Bound(VARIABLE v@3) / CollectionMethod(COLLECTION_TYPE aa@2)")
        }
        // EXTEND and TRIM exist only for nested tables and varrays (PLS-00306).
        assertThat(steps(assoc.replace("MEMBER", "extend"), "v")).isEqualTo("Bound(VARIABLE v@3) / WrongKind(COLLECTION_TYPE aa@2)")
        assertThat(steps(assoc.replace("MEMBER", "trim"), "v")).isEqualTo("Bound(VARIABLE v@3) / WrongKind(COLLECTION_TYPE aa@2)")
        // Any other member of any collection form (PLS-00302).
        assertThat(steps(assoc.replace("MEMBER", "foo"), "v")).isEqualTo("Bound(VARIABLE v@3) / Undeclared")
        val varray = "create procedure varray_members is\n  type vt is varray(3) of number;\n  v vt := vt(1);\nbegin\n  say(v.MEMBER);\nend;\n/\n"
        assertThat(steps(varray.replace("MEMBER", "extend"), "v")).isEqualTo("Bound(VARIABLE v@3) / CollectionMethod(COLLECTION_TYPE vt@2)")
        assertThat(steps(varray.replace("MEMBER", "foo"), "v")).isEqualTo("Bound(VARIABLE v@3) / Undeclared")
        // A collection or ref cursor type name takes no member (PLS-00302).
        assertThat(steps("create procedure collection_type_member is\n  type ct is table of number;\n  x number;\nbegin\n  x := ct.count;\nend;\n/\n", "ct"))
            .isEqualTo("Bound(COLLECTION_TYPE ct@2) / WrongKind(COLLECTION_TYPE ct@2)")
        assertThat(steps("create procedure ref_cursor_type_member is\n  type rc is ref cursor;\n  x number;\nbegin\n  x := rc.a;\nend;\n/\n", "rc"))
            .isEqualTo("Bound(REF_CURSOR_TYPE rc@2) / WrongKind(REF_CURSOR_TYPE rc@2)")
    }

    @Test
    fun cursorOperandsAndForControls() {
        // An explicit cursor.
        assertSteps(
            "create procedure explicit_cursor is\n  cursor c is select 1 a from dual;\nbegin\n  /*A*/ open c;\nend;\n/\n" + other,
            "c", listOf("Bound(CURSOR c@2)"), context = CURSOR_OPERAND
        )
        // OPEN … FOR on an explicit cursor (PLS-00382).
        assertThat(steps("create procedure t is\n  cursor c is select 1 a from dual;\nbegin\n  open c for select 1 a from dual;\nend;\n/\n", "c", context = OPEN_FOR_OPERAND))
            .isEqualTo("WrongKind(CURSOR c@2)")
        // A variable is a cursor variable or not: typing.
        assertThat(steps("create procedure t is\n  k1 number;\nbegin\n  close k1;\nend;\n/\n", "k1"))
            .isEqualTo("Unsupported(KIND_IN_CONTEXT VARIABLE CURSOR_OPERAND)")
        // An exception or a procedure captures the operand (PLS-00456).
        assertThat(steps("create procedure t is\n  cursor k1 is select 1 a from dual;\nbegin\n  declare k1 exception; begin open k1; end;\nend;\n/\n", "k1", context = CURSOR_OPERAND))
            .isEqualTo("WrongKind(EXCEPTION k1@4)")
        assertThat(steps("create procedure t is\n  procedure k1 is begin null; end;\nbegin\n  open k1;\nend;\n/\n", "k1", context = CURSOR_OPERAND))
            .isEqualTo("WrongKind(PROCEDURE k1@2)")
        // A cursor in a FOR control, with or without arguments.
        assertSteps(
            "create procedure cursor_for_control is\n  cursor c(p number) is select p a from dual;\nbegin\n  /*A*/ for r in c(9) loop null; end loop;\nend;\n/\n" + other,
            "c", listOf("Bound(CURSOR c@2)"), context = FOR_CONTROL
        )
        // A value iterated once; an exception or a subtype (PLS-00320, PLS-00330).
        assertThat(steps("create procedure t is\n  k1 number := 5;\nbegin\n  for r in k1 loop null; end loop;\nend;\n/\n", "k1", context = FOR_CONTROL))
            .isEqualTo("Bound(VARIABLE k1@2)")
        assertThat(steps("create procedure t is\n  function k1 return number is begin return 3; end;\nbegin\n  for r in k1 loop null; end loop;\nend;\n/\n", "k1", context = FOR_CONTROL))
            .isEqualTo("OverloadSet(FUNCTION k1@2)")
        assertThat(steps("create procedure t is\n  k1 exception;\nbegin\n  for r in k1 loop null; end loop;\nend;\n/\n", "k1", context = FOR_CONTROL))
            .isEqualTo("WrongKind(EXCEPTION k1@2)")
    }

    @Test
    fun qualifiedOutcomesAreDeterministic() {
        val source = "create package p as\n  type rt is record (f number);\n  g rt;\nend;\n/\ncreate package body p as\n" +
            "  procedure a is\n    v rt;\n  begin\n    <<l>> for i in 1..2 loop v.f := p.g.f + a.v.f; exit l; end loop;\n  end;\nend;\n/\n"
        fun all(order: (List<LocalReference>) -> List<LocalReference>): List<String> {
            val analysis = support.analyze(source)
            val outcomes = order(analysis.references).associateWith { LocalResolver.resolveQualified(analysis.model, it) }
            return analysis.references.map { "$it -> ${outcomes.getValue(it).joinToString(" / ") { o -> support.render(analysis.model, o) }}" }
        }
        val forward = all { it }
        assertThat(all { it.reversed() }).isEqualTo(forward)
        assertThat(forward).anyMatch { it.contains("Bound(PACKAGE_BODY p@6) / Bound(VARIABLE g@3) / Field(RECORD_TYPE rt@2.f)") }
            .anyMatch { it.contains("Bound(PROCEDURE a@7) / Bound(VARIABLE v@8) / Field(RECORD_TYPE rt@2.f)") }
            .anyMatch { it.contains("LOOP_TARGET") && it.endsWith("Bound(LABEL l@10)") }
    }

    companion object {
        private const val A = "/*A*/"
        private const val S = "/*S*/"
        private const val IF = "\$IF \$\$x \$THEN NULL; \$END"
    }
}
