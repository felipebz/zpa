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
package com.felipebz.zpa.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectDeclarationRole
import com.felipebz.zpa.api.project.ProjectFunctionView
import com.felipebz.zpa.api.project.ProjectIdentifier
import com.felipebz.zpa.api.project.ProjectInventory
import com.felipebz.zpa.api.project.ProjectPreparationState
import com.felipebz.zpa.api.project.ProjectQualifiedName
import com.felipebz.zpa.api.project.ProjectSnapshot
import com.felipebz.zpa.api.project.ProjectSpecificationCorrelation
import com.felipebz.zpa.api.project.ProjectSubprogramView
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.project.ProjectDeclarationKind as PublicKind

/** Increment 1 of S2: standalone callable facts built by hand (no extraction yet). */
@OptIn(ZpaExperimentalApi::class)
class StandaloneSubprogramDeclarationTest {
    private val extractor = ProjectDeclarationExtractor()

    private fun id(s: String) = OracleIdentifier.fromSource(s)
    private fun name(vararg s: String) = QualifiedName(s.map(::id))
    private fun range(file: FileId, line: Int, col: Int = 0) = SourceRange(file, line, col, line, col + 10)
    private fun param(n: String, ordinal: Int, file: FileId, type: String = "NUMBER") =
        ProjectParameter(id(n), ordinal, ParameterMode.IN, false, NamedTypeRef(name(type), range(file, 1)), false, range(file, 1))
    private fun procedure(file: FileId, line: Int, vararg segments: String, params: List<ProjectParameter> = emptyList()) =
        StandaloneProcedureDeclaration(name(*segments), params, file, range(file, line))
    private fun function(file: FileId, line: Int, vararg segments: String, deterministic: Boolean = false) =
        StandaloneFunctionDeclaration(name(*segments), emptyList(), NamedTypeRef(name("NUMBER"), range(file, line)),
            file, range(file, line), deterministic)

    private fun index(vararg files: Pair<FileId, List<ProjectDeclaration>>) =
        ProjectSymbolIndexBuilder().also { b -> files.forEach { (f, d) -> b.add(f, d) } }.build()

    @Test
    fun factsHaveStandaloneKindsRolesAndNamesAsWritten() {
        val file = FileId("s.sql")
        val p = procedure(file, 1, "run")
        val f = function(file, 2, "app", "\"Calc\"")
        assertThat(p.kind).isEqualTo(ProjectDeclarationKind.STANDALONE_PROCEDURE)
        assertThat(f.kind).isEqualTo(ProjectDeclarationKind.STANDALONE_FUNCTION)
        assertThat(listOf(p.role, f.role)).containsOnly(DeclarationRole.STANDALONE)
        assertThat(p.qualifiedName()).isEqualTo(name("RUN"))
        assertThat(f.qualifiedName()).isEqualTo(name("APP", "\"Calc\""))
        assertThat(f.qualifiedName()).isNotEqualTo(name("app", "calc"))
        assertThat(f.qualifiedName().segments).hasSize(2)
        assertThat(p).isNotInstanceOf(PackageSubprogramDeclaration::class.java)
        assertThat(f).isNotInstanceOf(PackageSubprogramDeclaration::class.java)
    }

