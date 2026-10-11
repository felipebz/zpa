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
import com.felipebz.zpa.api.project.ProjectSpecificationCorrelation.Status
import com.felipebz.zpa.project.DeclarationRole
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.PackageProcedureDeclaration
import com.felipebz.zpa.project.ProjectIndexPreparationResult
import com.felipebz.zpa.project.ProjectSymbolIndexBuilder
import com.felipebz.zpa.project.QualifiedName
import com.felipebz.zpa.project.SourceRange
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

@OptIn(ZpaExperimentalApi::class)
class ProjectSpecificationCorrelationTest {
    private fun prepare(vararg files: Pair<String, String>, failing: Boolean = false): ProjectSnapshot =
        ProjectSnapshots.prepare(
            files.map { (id, text) -> ProjectSourceInput.ofText(id, text) } +
                if (failing) listOf(ProjectSourceInput.ofReader("zz.sql", ProjectSourceReader { throw IllegalStateException("x") }))
                else emptyList(),
            false
        )

    private fun pkgSpec(vararg members: String) = "CREATE PACKAGE p AS ${members.joinToString(" ")} END p;"
    private fun pkgBody(vararg members: String) = "CREATE PACKAGE BODY p AS ${members.joinToString(" ")} END p;"

    private fun ProjectSnapshot.bodies(): List<ProjectPackageSubprogramView> = declarations
        .filterIsInstance<ProjectPackageSubprogramView>().filter { it.role == ProjectDeclarationRole.BODY }

    private fun statusOf(specHeader: String, bodyHeader: String): Status {
        val s = prepare("s.sql" to pkgSpec("$specHeader;"), "b.sql" to pkgBody("$bodyHeader IS BEGIN NULL; END;"))
        return s.correlateSpecification(s.bodies().single().id).status
    }

    private fun fnStatus(specHeader: String, bodyHeader: String): Status {
        val s = prepare("s.sql" to pkgSpec("$specHeader;"), "b.sql" to pkgBody("$bodyHeader IS BEGIN RETURN 1; END;"))
        return s.correlateSpecification(s.bodies().single().id).status
    }

    @Test
    fun procedureAndFunctionBodiesResolveToTheirSpecifications() {
        val s = prepare(
            "s.sql" to pkgSpec("PROCEDURE pr(a IN NUMBER);", "FUNCTION fn(x NUMBER) RETURN VARCHAR2;"),
            "b.sql" to pkgBody("PROCEDURE pr(a IN NUMBER) IS BEGIN NULL; END;",
                "FUNCTION fn(x NUMBER) RETURN VARCHAR2 IS BEGIN RETURN NULL; END;")
        )
        s.bodies().forEach { body ->
            val c = s.correlateSpecification(body.id)
            assertThat(c.status).isEqualTo(Status.RESOLVED)
            assertThat(c.body.id).isEqualTo(body.id)
            assertThat(c.specification.get().role).isEqualTo(ProjectDeclarationRole.SPECIFICATION)
            assertThat(c.specification.get().kind).isEqualTo(body.kind)
            assertThat(c.candidates.single().id).isEqualTo(c.specification.get().id)
            assertThat(c.failures).isEmpty()
            assertThat(s.declarations.map { it.id }).contains(c.specification.get().id)
            assertThat(s.findPackageSubprograms(c.specification.get().packageOwner, c.specification.get().localName)
                .declarations.map { it.id }).contains(c.specification.get().id)
            assertThat(s.findDeclarations(c.specification.get().qualifiedName).declarations.map { it.id })
                .contains(c.specification.get().id)
        }
        assertThat(s.bodies().map { s.correlateSpecification(it.id).specification.get() }
            .filterIsInstance<ProjectFunctionView>()).hasSize(1)
    }

