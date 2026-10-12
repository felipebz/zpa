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

/** A package-body subprogram can be captured by a private declaration that no fact represents. */
@OptIn(ZpaExperimentalApi::class)
class ProjectBodyAnchorScopeTest {
    private val sources = listOf(
        "p.sql" to "CREATE PACKAGE p AS g NUMBER; SUBTYPE s IS NUMBER; END p;\n/\n",
        "chain.sql" to "CREATE PACKAGE ch AS a p.g%TYPE; b a%TYPE; END ch;\n/\n",
        "bc.sql" to """
            CREATE PACKAGE bc AS PROCEDURE w(x OUT p.g%TYPE); END bc;
            /
            CREATE PACKAGE BODY bc AS
              TYPE r IS RECORD (g DATE);
              p r;
              PROCEDURE w(x OUT p.g%TYPE) IS BEGIN x := SYSDATE; END;
            END bc;
            /
        """.trimIndent(),
        "clean.sql" to """
            CREATE PACKAGE cl AS PROCEDURE w(x p.g%TYPE, y g%TYPE, z p.s, r t%ROWTYPE, q REF p.s); g NUMBER; END cl;
            /
            CREATE PACKAGE BODY cl AS
              PROCEDURE w(x p.g%TYPE, y g%TYPE, z p.s, r t%ROWTYPE, q REF p.s) IS BEGIN NULL; END;
              FUNCTION f(g NUMBER, k p.g%TYPE) RETURN p.g%TYPE IS BEGIN RETURN 1; END;
            END cl;
            /
            CREATE PROCEDURE sp(x p.g%TYPE) IS BEGIN NULL; END;
            /
        """.trimIndent()
    )

    private fun n(vararg s: String) = ProjectQualifiedName.of(*s)
    private fun prepare(concurrent: Boolean = false, list: List<Pair<String, String>> = sources, vararg more: ProjectSourceInput) =
        ProjectSnapshots.prepare(list.map { (id, text) -> ProjectSourceInput.ofText(id, text) } + more, concurrent)

    private fun sub(s: ProjectSnapshot, pkg: String, name: String, role: ProjectDeclarationRole) =
        s.findPackageSubprograms(n(pkg), ProjectIdentifier.fromSource(name)).declarations.single { it.role == role }

