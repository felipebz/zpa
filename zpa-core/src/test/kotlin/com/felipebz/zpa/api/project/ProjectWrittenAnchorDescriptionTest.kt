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
import com.felipebz.zpa.api.project.ProjectTypeReference.Anchor
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.*
import com.felipebz.zpa.api.project.ProjectBuiltInTypeCategory as Category
import com.felipebz.zpa.api.project.ProjectValueResolution.Status as ValueStatus

/** Caller-written `%TYPE`/`%ROWTYPE` anchors described through the existing source describer. */
@OptIn(ZpaExperimentalApi::class)
class ProjectWrittenAnchorDescriptionTest {
    private val pSpec = """
        CREATE PACKAGE p AS
          SUBTYPE money IS NUMBER;
          SUBTYPE amount IS money;
          g NUMBER;
          c CONSTANT VARCHAR2(30) := 'v';
          TYPE rec_t IS RECORD (id NUMBER);
          r rec_t;
          m amount;
          "Mixed" NUMBER;
          ext employees.salary%TYPE;
          row_ref employees%ROWTYPE;
        END p;
    """.trimIndent()
    private val qSpec = """
        CREATE PACKAGE q AS
          a p.g%TYPE;
          b p.c%TYPE;
          rr p.r%TYPE;
          mm p.m%TYPE;
          qm p."Mixed"%TYPE;
          col employees.salary%TYPE;
          row_q employees%ROWTYPE;
          fp p.r.id%TYPE;
          miss p.nope%TYPE;
          via p.ext%TYPE;
        END q;
    """.trimIndent()
    private val extra = """
        CREATE PACKAGE ca AS a cb.b%TYPE; END ca;
        /
        CREATE PACKAGE cb AS b ca.a%TYPE; END cb;
        /
        CREATE PACKAGE dup AS g NUMBER; END dup;
        /
        CREATE PACKAGE dup AS x NUMBER; END dup;
    """.trimIndent()
    private val files = listOf("p.sql" to pSpec, "q.sql" to qSpec, "x.sql" to extra)

    private fun n(vararg s: String) = ProjectQualifiedName.of(*s)
    private fun prepare(list: List<Pair<String, String>> = files, vararg more: ProjectSourceInput) =
        ProjectSnapshots.prepare(list.map { (id, text) -> ProjectSourceInput.ofText(id, text) } + more, false)

    private val snapshot = prepare()

    private fun w(anchor: Anchor, vararg name: String) = ProjectWrittenAnchor.of(n(*name), anchor)
    private fun type(vararg name: String) = w(Anchor.TYPE, *name)
    private fun row(vararg name: String) = w(Anchor.ROWTYPE, *name)
    private fun describe(anchor: ProjectWrittenAnchor, owner: ProjectQualifiedName? = null, s: ProjectSnapshot = snapshot) =
        s.describeType(anchor, owner)
    private fun view(vararg name: String) = snapshot.findDeclarations(n(*name)).declarations.single()
    private fun value(vararg name: String) = view(*name) as ProjectPackageValueView
    private fun retained(vararg name: String) = snapshot.describeType(value(*name).typeRef, value(*name).owner)

    private fun assertStopAtRequest(d: ProjectTypeDescription, request: ProjectWrittenAnchor) {
        assertThat(d.steps).isEmpty()
        assertThat(d.terminalReference).isEmpty
        assertThat(d.writtenAnchor.get()).isSameAs(request)
    }

    @Test
    fun packageValueTypeResolvesToBuiltInScalar() {
        val request = type("p", "g")
        val d = describe(request)
        assertThat(d.status).isEqualTo(Status.BUILT_IN)
        assertThat(d.builtInCategory).isEqualTo(Optional.of(Category.NUMERIC))
        assertThat(d.steps.map { it.id }).containsExactly(view("p", "g").id)
        // After following the value, the terminal is the genuine retained reference with its real range.
        assertThat(d.terminalReference.get().sourceRange).isEqualTo(value("p", "g").typeRef.sourceRange)
        assertThat(d.writtenAnchor.get()).isSameAs(request)
        assertThat(describe(type("p", "c")).builtInCategory).isEqualTo(Optional.of(Category.CHARACTER))
    }

