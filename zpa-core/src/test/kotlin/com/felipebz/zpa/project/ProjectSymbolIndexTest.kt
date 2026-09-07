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
}