    private fun assertScopeStop(d: ProjectTypeDescription, reference: ProjectTypeReference) {
        assertThat(d.status).isEqualTo(Status.ANCHORED_REFERENCE)
        assertThat(d.anchorStop).isEqualTo(Optional.of(AnchorStop.ENCLOSING_SCOPE))
        assertThat(d.valueResolution).isEmpty
        assertThat(d.terminalReference).isPresent
        assertThat(d.terminalReference.get().kind).isEqualTo(ProjectTypeReference.Kind.ANCHORED)
        assertThat(d.terminalReference.get().sourceRange).isEqualTo(reference.sourceRange)
        assertThat(d.terminalReference.get().name).isEqualTo(reference.name)
        assertThatThrownBy { (d.steps as MutableList<ProjectDeclarationView>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun privateDeclarationShadowingAQualifiedAnchorIsNotResolvedThroughTheExternalPackage() {
        val s = prepare()
        val body = sub(s, "bc", "w", ProjectDeclarationRole.BODY)
        assertScopeStop(s.describeType(body.parameters[0].typeRef, n("bc")), body.parameters[0].typeRef)
        // the specification of the same subprogram has no private scope
        val spec = sub(s, "bc", "w", ProjectDeclarationRole.SPECIFICATION)
        val d = s.describeType(spec.parameters[0].typeRef, n("bc"))
        assertThat(d.status).isEqualTo(Status.BUILT_IN)
        assertThat(d.builtInCategory).isEqualTo(Optional.of(ProjectBuiltInTypeCategory.NUMERIC))
    }

    @Test
    fun absenceOfPrivateDeclarationsCannotBeEstablished() {
        val s = prepare()
        val body = sub(s, "cl", "w", ProjectDeclarationRole.BODY)
        assertScopeStop(s.describeType(body.parameters[0].typeRef, n("cl")), body.parameters[0].typeRef) // qualified
        assertScopeStop(s.describeType(body.parameters[1].typeRef, n("cl")), body.parameters[1].typeRef) // unqualified
        val f = sub(s, "cl", "f", ProjectDeclarationRole.BODY) as ProjectFunctionView
        assertScopeStop(s.describeType(f.parameters[1].typeRef, n("cl")), f.parameters[1].typeRef) // qualified
        assertScopeStop(s.describeType(f.returnType, n("cl")), f.returnType)
    }

    @Test
    fun specificationsAndStandaloneSubprogramsKeepResolving() {
        val s = prepare()
        val spec = sub(s, "cl", "w", ProjectDeclarationRole.SPECIFICATION)
        assertThat(s.describeType(spec.parameters[0].typeRef, n("cl")).values.map { it.qualifiedName }).containsExactly(n("p", "g"))
        // unqualified g is declared later in the same specification: the specification rules are unchanged
        assertThat(s.describeType(spec.parameters[1].typeRef, n("cl")).anchorStop).isEqualTo(Optional.of(AnchorStop.DECLARED_LATER))
    }

    @Test
    fun standaloneQualifiedAnchorStillResolves() {
        val s = prepare()
        val sp = s.findDeclarations(n("sp")).declarations.single() as ProjectSubprogramView
        assertThat(s.describeType(sp.parameters[0].typeRef, null).values.map { it.qualifiedName }).containsExactly(n("p", "g"))
    }

    @Test
    fun rowtypeAndRefKeepTheirOwnStopsInBodies() {
        val s = prepare()
        val body = sub(s, "cl", "w", ProjectDeclarationRole.BODY)
        val row = s.describeType(body.parameters[3].typeRef, n("cl"))
        assertThat(row.anchorStop).isEqualTo(Optional.of(AnchorStop.ROWTYPE))
        assertThat(s.describeType(body.parameters[4].typeRef, n("cl")).status).isEqualTo(Status.REF_REFERENCE)
        // a named type is unchanged
        assertThat(s.describeType(body.parameters[2].typeRef, n("cl")).status).isEqualTo(Status.BUILT_IN)
    }

    @Test
    fun incompleteProjectsAreNotLookupsAndOrdinaryChainsAreUnchanged() {
        val chain = listOf("p.sql" to sources[0].second, "b.sql" to """
            CREATE PACKAGE bs AS SUBTYPE t IS NUMBER; v t; PROCEDURE w(x v%TYPE); END bs;
            /
            CREATE PACKAGE BODY bs AS PROCEDURE w(x v%TYPE) IS BEGIN NULL; END; END bs;
            /
        """.trimIndent())
        for (concurrent in listOf(false, true)) {
            val s = prepare(concurrent, chain)
            val body = sub(s, "bs", "w", ProjectDeclarationRole.BODY)
            assertScopeStop(s.describeType(body.parameters[0].typeRef, n("bs")), body.parameters[0].typeRef)
            val failed = prepare(concurrent, chain, ProjectSourceInput.ofReader("bad.sql") { throw java.io.IOException("x") })
            val b2 = sub(failed, "bs", "w", ProjectDeclarationRole.BODY)
            assertScopeStop(failed.describeType(b2.parameters[0].typeRef, n("bs")), b2.parameters[0].typeRef)
        }
        // ordinary chain traversal outside body subprograms is unchanged
        val s = prepare()
        val a = s.findDeclarations(n("ch", "b")).declarations.single() as ProjectPackageValueView
        val d = s.describeType(a.typeRef, a.owner)
        assertThat(d.steps.map { it.qualifiedName }).containsExactly(n("ch", "a"), n("p", "g"))
    }
}
