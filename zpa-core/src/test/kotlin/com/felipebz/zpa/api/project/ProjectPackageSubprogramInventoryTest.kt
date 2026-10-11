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
import com.felipebz.zpa.project.DeclarationRole
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.PackageProcedureDeclaration
import com.felipebz.zpa.project.PackageSubprogramDeclaration
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparationResult
import com.felipebz.zpa.project.ProjectSubprogramSpecificationResolver
import com.felipebz.zpa.project.ProjectSymbolIndex
import com.felipebz.zpa.project.ProjectSymbolIndexBuilder
import com.felipebz.zpa.project.QualifiedName
import com.felipebz.zpa.project.SourceRange
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

@OptIn(ZpaExperimentalApi::class)
class ProjectPackageSubprogramInventoryTest {
    private fun prepare(vararg files: Pair<String, String>, failing: Boolean = false): ProjectSnapshot =
        ProjectSnapshots.prepare(
            files.map { (id, text) -> ProjectSourceInput.ofText(id, text) } +
                if (failing) listOf(ProjectSourceInput.ofReader("zz.sql", ProjectSourceReader { throw IllegalStateException("x") }))
                else emptyList(),
            false
        )

    private fun pkg(vararg s: String) = ProjectQualifiedName.of(*s)
    private fun id(s: String) = ProjectIdentifier.fromSource(s)

    private val spec = "CREATE PACKAGE p AS PROCEDURE run(a NUMBER); PROCEDURE run(a VARCHAR2, b NUMBER DEFAULT 1); " +
        "FUNCTION run RETURN NUMBER; PROCEDURE \"Run\"; PROCEDURE other; END p;"
    private val body = "CREATE PACKAGE BODY p AS PROCEDURE run(a NUMBER) IS BEGIN NULL; END; " +
        "PROCEDURE hidden IS BEGIN NULL; END; END p;"

    @Test
    fun returnsEveryOverloadKindAndRoleWithoutPairingOrRanking() {
        val s = prepare("s.sql" to spec, "b.sql" to body)
        val inv = s.findPackageSubprograms(pkg("p"), id("run"))
        assertThat(inv.status).isEqualTo(ProjectInventory.Status.COMPLETE)
        // Index order: b.sql before s.sql; body and its specification are both kept.
        assertThat(inv.declarations.map { it.sourceRange.fileId to it.role }).containsExactly(
            "b.sql" to ProjectDeclarationRole.BODY,
            "s.sql" to ProjectDeclarationRole.SPECIFICATION,
            "s.sql" to ProjectDeclarationRole.SPECIFICATION,
            "s.sql" to ProjectDeclarationRole.SPECIFICATION
        )
        assertThat(inv.declarations.map { it.kind }).containsExactly(
            ProjectDeclarationKind.PACKAGE_PROCEDURE, ProjectDeclarationKind.PACKAGE_PROCEDURE,
            ProjectDeclarationKind.PACKAGE_PROCEDURE, ProjectDeclarationKind.PACKAGE_FUNCTION
        )
        assertThat(inv.declarations.map { it.parameters.size }).containsExactly(1, 1, 2, 0)
        assertThat(inv.declarations.map { it.packageOwner }).containsOnly(pkg("p"))
        val fn = inv.declarations.last()
        assertThat(fn).isInstanceOf(ProjectFunctionView::class.java)
        assertThat((fn as ProjectFunctionView).returnType.name.last.originalSpelling).isEqualTo("NUMBER")
        assertThat(inv.declarations.map { it.id }).doesNotHaveDuplicates()
    }

    @Test
    fun bodyOnlyUnknownOwnersQuotingAndNoPackageContainerRequired() {
        val s = prepare("b.sql" to body, "s.sql" to spec)
        val hidden = s.findPackageSubprograms(pkg("p"), id("hidden"))
        assertThat(hidden.status).isEqualTo(ProjectInventory.Status.COMPLETE)
        assertThat(hidden.declarations.single().role).isEqualTo(ProjectDeclarationRole.BODY)

        val bodyOnly = prepare("b.sql" to "CREATE PACKAGE BODY nopkg AS PROCEDURE x IS BEGIN NULL; END; END nopkg;")
        assertThat(bodyOnly.findPackageSubprograms(pkg("nopkg"), id("x")).declarations).hasSize(1)

        assertThat(s.findPackageSubprograms(pkg("p"), id("nope")).declarations).isEmpty()
        assertThat(s.findPackageSubprograms(pkg("q"), id("run")).declarations).isEmpty()
        assertThat(s.findPackageSubprograms(pkg("x", "p"), id("run")).declarations).isEmpty()
        assertThat(s.findPackageSubprograms(pkg("p"), id("\"RUN\"")).declarations).isEmpty()
        assertThat(s.findPackageSubprograms(pkg("p"), id("\"Run\"")).declarations).hasSize(1)
        assertThat(s.findPackageSubprograms(pkg("P"), id("RUN")).declarations).hasSize(4)
    }