    @Test
    fun parametersAreDefensivelyCopiedAndUnmodifiable() {
        val file = FileId("s.sql")
        val source = mutableListOf(param("a", 1, file))
        val p = procedure(file, 1, "run", params = source)
        source += param("b", 2, file)
        assertThat(p.parameters.map { it.name.originalSpelling }).containsExactly("a")
        @Suppress("UNCHECKED_CAST")
        assertThatThrownBy { (p.parameters as MutableList<ProjectParameter>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun equalityFollowsFactValueConventions() {
        val file = FileId("s.sql")
        assertThat(procedure(file, 1, "run")).isEqualTo(procedure(file, 1, "RUN"))
        assertThat(procedure(file, 1, "run").hashCode()).isEqualTo(procedure(file, 1, "RUN").hashCode())
        assertThat(procedure(file, 1, "run")).isNotEqualTo(procedure(file, 2, "run"))
        assertThat(procedure(file, 1, "run", params = listOf(param("a", 1, file))))
            .isNotEqualTo(procedure(file, 1, "run"))
        assertThat(function(file, 1, "f")).isEqualTo(function(file, 1, "f"))
        assertThat(function(file, 1, "f")).isNotEqualTo(function(file, 1, "f", deterministic = true))
        assertThat(function(file, 1, "f")).isNotEqualTo(procedure(file, 1, "f"))
    }

    @Test
    fun indexOrdersDeduplicatesNothingAndKeepsRepeatedReferences() {
        val a = FileId("a.sql")
        val b = FileId("b.sql")
        val shared = function(b, 1, "f")
        val idx = index(
            b to listOf(shared, procedure(b, 3, "p"), shared),
            a to listOf(procedure(a, 5, "p"), procedure(a, 1, "p"), procedure(a, 1, "p"))
        )
        assertThat(idx.fileIds).containsExactly(a, b)
        assertThat(idx.occurrencesFor(a).map { it.ordinal to it.declaration.sourceRange.startLine })
            .containsExactly(0 to 1, 1 to 1, 2 to 5)
        val bOccurrences = idx.occurrencesFor(b)
        assertThat(bOccurrences.map { it.ordinal }).containsExactly(0, 1, 2)
        assertThat(bOccurrences.filter { it.declaration === shared }).hasSize(2)
        assertThat(idx.findDeclarations(name("p"))).hasSize(4)
        assertThat(idx.findDeclarations(name("f"))).containsExactly(shared, shared)
        // Independent of addition order.
        val reversed = index(a to listOf(procedure(a, 1, "p"), procedure(a, 1, "p"), procedure(a, 5, "p")),
            b to listOf(shared, shared, procedure(b, 3, "p")))
        assertThat(reversed.declarations).containsExactlyElementsOf(idx.declarations)
    }

    @Test
    fun standaloneAndPackageMemberWithTheSameTextualNameBothRemain() {
        val pkgFile = FileId("pkg.sql")
        val pkgFacts = extractor.extract(pkgFile, "CREATE PACKAGE app AS FUNCTION f RETURN NUMBER; END app;")
        val sFile = FileId("s.sql")
        val standalone = function(sFile, 1, "app", "f")
        val idx = index(pkgFile to pkgFacts, sFile to listOf(standalone))
        val both = idx.findDeclarations(name("app", "f"))
        assertThat(both.map { it.kind }).containsExactly(ProjectDeclarationKind.PACKAGE_FUNCTION, ProjectDeclarationKind.STANDALONE_FUNCTION)
        // Package-only lookups never see standalone facts.
        assertThat(idx.findSubprograms(name("app"), id("f"))).hasSize(1).allMatch { it is PackageFunctionDeclaration }
        assertThat(idx.findTypes(name("app"), id("f"))).isEmpty()
    }

    @Test
    fun snapshotProjectsGenericKindsAndKeepsPackageQueriesAndCorrelationUnchanged() {
        val specFile = FileId("spec.sql")
        val bodyFile = FileId("body.sql")
        val sFile = FileId("s.sql")
        val facts = extractor.extract(specFile, "CREATE PACKAGE app AS PROCEDURE p; FUNCTION f RETURN NUMBER; END app;")
        val bodyFacts = extractor.extract(bodyFile, "CREATE PACKAGE BODY app AS PROCEDURE p IS BEGIN NULL; END; END app;")
        val standalone = listOf(procedure(sFile, 1, "app", "p"), function(sFile, 2, "app", "f"))
        val result = ProjectIndexPreparationResult(index(specFile to facts, bodyFile to bodyFacts, sFile to standalone), 3, emptyList())
        val snapshot = ProjectSnapshot.create(ProjectAnalysisContext.prepared(result))

        assertThat(snapshot.preparationState).isEqualTo(ProjectPreparationState.PREPARED_WITH_DECLARATIONS)
        val inventory = snapshot.findDeclarations(ProjectQualifiedName.of("app", "f"))
        assertThat(inventory.status).isEqualTo(ProjectInventory.Status.COMPLETE)
        assertThat(inventory.declarations.map { it.kind }).containsExactly(PublicKind.STANDALONE_FUNCTION, PublicKind.PACKAGE_FUNCTION) // file order: s.sql < spec.sql
        val view = inventory.declarations.first()
        assertThat(view.role).isEqualTo(ProjectDeclarationRole.STANDALONE)
        assertThat(view.qualifiedName).isEqualTo(ProjectQualifiedName.of("app", "f"))
        // Increment 1 keeps the generic view: no callable facets yet.
        assertThat(view).isNotInstanceOf(ProjectSubprogramView::class.java).isNotInstanceOf(ProjectFunctionView::class.java)
        assertThat(snapshot.declarationsFor("s.sql").declarations.map { it.kind })
            .containsExactly(PublicKind.STANDALONE_PROCEDURE, PublicKind.STANDALONE_FUNCTION)
        assertThat(snapshot.declarations.map { it.id }).contains(view.id)

        val members = snapshot.findPackageSubprograms(ProjectQualifiedName.of("app"), ProjectIdentifier.fromSource("p"))
        assertThat(members.declarations.map { it.role }).containsExactly(ProjectDeclarationRole.BODY, ProjectDeclarationRole.SPECIFICATION)
        val correlation = snapshot.correlateSpecification(members.declarations.first().id)
        assertThat(correlation.status).isEqualTo(ProjectSpecificationCorrelation.Status.RESOLVED)
        val standaloneId = snapshot.declarationsFor("s.sql").declarations.first().id
        assertThatThrownBy { snapshot.correlateSpecification(standaloneId) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun existingKindOrdinalsAreUnchanged() {
        val original = listOf(
            "PACKAGE", "PACKAGE_BODY", "STANDALONE_TYPE", "PACKAGE_TYPE", "PACKAGE_SUBTYPE",
            "PACKAGE_PROCEDURE", "PACKAGE_FUNCTION", "SEQUENCE"
        )
        original.forEachIndexed { ordinal, name ->
            assertThat(ProjectDeclarationKind.valueOf(name).ordinal).isEqualTo(ordinal)
            assertThat(PublicKind.valueOf(name).ordinal).isEqualTo(ordinal)
        }
        assertThat(ProjectDeclarationKind.STANDALONE_PROCEDURE.ordinal).isEqualTo(8)
        assertThat(ProjectDeclarationKind.STANDALONE_FUNCTION.ordinal).isEqualTo(9)
        listOf("STANDALONE_PROCEDURE", "STANDALONE_FUNCTION").forEach {
            assertThat(PublicKind.valueOf(it).ordinal).isEqualTo(ProjectDeclarationKind.valueOf(it).ordinal)
        }
    }
}
