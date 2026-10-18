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
import com.felipebz.zpa.api.project.ProjectExternalTypeDescription.Status
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.project.ProjectDatatypeFamily as Family
import com.felipebz.zpa.api.project.ProjectTypeDescription.AnchorStop

/**
 * Anchors behind a database link through the public API: their link is visible, they correlate headings by link, and
 * they are never read against project sources or the local metadata.
 */
@OptIn(ZpaExperimentalApi::class)
class ProjectRemoteAnchorTest {
    private val hr = ProjectIdentifier.fromSource("HR")

    /** A local `T` whose column `C` is a NUMBER: a remote `t.c@l%TYPE` must never bind to it. */
    private val md = ProjectExternalMetadata.builder("fixture").completeSchema("HR").relation(
        ProjectExternalRelation.of("HR", "T", true, listOf(ProjectExternalColumn.of("C", 1, ProjectExternalDatatype.of("NUMBER", null, null, null, 10, 0))))
    ).build()

    private fun prepare(vararg files: Pair<String, String>) = ProjectSnapshots.prepare(files.map { (id, text) -> ProjectSourceInput.ofText(id, text) }, false)
    private fun n(vararg s: String) = ProjectQualifiedName.of(*s)
    private fun value(s: ProjectSnapshot, vararg name: String) = s.findDeclarations(n(*name)).declarations.single() as ProjectPackageValueView

    @Test
    fun theLinkIsPartOfThePublicReference() {
        val s = prepare("p.sql" to "CREATE PACKAGE p AS x t.c@internet.intranet%TYPE; y t.c%TYPE; END p;")
        val remote = value(s, "p", "x").typeRef
        assertThat(remote.kind).isEqualTo(ProjectTypeReference.Kind.ANCHORED)
        assertThat(remote.name).isEqualTo(n("t", "c"))
        assertThat(remote.link).contains(n("internet", "intranet"))
        assertThat(remote.toString()).isEqualTo("ANCHORED t.c@internet.intranet")
        assertThat(value(s, "p", "y").typeRef.link).isEmpty
    }

    @Test
    fun aRemoteAnchorStopsWithoutAnyLookup() {
        val s = prepare(
            "p.sql" to "CREATE PACKAGE p AS x t.c@l%TYPE; y t.c%TYPE; r t@l%ROWTYPE; v x%TYPE; END p;",
            // A same-named project value must not be reached either.
            "t.sql" to "CREATE PACKAGE t AS c NUMBER; END t;"
        )
        val remote = s.describeExternalType(value(s, "p", "x").typeRef, n("p"), md, hr)
        assertThat(remote.source.status).isEqualTo(ProjectTypeDescription.Status.ANCHORED_REFERENCE)
        assertThat(remote.source.anchorStop).contains(AnchorStop.REMOTE)
        assertThat(remote.status).isEqualTo(Status.REMOTE)
        assertThat(remote.lookup).isEmpty
        assertThat(remote.relationName).isEmpty
        assertThat(remote.anchor).contains(ProjectTypeReference.Anchor.TYPE)
        assertThat(remote.anchorName).contains(n("t", "c"))
        assertThat(s.describeType(value(s, "p", "x").typeRef, n("p")).anchorStop).contains(AnchorStop.REMOTE)

        // Without the link the same name is read as before: here the project value `t.c` wins.
        val local = s.describeExternalType(value(s, "p", "y").typeRef, n("p"), md, hr)
        assertThat(local.status).isNotEqualTo(Status.REMOTE)
        assertThat(local.source.anchorStop.orElse(null)).isNotEqualTo(AnchorStop.REMOTE)

        // A remote %ROWTYPE stops as REMOTE too, before the %ROWTYPE stop.
        val row = s.describeExternalType(value(s, "p", "r").typeRef, n("p"), md, hr)
        assertThat(row.status).isEqualTo(Status.REMOTE)
        assertThat(row.anchor).contains(ProjectTypeReference.Anchor.ROWTYPE)

        // Following a package value whose own type is remote ends at that remote anchor.
        val followed = s.describeExternalType(value(s, "p", "v").typeRef, n("p"), md, hr)
        assertThat(followed.source.anchorStop).contains(AnchorStop.REMOTE)
        assertThat(followed.source.values.map { it.qualifiedName.toString() }).containsExactly("p.x")
        assertThat(followed.status).isEqualTo(Status.REMOTE)
    }

