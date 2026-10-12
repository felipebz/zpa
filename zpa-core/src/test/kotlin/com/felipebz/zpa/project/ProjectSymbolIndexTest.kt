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

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ProjectSymbolIndexTest {
    private val extractor = ProjectDeclarationExtractor()

    @Test
    fun indexIsIndependentOfFileAdditionOrder() {
        val declarations = listOf(
            FileId("b.sql") to extractor.extract(FileId("b.sql"), "CREATE PACKAGE p AS PROCEDURE work(value IN NUMBER); END p;"),
            FileId("a.sql") to extractor.extract(FileId("a.sql"), "CREATE PACKAGE p AS PROCEDURE work(value IN VARCHAR2); END p;")
        )

        val first = ProjectSymbolIndexBuilder().also { builder ->
            declarations.forEach { (file, facts) -> builder.add(file, facts) }
        }.build()
        val second = ProjectSymbolIndexBuilder().also { builder ->
            declarations.asReversed().forEach { (file, facts) -> builder.add(file, facts.asReversed()) }
        }.build()

        assertThat(first.fileIds).containsExactly(FileId("a.sql"), FileId("b.sql"))
        assertThat(first.declarations).containsExactlyElementsOf(second.declarations)
        val owner = QualifiedName(OracleIdentifier.fromSource("p"))
        val name = OracleIdentifier.fromSource("WORK")
        assertThat(first.findSubprograms(owner, name)).containsExactlyElementsOf(second.findSubprograms(owner, name))
        assertThat(first.findDeclarations(owner.append(name))).containsExactlyElementsOf(second.findDeclarations(owner.append(name)))
    }

    @Test
    fun indexPreservesDuplicateDeclarationsAndOverloadCandidates() {
        val file = FileId("p.sql")
        val facts = extractor.extract(file, """
            CREATE PACKAGE p AS
              PROCEDURE work(value IN NUMBER);
              PROCEDURE work(value IN VARCHAR2);
            END p;
        """.trimIndent())
        val index = ProjectSymbolIndexBuilder().also { it.add(file, facts) }.build()

        val candidates = index.findSubprograms(
            QualifiedName(OracleIdentifier.fromSource("P")),
            OracleIdentifier.fromSource("work")
        )
        assertThat(candidates).hasSize(2)
        assertThat(candidates.map { it.overloadIdentity() }.distinct()).hasSize(2)
    }

    @Test
    fun indexesSequencesByQualifiedIdentityAndPreservesDuplicates() {
        val firstFile = FileId("a.sql")
        val secondFile = FileId("b.sql")
        val firstFacts = extractor.extract(firstFile, "CREATE SEQUENCE app.seq;")
        val secondFacts = extractor.extract(secondFile, "CREATE SEQUENCE APP.SEQ;")
        val index = ProjectSymbolIndexBuilder().also {
            it.add(secondFile, secondFacts)
            it.add(firstFile, firstFacts)
        }.build()

        val name = QualifiedName(listOf(OracleIdentifier.fromSource("app"), OracleIdentifier.fromSource("seq")))
        assertThat(index.findSequences(name)).containsExactly(
            firstFacts.filterIsInstance<SequenceDeclaration>().single(),
            secondFacts.filterIsInstance<SequenceDeclaration>().single()
        )
        assertThat(index.findSequences(QualifiedName(OracleIdentifier.fromSource("seq")))).isEmpty()
    }

    @Test
    fun sequenceIndexPreparationIsDeterministicInSerialAndConcurrentModes() {
        val sources = listOf(
            ProjectSource(FileId("b.sql")) { "CREATE SEQUENCE app.second;" },
            ProjectSource(FileId("a.sql")) { "CREATE SEQUENCE app.first;" }
        )
        val serial = ProjectIndexPreparation().prepare(sources, concurrent = false)
        val concurrent = ProjectIndexPreparation().prepare(sources, concurrent = true)

        assertThat(serial.index.declarations).containsExactlyElementsOf(concurrent.index.declarations)
        assertThat(serial.index.findSequences(QualifiedName(listOf(
            OracleIdentifier.fromSource("app"),
            OracleIdentifier.fromSource("first")
        )))).hasSize(1)
    }

    @Test
    fun indexPreservesSpecificationAndBodyCandidatesDeterministically() {
        val specificationFile = FileId("a_spec.sql")
        val bodyFile = FileId("b_body.sql")
        val specificationSource = "CREATE PACKAGE p AS PROCEDURE work(value IN NUMBER); END p;"
        val bodySource = """
            CREATE PACKAGE BODY p AS
              PROCEDURE work(value IN NUMBER) IS BEGIN NULL; END;
              PROCEDURE work(value IN VARCHAR2) IS BEGIN NULL; END;
              PROCEDURE helper(value IN DATE) IS BEGIN NULL; END;
            END p;
        """.trimIndent()

        val first = ProjectIndexPreparation().prepare(
            listOf(
                ProjectSource(specificationFile) { specificationSource },
                ProjectSource(bodyFile) { bodySource }
            ),
            concurrent = false
        ).index
        val second = ProjectIndexPreparation().prepare(
            listOf(
                ProjectSource(bodyFile) { bodySource },
                ProjectSource(specificationFile) { specificationSource }
            ),
            concurrent = false
        ).index

        val owner = QualifiedName(OracleIdentifier.fromSource("p"))
        val candidates = first.findSubprograms(owner, OracleIdentifier.fromSource("work"))
        val reorderedCandidates = second.findSubprograms(owner, OracleIdentifier.fromSource("work"))
        assertThat(candidates).containsExactlyElementsOf(reorderedCandidates)
        assertThat(candidates.map { it.role }).containsExactly(DeclarationRole.SPECIFICATION, DeclarationRole.BODY, DeclarationRole.BODY)
        assertThat(candidates.map { it.overloadIdentity() }.distinct()).hasSize(2)
        assertThat(first.findSubprograms(owner, OracleIdentifier.fromSource("helper")))
            .singleElement()
            .extracting { it.role }
            .isEqualTo(DeclarationRole.BODY)
    }

    @Test
    fun typeLookupIncludesPackageSubtypes() {
        val file = FileId("subtype.sql")
        val facts = extractor.extract(file, "CREATE PACKAGE p AS SUBTYPE small IS NUMBER; END p;")
        val index = ProjectSymbolIndexBuilder().also { it.add(file, facts) }.build()

        assertThat(index.findTypes(QualifiedName(OracleIdentifier.fromSource("p")), OracleIdentifier.fromSource("small")))
            .singleElement()
            .isInstanceOf(PackageSubtypeDeclaration::class.java)
    }

    @Test
    fun indexReturnsBothQuotedAndUnquotedPackageCandidates() {
        val quotedFile = FileId("quoted.sql")
        val unquotedFile = FileId("unquoted.sql")
        val quoted = extractor.extract(quotedFile, "CREATE PACKAGE \"Foo\" AS END \"Foo\";")
        val unquoted = extractor.extract(unquotedFile, "CREATE PACKAGE FOO AS END FOO;")
        val index = ProjectSymbolIndexBuilder().also {
            it.add(quotedFile, quoted)
            it.add(unquotedFile, unquoted)
        }.build()

        assertThat(index.findPackages(QualifiedName(OracleIdentifier.fromSource("FOO")))).hasSize(1)
        assertThat(index.findPackages(QualifiedName(OracleIdentifier.fromSource("\"Foo\"")))).hasSize(1)
    }

    @Test
    fun preservesDuplicateCandidatesThatShareIdentityButDifferInRuleMetadata() {
        val file = FileId("metadata.sql")
        val facts = extractor.extract(file, """
            CREATE PACKAGE p AS
              PROCEDURE work(value IN OUT NOCOPY CLOB);
              PROCEDURE work(value IN OUT CLOB);
            END p;
        """.trimIndent())
        val index = ProjectSymbolIndexBuilder().also { it.add(file, facts) }.build()

        val candidates = index.findSubprograms(
            QualifiedName(OracleIdentifier.fromSource("p")),
            OracleIdentifier.fromSource("work")
        )
        assertThat(candidates).hasSize(2)
        assertThat(candidates.map { it.overloadIdentity() }.distinct()).hasSize(1)
        assertThat(candidates.map { it.parameters.single().nocopy }).containsExactlyInAnyOrder(false, true)
    }

    @Test
    fun frozenCollectionsCannotBeMutated() {
        val file = FileId("immutable.sql")
        val index = ProjectSymbolIndexBuilder().also {
            it.add(file, extractor.extract(file, "CREATE PACKAGE p AS END p;"))
        }.build()

        assertThatThrownBy { (index.fileIds as MutableList<FileId>).add(FileId("other.sql")) }
            .isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { (index.declarations as MutableList<ProjectDeclaration>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun declarationsForReturnsOnlyFrozenFactsForTheRequestedFile() {
        val firstFile = FileId("first.sql")
        val secondFile = FileId("second.sql")
        val firstFacts = extractor.extract(firstFile, "CREATE PACKAGE p AS PROCEDURE first_work; END p;")
        val secondFacts = extractor.extract(secondFile, "CREATE PACKAGE q AS PROCEDURE second_work; END q;")
        val index = ProjectSymbolIndexBuilder().also {
            it.add(firstFile, firstFacts)
            it.add(secondFile, secondFacts)
        }.build()

        val perFile = index.declarationsFor(firstFile)

        assertThat(perFile).containsExactlyElementsOf(firstFacts)
        assertThat(perFile).allSatisfy { declaration ->
            assertThat(declaration.fileId).isEqualTo(firstFile)
            assertThat(index.declarations.single { it === declaration }).isSameAs(declaration)
        }
        assertThat(index.declarationsFor(secondFile)).containsExactlyElementsOf(secondFacts)
        val unknownFileFacts = index.declarationsFor(FileId("unknown.sql"))
        assertThat(unknownFileFacts).isEmpty()
        assertThatThrownBy { (unknownFileFacts as MutableList<ProjectDeclaration>).add(firstFacts.first()) }
            .isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { (perFile as MutableList<ProjectDeclaration>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun declarationsForHasStableOrderingIndependentOfInputOrder() {
        val firstFile = FileId("first.sql")
        val secondFile = FileId("second.sql")
        val firstFacts = extractor.extract(firstFile, """
            CREATE PACKAGE p AS
              PROCEDURE first_work;
              TYPE first_type IS RECORD (id NUMBER);
            END p;
        """.trimIndent())
        val secondFacts = extractor.extract(secondFile, "CREATE PACKAGE q AS PROCEDURE second_work; END q;")

        val first = ProjectSymbolIndexBuilder().also {
            it.add(firstFile, firstFacts)
            it.add(secondFile, secondFacts)
        }.build()
        val second = ProjectSymbolIndexBuilder().also {
            it.add(secondFile, secondFacts.asReversed())
            it.add(firstFile, firstFacts.asReversed())
        }.build()

        assertThat(first.declarationsFor(firstFile)).containsExactlyElementsOf(second.declarationsFor(firstFile))
        assertThat(first.declarationsFor(secondFile)).containsExactlyElementsOf(second.declarationsFor(secondFile))
    }

    private fun indexOf(concurrent: Boolean, files: List<Pair<String, String>>) = ProjectIndexPreparation().prepare(
        files.map { (name, text) -> ProjectSource(FileId(name), ProjectSourceReader { text }) }, concurrent
    ).index

    private fun reference(index: ProjectSymbolIndex, name: QualifiedName) = index.occurrences.filter { it.declaration.qualifiedName() == name }

    private fun assertMatchesLinearReference(index: ProjectSymbolIndex) {
        val names = index.occurrences.map { it.declaration.qualifiedName() }.distinct() +
            QualifiedName(OracleIdentifier.fromSource("absent")) + QualifiedName(listOf(OracleIdentifier.fromSource("p"), OracleIdentifier.fromSource("absent")))
        names.forEach { name ->
            val indexed = index.findOccurrences(name)
            val expected = reference(index, name)
            assertThat(indexed).describedAs(name.toString()).hasSameSizeAs(expected)
            indexed.zip(expected).forEach { (a, b) -> assertThat(a).isSameAs(b) }
            assertThat(index.findDeclarations(name)).isEqualTo(expected.map { it.declaration })
        }
    }

    @Test
    fun exactNameLookupMatchesTheLinearReferenceForEveryKindAndName() {
        val files = listOf(
            "b.sql" to """
                CREATE PACKAGE p AS
                  g NUMBER;
                  "Mixed" NUMBER;
                  c CONSTANT NUMBER := 1;
                  TYPE t IS RECORD (a NUMBER);
                  SUBTYPE s IS NUMBER;
                  PROCEDURE g;
                  PROCEDURE work(a NUMBER);
                  PROCEDURE work(a DATE);
                  FUNCTION f RETURN NUMBER;
                END p;
                /
                CREATE PACKAGE BODY p AS PROCEDURE work(a NUMBER) IS BEGIN NULL; END; END p;
                /
                CREATE SEQUENCE seq;
                /
                CREATE TYPE st AS TABLE OF NUMBER;
                /
                CREATE PROCEDURE standalone_proc IS BEGIN NULL; END;
            """.trimIndent(),
            "a.sql" to "CREATE PACKAGE p AS g DATE; \"MIXED\" NUMBER; TYPE t IS RECORD (a NUMBER); END p;\n/\nCREATE SEQUENCE seq;\n/\nCREATE PACKAGE app.q AS x NUMBER; END q;"
        )
        val serial = indexOf(false, files)
        assertMatchesLinearReference(serial)
        assertMatchesLinearReference(indexOf(true, files))
        assertMatchesLinearReference(indexOf(false, files.reversed()))
        assertThat(indexOf(true, files).occurrences.map { it.fileId.value to it.ordinal }).isEqualTo(serial.occurrences.map { it.fileId.value to it.ordinal })

        fun name(vararg s: String) = QualifiedName(s.map(OracleIdentifier::fromSource))
        // Values and subprograms sharing a qualified name, in canonical order, across duplicate files.
        val g = serial.findOccurrences(name("p", "g"))
        assertThat(g.map { it.declaration.kind }).containsExactly(
            ProjectDeclarationKind.PACKAGE_VARIABLE, ProjectDeclarationKind.PACKAGE_VARIABLE, ProjectDeclarationKind.PACKAGE_PROCEDURE
        )
        assertThat(g.map { it.fileId.value }).containsExactly("a.sql", "b.sql", "b.sql")
        // "MIXED" and mixed are one normalized name; "Mixed" differs by case; qualified and standalone names share the map.
        assertThat(serial.findOccurrences(name("p", "mixed"))).hasSize(1)
        assertThat(serial.findOccurrences(QualifiedName(listOf(OracleIdentifier.fromSource("p"), OracleIdentifier.fromSource("\"Mixed\""))))).hasSize(1)
        assertThat(serial.findOccurrences(QualifiedName(listOf(OracleIdentifier.fromSource("p"), OracleIdentifier.fromSource("\"MIXED\""))))).hasSize(1)
        assertThat(serial.findOccurrences(name("seq")).map { it.fileId.value }).containsExactly("a.sql", "b.sql")
        assertThat(serial.findOccurrences(name("st")).single().declaration).isInstanceOf(StandaloneTypeDeclaration::class.java)
        assertThat(serial.findOccurrences(name("app", "q", "x"))).hasSize(1)
        assertThat(serial.findOccurrences(name("p", "work"))).hasSize(3)
        assertThat(serial.findOccurrences(name("nothing"))).isEmpty()
        assertThatThrownBy { (serial.findOccurrences(name("seq")) as MutableList<IndexedDeclaration>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun exactNameLookupOnAnEmptyIndex() {
        val empty = ProjectSymbolIndexBuilder().build()
        assertThat(empty.findOccurrences(QualifiedName(OracleIdentifier.fromSource("p")))).isEmpty()
        assertThat(empty.findDeclarations(QualifiedName(OracleIdentifier.fromSource("p")))).isEmpty()
    }
}