    @Test
    fun packageValueTypeResolvesToDeclaredRecord() {
        val d = describe(type("p", "r"))
        assertThat(d.status).isEqualTo(Status.DECLARED_TYPE)
        assertThat(d.declaration.get().qualifiedName).isEqualTo(n("p", "rec_t"))
        assertThat((d.declaration.get() as ProjectPackageTypeView).recordFields.map { it.name.originalSpelling }).containsExactly("id")
        assertThat(d.steps.map { it.qualifiedName }).containsExactly(n("p", "r"))
    }

    @Test
    fun subtypeChainsAreFollowedAfterTheValue() {
        val d = describe(type("p", "m"))
        assertThat(d.status).isEqualTo(Status.BUILT_IN)
        assertThat(d.steps.map { it.qualifiedName }).containsExactly(n("p", "m"), n("p", "amount"), n("p", "money"))
        assertThat(d.values).hasSize(1)
        assertThat(d.subtypes.map { it.qualifiedName }).containsExactly(n("p", "amount"), n("p", "money"))
    }

    @Test
    fun identifierIdentityFollowsOracleRules() {
        assertThat(type("p", "g")).isEqualTo(type("P", "\"G\"")).hasSameHashCodeAs(type("P", "\"G\""))
        assertThat(type("p", "g")).isNotEqualTo(type("p", "\"g\"")).isNotEqualTo(row("p", "g"))
        assertThat(describe(type("P", "G")).steps.map { it.id }).containsExactly(view("p", "g").id)
        assertThat(describe(type("p", "\"Mixed\"")).status).isEqualTo(Status.BUILT_IN)
        val wrongCase = describe(type("p", "\"mixed\""))
        assertThat(wrongCase.status).isEqualTo(Status.UNRESOLVED_VALUE)
        assertThat(wrongCase.valueResolution.get().status).isEqualTo(ValueStatus.NOT_FOUND_IN_PROJECT)
        assertThat(type("p", "g").toString()).isEqualTo("p.g%TYPE")
    }

    @Test
    fun missingPackageValueStopsAtTheRequest() {
        val request = type("p", "nope")
        val d = describe(request)
        assertThat(d.status).isEqualTo(Status.UNRESOLVED_VALUE)
        assertThat(d.valueResolution.get().status).isEqualTo(ValueStatus.NOT_FOUND_IN_PROJECT)
        assertThat(d.valueResolution.get().name).isEqualTo(request.name)
        assertStopAtRequest(d, request)
    }

    @Test
    fun incompletePreparationAndDuplicateSpecificationsStayUnresolved() {
        val failed = prepare(files, ProjectSourceInput.ofReader("bad.sql") { throw java.io.IOException("x") })
        val request = type("p", "g")
        val incomplete = describe(request, s = failed)
        assertThat(incomplete.status).isEqualTo(Status.UNRESOLVED_VALUE)
        assertThat(incomplete.valueResolution.get().status).isEqualTo(ValueStatus.INCOMPLETE_INDEX)
        assertStopAtRequest(incomplete, request)

        // Two values p.g in two specifications: ambiguous value lookup.
        val twoValues = prepare(files + ("p2.sql" to "CREATE PACKAGE p AS g DATE; END p;"))
        assertThat(describe(type("p", "g"), s = twoValues).valueResolution.get().status).isEqualTo(ValueStatus.AMBIGUOUS)
        // One value, but two specifications of its package: not one scope.
        val dup = describe(type("dup", "g"))
        assertThat(dup.status).isEqualTo(Status.ANCHORED_REFERENCE)
        assertThat(dup.anchorStop).isEqualTo(Optional.of(AnchorStop.ENCLOSING_SCOPE))
        assertThat(dup.terminalReference).isEmpty
    }