    @Test
    fun qualifiedOwnerMatchesExactly() {
        val s = prepare("a.sql" to "CREATE PACKAGE a.p AS PROCEDURE f; END p;", "b.sql" to "CREATE PACKAGE b.p AS PROCEDURE f; END p;")
        assertThat(s.findPackageSubprograms(pkg("a", "p"), id("f")).declarations.single().sourceRange.fileId).isEqualTo("a.sql")
        assertThat(s.findPackageSubprograms(pkg("p"), id("f")).declarations).isEmpty()
    }

    @Test
    fun duplicatesKeepDistinctIdsAndAgreeWithOtherInventories() {
        val s = prepare("a.sql" to spec, "b.sql" to spec)
        val inv = s.findPackageSubprograms(pkg("p"), id("other"))
        assertThat(inv.declarations).hasSize(2)
        assertThat(inv.declarations.map { it.id }).doesNotHaveDuplicates()
        assertThat(inv.declarations.map { it.id })
            .isEqualTo(s.findDeclarations(pkg("p", "other")).declarations.map { it.id })
        assertThat(s.declarations.map { it.id }).containsAll(inv.declarations.map { it.id })
    }

    @Test
    fun incompleteIndexKeepsKnownCandidatesAndNeverBecomesComplete() {
        val s = prepare("s.sql" to spec, failing = true)
        val known = s.findPackageSubprograms(pkg("p"), id("run"))
        assertThat(known.status).isEqualTo(ProjectInventory.Status.INCOMPLETE_INDEX)
        assertThat(known.declarations).hasSize(3)
        assertThat(known.failures.single().fileId).isEqualTo("zz.sql")
        val empty = s.findPackageSubprograms(pkg("p"), id("none"))
        assertThat(empty.status).isEqualTo(ProjectInventory.Status.INCOMPLETE_INDEX)
        assertThat(empty.declarations).isEmpty()
        @Suppress("UNCHECKED_CAST")
        assertThatThrownBy { (known.declarations as MutableList<ProjectPackageSubprogramView>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)
        @Suppress("UNCHECKED_CAST")
        assertThatThrownBy { (known.failures as MutableList<ProjectPreparationFailure>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun sameFactReferenceInsertedTwiceYieldsTwoOccurrencesAndNotPreparedIsInternal() {
        val file = FileId("r.sql")
        val owner = QualifiedName(listOf(OracleIdentifier.fromSource("p")))
        val fact = PackageProcedureDeclaration(owner, OracleIdentifier.fromSource("run"), emptyList(), file,
            SourceRange(file, 1, 0, 1, 5))
        val builder = ProjectSymbolIndexBuilder()
        builder.add(file, listOf(fact, fact))
        val index = builder.build()
        val result = ProjectIndexPreparationResult(index, 1, emptyList())
        val snapshot = ProjectSnapshot.create(result, ProjectPreparationState.PREPARED_WITH_DECLARATIONS, emptyList(), listOf("r.sql"))
        val inv = snapshot.findPackageSubprograms(pkg("p"), id("run"))
        assertThat(inv.declarations.map { it.id.ordinal }).containsExactly(0, 1)
        // Existing internal lookup is unchanged: same facts, same order.
        assertThat(index.findSubprograms(owner, OracleIdentifier.fromSource("run"))).containsExactly(fact, fact)

        val empty = ProjectIndexPreparationResult(ProjectSymbolIndex.empty(), 0, emptyList())
        val unprepared = ProjectSnapshot.create(empty, ProjectPreparationState.NOT_PREPARED, emptyList(), emptyList())
        assertThat(unprepared.findPackageSubprograms(pkg("p"), id("run")).status).isEqualTo(ProjectInventory.Status.NOT_PREPARED)
    }

    @Test
    fun publicQueryMatchesInternalLookupOrderAndLeavesHeadingCorrelationUntouched() {
        val s = prepare("s.sql" to spec, "b.sql" to body)
        val result = ProjectSnapshot::class.java.getDeclaredField("preparationResult")
            .apply { isAccessible = true }.get(s) as ProjectIndexPreparationResult
        val owner = QualifiedName(listOf(OracleIdentifier.fromSource("p")))
        val internal = result.index.findSubprograms(owner, OracleIdentifier.fromSource("run"))
        val resolver = ProjectSubprogramSpecificationResolver(ProjectAnalysisContext.prepared(result))
        val bodies = internal.filter { it.role == DeclarationRole.BODY }
        val before = bodies.map { it.headerIdentity() to resolver.resolve(it)::class }

        val inv = s.findPackageSubprograms(pkg("p"), id("run"))
        inv.declarations.forEach { v -> v.parameters.forEach { it.typeRef }; (v as? ProjectFunctionView)?.returnType }

        assertThat(inv.declarations.map { it.sourceRange.fileId to it.sourceRange.startColumn })
            .isEqualTo(internal.map { it.sourceRange.fileId.value to it.sourceRange.startColumn })
        assertThat(bodies.map { it.headerIdentity() to resolver.resolve(it)::class }).isEqualTo(before)
        assertThat(internal).allMatch { it is PackageSubprogramDeclaration }
    }
}
