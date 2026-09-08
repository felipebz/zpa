package com.felipebz.zpa.project

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProjectTypeResolverTest {
    private val extractor = ProjectDeclarationExtractor()
    private val range = SourceRange(FileId("reference.sql"), 1, 0, 1, 1)

    @Test
    fun resolvesStandalonePackageTypeAndPackageSubtype() {
        val standaloneFile = FileId("standalone.sql")
        val packageFile = FileId("package.sql")
        val standalone = extractor.extract(standaloneFile, "CREATE TYPE t AS OBJECT (id NUMBER);")
        val packageDeclarations = extractor.extract(
            packageFile,
            "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); SUBTYPE small IS NUMBER; END p;"
        )
        val resolver = ProjectTypeResolver(context(standaloneFile to standalone, packageFile to packageDeclarations))

        assertThat(resolver.resolve(named("t"))).isInstanceOf(ProjectTypeResolution.Resolved::class.java)
        assertThat(resolver.resolve(named("p.t"))).isInstanceOf(ProjectTypeResolution.Resolved::class.java)
        assertThat(resolver.resolve(named("p.small"))).isInstanceOf(ProjectTypeResolution.Resolved::class.java)
    }

    @Test
    fun completeEmptyIndexMeansNotFoundInProject() {
        val resolution = ProjectTypeResolver(context()).resolve(named("missing"))

        assertThat(resolution).isEqualTo(ProjectTypeResolution.NotFoundInProject(named("missing")))
    }

    @Test
    fun duplicateDeclarationsAreAmbiguousInDeterministicIndexOrder() {
        val a = FileId("a.sql")
        val b = FileId("b.sql")
        val sources = listOf(
            b to extractor.extract(b, "CREATE TYPE t AS OBJECT (id NUMBER);"),
            a to extractor.extract(a, "CREATE TYPE t AS OBJECT (id NUMBER);")
        )
        val first = ProjectTypeResolver(context(*sources.toTypedArray())).resolve(named("t"))
        val second = ProjectTypeResolver(context(*sources.asReversed().toTypedArray())).resolve(named("t"))

        assertThat(first).isInstanceOf(ProjectTypeResolution.Ambiguous::class.java)
        val firstCandidates = (first as ProjectTypeResolution.Ambiguous).candidates
        val secondCandidates = (second as ProjectTypeResolution.Ambiguous).candidates
        assertThat(firstCandidates).containsExactlyElementsOf(secondCandidates)
        assertThat(firstCandidates.map { it.fileId.value }).containsExactly("a.sql", "b.sql")
    }

    @Test
    fun preservesOracleNameSemanticsAndQualifiedSegmentQuoting() {
        val unquoted = FileId("unquoted.sql")
        val quoted = FileId("quoted.sql")
        val qualified = FileId("qualified.sql")
        val context = context(
            unquoted to extractor.extract(unquoted, "CREATE TYPE FOO AS OBJECT (id NUMBER);"),
            quoted to extractor.extract(quoted, "CREATE TYPE \"Foo\" AS OBJECT (id NUMBER);"),
            qualified to extractor.extract(qualified, "CREATE TYPE \"P\".\"T\" AS OBJECT (id NUMBER);")
        )
        val resolver = ProjectTypeResolver(context)

        assertThat(resolver.resolve(named("foo"))).isInstanceOf(ProjectTypeResolution.Resolved::class.java)
        assertThat(resolver.resolve(named("\"Foo\""))).isInstanceOf(ProjectTypeResolution.Resolved::class.java)
        assertThat(resolver.resolve(named("Foo"))).isInstanceOf(ProjectTypeResolution.Resolved::class.java)
        assertThat(resolver.resolve(named("\"P\".\"T\""))).isInstanceOf(ProjectTypeResolution.Resolved::class.java)
        assertThat(resolver.resolve(named("P.\"T\"")))
            .isEqualTo(ProjectTypeResolution.NotFoundInProject(named("P.\"T\"")))
    }

    @Test
    fun packageAndQualifiedStandaloneTypeRemainAmbiguous() {
        val packageFile = FileId("package.sql")
        val standaloneFile = FileId("qualified.sql")
        val context = context(
            packageFile to extractor.extract(packageFile, "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER); END p;"),
            standaloneFile to extractor.extract(standaloneFile, "CREATE TYPE p.t AS OBJECT (id NUMBER);")
        )

        val resolution = ProjectTypeResolver(context).resolve(named("P.T"))

        assertThat(resolution).isInstanceOf(ProjectTypeResolution.Ambiguous::class.java)
        assertThat((resolution as ProjectTypeResolution.Ambiguous).candidates.map { it.fileId.value })
            .containsExactly("package.sql", "qualified.sql")
    }

    @Test
    fun resolvesNamesWithMoreThanTwoSegmentsStructurally() {
        val file = FileId("schema.sql")
        val declaration = StandaloneTypeDeclaration(
            QualifiedName(listOf(
                OracleIdentifier.fromSource("S"),
                OracleIdentifier.fromSource("P"),
                OracleIdentifier.fromSource("T")
            )),
            ProjectTypeShape.OBJECT,
            file,
            range
        )
        val resolver = ProjectTypeResolver(context(file to listOf(declaration)))

        val resolution = resolver.resolve(named("s.p.t"))

        assertThat(resolution).isEqualTo(ProjectTypeResolution.Resolved(named("s.p.t"), declaration))
    }

    @Test
    fun everyPreparationStateIsPreservedByResolution() {
        val reference = named("t")
        val notPrepared = ProjectTypeResolver(ProjectAnalysisContext.NOT_PREPARED).resolve(reference)
        val empty = ProjectTypeResolver(context()).resolve(reference)
        val failed = ProjectTypeResolver(
            contextWithFailure(FileId("broken.sql"), includeKnownType = false)
        ).resolve(reference)
        val failedWithKnown = ProjectTypeResolver(
            contextWithFailure(FileId("broken.sql"), includeKnownType = true)
        ).resolve(reference)

        assertThat(notPrepared).isInstanceOf(ProjectTypeResolution.NotPrepared::class.java)
        assertThat(empty).isInstanceOf(ProjectTypeResolution.NotFoundInProject::class.java)
        assertThat(failed).isInstanceOf(ProjectTypeResolution.IncompleteIndex::class.java)
        assertThat(failedWithKnown).isInstanceOf(ProjectTypeResolution.IncompleteIndex::class.java)
        assertThat((failedWithKnown as ProjectTypeResolution.IncompleteIndex).knownCandidates).hasSize(1)
    }

    @Test
    fun incompleteIndexNeverClaimsUniquenessWithMultipleKnownCandidates() {
        val context = contextWithFailure(
            FileId("broken.sql"),
            includeKnownType = true,
            additionalKnownType = FileId("second.sql")
        )
        val resolution = ProjectTypeResolver(context).resolve(named("t"))

        assertThat(resolution).isInstanceOf(ProjectTypeResolution.IncompleteIndex::class.java)
        assertThat((resolution as ProjectTypeResolution.IncompleteIndex).knownCandidates.map { it.fileId.value })
            .containsExactly("first.sql", "second.sql")
    }

    private fun named(source: String): NamedTypeRef = NamedTypeRef(
        QualifiedName(source.split('.').map(OracleIdentifier::fromSource)),
        range
    )

    private fun context(vararg declarations: Pair<FileId, List<ProjectDeclaration>>): ProjectAnalysisContext {
        val builder = ProjectSymbolIndexBuilder()
        declarations.forEach { (fileId, facts) -> builder.add(fileId, facts) }
        return ProjectAnalysisContext.prepared(
            ProjectIndexPreparationResult(builder.build(), declarations.size, emptyList())
        )
    }

    private fun contextWithFailure(
        failedFile: FileId,
        includeKnownType: Boolean,
        additionalKnownType: FileId? = null
    ): ProjectAnalysisContext {
        val builder = ProjectSymbolIndexBuilder()
        if (includeKnownType) {
            val first = FileId("first.sql")
            builder.add(first, extractor.extract(first, "CREATE TYPE t AS OBJECT (id NUMBER);"))
            additionalKnownType?.let { fileId ->
                builder.add(fileId, extractor.extract(fileId, "CREATE TYPE t AS OBJECT (id NUMBER);"))
            }
        }
        val attemptedFiles = if (!includeKnownType) 1 else if (additionalKnownType == null) 2 else 3
        val result = ProjectIndexPreparationResult(
            builder.build(),
            attemptedFiles,
            listOf(ProjectIndexPreparationFailure(failedFile, "test.failure"))
        )
        return ProjectAnalysisContext.prepared(result)
    }
}