    @Test
    fun bodyOnlyIsNotFoundAndDuplicateSpecificationsAreAmbiguous() {
        val bodyOnly = prepare("b.sql" to pkgBody("PROCEDURE pr IS BEGIN NULL; END;"))
        val nf = bodyOnly.correlateSpecification(bodyOnly.bodies().single().id)
        assertThat(nf.status).isEqualTo(Status.NOT_FOUND)
        assertThat(nf.specification).isEmpty
        assertThat(nf.candidates).isEmpty()

        val spec = pkgSpec("PROCEDURE pr;")
        val dup = prepare("a.sql" to spec, "c.sql" to spec, "b.sql" to pkgBody("PROCEDURE pr IS BEGIN NULL; END;"))
        val amb = dup.correlateSpecification(dup.bodies().single().id)
        assertThat(amb.status).isEqualTo(Status.AMBIGUOUS)
        assertThat(amb.specification).isEmpty
        assertThat(amb.candidates.map { it.id }).doesNotHaveDuplicates().hasSize(2)
        assertThat(amb.candidates.map { it.sourceRange.fileId }).containsExactly("a.sql", "c.sql")
        assertThat(amb.body.id).isEqualTo(dup.bodies().single().id)
    }

    @Test
    fun failuresTakePrecedenceForZeroOneAndManyMatches() {
        val body = pkgBody("PROCEDURE pr IS BEGIN NULL; END;")
        fun check(specs: List<String>, expected: Int) {
            val files = specs.mapIndexed { i, t -> "s$i.sql" to t } + ("b.sql" to body)
            val s = prepare(*files.toTypedArray(), failing = true)
            val c = s.correlateSpecification(s.bodies().single().id)
            assertThat(c.status).isEqualTo(Status.INCOMPLETE)
            assertThat(c.specification).isEmpty
            assertThat(c.candidates).hasSize(expected)
            assertThat(c.failures.single().fileId).isEqualTo("zz.sql")
            @Suppress("UNCHECKED_CAST")
            assertThatThrownBy { (c.candidates as MutableList<ProjectPackageSubprogramView>).clear() }
                .isInstanceOf(UnsupportedOperationException::class.java)
            @Suppress("UNCHECKED_CAST")
            assertThatThrownBy { (c.failures as MutableList<ProjectPreparationFailure>).clear() }
                .isInstanceOf(UnsupportedOperationException::class.java)
        }
        check(emptyList(), 0)
        check(listOf(pkgSpec("PROCEDURE pr;")), 1)
        check(listOf(pkgSpec("PROCEDURE pr;"), pkgSpec("PROCEDURE pr;")), 2)
    }

    @Test
    fun headingMatchingIsUnchangedByPublicViews() {
        assertThat(statusOf("PROCEDURE pr(a IN NUMBER)", "PROCEDURE pr(a IN NUMBER)")).isEqualTo(Status.RESOLVED)
        // Modes, names, order, precision/scale and return type distinguish headings.
        assertThat(statusOf("PROCEDURE pr(a IN NUMBER)", "PROCEDURE pr(a OUT NUMBER)")).isEqualTo(Status.NOT_FOUND)
        assertThat(statusOf("PROCEDURE pr(a NUMBER)", "PROCEDURE pr(b NUMBER)")).isEqualTo(Status.NOT_FOUND)
        assertThat(statusOf("PROCEDURE pr(a NUMBER, b VARCHAR2)", "PROCEDURE pr(b VARCHAR2, a NUMBER)"))
            .isEqualTo(Status.NOT_FOUND)
        assertThat(statusOf("PROCEDURE pr(a NUMBER(10,2))", "PROCEDURE pr(a NUMBER(10,2))")).isEqualTo(Status.RESOLVED)
        assertThat(statusOf("PROCEDURE pr(a NUMBER(10,2))", "PROCEDURE pr(a NUMBER(9,2))")).isEqualTo(Status.NOT_FOUND)
        assertThat(fnStatus("FUNCTION f RETURN NUMBER", "FUNCTION f RETURN NUMBER")).isEqualTo(Status.RESOLVED)
        assertThat(fnStatus("FUNCTION f RETURN NUMBER", "FUNCTION f RETURN VARCHAR2")).isEqualTo(Status.NOT_FOUND)
        assertThat(fnStatus("FUNCTION f RETURN NUMBER(5)", "FUNCTION f RETURN NUMBER(6)")).isEqualTo(Status.NOT_FOUND)
        // Defaults, NOCOPY and DETERMINISTIC are ignored.
        assertThat(statusOf("PROCEDURE pr(a IN OUT NOCOPY CLOB)", "PROCEDURE pr(a IN OUT CLOB)")).isEqualTo(Status.RESOLVED)
        assertThat(statusOf("PROCEDURE pr(a NUMBER DEFAULT 1)", "PROCEDURE pr(a NUMBER)")).isEqualTo(Status.RESOLVED)
        assertThat(fnStatus("FUNCTION f RETURN NUMBER DETERMINISTIC", "FUNCTION f RETURN NUMBER")).isEqualTo(Status.RESOLVED)
    }