    @Test
    fun theLocalReadingOfTheSameNameStillResolves() {
        val s = prepare("p.sql" to "CREATE PACKAGE p AS x t.c@l%TYPE; y t.c%TYPE; END p;")
        assertThat(s.describeExternalType(value(s, "p", "y").typeRef, n("p"), md, hr).status).isEqualTo(Status.RESOLVED)
        assertThat(s.describeExternalType(value(s, "p", "x").typeRef, n("p"), md, hr).status).isEqualTo(Status.REMOTE)
    }

    @Test
    fun narrowingNeverUsesARemoteFormal() {
        fun binding(formal: String): ProjectBindingCompatibility {
            val s = prepare("p.sql" to "CREATE PACKAGE p AS PROCEDURE pick(x $formal); PROCEDURE pick(x DATE); END p;")
            val structural = s.resolveCall(
                ProjectCallTarget.packageMember(n("p"), ProjectIdentifier.fromSource("pick")),
                ProjectCallKind.PROCEDURE, ProjectCallArguments.of(listOf(ProjectCallArgument.positional()))
            )
            val typed = s.narrowCall(structural, listOf(ProjectArgumentType.builtIn("NUMBER")), md, hr)
            return typed.evaluations.first { it.candidate.declaration.parameters.single().typeRef.name == n("t", "c") }.bindings.single()
        }
        assertThat(binding("t.c%TYPE").formalFamily).contains(Family.NUMERIC)
        assertThat(binding("t.c@l%TYPE").formalFamily).isEmpty
        assertThat(binding("t.c@l%ROWTYPE").formalFamily).contains(Family.COMPOSITE)
    }

    @Test
    fun headingsCorrelateOnlyWithTheSameLink() {
        fun status(specType: String, bodyType: String): ProjectSpecificationCorrelation.Status {
            val s = prepare(
                "s.sql" to "CREATE PACKAGE p AS FUNCTION f(i $specType) RETURN $specType; END p;",
                "b.sql" to "CREATE PACKAGE BODY p AS FUNCTION f(i $bodyType) RETURN $bodyType IS BEGIN RETURN NULL; END; END p;"
            )
            val body = s.declarations.filterIsInstance<ProjectPackageSubprogramView>().single { it.role == ProjectDeclarationRole.BODY }
            return s.correlateSpecification(body.id).status
        }
        assertThat(status("t.c@lnk%TYPE", "T.C@LNK%type")).isEqualTo(ProjectSpecificationCorrelation.Status.RESOLVED)
        assertThat(status("t.c@lnk%TYPE", "t.c@\"LNK\"%TYPE")).isEqualTo(ProjectSpecificationCorrelation.Status.RESOLVED)
        assertThat(status("t.c@a%TYPE", "t.c@b%TYPE")).isNotEqualTo(ProjectSpecificationCorrelation.Status.RESOLVED)
        assertThat(status("t.c@a%TYPE", "t.c%TYPE")).isNotEqualTo(ProjectSpecificationCorrelation.Status.RESOLVED)
    }

    @Test
    fun differentLinksAreDifferentOverloads() {
        val s = prepare("s.sql" to "CREATE PACKAGE p AS PROCEDURE q(i t.c@a%TYPE); PROCEDURE q(i t.c@b%TYPE); PROCEDURE q(i t.c%TYPE); END p;")
        val overloads = s.findPackageSubprograms(n("p"), ProjectIdentifier.fromSource("q")).declarations
        assertThat(overloads).hasSize(3)
        assertThat(overloads.map { (it as ProjectPackageSubprogramView).parameters.single().typeRef.link.map(Any::toString).orElse("") })
            .containsExactly("a", "b", "")
    }
}
