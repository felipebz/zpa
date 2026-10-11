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
import com.felipebz.zpa.api.project.ProjectIdentifier
import com.felipebz.zpa.api.project.ProjectInventory
import com.felipebz.zpa.api.project.ProjectPreparationState
import com.felipebz.zpa.api.project.ProjectQualifiedName
import com.felipebz.zpa.api.project.ProjectSnapshot
import com.felipebz.zpa.api.project.ProjectSubprogramView
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.project.ProjectDeclarationKind as PublicKind

/** Package value facts built by hand: model, index and generic public projection. */
@OptIn(ZpaExperimentalApi::class)
class PackageValueDeclarationTest {
    private val extractor = ProjectDeclarationExtractor()

    private fun id(s: String) = OracleIdentifier.fromSource(s)
    private fun name(vararg s: String) = QualifiedName(s.map(::id))
    private fun range(file: FileId, line: Int, col: Int = 0) = SourceRange(file, line, col, line, col + 10)
    private fun named(t: String, file: FileId) = NamedTypeRef(name(t), range(file, 1))
    private fun value(
        file: FileId, line: Int, owner: QualifiedName, n: String, constant: Boolean = false,
        type: TypeRef = named("NUMBER", file)
    ) = PackageValueDeclaration(owner, id(n), constant, type, file, range(file, line))

    private fun index(vararg files: Pair<FileId, List<ProjectDeclaration>>) =
        ProjectSymbolIndexBuilder().also { b -> files.forEach { (f, d) -> b.add(f, d) } }.build()

    @Test
    fun kindsRolesAndOrdinals() {
        val f = FileId("a.sql")
        assertThat(value(f, 1, name("p"), "g").kind).isEqualTo(ProjectDeclarationKind.PACKAGE_VARIABLE)
        assertThat(value(f, 1, name("p"), "c", constant = true).kind).isEqualTo(ProjectDeclarationKind.PACKAGE_CONSTANT)
        assertThat(listOf(value(f, 1, name("p"), "g"), value(f, 1, name("p"), "c", true)).map { it.role })
            .containsOnly(DeclarationRole.SPECIFICATION)
        assertThat(value(f, 1, name("p"), "g")).isNotInstanceOf(ProjectTypeDeclaration::class.java)
            .isNotInstanceOf(PackageSubprogramDeclaration::class.java)
        assertThat(ProjectDeclarationKind.PACKAGE_VARIABLE.ordinal).isEqualTo(10)
        assertThat(ProjectDeclarationKind.PACKAGE_CONSTANT.ordinal).isEqualTo(11)
        assertThat(PublicKind.PACKAGE_VARIABLE.ordinal).isEqualTo(10)
        assertThat(PublicKind.PACKAGE_CONSTANT.ordinal).isEqualTo(11)
        listOf("PACKAGE", "PACKAGE_BODY", "STANDALONE_TYPE", "PACKAGE_TYPE", "PACKAGE_SUBTYPE", "PACKAGE_PROCEDURE",
            "PACKAGE_FUNCTION", "SEQUENCE", "STANDALONE_PROCEDURE", "STANDALONE_FUNCTION").forEachIndexed { i, n ->
            assertThat(ProjectDeclarationKind.valueOf(n).ordinal).isEqualTo(i)
            assertThat(PublicKind.valueOf(n).ordinal).isEqualTo(i)
        }
    }

    @Test
    fun qualifiedNamesKeepOwnerAndQuotingSemantics() {
        val f = FileId("a.sql")
        assertThat(value(f, 1, name("p"), "g").qualifiedName()).isEqualTo(name("P", "G"))
        assertThat(value(f, 1, name("s", "p"), "g").qualifiedName().segments).hasSize(3)
        assertThat(value(f, 1, name("p"), "\"Mixed\"").qualifiedName()).isNotEqualTo(name("p", "mixed"))
        assertThat(value(f, 1, name("p"), "\"Mixed\"").qualifiedName()).isEqualTo(name("P", "\"Mixed\""))
        val dotted = value(f, 1, name("p"), "\"a.b\"").qualifiedName()
        assertThat(dotted.segments).hasSize(2)
        assertThat(dotted).isNotEqualTo(name("p", "a", "b"))
    }

