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
package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectTypeDescription.AnchorStop
import com.felipebz.zpa.api.project.ProjectTypeDescription.Status
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.*
import com.felipebz.zpa.api.project.ProjectBuiltInTypeCategory as Category
import com.felipebz.zpa.api.project.ProjectValueResolution.Status as ValueStatus

/** Source-backed `%TYPE` traversal; the scope rules follow the Oracle probes recorded in the S5 design. */
@OptIn(ZpaExperimentalApi::class)
class ProjectAnchorDescriptionTest {
    private val pSpec = """
        CREATE PACKAGE p AS
          SUBTYPE numeric_t IS NUMBER;
          g NUMBER;
          c CONSTANT VARCHAR2(30) := 'v';
          first_value numeric_t;
          second_value first_value%TYPE;
          third_value second_value%TYPE;
          TYPE rec_t IS RECORD (id NUMBER);
          TYPE rows_t IS TABLE OF rec_t;
          rec_value rec_t;
          rows_value rows_t;
          alias_value g%TYPE;
          later_ref later_value%TYPE;
          later_value NUMBER;
          self_ref self_ref%TYPE;
          "Mixed" NUMBER;
          quoted_ref "Mixed"%TYPE;
          qual_self p.g%TYPE;
          field_ref rec_value.id%TYPE;
          type_ref rec_t%TYPE;
          TYPE cap_t IS RECORD (a NUMBER, b a%TYPE, c2 g%TYPE);
          PROCEDURE proc(g NUMBER, x g%TYPE, y c%TYPE);
          FUNCTION fn(c NUMBER) RETURN c%TYPE;
          column_ref employees.salary%TYPE;
          row_ref employees%ROWTYPE;
          obj_ref REF obj_t;
        END p;
    """.trimIndent()
    private val qSpec = """
        CREATE PACKAGE q AS
          a p.g%TYPE;
          b p.c%TYPE;
          r p.rec_value%TYPE;
          rs p.rows_value%TYPE;
          al p.alias_value%TYPE;
          t3 p.third_value%TYPE;
          fa p.field_ref%TYPE;
          col employees.salary%TYPE;
          qm p."Mixed"%TYPE;
        END q;
        /
        CREATE PACKAGE q2 AS
          x p.g%TYPE;
          p NUMBER;
        END q2;
    """.trimIndent()
    private val extra = """
        CREATE PACKAGE m1 AS g NUMBER; END m1;
        /
        CREATE PACKAGE m2 AS x m1.g%TYPE; END m2;
        /
        CREATE PACKAGE m3 AS x m4.g%TYPE; END m3;
        /
        CREATE PACKAGE m4 AS g NUMBER; END m4;
        /
        CREATE PACKAGE dup AS g NUMBER; END dup;
        /
        CREATE PACKAGE dup AS x g%TYPE; END dup;
        /
        CREATE PACKAGE k AS g NUMBER; PROCEDURE g; END k;
        /
        CREATE PACKAGE kq AS x k.g%TYPE; END kq;
        /
        CREATE PROCEDURE sp(x g%TYPE, y p.g%TYPE) IS BEGIN NULL; END;
        /
        CREATE PACKAGE bp AS PROCEDURE w(a NUMBER); END bp;
        /
        CREATE PACKAGE BODY bp AS PROCEDURE w(x g%TYPE, y p.g%TYPE) IS BEGIN NULL; END; END bp;
        /
        CREATE PACKAGE ca AS a cb.b%TYPE; END ca;
        /
        CREATE PACKAGE cb AS b ca.a%TYPE; END cb;
        /
        CREATE PACKAGE pa AS SUBTYPE s IS pb.v%TYPE; END pa;
        /
        CREATE PACKAGE pb AS v pa.s; END pb;
    """.trimIndent()
    private val dupP = "CREATE PACKAGE p AS g DATE; END p;"
    private val files = listOf("p.sql" to pSpec, "q.sql" to qSpec, "x.sql" to extra)

    private fun n(vararg s: String) = ProjectQualifiedName.of(*s)
    private fun prepare(concurrent: Boolean = false, list: List<Pair<String, String>> = files, vararg more: ProjectSourceInput) =
        ProjectSnapshots.prepare(list.map { (id, text) -> ProjectSourceInput.ofText(id, text) } + more, concurrent)

