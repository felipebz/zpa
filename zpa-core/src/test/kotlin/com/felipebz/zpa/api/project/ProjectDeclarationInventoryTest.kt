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
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.PackageDeclaration
import com.felipebz.zpa.project.ProjectDeclarationExtractor
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectIndexPreparationResult
import com.felipebz.zpa.project.ProjectSymbolIndexBuilder
import com.felipebz.zpa.project.SequenceDeclaration
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

@OptIn(ZpaExperimentalApi::class)
class ProjectDeclarationInventoryTest {
    private fun prepare(vararg files: Pair<String, String>, concurrent: Boolean = false): ProjectSnapshot =
        ProjectSnapshots.prepare(files.map { (id, text) -> ProjectSourceInput.ofText(id, text) }, concurrent)

    private fun failing(id: String) = ProjectSourceInput.ofReader(id, ProjectSourceReader { throw IllegalStateException("x") })

    private val fixture = arrayOf(
        "a_spec.sql" to "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); SUBTYPE small IS NUMBER; " +
            "PROCEDURE pr(a NUMBER); FUNCTION f RETURN NUMBER; END p;",
        "b_body.sql" to "CREATE PACKAGE BODY p AS PROCEDURE pr(a NUMBER) IS BEGIN NULL; END; END p;",
        "c_type.sql" to "CREATE TYPE ty AS OBJECT (id NUMBER);",
        "d_seq.sql" to "CREATE SEQUENCE app.seq;"
    )

    @Test
    fun exposesKindRoleNameFileRangeAndIdForEveryExtractedCategory() {
        val snapshot = prepare(*fixture)
        val byKind = snapshot.declarations.groupBy { it.kind }

        // The extractor never emits package-body containers; none are synthesized.
        assertThat(byKind.keys).containsExactlyInAnyOrder(
            ProjectDeclarationKind.PACKAGE, ProjectDeclarationKind.PACKAGE_TYPE,
            ProjectDeclarationKind.PACKAGE_SUBTYPE, ProjectDeclarationKind.PACKAGE_PROCEDURE,
            ProjectDeclarationKind.PACKAGE_FUNCTION, ProjectDeclarationKind.STANDALONE_TYPE,
            ProjectDeclarationKind.SEQUENCE
        )
        fun single(kind: ProjectDeclarationKind, file: String) =
            byKind.getValue(kind).single { it.fileId == file }

        val pkg = single(ProjectDeclarationKind.PACKAGE, "a_spec.sql")
        assertThat(pkg.role).isEqualTo(ProjectDeclarationRole.SPECIFICATION)
        assertThat(pkg.qualifiedName).isEqualTo(ProjectQualifiedName.of("P"))
        assertThat(pkg.sourceRange.fileId).isEqualTo("a_spec.sql")
        assertThat(pkg.sourceRange.startLine).isEqualTo(1)
        assertThat(pkg.id.fileId).isEqualTo("a_spec.sql")

        val type = single(ProjectDeclarationKind.PACKAGE_TYPE, "a_spec.sql")
        assertThat(type.role).isEqualTo(ProjectDeclarationRole.SPECIFICATION)
        assertThat(type.qualifiedName).isEqualTo(ProjectQualifiedName.of("p", "t"))
        assertThat(single(ProjectDeclarationKind.PACKAGE_SUBTYPE, "a_spec.sql").qualifiedName)
            .isEqualTo(ProjectQualifiedName.of("p", "small"))
        assertThat(single(ProjectDeclarationKind.PACKAGE_FUNCTION, "a_spec.sql").qualifiedName)
            .isEqualTo(ProjectQualifiedName.of("p", "f"))

        val specProcedure = single(ProjectDeclarationKind.PACKAGE_PROCEDURE, "a_spec.sql")
        val bodyProcedure = single(ProjectDeclarationKind.PACKAGE_PROCEDURE, "b_body.sql")
        assertThat(specProcedure.role).isEqualTo(ProjectDeclarationRole.SPECIFICATION)
        assertThat(bodyProcedure.role).isEqualTo(ProjectDeclarationRole.BODY)
        assertThat(bodyProcedure.qualifiedName).isEqualTo(ProjectQualifiedName.of("p", "pr"))
        assertThat(bodyProcedure.sourceRange.fileId).isEqualTo("b_body.sql")

        val standalone = single(ProjectDeclarationKind.STANDALONE_TYPE, "c_type.sql")
        assertThat(standalone.role).isEqualTo(ProjectDeclarationRole.STANDALONE)
        assertThat(standalone.qualifiedName).isEqualTo(ProjectQualifiedName.of("ty"))

        val sequence = single(ProjectDeclarationKind.SEQUENCE, "d_seq.sql")
        assertThat(sequence.role).isEqualTo(ProjectDeclarationRole.STANDALONE)
        assertThat(sequence.qualifiedName).isEqualTo(ProjectQualifiedName.of("app", "seq"))
        val range = sequence.sourceRange
        assertThat(range.startLine).isEqualTo(1)
        assertThat(range.startColumn).isGreaterThanOrEqualTo(0)
        assertThat(range.endColumn).isGreaterThan(range.startColumn)
    }