    @Test
    fun typeRefVariantsAndValueEquality() {
        val f = FileId("a.sql")
        val r = range(f, 1)
        val types = listOf<TypeRef>(NamedTypeRef(name("NUMBER"), r), AnchoredTypeRef(name("t", "c"), TypeAnchor.TYPE, r),
            AnchoredTypeRef(name("t"), TypeAnchor.ROWTYPE, r), RefTypeRef(name("o"), r))
        types.forEach { assertThat(value(f, 1, name("p"), "g", type = it).typeRef).isSameAs(it) }
        val base = value(f, 1, name("p"), "g")
        assertThat(base).isEqualTo(value(f, 1, name("P"), "G"))
        assertThat(base.hashCode()).isEqualTo(value(f, 1, name("P"), "G").hashCode())
        assertThat(base).isNotEqualTo(value(f, 1, name("p"), "h"))
        assertThat(base).isNotEqualTo(value(f, 1, name("q"), "g"))
        assertThat(base).isNotEqualTo(value(f, 1, name("p"), "g", constant = true))
        assertThat(base).isNotEqualTo(value(f, 1, name("p"), "g", type = types[1]))
        assertThat(base).isNotEqualTo(value(f, 2, name("p"), "g"))
        assertThat(base).isNotEqualTo(value(FileId("b.sql"), 1, name("p"), "g", type = named("NUMBER", FileId("b.sql"))))
    }

    @Test
    fun indexOrdersDeterministicallyAndKeepsEveryOccurrence() {
        val a = FileId("a.sql")
        val b = FileId("b.sql")
        val shared = value(b, 1, name("p"), "g")
        val aFacts = listOf(value(a, 5, name("p"), "g"), value(a, 1, name("p"), "c", true), value(a, 1, name("p"), "g"))
        val idx = index(b to listOf(shared, value(b, 3, name("p"), "g"), shared), a to aFacts)
        assertThat(idx.occurrencesFor(a).map { it.ordinal to it.declaration.sourceRange.startLine }).containsExactly(0 to 1, 1 to 1, 2 to 5)
        assertThat(idx.occurrencesFor(b).map { it.ordinal }).containsExactly(0, 1, 2)
        assertThat(idx.occurrencesFor(b).count { it.declaration === shared }).isEqualTo(2)
        assertThat(idx.findOccurrences(name("p", "g"))).hasSize(5)
        val reversed = index(a to aFacts.asReversed(), b to listOf(shared, shared, value(b, 3, name("p"), "g")))
        assertThat(reversed.declarations).containsExactlyElementsOf(idx.declarations)
        assertThat(idx.findDeclarations(name("p", "g")).map { it.fileId.value }).containsExactly("a.sql", "a.sql", "b.sql", "b.sql", "b.sql")
    }

    @Test
    fun valuesAreInvisibleToTypeSubprogramPackageAndSequenceIndexesAndCollideWithoutDeduplication() {
        val f = FileId("a.sql")
        val spec = extractor.extract(f, "CREATE PACKAGE p AS PROCEDURE g; TYPE t IS RECORD (a NUMBER); END p;")
        val s = FileId("s.sql")
        val standalone = StandaloneFunctionDeclaration(name("p", "g"), emptyList(), named("NUMBER", s), s, range(s, 1))
        val idx = index(f to spec + value(f, 99, name("p"), "g") + value(f, 98, name("p"), "t"), s to listOf(standalone))
        assertThat(idx.findDeclarations(name("p", "g")).map { it.kind }).containsExactlyInAnyOrder(
            ProjectDeclarationKind.PACKAGE_PROCEDURE, ProjectDeclarationKind.PACKAGE_VARIABLE, ProjectDeclarationKind.STANDALONE_FUNCTION)
        assertThat(idx.findSubprograms(name("p"), id("g"))).hasSize(1).allMatch { it is PackageProcedureDeclaration }
        assertThat(idx.findTypes(name("p"), id("g"))).isEmpty()
        assertThat(idx.findTypes(name("p"), id("t"))).hasSize(1).allMatch { it is PackageTypeDeclaration }
        assertThat(idx.findTypeOccurrences(name("p"), id("t"))).hasSize(1)
        assertThat(idx.findPackages(name("p", "g"))).isEmpty()
        assertThat(idx.findPackages(name("p"))).hasSize(1)
        assertThat(idx.findSequences(name("p", "g"))).isEmpty()
    }