    private val snapshot = prepare()

    private fun value(s: ProjectSnapshot, vararg name: String) = s.findDeclarations(n(*name)).declarations.single() as ProjectPackageValueView
    private fun value(vararg name: String) = value(snapshot, *name)
    private fun describe(s: ProjectSnapshot, vararg name: String) = s.describeType(value(s, *name).typeRef, value(s, *name).owner)
    private fun describe(vararg name: String) = describe(snapshot, *name)
    private fun ids(d: ProjectTypeDescription) = d.steps.map { it.id }
    private fun id(vararg name: String) = snapshot.findDeclarations(n(*name)).declarations.single().id

    @Test
    fun directAnchorsFollowTheSourceValueToItsDeclaredType() {
        val a = describe("q", "a")
        assertThat(a.status).isEqualTo(Status.BUILT_IN)
        assertThat(a.builtInCategory).isEqualTo(Optional.of(Category.NUMERIC))
        assertThat(ids(a)).containsExactly(id("p", "g"))
        assertThat(a.values.map { it.qualifiedName }).containsExactly(n("p", "g"))
        assertThat(a.subtypes).isEmpty()
        assertThat(a.terminalReference.get().sourceRange).isEqualTo(value("p", "g").typeRef.sourceRange)
        assertThat(a.anchorStop).isEmpty
        assertThat(a.valueResolution).isEmpty

        val b = describe("q", "b")
        assertThat(b.builtInCategory).isEqualTo(Optional.of(Category.CHARACTER))
        assertThat(ids(b)).containsExactly(id("p", "c"))
        assertThat(b.values.single().isConstant).isTrue()

        val record = describe("q", "r")
        assertThat(record.status).isEqualTo(Status.DECLARED_TYPE)
        assertThat(record.declaration.get().id).isEqualTo(id("p", "rec_t"))
        assertThat((record.declaration.get() as ProjectPackageTypeView).recordFields.map { it.name.originalSpelling }).containsExactly("id")
        assertThat(ids(record)).containsExactly(id("p", "rec_value"))

        val rows = describe("q", "rs")
        assertThat(rows.declaration.get().id).isEqualTo(id("p", "rows_t"))
        assertThat((rows.declaration.get() as ProjectShapedTypeView).elementType.get().name).isEqualTo(n("rec_t"))

        val quoted = describe("q", "qm")
        assertThat(quoted.builtInCategory).isEqualTo(Optional.of(Category.NUMERIC))
        assertThat(describe("p", "quoted_ref").values.map { it.qualifiedName }).containsExactly(n("p", "\"Mixed\""))
        assertThat(describe("p", "qual_self").values.map { it.qualifiedName }).containsExactly(n("p", "g"))
    }