    @Test
    fun invalidInputsAreRejectedBeforeSemanticCorrelation() {
        val src = arrayOf("s.sql" to pkgSpec("PROCEDURE pr;"), "b.sql" to pkgBody("PROCEDURE pr IS BEGIN NULL; END;"),
            "t.sql" to "CREATE TYPE ty AS OBJECT (a NUMBER);", "q.sql" to "CREATE SEQUENCE sq;")
        val s = prepare(*src)
        val other = prepare(*src)
        val failing = prepare(*src, failing = true)
        val bodyId = s.bodies().single().id
        assertThat(other.bodies().single().id.fileId).isEqualTo(bodyId.fileId)
        assertThat(other.bodies().single().id.ordinal).isEqualTo(bodyId.ordinal)

        assertThatThrownBy { s.correlateSpecification(other.bodies().single().id) }
            .isInstanceOf(IllegalArgumentException::class.java)
        // Failures never bypass validation.
        assertThatThrownBy { failing.correlateSpecification(s.bodies().single().id) }
            .isInstanceOf(IllegalArgumentException::class.java)
        val specId = s.declarations.first { it.role == ProjectDeclarationRole.SPECIFICATION && it is ProjectPackageSubprogramView }.id
        assertThatThrownBy { s.correlateSpecification(specId) }.isInstanceOf(IllegalArgumentException::class.java)
        listOf(ProjectDeclarationKind.STANDALONE_TYPE, ProjectDeclarationKind.SEQUENCE, ProjectDeclarationKind.PACKAGE).forEach { k ->
            val id = s.declarations.first { it.kind == k }.id
            assertThatThrownBy { s.correlateSpecification(id) }.isInstanceOf(IllegalArgumentException::class.java)
        }
        // Coordinates that exist in no file of this snapshot.
        val scope = ProjectSnapshot::class.java.getDeclaredField("scope").apply { isAccessible = true }.get(s) as ProjectSnapshotScope
        listOf(ProjectDeclarationId.create(scope, "b.sql", 99), ProjectDeclarationId.create(scope, "nope.sql", 0),
            ProjectDeclarationId.create(scope, " ", 0)).forEach {
            assertThatThrownBy { s.correlateSpecification(it) }.isInstanceOf(IllegalArgumentException::class.java)
        }
        @Suppress("CAST_NEVER_SUCCEEDS", "UNCHECKED_CAST")
        assertThatThrownBy { s.correlateSpecification(null as ProjectDeclarationId) }.isInstanceOf(NullPointerException::class.java)
    }

    @Test
    fun repeatedFactReferencesAndEqualFactsStayDistinctCandidates() {
        val file = FileId("r.sql")
        val owner = QualifiedName(listOf(OracleIdentifier.fromSource("p")))
        val name = OracleIdentifier.fromSource("run")
        fun proc(role: DeclarationRole, line: Int) = PackageProcedureDeclaration(owner, name, emptyList(), file,
            SourceRange(file, line, 0, line, 5), role)
        val spec = proc(DeclarationRole.SPECIFICATION, 1)
        val equalSpec = proc(DeclarationRole.SPECIFICATION, 1)
        val body = proc(DeclarationRole.BODY, 3)
        val builder = ProjectSymbolIndexBuilder()
        builder.add(file, listOf(spec, spec, equalSpec, body))
        val result = ProjectIndexPreparationResult(builder.build(), 1, emptyList())
        val s = ProjectSnapshot.create(result, ProjectPreparationState.PREPARED_WITH_DECLARATIONS, emptyList(), listOf("r.sql"))
        val c = s.correlateSpecification(s.bodies().single().id)
        assertThat(c.status).isEqualTo(Status.AMBIGUOUS)
        assertThat(c.candidates.map { it.id.ordinal }).containsExactly(0, 1, 2)
        assertThat(c.body.id.ordinal).isEqualTo(3)
    }
}