    @Test
    fun writtenExternalColumnTypeStopsAtTheRequest() {
        val request = type("employees", "salary")
        val d = describe(request)
        assertThat(d.status).isEqualTo(Status.UNRESOLVED_VALUE)
        assertThat(d.valueResolution.get().status).isEqualTo(ValueStatus.NOT_FOUND_IN_PROJECT)
        assertStopAtRequest(d, request)
        // A retained external anchor reached through a value keeps its real terminal.
        val via = describe(type("p", "ext"))
        assertThat(via.status).isEqualTo(Status.UNRESOLVED_VALUE)
        assertThat(via.steps.map { it.qualifiedName }).containsExactly(n("p", "ext"))
        assertThat(via.terminalReference.get().sourceRange).isEqualTo(value("p", "ext").typeRef.sourceRange)
    }

    @Test
    fun writtenExternalRelationRowtypeStopsAtTheRequest() {
        val request = row("employees")
        val d = describe(request)
        assertThat(d.status).isEqualTo(Status.ANCHORED_REFERENCE)
        assertThat(d.anchorStop).isEqualTo(Optional.of(AnchorStop.ROWTYPE))
        assertThat(d.valueResolution).isEmpty
        assertStopAtRequest(d, request)
        assertThat(describe(row("hr", "employees"), n("p")).anchorStop).isEqualTo(Optional.of(AnchorStop.ROWTYPE))
    }

    @Test
    fun missingOwnerBlocksOneSegmentTypeButNotRowtype() {
        val typeRequest = type("g")
        val noOwner = describe(typeRequest)
        assertThat(noOwner.status).isEqualTo(Status.ANCHORED_REFERENCE)
        assertThat(noOwner.anchorStop).isEqualTo(Optional.of(AnchorStop.ENCLOSING_SCOPE))
        assertStopAtRequest(noOwner, typeRequest)
        assertThat(describe(type("g"), n("p")).steps.map { it.id }).containsExactly(view("p", "g").id)

        val rowRequest = row("employees")
        assertThat(describe(rowRequest).anchorStop).isEqualTo(Optional.of(AnchorStop.ROWTYPE))
        assertThat(describe(rowRequest, n("p")).anchorStop).isEqualTo(Optional.of(AnchorStop.ROWTYPE))
    }

    @Test
    fun fieldPathCompetitionMemberCaptureAndCycles() {
        val fieldPath = type("p", "r", "id")
        val competing = describe(fieldPath)
        assertThat(competing.anchorStop).isEqualTo(Optional.of(AnchorStop.FIELD_PATH))
        assertStopAtRequest(competing, fieldPath)
        // A package value named like the first segment captures it: the qualified name is a field path.
        assertThat(describe(type("r", "id"), n("p")).anchorStop).isEqualTo(Optional.of(AnchorStop.FIELD_PATH))
        // A package declaration that is not a value captures the first segment.
        assertThat(describe(type("rec_t"), n("p")).anchorStop).isEqualTo(Optional.of(AnchorStop.ENCLOSING_SCOPE))
        assertThat(describe(type("rec_t", "id"), n("p")).anchorStop).isEqualTo(Optional.of(AnchorStop.ENCLOSING_SCOPE))

        val cycle = describe(type("ca", "a"))
        assertThat(cycle.status).isEqualTo(Status.CYCLE)
        assertThat(cycle.steps.map { it.qualifiedName }).containsExactly(n("ca", "a"), n("cb", "b"))
        assertThat(cycle.terminalReference.get().sourceRange).isEqualTo(value("cb", "b").typeRef.sourceRange)
    }