    @Test
    fun valueAndSubtypeChainsKeepOrderedStepsAndIdentity() {
        val alias = describe("q", "al")
        assertThat(alias.values.map { it.qualifiedName }).containsExactly(n("p", "alias_value"), n("p", "g"))
        assertThat(alias.builtInCategory).isEqualTo(Optional.of(Category.NUMERIC))
        val chain = describe("q", "t3")
        assertThat(chain.status).isEqualTo(Status.BUILT_IN)
        assertThat(chain.values.map { it.qualifiedName }).containsExactly(n("p", "third_value"), n("p", "second_value"), n("p", "first_value"))
        assertThat(chain.subtypes.map { it.qualifiedName }).containsExactly(n("p", "numeric_t"))
        assertThat(ids(chain)).containsExactly(id("p", "third_value"), id("p", "second_value"), id("p", "first_value"), id("p", "numeric_t"))
        assertThat(chain.terminalReference.get().sourceRange).isEqualTo((snapshot.findDeclarations(n("p", "numeric_t")).declarations.single() as ProjectSubtypeView).baseType.sourceRange)
        assertThatThrownBy { (chain.steps as MutableList<*>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { (chain.values as MutableList<*>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun declarationOrderInTheSameSpecificationIsChecked() {
        listOf("later_ref", "self_ref").forEach {
            val d = describe("p", it)
            assertThat(d.status).describedAs(it).isEqualTo(Status.ANCHORED_REFERENCE)
            assertThat(d.anchorStop).isEqualTo(Optional.of(AnchorStop.DECLARED_LATER))
            assertThat(d.terminalReference.get().sourceRange).isEqualTo(value("p", it).typeRef.sourceRange)
            assertThat(d.steps).isEmpty()
        }
        // Different package units are not ordered against each other, even in one file and when declared later.
        assertThat(describe("m2", "x").builtInCategory).isEqualTo(Optional.of(Category.NUMERIC))
        assertThat(describe("m3", "x").builtInCategory).isEqualTo(Optional.of(Category.NUMERIC))
        // One value is unique, but two specifications named dup are not one binding scope.
        val dup = describe("dup", "x")
        assertThat(dup.status).isEqualTo(Status.ANCHORED_REFERENCE)
        assertThat(dup.anchorStop).isEqualTo(Optional.of(AnchorStop.ENCLOSING_SCOPE))
    }

    @Test
    fun scopeCaptureStopsBeforeAnyCoincidentalValue() {
        fun stop(d: ProjectTypeDescription, why: AnchorStop) {
            assertThat(d.status).isEqualTo(Status.ANCHORED_REFERENCE)
            assertThat(d.anchorStop).isEqualTo(Optional.of(why))
            assertThat(d.valueResolution).isEmpty
        }
        stop(describe("p", "type_ref"), AnchorStop.ENCLOSING_SCOPE) // a package type captures rec_t
        stop(describe("p", "field_ref"), AnchorStop.FIELD_PATH) // rec_value is a package value: field path
        stop(describe("q", "fa"), AnchorStop.FIELD_PATH)
        assertThat(describe("q", "fa").values.map { it.qualifiedName }).containsExactly(n("p", "field_ref"))
        stop(describe("q2", "x"), AnchorStop.FIELD_PATH) // q2 declares a value p that captures p.g

        val cap = snapshot.findDeclarations(n("p", "cap_t")).declarations.single() as ProjectPackageTypeView
        stop(snapshot.describeType(cap.recordFields[1].typeRef, cap.owner), AnchorStop.ENCLOSING_SCOPE) // sibling field a
        assertThat(snapshot.describeType(cap.recordFields[2].typeRef, cap.owner).values.map { it.qualifiedName }).containsExactly(n("p", "g"))

        val proc = snapshot.findDeclarations(n("p", "proc")).declarations.single() as ProjectSubprogramView
        stop(snapshot.describeType(proc.parameters[1].typeRef, p), AnchorStop.ENCLOSING_SCOPE) // parameter g
        assertThat(snapshot.describeType(proc.parameters[2].typeRef, p).builtInCategory).isEqualTo(Optional.of(Category.CHARACTER))
        val fn = snapshot.findDeclarations(n("p", "fn")).declarations.single() as ProjectFunctionView
        stop(snapshot.describeType(fn.returnType, p), AnchorStop.ENCLOSING_SCOPE) // conservative: parameter c

        // Standalone: no package scope, only qualified names; body-held: one-segment names cannot be judged.
        val standalone = snapshot.findDeclarations(n("sp")).declarations.single() as ProjectSubprogramView
        stop(snapshot.describeType(standalone.parameters[0].typeRef, null), AnchorStop.ENCLOSING_SCOPE)
        assertThat(snapshot.describeType(standalone.parameters[1].typeRef, null).values.map { it.qualifiedName }).containsExactly(n("p", "g"))
        val body = snapshot.findPackageSubprograms(n("bp"), ProjectIdentifier.fromSource("w")).declarations.single { it.role == ProjectDeclarationRole.BODY }
        stop(snapshot.describeType(body.parameters[0].typeRef, n("bp")), AnchorStop.ENCLOSING_SCOPE)
        assertThat(snapshot.describeType(body.parameters[1].typeRef, n("bp")).values.map { it.qualifiedName }).containsExactly(n("p", "g"))
    }

    @Test
    fun rowtypeAndRefStopWithoutAnyValueLookup() {
        val row = describe("p", "row_ref")
        assertThat(row.status).isEqualTo(Status.ANCHORED_REFERENCE)
        assertThat(row.anchorStop).isEqualTo(Optional.of(AnchorStop.ROWTYPE))
        val failed = prepare(false, files, ProjectSourceInput.ofReader("bad.sql") { throw java.io.IOException("x") })
        assertThat(describe(failed, "p", "row_ref").anchorStop).isEqualTo(Optional.of(AnchorStop.ROWTYPE))
        assertThat(describe(failed, "p", "type_ref").anchorStop).isEqualTo(Optional.of(AnchorStop.ENCLOSING_SCOPE))
        assertThat(describe("p", "obj_ref").status).isEqualTo(Status.REF_REFERENCE)
    }

    @Test
    fun unresolvedValuesCarryTheExistingLookupResult() {
        val col = describe("q", "col")
        assertThat(col.status).isEqualTo(Status.UNRESOLVED_VALUE)
        assertThat(col.valueResolution.get().status).isEqualTo(ValueStatus.NOT_FOUND_IN_PROJECT)
        assertThat(col.valueResolution.get().name).isEqualTo(n("employees", "salary"))
        assertThat(col.terminalReference.get().kind).isEqualTo(ProjectTypeReference.Kind.ANCHORED)
        assertThat(col.steps).isEmpty()
        assertThat(col.declaration).isEmpty
        assertThat(col.resolution).isEmpty
        // Collisions and duplicates are ambiguous values.
        val collision = describe("kq", "x")
        assertThat(collision.valueResolution.get().status).isEqualTo(ValueStatus.AMBIGUOUS)
        assertThat(collision.valueResolution.get().candidates.map { it.kind }).containsExactly(ProjectDeclarationKind.PACKAGE_VARIABLE, ProjectDeclarationKind.PACKAGE_PROCEDURE)
        val duplicated = prepare(false, files + ("p2.sql" to dupP))
        val ambiguous = describe(duplicated, "q", "a")
        assertThat(ambiguous.status).isEqualTo(Status.UNRESOLVED_VALUE)
        assertThat(ambiguous.valueResolution.get().status).isEqualTo(ValueStatus.AMBIGUOUS)
        assertThat(ambiguous.valueResolution.get().candidates).hasSize(2)
        // The value is unique but p is specified twice: not a certain binding.
        val twoSpecs = prepare(false, files + ("p3.sql" to "CREATE PACKAGE p AS other NUMBER; END p;"))
        assertThat(describe(twoSpecs, "q", "a").anchorStop).isEqualTo(Optional.of(AnchorStop.ENCLOSING_SCOPE))
        // Failures take precedence over a unique known value and over no candidate.
        val failed = prepare(false, files, ProjectSourceInput.ofReader("bad.sql") { throw java.io.IOException("x") })
        listOf("a", "col").forEach {
            val d = describe(failed, "q", it)
            assertThat(d.status).describedAs(it).isEqualTo(Status.UNRESOLVED_VALUE)
            assertThat(d.valueResolution.get().status).isEqualTo(ValueStatus.INCOMPLETE_INDEX)
        }
        assertThat(describe(failed, "q", "a").valueResolution.get().candidates).hasSize(1)
        assertThat(describe(failed, "q", "t3").valueResolution.get().status).isEqualTo(ValueStatus.INCOMPLETE_INDEX)
        // An intermediate unresolved named type keeps the existing UNRESOLVED_NAME.
        val named = prepare(false, listOf("n.sql" to "CREATE PACKAGE nn AS v unknown_t; w v%TYPE; END nn;"))
        val d = describe(named, "nn", "w")
        assertThat(d.status).isEqualTo(Status.UNRESOLVED_NAME)
        assertThat(d.values.map { it.qualifiedName }).containsExactly(n("nn", "v"))
    }

    @Test
    fun referencesWithoutAnExactHolderGetNoScope() {
        val reference = value("q", "a").typeRef
        // A snapshot without the file, one with a different name at the same range, and an unprepared-like empty one.
        val empty = prepare(false, listOf("other.sql" to "CREATE PACKAGE o AS z NUMBER; END o;"))
        assertThat(empty.describeType(reference, n("q")).anchorStop).isEqualTo(Optional.of(AnchorStop.ENCLOSING_SCOPE))
        val renamed = prepare(false, listOf("q.sql" to qSpec.replace("a p.g%TYPE;", "a p.h%TYPE;")) + ("p.sql" to pSpec))
        assertThat(renamed.describeType(reference, n("q")).anchorStop).isEqualTo(Optional.of(AnchorStop.ENCLOSING_SCOPE))
        // Identical source facts in another snapshot cannot be told apart by the public reference alone.
        val twin = prepare(true)
        assertThat(twin.describeType(reference, n("q")).values.map { it.qualifiedName }).containsExactly(n("p", "g"))
        // The supplied package owner is never promoted to a scope.
        assertThat(empty.describeType(reference, n("p")).anchorStop).isEqualTo(Optional.of(AnchorStop.ENCLOSING_SCOPE))
    }

    @Test
    fun cyclesAcrossUnitsAreDetectedOnceByOccurrenceForValuesAndSubtypes() {
        val values = describe("ca", "a")
        assertThat(values.status).isEqualTo(Status.CYCLE)
        assertThat(values.values.map { it.qualifiedName }).containsExactly(n("cb", "b"), n("ca", "a"))
        assertThat(ids(values).toSet()).hasSize(2)
        val mixed = describe("pb", "v")
        assertThat(mixed.status).isEqualTo(Status.CYCLE)
        assertThat(mixed.steps.map { it.kind }).containsExactly(ProjectDeclarationKind.PACKAGE_SUBTYPE, ProjectDeclarationKind.PACKAGE_VARIABLE)
        assertThat(mixed.subtypes).hasSize(1)
        assertThat(mixed.values).hasSize(1)
    }

    @Test
    fun longAcyclicValueChainsAreNotLimitedNorRecursive() {
        val size = 3000
        val source = "CREATE PACKAGE big AS\n  v0 NUMBER;\n" + (1 until size).joinToString("\n") { "  v$it v${it - 1}%TYPE;" } + "\nEND big;"
        val big = prepare(false, listOf("big.sql" to source))
        val described = describe(big, "big", "v${size - 1}")
        assertThat(described.status).isEqualTo(Status.BUILT_IN)
        assertThat(described.values).hasSize(size - 1)
        assertThat(described.steps.map { it.id }.toSet()).hasSize(size - 1)
    }

    @Test
    fun resultsAreDeterministicForSerialConcurrentAndReversedPreparation() {
        fun summary(s: ProjectSnapshot) = listOf(
            listOf("q", "a"), listOf("q", "t3"), listOf("q", "fa"), listOf("q", "col"), listOf("p", "later_ref"), listOf("ca", "a"), listOf("kq", "x")
        ).map {
            val d = describe(s, *it.toTypedArray())
            listOf(d.status, d.steps.map { v -> v.id.fileId to v.id.ordinal }, d.anchorStop, d.valueResolution.map { r -> r.status }, d.builtInCategory)
        }
        val serial = summary(prepare(false))
        assertThat(summary(prepare(true))).isEqualTo(serial)
        assertThat(summary(prepare(false, files.reversed()))).isEqualTo(serial)
    }

    @Test
    fun optionalPropertiesArePresentOnlyForTheirStatus() {
        listOf(describe("q", "a"), describe("q", "r"), describe("q", "col"), describe("p", "later_ref"), describe("p", "obj_ref"), describe("ca", "a")).forEach {
            assertThat(it.valueResolution.isPresent).isEqualTo(it.status == Status.UNRESOLVED_VALUE)
            assertThat(it.anchorStop.isPresent).isEqualTo(it.status == Status.ANCHORED_REFERENCE)
            assertThat(it.resolution.isPresent).isEqualTo(it.status == Status.UNRESOLVED_NAME)
            assertThat(it.declaration.isPresent).isEqualTo(it.status == Status.DECLARED_TYPE)
            assertThat(it.builtInCategory.isPresent).isEqualTo(it.status == Status.BUILT_IN)
            assertThat(it.steps.filterIsInstance<ProjectPackageValueView>()).isEqualTo(it.values)
            assertThat(it.steps.filterIsInstance<ProjectSubtypeView>()).isEqualTo(it.subtypes)
        }
    }

    private val p = n("p")
}