    @Test
    fun extractionDoesNotEmitValuesYet() {
        val kinds = extractor.extract(FileId("a.sql"), "CREATE PACKAGE p AS g NUMBER; c CONSTANT NUMBER := 1; END p;").map { it.kind }
        assertThat(kinds).containsExactly(ProjectDeclarationKind.PACKAGE)
    }

    @Test
    fun genericPublicProjectionIsCanonicalIsolatedAndUnmodifiable() {
        val a = FileId("a.sql")
        val b = FileId("b.sql")
        val spec = extractor.extract(a, "CREATE PACKAGE p AS PROCEDURE g; END p;")
        val body = extractor.extract(b, "CREATE PACKAGE BODY p AS PROCEDURE g IS BEGIN NULL; END; END p;")
        val facts = listOf(value(a, 50, name("p"), "g"), value(a, 51, name("p"), "g"), value(a, 52, name("p"), "c", true))
        fun snapshot() = ProjectSnapshot.create(
            ProjectIndexPreparationResult(index(a to spec + facts, b to body), 2, emptyList()),
            ProjectPreparationState.PREPARED_WITH_DECLARATIONS, emptyList(), listOf("a.sql", "b.sql"))
        val s = snapshot()
        val byName = s.findDeclarations(ProjectQualifiedName.of("p", "g")).declarations
        val values = byName.filter { it.kind == PublicKind.PACKAGE_VARIABLE }
        assertThat(values).hasSize(2)
        assertThat(values.map { it.role }).containsOnly(ProjectDeclarationRole.SPECIFICATION)
        assertThat(values.map { it.qualifiedName }).containsOnly(ProjectQualifiedName.of("p", "g"))
        assertThat(values.map { it.fileId }).containsOnly("a.sql")
        assertThat(values.map { it.sourceRange.startLine }).containsExactly(50, 51)
        assertThat(values).noneMatch { it is ProjectSubprogramView }
        assertThat(values.map { it.id }).doesNotHaveDuplicates()
        val all = s.declarations.map { it.id }
        assertThat(all).containsAll(values.map { it.id })
        assertThat(s.declarationsFor("a.sql").declarations.filter { it.kind == PublicKind.PACKAGE_VARIABLE }.map { it.id })
            .isEqualTo(values.map { it.id })
        val constant = s.declarations.single { it.kind == PublicKind.PACKAGE_CONSTANT }
        assertThat(s.findDeclarations(ProjectQualifiedName.of("p", "c")).declarations.single().id).isEqualTo(constant.id)

        assertThat(snapshot().declarations.map { it.id }).doesNotContainAnyElementsOf(all)

        @Suppress("UNCHECKED_CAST")
        assertThatThrownBy { (s.declarations as MutableList<Any?>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        @Suppress("UNCHECKED_CAST")
        assertThatThrownBy { (s.declarationsFor("a.sql").declarations as MutableList<Any?>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)

        val members = s.findPackageSubprograms(ProjectQualifiedName.of("p"), ProjectIdentifier.fromSource("g"))
        assertThat(members.status).isEqualTo(ProjectInventory.Status.COMPLETE)
        assertThat(members.declarations.map { it.id }).doesNotContainAnyElementsOf(values.map { it.id })
        assertThat(s.findPackageSubprograms(ProjectQualifiedName.of("p"), ProjectIdentifier.fromSource("c")).declarations).isEmpty()
        assertThat(s.resolveNamedType(ProjectQualifiedName.of("g"), ProjectQualifiedName.of("p")).candidates).isEmpty()
        assertThatThrownBy { s.correlateSpecification(values.first().id) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { s.correlateSpecification(constant.id) }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