    @Test
    fun writtenAndRetainedAnchorsAgreeWhereBothApply() {
        // Same written name and owner as each retained value of q: identical walk outside the retained-only guards.
        for (name in listOf("a", "b", "rr", "mm", "qm", "col", "row_q", "fp", "miss", "via")) {
            val reference = value("q", name).typeRef
            val fromRetained = retained("q", name)
            val fromWritten = describe(ProjectWrittenAnchor.of(reference.name, reference.anchor.get()), n("q"))
            assertThat(fromWritten.status).describedAs(name).isEqualTo(fromRetained.status)
            assertThat(fromWritten.anchorStop).describedAs(name).isEqualTo(fromRetained.anchorStop)
            assertThat(fromWritten.steps.map { it.id }).describedAs(name).isEqualTo(fromRetained.steps.map { it.id })
            assertThat(fromWritten.builtInCategory).describedAs(name).isEqualTo(fromRetained.builtInCategory)
            assertThat(fromWritten.valueResolution.map { it.status }).describedAs(name).isEqualTo(fromRetained.valueResolution.map { it.status })
            if (fromRetained.steps.isNotEmpty()) {
                assertThat(fromWritten.terminalReference.map { it.sourceRange }).describedAs(name)
                    .isEqualTo(fromRetained.terminalReference.map { it.sourceRange })
            } else {
                assertThat(fromWritten.terminalReference).describedAs(name).isEmpty
                assertThat(fromRetained.terminalReference).describedAs(name).isPresent
            }
            assertThat(fromRetained.writtenAnchor).describedAs(name).isEmpty
        }
    }

    @Test
    fun nameOnlyAndRetainedQueriesAreUnchanged() {
        val byName = snapshot.describeType(n("amount"), n("p"))
        assertThat(byName.status).isEqualTo(Status.BUILT_IN)
        assertThat(byName.writtenAnchor).isEmpty
        assertThat(byName.terminalReference.get().sourceRange).isEqualTo((view("p", "money") as ProjectSubtypeView).baseType.sourceRange)
        val stopped = retained("p", "row_ref")
        assertThat(stopped.anchorStop).isEqualTo(Optional.of(AnchorStop.ROWTYPE))
        assertThat(stopped.terminalReference.get().sourceRange).isEqualTo(value("p", "row_ref").typeRef.sourceRange)
        assertThat(stopped.writtenAnchor).isEmpty
    }

    @Test
    fun resultsAreImmutableAndEchoTheRequest() {
        val request = type("p", "m")
        val d = describe(request)
        assertThat(d.writtenAnchor.get()).isSameAs(request)
        assertThat(d.writtenAnchor.get().name).isEqualTo(n("p", "m"))
        assertThat(d.writtenAnchor.get().anchor).isEqualTo(Anchor.TYPE)
        assertThatThrownBy { (d.steps as MutableList<ProjectDeclarationView>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { (d.subtypes as MutableList<ProjectSubtypeView>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { (n("p", "m").segments as MutableList<ProjectIdentifier>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun invalidResultsAreRejected() {
        val missing = snapshot.resolvePackageValue(n("p", "nope"), null)
        val someView = view("p", "g")
        val typeRequest = type("p", "nope")
        // Empty terminal without a written anchor: the retained/name contract is unchanged.
        assertThatThrownBy { ProjectTypeDescription.anchorStopped(emptyList(), null, AnchorStop.ROWTYPE) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { ProjectTypeDescription.unresolvedValue(emptyList(), missing, null) }.isInstanceOf(IllegalArgumentException::class.java)
        // A stop at the request needs empty steps and a matching anchor kind and name.
        assertThatThrownBy { ProjectTypeDescription.anchorStopped(listOf(someView), null, AnchorStop.ENCLOSING_SCOPE, typeRequest) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { ProjectTypeDescription.anchorStopped(emptyList(), null, AnchorStop.ROWTYPE, typeRequest) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { ProjectTypeDescription.anchorStopped(emptyList(), null, AnchorStop.FIELD_PATH, row("p", "nope")) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { ProjectTypeDescription.unresolvedValue(emptyList(), missing, null, row("p", "nope")) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { ProjectTypeDescription.unresolvedValue(emptyList(), missing, null, type("p", "other")) }
            .isInstanceOf(IllegalArgumentException::class.java)
        // The valid shapes are accepted.
        assertThat(ProjectTypeDescription.unresolvedValue(emptyList(), missing, null, typeRequest).writtenAnchor).contains(typeRequest)
        assertThat(ProjectTypeDescription.anchorStopped(emptyList(), null, AnchorStop.ROWTYPE, row("employees")).status).isEqualTo(Status.ANCHORED_REFERENCE)
    }
}
