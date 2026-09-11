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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.project

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ProjectSubprogramSpecificationResolverTest {
    private val owner = QualifiedName(OracleIdentifier.fromSource("p"))

    @Test
    fun correlatesProcedureDespiteNocopyAndDefaultDifferences() {
        val context = context(
            "p_spec.sql" to """
                CREATE PACKAGE p AS
                  PROCEDURE test(value IN OUT NOCOPY CLOB DEFAULT NULL);
                END p;
            """.trimIndent(),
            "p_body.sql" to """
                CREATE PACKAGE BODY p AS
                  PROCEDURE test(value IN OUT CLOB) IS
                  BEGIN
                    NULL;
                  END test;
                END p;
            """.trimIndent()
        )
        val specification = specification(context, "TEST")
        val body = body(context, "TEST")

        val result = ProjectSubprogramSpecificationResolver(context).resolve(body)

        assertThat(result).isEqualTo(
            ProjectSubprogramSpecificationResolution.Resolved(body, specification)
        )
        assertThat(specification.parameters.single().nocopy).isTrue
        assertThat(body.parameters.single().nocopy).isFalse
        assertThat(specification.parameters.single().defaultPresent).isTrue
        assertThat(body.parameters.single().defaultPresent).isFalse
    }

    @Test
    fun correlatesFunctionsAndPreservesReturnMetadata() {
        val context = context(
            "p_spec.sql" to "CREATE PACKAGE p AS FUNCTION make_value(value IN NUMBER) RETURN VARCHAR2; END p;",
            "p_body.sql" to "CREATE PACKAGE BODY p AS FUNCTION make_value(value IN NUMBER) RETURN VARCHAR2 IS BEGIN RETURN NULL; END make_value; END p;"
        )
        val specification = specification(context, "MAKE_VALUE")
        val body = body(context, "MAKE_VALUE")

        val result = ProjectSubprogramSpecificationResolver(context).resolve(body)

        assertThat(result).isEqualTo(ProjectSubprogramSpecificationResolution.Resolved(body, specification))
        assertThat((specification as PackageFunctionDeclaration).returnType.structuralKey())
            .isEqualTo((body as PackageFunctionDeclaration).returnType.structuralKey())
        assertThat(specification.sourceRange).isNotEqualTo(body.sourceRange)
    }

    @Test
    fun doesNotCorrelatePrivateBodyHelper() {
        val context = context(
            "p_body.sql" to "CREATE PACKAGE BODY p AS PROCEDURE helper(value NUMBER) IS BEGIN NULL; END helper; END p;"
        )

        val result = ProjectSubprogramSpecificationResolver(context).resolve(body(context, "HELPER"))

        assertThat(result).isEqualTo(ProjectSubprogramSpecificationResolution.NotFound(body(context, "HELPER")))
    }

    @Test
    fun distinguishesOverloadsWithoutUsingDeclarationOrder() {
        val sources = listOf(
            "p_spec_number.sql" to "CREATE PACKAGE p AS PROCEDURE work(value NUMBER); END p;",
            "p_spec_text.sql" to "CREATE PACKAGE p AS PROCEDURE work(value VARCHAR2); END p;",
            "p_body.sql" to """
                CREATE PACKAGE BODY p AS
                  PROCEDURE work(value NUMBER) IS BEGIN NULL; END work;
                  PROCEDURE work(value VARCHAR2) IS BEGIN NULL; END work;
                END p;
            """.trimIndent()
        )
        val first = context(*sources.toTypedArray())
        val second = context(*sources.asReversed().toTypedArray())

        val firstBodies = subprograms(first, "WORK").filter { it.role == DeclarationRole.BODY }
        val secondBodies = subprograms(second, "WORK").filter { it.role == DeclarationRole.BODY }
        val firstResults = firstBodies.map { ProjectSubprogramSpecificationResolver(first).resolve(it) }
        val secondResults = secondBodies.map { ProjectSubprogramSpecificationResolver(second).resolve(it) }

        assertThat(firstResults).containsExactlyElementsOf(secondResults)
        assertThat(firstResults).allMatch { it is ProjectSubprogramSpecificationResolution.Resolved }
        assertThat(firstResults.map { (it as ProjectSubprogramSpecificationResolution.Resolved).specification.parameters.single().typeRef.structuralKey() })
            .containsExactly(
                NamedTypeRef(QualifiedName(OracleIdentifier.fromSource("NUMBER")), SourceRange(FileId("test"), 1, 0, 1, 1)).structuralKey(),
                NamedTypeRef(QualifiedName(OracleIdentifier.fromSource("VARCHAR2")), SourceRange(FileId("test"), 1, 0, 1, 1)).structuralKey()
            )
    }

    @Test
    fun distinguishesProcedureFromFunctionWithTheSameName() {
        val context = context(
            "p_spec.sql" to "CREATE PACKAGE p AS PROCEDURE work(value NUMBER); END p;",
            "p_body.sql" to "CREATE PACKAGE BODY p AS FUNCTION work(value NUMBER) RETURN NUMBER IS BEGIN RETURN NULL; END work; END p;"
        )

        val result = ProjectSubprogramSpecificationResolver(context).resolve(body(context, "WORK"))

        assertThat(result).isInstanceOf(ProjectSubprogramSpecificationResolution.NotFound::class.java)
    }

    @Test
    fun doesNotCorrelateMeaningfullyDifferentHeaders() {
        assertNotFound(
            "CREATE PACKAGE p AS PROCEDURE work(value NUMBER); END p;",
            "CREATE PACKAGE BODY p AS PROCEDURE work(value NUMBER, other NUMBER) IS BEGIN NULL; END work; END p;"
        )
        assertNotFound(
            "CREATE PACKAGE p AS PROCEDURE work(value NUMBER); END p;",
            "CREATE PACKAGE BODY p AS PROCEDURE work(other NUMBER) IS BEGIN NULL; END work; END p;"
        )
        assertNotFound(
            "CREATE PACKAGE p AS PROCEDURE work(value NUMBER); END p;",
            "CREATE PACKAGE BODY p AS PROCEDURE work(value VARCHAR2) IS BEGIN NULL; END work; END p;"
        )
        assertNotFound(
            "CREATE PACKAGE p AS PROCEDURE work(value IN NUMBER); END p;",
            "CREATE PACKAGE BODY p AS PROCEDURE work(value OUT NUMBER) IS BEGIN NULL; END work; END p;"
        )
        assertNotFound(
            "CREATE PACKAGE p AS FUNCTION work(value NUMBER) RETURN NUMBER; END p;",
            "CREATE PACKAGE BODY p AS FUNCTION work(value NUMBER) RETURN VARCHAR2 IS BEGIN RETURN NULL; END work; END p;"
        )
    }

    @Test
    fun preservesQuotedIdentifierSemantics() {
        val context = context(
            "p_spec.sql" to "CREATE PACKAGE \"Pack\" AS PROCEDURE \"Work\"(\"Arg\" IN \"Owner\".\"Type\"); END \"Pack\";",
            "p_body.sql" to "CREATE PACKAGE BODY \"Pack\" AS PROCEDURE \"Work\"(\"Arg\" IN \"Owner\".\"Type\") IS BEGIN NULL; END \"Work\"; END \"Pack\";"
        )
        val qualifiedOwner = QualifiedName(OracleIdentifier.fromSource("\"Pack\""))
        val specification = subprograms(context, qualifiedOwner, OracleIdentifier.fromSource("\"Work\""))
            .single { it.role == DeclarationRole.SPECIFICATION }
        val body = subprograms(context, qualifiedOwner, OracleIdentifier.fromSource("\"Work\""))
            .single { it.role == DeclarationRole.BODY }

        val result = ProjectSubprogramSpecificationResolver(context).resolve(body)

        assertThat(result).isEqualTo(ProjectSubprogramSpecificationResolution.Resolved(body, specification))
    }

    @Test
    fun doesNotCollapseCharacterSetReturnHeadingIntoPlainReturnHeading() {
        val context = context(
            "p_spec.sql" to "CREATE PACKAGE p AS FUNCTION convert_xml RETURN CLOB; END p;",
            "p_body.sql" to "CREATE PACKAGE BODY p AS FUNCTION convert_xml RETURN CLOB CHARACTER SET I_XML%CHARSET IS BEGIN RETURN NULL; END convert_xml; END p;"
        )
        val specification = specification(context, "CONVERT_XML") as PackageFunctionDeclaration
        val body = body(context, "CONVERT_XML") as PackageFunctionDeclaration

        assertThat(body.returnType).isInstanceOf(NamedTypeRef::class.java)
        assertThat(body.returnType.structuralKey()).isEqualTo(specification.returnType.structuralKey())
        assertThat(body.overloadIdentity()).isEqualTo(specification.overloadIdentity())
        assertThat(body.returnType.headerIdentityKey()).isNotEqualTo(specification.returnType.headerIdentityKey())
        assertThat(ProjectSubprogramSpecificationResolver(context).resolve(body))
            .isEqualTo(ProjectSubprogramSpecificationResolution.NotFound(body))
    }

    @Test
    fun correlatesMatchingCharacterSetReturnHeadings() {
        val context = context(
            "p_spec.sql" to "CREATE PACKAGE p AS FUNCTION convert_xml RETURN CLOB CHARACTER SET ANY_CS; END p;",
            "p_body.sql" to "CREATE PACKAGE BODY p AS FUNCTION convert_xml RETURN CLOB CHARACTER SET ANY_CS IS BEGIN RETURN NULL; END convert_xml; END p;"
        )
        val specification = specification(context, "CONVERT_XML")
        val body = body(context, "CONVERT_XML")

        assertThat(ProjectSubprogramSpecificationResolver(context).resolve(body))
            .isEqualTo(ProjectSubprogramSpecificationResolution.Resolved(body, specification))
    }

    @Test
    fun copyDropsHeadingIdentityWhenTheNameChanges() {
        val range = SourceRange(FileId("test.sql"), 1, 0, 1, 9)
        val original = NamedTypeRef.withHeaderTypeIdentity(
            QualifiedName(OracleIdentifier.fromSource("TIMESTAMP")),
            range,
            "timestamp-with-time-zone"
        )

        assertThat(original.copy().headerIdentityKey()).isEqualTo("timestamp-with-time-zone")
        assertThat(
            original.copy(name = QualifiedName(OracleIdentifier.fromSource("VARCHAR2")))
                .headerIdentityKey()
        ).isEqualTo(
            NamedTypeRef(QualifiedName(OracleIdentifier.fromSource("VARCHAR2")), range).structuralKey()
        )
    }

    @Test
    fun characterSetFormsUseTheSameGeneralHeadingFingerprint() {
        assertNotFound(
            "CREATE PACKAGE p AS FUNCTION convert_xml RETURN CLOB; END p;",
            "CREATE PACKAGE BODY p AS FUNCTION convert_xml RETURN CLOB CHARACTER SET ANY_CS IS BEGIN RETURN NULL; END convert_xml; END p;",
            "CONVERT_XML"
        )

        val context = context(
            "p_spec.sql" to "CREATE PACKAGE p AS FUNCTION convert_xml RETURN CLOB CHARACTER SET I_XML%CHARSET; END p;",
            "p_body.sql" to "CREATE PACKAGE BODY p AS FUNCTION convert_xml RETURN CLOB CHARACTER SET I_XML%CHARSET IS BEGIN RETURN NULL; END convert_xml; END p;"
        )
        val specification = specification(context, "CONVERT_XML")
        val body = body(context, "CONVERT_XML")

        assertThat(ProjectSubprogramSpecificationResolver(context).resolve(body))
            .isEqualTo(ProjectSubprogramSpecificationResolution.Resolved(body, specification))
    }

    @Test
    fun doesNotCorrelateTimestampWithAndWithoutTimeZone() {
        assertNotFound(
            "CREATE PACKAGE p AS FUNCTION current_value RETURN TIMESTAMP; END p;",
            "CREATE PACKAGE BODY p AS FUNCTION current_value RETURN TIMESTAMP WITH TIME ZONE IS BEGIN RETURN NULL; END current_value; END p;",
            "CURRENT_VALUE"
        )
        assertNotFound(
            "CREATE PACKAGE p AS FUNCTION current_value RETURN TIMESTAMP WITH TIME ZONE; END p;",
            "CREATE PACKAGE BODY p AS FUNCTION current_value RETURN TIMESTAMP WITH LOCAL TIME ZONE IS BEGIN RETURN NULL; END current_value; END p;",
            "CURRENT_VALUE"
        )
    }

    @Test
    fun correlatesIdenticalTimestampWithTimeZoneHeadings() {
        val context = context(
            "p_spec.sql" to "CREATE PACKAGE p AS FUNCTION current_value RETURN TIMESTAMP WITH TIME ZONE; END p;",
            "p_body.sql" to "CREATE PACKAGE BODY p AS FUNCTION current_value RETURN TIMESTAMP WITH TIME ZONE IS BEGIN RETURN NULL; END current_value; END p;"
        )

        assertThat(ProjectSubprogramSpecificationResolver(context).resolve(body(context, "CURRENT_VALUE")))
            .isEqualTo(
                ProjectSubprogramSpecificationResolution.Resolved(
                    body(context, "CURRENT_VALUE"),
                    specification(context, "CURRENT_VALUE")
                )
            )
    }

    @Test
    fun normalizesWhitespaceAndUnquotedCaseInHeadingTypeFingerprints() {
        val context = context(
            "p_spec.sql" to "CREATE PACKAGE p AS FUNCTION current_value RETURN timestamp  WITH  time zone; END p;",
            "p_body.sql" to "CREATE PACKAGE BODY p AS FUNCTION current_value RETURN TIMESTAMP WITH TIME ZONE IS BEGIN RETURN NULL; END current_value; END p;"
        )

        assertThat(ProjectSubprogramSpecificationResolver(context).resolve(body(context, "CURRENT_VALUE")))
            .isInstanceOf(ProjectSubprogramSpecificationResolution.Resolved::class.java)
    }

    @Test
    fun doesNotCorrelateDifferentIntervalHeadingModifiers() {
        assertNotFound(
            "CREATE PACKAGE p AS FUNCTION duration RETURN INTERVAL YEAR TO MONTH; END p;",
            "CREATE PACKAGE BODY p AS FUNCTION duration RETURN INTERVAL DAY TO SECOND IS BEGIN RETURN NULL; END duration; END p;",
            "DURATION"
        )
    }

    @Test
    fun preservesOtherDatatypeTokenDifferences() {
        assertNotFound(
            "CREATE PACKAGE p AS PROCEDURE work(value VARCHAR2(10)); END p;",
            "CREATE PACKAGE BODY p AS PROCEDURE work(value VARCHAR2(20)) IS BEGIN NULL; END work; END p;"
        )
        assertNotFound(
            "CREATE PACKAGE p AS PROCEDURE work(value LONG); END p;",
            "CREATE PACKAGE BODY p AS PROCEDURE work(value LONG RAW) IS BEGIN NULL; END work; END p;"
        )
    }

    @Test
    fun correlatesIdenticalIntervalHeadingModifiers() {
        val context = context(
            "p_spec.sql" to "CREATE PACKAGE p AS FUNCTION duration RETURN INTERVAL YEAR(3) TO MONTH; END p;",
            "p_body.sql" to "CREATE PACKAGE BODY p AS FUNCTION duration RETURN INTERVAL YEAR(3) TO MONTH IS BEGIN RETURN NULL; END duration; END p;"
        )

        assertThat(ProjectSubprogramSpecificationResolver(context).resolve(body(context, "DURATION")))
            .isInstanceOf(ProjectSubprogramSpecificationResolution.Resolved::class.java)
    }

    @Test
    fun anchoredTypeReferencesRemainDistinctFromNamedTypes() {
        assertNotFound(
            "CREATE PACKAGE p AS PROCEDURE work(value employees.hire_date%TYPE); END p;",
            "CREATE PACKAGE BODY p AS PROCEDURE work(value DATE) IS BEGIN NULL; END work; END p;"
        )
    }

    @Test
    fun quotedAndUnquotedTypeIdentifiersRemainDistinct() {
        assertNotFound(
            "CREATE PACKAGE p AS PROCEDURE work(value owner.type_name); END p;",
            "CREATE PACKAGE BODY p AS PROCEDURE work(value \"owner\".\"type_name\") IS BEGIN NULL; END work; END p;"
        )
    }

    @Test
    fun functionOptionsOutsideTheHeadingDoNotChangeCorrelation() {
        val context = context(
            "p_spec.sql" to "CREATE PACKAGE p AS FUNCTION current_value RETURN NUMBER DETERMINISTIC; END p;",
            "p_body.sql" to "CREATE PACKAGE BODY p AS FUNCTION current_value RETURN NUMBER IS BEGIN RETURN NULL; END current_value; END p;"
        )

        assertThat(ProjectSubprogramSpecificationResolver(context).resolve(body(context, "CURRENT_VALUE")))
            .isInstanceOf(ProjectSubprogramSpecificationResolution.Resolved::class.java)
    }

    @Test
    fun resultCacheClausesOutsideTheHeadingDoNotChangeCorrelation() {
        val context = context(
            "p_spec.sql" to "CREATE PACKAGE p AS FUNCTION current_value RETURN NUMBER RESULT_CACHE; END p;",
            "p_body.sql" to "CREATE PACKAGE BODY p AS FUNCTION current_value RETURN NUMBER RESULT_CACHE RELIES_ON (values) IS BEGIN RETURN NULL; END current_value; END p;"
        )

        assertThat(ProjectSubprogramSpecificationResolver(context).resolve(body(context, "CURRENT_VALUE")))
            .isInstanceOf(ProjectSubprogramSpecificationResolution.Resolved::class.java)
    }

    @Test
    fun incompletePreparationNeverReturnsAuthoritativeCorrelation() {
        val specFile = FileId("p_spec.sql")
        val bodyFile = FileId("p_body.sql")
        val specification = ProjectDeclarationExtractor().extract(
            specFile,
            "CREATE PACKAGE p AS PROCEDURE work(value NUMBER); END p;"
        ).filterIsInstance<PackageProcedureDeclaration>().single()
        val body = ProjectDeclarationExtractor().extract(
            bodyFile,
            "CREATE PACKAGE BODY p AS PROCEDURE work(value NUMBER) IS BEGIN NULL; END work; END p;"
        ).filterIsInstance<PackageProcedureDeclaration>().single()
        val builder = ProjectSymbolIndexBuilder()
        builder.add(specFile, listOf(specification))
        val context = ProjectAnalysisContext.prepared(
            ProjectIndexPreparationResult(
                builder.build(),
                attemptedFileCount = 3,
                failures = listOf(ProjectIndexPreparationFailure(FileId("broken.sql"), "test.failure"))
            )
        )

        val result = ProjectSubprogramSpecificationResolver(context).resolve(body)

        assertThat(result).isEqualTo(
            ProjectSubprogramSpecificationResolution.Incomplete(
                body,
                listOf(specification),
                listOf(ProjectIndexPreparationFailure(FileId("broken.sql"), "test.failure"))
            )
        )
    }

    @Test
    fun preservesAmbiguityAndCandidateOrder() {
        val sources = listOf(
            "b_spec.sql" to "CREATE PACKAGE p AS PROCEDURE work(value NUMBER); END p;",
            "a_spec.sql" to "CREATE PACKAGE p AS PROCEDURE work(value NUMBER); END p;",
            "p_body.sql" to "CREATE PACKAGE BODY p AS PROCEDURE work(value NUMBER) IS BEGIN NULL; END work; END p;"
        )
        val first = context(*sources.toTypedArray())
        val second = context(*sources.asReversed().toTypedArray())

        val firstBody = body(first, "WORK")
        val secondBody = body(second, "WORK")
        val firstResult = ProjectSubprogramSpecificationResolver(first).resolve(firstBody)
        val secondResult = ProjectSubprogramSpecificationResolver(second).resolve(secondBody)

        assertThat(firstResult).isInstanceOf(ProjectSubprogramSpecificationResolution.Ambiguous::class.java)
        assertThat(secondResult).isInstanceOf(ProjectSubprogramSpecificationResolution.Ambiguous::class.java)
        val firstAmbiguous = firstResult as ProjectSubprogramSpecificationResolution.Ambiguous
        val secondAmbiguous = secondResult as ProjectSubprogramSpecificationResolution.Ambiguous
        assertThat(firstAmbiguous.candidates).containsExactlyElementsOf(secondAmbiguous.candidates)
        assertThatThrownBy {
            (firstAmbiguous.candidates as MutableList<PackageSubprogramDeclaration>).clear()
        }.isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun distinguishesPreparedEmptyFromNotPrepared() {
        val body = ProjectDeclarationExtractor().extract(
            FileId("p_body.sql"),
            "CREATE PACKAGE BODY p AS PROCEDURE work IS BEGIN NULL; END work; END p;"
        ).filterIsInstance<PackageProcedureDeclaration>().single()
        val emptyContext = ProjectAnalysisContext.prepared(
            ProjectIndexPreparationResult(ProjectSymbolIndexBuilder().build(), 0, emptyList())
        )

        assertThat(ProjectSubprogramSpecificationResolver(emptyContext).resolve(body))
            .isEqualTo(ProjectSubprogramSpecificationResolution.NotFound(body))
        assertThat(ProjectSubprogramSpecificationResolver(ProjectAnalysisContext.NOT_PREPARED).resolve(body))
            .isEqualTo(ProjectSubprogramSpecificationResolution.NotPrepared(body))
    }

    @Test
    fun rejectsSpecificationAsQueryInput() {
        val specification = ProjectDeclarationExtractor().extract(
            FileId("p_spec.sql"),
            "CREATE PACKAGE p AS PROCEDURE work; END p;"
        ).filterIsInstance<PackageProcedureDeclaration>().singleOrNull()

        assertThat(specification).isNotNull
        assertThatThrownBy {
            ProjectSubprogramSpecificationResolver(ProjectAnalysisContext.NOT_PREPARED)
                .resolve(specification!!)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun assertNotFound(specificationSource: String, bodySource: String, name: String = "WORK") {
        val context = context("p_spec.sql" to specificationSource, "p_body.sql" to bodySource)
        val result = ProjectSubprogramSpecificationResolver(context).resolve(body(context, name))
        assertThat(result).isInstanceOf(ProjectSubprogramSpecificationResolution.NotFound::class.java)
    }

    private fun context(vararg sources: Pair<String, String>): ProjectAnalysisContext =
        ProjectAnalysisContext.prepared(
            ProjectIndexPreparation().prepare(
                sources.map { (file, source) -> ProjectSource(FileId(file)) { source } },
                concurrent = false
            )
        )

    private fun index(context: ProjectAnalysisContext): ProjectSymbolIndex =
        (context.state as ProjectAnalysisContext.State.Prepared).result.index

    private fun subprograms(context: ProjectAnalysisContext, name: String): List<PackageSubprogramDeclaration> =
        subprograms(context, owner, OracleIdentifier.fromSource(name))

    private fun subprograms(
        context: ProjectAnalysisContext,
        owner: QualifiedName,
        name: OracleIdentifier
    ): List<PackageSubprogramDeclaration> = index(context).findSubprograms(owner, name)

    private fun specification(context: ProjectAnalysisContext, name: String): PackageSubprogramDeclaration =
        subprograms(context, name).single { it.role == DeclarationRole.SPECIFICATION }

    private fun body(context: ProjectAnalysisContext, name: String): PackageSubprogramDeclaration =
        subprograms(context, name).single { it.role == DeclarationRole.BODY }
}