    @Test
    fun sameOccurrenceHasEqualIdsAcrossQueryPathsAndDistinctOccurrencesDiffer() {
        val snapshot = prepare(*fixture)
        val all = snapshot.declarations

        assertThat(all.map { it.id }).doesNotHaveDuplicates()
        val viaFile = snapshot.declarationsFor("a_spec.sql").declarations
        assertThat(viaFile.map { it.id }).containsExactlyElementsOf(
            all.filter { it.fileId == "a_spec.sql" }.map { it.id }
        )
        val viaName = snapshot.findDeclarations(ProjectQualifiedName.of("APP", "SEQ")).declarations.single()
        assertThat(viaName.id).isEqualTo(all.single { it.kind == ProjectDeclarationKind.SEQUENCE }.id)
        assertThat(viaName.id.hashCode()).isEqualTo(
            all.single { it.kind == ProjectDeclarationKind.SEQUENCE }.id.hashCode()
        )
        // Ordinals are per-file positions.
        assertThat(viaFile.map { it.id.ordinal }).containsExactly(0, 1, 2, 3, 4)
    }

    @Test
    fun duplicateSourcesStayDistinctOccurrencesWithPreservedOrder() {
        val snapshot = prepare(
            "one.sql" to "CREATE SEQUENCE s; CREATE SEQUENCE s;",
            "two.sql" to "CREATE SEQUENCE s;"
        )
        val found = snapshot.findDeclarations(ProjectQualifiedName.of("S")).declarations

        assertThat(found.map { it.id.fileId to it.id.ordinal })
            .containsExactly("one.sql" to 0, "one.sql" to 1, "two.sql" to 0)
        assertThat(found.map { it.id }.toSet()).hasSize(3)
    }

    @Test
    fun sameFactReferenceInsertedTwiceReceivesDistinctOccurrences() {
        val file = FileId("f.sql")
        val fact = ProjectDeclarationExtractor().extract(file, "CREATE SEQUENCE s;").single() as SequenceDeclaration
        val builder = ProjectSymbolIndexBuilder()
        builder.add(file, listOf(fact, fact))
        val index = builder.build()

        assertThat(index.occurrences.map { it.ordinal }).containsExactly(0, 1)
        assertThat(index.occurrences[0]).isNotSameAs(index.occurrences[1])
        assertThat(index.occurrences[0].declaration).isSameAs(index.occurrences[1].declaration)
        assertThat(index.findOccurrences(fact.name).map { it.ordinal }).containsExactly(0, 1)
        assertThat(index.declarations).hasSize(2)
        // Fact equality is untouched.
        assertThat(fact).isEqualTo(ProjectDeclarationExtractor().extract(file, "CREATE SEQUENCE s;").single())
    }

    @Test
    fun idsFromSeparateSnapshotsAreUnequalEvenForIdenticalSourceAndCoordinates() {
        val a = prepare(*fixture)
        val b = prepare(*fixture)
        val idsA = a.declarations.map { it.id }
        val idsB = b.declarations.map { it.id }

        assertThat(idsA.map { it.fileId to it.ordinal }).isEqualTo(idsB.map { it.fileId to it.ordinal })
        idsA.indices.forEach { assertThat(idsA[it]).isNotEqualTo(idsB[it]) }

        val map = HashMap<ProjectDeclarationId, ProjectSnapshot>()
        idsA.forEach { map[it] = a }
        idsB.forEach { map[it] = b }
        assertThat(map).hasSize(idsA.size + idsB.size)
        assertThat(map[idsA[0]]).isSameAs(a)
        assertThat(map[idsB[0]]).isSameAs(b)

        // Later preparations do not disturb retained IDs.
        val retained = a.declarations[0].id
        val hash = retained.hashCode()
        prepare(*fixture)
        assertThat(retained).isEqualTo(a.declarations[0].id)
        assertThat(retained.hashCode()).isEqualTo(hash)
    }

