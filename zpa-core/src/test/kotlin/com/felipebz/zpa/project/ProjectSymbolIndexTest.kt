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
        assertThatThrownBy { (unknownFileFacts as java.util.List<ProjectDeclaration>).add(firstFacts.first()) }
            .isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { (perFile as java.util.List<ProjectDeclaration>).clear() }
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
}
