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

import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.symbols.PlSqlType
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.squid.PlSqlConfiguration
import com.felipebz.zpa.symbols.DefaultTypeSolver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class TypeRefSemanticResolverTest {
    private val extractor = ProjectDeclarationExtractor()
    private val range = SourceRange(FileId("reference.sql"), 1, 0, 1, 1)
    private val resolver = TypeRefSemanticResolver(ProjectTypeResolver(emptyContext()))

    @Test
    fun classifiesRepresentativeBuiltInsWithoutCreatingDatatypes() {
        assertThat(resolver.resolve(named("NUMBER")))
            .isEqualTo(TypeRefSemanticResolution.BuiltIn(named("NUMBER"), PlSqlType.NUMERIC))
        assertThat(resolver.resolve(named("VARCHAR2")))
            .isEqualTo(TypeRefSemanticResolution.BuiltIn(named("VARCHAR2"), PlSqlType.CHARACTER))
        assertThat(resolver.resolve(named("DATE")))
            .isEqualTo(TypeRefSemanticResolution.BuiltIn(named("DATE"), PlSqlType.DATE))
        assertThat(resolver.resolve(named("CLOB")))
            .isEqualTo(TypeRefSemanticResolution.BuiltIn(named("CLOB"), PlSqlType.LOB))
        assertThat(resolver.resolve(named("BOOLEAN")))
            .isEqualTo(TypeRefSemanticResolution.BuiltIn(named("BOOLEAN"), PlSqlType.BOOLEAN))
        assertThat(resolver.resolve(named("JSON")))
            .isEqualTo(TypeRefSemanticResolution.BuiltIn(named("JSON"), PlSqlType.JSON))
    }

    @Test
    fun agreesWithDefaultTypeSolverForSupportedTypeRefSpellings() {
        val parser = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8))
        val solver = DefaultTypeSolver()
        val examples = listOf(
            "number" to "NUMBER",
            "varchar2(100)" to "VARCHAR2",
            "date" to "DATE",
            "clob" to "CLOB",
            "boolean" to "BOOLEAN",
            "json" to "JSON",
            "timestamp" to "TIMESTAMP",
            "interval year to month" to "INTERVAL",
            "long raw" to "LONG",
            "double precision" to "DOUBLE"
        )

        examples.forEach { (source, referenceName) ->
            parser.setRootRule(parser.grammar.rule(PlSqlGrammar.DATATYPE))
            val legacyType = solver.solve(parser.parse(source), null).type
            val semantic = resolver.resolve(named(referenceName))

            assertThat(semantic).isInstanceOf(TypeRefSemanticResolution.BuiltIn::class.java)
            assertThat((semantic as TypeRefSemanticResolution.BuiltIn).type).isEqualTo(legacyType)
        }
    }

    @Test
    fun delegatesProjectNamedTypesAndPreservesResolutionStates() {
        val file = FileId("types.sql")
        val declaration = extractor.extract(
            file,
            "CREATE PACKAGE customer_pkg AS TYPE customer_rec IS RECORD (id NUMBER); END customer_pkg;"
        ).filterIsInstance<PackageTypeDeclaration>().single()
        val context = preparedContext(file to listOf(declaration))
        val typeResolver = TypeRefSemanticResolver(ProjectTypeResolver(context))

        val result = typeResolver.resolve(named("customer_pkg.customer_rec"))

        assertThat(result).isEqualTo(
            TypeRefSemanticResolution.Project(
                named("customer_pkg.customer_rec"),
                ProjectTypeResolution.Resolved(named("customer_pkg.customer_rec"), declaration)
            )
        )
    }

    @Test
    fun usesExplicitPackageOwnerForUnqualifiedProjectTypes() {
        val file = FileId("types.sql")
        val declaration = extractor.extract(
            file,
            "CREATE PACKAGE p AS TYPE child_t IS RECORD (id NUMBER); END p;"
        ).filterIsInstance<PackageTypeDeclaration>().single()
        val context = preparedContext(file to listOf(declaration))
        val typeResolver = TypeRefSemanticResolver(ProjectTypeResolver(context))
        val owner = ProjectTypeLookupContext(QualifiedName(OracleIdentifier.fromSource("p")))

        val result = typeResolver.resolve(named("child_t"), owner)

        assertThat(result).isEqualTo(
            TypeRefSemanticResolution.Project(
                named("child_t"),
                ProjectTypeResolution.Resolved(named("child_t"), declaration)
            )
        )
    }

    @Test
    fun preservesProjectNotFoundAndDoesNotTreatQualifiedNamesAsBuiltIns() {
        val qualified = resolver.resolve(named("some_pkg.NUMBER"))
        val external = resolver.resolve(named("external_schema.external_type"))

        assertThat(qualified).isInstanceOf(TypeRefSemanticResolution.Project::class.java)
        assertThat((qualified as TypeRefSemanticResolution.Project).resolution)
            .isEqualTo(ProjectTypeResolution.NotFoundInProject(named("some_pkg.NUMBER")))
        assertThat(external).isInstanceOf(TypeRefSemanticResolution.Project::class.java)
        assertThat((external as TypeRefSemanticResolution.Project).resolution)
            .isEqualTo(ProjectTypeResolution.NotFoundInProject(named("external_schema.external_type")))
    }

    @Test
    fun quotedBuiltInLookingNamesUseProjectResolution() {
        val reference = named("\"NUMBER\"")

        val result = resolver.resolve(reference)

        assertThat(result).isEqualTo(
            TypeRefSemanticResolution.Project(
                reference,
                ProjectTypeResolution.NotFoundInProject(reference)
            )
        )
    }

    @Test
    fun unquotedBuiltInClassificationTakesPrecedenceOverAProjectCandidate() {
        val file = FileId("number.sql")
        val declaration = StandaloneTypeDeclaration(
            named("NUMBER").name,
            ProjectTypeShape.OBJECT,
            file,
            range.copy(fileId = file)
        )
        val typeResolver = TypeRefSemanticResolver(
            ProjectTypeResolver(preparedContext(file to listOf(declaration)))
        )

        val result = typeResolver.resolve(named("number"))

        assertThat(result).isEqualTo(
            TypeRefSemanticResolution.BuiltIn(named("number"), PlSqlType.NUMERIC)
        )
    }

    @Test
    fun preservesAmbiguousAndIncompleteProjectStates() {
        val first = FileId("first.sql")
        val second = FileId("second.sql")
        val known = extractor.extract(first, "CREATE TYPE t AS OBJECT (id NUMBER);")
        val duplicate = extractor.extract(second, "CREATE TYPE t AS OBJECT (id NUMBER);")
        val ambiguousResolver = TypeRefSemanticResolver(
            ProjectTypeResolver(preparedContext(first to known, second to duplicate))
        )
        val ambiguous = ambiguousResolver.resolve(named("t"))

        assertThat(ambiguous).isInstanceOf(TypeRefSemanticResolution.Project::class.java)
        assertThat((ambiguous as TypeRefSemanticResolution.Project).resolution)
            .isInstanceOf(ProjectTypeResolution.Ambiguous::class.java)

        val failedResult = ProjectIndexPreparationResult(
            ProjectSymbolIndexBuilder().apply { add(first, known) }.build(),
            attemptedFileCount = 2,
            failures = listOf(ProjectIndexPreparationFailure(second, "test failure"))
        )
        val incompleteResolver = TypeRefSemanticResolver(
            ProjectTypeResolver(ProjectAnalysisContext.prepared(failedResult))
        )
        val incomplete = incompleteResolver.resolve(named("t"))

        assertThat(incomplete).isInstanceOf(TypeRefSemanticResolution.Project::class.java)
        assertThat((incomplete as TypeRefSemanticResolution.Project).resolution)
            .isInstanceOf(ProjectTypeResolution.IncompleteIndex::class.java)
    }

    @Test
    fun preservesNotPreparedProjectState() {
        val reference = named("external_type")

        val result = TypeRefSemanticResolver(
            ProjectTypeResolver(ProjectAnalysisContext.NOT_PREPARED)
        ).resolve(reference)

        assertThat(result).isEqualTo(
            TypeRefSemanticResolution.Project(
                reference,
                ProjectTypeResolution.NotPrepared(reference)
            )
        )
    }

    @Test
    fun keepsAnchoredAndRefTypesExplicitlyUnsupported() {
        val anchored = AnchoredTypeRef(
            QualifiedName(listOf(OracleIdentifier.fromSource("some_table"), OracleIdentifier.fromSource("id"))),
            TypeAnchor.TYPE,
            range
        )
        val ref = RefTypeRef(QualifiedName(OracleIdentifier.fromSource("some_object_type")), range)

        assertThat(resolver.resolve(anchored)).isEqualTo(TypeRefSemanticResolution.Unsupported(anchored))
        assertThat(resolver.resolve(ref)).isEqualTo(TypeRefSemanticResolution.Unsupported(ref))
    }

    private fun named(source: String): NamedTypeRef = NamedTypeRef(
        QualifiedName(source.split('.').map(OracleIdentifier::fromSource)),
        range
    )

    private fun emptyContext() = ProjectAnalysisContext.prepared(
        ProjectIndexPreparationResult(ProjectSymbolIndexBuilder().build(), 0, emptyList())
    )

    private fun preparedContext(vararg declarations: Pair<FileId, List<ProjectDeclaration>>) =
        ProjectAnalysisContext.prepared(
            ProjectIndexPreparationResult(
                ProjectSymbolIndexBuilder().apply {
                    declarations.forEach { (fileId, facts) -> add(fileId, facts) }
                }.build(),
                declarations.size,
                emptyList()
            )
        )
}