    @Test
    fun identifierAndNameEqualitySemantics() {
        assertThat(ProjectIdentifier.fromSource("Foo")).isEqualTo(ProjectIdentifier.fromSource("FOO"))
        assertThat(ProjectIdentifier.fromSource("Foo").hashCode()).isEqualTo(ProjectIdentifier.fromSource("fOO").hashCode())
        assertThat(ProjectIdentifier.fromSource("\"Foo\"")).isNotEqualTo(ProjectIdentifier.fromSource("\"FOO\""))
        assertThat(ProjectIdentifier.fromSource("\"Foo\"")).isEqualTo(ProjectIdentifier.fromSource("\"Foo\""))
        assertThat(ProjectIdentifier.fromSource("\"FOO\"")).isNotEqualTo(ProjectIdentifier.fromSource("FOO"))
        assertThat(ProjectIdentifier.fromSource("\"Foo\"").isQuoted).isTrue()
        assertThat(ProjectIdentifier.fromSource("Foo").isQuoted).isFalse()
        assertThat(ProjectIdentifier.fromSource("\"Foo\"").originalSpelling).isEqualTo("\"Foo\"")

        assertThat(ProjectQualifiedName.of("a", "b").segments).hasSize(2)
        assertThat(ProjectQualifiedName.of("\"a.b\"").segments).hasSize(1)
        assertThat(ProjectQualifiedName.of("a.b").segments).hasSize(1)
        assertThat(ProjectQualifiedName.of("\"a.b\"").last.isQuoted).isTrue()
        assertThat(ProjectQualifiedName.of("A", "b")).isEqualTo(ProjectQualifiedName.of("a", "B"))
        assertThat(ProjectQualifiedName.of("a", "b")).isNotEqualTo(ProjectQualifiedName.of("b", "a"))
        assertThat(ProjectQualifiedName.of("a", "b").hashCode()).isEqualTo(ProjectQualifiedName.of("A", "B").hashCode())
        assertThatThrownBy { ProjectQualifiedName.of() }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { ProjectIdentifier.fromSource("") }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun segmentsAndInventoryCollectionsAreRuntimeUnmodifiable() {
        val snapshot = prepare(*fixture)
        val failing = ProjectSnapshots.prepare(listOf(ProjectSourceInput.ofText("ok.sql", "CREATE SEQUENCE s;"), failing("bad.sql")))
        val name = ProjectQualifiedName.of("a", "b")

        @Suppress("UNCHECKED_CAST")
        assertThatThrownBy { (name.segments as MutableList<ProjectIdentifier>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)
        @Suppress("UNCHECKED_CAST")
        assertThatThrownBy { (snapshot.declarations as MutableList<ProjectDeclarationView>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)
        val inventory = failing.findDeclarations(ProjectQualifiedName.of("s"))
        @Suppress("UNCHECKED_CAST")
        assertThatThrownBy { (inventory.declarations as MutableList<ProjectDeclarationView>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)
        @Suppress("UNCHECKED_CAST")
        assertThatThrownBy { (inventory.failures as MutableList<ProjectPreparationFailure>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)
        assertThat(name.segments).hasSize(2)
        assertThat(failing.findDeclarations(ProjectQualifiedName.of("s")).declarations).hasSize(1)
    }

    @Test
    fun inventoryOrderIsDeterministicAcrossInputOrderAndConcurrency() {
        val serial = prepare(*fixture, concurrent = false).declarations
        val concurrent = prepare(*fixture.reversedArray(), concurrent = true).declarations
        fun List<ProjectDeclarationView>.keys() =
            map { Triple(it.id.fileId, it.id.ordinal, it.kind) }

        assertThat(concurrent.keys()).isEqualTo(serial.keys())
        assertThat(serial.map { it.fileId }).isSorted()
    }

    @Test
    fun incompleteIndexKeepsKnownDeclarationsAndExposesFailures() {
        val snapshot = ProjectSnapshots.prepare(
            listOf(ProjectSourceInput.ofText("ok.sql", "CREATE SEQUENCE s;"), failing("bad.sql")), false
        )

        assertThat(snapshot.declarations).hasSize(1)
        val all = snapshot.declarationsFor("ok.sql")
        assertThat(all.status).isEqualTo(ProjectInventory.Status.INCOMPLETE_INDEX)
        assertThat(all.declarations).hasSize(1)
        assertThat(all.failures.map { it.fileId }).containsExactly("bad.sql")

        val failed = snapshot.declarationsFor("bad.sql")
        assertThat(failed.status).isEqualTo(ProjectInventory.Status.INCOMPLETE_INDEX)
        assertThat(failed.declarations).isEmpty()

        val missing = snapshot.findDeclarations(ProjectQualifiedName.of("nope"))
        assertThat(missing.status).isEqualTo(ProjectInventory.Status.INCOMPLETE_INDEX)
        assertThat(missing.declarations).isEmpty()
        assertThat(snapshot.findDeclarations(ProjectQualifiedName.of("s")).declarations).hasSize(1)
    }

    @Test
    fun unknownFileIdsYieldEmptyInventoriesWithoutNamespaceFallback() {
        val snapshot = prepare(*fixture)

        listOf("missing.sql", "D_SEQ.SQL", "./d_seq.sql", " ", "").forEach {
            val inventory = snapshot.declarationsFor(it)
            assertThat(inventory.status).isEqualTo(ProjectInventory.Status.COMPLETE)
            assertThat(inventory.declarations).isEmpty()
        }
    }

    @Test
    fun exactNameLookupDoesNotResolveRankOrScope() {
        val snapshot = prepare(
            "p.sql" to "CREATE PACKAGE p AS PROCEDURE q(a NUMBER); PROCEDURE q(a VARCHAR2); END p;",
            "t.sql" to "CREATE TYPE p.q AS OBJECT (id NUMBER);"
        )

        val matches = snapshot.findDeclarations(ProjectQualifiedName.of("P", "Q")).declarations
        assertThat(matches.map { it.kind }).containsExactlyInAnyOrder(
            ProjectDeclarationKind.PACKAGE_PROCEDURE, ProjectDeclarationKind.PACKAGE_PROCEDURE,
            ProjectDeclarationKind.STANDALONE_TYPE
        )
        assertThat(snapshot.findDeclarations(ProjectQualifiedName.of("q")).declarations).isEmpty()
        assertThat(snapshot.findDeclarations(ProjectQualifiedName.of("\"Q\"", "P")).declarations).isEmpty()
        assertThat(snapshot.findDeclarations(ProjectQualifiedName.of("\"P\"", "Q")).declarations).isEmpty()
        assertThat(snapshot.findDeclarations(ProjectQualifiedName.of("P", "Q")).status)
            .isEqualTo(ProjectInventory.Status.COMPLETE)
    }

    @Test
    fun emptyPreparedSnapshotHasCompleteEmptyInventories() {
        val snapshot = ProjectSnapshots.prepare(emptyList<ProjectSourceInput>())

        assertThat(snapshot.declarations).isEmpty()
        assertThat(snapshot.findDeclarations(ProjectQualifiedName.of("x")).status)
            .isEqualTo(ProjectInventory.Status.COMPLETE)
    }

    @Test
    fun notPreparedInventoryIsEmptyWithoutFailures() {
        val inventory = ProjectInventory.notPrepared<ProjectDeclarationView>()

        assertThat(inventory.status).isEqualTo(ProjectInventory.Status.NOT_PREPARED)
        assertThat(inventory.declarations).isEmpty()
        assertThat(inventory.failures).isEmpty()
    }

    @Test
    fun indexPreservesPackageFactsAsAuthoritativeOccurrences() {
        val result: ProjectIndexPreparationResult = ProjectIndexPreparation().prepare(
            listOf(com.felipebz.zpa.project.ProjectSource(FileId("p.sql")) { "CREATE PACKAGE p AS END p;" }), false
        )
        val occurrence = result.index.occurrences.single()

        assertThat(occurrence.declaration).isInstanceOf(PackageDeclaration::class.java)
        assertThat(result.index.declarations.single()).isSameAs(occurrence.declaration)
        assertThat(result.index.occurrencesFor(FileId("p.sql")).single()).isSameAs(occurrence)
    }
}
